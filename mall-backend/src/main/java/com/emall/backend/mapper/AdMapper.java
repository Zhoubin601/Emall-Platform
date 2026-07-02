package com.emall.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emall.backend.entity.Ad;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AdMapper extends BaseMapper<Ad> {
    // 继承了 BaseMapper<Ad> 之后，里面什么都不用写，
    // 就能自动拥有 selectList、insert、deleteById 等所有强大方法！
}