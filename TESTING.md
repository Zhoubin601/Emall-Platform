# 自动化测试

项目使用 Vitest 测试前端状态管理和路由鉴权，使用 Playwright 在真实 Chromium 中验证前台商城与管理后台的关键登录流程。

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
