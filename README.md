# Emall Platform (E-MALL 全栈数字化电商平台)

[![CI](https://github.com/Zhoubin601/Emall-Platform/actions/workflows/ci.yml/badge.svg)](https://github.com/Zhoubin601/Emall-Platform/actions/workflows/ci.yml)
[![Security](https://github.com/Zhoubin601/Emall-Platform/actions/workflows/security.yml/badge.svg)](https://github.com/Zhoubin601/Emall-Platform/actions/workflows/security.yml)

[📄 全业务功能与架构实践总结报告](PROJECT_PRACTICE_SUMMARY.md) | [🧪 自动化测试说明](TESTING.md)

**E-MALL** 是一个基于现代化技术栈自主构建的企业级 B2C 全栈电商平台，包含**前台买家商城 (`emall-client`)**、**中后台运营管理系统 (`emall-admin`)** 以及 **Spring Boot 核心业务服务端 (`mall-backend`)**。项目采用前后端分离架构，提供从商品浏览、多规格 SKU 选购、限时秒杀、购物车结算、订单履约与售后退款，到买家秀评价、实时在线客服及全域数据大屏的完整电商闭环。

---

## 🏗️ 系统总体架构 (System Architecture)

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                           E-MALL 平台总体架构                            │
├───────────────────────────────────┬─────────────────────────────────────┤
│       前台商城 (emall-client)      │       后台管理系统 (emall-admin)      │
│   Vue 3 + TS + Vite + Pinia       │   Vue 3 + TS + Vite + ECharts       │
│   现代化商城/秒杀/多规格/在线客服   │   Bento 经营驾驶舱/商品/订单/RBAC   │
├───────────────────────────────────┴─────────────────────────────────────┤
│                         反向代理 / 路由接入层                            │
│              Vite Dev Proxy (/api, /uploads) / Nginx Gateway            │
├─────────────────────────────────────────────────────────────────────────┤
│                        核心服务端 (mall-backend)                         │
│   Java 21 + Spring Boot 3.2.5 + Spring Security + MyBatis-Plus 3.5.5    │
│   JWT 无状态鉴权 │ 服务端可信计价 │ 原子库存防超卖 │ EasyExcel 报表导入导出│
├───────────────────────────────────┬─────────────────────────────────────┤
│       数据持久层 (MySQL 8.x)       │        分布式缓存与幂等锁 (Redis)   │
│    InnoDB + Flyway (V0~V3) 迁移   │     会话验证码 / 热点缓存 / 订单幂等│
└───────────────────────────────────┴─────────────────────────────────────┘
```

---

## 🛠️ 技术栈 (Technology Stack)

### 🚀 后端服务 (`mall-backend`)
- **核心框架**: Java 21 + Spring Boot 3.2.5
- **安全与鉴权**: Spring Security + JWT + BCrypt（支持买家、普通管理员、超级管理员三级 RBAC）
- **持久层与迁移**: MyBatis-Plus 3.5.5 + Flyway 版本化数据库迁移 (`V0` ~ `V3`)
- **数据库与缓存**: MySQL 8.0+ / Redis 7.0+
- **邮件与报表**: Spring Mail (SMTP 587 STARTTLS 动态验证码) + Alibaba EasyExcel 3.3.2
- **测试与质量门禁**: JUnit 5 + Mockito + Testcontainers + JaCoCo 覆盖率门禁
- **容器化部署**: Docker & Docker Compose

### 💻 前台买家商城 (`emall-client`)
- **核心框架**: Vue 3 (Composition API) + TypeScript + Vite
- **状态与路由**: Pinia + Vue Router 4（含登录态路由守卫）
- **UI 与设计系统**: Element Plus（按需自动引入）+ Tech Sapphire Blue & Coral Orange 现代电商设计令牌 + 专属价格排印组件 (`EmPrice.vue`)
- **网络通信**: Axios 统一拦截器 + Vite 代理转发

### 📊 后台运营管理系统 (`emall-admin`)
- **核心框架**: Vue 3 (Composition API) + TypeScript + Vite
- **数据可视化**: Apache ECharts（按需构建与 Rolldown 分块缓存，Bento Grid 全域运营驾驶舱）
- **状态与权限**: Pinia + 角色级路由守卫（区分超级管理员 `role = 2` 与运营管理员 `role = 1`）
- **UI 组件库**: Element Plus（按需自动引入）

### 🧪 自动化测试与联调工具
- **单元测试**: Vitest (`emall-client` & `emall-admin`) + JUnit 5 (`mall-backend`)
- **端到端测试**: Playwright Chromium 真实浏览器冒烟测试 (`e2e/`)
- **轻量联调**: 内置零依赖 Node.js 接口模拟服务 (`mock-server.js`) 与内存级 RESP 协议模拟服务 (`redis-mock.js`)

---

## 📂 项目目录结构 (Project Structure)

```text
Emall-Platform/
├── emall-client/                # 前台买家商城 (Web/移动自适应，默认端口 5173)
│   └── src/
│       ├── components/          # 公共组件 (ClientHeader, EmPrice, UserCenterLayout 等)
│       ├── router/              # 路由配置与鉴权守卫
│       ├── stores/              # Pinia 状态管理 (user, cart)
│       ├── utils/               # Axios 请求封装与本地存储工具
│       └── views/               # 核心页面 (首页/详情/购物车/结算/订单/评价/个人中心等)
├── emall-admin/                 # 后台运营管理系统 (默认端口 5174)
│   └── src/
│       ├── layout/              # 后台主布局与侧边导航
│       ├── router/              # 后台路由与超管权限拦截
│       ├── stores/              # 管理员登录态管理
│       └── views/               # 业务模块 (驾驶舱/商品/分类/订单/评价/客服/轮播/公告/用户)
├── mall-backend/                # Spring Boot Java 服务端 (默认端口 8080)
│   ├── src/main/java/           # 控制器、服务层、安全配置、实体与 Mapper
│   ├── src/main/resources/      # application.yml 与 Flyway SQL 迁移脚本 (V0~V3)
│   ├── src/test/java/           # 单元测试与 Testcontainers 集成测试
│   └── docker-compose.yml       # MySQL + Redis + Backend 容器编排配置
├── e2e/                         # Playwright 端到端浏览器自动化测试套件
├── mock-server.js               # 零外部依赖的全功能 API Mock 服务 (用于快速演示/离线联调)
├── redis-mock.js                # 轻量级 Redis RESP 协议内存服务
├── start-all.ps1 / start-all.bat# Windows 一键启动全栈服务脚本
├── stop-all.ps1                 # Windows 一键停止所有服务脚本
├── PROJECT_PRACTICE_SUMMARY.md  # 全业务功能与架构实践总结报告
├── TESTING.md                   # 自动化测试执行指南
└── README.md                    # 项目说明文档
```

---

## ✨ 核心功能模块 (Core Features)

### 🛒 前台买家商城 (`emall-client`)
- **用户认证与安全**: 账号登录、邮箱验证码注册（QQ 邮箱 SMTP 587 STARTTLS 实发）、找回/重置密码、个人资料更新、多收货地址管理与默认地址设置。
- **沉浸式导购首页**: 顶部站内公告轮播与弹窗、两级商品类目树导航、热搜词榜单、大促轮播海报、限时秒杀倒计时专区、多维商品筛选与排序。
- **商品详情与多规格 SKU**: 支持多维度规格（如颜色、内存、版本）动态切换，实时联动对应 SKU 的专属售价、划线原价与剩余库存，支持商品收藏与买家秀图文评价浏览。
- **购物车与可信结算**: 本地持久化购物车、全选/单选动态计价、批量校验实时库存与价格；结算页支持满减优惠券智能匹配折抵与模拟收银台（支付宝/微信扫码支付）。
- **订单全生命周期与售后**: 订单多状态跟踪（待付款 $\rightarrow$ 待发货 $\rightarrow$ 待收货 $\rightarrow$ 已完成 / 已取消）、顺丰物流轨迹时间线查看、买家发起退款申请、确认收货及多星级晒图评价。
- **实时在线智能客服**: 全站常驻悬浮客服入口，抽屉式实时对话窗口，支持消息发送与高可用短轮询同步。

### ⚙️ 后台运营管理系统 (`emall-admin`)
- **全域数据运营驾驶舱**: 采用 Bento Grid 布局与 ECharts 图表，实时展示累计 GMV、今日实时交易额、累计订单流转量、平台有效买家数、7/30/季度销售走势曲线、订单状态环形分布、TOP 5 爆款排行及微服务集群健康态势。
- **商品与 SKU 规格中心**: 商品上架/下架、限时秒杀活动配置（特惠价、秒杀库存、活动时段）、多规格 SKU 矩阵维护、库存一键校准，以及基于 EasyExcel 的商品数据批量导入与导出。
- **类目树编排**: 一级核心大类与二级垂直子类树形结构展示、大类覆盖率统计、快捷新增子类与层级过滤。
- **订单履约与售后工作台**: 全渠道订单检索、多状态筛选、订单详情抽屉、一键发货出库、退款申请审核（同意退款自动回补库存 / 驳回申请）及订单报表导出。
- **互动、内容与客服中心**: 买家评价星级筛选与合规巡检、在线客服实时会话工作台、用户意见反馈工单回复、首页营销轮播图配置及全站公告发布。
- **多角色用户与权限管控**: 仅超级管理员 (`role = 2`) 可访问用户与权限管理模块，支持新增管理员/买家、角色分配与违规账号封禁。

### 🔐 服务端安全与交易一致性保障 (`mall-backend`)
- **认证与越权防护**: 基于 Spring Security + JWT 区分匿名接口、买家接口、普通管理员接口 (`ROLE_ADMIN`) 与超级管理员接口 (`ROLE_SUPER_ADMIN`)，严格校验订单、地址、收藏、评价等资源的用戸归属权，防止水平与垂直越权。
- **服务端可信二次计价**: 下单接口忽略客户端传入的价格字段，由服务端根据数据库实时商品/SKU 价格、活动有效期、秒杀库存及优惠券门槛重新计算应付金额。
- **高并发原子防超卖**: 采用数据库条件更新（`UPDATE ... SET stock = stock - count WHERE id = ? AND stock >= count`）原子扣减 SKU 与秒杀库存；当秒杀库存售罄时自动熔断恢复日常价，订单取消或退款通过时按状态机精准回滚库存与优惠券。
- **防重复提交与状态机约束**: 结合 Redis 幂等键拦截重复下单请求，并通过 `OrderStatusPolicy` 严格限制订单状态合法流转路径。

---

## 🚀 快速启动指南 (Quick Start)

本项目支持 **Docker 容器化启动**、**Windows 一键脚本启动** 以及 **零依赖 Mock 演示启动** 三种模式。

### 方式一：Docker Compose 启动后端 + 本地启动前端（推荐生产/标准开发）

#### 1. 环境要求
- **Docker & Docker Compose v2**
- **Node.js 22+**（运行前端与自动化测试）
- **Java 21 & Maven 3.9+**（仅本地编译或运行后端测试时需要）

#### 2. 启动后端、MySQL 与 Redis
```bash
cd mall-backend
# 复制环境变量模板并按需修改 DB_PASSWORD 和不少于 32 字符的 JWT_SECRET
cp .env.example .env

# 构建并后台启动 MySQL、Redis 与 Spring Boot 服务
docker compose up -d --build

# 查看服务运行状态与 Flyway 迁移日志
docker compose ps
docker compose logs -f backend
```
> 首次启动时，Spring Boot 会自动执行 `mall-backend/src/main/resources/db/migration` 下的 `V0 ~ V3` 数据库迁移脚本，默认监听 `http://localhost:8080`。

#### 3. 启动前台商城与管理后台
```bash
# 启动前台商城 (http://localhost:5173)
cd emall-client
npm ci
npm run dev

# 新开终端启动管理后台 (http://localhost:5174)
cd emall-admin
npm ci
npm run dev
```

> **Windows 快捷启动**：在项目根目录下可直接双击或运行 `.\start-all.bat`（基于 Docker Compose）或 `.\start-all.ps1`（基于本机 MySQL/Redis/Java 环境），停止服务可运行 `.\stop-all.ps1`。

---

### 方式二：零依赖快速体验模式（Node Mock Server）

如果本机未安装 Docker、MySQL 或 Java，可直接启动根目录下的轻量级全功能 Mock 服务，立即体验前后端完整交互：

```bash
# 1. 启动 8080 端口的全功能 API Mock 服务
node mock-server.js

# 2. 启动前台商城 (http://localhost:5173)
npm --prefix emall-client run dev

# 3. 启动管理后台 (http://localhost:5174)
npm --prefix emall-admin run dev
```

---

## 🗄️ 数据库版本化迁移 (Flyway Migrations)

项目数据库结构由 Flyway 自动管理，迁移脚本位于 `mall-backend/src/main/resources/db/migration/`：

| 版本 | 脚本文件 | 说明 |
| :---: | :--- | :--- |
| `V0` | `V0__create_base_schema.sql` | 创建 16 张核心业务基础表（用户、商品、SKU、分类、订单、评价、优惠券、客服等） |
| `V1` | `V1__add_order_coupon_and_constraints.sql` | 增加订单优惠券抵扣字段、订单号唯一约束及优惠券查询索引 |
| `V2` | `V2__enforce_unique_user_email.sql` | 清理重复邮箱并添加用户邮箱唯一性约束 |
| `V3` | `V3__add_promo_stock_and_sku_id.sql` | 增加商品限时秒杀库存 (`promo_stock`) 与订单项 `sku_id` 字段 |

### 创建首个管理员账号
Flyway 仅初始化表结构，不内置固定明文密码。首次在真实数据库部署后：
1. 在前台商城 (`http://localhost:5173/login`) 注册一个普通账号（密码经 BCrypt 加密存储）。
2. 在 MySQL 中将该账号提升为管理员（`role = 1` 为普通管理员，`role = 2` 为超级管理员）：
   ```sql
   UPDATE sys_user
   SET role = 2
   WHERE username = 'your-admin-username';
   ```

---

## ✅ 自动化测试与质量门禁 (Testing & CI)

项目配置了完善的单元测试、集成测试、E2E 浏览器冒烟测试及 GitHub Actions 持续集成流水线：

### 1. 前端单元测试与生产构建
```bash
# 运行前台与管理后台全部 Vitest 单元测试
npm test

# 分别构建前台与管理后台生产包 (含 TypeScript 类型检查)
npm --prefix emall-client run build
npm --prefix emall-admin run build
```

### 2. Playwright 端到端浏览器测试
```bash
# 首次运行需安装 Chromium 浏览器内核
npx playwright install chromium

# 运行端到端冒烟测试 (自动拉起双端开发服务器并验证核心登录与权限流)
npm run test:e2e

# 一键运行全部前端单元测试 + E2E 测试
npm run test:all
```

### 3. 后端单元测试与覆盖率校验
```bash
cd mall-backend

# 仅运行单元测试 (无需启动 Docker)
mvn test -Dtest="!*IntegrationTest"

# 完整验证 (含 Testcontainers 真实 MySQL/Redis 并发与幂等集成测试 + JaCoCo 覆盖率门禁)
mvn --batch-mode --no-transfer-progress verify
```

---

## 📄 许可证 (License)

本项目遵循 [MIT](LICENSE) 开源许可证。
