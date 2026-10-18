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

