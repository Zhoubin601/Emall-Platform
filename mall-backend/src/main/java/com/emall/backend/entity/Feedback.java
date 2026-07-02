package com.emall.backend.entity;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("cms_feedback")
public class Feedback {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String type;
    private String content;
    private String reply; // 旧版留言回复字段（保留以防报错）
    private Integer status; // 旧版状态（保留）

    // ✨ 核心升级：标记这条消息是谁发的 (0: 买家, 1: 客服)
    private Integer senderRole;

    private LocalDateTime createTime;
}