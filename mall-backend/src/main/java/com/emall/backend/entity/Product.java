package com.emall.backend.entity;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("pms_product")
public class Product {
    // ✨ 核心修复：放出 ID，用于导入时识别是更新还是新增！
    @ExcelProperty("商品ID(原样保留为修改,留空为新增)")
    @TableId(type = IdType.AUTO)
    private Long id;

    @ExcelProperty("分类ID(1数码/2服装/3家居)")
    private Long categoryId;

    @ExcelProperty("商品名称")
    private String name;

    @ExcelProperty("商品单价")
    private BigDecimal price;

    @ExcelProperty("当前库存")
    private Integer stock;

    @ExcelProperty("商品详细描述")
    private String description;

    @ExcelProperty("上架状态(1上架/0下架)")
    private Integer status;

    @ExcelIgnore
    private Integer sales;

    @ExcelIgnore
    private String picUrl;

    @ExcelIgnore
    private LocalDateTime createTime;

    @ExcelIgnore
    private BigDecimal promoPrice;

    @ExcelIgnore
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime promoStartTime;

    @ExcelIgnore
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime promoEndTime;
}