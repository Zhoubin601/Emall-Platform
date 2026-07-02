package com.emall.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("pms_comment")
public class Comment {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long productId;
    private Long userId;
    private String nickname;
    private String avatar;
    private Integer star;
    private String content;
    // 在 Comment.java 中追加
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String productName; // ✨ 商品名称
    private String pics;
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private String orderSn;    // ✨ 订单号
    @com.fasterxml.jackson.annotation.JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private java.time.LocalDateTime createTime;}