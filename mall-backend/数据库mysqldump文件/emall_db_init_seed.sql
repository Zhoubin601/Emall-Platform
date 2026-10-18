-- ============================================================
-- E-MALL 全栈电商平台 - 完整数据库初始化与演示种子数据脚本
-- 导出时间：2026-10-18
-- 数据库名：emall_db (MySQL 8.0+)
-- 编码字符集：utf8mb4 / utf8mb4_unicode_ci
-- 使用方式：mysql -u root -p emall_db < emall_db_init_seed.sql
-- 默认超管：admin / 123456
-- 默认买家：buyer / 123456 或 605 / 605
-- ============================================================

CREATE DATABASE IF NOT EXISTS `emall_db` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `emall_db`;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- [1/2] 结构定义 (DDL)
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


ALTER TABLE oms_order
    ADD COLUMN user_coupon_id BIGINT NULL AFTER user_id;

ALTER TABLE oms_order
    ADD UNIQUE INDEX uk_oms_order_sn (order_sn),
    ADD INDEX idx_oms_order_user_coupon_id (user_coupon_id);


UPDATE sys_user
SET email = NULL
WHERE email IS NOT NULL AND LENGTH(TRIM(email)) = 0;

ALTER TABLE sys_user
    MODIFY COLUMN email VARCHAR(254) DEFAULT NULL COMMENT '邮箱';

CREATE UNIQUE INDEX uk_sys_user_email ON sys_user (email);


ALTER TABLE pms_product
    ADD COLUMN promo_stock INT DEFAULT NULL COMMENT '秒杀限量库存',
    ADD COLUMN promo_sku_id BIGINT DEFAULT NULL COMMENT '秒杀绑定SKU ID';


-- [2/2] 初始演示种子数据 (DML)
-- ============================================================
-- Flyway V4: 初始业务演示种子数据迁移脚本
-- 说明：内置默认超管/买家账号(BCrypt加密)、分类、初始商品、SKU矩阵、
--       优惠券、轮播广告、公告及历史订单数据，支持 Docker 开箱即用。
-- 默认账号：
--   超级管理员：admin / 123456 (角色: 2)
--   买家账号：  buyer / 123456 (角色: 0) 或 605 / 605
--   运营管理员：603 / 603 (角色: 1) 或 606 / 606
-- ============================================================

-- ------------------------------------------------------------
-- Table: sys_user
-- ------------------------------------------------------------
INSERT IGNORE INTO `sys_user` (`id`, `username`, `password`, `nickname`, `phone`, `email`, `avatar`, `role`, `status`, `create_time`) VALUES
(1, 'admin', '$2a$10$eCdQDjDQncgl5dPM8KoBlOsggTjy2Pum4ok7jF02ZBPMEOuzasBna', '系统管理员', '13800000000', NULL, '/uploads/avatar_admin_1778503107711.png', 2, 1, '2026-05-08 13:40:13'),
(2, '601', '$2a$10$oXEqRLvwHfrCOX5u2haPouedBGl3qExrtttbjlIq1GKnSORgKIjqS', 'Z', NULL, NULL, NULL, 0, 1, '2026-05-08 15:29:20'),
(3, 'buyer', '$2a$10$eCdQDjDQncgl5dPM8KoBlOsggTjy2Pum4ok7jF02ZBPMEOuzasBna', 'VIP买家体验官', '13800138000', 'buyer@emall.com', '/uploads/default-avatar.png', 0, 1, '2026-05-09 10:00:00'),
(6, '603', '$2a$10$97.mMwI.UtuhJT9DoKHjQO/PRYWTgo9vPBNAnmkENu6kUp82sBWGW', 'zmjjkk (技术运营)', '13800000603', '3283511301@qq.com', '/uploads/avatar_603_1778672974961.png', 1, 1, '2026-05-11 19:58:20'),
(7, '605', '$2a$10$HB/uxsWdF8aLypqBAdxLJOhJipIy2RF1ILAYsTIcqogXNHfMUTkXq', 'zbb (品质买家)', '13800000605', '3185130953@qq.com', '/uploads/default-avatar.png', 0, 1, '2026-05-13 10:54:37'),
(8, '606', '$2a$10$P97gZy9tbBoEeFwOuBkK6.lpoxvDEVhLw/7G1y3umU5uUag8KAMPe', '运营小E', '13800000606', NULL, '/uploads/default-avatar.png', 1, 1, '2026-05-13 12:04:24');

-- ------------------------------------------------------------
-- Table: pms_category
-- ------------------------------------------------------------
INSERT IGNORE INTO `pms_category` (`id`, `name`, `parent_id`, `level`) VALUES
(1,'手机数码',0,1),
(2,'电脑办公',0,1),
(3,'服装服饰',0,1),
(4,'家居日用',0,1),
(11,'智能手机',1,2),
(12,'影音娱乐',1,2),
(21,'笔记本',2,2),
(22,'电脑外设',2,2),
(31,'潮流男装',3,2),
(32,'时尚女装',3,2),
(41,'咖啡茶饮',4,2),
(42,'生活好物',4,2),
(46,'222',1,2),
(47,'111',0,1),
(48,'23123',0,1);

-- ------------------------------------------------------------
-- Table: pms_product
-- ------------------------------------------------------------
INSERT IGNORE INTO `pms_product` (`id`, `name`, `category_id`, `price`, `stock`, `pic_url`, `description`, `status`, `create_time`, `sales`, `promo_price`, `promo_start_time`, `promo_end_time`, `promo_stock`, `promo_sku_id`) VALUES
(1,'iPhone 15 Pro Max',11,9999.00,47,'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=400&q=80','A17 Pro芯片，全新钛金属机身',1,'2026-05-08 16:12:53',1500,5000.00,'2026-05-11 23:10:17','2026-12-31 23:59:59', 20, 1),
(2,'MacBook Pro M3',21,14999.00,39,'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=400&q=80','满血性能释放，顶级生产力工具',1,'2026-05-08 16:12:53',950,NULL,NULL,NULL, NULL, NULL),
(3,'索尼头戴式降噪耳机',12,1899.00,521,'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=400&q=80','自适应降噪，沉浸式Hi-Fi音质',1,'2026-05-08 16:12:53',3500,NULL,NULL,NULL, NULL, NULL),
(4,'RGB 机械键盘 青轴',22,499.00,118,'https://images.unsplash.com/photo-1595225476474-87563907a212?auto=format&fit=crop&w=400&q=80','全键无冲，清脆打字手感',1,'2026-05-08 16:12:53',850,NULL,NULL,NULL, NULL, NULL),
(5,'纯棉基础款 白色T恤',31,59.00,994,'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=400&q=80','100%纯棉，透气舒适，百搭打底',1,'2026-05-08 16:12:53',5600,NULL,NULL,NULL, NULL, NULL),
(6,'意式深度烘焙 咖啡豆',41,89.00,201,'https://images.unsplash.com/photo-1559525839-b184a4d698c7?auto=format&fit=crop&w=400&q=80','深度烘焙，浓郁焦糖与坚果香',1,'2026-05-08 16:12:53',1500,NULL,NULL,NULL, NULL, NULL),
(7,'人体工学 无线鼠标',22,399.00,90,'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?auto=format&fit=crop&w=400&q=80','超轻量化设计，零延迟响应',1,'2026-05-08 16:12:53',450,NULL,NULL,NULL, NULL, NULL),
(8,'复古胶片 照相机',12,2599.00,60,'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?auto=format&fit=crop&w=400&q=80','经典设计，重温旧时光',1,'2026-05-08 16:12:53',280,NULL,NULL,NULL, NULL, NULL),
(9,'时尚百搭 运动跑鞋',31,499.00,299,'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=400&q=80','轻盈减震，透气飞织鞋面',1,'2026-05-08 16:12:53',1200,NULL,NULL,NULL, NULL, NULL),
(10,'极简风 陶瓷马克杯',42,29.00,399,'https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?auto=format&fit=crop&w=400&q=80','手工高温烧制，圆润舒适',1,'2026-05-08 16:12:53',800,NULL,NULL,NULL, NULL, NULL),
(11,'华为 Mate 60 Pro',11,6999.00,98,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Huawei+Mate60','遥遥领先，玄武架构',1,'2026-05-08 16:12:53',2300,NULL,NULL,NULL, NULL, NULL),
(12,'小米 14 徕卡光学',11,4599.00,150,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Xiaomi+14','第三代骁龙8，徕卡影像',1,'2026-05-08 16:12:53',1200,NULL,NULL,NULL, NULL, NULL),
(13,'联想 拯救者 Y9000P',21,9999.00,20,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Legion+Y9000P','电竞级游戏本，满血释放',1,'2026-05-08 16:12:53',650,NULL,NULL,NULL, NULL, NULL),
(14,'戴尔 XPS 15',21,12999.00,15,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Dell+XPS+15','碳纤维掌托，轻薄创作本',1,'2026-05-08 16:12:53',110,NULL,NULL,NULL, NULL, NULL),
(15,'苹果 AirPods Pro 2',12,1899.00,300,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=AirPods+Pro','超强主动降噪',1,'2026-05-08 16:12:53',4500,NULL,NULL,NULL, NULL, NULL),
(16,'极米 4K 投影仪',12,5499.00,30,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=4K+Projector','家庭影院，哈曼卡顿音响',1,'2026-05-08 16:12:53',120,4999.00,'2026-05-06 00:00:00','2026-12-31 23:59:59', 15, 17),
(17,'Keychron 机械键盘',22,499.00,80,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Keychron+K8','蓝牙双模，客制化体验',1,'2026-05-08 16:12:53',540,NULL,NULL,NULL, NULL, NULL),
(18,'LG 27寸 4K 显示器',22,1999.00,40,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=4K+Monitor','IPS硬屏，设计师首选',1,'2026-05-08 16:12:53',340,NULL,NULL,NULL, NULL, NULL),
(19,'日系复古 工装束脚裤',31,129.00,350,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Cargo+Pants','宽松直筒，多口袋设计',1,'2026-05-08 16:12:53',890,NULL,NULL,NULL, NULL, NULL),
(20,'防泼水 户外冲锋衣',31,299.00,150,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Jacket','防风防水，抓绒内胆',1,'2026-05-08 16:12:53',310,NULL,NULL,NULL, NULL, NULL),
(21,'法式复古 碎花连衣裙',32,189.00,250,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Dress','V领收腰，仙气飘飘',1,'2026-05-08 16:12:53',1200,NULL,NULL,NULL, NULL, NULL),
(22,'高腰显瘦 阔腿牛仔裤',32,139.00,400,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Jeans','弹力修身，百搭显腿长',1,'2026-05-08 16:12:53',2100,NULL,NULL,NULL, NULL, NULL),
(23,'西湖龙井 明前特级',41,258.00,50,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Green+Tea','核心产区，清香醇厚',1,'2026-05-08 16:12:53',320,NULL,NULL,NULL, NULL, NULL),
(24,'三顿半 冻干咖啡',41,159.00,7,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Instant+Coffee','3秒冷水即溶，24颗装',1,'2026-05-08 16:12:53',4500,NULL,NULL,NULL, NULL, NULL),
(25,'飞利浦 电动牙刷',42,299.00,300,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Toothbrush','声波震动，3种清洁模式',1,'2026-05-08 16:12:53',1800,NULL,NULL,NULL, NULL, NULL),
(26,'戴森 智能吹风机',42,2999.00,40,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Hair+Dryer','防飞翘顺发风嘴',1,'2026-05-08 16:12:53',450,NULL,NULL,NULL, NULL, NULL),
(27,'大疆 无人机 Mini 4',11,4799.00,25,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=DJI+Mini+4','全向主动避障',1,'2026-05-08 16:12:53',280,NULL,NULL,NULL, NULL, NULL),
(28,'任天堂 Switch OLED',12,2299.00,84,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Switch+OLED','7英寸OLED屏幕',1,'2026-05-08 16:12:53',1800,NULL,NULL,NULL, NULL, NULL),
(29,'西部数据 2TB 硬盘',22,459.00,498,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=2TB+HDD','小巧便携，安全加密',1,'2026-05-08 16:12:53',3200,NULL,NULL,NULL, NULL, NULL),
(30,'小米 智能音箱 Pro',12,299.00,596,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Smart+Speaker','全屋智能控制中枢',1,'2026-05-08 16:12:53',4100,NULL,NULL,NULL, NULL, NULL);

-- ------------------------------------------------------------
-- Table: pms_sku
-- ------------------------------------------------------------
INSERT IGNORE INTO `pms_sku` (`id`, `product_id`, `spec_name`, `price`, `stock`, `pic_url`) VALUES
(1,1,'钛金属 256GB (标准版)',8999.00,23,'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=400&q=80'),
(2,1,'钛金属 512GB (高配版)',9999.00,24,'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=400&q=80'),
(3,2,'默认标准款',14999.00,39,'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=400&q=80'),
(4,3,'默认标准款',1899.00,499,'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=400&q=80'),
(5,4,'默认标准款',499.00,118,'https://images.unsplash.com/photo-1595225476474-87563907a212?auto=format&fit=crop&w=400&q=80'),
(6,5,'默认标准款',59.00,994,'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=400&q=80'),
(7,6,'默认标准款',89.00,201,'https://images.unsplash.com/photo-1559525839-b184a4d698c7?auto=format&fit=crop&w=400&q=80'),
(8,7,'默认标准款',399.00,90,'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?auto=format&fit=crop&w=400&q=80'),
(9,8,'默认标准款',2599.00,60,'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?auto=format&fit=crop&w=400&q=80'),
(10,9,'默认标准款',499.00,299,'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=400&q=80'),
(11,10,'默认标准款',29.00,399,'https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?auto=format&fit=crop&w=400&q=80'),
(12,11,'默认标准款',6999.00,98,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Huawei+Mate60'),
(13,12,'默认标准款',4599.00,150,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Xiaomi+14'),
(14,13,'默认标准款',9999.00,20,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Legion+Y9000P'),
(15,14,'默认标准款',12999.00,15,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Dell+XPS+15'),
(16,15,'默认标准款',1899.00,300,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=AirPods+Pro'),
(17,16,'默认标准款',5499.00,30,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=4K+Projector'),
(18,17,'默认标准款',499.00,80,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Keychron+K8'),
(19,18,'默认标准款',1999.00,40,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=4K+Monitor'),
(20,19,'默认标准款',129.00,350,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Cargo+Pants'),
(21,20,'默认标准款',299.00,150,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Jacket'),
(22,21,'默认标准款',189.00,250,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Dress'),
(23,22,'默认标准款',139.00,400,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Jeans'),
(24,23,'默认标准款',258.00,50,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Green+Tea'),
(25,24,'默认标准款',159.00,7,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Instant+Coffee'),
(26,25,'默认标准款',299.00,300,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Toothbrush'),
(27,26,'默认标准款',2999.00,40,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Hair+Dryer'),
(28,27,'默认标准款',4799.00,25,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=DJI+Mini+4'),
(29,28,'默认标准款',2299.00,84,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Switch+OLED'),
(30,29,'默认标准款',459.00,498,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=2TB+HDD'),
(31,30,'默认标准款',299.00,596,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Smart+Speaker'),
(32,3,'2222',1899.00,22,'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=400&q=80');

-- ------------------------------------------------------------
-- Table: sms_coupon
-- ------------------------------------------------------------
INSERT IGNORE INTO `sms_coupon` (`id`, `name`, `min_amount`, `discount_amount`, `end_time`, `create_time`) VALUES
(1,'新人专享券',100.00,20.00,'2026-12-31 23:59:59','2026-05-11 22:57:54'),
(2,'数码狂欢神券',3000.00,200.00,'2026-12-31 23:59:59','2026-05-11 22:57:54');

-- ------------------------------------------------------------
-- Table: sms_user_coupon
-- ------------------------------------------------------------
INSERT IGNORE INTO `sms_user_coupon` (`id`, `user_id`, `coupon_id`, `status`, `get_time`, `use_time`) VALUES
(1,6,1,1,'2026-05-11 23:03:47','2026-05-12 13:21:28'),
(2,6,2,1,'2026-05-11 23:03:47','2026-05-11 23:34:15'),
(3,6,2,1,'2026-05-11 23:37:09','2026-05-11 23:37:15'),
(4,6,2,1,'2026-05-12 10:40:19','2026-05-12 18:40:27'),
(5,6,2,1,'2026-05-12 10:41:10','2026-05-12 18:41:15'),
(6,6,2,1,'2026-05-12 10:41:24','2026-05-12 18:42:10'),
(7,6,2,1,'2026-05-12 11:17:34','2026-05-12 19:17:40'),
(8,6,2,1,'2026-05-12 11:20:26','2026-05-12 19:20:33'),
(9,6,2,1,'2026-05-13 10:09:10','2026-05-13 18:10:46'),
(10,6,2,0,'2026-05-13 10:45:20',NULL),
(11,6,1,0,'2026-05-13 11:13:20',NULL);

-- ------------------------------------------------------------
-- Table: cms_ad
-- ------------------------------------------------------------
INSERT IGNORE INTO `cms_ad` (`id`, `title`, `pic_url`, `link_url`, `sort`, `status`, `create_time`) VALUES
(1,'新品发布会','https://images.unsplash.com/photo-1607082348824-0a96f2a4b9da?auto=format&fit=crop&w=1200&q=80',NULL,1,1,'2026-05-12 12:38:44'),
(2,'夏日狂欢节','https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=1200&q=80',NULL,2,1,'2026-05-12 12:38:44'),
(3,'数码极客周','https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?auto=format&fit=crop&w=1200&q=80',NULL,1,1,'2026-05-12 12:38:44'),
(4,'111','https://img.shetu66.com/2023/06/29/1688025012523974.png','',1,1,'2026-05-12 12:53:28'),
(5,'222','/uploads/common_1778561939513.png','',1,1,'2026-05-12 12:59:02');

-- ------------------------------------------------------------
-- Table: cms_notice
-- ------------------------------------------------------------
INSERT IGNORE INTO `cms_notice` (`id`, `title`, `content`, `is_active`, `create_time`) VALUES
(1,'🎉 欢迎来到 E-MALL 严选商城！','新店开业，全场秒杀活动火热进行中，更有千元神券等你来抢！祝您购物愉快。666',1,'2026-05-12 12:05:30'),
(2,'22','22',1,'2026-05-12 12:05:30');

-- ------------------------------------------------------------
-- Table: cms_feedback
-- ------------------------------------------------------------
INSERT IGNORE INTO `cms_feedback` (`id`, `user_id`, `type`, `content`, `reply`, `status`, `create_time`, `sender_role`) VALUES
(1,6,'商品咨询','你好吗？\n','你好请问怎么了？',1,'2026-05-12 12:10:34',0),
(2,6,'在线沟通','有点事这么说？',NULL,1,'2026-05-12 12:23:28',0),
(3,6,'在线沟通','在吗？\n',NULL,1,'2026-05-12 12:23:32',0),
(4,6,'在线沟通','在的\n',NULL,1,'2026-05-12 12:23:38',1),
(5,6,'购物体验','是的是的','嗯',1,'2026-05-12 12:28:45',0),
(6,6,'在线沟通','..\n',NULL,1,'2026-05-12 11:29:26',0),
(7,6,'在线沟通','1',NULL,1,'2026-05-12 11:29:29',0),
(8,6,'在线沟通','111',NULL,1,'2026-05-12 11:29:38',0),
(9,6,'在线沟通','111',NULL,1,'2026-05-12 11:29:43',0),
(10,6,'在线沟通','11',NULL,1,'2026-05-12 11:29:48',1),
(11,6,'购物体验','333','555',1,'2026-05-12 11:29:57',0),
(12,6,'在线沟通','1',NULL,1,'2026-05-12 11:34:30',0),
(13,6,'在线沟通','1',NULL,1,'2026-05-12 11:34:34',0),
(14,6,'在线沟通','111',NULL,1,'2026-05-12 11:36:48',0),
(15,6,'在线沟通','1',NULL,1,'2026-05-12 20:28:39',0),
(16,6,'在线沟通','2',NULL,1,'2026-05-12 20:28:47',1),
(17,6,'在线沟通','3\n',NULL,1,'2026-05-12 20:28:51',0),
(18,6,'在线沟通','4\n',NULL,1,'2026-05-12 20:28:53',1),
(19,6,'功能建议','666',NULL,0,'2026-05-13 11:17:44',0);

-- ------------------------------------------------------------
-- Table: pms_hot_search
-- ------------------------------------------------------------
INSERT IGNORE INTO `pms_hot_search` (`id`, `keyword`, `search_count`) VALUES
(1, '白色', 18),
(2, 'T恤', 25),
(3, 'OLED', 30),
(4, '机械键盘', 45),
(5, '极米 4K 投影仪', 52),
(6, 'iPhone 15 Pro Max', 99);

-- ------------------------------------------------------------
-- Table: pms_favorite
-- ------------------------------------------------------------
INSERT IGNORE INTO `pms_favorite` (`id`, `user_id`, `product_id`, `create_time`) VALUES
(8,6,24,'2026-05-11 20:57:12'),
(9,6,29,'2026-05-12 11:33:05'),
(10,6,1,'2026-05-13 09:56:34');

-- ------------------------------------------------------------
-- Table: pms_comment
-- ------------------------------------------------------------
INSERT IGNORE INTO `pms_comment` (`id`, `product_id`, `user_id`, `nickname`, `avatar`, `star`, `content`, `pics`, `create_time`, `order_id`) VALUES
(13,4,2,'601',NULL,5,'222','http://localhost:8080/uploads/601_RGB 机械键盘 青轴_1778419293382.png','2026-05-10 21:21:33',14),
(15,4,6,'603','/uploads/avatar_603_1778503313405.png',5,'222','http://localhost:8080/uploads/603_RGB 机械键盘 青轴_1778504226760.png','2026-05-11 20:57:07',17),
(16,1,6,'603','/uploads/avatar_603_1778503313405.png',5,'666',NULL,'2026-05-11 23:26:21',18),
(17,1,6,'603','/uploads/avatar_603_1778503313405.png',5,'333',NULL,'2026-05-11 23:34:40',20),
(18,29,6,'603','/uploads/avatar_603_1778503313405.png',5,'666','http://localhost:8080/uploads/603_西部数据 2TB 硬盘_1778564126111.png','2026-05-12 13:35:27',23),
(19,6,6,'603','/uploads/avatar_603_1778503313405.png',5,'666','/uploads/603_意式深度烘焙 咖啡豆_1778583558658.png','2026-05-12 10:59:19',29),
(20,1,6,'603','/uploads/avatar_603_1778503313405.png',5,'666666666666666666','/uploads/603_iPhone 15 Pro Max_1778589825953.png','2026-05-12 12:43:48',40);

-- ------------------------------------------------------------
-- Table: ums_address
-- ------------------------------------------------------------
INSERT IGNORE INTO `ums_address` (`id`, `user_id`, `receiver_name`, `receiver_phone`, `province`, `city`, `region`, `detail_address`, `is_default`, `create_time`) VALUES
(1,2,'1','1','1','1','1','1',1,'2026-05-08 15:49:49'),
(2,2,'2','2','2','2','2','2',0,'2026-05-08 15:52:32'),
(3,2,'3','3','3','3','3','3',0,'2026-05-08 15:52:35'),
(4,6,'1','1','1','1','1','1',1,'2026-05-11 20:56:36');

-- ------------------------------------------------------------
-- Table: oms_order
-- ------------------------------------------------------------
INSERT IGNORE INTO `oms_order` (`id`, `user_id`, `user_coupon_id`, `order_sn`, `total_amount`, `status`, `create_time`, `product_id`, `comment_status`) VALUES
(1, 0, NULL, '1e7255c471854181b876eeeb9ed3aa41',9999.00,2,'2026-05-08 17:23:53',1,0),
(2, 1, NULL, 'a846fc246d05423185da6fdef29f6784',9999.00,2,'2026-05-08 17:23:53',1,0),
(3, 1, NULL, '9c535ba856f44d49b5d86127e8a7bab1',9999.00,2,'2026-05-08 17:23:53',1,0),
(12, 2, NULL, 'e5e71c9867294a6ca042b4831d01569c',15009.00,3,'2026-05-10 21:08:02',2,0),
(13, 2, NULL, '14406cecb85a4738a9cf2e3d665c4a5e',10009.00,3,'2026-05-10 21:08:12',1,0),
(14, 2, NULL, '6e27d725bd944e9ab6498ad16f435c1c',509.00,3,'2026-05-10 21:08:17',4,1),
(15, 2, NULL, 'd412ee3654074c4f97c74461303c58de',509.00,5,'2026-05-10 21:27:35',9,0),
(16, 2, NULL, '5f4c1ed84a1d42c2834e68d5089ce10b',15009.00,3,'2026-05-11 19:02:32',2,0),
(17, 6, NULL, 'e2fb8a5e7b2240aaad043ef4c1fb9278',509.00,3,'2026-05-11 20:56:41',4,1),
(18, 6, NULL, '810bdc2644ae4653a62c208171824e78',4810.00,3,'2026-05-11 23:25:53',1,1),
(19, 6, NULL, '36273c160b9a4876bcfcb3b3bffc887b',4810.00,4,'2026-05-11 23:26:45',1,0),
(20, 6, NULL, '678ae876fa14492189bf92e85546596e',4810.00,3,'2026-05-11 23:34:15',1,1),
(21, 6, NULL, '77230974e3254d3685d7946f9da93c98',4810.00,5,'2026-05-11 23:37:15',1,0),
(22, 6, NULL, 'ba9c9e4c923949a4af2c0c213cbe90e0',4990.00,5,'2026-05-12 13:21:27',1,0),
(23, 6, NULL, '2a5d794300c5464b8dd789fdd8a051eb',469.00,3,'2026-05-12 13:35:04',29,1),
(24, 6, NULL, 'ee033acd180747bda6b7738e67ed3b26',4810.00,5,'2026-05-12 10:40:27',1,0),
(25, 6, NULL, '9b34c49643fd4648a69f866cea6a793b',6809.00,3,'2026-05-12 10:41:14',11,0),
(26, 6, NULL, 'f32df34472e848f3b4f41d5c6f99c3c0',6809.00,3,'2026-05-12 10:42:10',11,0),
(27, 6, NULL, '8ae2859d15304a2da6edfa29dca4c855',99.00,3,'2026-05-12 10:46:19',6,0),
(28, 6, NULL, '97f2a88b64124b178c7bd7013b2c4e2e',99.00,4,'2026-05-12 10:48:27',6,0),
(29, 6, NULL, '85595d91fae74e3d920f96efb3c5b21e',99.00,3,'2026-05-12 18:52:10',6,1),
(30, 6, NULL, '0c148877425444929ade60de43770307',5010.00,4,'2026-05-12 19:11:26',1,0),
(31, 6, NULL, 'ca867dbdd07b4567928181c71c48b5f7',4810.00,4,'2026-05-12 19:17:39',1,0),
(32, 6, NULL, '32743a5088c042acb6061a122fd70b7b',5309.00,4,'2026-05-12 19:20:33',16,0),
(33, 6, NULL, '74b5d211c38d4d5da64c23a5263f2aa7',2309.00,4,'2026-05-12 19:25:40',28,0),
(34, 6, NULL, '20008e6a6f7a47159a9414b439b3b88c',469.00,2,'2026-05-12 20:29:15',29,0),
(40, 6, NULL, 'd1f314fbf2e14f688616de5bd07847ca',5010.00,3,'2026-05-12 20:43:06',1,1),
(41, 6, NULL, 'a9cfa8b409a94381a5dcb4a8e468d5aa',4987.00,1,'2026-05-13 18:10:45',1,0);

-- ------------------------------------------------------------
-- Table: oms_order_item
-- ------------------------------------------------------------
INSERT IGNORE INTO `oms_order_item` (`id`, `order_id`, `product_id`, `sku_id`, `product_name`, `product_price`, `product_count`) VALUES
(1,1,2,NULL,'MacbookM2',9999.00,1),
(2,2,2,NULL,'MacbookM2',9999.00,1),
(3,3,2,NULL,'MacbookM2',9999.00,1),
(4,4,2,NULL,'MacbookM2',9999.00,1),
(5,5,2,NULL,'MacbookM2',9999.00,1),
(6,6,2,NULL,'MacbookM2',9999.00,1),
(7,7,1,NULL,'iPhone 15 Pro Max',9999.00,1),
(8,7,3,NULL,'索尼头戴式降噪耳机',1899.00,1),
(9,8,5,NULL,'纯棉基础款 白色T恤',59.00,1),
(10,9,5,NULL,'纯棉基础款 白色T恤',59.00,1),
(11,10,10,NULL,'极简风 陶瓷马克杯 (默认标准版)',29.00,1),
(12,11,2,NULL,'MacBook Pro M3 (尊享豪华版)',14999.00,1),
(13,11,5,NULL,'纯棉基础款 白色T恤 (高级升级版)',59.00,1),
(14,12,2,NULL,'MacBook Pro M3 (默认规格)',14999.00,1),
(15,13,1,NULL,'iPhone 15 Pro Max (默认规格)',9999.00,1),
(16,14,4,NULL,'RGB 机械键盘 青轴 (默认规格)',499.00,1),
(17,15,9,NULL,'时尚百搭 运动跑鞋 (默认规格)',499.00,1),
(18,16,2,NULL,'MacBook Pro M3 (默认规格)',14999.00,1),
(19,17,4,NULL,'RGB 机械键盘 青轴 (默认标准版)',499.00,1),
(20,18,1,NULL,'iPhone 15 Pro Max (默认规格)',5000.00,1),
(21,19,1,NULL,'iPhone 15 Pro Max (钛金属 256GB (标准版))',5000.00,1),
(22,20,1,NULL,'iPhone 15 Pro Max (默认规格)',5000.00,1),
(23,21,1,NULL,'iPhone 15 Pro Max (钛金属 256GB (标准版))',5000.00,1),
(24,22,1,NULL,'iPhone 15 Pro Max (钛金属 512GB (高配版))',5000.00,1),
(25,23,29,30,'西部数据 2TB 硬盘 (默认标准款)',459.00,1),
(26,24,1,2,'iPhone 15 Pro Max (钛金属 512GB (高配版))',5000.00,1),
(27,25,11,12,'华为 Mate 60 Pro (默认标准款)',6999.00,1),
(28,26,11,12,'华为 Mate 60 Pro (默认标准款)',6999.00,1),
(29,27,6,7,'意式深度烘焙 咖啡豆 (默认标准款)',89.00,1),
(30,28,6,7,'意式深度烘焙 咖啡豆 (默认标准款)',89.00,1),
(31,29,6,7,'意式深度烘焙 咖啡豆 (默认标准款)',89.00,1),
(32,30,1,1,'iPhone 15 Pro Max (钛金属 256GB (标准版))',5000.00,1),
(33,31,1,2,'iPhone 15 Pro Max (钛金属 512GB (高配版))',5000.00,1),
(34,32,16,17,'极米 4K 投影仪 (默认标准款)',5499.00,1),
(35,33,28,29,'任天堂 Switch OLED (默认标准款)',2299.00,1),
(36,34,29,30,'西部数据 2TB 硬盘 (默认标准款)',459.00,1),
(42,40,1,1,'iPhone 15 Pro Max (钛金属 256GB (标准版))',5000.00,1),
(43,41,1,2,'iPhone 15 Pro Max (钛金属 512GB (高配版))',5000.00,1),
(44,41,5,6,'纯棉基础款 白色T恤 (默认标准款)',59.00,3);

-- ------------------------------------------------------------
-- Table: ums_user
-- ------------------------------------------------------------
INSERT IGNORE INTO `ums_user` (`id`, `username`, `password`, `nickname`, `create_time`) VALUES
(1,'admin','123456','系统管理员','2026-05-08 15:15:31');



SET FOREIGN_KEY_CHECKS = 1;
