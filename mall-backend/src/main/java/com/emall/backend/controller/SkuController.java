package com.emall.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.emall.backend.entity.Product;
import com.emall.backend.entity.Sku;
import com.emall.backend.mapper.ProductMapper;
import com.emall.backend.mapper.SkuMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("/api/sku")
public class SkuController {

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private ProductMapper productMapper;

    @GetMapping("/list/{productId}")
    public List<Sku> getSkusByProductId(@PathVariable Long productId) {
        return skuMapper.selectList(new QueryWrapper<Sku>().eq("product_id", productId));
    }

    @PostMapping("/add")
    public String addSku(@RequestBody Sku sku) {
        skuMapper.insert(sku);
        syncProductStock(sku.getProductId()); // ✨ 新增同步
        return "规格添加成功";
    }

    @PutMapping("/update")
    public String updateSku(@RequestBody Sku sku) {
        // 更新前先拿到原本的 productId，防止修改时变动了所属商品
        Sku oldSku = skuMapper.selectById(sku.getId());
        skuMapper.updateById(sku);
        if (oldSku != null) syncProductStock(oldSku.getProductId());
        return "规格更新成功";
    }

    @DeleteMapping("/delete/{id}")
    public String deleteSku(@PathVariable Long id) {
        Sku sku = skuMapper.selectById(id);
        if (sku != null) {
            skuMapper.deleteById(id);
            syncProductStock(sku.getProductId()); // ✨ 删除同步
        }
        return "规格删除成功";
    }

    /**
     * ✨ 核心联动引擎：强制同步主表总库存 = 所有有效 SKU 库存之和
     */
    private void syncProductStock(Long productId) {
        if (productId == null) return;
        List<Sku> skus = skuMapper.selectList(new QueryWrapper<Sku>().eq("product_id", productId));
        int totalStock = skus.stream().mapToInt(s -> s.getStock() != null ? s.getStock() : 0).sum();

        Product p = new Product();
        p.setId(productId);
        p.setStock(totalStock);
        productMapper.updateById(p);
    }
}