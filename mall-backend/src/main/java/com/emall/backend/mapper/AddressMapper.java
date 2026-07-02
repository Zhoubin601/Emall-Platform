package com.emall.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emall.backend.entity.Address;
import org.apache.ibatis.annotations.Mapper;

/**
 * 收货地址 Mapper 接口
 * 只要继承了 BaseMapper<Address>，所有的 CRUD 功能就都齐了
 */
@Mapper
public interface AddressMapper extends BaseMapper<Address> {
    // 这里暂时保持空白即可，MyBatis-Plus 会自动帮你实现基础功能
}