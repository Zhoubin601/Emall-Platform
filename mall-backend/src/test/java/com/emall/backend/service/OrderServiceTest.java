package com.emall.backend.service;

import com.emall.backend.dto.order.CreateOrderItemRequest;
import com.emall.backend.dto.order.CreateOrderRequest;
import com.emall.backend.entity.Order;
import com.emall.backend.entity.OrderItem;
import com.emall.backend.entity.Product;
import com.emall.backend.entity.Sku;
import com.emall.backend.entity.UserCoupon;
import com.emall.backend.entity.Coupon;
import com.emall.backend.mapper.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class OrderServiceTest {
    @BeforeEach
    void initializeTransactionSynchronization() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void clearTransactionSynchronization() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void ignoresClientPricesAndBuildsAuthoritativeSnapshots() {
        OrderMapper orderMapper = mock(OrderMapper.class);
        OrderItemMapper itemMapper = mock(OrderItemMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        SkuMapper skuMapper = mock(SkuMapper.class);
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);
        CouponMapper couponMapper = mock(CouponMapper.class);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(valueOperations.setIfAbsent(anyString(), eq("PROCESSING"), any(Duration.class))).thenReturn(true);

        Sku sku = new Sku();
        sku.setId(3L);
        sku.setProductId(9L);
        sku.setSpecName("官方规格");
        sku.setPrice(new BigDecimal("100.00"));
        sku.setStock(8);
        Product product = new Product();
        product.setId(9L);
        product.setName("真实商品");
        product.setStatus(1);

        when(skuMapper.selectById(3L)).thenReturn(sku);
        when(productMapper.selectById(9L)).thenReturn(product);
        when(skuMapper.deductStock(3L, 2)).thenReturn(1);
        when(skuMapper.selectList(any())).thenReturn(List.of(sku));
        doAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(77L);
            return 1;
        }).when(orderMapper).insert(any(Order.class));

        CreateOrderRequest request = new CreateOrderRequest(
                null, List.of(new CreateOrderItemRequest(3L, 2)));

        OrderService service = new OrderService(
                orderMapper, itemMapper, productMapper, skuMapper,
                userCouponMapper, couponMapper, redisTemplate, new OrderStatusPolicy());
        OrderService.CreatedOrder result = service.createOrder(
                request, 7L, "test-idempotency-key-0001");

        ArgumentCaptor<Order> savedOrder = ArgumentCaptor.forClass(Order.class);
        ArgumentCaptor<OrderItem> savedItem = ArgumentCaptor.forClass(OrderItem.class);
        verify(orderMapper).insert(savedOrder.capture());
        verify(itemMapper).insert(savedItem.capture());

        assertThat(result.orderId()).isEqualTo(77L);
        assertThat(result.totalAmount()).isEqualByComparingTo("210.00");
        assertThat(savedOrder.getValue().getTotalAmount()).isEqualByComparingTo("210.00");
        assertThat(savedItem.getValue().getProductId()).isEqualTo(9L);
        assertThat(savedItem.getValue().getProductName()).isEqualTo("真实商品 (官方规格)");
        assertThat(savedItem.getValue().getProductPrice()).isEqualByComparingTo("100.00");
        verify(skuMapper).deductStock(3L, 2);
    }

    @Test
    void cancellationRestoresInventoryAndReusableCouponOnce() {
        OrderMapper orderMapper = mock(OrderMapper.class);
        OrderItemMapper itemMapper = mock(OrderItemMapper.class);
        ProductMapper productMapper = mock(ProductMapper.class);
        SkuMapper skuMapper = mock(SkuMapper.class);
        UserCouponMapper userCouponMapper = mock(UserCouponMapper.class);
        CouponMapper couponMapper = mock(CouponMapper.class);

        Order order = new Order();
        order.setId(88L);
        order.setUserId(7L);
        order.setUserCouponId(12L);
        order.setStatus(OrderStatusPolicy.PENDING_PAYMENT);
        OrderItem item = new OrderItem();
        item.setSkuId(3L);
        item.setProductId(9L);
        item.setProductCount(2);
        Sku sku = new Sku();
        sku.setId(3L);
        sku.setProductId(9L);
        sku.setStock(10);
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setId(12L);
        userCoupon.setUserId(7L);
        userCoupon.setCouponId(5L);
        userCoupon.setStatus(1);
        Coupon coupon = new Coupon();
        coupon.setId(5L);
        coupon.setEndTime(LocalDateTime.now().plusDays(1));

        when(orderMapper.updateStatusIfCurrent(88L, 0, 4)).thenReturn(1);
        when(itemMapper.selectList(any())).thenReturn(List.of(item));
        when(skuMapper.restoreStock(3L, 2)).thenReturn(1);
        when(skuMapper.selectList(any())).thenReturn(List.of(sku));
        when(userCouponMapper.selectById(12L)).thenReturn(userCoupon);
        when(couponMapper.selectById(5L)).thenReturn(coupon);
        when(userCouponMapper.release(12L, 7L, 0)).thenReturn(1);

        OrderService service = new OrderService(
                orderMapper, itemMapper, productMapper, skuMapper,
                userCouponMapper, couponMapper, mock(StringRedisTemplate.class), new OrderStatusPolicy());
        service.changeStatus(order, OrderStatusPolicy.CANCELLED, false);

        verify(skuMapper).restoreStock(3L, 2);
        verify(userCouponMapper).release(12L, 7L, 0);
    }
}
