package com.emall.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.emall.backend.entity.User;
import com.emall.backend.dto.user.LoginRequest;
import com.emall.backend.dto.user.RegisterRequest;
import com.emall.backend.dto.user.ResetPasswordRequest;
import com.emall.backend.mapper.UserMapper;
import com.emall.backend.security.AuthenticatedUser;
import com.emall.backend.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/user")
@Validated
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    private static final long CODE_TTL_SECONDS = 300;
    private static final long CODE_RESEND_SECONDS = 60;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, VerificationCode> codeCache = new ConcurrentHashMap<>();

    /**
     * 1. 管理端专用：获取所有用户列表
     */
    @GetMapping("/list")
    public List<User> getUserList() {
        return userMapper.selectList(new QueryWrapper<User>().orderByDesc("id"));
    }

    /**
     * ✨ 核心修复 2：管理端专用：新增用户/管理员 (解决 404 Bug)
     */
    @PostMapping("/add")
    public String addUser(@RequestBody User user) {
        // 校验账号是否重复
        Long count = userMapper.selectCount(new QueryWrapper<User>().eq("username", user.getUsername()));
        if (count > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该登录账号已存在，请换一个");
        }

        validatePassword(user.getPassword());
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 兜底头像与状态
        if (user.getAvatar() == null || user.getAvatar().isEmpty()) {
            user.setAvatar("/uploads/default-avatar.png");
        }
        if (user.getStatus() == null) user.setStatus(1);

        userMapper.insert(user);
        return "新增账号成功";
    }

    /**
     * 3. 管理端专用：删除用户
     */
    @DeleteMapping("/{id}")
    public String deleteUser(@PathVariable Long id) {
        userMapper.deleteById(id);
        return "用户删除成功";
    }

    /**
     * 4. 发送验证码
     */
    @PostMapping("/sendCode")
    public String sendCode(
            @RequestParam
            @NotBlank(message = "邮箱不能为空")
            @Email(message = "邮箱格式不正确") String email) {
        String normalizedEmail = normalizeEmail(email);
        Instant now = Instant.now();
        VerificationCode existing = codeCache.get(normalizedEmail);
        if (existing != null && now.isBefore(existing.sentAt().plusSeconds(CODE_RESEND_SECONDS))) {
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "验证码发送过于频繁，请稍后再试");
        }
        String code = String.format("%06d", secureRandom.nextInt(1_000_000));
        codeCache.put(normalizedEmail, new VerificationCode(code, now, now.plusSeconds(CODE_TTL_SECONDS)));

        try {
            if (mailSender == null || fromEmail.isBlank()) {
                throw new IllegalStateException("邮件服务未配置");
            }
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(normalizedEmail);
            message.setSubject("E-MALL 验证码");
            message.setText("您的验证码为：" + code + "，请在5分钟内完成验证。");
            mailSender.send(message);
            return "验证码已发送";
        } catch (Exception e) {
            codeCache.remove(normalizedEmail);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "验证码邮件发送失败，请稍后重试");
        }
    }

    /**
     * 5. 买家注册
     */
    @PostMapping("/register")
    public String register(@Valid @RequestBody RegisterRequest request) {
        String normalizedEmail = verifyCode(request.email(), request.code());

        Long count = userMapper.selectCount(new QueryWrapper<User>().eq("username", request.username()));
        if (count > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该登录账号已存在");
        }

        User user = new User();
        user.setUsername(request.username());
        user.setNickname(request.nickname());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEmail(normalizedEmail);
        user.setRole(0); // 默认买家
        user.setAvatar("/uploads/default-avatar.png");

        userMapper.insert(user);
        codeCache.remove(normalizedEmail);
        return "注册成功";
    }

    /**
     * 6. 统一登录
     */
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest user) {
        User dbUser = authenticate(user.username(), user.password(), false);
        return new AuthResponse(jwtService.issueToken(dbUser), dbUser);
    }

    /**
     * 7. 管理员专用登录 (带权限校验)
     */
    @PostMapping("/adminLogin")
    public AuthResponse adminLogin(@Valid @RequestBody LoginRequest user) {
        User dbUser = authenticate(user.username(), user.password(), true);
        return new AuthResponse(jwtService.issueToken(dbUser), dbUser);
    }

    /**
     * 8. 资料更新 (个人中心 & 管理员修改状态/信息)
     */
    /**
     * 8. 资料更新 (个人中心 & 管理员修改状态/角色/信息)
     */
    @PutMapping("/update")
    public User updateInfo(@RequestBody User user, Authentication authentication) {
        AuthenticatedUser currentUser = (AuthenticatedUser) authentication.getPrincipal();
        if (!currentUser.isAdmin() && !Objects.equals(currentUser.id(), user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不能修改其他用户的资料");
        }

        // 普通用户不能修改自己的账号、角色或封禁状态。
        if (!currentUser.isAdmin()) {
            user.setUsername(null);
            user.setRole(null);
            user.setStatus(null);
            user.setCreateTime(null);
        }

        if (user.getPassword() != null && user.getPassword().trim().isEmpty()) {
            user.setPassword(null);
        } else if (user.getPassword() != null) {
            validatePassword(user.getPassword());
            user.setPassword(passwordEncoder.encode(user.getPassword()));
        }

        userMapper.updateById(user);
        return userMapper.selectById(user.getId());
    }

    /**
     * 9. 找回密码
     */
    @PostMapping("/resetPassword")
    public String resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        String normalizedEmail = verifyCode(request.email(), request.code());
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("email", normalizedEmail));
        if (user == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "该邮箱未注册");

        user.setPassword(passwordEncoder.encode(request.password()));
        userMapper.updateById(user);
        codeCache.remove(normalizedEmail);
        return "重置成功";
    }

    private User authenticate(String username, String rawPassword, boolean adminOnly) {
        if (username == null || rawPassword == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        User dbUser = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
        if (dbUser == null || !passwordMatches(dbUser, rawPassword)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }
        if (adminOnly && (dbUser.getRole() == null || dbUser.getRole() < 1)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "该账号没有管理权限");
        }
        if (dbUser.getStatus() != null && dbUser.getStatus() == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "该账号已被禁用");
        }
        return dbUser;
    }

    private boolean passwordMatches(User user, String rawPassword) {
        String storedPassword = user.getPassword();
        if (storedPassword == null) return false;
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }

        // 兼容旧数据：首次成功登录后立即升级为 BCrypt，后续不再使用明文比较。
        boolean matches = MessageDigest.isEqual(
                rawPassword.getBytes(StandardCharsets.UTF_8),
                storedPassword.getBytes(StandardCharsets.UTF_8));
        if (matches) {
            user.setPassword(passwordEncoder.encode(rawPassword));
            userMapper.updateById(user);
        }
        return matches;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "密码长度必须为 8 到 72 个字符");
        }
    }

    private String verifyCode(String email, String submittedCode) {
        String normalizedEmail = normalizeEmail(email);
        VerificationCode cached = codeCache.get(normalizedEmail);
        if (cached == null || Instant.now().isAfter(cached.expiresAt())
                || submittedCode == null || !MessageDigest.isEqual(
                submittedCode.getBytes(StandardCharsets.UTF_8),
                cached.code().getBytes(StandardCharsets.UTF_8))) {
            if (cached != null && Instant.now().isAfter(cached.expiresAt())) {
                codeCache.remove(normalizedEmail);
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码错误或已失效");
        }
        return normalizedEmail;
    }

    private String normalizeEmail(String email) {
        if (email == null || !email.trim().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邮箱格式不正确");
        }
        return email.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public record AuthResponse(String token, User user) {}
    private record VerificationCode(String code, Instant sentAt, Instant expiresAt) {}

}
