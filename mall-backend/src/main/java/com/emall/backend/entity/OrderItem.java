package com.emall.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@TableName("oms_order_item")
public class OrderItem implements Serializable {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long orderId;
    private Long productId;

    // ✨ 核心修复：补齐 skuId 字段，用于精准扣减规格库存
    private Long skuId;

    private String productName;
    private BigDecimal productPrice;
    private Integer productCount;
}