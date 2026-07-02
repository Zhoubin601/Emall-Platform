package com.emall.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emall.backend.entity.Comment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 商品评价 Mapper 接口
 * 继承 BaseMapper 后自动拥有完整的 CRUD 能力
 */
@Mapper
public interface CommentMapper extends BaseMapper<Comment> {
    @Select("SELECT c.*, p.name AS productName, o.order_sn AS orderSn " +
            "FROM pms_comment c " +
            "LEFT JOIN pms_product p ON c.product_id = p.id " +
            "LEFT JOIN oms_order o ON c.order_id = o.id " +
            "WHERE c.user_id = #{userId} ORDER BY c.create_time DESC")
    List<Comment> selectMyComments(Long userId);
    // 基础的评价提交 (insert) 和 评价列表查询 (selectList) 均由 BaseMapper 提供
}