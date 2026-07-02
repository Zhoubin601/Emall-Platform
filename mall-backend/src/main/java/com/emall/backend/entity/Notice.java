package com.emall.backend.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cms_notice")
public class Notice {
    private Long id;
    private String title;
    private String content;
    private Integer isActive;
    private LocalDateTime createTime;
}