package com.emall.backend.entity;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class ProductExcelDTO {
    // ================= 商品基础信息 =================
    @ExcelProperty("商品ID(修改保留,新增留空)")
    private Long productId;

    @ExcelProperty("商品名称(必填)")
    private String productName;

    @ExcelProperty("分类ID(1数码/2服装/3家居)")
    private Long categoryId;

    @ExcelProperty("商品基础价")
    private BigDecimal productPrice;

    @ExcelProperty("上架状态(1上架/0下架)")
    private Integer status;

    @ExcelProperty("商品描述")
    private String description;

    // ================= 商品规格信息(SKU) =================
    @ExcelProperty("规格ID(修改保留,新增留空)")
    private Long skuId;

    @ExcelProperty("规格名称(如:红色/256G)")
    private String specName;

    @ExcelProperty("规格专属价")
    private BigDecimal skuPrice;

    @ExcelProperty("规格补货库存")
    private Integer skuStock;

    @ExcelProperty("规格图片URL")
    private String skuPicUrl;
}