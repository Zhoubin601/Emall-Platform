package com.emall.backend.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.emall.backend.entity.Favorite;
import org.apache.ibatis.annotations.Mapper;

/**
 * 商品收藏 Mapper 接口
 * 继承 BaseMapper 后自动拥有完整的 CRUD 能力
 */
@Mapper
public interface FavoriteMapper extends BaseMapper<Favorite> {
    // 基础的增删改查已由 BaseMapper 提供，若无特殊 SQL 需求，此处保持空白即可
}