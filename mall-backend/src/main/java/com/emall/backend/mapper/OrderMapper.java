package com.emall.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emall.backend.entity.Order;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import java.util.List;

@Mapper
public interface OrderMapper extends BaseMapper<Order> {

    /**
     * ✨ 自定义查询：确保查出 product_id
     * 这里使用 @Select 注解直接写 SQL，#{userId} 是占位符
     */
    @Select("SELECT id, user_id, order_sn, total_amount, status, product_id, create_time, " +
            "comment_status " + // ✨ 关键：必须查出这个订单专属的评价状态
            "FROM oms_order WHERE user_id = #{userId} ORDER BY create_time DESC")
    List<Order> selectByUserId(Long userId);

    @Update("UPDATE oms_order SET status = #{targetStatus} " +
            "WHERE id = #{id} AND status = #{currentStatus}")
    int updateStatusIfCurrent(
            @Param("id") Long id,
            @Param("currentStatus") Integer currentStatus,
            @Param("targetStatus") Integer targetStatus);
}
