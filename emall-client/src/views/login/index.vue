<script setup lang="ts">
import { ref, reactive, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  User, Lock, Message, Key, EditPen, ShoppingBag, ArrowLeft,
  CircleCheckFilled, Van, Medal
} from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()

// 状态升级：'login' | 'register' | 'forgot'
const viewMode = ref<'login' | 'register' | 'forgot'>('login')
const loading = ref(false)
const countdown = ref(0)
let timer: any = null

// --- 表单数据 ---
const loginForm = reactive({ username: '', password: '' })
const regForm = reactive({ username: '', nickname: '', email: '', code: '', password: '' })
const resetForm = reactive({ email: '', code: '', password: '' })
const agreeTerms = ref(true)

// 快速填入测试账号
const fillTestAccount = (user: string, pass: string) => {
  loginForm.username = user
  loginForm.password = pass
  ElMessage.info(`已填入测试账号：${user}`)
}

// 1. 发送验证码 (注册和找回密码共用)
const handleSendCode = async () => {
  const email = viewMode.value === 'register' ? regForm.email : resetForm.email
  if (!email) return ElMessage.warning('请先输入正确的安全邮箱')
  
  // 简易正则校验
  const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailPattern.test(email)) return ElMessage.warning('邮箱格式不正确')

  try {
    await request.post('/user/sendCode', null, { params: { email } })
    ElMessage.success('验证码已发送至邮箱，请查收！')
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer)
        countdown.value = 0
      }
    }, 1000)
  } catch (e) {
    ElMessage.error('验证码发送失败，请确认后端服务正常')
  }
}

// 2. 注册账号
const handleRegister = async () => {
  if (!agreeTerms.value) {
    return ElMessage.warning('请先阅读并勾选用户协议与隐私条款')
  }
  if (!regForm.username || !regForm.nickname || !regForm.code || !regForm.password) {
    return ElMessage.warning('请将注册信息填写完整')
  }
  if (regForm.password.length < 8) return ElMessage.warning('为保证安全，密码至少需 8 个字符')
  
  loading.value = true
  try {
    await request.post('/user/register', regForm)
    ElMessage.success('🎉 注册成功！已为您自动切换至登录页面')
    viewMode.value = 'login'
    loginForm.username = regForm.username
    loginForm.password = ''
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '注册失败：验证码错误或用户名已存在')
  } finally {
    loading.value = false
  }
}

// 3. 登录账号
const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    return ElMessage.warning('请输入用户名和密码')
  }
  loading.value = true
  try {
    const res: any = await request.post('/user/login', loginForm)
    userStore.setLogin(res.token, res.user)
    ElMessage.success(`欢迎回到易商城，${res.user.nickname || res.user.username}！`)
    router.push('/')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '登录失败：账号或密码错误')
  } finally {
    loading.value = false
  }
}

// 4. 找回重置密码
const handleResetPassword = async () => {
  if (!resetForm.email || !resetForm.code || !resetForm.password) {
    return ElMessage.warning('请将验证信息填写完整')
  }
  if (resetForm.password.length < 8) return ElMessage.warning('新密码至少需 8 个字符')
  loading.value = true
  try {
    await request.post('/user/resetPassword', resetForm)
    ElMessage.success('密码重置成功！请使用新密码登录')
    resetForm.code = ''
    resetForm.password = ''
    viewMode.value = 'login'
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '重置失败：验证码错误或邮箱未绑定')
  } finally {
    loading.value = false
  }
}

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="auth-page">
    <!-- 背景流光与微粒 -->
    <div class="glow-orb orb-1"></div>
    <div class="glow-orb orb-2"></div>
    <div class="glow-orb orb-3"></div>

    <!-- 顶栏快捷导航 -->
    <header class="auth-topbar">
      <div class="topbar-container">
        <router-link to="/" class="topbar-brand">
          <h1 class="brand-text">E-MALL</h1>
          <span class="brand-dot"></span>
          <span class="brand-sub">官方臻选商城</span>
        </router-link>
        <router-link to="/" class="back-home-link">
          <el-icon><ArrowLeft /></el-icon>
          <span>返回商城首页</span>
        </router-link>
      </div>
    </header>

    <!-- 主展示双栏卡片 -->
    <main class="auth-card-container">
      <div class="auth-card">
        <!-- 左侧：品牌视觉展翼区 -->
        <div class="brand-showcase-panel">
          <div class="panel-overlay"></div>
          <div class="panel-content">
            <div class="showcase-logo">
              <div class="logo-badge">
                <el-icon><ShoppingBag /></el-icon>
              </div>
              <div>
                <h2 class="showcase-title">易商城 · 极物</h2>
                <p class="showcase-subtitle">E-MALL PLATFORM 2026</p>
              </div>
            </div>

            <div class="slogan-box">
              <h3 class="slogan-main">探索全球尖货<br>让每一刻生活熠熠生辉</h3>
              <p class="slogan-desc">汇聚数码智酷、潮流美学与匠心好物，为您提供全天候尊崇选购体验与无忧售后保障。</p>
            </div>

            <!-- 特权特性徽标 -->
            <div class="feature-pills">
              <div class="feature-item">
                <div class="feature-icon"><el-icon><CircleCheckFilled /></el-icon></div>
                <div class="feature-text">
                  <strong>正品自营直采</strong>
                  <span>100% 品牌官方直发</span>
                </div>
              </div>
              <div class="feature-item">
                <div class="feature-icon"><el-icon><Van /></el-icon></div>
                <div class="feature-text">
                  <strong>顺丰闪电速递</strong>
                  <span>特快专线 次日必达</span>
                </div>
              </div>
              <div class="feature-item">
                <div class="feature-icon"><el-icon><Medal /></el-icon></div>
                <div class="feature-text">
                  <strong>尊享 VIP 礼遇</strong>
                  <span>专属折上折与双倍积分</span>
                </div>
              </div>
            </div>

            <div class="trust-footer">
              <div class="trust-stars">★★★★★</div>
              <p>超 100,000+ 挑剔品味用户的共同选择</p>
            </div>
          </div>
        </div>

        <!-- 右侧：表单操作交互区 -->
        <div class="form-panel">
          <!-- 切换 Tab (登录 / 注册) -->
          <div class="form-header" v-if="viewMode !== 'forgot'">
            <div class="auth-tabs">
              <button
                class="tab-btn"
                :class="{ active: viewMode === 'login' }"
                @click="viewMode = 'login'"
              >
                <span>账号登录</span>
                <div class="tab-indicator" v-if="viewMode === 'login'"></div>
              </button>
              <button
                class="tab-btn"
                :class="{ active: viewMode === 'register' }"
                @click="viewMode = 'register'"
              >
                <span>新用户注册</span>
                <div class="tab-indicator" v-if="viewMode === 'register'"></div>
              </button>
            </div>
            <p class="mode-tip">
              {{ viewMode === 'login' ? '欢迎回来，请输入您的登录凭证' : '只需 30 秒，即可解锁专属会员特权' }}
            </p>
          </div>

          <!-- 忘记密码标题区 -->
          <div class="form-header-forgot" v-else>
            <div class="back-link-btn" @click="viewMode = 'login'">
              <el-icon><ArrowLeft /></el-icon>
              <span>返回登录</span>
            </div>
            <h3 class="forgot-title">安全验证与密码重置</h3>
            <p class="mode-tip">请输入您绑定的安全邮箱以接收验证码</p>
          </div>

          <!-- 登录表单 -->
          <transition name="form-fade" mode="out-in">
            <div v-if="viewMode === 'login'" class="form-wrapper" key="login">
              <div class="input-group">
                <label class="input-label">账号 / 用户名</label>
                <el-input
                  v-model="loginForm.username"
                  placeholder="请输入登录账号"
                  :prefix-icon="User"
                  size="large"
                  class="premium-input"
                />
              </div>

              <div class="input-group mt-4">
                <div class="label-with-action">
                  <label class="input-label">登录密码</label>
                  <span class="link-text" @click="viewMode = 'forgot'">忘记密码？</span>
                </div>
                <el-input
                  v-model="loginForm.password"
                  type="password"
                  placeholder="请输入登录密码"
                  :prefix-icon="Lock"
                  size="large"
                  show-password
                  class="premium-input"
                  @keyup.enter="handleLogin"
                />
              </div>

              <!-- 快速填充测试账号提示 -->
              <div class="test-account-hint">
                <span class="hint-label">快速体验：</span>
                <button class="hint-chip" @click="fillTestAccount('601', '123')">普通买家 (601)</button>
                <button class="hint-chip" @click="fillTestAccount('admin', '123')">系统管理员 (admin)</button>
              </div>

              <el-button
                type="primary"
                class="submit-gradient-btn mt-6"
                :loading="loading"
                @click="handleLogin"
              >
                开启购物之旅
              </el-button>

              <div class="terms-notice mt-4">
                登录即代表您已同意
                <a href="javascript:void(0)">《用户服务协议》</a> 与
                <a href="javascript:void(0)">《隐私保护政策》</a>
              </div>
            </div>

            <!-- 注册表单 -->
            <div v-else-if="viewMode === 'register'" class="form-wrapper" key="register">
              <div class="input-group">
                <label class="input-label">设置用户名</label>
                <el-input
                  v-model="regForm.username"
                  placeholder="建议使用 4-16 位字母或数字"
                  :prefix-icon="User"
                  size="large"
                  class="premium-input"
                />
              </div>

              <div class="input-group mt-3">
                <label class="input-label">个性昵称</label>
                <el-input
                  v-model="regForm.nickname"
                  placeholder="如：极客星人、周同学"
                  :prefix-icon="EditPen"
                  size="large"
                  class="premium-input"
                />
              </div>

              <div class="input-group mt-3">
                <label class="input-label">安全邮箱 (找回凭证)</label>
                <el-input
                  v-model="regForm.email"
                  placeholder="请输入您的常用有效邮箱"
                  :prefix-icon="Message"
                  size="large"
                  class="premium-input"
                />
              </div>

              <div class="input-group mt-3">
                <label class="input-label">邮箱验证码</label>
                <div class="code-input-row">
                  <el-input
                    v-model="regForm.code"
                    placeholder="输入 6 位数字验证码"
                    :prefix-icon="Key"
                    size="large"
                    class="premium-input code-flex"
                  />
                  <el-button
                    class="send-code-btn"
                    :disabled="countdown > 0"
                    @click="handleSendCode"
                  >
                    {{ countdown > 0 ? `${countdown}s 后重试` : '获取验证码' }}
                  </el-button>
                </div>
              </div>

              <div class="input-group mt-3">
                <label class="input-label">登录密码 (至少8位)</label>
                <el-input
                  v-model="regForm.password"
                  type="password"
                  placeholder="建议组合英文字母与数字"
                  :prefix-icon="Lock"
                  size="large"
                  show-password
                  class="premium-input"
                />
              </div>

              <div class="checkbox-row mt-4">
                <el-checkbox v-model="agreeTerms">
                  <span class="terms-text">我已认真阅读并完全同意 <a href="javascript:void(0)">《用户服务协议》</a> 与 <a href="javascript:void(0)">《隐私政策》</a></span>
                </el-checkbox>
              </div>

              <el-button
                type="primary"
                class="submit-gradient-btn mt-4"
                :loading="loading"
                @click="handleRegister"
              >
                立即完成注册
              </el-button>
            </div>

            <!-- 找回重置密码表单 -->
            <div v-else class="form-wrapper" key="forgot">
              <div class="input-group">
                <label class="input-label">绑定的安全邮箱</label>
                <el-input
                  v-model="resetForm.email"
                  placeholder="请输入账号绑定的邮箱地址"
                  :prefix-icon="Message"
                  size="large"
                  class="premium-input"
                />
              </div>

              <div class="input-group mt-4">
                <label class="input-label">安全验证码</label>
                <div class="code-input-row">
                  <el-input
                    v-model="resetForm.code"
                    placeholder="6 位邮箱验证码"
                    :prefix-icon="Key"
                    size="large"
                    class="premium-input code-flex"
                  />
                  <el-button
                    class="send-code-btn"
                    :disabled="countdown > 0"
                    @click="handleSendCode"
                  >
                    {{ countdown > 0 ? `${countdown}s 后重试` : '获取验证码' }}
                  </el-button>
                </div>
              </div>

              <div class="input-group mt-4">
                <label class="input-label">设定新密码</label>
                <el-input
                  v-model="resetForm.password"
                  type="password"
                  placeholder="请输入不少于 8 位的新密码"
                  :prefix-icon="Lock"
                  size="large"
                  show-password
                  class="premium-input"
                />
              </div>

              <el-button
                type="primary"
                class="submit-gradient-btn mt-6"
                :loading="loading"
                @click="handleResetPassword"
              >
                验证并确认重置密码
              </el-button>
            </div>
          </transition>
        </div>
      </div>
    </main>

    <!-- 页脚版权与备案信息 -->
    <footer class="auth-footer">
      <div class="footer-links">
        <a href="javascript:void(0)">关于易商城</a>
        <span class="sep">·</span>
        <a href="javascript:void(0)">服务条款</a>
        <span class="sep">·</span>
        <a href="javascript:void(0)">隐私政策</a>
        <span class="sep">·</span>
        <a href="javascript:void(0)">知识产权维权</a>
        <span class="sep">·</span>
        <a href="javascript:void(0)">联系客服</a>
      </div>
      <p class="copyright">Copyright © 2026 E-MALL Platform. All Rights Reserved. 浙ICP备20268888号</p>
    </footer>
  </div>
</template>

<style scoped>
/* 1. 页面基础 */
.auth-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background: #0f172a;
  background-image: 
    radial-gradient(at 0% 0%, rgba(2, 132, 199, 0.2) 0px, transparent 50%),
    radial-gradient(at 100% 100%, rgba(14, 165, 233, 0.15) 0px, transparent 50%),
    radial-gradient(at 50% 50%, rgba(15, 23, 42, 1) 0px, rgba(2, 6, 23, 1) 100%);
  position: relative;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "PingFang SC", "Hiragino Sans GB", "Microsoft YaHei", sans-serif;
}

/* 背景光效球 */
.glow-orb {
  position: absolute;
  border-radius: 50%;
  filter: blur(100px);
  pointer-events: none;
  z-index: 0;
}
.orb-1 {
  width: 500px;
  height: 500px;
  background: rgba(2, 132, 199, 0.25);
  top: -150px;
  left: -150px;
}
.orb-2 {
  width: 450px;
  height: 450px;
  background: rgba(14, 165, 233, 0.18);
  bottom: -100px;
  right: -100px;
}
.orb-3 {
  width: 300px;
  height: 300px;
  background: rgba(56, 189, 248, 0.15);
  top: 40%;
  right: 25%;
}

/* 2. 顶部微导航栏 */
.auth-topbar {
  position: relative;
  z-index: 10;
  padding: 16px 0;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(10px);
  background: rgba(15, 23, 42, 0.4);
}
.topbar-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.topbar-brand {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
}
.topbar-brand .brand-text {
  margin: 0;
  line-height: 1;
  font-size: 20px;
  font-weight: 900;
  letter-spacing: 1px;
  background: linear-gradient(135deg, #0284c7 0%, #38bdf8 100%);
  -webkit-background-clip: text;
  color: transparent;
}
.topbar-brand .brand-dot {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #94a3b8;
}
.topbar-brand .brand-sub {
  font-size: 13px;
  color: #94a3b8;
  font-weight: 500;
}
.back-home-link {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #cbd5e1;
  text-decoration: none;
  font-size: 13px;
  padding: 6px 14px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.1);
  transition: all 0.25s ease;
}
.back-home-link:hover {
  color: #fff;
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(255, 255, 255, 0.25);
  transform: translateX(-2px);
}

/* 3. 卡片容器与双栏设计 */
.auth-card-container {
  position: relative;
  z-index: 10;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 20px;
}
.auth-card {
  width: 1000px;
  max-width: 100%;
  min-height: 600px;
  display: flex;
  border-radius: 24px;
  overflow: hidden;
  background: #ffffff;
  box-shadow: 
    0 25px 50px -12px rgba(0, 0, 0, 0.4),
    0 0 0 1px rgba(255, 255, 255, 0.1);
}

/* 左侧品牌展翼面板 */
.brand-showcase-panel {
  flex: 1;
  position: relative;
  background: linear-gradient(145deg, #0c4a6e 0%, #0f172a 60%, #0369a1 100%);
  color: #ffffff;
  padding: 48px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  overflow: hidden;
}
.panel-overlay {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 20% 30%, rgba(14, 165, 233, 0.25) 0%, transparent 60%);
  pointer-events: none;
}
.panel-content {
  position: relative;
  z-index: 2;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}
.showcase-logo {
  display: flex;
  align-items: center;
  gap: 14px;
}
.logo-badge {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, #0284c7, #0ea5e9);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  color: #ffffff;
  box-shadow: 0 8px 16px rgba(2, 132, 199, 0.4);
}
.showcase-title {
  font-size: 18px;
  font-weight: 800;
  letter-spacing: 0.5px;
  margin: 0;
  color: #f8fafc;
}
.showcase-subtitle {
  font-size: 11px;
  color: #94a3b8;
  margin: 2px 0 0 0;
  letter-spacing: 1.5px;
}
.slogan-box {
  margin: 36px 0 28px;
}
.slogan-main {
  font-size: 26px;
  font-weight: 800;
  line-height: 1.35;
  margin: 0 0 12px;
  background: linear-gradient(135deg, #ffffff 30%, #7dd3fc 100%);
  -webkit-background-clip: text;
  color: transparent;
}
.slogan-desc {
  font-size: 13px;
  line-height: 1.65;
  color: #cbd5e1;
  margin: 0;
}

.feature-pills {
  display: flex;
  flex-direction: column;
  gap: 16px;
}
.feature-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 16px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(8px);
}
.feature-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: rgba(14, 165, 233, 0.2);
  color: #38bdf8;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
}
.feature-text strong {
  display: block;
  font-size: 13px;
  color: #f8fafc;
  font-weight: 600;
}
.feature-text span {
  font-size: 11px;
  color: #94a3b8;
}
.trust-footer {
  margin-top: 32px;
  padding-top: 18px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}
.trust-stars {
  color: #fbbf24;
  font-size: 14px;
  letter-spacing: 2px;
}
.trust-footer p {
  font-size: 12px;
  color: #94a3b8;
  margin: 4px 0 0;
}

/* 右侧表单操作区 */
.form-panel {
  flex: 1.15;
  padding: 48px 44px;
  background: #ffffff;
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.form-header {
  margin-bottom: 24px;
}
.auth-tabs {
  display: flex;
  gap: 28px;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 12px;
}
.tab-btn {
  background: none;
  border: none;
  font-size: 18px;
  font-weight: 700;
  color: #94a3b8;
  cursor: pointer;
  position: relative;
  padding: 0 0 8px;
  transition: all 0.2s ease;
}
.tab-btn.active {
  color: #0f172a;
}
.tab-indicator {
  position: absolute;
  bottom: -13px;
  left: 0;
  width: 100%;
  height: 3px;
  border-radius: 2px;
  background: linear-gradient(90deg, #0284c7, #0ea5e9);
}
.mode-tip {
  font-size: 13px;
  color: #64748b;
  margin: 12px 0 0;
}

.form-header-forgot {
  margin-bottom: 24px;
}
.back-link-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #64748b;
  cursor: pointer;
  margin-bottom: 10px;
  transition: color 0.2s;
}
.back-link-btn:hover {
  color: #0284c7;
}
.forgot-title {
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
  margin: 0;
}

/* 输入框规范 */
.input-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.input-label {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}
.label-with-action {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.link-text {
  font-size: 12px;
  color: #0284c7;
  cursor: pointer;
  font-weight: 500;
}
.link-text:hover {
  text-decoration: underline;
}

.premium-input :deep(.el-input__wrapper) {
  border-radius: 12px;
  background-color: #f8fafc;
  border: 1px solid #e2e8f0;
  box-shadow: none !important;
  padding: 4px 14px;
  transition: all 0.2s ease;
}
.premium-input :deep(.el-input__wrapper:hover) {
  background-color: #ffffff;
  border-color: #cbd5e1;
}
.premium-input :deep(.el-input__wrapper.is-focus) {
  background-color: #ffffff;
  border-color: #0284c7;
  box-shadow: 0 0 0 3px rgba(2, 132, 199, 0.15) !important;
}

.code-input-row {
  display: flex;
  gap: 10px;
}
.code-flex {
  flex: 1;
}
.send-code-btn {
  height: 40px;
  border-radius: 12px;
  padding: 0 16px;
  font-weight: 600;
  font-size: 13px;
  border-color: #bae6fd;
  color: #0284c7;
  background: #f0f9ff;
  transition: all 0.2s ease;
}
.send-code-btn:hover:not(:disabled) {
  background: #e0f2fe;
  border-color: #7dd3fc;
  color: #0369a1;
}

/* 快速填充芯片 */
.test-account-hint {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 14px;
  padding: 8px 12px;
  border-radius: 8px;
  background: #f8fafc;
  border: 1px dashed #e2e8f0;
}
.hint-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
}
.hint-chip {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 6px;
  padding: 3px 8px;
  font-size: 11px;
  color: #0284c7;
  cursor: pointer;
  font-weight: 600;
  transition: all 0.2s ease;
}
.hint-chip:hover {
  background: #e0f2fe;
  border-color: #7dd3fc;
  color: #0369a1;
}

/* 提交按钮 */
.submit-gradient-btn {
  width: 100%;
  height: 48px;
  border-radius: 14px;
  font-size: 15px;
  font-weight: 700;
  letter-spacing: 1px;
  background: linear-gradient(135deg, #0284c7 0%, #0ea5e9 100%);
  border: none;
  box-shadow: 0 8px 20px rgba(2, 132, 199, 0.3);
  transition: all 0.25s ease;
}
.submit-gradient-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 12px 24px rgba(2, 132, 199, 0.4);
  background: linear-gradient(135deg, #0369a1 0%, #0284c7 100%);
}

.checkbox-row {
  display: flex;
  align-items: center;
}
.terms-text {
  font-size: 12px;
  color: #64748b;
}
.terms-text a {
  color: #0284c7;
  text-decoration: none;
}
.terms-notice {
  text-align: center;
  font-size: 12px;
  color: #94a3b8;
}
.terms-notice a {
  color: #64748b;
  text-decoration: none;
}
.terms-notice a:hover {
  color: #0284c7;
}

/* 4. 页脚 */
.auth-footer {
  position: relative;
  z-index: 10;
  text-align: center;
  padding: 20px 0 28px;
}
.footer-links {
  display: flex;
  justify-content: center;
  gap: 12px;
  align-items: center;
}
.footer-links a {
  color: #94a3b8;
  font-size: 12px;
  text-decoration: none;
  transition: color 0.2s;
}
.footer-links a:hover {
  color: #e2e8f0;
}
.footer-links .sep {
  color: #475569;
  font-size: 12px;
}
.copyright {
  font-size: 12px;
  color: #64748b;
  margin: 8px 0 0;
}

/* 动画与工具类 */
.mt-3 { margin-top: 12px; }
.mt-4 { margin-top: 16px; }
.mt-6 { margin-top: 24px; }

.form-fade-enter-active,
.form-fade-leave-active {
  transition: all 0.2s ease;
}
.form-fade-enter-from {
  opacity: 0;
  transform: translateY(8px);
}
.form-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

@media (max-width: 860px) {
  .brand-showcase-panel {
    display: none;
  }
  .auth-card {
    width: 460px;
  }
}
</style>
