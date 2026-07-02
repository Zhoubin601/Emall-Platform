<script setup lang="ts">
import { ref, reactive, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Message, Key, EditPen, ShoppingBag, ArrowLeft } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import request from '../../utils/request'
import { useUserStore } from '../../stores/user'

const router = useRouter()
const userStore = useUserStore()

// ✨ 状态升级：'login' | 'register' | 'forgot'
const viewMode = ref<'login' | 'register' | 'forgot'>('login')
const loading = ref(false)
const countdown = ref(0)
let timer: any = null

// --- 表单数据 ---
const loginForm = reactive({ username: '', password: '' })
const regForm = reactive({ username: '', nickname: '', email: '', code: '', password: '' })
// ✨ 新增：找回密码表单
const resetForm = reactive({ email: '', code: '', password: '' })

// --- 核心业务逻辑 ---

// 1. 发送验证码 (注册和找回密码共用)
const handleSendCode = async () => {
  // 根据当前所在页面，判断取哪个邮箱
  const email = viewMode.value === 'register' ? regForm.email : resetForm.email
  if (!email) return ElMessage.warning('请先输入正确的邮箱地址')
  
  try {
    await request.post('/user/sendCode', null, { params: { email } })
    ElMessage.success('验证码已发送，请查收！')
    countdown.value = 60
    timer = setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
  } catch (e) { ElMessage.error('发送失败，请检查后端是否正常') }
}

// 2. 注册账号
const handleRegister = async () => {
  if (!regForm.username || !regForm.nickname || !regForm.code || !regForm.password) {
    return ElMessage.warning('请将注册信息填写完整')
  }
  loading.value = true
  try {
    await request.post('/user/register', regForm)
    ElMessage.success('注册成功！快去登录吧')
    viewMode.value = 'login'
    loginForm.username = regForm.username
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '注册失败：验证码错误或账号重复')
  } finally { loading.value = false }
}

// 3. 登录账号
const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) return ElMessage.warning('请输入账号密码')
  loading.value = true
  try {
    const res: any = await request.post('/user/login', loginForm)
    userStore.setUser(res)
    ElMessage.success(`欢迎回来，${res.nickname || res.username}`)
    router.push('/')
  } catch (e) { ElMessage.error('登录失败：账号或密码错误') }
  finally { loading.value = false }
}

// 4. ✨ 找回密码
const handleResetPassword = async () => {
  if (!resetForm.email || !resetForm.code || !resetForm.password) {
    return ElMessage.warning('请将验证信息填写完整')
  }
  loading.value = true
  try {
    // 你的后端刚好是用 RegisterRequest 接收的，包含 email, code, password
    await request.post('/user/resetPassword', resetForm)
    ElMessage.success('密码重置成功！请使用新密码登录')
    // 重置成功后，清空表单并切回登录页
    resetForm.code = ''
    resetForm.password = ''
    viewMode.value = 'login'
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '重置失败：验证码错误或邮箱未注册')
  } finally { loading.value = false }
}

onUnmounted(() => { if (timer) clearInterval(timer) })
</script>

<template>
  <div class="login-page">
    <div class="bg-blob blob-1"></div>
    <div class="bg-blob blob-2"></div>
    <div class="bg-blob blob-3"></div>

    <el-card class="glass-card" shadow="never">
      <div class="brand-header">
        <div class="logo-icon"><el-icon><ShoppingBag /></el-icon></div>
        <h1 class="brand-title">E-MALL</h1>
        <p class="brand-subtitle">遇见你心仪的全球好物</p>
      </div>

      <div class="tab-wrapper" v-if="viewMode !== 'forgot'">
        <div class="tab-item" :class="{ active: viewMode === 'login' }" @click="viewMode = 'login'">登 录</div>
        <div class="tab-item" :class="{ active: viewMode === 'register' }" @click="viewMode = 'register'">注 册</div>
      </div>

      <transition name="fade-transform" mode="out-in">
        <div v-if="viewMode === 'login'" class="form-body" key="login">
          <el-input v-model="loginForm.username" placeholder="请输入登录账号" :prefix-icon="User" size="large" class="custom-input" />
          <el-input v-model="loginForm.password" type="password" placeholder="请输入登录密码" :prefix-icon="Lock" size="large" show-password class="custom-input mt-4" @keyup.enter="handleLogin" />
          
          <div class="forgot-pwd-row">
            <span class="forgot-link" @click="viewMode = 'forgot'">忘记密码？</span>
          </div>

          <el-button type="primary" class="action-btn mt-4" :loading="loading" @click="handleLogin">开启购物之旅</el-button>
        </div>

        <div v-else-if="viewMode === 'register'" class="form-body" key="register">
          <el-input v-model="regForm.username" placeholder="设置登录账号" :prefix-icon="User" size="large" class="custom-input" />
          <el-input v-model="regForm.nickname" placeholder="您的称呼 (如：周同学)" :prefix-icon="EditPen" size="large" class="custom-input mt-4" />
          <el-input v-model="regForm.email" placeholder="安全邮箱地址 (找回密码凭证)" :prefix-icon="Message" size="large" class="custom-input mt-4" />
          
          <div class="code-box mt-4">
            <el-input v-model="regForm.code" placeholder="六位验证码" :prefix-icon="Key" size="large" class="custom-input" />
            <el-button class="code-btn" :disabled="countdown > 0" @click="handleSendCode">
              {{ countdown > 0 ? `${countdown}s` : '获取' }}
            </el-button>
          </div>
          
          <el-input v-model="regForm.password" type="password" placeholder="设置登录密码" :prefix-icon="Lock" size="large" show-password class="custom-input mt-4" />
          <el-button type="primary" class="action-btn mt-8" :loading="loading" @click="handleRegister">注 册 账 号</el-button>
        </div>

        <div v-else class="form-body" key="forgot">
          <div class="back-login-row">
            <span class="back-link" @click="viewMode = 'login'">
              <el-icon><ArrowLeft /></el-icon> 返回登录
            </span>
            <span class="forgot-title">安全验证</span>
          </div>

          <el-input v-model="resetForm.email" placeholder="请输入绑定的邮箱" :prefix-icon="Message" size="large" class="custom-input mt-4" />
          
          <div class="code-box mt-4">
            <el-input v-model="resetForm.code" placeholder="六位验证码" :prefix-icon="Key" size="large" class="custom-input" />
            <el-button class="code-btn" :disabled="countdown > 0" @click="handleSendCode">
              {{ countdown > 0 ? `${countdown}s` : '获取' }}
            </el-button>
          </div>

          <el-input v-model="resetForm.password" type="password" placeholder="设置新的登录密码" :prefix-icon="Lock" size="large" show-password class="custom-input mt-4" />
          
          <el-button type="primary" class="action-btn action-btn-reset mt-8" :loading="loading" @click="handleResetPassword">确 认 重 置</el-button>
        </div>
      </transition>
    </el-card>

    <div class="footer-tips">© 2026 E-MALL Digital Shop. All Rights Reserved.</div>
  </div>
</template>

<style scoped>
/* 1. 基础布局与背景设计 */
.login-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background-color: #f0f9ff;
  overflow: hidden;
  position: relative;
}

.bg-blob {
  position: absolute;
  filter: blur(80px);
  z-index: 0;
  border-radius: 50%;
  opacity: 0.6;
}
.blob-1 { width: 400px; height: 400px; background: #bae6fd; top: -100px; left: -100px; }
.blob-2 { width: 300px; height: 300px; background: #e0f2fe; bottom: -50px; right: 10%; }
.blob-3 { width: 250px; height: 250px; background: #7dd3fc; top: 20%; right: -50px; }

/* 2. 卡片与品牌 */
.glass-card {
  width: 420px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.7);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.5);
  box-shadow: 0 20px 50px rgba(2, 132, 199, 0.1);
  padding: 20px 10px;
  z-index: 1;
}

.brand-header { text-align: center; margin-bottom: 30px; }
.logo-icon {
  font-size: 32px; color: #0ea5e9; background: #f0f9ff; width: 60px; height: 60px;
  display: flex; align-items: center; justify-content: center; border-radius: 18px; margin: 0 auto 15px;
}
.brand-title {
  font-size: 28px; font-weight: 900; letter-spacing: 2px; margin: 0;
  background: linear-gradient(135deg, #0369a1, #38bdf8); -webkit-background-clip: text; color: transparent;
}
.brand-subtitle { font-size: 13px; color: #94a3b8; margin-top: 5px; }

/* 3. Tab 切换 */
.tab-wrapper {
  display: flex; background: #f1f5f9; border-radius: 14px; padding: 5px; margin: 0 20px 30px;
}
.tab-item {
  flex: 1; text-align: center; padding: 10px; cursor: pointer;
  font-weight: bold; font-size: 14px; color: #64748b; transition: all 0.3s; border-radius: 10px;
}
.tab-item.active { background: #fff; color: #0284c7; box-shadow: 0 4px 10px rgba(0, 0, 0, 0.05); }

/* 4. 表单与按钮 */
.form-body { padding: 0 20px 10px; }
.custom-input :deep(.el-input__wrapper) {
  border-radius: 12px; background-color: rgba(255, 255, 255, 0.8);
  box-shadow: none !important; border: 1px solid #e2e8f0;
}
.custom-input :deep(.el-input__wrapper.is-focus) { border-color: #0ea5e9; }

.code-box { display: flex; gap: 12px; }
.code-btn { border-radius: 12px; padding: 0 20px; border-color: #e2e8f0; color: #0284c7; font-weight: bold; }

.action-btn {
  width: 100%; height: 50px; border-radius: 15px; font-size: 16px; font-weight: bold;
  background: linear-gradient(135deg, #0ea5e9, #0284c7); border: none;
  box-shadow: 0 10px 20px rgba(14, 165, 233, 0.2); transition: all 0.3s;
}
.action-btn:hover { transform: translateY(-2px); box-shadow: 0 15px 25px rgba(14, 165, 233, 0.3); }

.action-btn-reset { background: linear-gradient(135deg, #10b981, #059669); box-shadow: 0 10px 20px rgba(16, 185, 129, 0.2); }
.action-btn-reset:hover { box-shadow: 0 15px 25px rgba(16, 185, 129, 0.3); }

/* 5. 链接与间距 */
.forgot-pwd-row { display: flex; justify-content: flex-end; margin-top: 10px; }
.forgot-link { font-size: 13px; color: #0ea5e9; cursor: pointer; transition: color 0.2s; }
.forgot-link:hover { color: #0284c7; text-decoration: underline; }

.back-login-row { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.back-link { font-size: 14px; color: #64748b; cursor: pointer; display: flex; align-items: center; gap: 4px; transition: color 0.2s; }
.back-link:hover { color: #0f172a; }
.forgot-title { font-weight: bold; color: #0f172a; font-size: 15px; }

.mt-4 { margin-top: 15px; }
.mt-8 { margin-top: 30px; }
.footer-tips { margin-top: 30px; font-size: 12px; color: #94a3b8; z-index: 1; }

/* 6. 动画 */
.fade-transform-enter-active, .fade-transform-leave-active { transition: all 0.3s; }
.fade-transform-enter-from { opacity: 0; transform: translateX(-30px); }
.fade-transform-leave-to { opacity: 0; transform: translateX(30px); }
</style>