package com.emall.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emall.backend.entity.Sku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SkuMapper extends BaseMapper<Sku> {
    @Update("UPDATE pms_sku SET stock = stock - #{quantity} " +
            "WHERE id = #{skuId} AND stock >= #{quantity}")
    int deductStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    @Update("UPDATE pms_sku SET stock = stock + #{quantity} WHERE id = #{skuId}")
    int restoreStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);
}
