package com.emall.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.emall.backend.entity.Ad;
import com.emall.backend.mapper.AdMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ad")
public class AdController {

    @Autowired
    private AdMapper adMapper;

    // 管理端：获取所有广告列表（支持搜索标题）
    @GetMapping("/list")
    public List<Ad> getAdList(@RequestParam(required = false) String title) {
        QueryWrapper<Ad> qw = new QueryWrapper<>();
        if (StringUtils.hasText(title)) qw.like("title", title);
        qw.orderByAsc("sort");
        return adMapper.selectList(qw);
    }

    // 买家端：获取展示中的广告
    @GetMapping("/active")
    public List<Ad> getActiveAds() {
        return adMapper.selectList(new QueryWrapper<Ad>().eq("status", 1).orderByAsc("sort"));
    }

    @PostMapping("/add")
    public String addAd(@RequestBody Ad ad) {
        adMapper.insert(ad);
        return "添加成功";
    }

    @PutMapping("/update")
    public String updateAd(@RequestBody Ad ad) {
        adMapper.updateById(ad);
        return "更新成功";
    }

    @DeleteMapping("/delete/{id}")
    public String deleteAd(@PathVariable Long id) {
        adMapper.deleteById(id);
        return "删除成功";
    }
}
