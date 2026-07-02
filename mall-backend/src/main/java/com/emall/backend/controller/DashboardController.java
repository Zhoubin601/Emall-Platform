package com.emall.backend.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.emall.backend.entity.Order;
import com.emall.backend.entity.Product;
import com.emall.backend.entity.User;
import com.emall.backend.mapper.OrderMapper;
import com.emall.backend.mapper.ProductMapper;
import com.emall.backend.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@CrossOrigin
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ProductMapper productMapper;

    @GetMapping("/data")
    public Map<String, Object> getDashboardData() {
        Map<String, Object> result = new HashMap<>();

        // ================= 1. 核心指标统计 =================
        // 总买家数 (role = 0)
        long totalUsers = userMapper.selectCount(new QueryWrapper<User>().eq("role", 0));

        List<Order> allOrders = orderMapper.selectList(null);
        long totalOrders = allOrders.size();

        BigDecimal totalSales = BigDecimal.ZERO;
        BigDecimal todaySales = BigDecimal.ZERO;
        LocalDate today = LocalDate.now();

        for (Order o : allOrders) {
            // 排除已取消(4)和已退款(5)的废单
            if (o.getStatus() != null && o.getStatus() != 4 && o.getStatus() != 5) {
                totalSales = totalSales.add(o.getTotalAmount());
                // 如果是今天的订单
                if (o.getCreateTime() != null && o.getCreateTime().toLocalDate().isEqual(today)) {
                    todaySales = todaySales.add(o.getTotalAmount());
                }
            }
        }

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalUsers", totalUsers);
        metrics.put("totalOrders", totalOrders);
        metrics.put("totalSales", totalSales);
        metrics.put("todaySales", todaySales);
        result.put("metrics", metrics);

        // ================= 2. 销量趋势折线图 (近7天推演) =================
        List<String> dates = new ArrayList<>();
        List<BigDecimal> salesTrend = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-dd");

        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            dates.add(date.format(formatter));

            // 算出当天的有效销售额
            BigDecimal daySale = allOrders.stream()
                    .filter(o -> o.getStatus() != null && o.getStatus() != 4 && o.getStatus() != 5)
                    .filter(o -> o.getCreateTime() != null && o.getCreateTime().toLocalDate().isEqual(date))
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            salesTrend.add(daySale);
        }
        Map<String, Object> trend = new HashMap<>();
        trend.put("dates", dates);
        trend.put("sales", salesTrend);
        result.put("trend", trend);

        // ================= 3. 订单状态分布饼图 =================
        long status0 = allOrders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 0).count();
        long status1 = allOrders.stream().filter(o -> o.getStatus() != null && (o.getStatus() == 1 || o.getStatus() == 2)).count(); // 待发货+待收货 = 履约中
        long status3 = allOrders.stream().filter(o -> o.getStatus() != null && o.getStatus() == 3).count();
        long status4 = allOrders.stream().filter(o -> o.getStatus() != null && (o.getStatus() == 4 || o.getStatus() == 5 || o.getStatus() == 6)).count(); // 取消/退款

        List<Map<String, Object>> statusData = new ArrayList<>();
        statusData.add(Map.of("name", "待支付", "value", status0));
        statusData.add(Map.of("name", "履约中", "value", status1));
        statusData.add(Map.of("name", "已完成", "value", status3));
        statusData.add(Map.of("name", "取消/退款", "value", status4));
        result.put("statusData", statusData);

        // ================= 4. 热销商品 TOP 5 排行榜 =================
        List<Product> topProducts = productMapper.selectList(new QueryWrapper<Product>().orderByDesc("sales").last("LIMIT 5"));
        List<String> rankNames = new ArrayList<>();
        List<Integer> rankSales = new ArrayList<>();

        for (Product p : topProducts) {
            rankNames.add(p.getName());
            rankSales.add(p.getSales() == null ? 0 : p.getSales());
        }

        // 由于 ECharts 横向柱状图从下往上画，我们需要反转一下数组让销量最高的置顶
        Collections.reverse(rankNames);
        Collections.reverse(rankSales);

        Map<String, Object> rank = new HashMap<>();
        rank.put("names", rankNames);
        rank.put("sales", rankSales);
        result.put("rank", rank);

        return result;
    }
}