package com.emall.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.emall.backend.entity.Feedback;
import com.emall.backend.entity.Notice;
import com.emall.backend.mapper.FeedbackMapper;
import com.emall.backend.mapper.NoticeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import com.emall.backend.security.AuthenticatedUser;
import com.emall.backend.security.AuthorizationService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/interaction")
public class InteractionController {

    @Autowired
    private FeedbackMapper feedbackMapper;
    @Autowired
    private NoticeMapper noticeMapper;
    @Autowired
    private AuthorizationService authorizationService;

    // === 🔽 以下为旧版兼容接口 (绝对保留，不破坏原有逻辑) 🔽 ===
    @PostMapping("/feedback/submit")
    public String submitFeedback(@RequestBody Feedback feedback, Authentication authentication) {
        validateMessageContent(feedback);
        if (feedback.getType() == null || feedback.getType().isBlank()
                || "在线沟通".equals(feedback.getType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择有效的反馈类型");
        }
        feedback.setUserId(authorizationService.currentUser(authentication).id());
        feedback.setSenderRole(0);
        feedback.setStatus(0);
        feedback.setReply(null);
        feedback.setCreateTime(java.time.LocalDateTime.now().withNano(0));
        feedbackMapper.insert(feedback);
        return "提交成功，客服将尽快为您解答！";
    }

    @GetMapping("/feedback/my")
    public List<Feedback> getMyFeedbacks(@RequestParam Long userId, Authentication authentication) {
        userId = authorizationService.requireSelfOrAdmin(authentication, userId);
        return feedbackMapper.selectList(new QueryWrapper<Feedback>()
                .eq("user_id", userId)
                .ne("type", "在线沟通")
                .orderByDesc("create_time"));
    }

    @GetMapping("/notice/active")
    public List<Notice> getActiveNotices() {
        return noticeMapper.selectList(new QueryWrapper<Notice>().eq("is_active", 1).orderByDesc("create_time"));
    }

    @GetMapping("/feedback/list")
    public List<Feedback> getAllFeedbacks() {
        return feedbackMapper.selectList(new QueryWrapper<Feedback>()
                .ne("type", "在线沟通")
                .orderByDesc("create_time"));
    }

    @PutMapping("/feedback/reply")
    public String replyFeedback(@RequestBody Feedback feedback) {
        if (feedback.getId() == null || feedback.getReply() == null || feedback.getReply().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "回复内容不能为空");
        }
        Feedback existing = feedbackMapper.selectById(feedback.getId());
        if (existing == null || "在线沟通".equals(existing.getType())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "反馈不存在");
        }
        Feedback update = new Feedback();
        update.setId(existing.getId());
        update.setReply(feedback.getReply().trim());
        update.setStatus(1);
        feedbackMapper.updateById(update);
        return "回复成功！";
    }

    // === 🔽 实时聊天引擎接口 🔽 ===
    @GetMapping("/chat/history")
    public List<Feedback> getChatHistory(@RequestParam Long userId, Authentication authentication) {
        userId = authorizationService.requireSelfOrAdmin(authentication, userId);
        return feedbackMapper.selectList(new QueryWrapper<Feedback>()
                .eq("user_id", userId)
                .eq("type", "在线沟通")
                .orderByAsc("create_time"));
    }

    @PostMapping("/chat/send")
    public String sendChat(@RequestBody Feedback chat, Authentication authentication) {
        validateMessageContent(chat);
        AuthenticatedUser currentUser = authorizationService.currentUser(authentication);
        if (currentUser.isAdmin()) {
            if (chat.getUserId() == null) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "缺少接收用户");
            }
            chat.setSenderRole(1);
        } else {
            chat.setUserId(currentUser.id());
            chat.setSenderRole(0);
        }
        chat.setStatus(1);
        chat.setType("在线沟通");
        chat.setContent(chat.getContent().trim());
        chat.setReply(null);

        // ✨ 核心修复：强行注入精准的当前时间！没有它，消息就会迷失在时间长河里
        chat.setCreateTime(java.time.LocalDateTime.now().withNano(0));

        feedbackMapper.insert(chat);
        return "发送成功";
    }
    @GetMapping("/chat/user-list")
    public List<Feedback> getChatUserList() {
        List<Feedback> allChats = feedbackMapper.selectList(new QueryWrapper<Feedback>()
                .eq("type", "在线沟通")
                .orderByDesc("create_time"));
        Map<Long, Feedback> latestChatMap = new LinkedHashMap<>();
        for (Feedback chat : allChats) {
            if (!latestChatMap.containsKey(chat.getUserId())) {
                latestChatMap.put(chat.getUserId(), chat);
            }
        }
        return new ArrayList<>(latestChatMap.values());
    }

    // === 🔽 以下为 ✨新增的公告管理端接口✨ 🔽 ===

    // 获取所有公告列表（包含已下线的）
    @GetMapping("/notice/list")
    public List<Notice> getAllNotices() {
        return noticeMapper.selectList(new QueryWrapper<Notice>().orderByDesc("create_time"));
    }

    // 发布新公告
    @PostMapping("/notice/add")
    public String addNotice(@RequestBody Notice notice) {
        if (notice.getIsActive() == null) {
            notice.setIsActive(1); // 默认直接上线发布
        }
        noticeMapper.insert(notice);
        return "公告发布成功";
    }

    // 修改公告（包括上下线状态切换）
    @PutMapping("/notice/update")
    public String updateNotice(@RequestBody Notice notice) {
        noticeMapper.updateById(notice);
        return "公告更新成功";
    }

    // 删除公告
    @DeleteMapping("/notice/delete/{id}")
    public String deleteNotice(@PathVariable Long id) {
        noticeMapper.deleteById(id);
        return "公告删除成功";
    }

    private void validateMessageContent(Feedback feedback) {
        if (feedback == null || feedback.getContent() == null || feedback.getContent().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "消息内容不能为空");
        }
        if (feedback.getContent().length() > 5000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "消息内容不能超过 5000 个字符");
        }
        feedback.setContent(feedback.getContent().trim());
    }
}
