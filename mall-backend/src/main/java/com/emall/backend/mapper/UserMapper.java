package com.emall.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emall.backend.entity.User;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {
    // 继承 BaseMapper 之后，这里面空着就行，神功已经大成
}