# Emall Platform (全栈电商平台项目)

这是一个基于现代化技术栈构建的 B2C 全栈电商平台（包含前台商城、后台管理系统以及 Java 后端服务）。项目设计采用了前后端分离架构，提供了完整的商品购买、订单处理、后台管理等电商核心链路功能。

---

## 🛠️ 技术栈 (Technology Stack)

### 🚀 后端 (mall-backend)
- **核心框架**: Java 21 + Spring Boot 3.2.5
- **持久层框架**: MyBatis-Plus 3.5.5
- **数据库**: MySQL 8.0
- **缓存**: Redis
- **Excel 处理**: Alibaba EasyExcel 3.3.2
- **容器化部署**: Docker & Docker Compose

### 💻 前端商城 (emall-client)
- **核心框架**: Vue 3 (Composition API)
- **构建工具**: Vite
- **开发语言**: TypeScript
- **状态管理**: Pinia
- **UI 组件库**: Element Plus
- **网络请求**: Axios

### 📊 后端管理系统 (emall-admin)
- **核心框架**: Vue 3 (Composition API)
- **开发语言**: TypeScript
- **构建工具**: Vite
- **状态管理**: Pinia
- **UI 组件库**: Element Plus
- **图表展示**: ECharts
- **网络请求**: Axios

---

## 📂 项目结构 (Project Structure)

```text
Emall-Platform/
├── emall-client/        # 前台商城 (Web/H5 端) - 面向普通用户
├── emall-admin/         # 后台管理系统 - 面向管理员和运营人员
├── mall-backend/        # Java 服务端 - 核心业务逻辑与 API
├── .gitignore           # 全局 Git 忽略配置文件
└── README.md            # 项目说明文档
```

---

## ✨ 核心功能模块 (Features)

### 🛒 前台商城功能 (Client)
- **用户系统**: 注册、登录、个人信息修改、收货地址管理。
- **商品浏览**: 商品首页展示、轮播图广告、商品分类、商品搜索、商品详情查看。
- **购物车与下单**: 加入购物车、SKU 选择、订单结算、模拟支付。
- **订单管理**: 订单列表展示、订单状态跟踪、订单取消。
- **互动与营销**: 优惠券领取与使用、商品收藏、商品评价系统、站内通知查看。

### ⚙️ 后台管理系统 (Admin)
- **数据看板**: 数据大屏展示 (ECharts)，涵盖销售额、订单量、用户活跃度等关键指标统计。
- **商品管理**: 商品发布、上下架、SKU 管理、商品分类管理、商品数据 Excel 导入/导出。
- **订单管理**: 订单审核、发货处理、订单详情查看。
- **营销管理**: 轮播广告位配置、优惠券发放管理。
- **用户与反馈管理**: 用户列表管理、商品评价审核、用户反馈管理、系统公告发布。

### 🔐 安全与订单一致性
- **认证授权**: Spring Security + JWT + BCrypt，区分买家与管理员权限，并校验用户资源归属。
- **可信计价**: 下单金额、促销价格和优惠券均由服务端重新校验与计算，不信任客户端价格。
- **库存安全**: 使用数据库条件更新原子扣减 SKU 库存，取消订单或退款成功时按状态机恢复库存。
- **防重复下单**: 使用 Redis 幂等键拦截重复提交，并通过受控状态机限制订单状态流转。

---

## 🚀 快速启动 (Quick Start)

### 1. 环境准备
确保您的开发环境中安装了以下软件：
- **Docker & Docker Compose** (推荐，用于一键启动后端、MySQL 和 Redis)
- **Node.js** (推荐 v18+，用于运行前端项目)
- **Git** (版本控制)

### 2. 获取代码
```bash
git clone https://github.com/Zhoubin601/Emall-Platform.git
cd Emall-Platform
```

### 3. 启动后端服务 (Docker 方式)
进入 `mall-backend` 目录，我们已经为您配置好了多阶段构建的 `Dockerfile` 和自动初始化数据库的 `docker-compose.yml`。

```bash
cd mall-backend
# 创建仅供本机使用的环境变量文件，并填写数据库密码、JWT 密钥、邮箱和邮箱授权码
cp .env.example .env
# 一键启动 MySQL、Redis 以及 Spring Boot 后端
docker-compose up -d --build
```
> **注意**：`.env` 包含敏感配置，已被 Git 忽略，禁止提交到仓库。首次启动时 Docker 会自动导入数据库初始化脚本并拉取所需镜像，可能需要花费几分钟时间。后端服务默认运行在 `8080` 端口。

### 4. 启动前台商城项目 (Client)
```bash
cd ../emall-client
# 安装依赖
npm install
# 启动开发服务器
npm run dev
```

### 5. 启动后台管理系统 (Admin)
```bash
cd ../emall-admin
# 安装依赖
npm install
# 启动开发服务器
npm run dev
```

---

## 🔑 默认账号信息
*（如数据库中提供了默认演示账号，您可以在此补充登录账号和密码。如没有，可删除此项）*

- **前台测试用户**: 需自行注册或使用 `test` / `123456` (假设)
- **后台管理员**: 需自行注册或使用 `admin` / `123456` (假设)

---

## 📄 许可证 (License)
本项目遵循 [MIT](LICENSE) 开源许可证。
