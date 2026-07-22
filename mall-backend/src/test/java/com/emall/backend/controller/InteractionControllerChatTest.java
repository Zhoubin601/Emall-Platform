package com.emall.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.emall.backend.entity.Feedback;
import com.emall.backend.mapper.FeedbackMapper;
import com.emall.backend.security.AuthenticatedUser;
import com.emall.backend.security.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InteractionControllerChatTest {

    private final FeedbackMapper feedbackMapper = mock(FeedbackMapper.class);
    private final AuthorizationService authorizationService = mock(AuthorizationService.class);
    private final InteractionController controller = new InteractionController();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "feedbackMapper", feedbackMapper);
        ReflectionTestUtils.setField(controller, "authorizationService", authorizationService);
        when(feedbackMapper.selectList(any())).thenReturn(List.of());
    }

    @Test
    void chatHistoryOnlyQueriesOnlineMessages() {
        Authentication authentication = mock(Authentication.class);
        when(authorizationService.requireSelfOrAdmin(authentication, 7L)).thenReturn(7L);

        controller.getChatHistory(7L, authentication);

        assertChatTypeCondition(capturedQuery());
    }

    @Test
    void chatUserListOnlyQueriesOnlineMessages() {
        controller.getChatUserList();

        assertChatTypeCondition(capturedQuery());
    }

    @Test
    void myFeedbackListExcludesOnlineMessages() {
        Authentication authentication = mock(Authentication.class);
        when(authorizationService.requireSelfOrAdmin(authentication, 7L)).thenReturn(7L);

        controller.getMyFeedbacks(7L, authentication);

        QueryWrapper<Feedback> query = capturedQuery();
        assertThat(query.getCustomSqlSegment()).contains("type").contains("<>");
        assertThat(query.getParamNameValuePairs()).containsValue("在线沟通");
    }

    @Test
    void chatSendAlwaysUsesOnlineTypeAndAuthenticatedBuyer() {
        Authentication authentication = mock(Authentication.class);
        when(authorizationService.currentUser(authentication))
                .thenReturn(new AuthenticatedUser(7L, "buyer", 0));
        Feedback submitted = new Feedback();
        submitted.setUserId(99L);
        submitted.setType("功能建议");
        submitted.setContent("  hello  ");

        controller.sendChat(submitted, authentication);

        ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackMapper).insert(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(captor.getValue().getType()).isEqualTo("在线沟通");
        assertThat(captor.getValue().getContent()).isEqualTo("hello");
        assertThat(captor.getValue().getSenderRole()).isZero();
    }

    @Test
    void formalFeedbackCannotInjectMessagesIntoChat() {
        Authentication authentication = mock(Authentication.class);
        Feedback submitted = new Feedback();
        submitted.setType("在线沟通");
        submitted.setContent("injected chat");

        assertThatThrownBy(() -> controller.submitFeedback(submitted, authentication))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");

        verify(feedbackMapper, never()).insert(any());
    }

    @Test
    void feedbackReplyOnlyUpdatesReplyAndStatus() {
        Feedback existing = new Feedback();
        existing.setId(15L);
        existing.setUserId(7L);
        existing.setType("功能建议");
        Feedback request = new Feedback();
        request.setId(15L);
        request.setUserId(999L);
        request.setReply("  handled  ");
        when(feedbackMapper.selectById(15L)).thenReturn(existing);

        controller.replyFeedback(request);

        ArgumentCaptor<Feedback> captor = ArgumentCaptor.forClass(Feedback.class);
        verify(feedbackMapper).updateById(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(15L);
        assertThat(captor.getValue().getUserId()).isNull();
        assertThat(captor.getValue().getReply()).isEqualTo("handled");
        assertThat(captor.getValue().getStatus()).isEqualTo(1);
    }

    private QueryWrapper<Feedback> capturedQuery() {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<QueryWrapper<Feedback>> captor = ArgumentCaptor.forClass(QueryWrapper.class);
        verify(feedbackMapper).selectList(captor.capture());
        return captor.getValue();
    }

    private void assertChatTypeCondition(QueryWrapper<Feedback> query) {
        assertThat(query.getCustomSqlSegment()).contains("type");
        assertThat(query.getParamNameValuePairs()).containsValue("在线沟通");
    }
}
