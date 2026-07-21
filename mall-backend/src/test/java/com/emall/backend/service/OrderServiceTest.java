package com.emall.backend.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.emall.backend.dto.order.CreateOrderItemRequest;
import com.emall.backend.dto.order.CreateOrderRequest;
import com.emall.backend.entity.Coupon;
import com.emall.backend.entity.Order;
import com.emall.backend.entity.OrderItem;
import com.emall.backend.entity.Product;
import com.emall.backend.entity.Sku;
import com.emall.backend.entity.UserCoupon;
import com.emall.backend.mapper.CouponMapper;
import com.emall.backend.mapper.OrderItemMapper;
import com.emall.backend.mapper.OrderMapper;
import com.emall.backend.mapper.ProductMapper;
import com.emall.backend.mapper.SkuMapper;
import com.emall.backend.mapper.UserCouponMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderServiceTest {
    private static final Long USER_ID = 7L;
    private static final String IDEMPOTENCY_KEY = "test-idempotency-key-0001";

    private OrderMapper orderMapper;
    private OrderItemMapper itemMapper;
    private ProductMapper productMapper;
    private SkuMapper skuMapper;
    private UserCouponMapper userCouponMapper;
    private CouponMapper couponMapper;
    private StringRedisTemplate redisTemplate;
    private ValueOperations<String, String> valueOperations;
    private OrderService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        orderMapper = mock(OrderMapper.class);
        itemMapper = mock(OrderItemMapper.class);
        productMapper = mock(ProductMapper.class);
        skuMapper = mock(SkuMapper.class);
        userCouponMapper = mock(UserCouponMapper.class);
        couponMapper = mock(CouponMapper.class);
        redisTemplate = mock(StringRedisTemplate.class);
        valueOperations = mock(ValueOperations.class);
        service = new OrderService(
                orderMapper, itemMapper, productMapper, skuMapper,
                userCouponMapper, couponMapper, redisTemplate, new OrderStatusPolicy());
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    @Test
    void rejectsInvalidIdempotencyKeyBeforeAccessingRedis() {
        assertConflict(HttpStatus.BAD_REQUEST,
                () -> service.createOrder(orderRequest(null), USER_ID, "too-short"));

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void returnsPreviouslyCreatedOrderForCompletedIdempotencyKey() {
        prepareRedis("77", false);
        Order existing = order(77L, USER_ID, OrderStatusPolicy.PENDING_PAYMENT);
        existing.setTotalAmount(new BigDecimal("42.50"));
        when(orderMapper.selectById(77L)).thenReturn(existing);

        OrderService.CreatedOrder result = service.createOrder(
                orderRequest(null), USER_ID, IDEMPOTENCY_KEY);

        assertThat(result.orderId()).isEqualTo(77L);
        assertThat(result.totalAmount()).isEqualByComparingTo("42.50");
        verify(orderMapper, never()).insert(any(Order.class));
        verify(valueOperations, never()).setIfAbsent(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void rejectsCorruptedIdempotencyRecordBelongingToAnotherUser() {
        prepareRedis("77", false);
        when(orderMapper.selectById(77L))
                .thenReturn(order(77L, 999L, OrderStatusPolicy.PENDING_PAYMENT));

        assertConflict(HttpStatus.CONFLICT,
                () -> service.createOrder(orderRequest(null), USER_ID, IDEMPOTENCY_KEY));

        verify(redisTemplate).delete("order:idempotency:7:" + IDEMPOTENCY_KEY);
        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    void rejectsRequestWhileSameIdempotencyKeyIsProcessing() {
        prepareRedis("PROCESSING", false);

        assertConflict(HttpStatus.CONFLICT,
                () -> service.createOrder(orderRequest(null), USER_ID, IDEMPOTENCY_KEY));

        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    void ignoresClientPricesAndBuildsAuthoritativeSnapshots() {
        prepareCreation(new BigDecimal("100.00"), 8);

        OrderService.CreatedOrder result = service.createOrder(
                orderRequest(null), USER_ID, IDEMPOTENCY_KEY);

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

        TransactionSynchronization synchronization =
                TransactionSynchronizationManager.getSynchronizations().getFirst();
        synchronization.afterCommit();
        verify(valueOperations).set(
                "order:idempotency:7:" + IDEMPOTENCY_KEY,
                "77",
                Duration.ofHours(24));

    }

    @Test
    void usesActivePromotionAndConsumesEligibleCoupon() {
        CreationFixture fixture = prepareCreation(new BigDecimal("100.00"), 8);
        fixture.product().setPromoPrice(new BigDecimal("80.00"));
        fixture.product().setPromoStartTime(LocalDateTime.now().minusHours(1));
        fixture.product().setPromoEndTime(LocalDateTime.now().plusHours(1));
        prepareCoupon(12L, USER_ID, 0, new BigDecimal("150.00"),
                new BigDecimal("20.00"), LocalDateTime.now().plusDays(1));
        when(userCouponMapper.consume(12L, USER_ID)).thenReturn(1);

        OrderService.CreatedOrder result = service.createOrder(
                orderRequest(12L), USER_ID, IDEMPOTENCY_KEY);

        assertThat(result.totalAmount()).isEqualByComparingTo("150.00");
        verify(userCouponMapper).consume(12L, USER_ID);
    }

    @Test
    void rejectsCouponOwnedByAnotherUserBeforeChangingStock() {
        prepareCreation(new BigDecimal("100.00"), 8);
        prepareCoupon(12L, 99L, 0, new BigDecimal("100.00"),
                new BigDecimal("20.00"), LocalDateTime.now().plusDays(1));

        assertConflict(HttpStatus.CONFLICT,
                () -> service.createOrder(orderRequest(12L), USER_ID, IDEMPOTENCY_KEY));

        verify(skuMapper, never()).deductStock(any(), any());
        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    void rejectsCouponWhenSubtotalIsBelowMinimum() {
        prepareCreation(new BigDecimal("100.00"), 8);
        prepareCoupon(12L, USER_ID, 0, new BigDecimal("250.00"),
                new BigDecimal("20.00"), LocalDateTime.now().plusDays(1));

        assertConflict(HttpStatus.CONFLICT,
                () -> service.createOrder(orderRequest(12L), USER_ID, IDEMPOTENCY_KEY));

        verify(userCouponMapper, never()).consume(any(), any());
        verify(skuMapper, never()).deductStock(any(), any());
    }

    @Test
    void rejectsCouponLostToConcurrentConsumption() {
        prepareCreation(new BigDecimal("100.00"), 8);
        prepareCoupon(12L, USER_ID, 0, new BigDecimal("100.00"),
                new BigDecimal("20.00"), LocalDateTime.now().plusDays(1));
        when(userCouponMapper.consume(12L, USER_ID)).thenReturn(0);

        assertConflict(HttpStatus.CONFLICT,
                () -> service.createOrder(orderRequest(12L), USER_ID, IDEMPOTENCY_KEY));

        verify(skuMapper, never()).deductStock(any(), any());
        verify(orderMapper, never()).insert(any(Order.class));
    }

    @Test
    void rejectsOrderWhenAtomicStockDeductionFails() {
        prepareCreation(new BigDecimal("100.00"), 1);
        when(skuMapper.deductStock(3L, 2)).thenReturn(0);

        assertConflict(HttpStatus.CONFLICT,
                () -> service.createOrder(orderRequest(null), USER_ID, IDEMPOTENCY_KEY));

        verify(orderMapper, never()).insert(any(Order.class));
        TransactionSynchronization synchronization =
                TransactionSynchronizationManager.getSynchronizations().getFirst();
        synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK);
        verify(redisTemplate).delete("order:idempotency:7:" + IDEMPOTENCY_KEY);
    }

    @Test
    void cancellationRestoresInventoryAndReusableCouponOnce() {
        Order order = order(88L, USER_ID, OrderStatusPolicy.PENDING_PAYMENT);
        order.setUserCouponId(12L);
        prepareCancellation(order, LocalDateTime.now().plusDays(1), 0);

        service.changeStatus(order, OrderStatusPolicy.CANCELLED, false);

        verify(skuMapper).restoreStock(3L, 2);
        verify(userCouponMapper).release(12L, USER_ID, 0);
    }

    @Test
    void cancellationMarksExpiredCouponInsteadOfMakingItReusable() {
        Order order = order(88L, USER_ID, OrderStatusPolicy.PENDING_PAYMENT);
        order.setUserCouponId(12L);
        prepareCancellation(order, LocalDateTime.now().minusDays(1), 2);

        service.changeStatus(order, OrderStatusPolicy.CANCELLED, false);

        verify(userCouponMapper).release(12L, USER_ID, 2);
    }

    @Test
    void concurrentStatusChangeDoesNotRestoreInventory() {
        Order order = order(88L, USER_ID, OrderStatusPolicy.PENDING_PAYMENT);
        when(orderMapper.updateStatusIfCurrent(
                88L, OrderStatusPolicy.PENDING_PAYMENT, OrderStatusPolicy.CANCELLED))
                .thenReturn(0);

        assertConflict(HttpStatus.CONFLICT,
                () -> service.changeStatus(order, OrderStatusPolicy.CANCELLED, false));

        verify(itemMapper, never()).selectList(any());
        verify(skuMapper, never()).restoreStock(any(), any());
    }

    @Test
    void deletesOnlyTerminalOrders() {
        Order completed = order(88L, USER_ID, OrderStatusPolicy.COMPLETED);
        service.deleteOrder(completed);

        verify(itemMapper).delete(any(Wrapper.class));
        verify(orderMapper).deleteById(88L);

        Order paid = order(89L, USER_ID, OrderStatusPolicy.PAID);
        assertConflict(HttpStatus.CONFLICT, () -> service.deleteOrder(paid));
        verify(orderMapper, never()).deleteById(89L);
    }

    private CreationFixture prepareCreation(BigDecimal skuPrice, int stock) {
        prepareRedis(null, true);
        Sku sku = new Sku();
        sku.setId(3L);
        sku.setProductId(9L);
        sku.setSpecName("官方规格");
        sku.setPrice(skuPrice);
        sku.setStock(stock);
        Product product = new Product();
        product.setId(9L);
        product.setName("真实商品");
        product.setStatus(1);

        when(skuMapper.selectById(3L)).thenReturn(sku);
        when(productMapper.selectById(9L)).thenReturn(product);
        when(skuMapper.deductStock(3L, 2)).thenReturn(1);
        when(skuMapper.selectList(any())).thenReturn(List.of(sku));
        when(orderMapper.insert(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(77L);
            return 1;
        });
        return new CreationFixture(product);
    }

    private void prepareRedis(String existingValue, boolean acquired) {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(existingValue);
        when(valueOperations.setIfAbsent(anyString(), eq("PROCESSING"), any(Duration.class)))
                .thenReturn(acquired);
    }

    private void prepareCoupon(
            Long userCouponId,
            Long ownerId,
            int status,
            BigDecimal minimum,
            BigDecimal discount,
            LocalDateTime endTime) {
        UserCoupon userCoupon = new UserCoupon();
        userCoupon.setId(userCouponId);
        userCoupon.setUserId(ownerId);
        userCoupon.setCouponId(5L);
        userCoupon.setStatus(status);
        Coupon coupon = new Coupon();
        coupon.setId(5L);
        coupon.setMinAmount(minimum);
        coupon.setDiscountAmount(discount);
        coupon.setEndTime(endTime);
        when(userCouponMapper.selectById(userCouponId)).thenReturn(userCoupon);
        when(couponMapper.selectById(5L)).thenReturn(coupon);
    }

    private void prepareCancellation(Order order, LocalDateTime couponEndTime, int releaseStatus) {
        OrderItem item = new OrderItem();
        item.setSkuId(3L);
        item.setProductId(9L);
        item.setProductCount(2);
        Sku sku = new Sku();
        sku.setId(3L);
        sku.setProductId(9L);
        sku.setStock(10);

        when(orderMapper.updateStatusIfCurrent(
                order.getId(), order.getStatus(), OrderStatusPolicy.CANCELLED)).thenReturn(1);
        when(itemMapper.selectList(any())).thenReturn(List.of(item));
        when(skuMapper.restoreStock(3L, 2)).thenReturn(1);
        when(skuMapper.selectList(any())).thenReturn(List.of(sku));
        prepareCoupon(order.getUserCouponId(), USER_ID, 1,
                BigDecimal.ZERO, BigDecimal.TEN, couponEndTime);
        when(userCouponMapper.release(order.getUserCouponId(), USER_ID, releaseStatus)).thenReturn(1);
    }

    private CreateOrderRequest orderRequest(Long userCouponId) {
        return new CreateOrderRequest(
                userCouponId, List.of(new CreateOrderItemRequest(3L, 2)));
    }

    private Order order(Long id, Long userId, int status) {
        Order order = new Order();
        order.setId(id);
        order.setUserId(userId);
        order.setStatus(status);
        return order;
    }

    private void assertConflict(HttpStatus expectedStatus, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(expectedStatus));
    }

    private record CreationFixture(Product product) {
    }
}
