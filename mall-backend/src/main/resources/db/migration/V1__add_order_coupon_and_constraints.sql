ALTER TABLE oms_order
    ADD COLUMN user_coupon_id BIGINT NULL AFTER user_id;

ALTER TABLE oms_order
    ADD UNIQUE INDEX uk_oms_order_sn (order_sn),
    ADD INDEX idx_oms_order_user_coupon_id (user_coupon_id);
