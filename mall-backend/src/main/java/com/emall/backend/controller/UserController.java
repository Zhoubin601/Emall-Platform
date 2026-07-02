package com.emall.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.emall.backend.entity.User;
import com.emall.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@CrossOrigin // ✨ 核心修复 1：全局开启跨域支持，解决拦截问题
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserMapper userMapper;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    // 验证码缓存
    private final Map<String, String> codeCache = new ConcurrentHashMap<>();

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
            throw new RuntimeException("该登录账号已存在，请换一个");
        }

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
    public String sendCode(@RequestParam String email) {
        String code = String.valueOf(new Random().nextInt(899999) + 100000);
        codeCache.put(email, code);
        System.out.println("🚀 [E-MALL 调试] 验证码生成 -> [" + email + "]: " + code);

        try {
            if (mailSender != null && !fromEmail.isEmpty()) {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(fromEmail);
                message.setTo(email);
                message.setSubject("E-MALL 验证码");
                message.setText("您的验证码为：" + code + "，请在5分钟内完成验证。");
                mailSender.send(message);
                return "验证码已发送";
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "验证码已生成(请查看后端控制台)";
    }

    /**
     * 5. 买家注册
     */
    @PostMapping("/register")
    public String register(@RequestBody RegisterRequest request) {
        String cachedCode = codeCache.get(request.getEmail());
        if (cachedCode == null || !cachedCode.equals(request.getCode())) {
            throw new RuntimeException("验证码错误或已失效");
        }

        Long count = userMapper.selectCount(new QueryWrapper<User>().eq("username", request.getUsername()));
        if (count > 0) {
            throw new RuntimeException("该登录账号(Username)已存在");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setNickname(request.getNickname());
        user.setPassword(request.getPassword());
        user.setEmail(request.getEmail());
        user.setRole(0); // 默认买家
        user.setAvatar("/uploads/default-avatar.png");

        userMapper.insert(user);
        codeCache.remove(request.getEmail());
        return "注册成功";
    }

    /**
     * 6. 统一登录
     */
    @PostMapping("/login")
    public User login(@RequestBody User user) {
        User dbUser = userMapper.selectOne(new QueryWrapper<User>()
                .eq("username", user.getUsername())
                .eq("password", user.getPassword()));
        if (dbUser == null) {
            throw new RuntimeException("账号或密码错误");
        }
        // 如果是黑名单用户
        if (dbUser.getStatus() != null && dbUser.getStatus() == 0) {
            throw new RuntimeException("该账号已被封禁，请联系管理员");
        }
        return dbUser;
    }

    /**
     * 7. 管理员专用登录 (带权限校验)
     */
    @PostMapping("/adminLogin")
    public User adminLogin(@RequestBody User user) {
        User dbUser = userMapper.selectOne(new QueryWrapper<User>()
                .eq("username", user.getUsername())
                .eq("password", user.getPassword())
                .ge("role", 1)); // 必须是管理员
        if (dbUser == null) {
            throw new RuntimeException("权限不足或账号密码错误");
        }
        if (dbUser.getStatus() != null && dbUser.getStatus() == 0) {
            throw new RuntimeException("管理员账号异常禁用");
        }
        return dbUser;
    }

    /**
     * 8. 资料更新 (个人中心 & 管理员修改状态/信息)
     */
    /**
     * 8. 资料更新 (个人中心 & 管理员修改状态/角色/信息)
     */
    @PutMapping("/update")
    public User updateInfo(@RequestBody User user) {
        // ✨ 核心防爆逻辑：如果前端传来的密码是空字符串，说明不想修改密码。
        // 必须将其置为 null，这样 MyBatis-Plus 在 updateById 时就会自动忽略该字段，安全保留原密码！
        if (user.getPassword() != null && user.getPassword().trim().isEmpty()) {
            user.setPassword(null);
        }

        userMapper.updateById(user);
        return userMapper.selectById(user.getId());
    }

    /**
     * 9. 找回密码
     */
    @PostMapping("/resetPassword")
    public String resetPassword(@RequestBody RegisterRequest request) {
        String cachedCode = codeCache.get(request.getEmail());
        if (cachedCode == null || !cachedCode.equals(request.getCode())) {
            throw new RuntimeException("验证码错误");
        }
        User user = userMapper.selectOne(new QueryWrapper<User>().eq("email", request.getEmail()));
        if (user == null) throw new RuntimeException("该邮箱未注册");

        user.setPassword(request.getPassword());
        userMapper.updateById(user);
        codeCache.remove(request.getEmail());
        return "重置成功";
    }

    // 数据传输对象
    public static class RegisterRequest {
        private String username;
        private String nickname;
        private String email;
        private String password;
        private String code;
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }
}