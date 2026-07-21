package com.emall.backend.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.Set;

@Component
public class OrderStatusPolicy {
    public static final int PENDING_PAYMENT = 0;
    public static final int PAID = 1;
    public static final int SHIPPED = 2;
    public static final int COMPLETED = 3;
    public static final int CANCELLED = 4;
    public static final int REFUNDED = 5;
    public static final int REFUND_REQUESTED = 6;

    private static final Map<Integer, Set<Integer>> USER_TRANSITIONS = Map.of(
            PENDING_PAYMENT, Set.of(PAID, CANCELLED),
            PAID, Set.of(REFUND_REQUESTED),
            SHIPPED, Set.of(COMPLETED, REFUND_REQUESTED));

    private static final Map<Integer, Set<Integer>> ADMIN_TRANSITIONS = Map.of(
            PENDING_PAYMENT, Set.of(PAID, CANCELLED),
            PAID, Set.of(SHIPPED, CANCELLED, REFUND_REQUESTED),
            SHIPPED, Set.of(COMPLETED, CANCELLED, REFUND_REQUESTED),
            REFUND_REQUESTED, Set.of(REFUNDED, SHIPPED));

    public void validateTransition(int currentStatus, int targetStatus, boolean admin) {
        Map<Integer, Set<Integer>> transitions = admin ? ADMIN_TRANSITIONS : USER_TRANSITIONS;
        if (!transitions.getOrDefault(currentStatus, Set.of()).contains(targetStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "当前订单状态不允许执行该操作");
        }
    }

    public boolean shouldRestoreInventory(int currentStatus, int targetStatus) {
        return targetStatus == CANCELLED
                || (currentStatus == REFUND_REQUESTED && targetStatus == REFUNDED);
    }

    public boolean canDelete(int status) {
        return status == COMPLETED || status == CANCELLED || status == REFUNDED;
    }
}
