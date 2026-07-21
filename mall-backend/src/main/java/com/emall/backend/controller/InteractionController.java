package com.emall.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.emall.backend.entity.Feedback;
import com.emall.backend.entity.Notice;
import com.emall.backend.mapper.FeedbackMapper;
import com.emall.backend.mapper.NoticeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
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
        feedback.setUserId(authorizationService.currentUser(authentication).id());
        feedback.setSenderRole(0);
        feedback.setStatus(0);
        feedbackMapper.insert(feedback);
        return "提交成功，客服将尽快为您解答！";
    }

    @GetMapping("/feedback/my")
    public List<Feedback> getMyFeedbacks(@RequestParam Long userId, Authentication authentication) {
        userId = authorizationService.requireSelfOrAdmin(authentication, userId);
        return feedbackMapper.selectList(new QueryWrapper<Feedback>().eq("user_id", userId).orderByDesc("create_time"));
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
        feedback.setStatus(1);
        feedbackMapper.updateById(feedback);
        return "回复成功！";
    }

    // === 🔽 实时聊天引擎接口 🔽 ===
    @GetMapping("/chat/history")
    public List<Feedback> getChatHistory(@RequestParam Long userId, Authentication authentication) {
        userId = authorizationService.requireSelfOrAdmin(authentication, userId);
        return feedbackMapper.selectList(new QueryWrapper<Feedback>()
                .eq("user_id", userId)
                .orderByAsc("create_time"));
    }

    @PostMapping("/chat/send")
    public String sendChat(@RequestBody Feedback chat, Authentication authentication) {
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
        if (chat.getType() == null) {
            chat.setType("在线沟通");
        }

        // ✨ 核心修复：强行注入精准的当前时间！没有它，消息就会迷失在时间长河里
        chat.setCreateTime(java.time.LocalDateTime.now().withNano(0));

        feedbackMapper.insert(chat);
        return "发送成功";
    }
    @GetMapping("/chat/user-list")
    public List<Feedback> getChatUserList() {
        List<Feedback> allChats = feedbackMapper.selectList(new QueryWrapper<Feedback>().orderByDesc("create_time"));
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
}
