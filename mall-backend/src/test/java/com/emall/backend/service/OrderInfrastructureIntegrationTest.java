package com.emall.backend.service;

import com.emall.backend.dto.order.CreateOrderItemRequest;
import com.emall.backend.dto.order.CreateOrderRequest;
import com.emall.backend.mapper.SkuMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "security.jwt.secret=integration-test-secret-key-at-least-32-characters",
        "security.jwt.expiration-ms=3600000",
        "debug=false",
        "logging.level.root=WARN",
        "mybatis-plus.configuration.log-impl=org.apache.ibatis.logging.nologging.NoLoggingImpl"
})
class OrderInfrastructureIntegrationTest {
    private static final Long USER_ID = 7L;

    @Container
    static final MySQLContainer MYSQL = new MySQLContainer("mysql:8.0.36")
            .withDatabaseName("emall_integration")
            .withUsername("emall")
            .withPassword("emall-test-password")
            .withInitScript("db/integration-schema.sql");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7.0-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void registerInfrastructure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("storage.upload-dir",
                () -> System.getProperty("java.io.tmpdir") + "/emall-integration-uploads");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private SkuMapper skuMapper;

    @Autowired
    private OrderService orderService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void resetInfrastructure() {
        jdbcTemplate.update("DELETE FROM oms_order_item");
        jdbcTemplate.update("DELETE FROM oms_order");
        jdbcTemplate.update("DELETE FROM sms_user_coupon");
        jdbcTemplate.update("DELETE FROM sms_coupon");
        jdbcTemplate.update("DELETE FROM pms_sku");
        jdbcTemplate.update("DELETE FROM pms_product");
        redisTemplate.execute((RedisCallback<Void>) connection -> {
            connection.serverCommands().flushDb();
            return null;
        });
    }

    @Test
    void conditionalStockUpdateAllowsOnlyOneConcurrentBuyer() throws Exception {
        insertProduct(9L, 5);
        insertSku(3L, 9L, new BigDecimal("100.00"), 5);

        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Callable<Integer> deduction = () -> {
            ready.countDown();
            assertThat(start.await(10, TimeUnit.SECONDS)).isTrue();
            return skuMapper.deductStock(3L, 4);
        };

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Integer> first = executor.submit(deduction);
            Future<Integer> second = executor.submit(deduction);
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();

            assertThat(first.get(10, TimeUnit.SECONDS) + second.get(10, TimeUnit.SECONDS))
                    .isEqualTo(1);
            assertThat(stockOf(3L)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void failedOrderRollsBackCouponStockAndRedisIdempotencyKey() {
        insertProduct(9L, 5);
        insertSku(3L, 9L, new BigDecimal("100.00"), 5);
        insertSku(4L, 9L, new BigDecimal("50.00"), 0);
        insertCoupon(12L);
        String idempotencyKey = "integration-rollback-key-0001";

        CreateOrderRequest request = new CreateOrderRequest(12L, List.of(
                new CreateOrderItemRequest(3L, 2),
                new CreateOrderItemRequest(4L, 1)));

        assertThatThrownBy(() -> orderService.createOrder(request, USER_ID, idempotencyKey))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));

        assertThat(stockOf(3L)).isEqualTo(5);
        assertThat(stockOf(4L)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM sms_user_coupon WHERE id = 12", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT use_time FROM sms_user_coupon WHERE id = 12", LocalDateTime.class)).isNull();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM oms_order", Integer.class))
                .isZero();
        assertThat(redisTemplate.hasKey(redisKey(idempotencyKey))).isFalse();
    }

    @Test
    void committedOrderPersistsSnapshotAndRedisIdempotencyResult() {
        insertProduct(9L, 5);
        insertSku(3L, 9L, new BigDecimal("100.00"), 5);
        String idempotencyKey = "integration-success-key-0001";

        OrderService.CreatedOrder created = orderService.createOrder(
                new CreateOrderRequest(null,
                        List.of(new CreateOrderItemRequest(3L, 2))),
                USER_ID,
                idempotencyKey);

        assertThat(created.totalAmount()).isEqualByComparingTo("210.00");
        assertThat(stockOf(3L)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT stock FROM pms_product WHERE id = 9", Integer.class)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT product_name FROM oms_order_item WHERE order_id = ?",
                String.class,
                created.orderId())).isEqualTo("集成测试商品 (默认规格)");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT product_price FROM oms_order_item WHERE order_id = ?",
                BigDecimal.class,
                created.orderId())).isEqualByComparingTo("100.00");
        assertThat(redisTemplate.opsForValue().get(redisKey(idempotencyKey)))
                .isEqualTo(String.valueOf(created.orderId()));
        Long ttl = redisTemplate.getExpire(redisKey(idempotencyKey), TimeUnit.SECONDS);
        assertThat(ttl).isNotNull().isPositive().isLessThanOrEqualTo(DurationSeconds.ONE_DAY);
    }

    private void insertProduct(long id, int stock) {
        jdbcTemplate.update("""
                INSERT INTO pms_product
                    (id, name, price, stock, status, sales, create_time)
                VALUES (?, '集成测试商品', 100.00, ?, 1, 0, NOW())
                """, id, stock);
    }

    private void insertSku(long id, long productId, BigDecimal price, int stock) {
        jdbcTemplate.update("""
                INSERT INTO pms_sku (id, product_id, spec_name, price, stock)
                VALUES (?, ?, '默认规格', ?, ?)
                """, id, productId, price, stock);
    }

    private void insertCoupon(long userCouponId) {
        jdbcTemplate.update("""
                INSERT INTO sms_coupon
                    (id, name, min_amount, discount_amount, end_time, create_time)
                VALUES (5, '集成测试优惠券', 100.00, 20.00, DATE_ADD(NOW(), INTERVAL 1 DAY), NOW())
                """);
        jdbcTemplate.update("""
                INSERT INTO sms_user_coupon
                    (id, user_id, coupon_id, status, get_time, use_time)
                VALUES (?, ?, 5, 0, NOW(), NULL)
                """, userCouponId, USER_ID);
    }

    private int stockOf(long skuId) {
        return jdbcTemplate.queryForObject(
                "SELECT stock FROM pms_sku WHERE id = ?", Integer.class, skuId);
    }

    private String redisKey(String idempotencyKey) {
        return "order:idempotency:" + USER_ID + ":" + idempotencyKey;
    }

    private static final class DurationSeconds {
        private static final long ONE_DAY = TimeUnit.DAYS.toSeconds(1);

        private DurationSeconds() {
        }
    }
}
