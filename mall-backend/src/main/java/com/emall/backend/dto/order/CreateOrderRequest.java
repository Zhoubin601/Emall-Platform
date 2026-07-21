package com.emall.backend.dto.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record CreateOrderRequest(
        Long userCouponId,
        @NotEmpty(message = "订单至少需要一件商品")
        List<@Valid CreateOrderItemRequest> items) {
}
