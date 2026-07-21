# 自动化测试

项目使用 JUnit、Mockito 和 JaCoCo 验证后端核心业务，使用 Vitest 测试前端状态管理和路由鉴权，并使用 Playwright 在真实 Chromium 中验证前台商城与管理后台的关键登录流程。

## 后端测试与覆盖率

后端完整验证需要本机 Docker Engine 处于运行状态。Testcontainers 会自动创建并销毁隔离的 MySQL 8.0 和 Redis 7.0 容器，不会连接或修改开发数据库。

```bash
cd mall-backend
mvn --batch-mode --no-transfer-progress verify
```

订单服务测试覆盖服务端计价、促销价、优惠券归属与门槛、原子库存扣减、订单状态并发、库存回滚和幂等提交。其中 Testcontainers 集成测试从空 MySQL 开始执行真实 Flyway 迁移，并证明并发库存更新只能成功一次、失败订单完整回滚，以及成功订单与 Redis 幂等结果一致。`verify` 会生成 `target/site/jacoco/index.html`，并要求 `OrderService` 行覆盖率不低于 90%、分支覆盖率不低于 65%。

## 单元测试

在项目根目录运行两端的全部单元测试：

```bash
npm test
```

也可以分别运行：

```bash
npm test --prefix emall-client
npm test --prefix emall-admin
```

## 浏览器冒烟测试

首次运行前安装 Chromium：

```bash
npx playwright install chromium
```

随后执行：

```bash
npm run test:e2e
```

Playwright 会自动启动两个 Vite 开发服务器。测试会拦截并模拟所需 API，因此无需启动 Java 后端、MySQL 或 Redis。

## 全部前端测试

```bash
npm run test:all
```

GitHub Actions 会在每次推送到 `main` 和每个 Pull Request 中自动执行单元测试、生产构建及 Chromium 冒烟测试，并在浏览器测试结束后上传 Playwright HTML 报告。
