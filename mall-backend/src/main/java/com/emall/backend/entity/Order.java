package com.emall.backend.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.format.DateTimeFormat;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@TableName("oms_order")
public class Order {
    private Long productId;
    private Integer commentStatus;

    // 逻辑关联：该订单下的所有子项
    @TableField(exist = false)
    private List<OrderItem> items;

    // ✨ 核心补漏：接收前端传来的优惠券ID，但不存入订单主表
    @TableField(exist = false)
    private Long userCouponId;

    @ExcelProperty("订单ID")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ExcelProperty("买家ID")
    private Long userId;

    @ExcelProperty("订单编号")
    private String orderSn;

    @ExcelProperty("总金额")
    private BigDecimal totalAmount;

    @ExcelProperty("订单状态")
    private Integer status;

    @ExcelProperty("下单时间")
    @ColumnWidth(20)
    @DateTimeFormat("yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}