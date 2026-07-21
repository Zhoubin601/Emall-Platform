package com.emall.backend.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.emall.backend.entity.HotSearch;
import com.emall.backend.entity.Product;
import com.emall.backend.entity.ProductExcelDTO;
import com.emall.backend.entity.Sku;
import com.emall.backend.mapper.HotSearchMapper;
import com.emall.backend.mapper.ProductMapper;
import com.emall.backend.mapper.SkuMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import com.emall.backend.entity.Category;
import com.emall.backend.mapper.CategoryMapper;
@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private HotSearchMapper hotSearchMapper;

    @Autowired
    private SkuMapper skuMapper;


    // ✨ 1. 新增：注入分类的 Mapper
    @Autowired
    private CategoryMapper categoryMapper;

    @Cacheable(value = "productList", key = "#keyword + '_' + #categoryId + '_' + #sortBy")
    @GetMapping("/list")
    public List<Product> getProductList(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String sortBy
    ) {
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        wrapper.eq("status", 1);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like("name", keyword).or().like("description", keyword));
        }

        // ✨ 2. 核心修复：向下穿透查询子类
        if (categoryId != null && categoryId > 0) {
            // 去数据库查一下，这个 ID 下面有没有子分类 (parent_id = 它的)
            List<Category> subCategories = categoryMapper.selectList(new QueryWrapper<Category>().eq("parent_id", categoryId));

            if (subCategories != null && !subCategories.isEmpty()) {
                // 如果有子分类，说明点击的是一级大类
                List<Long> ids = subCategories.stream().map(Category::getId).collect(Collectors.toList());
                ids.add(categoryId); // 把大类自己的 ID 也加上，防止有商品直接挂在大类上
                // 使用 SQL 的 IN 语法，查询包含在这些 ID 里的所有商品
                wrapper.in("category_id", ids);
            } else {
                // 如果没有子分类，说明点击的就是最底层的子类，直接用 eq 等于就行
                wrapper.eq("category_id", categoryId);
            }
        }

        if ("price_asc".equals(sortBy)) wrapper.orderByAsc("price");
        else if ("price_desc".equals(sortBy)) wrapper.orderByDesc("price");
        else if ("sales".equals(sortBy)) wrapper.orderByDesc("sales");
        else wrapper.orderByDesc("create_time");

        return productMapper.selectList(wrapper);
    }

    @CacheEvict(value = "productList", allEntries = true)
    @PostMapping("/add")
    public String addProduct(@RequestBody Product product) {
        product.setStock(0); // ✨ 新增商品强制 0 库存，倒逼去配置规格
        productMapper.insert(product);
        return "新增成功";
    }

    @CacheEvict(value = "productList", allEntries = true)
    @PutMapping("/update")
    public String updateProduct(@RequestBody Product product) {
        product.setStock(null); // ✨ 核心防爆逻辑：防止普通编辑覆盖总库存
        productMapper.updateById(product);
        return "更新成功";
    }

    @DeleteMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id) {
        productMapper.deleteById(id);
        return "删除成功";
    }

    @GetMapping("/detail/{id}")
    public Product getProductDetail(@PathVariable Long id) {
        return productMapper.selectById(id);
    }

    @PostMapping("/searchRecord")
    public String recordSearch(@RequestParam String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) return "empty";
        String kw = keyword.trim();
        HotSearch exist = hotSearchMapper.selectOne(new QueryWrapper<HotSearch>().eq("keyword", kw));
        if (exist != null) {
            exist.setSearchCount(exist.getSearchCount() + 1);
            hotSearchMapper.updateById(exist);
        } else {
            HotSearch hs = new HotSearch();
            hs.setKeyword(kw);
            hs.setSearchCount(1);
            hotSearchMapper.insert(hs);
        }
        return "recorded";
    }

    @GetMapping("/hotSearches")
    public List<HotSearch> getHotSearches() {
        return hotSearchMapper.selectList(new QueryWrapper<HotSearch>().orderByDesc("search_count").last("LIMIT 6"));
    }

    @GetMapping("/skus/{productId}")
    public List<Sku> getProductSkus(@PathVariable Long productId) {
        return skuMapper.selectList(new QueryWrapper<Sku>().eq("product_id", productId));
    }


    // ================= ✨ 全量平铺级联：导入导出重构模块 =================

    @GetMapping("/export")
    public void exportProducts(HttpServletResponse response) throws Exception {
        // 1. 获取所有商品和规格
        List<Product> products = productMapper.selectList(null);
        List<Sku> skus = skuMapper.selectList(null);

        // 2. 将 Sku 按照商品 ID 分组
        Map<Long, List<Sku>> skuMap = skus.stream().collect(Collectors.groupingBy(Sku::getProductId));
        List<ProductExcelDTO> exportList = new ArrayList<>();

        for (Product p : products) {
            List<Sku> pSkus = skuMap.get(p.getId());
            // 如果这个商品没有规格，只导出商品基础信息
            if (pSkus == null || pSkus.isEmpty()) {
                ProductExcelDTO dto = new ProductExcelDTO();
                dto.setProductId(p.getId());
                dto.setProductName(p.getName());
                dto.setCategoryId(p.getCategoryId());
                dto.setProductPrice(p.getPrice());
                dto.setStatus(p.getStatus());
                dto.setDescription(p.getDescription());
                exportList.add(dto);
            } else {
                // 如果有规格，商品信息每行重复平铺，叠加右侧规格信息
                for (Sku s : pSkus) {
                    ProductExcelDTO dto = new ProductExcelDTO();
                    dto.setProductId(p.getId());
                    dto.setProductName(p.getName());
                    dto.setCategoryId(p.getCategoryId());
                    dto.setProductPrice(p.getPrice());
                    dto.setStatus(p.getStatus());
                    dto.setDescription(p.getDescription());

                    dto.setSkuId(s.getId());
                    dto.setSpecName(s.getSpecName());
                    dto.setSkuPrice(s.getPrice());
                    dto.setSkuStock(s.getStock());
                    dto.setSkuPicUrl(s.getPicUrl());
                    exportList.add(dto);
                }
            }
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("E-MALL全量商品与规格总表", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), ProductExcelDTO.class)
                .autoCloseStream(Boolean.FALSE).sheet("全量数据").doWrite(exportList);
    }

    @PostMapping("/import")
    @Transactional(rollbackFor = Exception.class) // ✨ 遇到任何错误立即回滚数据库！
    public String importProducts(@RequestParam("file") MultipartFile file) throws Exception {
        // 同步读取所有 Excel 行数据
        List<ProductExcelDTO> list = EasyExcel.read(file.getInputStream())
                .head(ProductExcelDTO.class).sheet().doReadSync();

        int pSuccess = 0;
        int sSuccess = 0;

        // ✨ 核心折叠逻辑：按照 商品ID（优先）或 商品名称 分组
        Map<String, List<ProductExcelDTO>> groupedData = list.stream()
                .filter(item -> item.getProductName() != null && !item.getProductName().trim().isEmpty())
                .collect(Collectors.groupingBy(item ->
                        item.getProductId() != null ? "ID_" + item.getProductId() : "NAME_" + item.getProductName()
                ));

        for (Map.Entry<String, List<ProductExcelDTO>> entry : groupedData.entrySet()) {
            List<ProductExcelDTO> items = entry.getValue();
            ProductExcelDTO first = items.get(0); // 取第一行作为主商品信息

            // ============ 1. 处理商品主表 (防重复覆盖) ============
            Product product = null;
            if (first.getProductId() != null) {
                product = productMapper.selectById(first.getProductId());
            }
            if (product == null) {
                product = productMapper.selectOne(new QueryWrapper<Product>().eq("name", first.getProductName()));
            }

            if (product == null) {
                // 不存在，走新增
                product = new Product();
                product.setName(first.getProductName());
                product.setCategoryId(first.getCategoryId());
                product.setPrice(first.getProductPrice());
                product.setStatus(first.getStatus() != null ? first.getStatus() : 1);
                product.setDescription(first.getDescription());
                product.setStock(0); // 暂定为 0，后面触发统计
                productMapper.insert(product);
                pSuccess++;
            } else {
                // 存在，走更新
                product.setName(first.getProductName());
                if(first.getCategoryId() != null) product.setCategoryId(first.getCategoryId());
                if(first.getProductPrice() != null) product.setPrice(first.getProductPrice());
                if(first.getStatus() != null) product.setStatus(first.getStatus());
                if(first.getDescription() != null) product.setDescription(first.getDescription());
                product.setStock(null); // ✨ 防护墙：屏蔽外界总库存，只允许后面联动更新！
                productMapper.updateById(product);
                pSuccess++;
            }

            // ============ 2. 处理该商品下的规格表 ============
            for (ProductExcelDTO item : items) {
                if (item.getSpecName() == null || item.getSpecName().trim().isEmpty()) continue;

                Sku sku = null;
                if (item.getSkuId() != null) {
                    sku = skuMapper.selectById(item.getSkuId());
                }
                if (sku == null) {
                    sku = skuMapper.selectOne(new QueryWrapper<Sku>()
                            .eq("product_id", product.getId())
                            .eq("spec_name", item.getSpecName()));
                }

                if (sku == null) {
                    // 新增规格
                    sku = new Sku();
                    sku.setProductId(product.getId());
                    sku.setSpecName(item.getSpecName());
                    sku.setPrice(item.getSkuPrice() != null ? item.getSkuPrice() : product.getPrice());
                    sku.setStock(item.getSkuStock() != null ? item.getSkuStock() : 0);
                    sku.setPicUrl(item.getSkuPicUrl());
                    skuMapper.insert(sku);
                    sSuccess++;
                } else {
                    // 更新规格
                    if(item.getSkuPrice() != null) sku.setPrice(item.getSkuPrice());
                    if(item.getSkuStock() != null) sku.setStock(item.getSkuStock());
                    if(item.getSkuPicUrl() != null) sku.setPicUrl(item.getSkuPicUrl());
                    skuMapper.updateById(sku);
                    sSuccess++;
                }
            }

            // ============ 3. ✨ 核心联动：强制同步该商品的总库存 ============
            syncProductStock(product.getId());
        }

        return "全量同步完成！成功处理商品主体 " + pSuccess + " 个，处理下辖规格 " + sSuccess + " 条。";
    }
    // ================= ✨ 终极大招：全库库存一键强力校对 =================
    @CacheEvict(value = "productList", allEntries = true) // 执行前先暴力清空所有商品缓存
    @GetMapping("/sync-inventory")
    public String syncAllInventory() {
        // 1. 查出数据库里所有的商品
        List<Product> products = productMapper.selectList(null);

        // 2. 挨个强制重新求和对账
        for (Product p : products) {
            syncProductStock(p.getId()); // 调用你底层的求和逻辑
        }

        return "全库库存强力校对完成！缓存已清空！";
    }
    /**
     * 库存联动引擎：强制同步主表总库存 = 所有有效 SKU 库存之和
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
