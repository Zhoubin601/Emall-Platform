package com.emall.backend.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.emall.backend.entity.*;
import com.emall.backend.mapper.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper itemMapper;

    @Autowired
    private ProductMapper productMapper;

    @Autowired
    private UserCouponMapper userCouponMapper;

    @Autowired
    private SkuMapper skuMapper;

    // ✨ 注入 RedisTemplate，用于下单后清理商品缓存
    @Autowired
    private StringRedisTemplate redisTemplate;

    @DeleteMapping("/{id}")
    public String deleteOrder(@PathVariable Long id) {
        orderMapper.deleteById(id);
        return "订单删除成功";
    }

    @PostMapping("/create")
    @Transactional // 开启事务
    public Long createOrder(@RequestBody Order order) {
        order.setOrderSn(UUID.randomUUID().toString().replace("-", ""));

        // ✨ 修复：时间精确到秒，配合数据库默认设置，防止订单乱序
        order.setCreateTime(LocalDateTime.now().withNano(0));
        orderMapper.insert(order);

        for (OrderItem item : order.getItems()) {
            item.setOrderId(order.getId());
            itemMapper.insert(item);

            // ✨ 核心修复：优先精准扣减具体的 SKU 规格库存
            if (item.getSkuId() != null) {
                Sku targetSku = skuMapper.selectById(item.getSkuId());
                if (targetSku == null) {
                    throw new RuntimeException("商品规格异常，请刷新重试！");
                }
                if (targetSku.getStock() < item.getProductCount()) {
                    throw new RuntimeException("规格 [" + targetSku.getSpecName() + "] 库存不足！");
                }
                // 扣减 SKU 库存
                targetSku.setStock(targetSku.getStock() - item.getProductCount());
                skuMapper.updateById(targetSku);

                // ✨ 核心修复：弃用容易算错的减法！直接触发求和同步，保证主表总库存强一致性！
                syncProductStock(item.getProductId());
            } else {
                // 防护墙：如果前端没有传 skuId，直接抛错阻断交易，防止库存乱套
                throw new RuntimeException("下单失败：缺失规格属性 (skuId)，请重新加购！");
            }
        }

        // 3. 优惠券核销
        if (order.getUserCouponId() != null) {
            UserCoupon userCoupon = userCouponMapper.selectById(order.getUserCouponId());
            if (userCoupon != null && userCoupon.getStatus() == 0) {
                userCoupon.setStatus(1);
                userCoupon.setUseTime(LocalDateTime.now());
                userCouponMapper.updateById(userCoupon);
            }
        }

        // ✨ 核心修复：下单成功后，无情踢掉 Redis 里的旧缓存！强制前端下次拿最新数据！
        redisTemplate.delete("productList");

        return order.getId();
    }

    /**
     * ✨ 新增：核心联动引擎，根据所有有效 SKU 的库存重新求和，并覆盖主表总库存
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

    @GetMapping("/list")
    public Page<Order> getAllOrders(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(required = false) String orderSn,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status) {

        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        if (StringUtils.hasText(orderSn)) queryWrapper.like("order_sn", orderSn);
        if (userId != null) queryWrapper.eq("user_id", userId);
        if (status != null) queryWrapper.eq("status", status);
        queryWrapper.orderByDesc("create_time");

        Page<Order> pageParam = new Page<>(page, size);
        return orderMapper.selectPage(pageParam, queryWrapper);
    }

    @PutMapping("/status/{id}/{status}")
    public String updateStatus(@PathVariable Long id, @PathVariable Integer status) {
        Order order = new Order();
        order.setId(id);
        order.setStatus(status);
        orderMapper.updateById(order);
        return "状态更新成功";
    }

    @GetMapping("/export")
    public void exportOrders(
            @RequestParam(required = false) String orderSn,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) Integer status,
            HttpServletResponse response) throws Exception {

        QueryWrapper<Order> queryWrapper = new QueryWrapper<>();
        if (StringUtils.hasText(orderSn)) queryWrapper.like("order_sn", orderSn);
        if (userId != null) queryWrapper.eq("user_id", userId);
        if (status != null) queryWrapper.eq("status", status);
        queryWrapper.orderByDesc("create_time");

        List<Order> list = orderMapper.selectList(queryWrapper);

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("E-MALL订单报表", "UTF-8").replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), Order.class)
                .autoCloseStream(Boolean.FALSE)
                .sheet("订单列表")
                .doWrite(list);
    }

    @GetMapping("/my")
    public List<Order> getMyOrders(@RequestParam Long userId) {
        // ✨ 修复：增加排序逻辑，确保“我的订单”永远最新的排在最上面
        return orderMapper.selectList(new QueryWrapper<Order>()
                .eq("user_id", userId)
                .orderByDesc("create_time"));
    }

    @GetMapping("/detail/{id}")
    public Order getOrderDetail(@PathVariable Long id) {
        Order order = orderMapper.selectById(id);
        if (order != null) {
            List<OrderItem> items = itemMapper.selectList(
                    new QueryWrapper<OrderItem>().eq("order_id", id)
            );
            order.setItems(items);
        }
        return order;
    }
}