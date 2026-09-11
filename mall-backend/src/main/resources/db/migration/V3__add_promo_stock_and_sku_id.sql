ALTER TABLE pms_product
    ADD COLUMN promo_stock INT DEFAULT NULL COMMENT '秒杀限量库存',
    ADD COLUMN promo_sku_id BIGINT DEFAULT NULL COMMENT '秒杀绑定SKU ID';
