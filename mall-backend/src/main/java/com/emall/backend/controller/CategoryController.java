package com.emall.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.emall.backend.entity.Category;
import com.emall.backend.mapper.CategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/category")
public class CategoryController {

    @Autowired
    private CategoryMapper categoryMapper;

    @GetMapping("/list")
    public List<Category> list() {
        // ✨ 修改：按 level 升序排列，避免查询不存在的 sort 字段
        return categoryMapper.selectList(new QueryWrapper<Category>().orderByAsc("level"));
    }

    @PostMapping("/add")
    public String add(@RequestBody Category category) {
        // 兜底默认值
        if (category.getParentId() == null) category.setParentId(0L);
        if (category.getLevel() == null) category.setLevel(1);

        categoryMapper.insert(category);
        return "分类新增成功";
    }

    @PutMapping("/update")
    public String update(@RequestBody Category category) {
        categoryMapper.updateById(category);
        return "分类修改成功";
    }

    @DeleteMapping("/delete/{id}")
    public String delete(@PathVariable Long id) {
        categoryMapper.deleteById(id);
        return "分类删除成功";
    }
}