package com.emall.backend.service;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderStatusPolicyTest {
    private final OrderStatusPolicy policy = new OrderStatusPolicy();

    @Test
    void buyerCanOnlyPerformExpectedTransitions() {
        assertThatCode(() -> policy.validateTransition(0, 1, false)).doesNotThrowAnyException();
        assertThatCode(() -> policy.validateTransition(0, 4, false)).doesNotThrowAnyException();
        assertThatCode(() -> policy.validateTransition(2, 3, false)).doesNotThrowAnyException();
        assertThatThrownBy(() -> policy.validateTransition(0, 3, false))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> policy.validateTransition(1, 2, false))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void administratorCanShipAndReviewRefunds() {
        assertThatCode(() -> policy.validateTransition(1, 2, true)).doesNotThrowAnyException();
        assertThatCode(() -> policy.validateTransition(6, 5, true)).doesNotThrowAnyException();
        assertThatCode(() -> policy.validateTransition(6, 2, true)).doesNotThrowAnyException();
    }

    @Test
    void inventoryIsRestoredOnlyForCancellationOrApprovedRefund() {
        assertThat(policy.shouldRestoreInventory(0, 4)).isTrue();
        assertThat(policy.shouldRestoreInventory(6, 5)).isTrue();
        assertThat(policy.shouldRestoreInventory(1, 6)).isFalse();
        assertThat(policy.shouldRestoreInventory(6, 2)).isFalse();
    }
}
