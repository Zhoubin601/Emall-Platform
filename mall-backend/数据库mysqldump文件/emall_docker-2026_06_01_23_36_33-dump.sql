-- MySQL dump 10.13  Distrib 8.0.45, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: emall_db
-- ------------------------------------------------------
-- Server version	8.0.46

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

--
-- Table structure for table `cms_ad`
--

DROP TABLE IF EXISTS `cms_ad`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cms_ad` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) DEFAULT NULL COMMENT '广告标题',
  `pic_url` varchar(500) NOT NULL COMMENT '图片链接',
  `link_url` varchar(500) DEFAULT NULL COMMENT '点击跳转链接',
  `sort` int DEFAULT '0' COMMENT '排序',
  `status` int DEFAULT '1' COMMENT '状态: 0禁用, 1启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='轮播广告表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cms_ad`
--

LOCK TABLES `cms_ad` WRITE;
/*!40000 ALTER TABLE `cms_ad` DISABLE KEYS */;
INSERT INTO `cms_ad` VALUES (1,'新品发布会','https://images.unsplash.com/photo-1607082348824-0a96f2a4b9da?auto=format&fit=crop&w=1200&q=80',NULL,1,1,'2026-05-12 12:38:44'),(2,'夏日狂欢节','https://images.unsplash.com/photo-1483985988355-763728e1935b?auto=format&fit=crop&w=1200&q=80',NULL,2,1,'2026-05-12 12:38:44'),(3,'数码极客周','https://images.unsplash.com/photo-1486406146926-c627a92ad1ab?auto=format&fit=crop&w=1200&q=80',NULL,1,1,'2026-05-12 12:38:44'),(4,'111','https://img.shetu66.com/2023/06/29/1688025012523974.png','',1,1,'2026-05-12 12:53:28'),(5,'222','/uploads/common_1778561939513.png','',1,1,'2026-05-12 12:59:02');
/*!40000 ALTER TABLE `cms_ad` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cms_feedback`
--

DROP TABLE IF EXISTS `cms_feedback`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cms_feedback` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '留言用户ID',
  `type` varchar(50) NOT NULL COMMENT '类型: 咨询/投诉/建议',
  `content` varchar(1000) NOT NULL COMMENT '留言内容',
  `reply` varchar(1000) DEFAULT NULL COMMENT '客服回复内容',
  `status` int DEFAULT '0' COMMENT '0:待回复, 1:已回复',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `sender_role` int DEFAULT '0' COMMENT '0:买家, 1:客服',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=20 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户反馈留言表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cms_feedback`
--

LOCK TABLES `cms_feedback` WRITE;
/*!40000 ALTER TABLE `cms_feedback` DISABLE KEYS */;
INSERT INTO `cms_feedback` VALUES (1,6,'商品咨询','你好吗？\n','你好请问怎么了？',1,'2026-05-12 12:10:34',0),(2,6,'在线沟通','有点事这么说？',NULL,1,'2026-05-12 12:23:28',0),(3,6,'在线沟通','在吗？\n',NULL,1,'2026-05-12 12:23:32',0),(4,6,'在线沟通','在的\n',NULL,1,'2026-05-12 12:23:38',1),(5,6,'购物体验','是的是的','嗯',1,'2026-05-12 12:28:45',0),(6,6,'在线沟通','..\n',NULL,1,'2026-05-12 11:29:26',0),(7,6,'在线沟通','1',NULL,1,'2026-05-12 11:29:29',0),(8,6,'在线沟通','111',NULL,1,'2026-05-12 11:29:38',0),(9,6,'在线沟通','111',NULL,1,'2026-05-12 11:29:43',0),(10,6,'在线沟通','11',NULL,1,'2026-05-12 11:29:48',1),(11,6,'购物体验','333','555',1,'2026-05-12 11:29:57',0),(12,6,'在线沟通','1',NULL,1,'2026-05-12 11:34:30',0),(13,6,'在线沟通','1',NULL,1,'2026-05-12 11:34:34',0),(14,6,'在线沟通','111',NULL,1,'2026-05-12 11:36:48',0),(15,6,'在线沟通','1',NULL,1,'2026-05-12 20:28:39',0),(16,6,'在线沟通','2',NULL,1,'2026-05-12 20:28:47',1),(17,6,'在线沟通','3\n',NULL,1,'2026-05-12 20:28:51',0),(18,6,'在线沟通','4\n',NULL,1,'2026-05-12 20:28:53',1),(19,6,'功能建议','666',NULL,0,'2026-05-13 11:17:44',0);
/*!40000 ALTER TABLE `cms_feedback` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `cms_notice`
--

DROP TABLE IF EXISTS `cms_notice`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cms_notice` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `title` varchar(255) NOT NULL COMMENT '公告标题',
  `content` text NOT NULL COMMENT '公告内容',
  `is_active` int DEFAULT '1' COMMENT '1:展示 0:隐藏',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统公告表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `cms_notice`
--

LOCK TABLES `cms_notice` WRITE;
/*!40000 ALTER TABLE `cms_notice` DISABLE KEYS */;
INSERT INTO `cms_notice` VALUES (1,'🎉 欢迎来到 E-MALL 严选商城！','新店开业，全场秒杀活动火热进行中，更有千元神券等你来抢！祝您购物愉快。666',1,'2026-05-12 12:05:30'),(2,'22','22',1,'2026-05-12 12:05:30');
/*!40000 ALTER TABLE `cms_notice` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oms_order`
--

DROP TABLE IF EXISTS `oms_order`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oms_order` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '下单用户ID',
  `order_sn` varchar(64) NOT NULL COMMENT '订单编号',
  `total_amount` decimal(10,2) NOT NULL COMMENT '订单总金额',
  `status` int DEFAULT '0' COMMENT '状态：0->待付款',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `product_id` bigint DEFAULT NULL,
  `comment_status` int DEFAULT '0' COMMENT '0-未评价, 1-已评价',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=42 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oms_order`
--

LOCK TABLES `oms_order` WRITE;
/*!40000 ALTER TABLE `oms_order` DISABLE KEYS */;
INSERT INTO `oms_order` VALUES (1,0,'1e7255c471854181b876eeeb9ed3aa41',9999.00,2,'2026-05-08 17:23:53',1,0),(2,1,'a846fc246d05423185da6fdef29f6784',9999.00,2,'2026-05-08 17:23:53',1,0),(3,1,'9c535ba856f44d49b5d86127e8a7bab1',9999.00,2,'2026-05-08 17:23:53',1,0),(12,2,'e5e71c9867294a6ca042b4831d01569c',15009.00,3,'2026-05-10 21:08:02',2,0),(13,2,'14406cecb85a4738a9cf2e3d665c4a5e',10009.00,3,'2026-05-10 21:08:12',1,0),(14,2,'6e27d725bd944e9ab6498ad16f435c1c',509.00,3,'2026-05-10 21:08:17',4,1),(15,2,'d412ee3654074c4f97c74461303c58de',509.00,5,'2026-05-10 21:27:35',9,0),(16,2,'5f4c1ed84a1d42c2834e68d5089ce10b',15009.00,3,'2026-05-11 19:02:32',2,0),(17,6,'e2fb8a5e7b2240aaad043ef4c1fb9278',509.00,3,'2026-05-11 20:56:41',4,1),(18,6,'810bdc2644ae4653a62c208171824e78',4810.00,3,'2026-05-11 23:25:53',1,1),(19,6,'36273c160b9a4876bcfcb3b3bffc887b',4810.00,4,'2026-05-11 23:26:45',1,0),(20,6,'678ae876fa14492189bf92e85546596e',4810.00,3,'2026-05-11 23:34:15',1,1),(21,6,'77230974e3254d3685d7946f9da93c98',4810.00,5,'2026-05-11 23:37:15',1,0),(22,6,'ba9c9e4c923949a4af2c0c213cbe90e0',4990.00,5,'2026-05-12 13:21:27',1,0),(23,6,'2a5d794300c5464b8dd789fdd8a051eb',469.00,3,'2026-05-12 13:35:04',29,1),(24,6,'ee033acd180747bda6b7738e67ed3b26',4810.00,5,'2026-05-12 10:40:27',1,0),(25,6,'9b34c49643fd4648a69f866cea6a793b',6809.00,3,'2026-05-12 10:41:14',11,0),(26,6,'f32df34472e848f3b4f41d5c6f99c3c0',6809.00,3,'2026-05-12 10:42:10',11,0),(27,6,'8ae2859d15304a2da6edfa29dca4c855',99.00,3,'2026-05-12 10:46:19',6,0),(28,6,'97f2a88b64124b178c7bd7013b2c4e2e',99.00,4,'2026-05-12 10:48:27',6,0),(29,6,'85595d91fae74e3d920f96efb3c5b21e',99.00,3,'2026-05-12 18:52:10',6,1),(30,6,'0c148877425444929ade60de43770307',5010.00,4,'2026-05-12 19:11:26',1,0),(31,6,'ca867dbdd07b4567928181c71c48b5f7',4810.00,4,'2026-05-12 19:17:39',1,0),(32,6,'32743a5088c042acb6061a122fd70b7b',5309.00,4,'2026-05-12 19:20:33',16,0),(33,6,'74b5d211c38d4d5da64c23a5263f2aa7',2309.00,4,'2026-05-12 19:25:40',28,0),(34,6,'20008e6a6f7a47159a9414b439b3b88c',469.00,2,'2026-05-12 20:29:15',29,0),(40,6,'d1f314fbf2e14f688616de5bd07847ca',5010.00,3,'2026-05-12 20:43:06',1,1),(41,6,'a9cfa8b409a94381a5dcb4a8e468d5aa',4987.00,1,'2026-05-13 18:10:45',1,0);
/*!40000 ALTER TABLE `oms_order` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `oms_order_item`
--

DROP TABLE IF EXISTS `oms_order_item`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `oms_order_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `order_id` bigint NOT NULL,
  `product_id` bigint NOT NULL,
  `sku_id` bigint DEFAULT NULL COMMENT '所选规格ID',
  `product_name` varchar(255) DEFAULT NULL,
  `product_price` decimal(10,2) DEFAULT NULL,
  `product_count` int DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=45 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `oms_order_item`
--

LOCK TABLES `oms_order_item` WRITE;
/*!40000 ALTER TABLE `oms_order_item` DISABLE KEYS */;
INSERT INTO `oms_order_item` VALUES (1,1,2,NULL,'MacbookM2',9999.00,1),(2,2,2,NULL,'MacbookM2',9999.00,1),(3,3,2,NULL,'MacbookM2',9999.00,1),(4,4,2,NULL,'MacbookM2',9999.00,1),(5,5,2,NULL,'MacbookM2',9999.00,1),(6,6,2,NULL,'MacbookM2',9999.00,1),(7,7,1,NULL,'iPhone 15 Pro Max',9999.00,1),(8,7,3,NULL,'索尼头戴式降噪耳机',1899.00,1),(9,8,5,NULL,'纯棉基础款 白色T恤',59.00,1),(10,9,5,NULL,'纯棉基础款 白色T恤',59.00,1),(11,10,10,NULL,'极简风 陶瓷马克杯 (默认标准版)',29.00,1),(12,11,2,NULL,'MacBook Pro M3 (尊享豪华版)',14999.00,1),(13,11,5,NULL,'纯棉基础款 白色T恤 (高级升级版)',59.00,1),(14,12,2,NULL,'MacBook Pro M3 (默认规格)',14999.00,1),(15,13,1,NULL,'iPhone 15 Pro Max (默认规格)',9999.00,1),(16,14,4,NULL,'RGB 机械键盘 青轴 (默认规格)',499.00,1),(17,15,9,NULL,'时尚百搭 运动跑鞋 (默认规格)',499.00,1),(18,16,2,NULL,'MacBook Pro M3 (默认规格)',14999.00,1),(19,17,4,NULL,'RGB 机械键盘 青轴 (默认标准版)',499.00,1),(20,18,1,NULL,'iPhone 15 Pro Max (默认规格)',5000.00,1),(21,19,1,NULL,'iPhone 15 Pro Max (钛金属 256GB (标准版))',5000.00,1),(22,20,1,NULL,'iPhone 15 Pro Max (默认规格)',5000.00,1),(23,21,1,NULL,'iPhone 15 Pro Max (钛金属 256GB (标准版))',5000.00,1),(24,22,1,NULL,'iPhone 15 Pro Max (钛金属 512GB (高配版))',5000.00,1),(25,23,29,30,'西部数据 2TB 硬盘 (默认标准款)',459.00,1),(26,24,1,2,'iPhone 15 Pro Max (钛金属 512GB (高配版))',5000.00,1),(27,25,11,12,'华为 Mate 60 Pro (默认标准款)',6999.00,1),(28,26,11,12,'华为 Mate 60 Pro (默认标准款)',6999.00,1),(29,27,6,7,'意式深度烘焙 咖啡豆 (默认标准款)',89.00,1),(30,28,6,7,'意式深度烘焙 咖啡豆 (默认标准款)',89.00,1),(31,29,6,7,'意式深度烘焙 咖啡豆 (默认标准款)',89.00,1),(32,30,1,1,'iPhone 15 Pro Max (钛金属 256GB (标准版))',5000.00,1),(33,31,1,2,'iPhone 15 Pro Max (钛金属 512GB (高配版))',5000.00,1),(34,32,16,17,'极米 4K 投影仪 (默认标准款)',5499.00,1),(35,33,28,29,'任天堂 Switch OLED (默认标准款)',2299.00,1),(36,34,29,30,'西部数据 2TB 硬盘 (默认标准款)',459.00,1),(42,40,1,1,'iPhone 15 Pro Max (钛金属 256GB (标准版))',5000.00,1),(43,41,1,2,'iPhone 15 Pro Max (钛金属 512GB (高配版))',5000.00,1),(44,41,5,6,'纯棉基础款 白色T恤 (默认标准款)',59.00,3);
/*!40000 ALTER TABLE `oms_order_item` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pms_category`
--

DROP TABLE IF EXISTS `pms_category`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pms_category` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `name` varchar(64) NOT NULL COMMENT '分类名称',
  `parent_id` bigint DEFAULT '0' COMMENT '父分类ID：0表示一级分类',
  `level` int DEFAULT '1' COMMENT '层级：1->一级；2->二级',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=49 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pms_category`
--

LOCK TABLES `pms_category` WRITE;
/*!40000 ALTER TABLE `pms_category` DISABLE KEYS */;
INSERT INTO `pms_category` VALUES (1,'手机数码',0,1),(2,'电脑办公',0,1),(3,'服装服饰',0,1),(4,'家居日用',0,1),(11,'智能手机',1,2),(12,'影音娱乐',1,2),(21,'笔记本',2,2),(22,'电脑外设',2,2),(31,'潮流男装',3,2),(32,'时尚女装',3,2),(41,'咖啡茶饮',4,2),(42,'生活好物',4,2),(46,'222',1,2),(47,'111',0,1),(48,'23123',0,1);
/*!40000 ALTER TABLE `pms_category` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pms_comment`
--

DROP TABLE IF EXISTS `pms_comment`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pms_comment` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `nickname` varchar(64) DEFAULT NULL COMMENT '用户昵称',
  `avatar` varchar(255) DEFAULT NULL COMMENT '用户头像',
  `star` int DEFAULT '5' COMMENT '评分：1-5星',
  `content` text COMMENT '评价内容',
  `pics` text COMMENT '评价图片，多图用逗号隔开',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `order_id` bigint DEFAULT NULL COMMENT '订单ID',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=21 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pms_comment`
--

LOCK TABLES `pms_comment` WRITE;
/*!40000 ALTER TABLE `pms_comment` DISABLE KEYS */;
INSERT INTO `pms_comment` VALUES (13,4,2,'601',NULL,5,'222','http://localhost:8080/uploads/601_RGB 机械键盘 青轴_1778419293382.png','2026-05-10 21:21:33',14),(15,4,6,'603','/uploads/avatar_603_1778503313405.png',5,'222','http://localhost:8080/uploads/603_RGB 机械键盘 青轴_1778504226760.png','2026-05-11 20:57:07',17),(16,1,6,'603','/uploads/avatar_603_1778503313405.png',5,'666',NULL,'2026-05-11 23:26:21',18),(17,1,6,'603','/uploads/avatar_603_1778503313405.png',5,'333',NULL,'2026-05-11 23:34:40',20),(18,29,6,'603','/uploads/avatar_603_1778503313405.png',5,'666','http://localhost:8080/uploads/603_西部数据 2TB 硬盘_1778564126111.png','2026-05-12 13:35:27',23),(19,6,6,'603','/uploads/avatar_603_1778503313405.png',5,'666','/uploads/603_意式深度烘焙 咖啡豆_1778583558658.png','2026-05-12 10:59:19',29),(20,1,6,'603','/uploads/avatar_603_1778503313405.png',5,'666666666666666666','/uploads/603_iPhone 15 Pro Max_1778589825953.png','2026-05-12 12:43:48',40);
/*!40000 ALTER TABLE `pms_comment` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pms_favorite`
--

DROP TABLE IF EXISTS `pms_favorite`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pms_favorite` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `product_id` bigint NOT NULL COMMENT '商品ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_product` (`user_id`,`product_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pms_favorite`
--

LOCK TABLES `pms_favorite` WRITE;
/*!40000 ALTER TABLE `pms_favorite` DISABLE KEYS */;
INSERT INTO `pms_favorite` VALUES (8,6,24,'2026-05-11 20:57:12'),(9,6,29,'2026-05-12 11:33:05'),(10,6,1,'2026-05-13 09:56:34');
/*!40000 ALTER TABLE `pms_favorite` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pms_hot_search`
--

DROP TABLE IF EXISTS `pms_hot_search`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pms_hot_search` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `keyword` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '搜索关键词',
  `search_count` int NOT NULL DEFAULT '1' COMMENT '搜索次数',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_keyword` (`keyword`) USING BTREE COMMENT '关键词唯一索引，防止重复记录'
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='热搜词统计表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pms_hot_search`
--

LOCK TABLES `pms_hot_search` WRITE;
/*!40000 ALTER TABLE `pms_hot_search` DISABLE KEYS */;
INSERT INTO `pms_hot_search` VALUES (1,'白色',3),(2,'T学',2),(3,'OlEd、',1),(4,'OlEd',1);
/*!40000 ALTER TABLE `pms_hot_search` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pms_product`
--

DROP TABLE IF EXISTS `pms_product`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pms_product` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '商品ID',
  `name` varchar(200) NOT NULL COMMENT '商品名称',
  `category_id` bigint DEFAULT NULL COMMENT '分类ID',
  `price` decimal(10,2) NOT NULL COMMENT '价格',
  `stock` int NOT NULL DEFAULT '0' COMMENT '库存数量',
  `pic_url` varchar(255) DEFAULT NULL COMMENT '商品主图',
  `description` text COMMENT '商品详情介绍',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-下架，1-上架',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `sales` int DEFAULT '0' COMMENT '历史销量',
  `promo_price` decimal(10,2) DEFAULT NULL COMMENT '秒杀特价',
  `promo_start_time` datetime DEFAULT NULL COMMENT '秒杀开始时间',
  `promo_end_time` datetime DEFAULT NULL COMMENT '秒杀结束时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=61 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品信息表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pms_product`
--

LOCK TABLES `pms_product` WRITE;
/*!40000 ALTER TABLE `pms_product` DISABLE KEYS */;
INSERT INTO `pms_product` VALUES (1,'iPhone 15 Pro Max',11,9999.00,47,'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=400&q=80','A17 Pro芯片，全新钛金属机身',1,'2026-05-08 16:12:53',1500,5000.00,'2026-05-11 23:10:17','2026-06-24 00:00:00'),(2,'MacBook Pro M3',21,14999.00,39,'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=400&q=80','满血性能释放，顶级生产力工具',1,'2026-05-08 16:12:53',950,NULL,NULL,NULL),(3,'索尼头戴式降噪耳机',12,1899.00,521,'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=400&q=80','自适应降噪，沉浸式Hi-Fi音质',1,'2026-05-08 16:12:53',3500,NULL,NULL,NULL),(4,'RGB 机械键盘 青轴',22,499.00,118,'https://images.unsplash.com/photo-1595225476474-87563907a212?auto=format&fit=crop&w=400&q=80','全键无冲，清脆打字手感',1,'2026-05-08 16:12:53',850,NULL,NULL,NULL),(5,'纯棉基础款 白色T恤',31,59.00,994,'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=400&q=80','100%纯棉，透气舒适，百搭打底',1,'2026-05-08 16:12:53',5600,NULL,NULL,NULL),(6,'意式深度烘焙 咖啡豆',41,89.00,201,'https://images.unsplash.com/photo-1559525839-b184a4d698c7?auto=format&fit=crop&w=400&q=80','深度烘焙，浓郁焦糖与坚果香',1,'2026-05-08 16:12:53',1500,NULL,NULL,NULL),(7,'人体工学 无线鼠标',22,399.00,90,'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?auto=format&fit=crop&w=400&q=80','超轻量化设计，零延迟响应',1,'2026-05-08 16:12:53',450,NULL,NULL,NULL),(8,'复古胶片 照相机',12,2599.00,60,'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?auto=format&fit=crop&w=400&q=80','经典设计，重温旧时光',1,'2026-05-08 16:12:53',280,NULL,NULL,NULL),(9,'时尚百搭 运动跑鞋',31,499.00,299,'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=400&q=80','轻盈减震，透气飞织鞋面',1,'2026-05-08 16:12:53',1200,NULL,NULL,NULL),(10,'极简风 陶瓷马克杯',42,29.00,399,'https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?auto=format&fit=crop&w=400&q=80','手工高温烧制，圆润舒适',1,'2026-05-08 16:12:53',800,NULL,NULL,NULL),(11,'华为 Mate 60 Pro',11,6999.00,98,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Huawei+Mate60','遥遥领先，玄武架构',1,'2026-05-08 16:12:53',2300,NULL,NULL,NULL),(12,'小米 14 徕卡光学',11,4599.00,150,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Xiaomi+14','第三代骁龙8，徕卡影像',1,'2026-05-08 16:12:53',1200,NULL,NULL,NULL),(13,'联想 拯救者 Y9000P',21,9999.00,20,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Legion+Y9000P','电竞级游戏本，满血释放',1,'2026-05-08 16:12:53',650,NULL,NULL,NULL),(14,'戴尔 XPS 15',21,12999.00,15,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Dell+XPS+15','碳纤维掌托，轻薄创作本',1,'2026-05-08 16:12:53',110,NULL,NULL,NULL),(15,'苹果 AirPods Pro 2',12,1899.00,300,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=AirPods+Pro','超强主动降噪',1,'2026-05-08 16:12:53',4500,NULL,NULL,NULL),(16,'极米 4K 投影仪',12,5499.00,30,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=4K+Projector','家庭影院，哈曼卡顿音响',1,'2026-05-08 16:12:53',120,23233.00,'2026-05-06 00:00:00','2026-06-17 00:00:00'),(17,'Keychron 机械键盘',22,499.00,80,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Keychron+K8','蓝牙双模，客制化体验',1,'2026-05-08 16:12:53',540,NULL,NULL,NULL),(18,'LG 27寸 4K 显示器',22,1999.00,40,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=4K+Monitor','IPS硬屏，设计师首选',1,'2026-05-08 16:12:53',340,NULL,NULL,NULL),(19,'日系复古 工装束脚裤',31,129.00,350,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Cargo+Pants','宽松直筒，多口袋设计',1,'2026-05-08 16:12:53',890,NULL,NULL,NULL),(20,'防泼水 户外冲锋衣',31,299.00,150,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Jacket','防风防水，抓绒内胆',1,'2026-05-08 16:12:53',310,NULL,NULL,NULL),(21,'法式复古 碎花连衣裙',32,189.00,250,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Dress','V领收腰，仙气飘飘',1,'2026-05-08 16:12:53',1200,NULL,NULL,NULL),(22,'高腰显瘦 阔腿牛仔裤',32,139.00,400,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Jeans','弹力修身，百搭显腿长',1,'2026-05-08 16:12:53',2100,NULL,NULL,NULL),(23,'西湖龙井 明前特级',41,258.00,50,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Green+Tea','核心产区，清香醇厚',1,'2026-05-08 16:12:53',320,NULL,NULL,NULL),(24,'三顿半 冻干咖啡',41,159.00,7,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Instant+Coffee','3秒冷水即溶，24颗装',1,'2026-05-08 16:12:53',4500,NULL,NULL,NULL),(25,'飞利浦 电动牙刷',42,299.00,300,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Toothbrush','声波震动，3种清洁模式',1,'2026-05-08 16:12:53',1800,NULL,NULL,NULL),(26,'戴森 智能吹风机',42,2999.00,40,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Hair+Dryer','防飞翘顺发风嘴',1,'2026-05-08 16:12:53',450,NULL,NULL,NULL),(27,'大疆 无人机 Mini 4',11,4799.00,25,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=DJI+Mini+4','全向主动避障',1,'2026-05-08 16:12:53',280,NULL,NULL,NULL),(28,'任天堂 Switch OLED',12,2299.00,84,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Switch+OLED','7英寸OLED屏幕',1,'2026-05-08 16:12:53',1800,NULL,NULL,NULL),(29,'西部数据 2TB 硬盘',22,459.00,498,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=2TB+HDD','小巧便携，安全加密',1,'2026-05-08 16:12:53',3200,NULL,NULL,NULL),(30,'小米 智能音箱 Pro',12,299.00,596,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Smart+Speaker','全屋智能控制中枢',1,'2026-05-08 16:12:53',4100,NULL,NULL,NULL);
/*!40000 ALTER TABLE `pms_product` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `pms_sku`
--

DROP TABLE IF EXISTS `pms_sku`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `pms_sku` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'SKU ID',
  `product_id` bigint NOT NULL COMMENT '关联的商品ID',
  `spec_name` varchar(255) NOT NULL COMMENT '规格名称(如: 钛金属 256GB)',
  `price` decimal(10,2) NOT NULL COMMENT '该规格的专属价格',
  `stock` int NOT NULL COMMENT '该规格的专属库存',
  `pic_url` varchar(255) DEFAULT NULL COMMENT '该规格的专属图片(可为空)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='商品规格(SKU)表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `pms_sku`
--

LOCK TABLES `pms_sku` WRITE;
/*!40000 ALTER TABLE `pms_sku` DISABLE KEYS */;
INSERT INTO `pms_sku` VALUES (1,1,'钛金属 256GB (标准版)',8999.00,23,'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=400&q=80'),(2,1,'钛金属 512GB (高配版)',9999.00,24,'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=400&q=80'),(3,2,'默认标准款',14999.00,39,'https://images.unsplash.com/photo-1517336714731-489689fd1ca8?auto=format&fit=crop&w=400&q=80'),(4,3,'默认标准款',1899.00,499,'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=400&q=80'),(5,4,'默认标准款',499.00,118,'https://images.unsplash.com/photo-1595225476474-87563907a212?auto=format&fit=crop&w=400&q=80'),(6,5,'默认标准款',59.00,994,'https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?auto=format&fit=crop&w=400&q=80'),(7,6,'默认标准款',89.00,201,'https://images.unsplash.com/photo-1559525839-b184a4d698c7?auto=format&fit=crop&w=400&q=80'),(8,7,'默认标准款',399.00,90,'https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?auto=format&fit=crop&w=400&q=80'),(9,8,'默认标准款',2599.00,60,'https://images.unsplash.com/photo-1526170375885-4d8ecf77b99f?auto=format&fit=crop&w=400&q=80'),(10,9,'默认标准款',499.00,299,'https://images.unsplash.com/photo-1542291026-7eec264c27ff?auto=format&fit=crop&w=400&q=80'),(11,10,'默认标准款',29.00,399,'https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?auto=format&fit=crop&w=400&q=80'),(12,11,'默认标准款',6999.00,98,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Huawei+Mate60'),(13,12,'默认标准款',4599.00,150,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Xiaomi+14'),(14,13,'默认标准款',9999.00,20,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Legion+Y9000P'),(15,14,'默认标准款',12999.00,15,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Dell+XPS+15'),(16,15,'默认标准款',1899.00,300,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=AirPods+Pro'),(17,16,'默认标准款',5499.00,30,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=4K+Projector'),(18,17,'默认标准款',499.00,80,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Keychron+K8'),(19,18,'默认标准款',1999.00,40,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=4K+Monitor'),(20,19,'默认标准款',129.00,350,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Cargo+Pants'),(21,20,'默认标准款',299.00,150,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Jacket'),(22,21,'默认标准款',189.00,250,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Dress'),(23,22,'默认标准款',139.00,400,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Jeans'),(24,23,'默认标准款',258.00,50,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Green+Tea'),(25,24,'默认标准款',159.00,7,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Instant+Coffee'),(26,25,'默认标准款',299.00,300,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Toothbrush'),(27,26,'默认标准款',2999.00,40,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Hair+Dryer'),(28,27,'默认标准款',4799.00,25,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=DJI+Mini+4'),(29,28,'默认标准款',2299.00,84,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Switch+OLED'),(30,29,'默认标准款',459.00,498,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=2TB+HDD'),(31,30,'默认标准款',299.00,596,'https://dummyimage.com/400x400/e0f2fe/0369a1.png&text=Smart+Speaker'),(32,3,'2222',1899.00,22,'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=400&q=80');
/*!40000 ALTER TABLE `pms_sku` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sms_coupon`
--

DROP TABLE IF EXISTS `sms_coupon`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sms_coupon` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '优惠券ID',
  `name` varchar(100) NOT NULL COMMENT '优惠券名称',
  `min_amount` decimal(10,2) NOT NULL COMMENT '使用门槛(满X元)',
  `discount_amount` decimal(10,2) NOT NULL COMMENT '减免金额(减Y元)',
  `end_time` datetime NOT NULL COMMENT '过期时间',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='优惠券基础表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sms_coupon`
--

LOCK TABLES `sms_coupon` WRITE;
/*!40000 ALTER TABLE `sms_coupon` DISABLE KEYS */;
INSERT INTO `sms_coupon` VALUES (1,'新人专享券',100.00,20.00,'2026-12-31 23:59:59','2026-05-11 22:57:54'),(2,'数码狂欢神券',3000.00,200.00,'2026-12-31 23:59:59','2026-05-11 22:57:54');
/*!40000 ALTER TABLE `sms_coupon` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sms_user_coupon`
--

DROP TABLE IF EXISTS `sms_user_coupon`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sms_user_coupon` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '领券记录ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `coupon_id` bigint NOT NULL COMMENT '优惠券ID',
  `status` int NOT NULL DEFAULT '0' COMMENT '状态：0->未使用；1->已使用；2->已过期',
  `get_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
  `use_time` datetime DEFAULT NULL COMMENT '使用时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=12 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户领券记录表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sms_user_coupon`
--

LOCK TABLES `sms_user_coupon` WRITE;
/*!40000 ALTER TABLE `sms_user_coupon` DISABLE KEYS */;
INSERT INTO `sms_user_coupon` VALUES (1,6,1,1,'2026-05-11 23:03:47','2026-05-12 13:21:28'),(2,6,2,1,'2026-05-11 23:03:47','2026-05-11 23:34:15'),(3,6,2,1,'2026-05-11 23:37:09','2026-05-11 23:37:15'),(4,6,2,1,'2026-05-12 10:40:19','2026-05-12 18:40:27'),(5,6,2,1,'2026-05-12 10:41:10','2026-05-12 18:41:15'),(6,6,2,1,'2026-05-12 10:41:24','2026-05-12 18:42:10'),(7,6,2,1,'2026-05-12 11:17:34','2026-05-12 19:17:40'),(8,6,2,1,'2026-05-12 11:20:26','2026-05-12 19:20:33'),(9,6,2,1,'2026-05-13 10:09:10','2026-05-13 18:10:46'),(10,6,2,0,'2026-05-13 10:45:20',NULL),(11,6,1,0,'2026-05-13 11:13:20',NULL);
/*!40000 ALTER TABLE `sms_user_coupon` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `sys_user`
--

DROP TABLE IF EXISTS `sys_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` varchar(50) NOT NULL COMMENT '用户名/账号',
  `password` varchar(100) NOT NULL COMMENT '密码',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `email` varchar(50) DEFAULT NULL COMMENT '邮箱',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `role` tinyint NOT NULL DEFAULT '0' COMMENT '角色：0-普通消费者，1-超级管理员',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：0-禁用，1-启用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统用户表';
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `sys_user`
--

LOCK TABLES `sys_user` WRITE;
/*!40000 ALTER TABLE `sys_user` DISABLE KEYS */;
INSERT INTO `sys_user` VALUES (1,'admin','123456','系统管理员','admin','','/uploads/avatar_admin_1778503107711.png',2,1,'2026-05-08 13:40:13'),(2,'601','','Z',NULL,NULL,NULL,0,1,'2026-05-08 15:29:20'),(6,'603','603','zmjjkk','603','3283511301@qq.com','/uploads/avatar_603_1778672974961.png',1,1,'2026-05-11 19:58:20'),(7,'605','605','zbb',NULL,'3185130953@qq.com','/uploads/default-avatar.png',0,1,'2026-05-13 10:54:37'),(8,'606','606','111','','','/uploads/default-avatar.png',1,1,'2026-05-13 12:04:24');
/*!40000 ALTER TABLE `sys_user` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ums_address`
--

DROP TABLE IF EXISTS `ums_address`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ums_address` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL COMMENT '所属用户ID',
  `receiver_name` varchar(64) NOT NULL COMMENT '收货人姓名',
  `receiver_phone` varchar(20) NOT NULL COMMENT '收货人手机号',
  `province` varchar(32) DEFAULT NULL COMMENT '省份',
  `city` varchar(32) DEFAULT NULL COMMENT '城市',
  `region` varchar(32) DEFAULT NULL COMMENT '区/县',
  `detail_address` varchar(255) DEFAULT NULL COMMENT '详细地址',
  `is_default` tinyint(1) DEFAULT '0' COMMENT '是否为默认地址：0->否；1->是',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ums_address`
--

LOCK TABLES `ums_address` WRITE;
/*!40000 ALTER TABLE `ums_address` DISABLE KEYS */;
INSERT INTO `ums_address` VALUES (1,2,'1','1','1','1','1','1',1,'2026-05-08 15:49:49'),(2,2,'2','2','2','2','2','2',0,'2026-05-08 15:52:32'),(3,2,'3','3','3','3','3','3',0,'2026-05-08 15:52:35'),(4,6,'1','1','1','1','1','1',1,'2026-05-11 20:56:36');
/*!40000 ALTER TABLE `ums_address` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `ums_user`
--

DROP TABLE IF EXISTS `ums_user`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `ums_user` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(64) NOT NULL COMMENT '账号',
  `password` varchar(64) NOT NULL COMMENT '密码',
  `nickname` varchar(64) DEFAULT NULL COMMENT '昵称',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `ums_user`
--

LOCK TABLES `ums_user` WRITE;
/*!40000 ALTER TABLE `ums_user` DISABLE KEYS */;
INSERT INTO `ums_user` VALUES (1,'admin','123456','系统管理员','2026-05-08 15:15:31');
/*!40000 ALTER TABLE `ums_user` ENABLE KEYS */;
UNLOCK TABLES;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2026-06-01 23:36:33
