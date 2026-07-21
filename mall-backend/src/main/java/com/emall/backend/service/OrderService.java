package com.emall.backend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.emall.backend.entity.*;
import com.emall.backend.dto.order.CreateOrderItemRequest;
import com.emall.backend.dto.order.CreateOrderRequest;
import com.emall.backend.mapper.*;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class OrderService {
    private static final BigDecimal SHIPPING_FEE = new BigDecimal("10.00");
    private static final String IDEMPOTENCY_PREFIX = "order:idempotency:";

    private final OrderMapper orderMapper;
    private final OrderItemMapper itemMapper;
    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;
    private final UserCouponMapper userCouponMapper;
    private final CouponMapper couponMapper;
    private final StringRedisTemplate redisTemplate;
    private final OrderStatusPolicy statusPolicy;

    public OrderService(
            OrderMapper orderMapper,
            OrderItemMapper itemMapper,
            ProductMapper productMapper,
            SkuMapper skuMapper,
            UserCouponMapper userCouponMapper,
            CouponMapper couponMapper,
            StringRedisTemplate redisTemplate,
            OrderStatusPolicy statusPolicy) {
        this.orderMapper = orderMapper;
        this.itemMapper = itemMapper;
        this.productMapper = productMapper;
        this.skuMapper = skuMapper;
        this.userCouponMapper = userCouponMapper;
        this.couponMapper = couponMapper;
        this.redisTemplate = redisTemplate;
        this.statusPolicy = statusPolicy;
    }

    @Transactional
    @CacheEvict(value = "productList", allEntries = true)
    public CreatedOrder createOrder(CreateOrderRequest request, Long userId, String idempotencyKey) {
        validateIdempotencyKey(idempotencyKey);
        String redisKey = IDEMPOTENCY_PREFIX + userId + ":" + idempotencyKey;
        String existing = redisTemplate.opsForValue().get(redisKey);
        if (existing != null && !"PROCESSING".equals(existing)) {
            Order existingOrder = orderMapper.selectById(Long.valueOf(existing));
            if (existingOrder == null || !Objects.equals(existingOrder.getUserId(), userId)) {
                redisTemplate.delete(redisKey);
                throw new ResponseStatusException(HttpStatus.CONFLICT, "幂等记录异常，请重新提交");
            }
            return new CreatedOrder(existingOrder.getId(), existingOrder.getTotalAmount());
        }
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(redisKey, "PROCESSING", Duration.ofMinutes(10));
        if (!Boolean.TRUE.equals(acquired)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "订单正在处理中，请勿重复提交");
        }

        AtomicReference<Long> committedOrderId = new AtomicReference<>();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                redisTemplate.opsForValue().set(redisKey, String.valueOf(committedOrderId.get()), Duration.ofHours(24));
            }

            @Override
            public void afterCompletion(int status) {
                if (status != TransactionSynchronization.STATUS_COMMITTED) redisTemplate.delete(redisKey);
            }
        });

        LocalDateTime now = LocalDateTime.now().withNano(0);
        List<OrderItem> snapshots = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        for (CreateOrderItemRequest submittedItem : request.items()) {
            Sku sku = skuMapper.selectById(submittedItem.skuId());
            if (sku == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "商品规格不存在");
            Product product = productMapper.selectById(sku.getProductId());
            if (product == null || product.getStatus() == null || product.getStatus() != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "商品已下架或不存在");
            }

            BigDecimal unitPrice = effectivePrice(product, sku, now);
            OrderItem snapshot = new OrderItem();
            snapshot.setProductId(product.getId());
            snapshot.setSkuId(sku.getId());
            snapshot.setProductName(product.getName() + " (" + sku.getSpecName() + ")");
            snapshot.setProductPrice(unitPrice);
            snapshot.setProductCount(submittedItem.productCount());
            snapshots.add(snapshot);
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(submittedItem.productCount())));
        }

        BigDecimal discount = validateAndConsumeCoupon(request.userCouponId(), userId, subtotal, now);
        BigDecimal total = subtotal.add(SHIPPING_FEE).subtract(discount)
                .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        Set<Long> affectedProducts = new HashSet<>();
        for (OrderItem item : snapshots) {
            if (skuMapper.deductStock(item.getSkuId(), item.getProductCount()) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "库存不足，请刷新后重试");
            }
            affectedProducts.add(item.getProductId());
        }

        Order order = new Order();
        order.setUserId(userId);
        order.setUserCouponId(request.userCouponId());
        order.setOrderSn(UUID.randomUUID().toString().replace("-", ""));
        order.setTotalAmount(total);
        order.setStatus(OrderStatusPolicy.PENDING_PAYMENT);
        order.setProductId(snapshots.getFirst().getProductId());
        order.setCommentStatus(0);
        order.setCreateTime(now);
        orderMapper.insert(order);

        for (OrderItem item : snapshots) {
            item.setOrderId(order.getId());
            itemMapper.insert(item);
        }
        affectedProducts.forEach(this::syncProductStock);
        committedOrderId.set(order.getId());
        return new CreatedOrder(order.getId(), order.getTotalAmount());
    }

    @Transactional
    @CacheEvict(value = "productList", allEntries = true)
    public void changeStatus(Order order, int targetStatus, boolean admin) {
        int currentStatus = order.getStatus();
        statusPolicy.validateTransition(currentStatus, targetStatus, admin);
        if (orderMapper.updateStatusIfCurrent(order.getId(), currentStatus, targetStatus) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "订单状态已变化，请刷新后重试");
        }
        if (statusPolicy.shouldRestoreInventory(currentStatus, targetStatus)) {
            restoreInventory(order.getId());
            releaseCoupon(order);
        }
    }

    @Transactional
    public void deleteOrder(Order order) {
        if (!statusPolicy.canDelete(order.getStatus())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "只能删除已完成、已取消或已退款的订单");
        }
        itemMapper.delete(new QueryWrapper<OrderItem>().eq("order_id", order.getId()));
        orderMapper.deleteById(order.getId());
    }

    private BigDecimal validateAndConsumeCoupon(Long userCouponId, Long userId, BigDecimal subtotal, LocalDateTime now) {
        if (userCouponId == null) return BigDecimal.ZERO;
        UserCoupon userCoupon = userCouponMapper.selectById(userCouponId);
        if (userCoupon == null || !Objects.equals(userCoupon.getUserId(), userId)
                || !Objects.equals(userCoupon.getStatus(), 0)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "优惠券不可用");
        }
        Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
        if (coupon == null || coupon.getEndTime() == null || now.isAfter(coupon.getEndTime())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "优惠券已过期");
        }
        if (coupon.getMinAmount() == null || subtotal.compareTo(coupon.getMinAmount()) < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "订单金额未达到优惠券使用门槛");
        }
        if (coupon.getDiscountAmount() == null || coupon.getDiscountAmount().signum() < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "优惠券配置异常");
        }
        if (userCouponMapper.consume(userCouponId, userId) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "优惠券已被使用，请刷新后重试");
        }
        return coupon.getDiscountAmount();
    }

    private BigDecimal effectivePrice(Product product, Sku sku, LocalDateTime now) {
        boolean promoActive = product.getPromoPrice() != null
                && product.getPromoStartTime() != null
                && product.getPromoEndTime() != null
                && !now.isBefore(product.getPromoStartTime())
                && !now.isAfter(product.getPromoEndTime());
        BigDecimal price = promoActive ? product.getPromoPrice() : sku.getPrice();
        if (price == null || price.signum() < 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "商品价格配置异常");
        }
        return price.setScale(2, RoundingMode.HALF_UP);
    }

    private void restoreInventory(Long orderId) {
        List<OrderItem> items = itemMapper.selectList(new QueryWrapper<OrderItem>().eq("order_id", orderId));
        Set<Long> affectedProducts = new HashSet<>();
        for (OrderItem item : items) {
            if (skuMapper.restoreStock(item.getSkuId(), item.getProductCount()) != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "库存恢复失败");
            }
            affectedProducts.add(item.getProductId());
        }
        affectedProducts.forEach(this::syncProductStock);
    }

    private void releaseCoupon(Order order) {
        if (order.getUserCouponId() == null) return;
        UserCoupon userCoupon = userCouponMapper.selectById(order.getUserCouponId());
        if (userCoupon == null || !Objects.equals(userCoupon.getUserId(), order.getUserId())) return;
        Coupon coupon = couponMapper.selectById(userCoupon.getCouponId());
        int targetStatus = coupon != null && coupon.getEndTime() != null
                && !LocalDateTime.now().isAfter(coupon.getEndTime()) ? 0 : 2;
        if (userCouponMapper.release(userCoupon.getId(), order.getUserId(), targetStatus) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "优惠券退回失败");
        }
    }

    private void syncProductStock(Long productId) {
        List<Sku> skus = skuMapper.selectList(new QueryWrapper<Sku>().eq("product_id", productId));
        int totalStock = skus.stream().mapToInt(sku -> sku.getStock() == null ? 0 : sku.getStock()).sum();
        Product product = new Product();
        product.setId(productId);
        product.setStock(totalStock);
        productMapper.updateById(product);
    }

    private void validateIdempotencyKey(String key) {
        if (key == null || !key.matches("^[A-Za-z0-9_-]{16,64}$")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "缺少有效的幂等键");
        }
    }

    public record CreatedOrder(Long orderId, BigDecimal totalAmount) {}
}
