package com.emall.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("pms_category")
public class Category {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    // ✨ 对应你数据库真实的字段
    private Long parentId;

    private Integer level;
}