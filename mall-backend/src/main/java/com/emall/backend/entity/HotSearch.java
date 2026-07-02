package com.emall.backend.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("pms_hot_search")
public class HotSearch {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String keyword;     // 搜索关键词
    private Integer searchCount; // 搜索次数
}