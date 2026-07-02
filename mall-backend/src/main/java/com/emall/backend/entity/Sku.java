package com.emall.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;

@Data
@TableName("pms_sku")
public class Sku {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long productId;
    private String specName;
    private BigDecimal price;
    private Integer stock;
    private String picUrl;
}