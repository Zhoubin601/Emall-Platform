CREATE TABLE pms_product (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    category_id BIGINT NULL,
    name VARCHAR(255) NULL,
    price DECIMAL(12, 2) NULL,
    stock INT NULL,
    pic_url VARCHAR(1000) NULL,
    description TEXT NULL,
    status INT NULL,
    sales INT NULL,
    create_time DATETIME NULL,
    promo_price DECIMAL(12, 2) NULL,
    promo_start_time DATETIME NULL,
    promo_end_time DATETIME NULL
) ENGINE=InnoDB;

CREATE TABLE pms_sku (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL,
    spec_name VARCHAR(255) NULL,
    price DECIMAL(12, 2) NULL,
    stock INT NOT NULL,
    pic_url VARCHAR(1000) NULL,
    INDEX idx_pms_sku_product_id (product_id)
) ENGINE=InnoDB;

CREATE TABLE sms_coupon (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(255) NULL,
    min_amount DECIMAL(12, 2) NULL,
    discount_amount DECIMAL(12, 2) NULL,
    end_time DATETIME NULL,
    create_time DATETIME NULL
) ENGINE=InnoDB;

CREATE TABLE sms_user_coupon (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    coupon_id BIGINT NOT NULL,
    status INT NOT NULL,
    get_time DATETIME NULL,
    use_time DATETIME NULL,
    INDEX idx_sms_user_coupon_owner (id, user_id, status)
) ENGINE=InnoDB;

CREATE TABLE oms_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    user_coupon_id BIGINT NULL,
    order_sn VARCHAR(64) NOT NULL,
    total_amount DECIMAL(12, 2) NOT NULL,
    status INT NOT NULL,
    product_id BIGINT NULL,
    comment_status INT NULL,
    create_time DATETIME NULL,
    UNIQUE INDEX uk_oms_order_sn (order_sn)
) ENGINE=InnoDB;

CREATE TABLE oms_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sku_id BIGINT NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_price DECIMAL(12, 2) NOT NULL,
    product_count INT NOT NULL,
    INDEX idx_oms_order_item_order_id (order_id)
) ENGINE=InnoDB;
