package com.emall.backend.controller;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.emall.backend.entity.Comment;
import com.emall.backend.entity.Order;
import com.emall.backend.entity.OrderItem;
import com.emall.backend.entity.User;
import com.emall.backend.mapper.CommentMapper;
import com.emall.backend.mapper.OrderItemMapper;
import com.emall.backend.mapper.OrderMapper;
import com.emall.backend.mapper.ProductMapper;
import com.emall.backend.mapper.UserMapper;
import com.emall.backend.security.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CommentControllerTest {

    private final CommentMapper commentMapper = mock(CommentMapper.class);
    private final OrderMapper orderMapper = mock(OrderMapper.class);
    private final OrderItemMapper orderItemMapper = mock(OrderItemMapper.class);
    private final ProductMapper productMapper = mock(ProductMapper.class);
    private final UserMapper userMapper = mock(UserMapper.class);
    private final AuthorizationService authorizationService = mock(AuthorizationService.class);
    private final Authentication authentication = mock(Authentication.class);
    private final CommentController controller = new CommentController();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "commentMapper", commentMapper);
        ReflectionTestUtils.setField(controller, "orderMapper", orderMapper);
        ReflectionTestUtils.setField(controller, "orderItemMapper", orderItemMapper);
        ReflectionTestUtils.setField(controller, "productMapper", productMapper);
        ReflectionTestUtils.setField(controller, "userMapper", userMapper);
        ReflectionTestUtils.setField(controller, "authorizationService", authorizationService);
    }

    @Test
    void completedOrderOwnerCanReviewAProductFromTheOrder() {
        Comment request = validComment();
        Order order = order(11L, 7L);
        User author = new User();
        author.setId(7L);
        author.setUsername("buyer");
        author.setNickname("Buyer Nickname");
        author.setAvatar("/avatar.png");

        when(orderMapper.selectById(11L)).thenReturn(order);
        when(orderItemMapper.selectOne(any(Wrapper.class))).thenReturn(new OrderItem());
        when(orderMapper.markCommentedIfEligible(11L)).thenReturn(1);
        when(userMapper.selectById(7L)).thenReturn(author);

        assertThat(controller.add(request, authentication)).isEqualTo("success");

        verify(authorizationService).requireOwner(authentication, 7L);
        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).insert(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(captor.getValue().getNickname()).isEqualTo("Buyer Nickname");
        assertThat(captor.getValue().getAvatar()).isEqualTo("/avatar.png");
        assertThat(captor.getValue().getContent()).isEqualTo("good product");
    }

    @Test
    void productOutsideTheOrderCannotBeReviewed() {
        Comment request = validComment();
        when(orderMapper.selectById(11L)).thenReturn(order(11L, 7L));
        when(orderItemMapper.selectOne(any(Wrapper.class))).thenReturn(null);

        assertThatThrownBy(() -> controller.add(request, authentication))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");

        verify(orderMapper, never()).markCommentedIfEligible(any());
        verify(commentMapper, never()).insert(any());
    }

    @Test
    void orderCannotBeReviewedTwiceOrBeforeCompletion() {
        Comment request = validComment();
        when(orderMapper.selectById(11L)).thenReturn(order(11L, 7L));
        when(orderItemMapper.selectOne(any(Wrapper.class))).thenReturn(new OrderItem());
        when(orderMapper.markCommentedIfEligible(11L)).thenReturn(0);

        assertThatThrownBy(() -> controller.add(request, authentication))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409 CONFLICT");

        verify(commentMapper, never()).insert(any());
    }

    @Test
    void invalidRatingAndBlankContentAreRejected() {
        Comment invalidRating = validComment();
        invalidRating.setStar(6);
        Comment blankContent = validComment();
        blankContent.setContent("  ");

        assertThatThrownBy(() -> controller.add(invalidRating, authentication))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
        assertThatThrownBy(() -> controller.add(blankContent, authentication))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400 BAD_REQUEST");
        verify(orderMapper, never()).selectById(any());
    }

    @Test
    void deletingTheLastReviewMakesTheOrderReviewableAgain() {
        Comment existing = validComment();
        existing.setId(31L);
        existing.setUserId(7L);
        when(commentMapper.selectById(31L)).thenReturn(existing);
        when(commentMapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        controller.deleteComment(31L, authentication);

        verify(commentMapper).deleteById(31L);
        verify(orderMapper).resetCommentStatus(11L);
    }

    @Test
    void updatingAReviewPreservesItsIdentityAndCreationTime() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 2, 3, 4);
        Comment existing = validComment();
        existing.setId(31L);
        existing.setUserId(7L);
        existing.setNickname("Original Author");
        existing.setAvatar("/original.png");
        existing.setCreateTime(createdAt);
        Comment update = validComment();
        update.setId(31L);
        update.setNickname("Forged Author");
        update.setAvatar("/forged.png");

        when(commentMapper.selectById(31L)).thenReturn(existing);

        controller.update(update, authentication);

        ArgumentCaptor<Comment> captor = ArgumentCaptor.forClass(Comment.class);
        verify(commentMapper).updateById(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(7L);
        assertThat(captor.getValue().getNickname()).isEqualTo("Original Author");
        assertThat(captor.getValue().getAvatar()).isEqualTo("/original.png");
        assertThat(captor.getValue().getCreateTime()).isEqualTo(createdAt);
    }

    private Comment validComment() {
        Comment comment = new Comment();
        comment.setOrderId(11L);
        comment.setProductId(22L);
        comment.setStar(5);
        comment.setContent("  good product  ");
        return comment;
    }

    private Order order(Long id, Long userId) {
        Order order = new Order();
        order.setId(id);
        order.setUserId(userId);
        return order;
    }
}
