package com.emall.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.emall.backend.entity.Favorite;
import com.emall.backend.entity.Product;
import com.emall.backend.mapper.FavoriteMapper;
import com.emall.backend.mapper.ProductMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/favorite")
public class FavoriteController {
    @Autowired private FavoriteMapper favoriteMapper;
    @Autowired
    private ProductMapper productMapper;

    // 切换收藏状态：收藏 <-> 取消
    @PostMapping("/toggle")
    public String toggle(@RequestBody Favorite favorite) {
        QueryWrapper<Favorite> wrapper = new QueryWrapper<Favorite>()
                .eq("user_id", favorite.getUserId())
                .eq("product_id", favorite.getProductId());
        Favorite exist = favoriteMapper.selectOne(wrapper);
        if (exist != null) {
            favoriteMapper.deleteById(exist.getId());
            return "uncollected"; // 已取消收藏
        } else {
            favoriteMapper.insert(favorite);
            return "collected"; // 已收藏
        }
    }

    // 获取当前用户的所有收藏商品详情
    @GetMapping("/list")
    public List<Product> list(@RequestParam Long userId) {
        // 这里采用简单的两步查询，也可以写 Join 语句
        List<Favorite> favorites = favoriteMapper.selectList(new QueryWrapper<Favorite>().eq("user_id", userId));
        if (favorites.isEmpty()) return new ArrayList<>();
        List<Long> productIds = favorites.stream().map(Favorite::getProductId).collect(Collectors.toList());
        return productMapper.selectBatchIds(productIds);
    }

    // 检查某个商品是否已被当前用户收藏
    @GetMapping("/status")
    public Boolean checkStatus(@RequestParam Long userId, @RequestParam Long productId) {
        return favoriteMapper.selectCount(new QueryWrapper<Favorite>()
                .eq("user_id", userId).eq("product_id", productId)) > 0;
    }
}