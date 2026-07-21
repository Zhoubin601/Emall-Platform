package com.emall.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emall.backend.entity.UserCoupon;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserCouponMapper extends BaseMapper<UserCoupon> {
    @Update("UPDATE sms_user_coupon SET status = 1, use_time = NOW() " +
            "WHERE id = #{id} AND user_id = #{userId} AND status = 0")
    int consume(@Param("id") Long id, @Param("userId") Long userId);

    @Update("UPDATE sms_user_coupon SET status = #{targetStatus}, use_time = NULL " +
            "WHERE id = #{id} AND user_id = #{userId} AND status = 1")
    int release(
            @Param("id") Long id,
            @Param("userId") Long userId,
            @Param("targetStatus") Integer targetStatus);
}
