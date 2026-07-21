CREATE TABLE cms_ad (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) DEFAULT NULL COMMENT '广告标题',
    pic_url VARCHAR(500) NOT NULL COMMENT '图片链接',
    link_url VARCHAR(500) DEFAULT NULL COMMENT '点击跳转链接',
    sort INT DEFAULT 0 COMMENT '排序',
    status INT DEFAULT 1 COMMENT '状态: 0禁用, 1启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='轮播广告表';

CREATE TABLE cms_feedback (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '留言用户ID',
    type VARCHAR(50) NOT NULL COMMENT '类型: 咨询/投诉/建议',
    content VARCHAR(1000) NOT NULL COMMENT '留言内容',
    reply VARCHAR(1000) DEFAULT NULL COMMENT '客服回复内容',
    status INT DEFAULT 0 COMMENT '0:待回复, 1:已回复',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    sender_role INT DEFAULT 0 COMMENT '0:买家, 1:客服',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户反馈留言表';

CREATE TABLE cms_notice (
    id BIGINT NOT NULL AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL COMMENT '公告标题',
    content TEXT NOT NULL COMMENT '公告内容',
    is_active INT DEFAULT 1 COMMENT '1:展示 0:隐藏',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统公告表';

CREATE TABLE oms_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '下单用户ID',
    order_sn VARCHAR(64) NOT NULL COMMENT '订单编号',
    total_amount DECIMAL(10, 2) NOT NULL COMMENT '订单总金额',
    status INT DEFAULT 0 COMMENT '状态：0->待付款',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    product_id BIGINT DEFAULT NULL,
    comment_status INT DEFAULT 0 COMMENT '0-未评价, 1-已评价',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE oms_order_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    sku_id BIGINT DEFAULT NULL COMMENT '所选规格ID',
    product_name VARCHAR(255) DEFAULT NULL,
    product_price DECIMAL(10, 2) DEFAULT NULL,
    product_count INT DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pms_category (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL COMMENT '分类名称',
    parent_id BIGINT DEFAULT 0 COMMENT '父分类ID：0表示一级分类',
    level INT DEFAULT 1 COMMENT '层级：1->一级；2->二级',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pms_comment (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_id BIGINT NOT NULL COMMENT '商品ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '用户昵称',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '用户头像',
    star INT DEFAULT 5 COMMENT '评分：1-5星',
    content TEXT COMMENT '评价内容',
    pics TEXT COMMENT '评价图片，多图用逗号隔开',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    order_id BIGINT DEFAULT NULL COMMENT '订单ID',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pms_favorite (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    product_id BIGINT NOT NULL COMMENT '商品ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_product (user_id, product_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE pms_hot_search (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    keyword VARCHAR(255) NOT NULL COMMENT '搜索关键词',
    search_count INT NOT NULL DEFAULT 1 COMMENT '搜索次数',
    PRIMARY KEY (id),
    UNIQUE KEY uk_keyword (keyword)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='热搜词统计表';

CREATE TABLE pms_product (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '商品ID',
    name VARCHAR(200) NOT NULL COMMENT '商品名称',
    category_id BIGINT DEFAULT NULL COMMENT '分类ID',
    price DECIMAL(10, 2) NOT NULL COMMENT '价格',
    stock INT NOT NULL DEFAULT 0 COMMENT '库存数量',
    pic_url VARCHAR(255) DEFAULT NULL COMMENT '商品主图',
    description TEXT COMMENT '商品详情介绍',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-下架，1-上架',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    sales INT DEFAULT 0 COMMENT '历史销量',
    promo_price DECIMAL(10, 2) DEFAULT NULL COMMENT '秒杀特价',
    promo_start_time DATETIME DEFAULT NULL COMMENT '秒杀开始时间',
    promo_end_time DATETIME DEFAULT NULL COMMENT '秒杀结束时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品信息表';

CREATE TABLE pms_sku (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'SKU ID',
    product_id BIGINT NOT NULL COMMENT '关联的商品ID',
    spec_name VARCHAR(255) NOT NULL COMMENT '规格名称',
    price DECIMAL(10, 2) NOT NULL COMMENT '该规格的专属价格',
    stock INT NOT NULL COMMENT '该规格的专属库存',
    pic_url VARCHAR(255) DEFAULT NULL COMMENT '该规格的专属图片',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品规格(SKU)表';

CREATE TABLE sms_coupon (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '优惠券ID',
    name VARCHAR(100) NOT NULL COMMENT '优惠券名称',
    min_amount DECIMAL(10, 2) NOT NULL COMMENT '使用门槛',
    discount_amount DECIMAL(10, 2) NOT NULL COMMENT '减免金额',
    end_time DATETIME NOT NULL COMMENT '过期时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='优惠券基础表';

CREATE TABLE sms_user_coupon (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '领券记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    coupon_id BIGINT NOT NULL COMMENT '优惠券ID',
    status INT NOT NULL DEFAULT 0 COMMENT '状态：0未使用；1已使用；2已过期',
    get_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    use_time DATETIME DEFAULT NULL COMMENT '使用时间',
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户领券记录表';

CREATE TABLE sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名/账号',
    password VARCHAR(100) NOT NULL COMMENT 'BCrypt 密码摘要',
    nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    phone VARCHAR(20) DEFAULT NULL COMMENT '手机号',
    email VARCHAR(50) DEFAULT NULL COMMENT '邮箱',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    role TINYINT NOT NULL DEFAULT 0 COMMENT '角色：0消费者，1管理员',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0禁用，1启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

CREATE TABLE ums_address (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '所属用户ID',
    receiver_name VARCHAR(64) NOT NULL COMMENT '收货人姓名',
    receiver_phone VARCHAR(20) NOT NULL COMMENT '收货人手机号',
    province VARCHAR(32) DEFAULT NULL COMMENT '省份',
    city VARCHAR(32) DEFAULT NULL COMMENT '城市',
    region VARCHAR(32) DEFAULT NULL COMMENT '区/县',
    detail_address VARCHAR(255) DEFAULT NULL COMMENT '详细地址',
    is_default TINYINT DEFAULT 0 COMMENT '是否为默认地址',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 保留旧版用户表以兼容早期数据；当前认证逻辑使用 sys_user。
CREATE TABLE ums_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(64) NOT NULL COMMENT '账号',
    password VARCHAR(64) NOT NULL COMMENT '密码',
    nickname VARCHAR(64) DEFAULT NULL COMMENT '昵称',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_ums_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
