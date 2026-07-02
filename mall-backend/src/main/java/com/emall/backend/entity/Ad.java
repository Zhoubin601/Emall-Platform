package com.emall.backend.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("cms_ad")
public class Ad {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String picUrl;
    private String linkUrl;
    private Integer sort;
    private Integer status;
    private LocalDateTime createTime;
}