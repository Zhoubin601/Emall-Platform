package com.emall.backend.dto.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateOrderItemRequest(
        @NotNull(message = "商品规格不能为空") Long skuId,
        @NotNull(message = "购买数量不能为空")
        @Min(value = 1, message = "购买数量不能小于 1")
        @Max(value = 100, message = "单个规格最多购买 100 件") Integer productCount) {
}
