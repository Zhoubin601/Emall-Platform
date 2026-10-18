package com.emall.backend.controller;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.emall.backend.entity.*;
import com.emall.backend.dto.order.CreateOrderRequest;
import com.emall.backend.mapper.*;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import com.emall.backend.security.AuthorizationService;
import com.emall.backend.security.AuthenticatedUser;
import com.emall.backend.service.OrderService;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private OrderItemMapper itemMapper;

    @Autowired(required = false)
    private ProductMapper productMapper;

    @Autowired(required = false)
    private SkuMapper skuMapper;

    @Autowired
    private AuthorizationService authorizationService;
    @Autowired
    private OrderService orderService;

    @DeleteMapping("/{id}")
    public String deleteOrder(@PathVariable Long id, Authentication authentication) {
        Order existing = orderMapper.selectById(id);
        if (existing == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在");
        authorizationService.requireOwnerOrAdmin(authentication, existing.getUserId());
        orderService.deleteOrder(existing);
        return "订单删除成功";
    }

    @PostMapping("/create")
    public OrderService.CreatedOrder createOrder(
            @Valid @RequestBody CreateOrderRequest order,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            Authentication authentication) {
        Long userId = authorizationService.currentUser(authentication).id();
        return orderService.createOrder(order, userId, idempotencyKey);
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
        Page<Order> result = orderMapper.selectPage(pageParam, queryWrapper);
        if (result != null) {
            populateOrderItems(result.getRecords());
        }
        return result;
    }

    @PutMapping("/status/{id}/{status}")
    public String updateStatus(@PathVariable Long id, @PathVariable Integer status, Authentication authentication) {
        Order existing = orderMapper.selectById(id);
        if (existing == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "订单不存在");
        authorizationService.requireOwnerOrAdmin(authentication, existing.getUserId());
        AuthenticatedUser currentUser = authorizationService.currentUser(authentication);
        orderService.changeStatus(existing, status, currentUser.isAdmin());
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
    public List<Order> getMyOrders(@RequestParam Long userId, Authentication authentication) {
        userId = authorizationService.requireSelfOrAdmin(authentication, userId);
        // ✨ 修复：增加排序逻辑并批量填充真实订单商品明细
        List<Order> orders = orderMapper.selectList(new QueryWrapper<Order>()
                .eq("user_id", userId)
                .orderByDesc("create_time"));
        populateOrderItems(orders);
        return orders;
    }

    @GetMapping("/detail/{id}")
    public Order getOrderDetail(@PathVariable Long id, Authentication authentication) {
        Order order = orderMapper.selectById(id);
        if (order != null) {
            authorizationService.requireOwnerOrAdmin(authentication, order.getUserId());
            populateOrderItems(List.of(order));
        }
        return order;
    }

    private void populateOrderItems(List<Order> orders) {
        if (orders == null || orders.isEmpty() || itemMapper == null) return;

        List<Long> orderIds = orders.stream()
                .map(Order::getId)
                .filter(Objects::nonNull)
                .toList();
        if (orderIds.isEmpty()) return;

        List<OrderItem> allItems = itemMapper.selectList(
                new QueryWrapper<OrderItem>().in("order_id", orderIds));
        if (allItems == null || allItems.isEmpty()) {
            orders.forEach(order -> order.setItems(List.of()));
            return;
        }

        Map<Long, Product> productMap = Map.of();
        if (productMapper != null) {
            Set<Long> productIds = allItems.stream()
                    .map(OrderItem::getProductId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!productIds.isEmpty()) {
                List<Product> products = productMapper.selectBatchIds(productIds);
                if (products != null) {
                    productMap = products.stream()
                            .filter(p -> p != null && p.getId() != null)
                            .collect(Collectors.toMap(Product::getId, Function.identity(), (a, b) -> a));
                }
            }
        }

        Map<Long, Sku> skuMap = Map.of();
        if (skuMapper != null) {
            Set<Long> skuIds = allItems.stream()
                    .map(OrderItem::getSkuId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            if (!skuIds.isEmpty()) {
                List<Sku> skus = skuMapper.selectBatchIds(skuIds);
                if (skus != null) {
                    skuMap = skus.stream()
                            .filter(s -> s != null && s.getId() != null)
                            .collect(Collectors.toMap(Sku::getId, Function.identity(), (a, b) -> a));
                }
            }
        }

        for (OrderItem item : allItems) {
            Sku sku = item.getSkuId() != null ? skuMap.get(item.getSkuId()) : null;
            Product product = item.getProductId() != null ? productMap.get(item.getProductId()) : null;

            if (sku != null && StringUtils.hasText(sku.getPicUrl())) {
                item.setProductPic(sku.getPicUrl());
            } else if (product != null && StringUtils.hasText(product.getPicUrl())) {
                item.setProductPic(product.getPicUrl());
            }

            if (sku != null && StringUtils.hasText(sku.getSpecName())) {
                item.setSpec(sku.getSpecName());
            } else if (StringUtils.hasText(item.getProductName())) {
                String name = item.getProductName();
                int openIdx = name.lastIndexOf(" (");
                if (openIdx > 0 && name.endsWith(")")) {
                    item.setSpec(name.substring(openIdx + 2, name.length() - 1));
                }
            }
        }

        Map<Long, List<OrderItem>> itemsByOrderId = allItems.stream()
                .filter(item -> item.getOrderId() != null)
                .collect(Collectors.groupingBy(OrderItem::getOrderId));

        for (Order order : orders) {
            List<OrderItem> orderItems = itemsByOrderId.getOrDefault(order.getId(), List.of());
            order.setItems(orderItems);
            if (order.getProductId() == null && !orderItems.isEmpty()) {
                order.setProductId(orderItems.getFirst().getProductId());
            }
        }
    }
}
