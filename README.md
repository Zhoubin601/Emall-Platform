# Emall Platform (全栈电商平台项目)

[![CI](https://github.com/Zhoubin601/Emall-Platform/actions/workflows/ci.yml/badge.svg)](https://github.com/Zhoubin601/Emall-Platform/actions/workflows/ci.yml)
[![Security](https://github.com/Zhoubin601/Emall-Platform/actions/workflows/security.yml/badge.svg)](https://github.com/Zhoubin601/Emall-Platform/actions/workflows/security.yml)

[自动化测试说明](TESTING.md)

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

### ⚡ 前端性能
- **组件按需加载**: Element Plus 组件与指令按页面自动引入，避免全量注册 UI 组件库。
- **图表按需构建**: ECharts 仅注册折线图、柱状图、饼图及必要组件，并使用 Rolldown 分块缓存。

---

## 🚀 快速启动 (Quick Start)

### 1. 环境准备
确保您的开发环境中安装了以下软件：
- **Docker & Docker Compose** (推荐，用于一键启动后端、MySQL 和 Redis)
- **Node.js 22+**（用于运行前端项目）
- **Java 21 与 Maven 3.9+**（仅本地运行、测试后端时需要）
- **Git** (版本控制)

### 2. 获取代码
```bash
git clone https://github.com/Zhoubin601/Emall-Platform.git
cd Emall-Platform
```

### 3. 启动后端服务 (Docker 方式)
进入 `mall-backend` 目录。Docker Compose 会启动 MySQL、Redis 和后端；后端在数据库健康后启动，并由 Flyway 自动创建或升级表结构。

```bash
cd mall-backend
# 创建仅供本机使用的环境变量文件
cp .env.example .env
# 编辑 .env，至少填写 DB_PASSWORD 和长度不少于 32 字符的 JWT_SECRET

# 构建并启动 MySQL、Redis 与后端
docker compose up -d --build

# 确认三个服务均为 running/healthy
docker compose ps

# 查看后端启动与 Flyway 迁移日志
docker compose logs -f backend
```
> **注意**：`.env` 包含敏感配置，已被 Git 忽略，禁止提交到仓库。首次启动会拉取镜像并执行 `V0 → V1` 数据库迁移，可能需要几分钟。后端默认运行在 `8080` 端口。

### 4. 启动前台商城项目 (Client)
```bash
cd ../emall-client
# 按锁文件安装依赖
npm ci
# 启动开发服务器
npm run dev
```

### 5. 启动后台管理系统 (Admin)
```bash
cd ../emall-admin
# 按锁文件安装依赖
npm ci
# 启动开发服务器
npm run dev
```

---

## 🗄️ 数据库部署

### 推荐方式：Docker Compose + Flyway

数据库部署不再依赖 mysqldump。MySQL 第一次创建空数据库后，Spring Boot 会自动执行 `mall-backend/src/main/resources/db/migration` 中的版本化迁移：

- `V0__create_base_schema.sql`：创建 16 张基础业务表。
- `V1__add_order_coupon_and_constraints.sql`：增加订单优惠券字段、订单号唯一索引和优惠券查询索引。
- `flyway_schema_history`：记录已经执行的版本和校验和；同一迁移只执行一次。

Compose 环境中的关键配置如下：

| 配置 | 默认值/位置 | 说明 |
| --- | --- | --- |
| 数据库名 | `emall_db` | MySQL 容器首次启动时创建 |
| 宿主机端口 | `3307` | 本机工具连接使用 `localhost:3307` |
| 容器内端口 | `3306` | 后端使用 `mysql:3306` 连接 |
| 数据目录 | `mall-backend/mysql-data` | 持久化数据库文件，已被 Git 忽略 |
| `DB_PASSWORD` | `.env` 必填 | MySQL root 密码及后端数据库密码 |
| `JWT_SECRET` | `.env` 必填 | 至少 32 字符，生产环境必须随机生成 |

可以进入 MySQL 验证迁移结果；`-p` 会交互式询问密码，不要把密码直接写进命令或提交到仓库：

```bash
docker compose exec mysql mysql -uroot -p emall_db
```

进入 MySQL 后执行：

```sql
SHOW TABLES;
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

正常情况下应看到 `V0` 和 `V1` 均成功，且业务表共 16 张。

### 部署到已有或外部 MySQL

1. 先备份现有数据库，并确认目标地址不是生产库的误操作副本。
2. 创建空数据库：

   ```sql
   CREATE DATABASE emall_db
     CHARACTER SET utf8mb4
     COLLATE utf8mb4_unicode_ci;
   ```

3. 为后端设置 `DB_URL`、`DB_USERNAME`、`DB_PASSWORD`、`REDIS_HOST` 和 `REDIS_PORT`。
4. 启动后端。Flyway 会在空库执行全部迁移；对于由旧版 dump 创建且尚无迁移历史的数据库，会先记录基线版本 `0`，再执行 `V1`。
5. 检查 `flyway_schema_history` 后再开放流量。已经应用过的迁移文件禁止直接修改，应新增更高版本的迁移继续升级。

外部数据库连接示例：

```bash
export DB_URL='jdbc:mysql://db.example.com:3306/emall_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai'
export DB_USERNAME='emall_app'
export DB_PASSWORD='replace-with-a-secret'
export REDIS_HOST='redis.example.com'
export REDIS_PORT='6379'
```

### 创建首个管理员

Flyway 只创建表结构，不写入固定账号或明文密码。首次部署后：

1. 在前台注册一个普通账号，确保密码由应用使用 BCrypt 保存。
2. 进入 MySQL，将该账号提升为管理员：

   ```sql
   UPDATE sys_user
   SET role = 1
   WHERE username = 'your-admin-username';
   ```

3. 确认只更新一行，然后使用该账号登录管理后台。不要从旧 dump 导入固定管理员密码。

### 数据持久化与重建提醒

- `docker compose down` 只停止容器，不会删除 `mysql-data` 中的数据。
- 若要重建空库，请先备份，再将 `mysql-data` 移到安全的备份目录后重新启动；不要直接删除尚未确认的数据库目录。
- 仓库中的历史 dump 不再由 Compose 自动导入，也不应作为生产部署来源。

---

## ✅ 自动化质量门禁

GitHub Actions 会在主分支提交和 Pull Request 上自动执行以下检查：

- Maven `verify`：编译后端、运行测试、执行订单服务覆盖率门禁并生成可执行 JAR。
- Testcontainers：在隔离的真实 MySQL 8.0 与 Redis 7.0 中验证并发扣库存、事务回滚和幂等提交。
- JaCoCo：订单服务行覆盖率不得低于 90%，分支覆盖率不得低于 65%，CI 会保留 HTML 报告。
- 两个 Vue 应用执行 `npm ci` 和生产构建。
- Gitleaks 扫描提交历史中的密钥泄露。
- Pull Request 依赖审查阻止引入高危漏洞。
- Dependabot 每周检查 Maven、npm 和 GitHub Actions 依赖更新。

提交前可在项目根目录运行相同的本地检查：

```bash
cd mall-backend && mvn --batch-mode --no-transfer-progress verify
cd ../emall-client && npm ci && npm run build
cd ../emall-admin && npm ci && npm run build
```

---

## 🔑 账号说明

项目不在文档或代码中提供固定明文密码。前台账号可通过注册流程创建；管理员账号应根据本地初始化数据配置，并在首次使用后更换密码。

---

## 📄 许可证 (License)
本项目遵循 [MIT](LICENSE) 开源许可证。
