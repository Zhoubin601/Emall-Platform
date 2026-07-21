<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { useAdminStore } from '../../stores/admin'
// ✨ 引入封装好的请求工具
import request from '../../utils/request' 

const router = useRouter()
const loading = ref(false)
const adminStore = useAdminStore();

const loginForm = reactive({
  username: '',
  password: ''
})

const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) {
    ElMessage.warning('请输入管理员账号和密码')
    return
  }
  
  loading.value = true
  
  try {
    // ✨ 调用后端我们刚刚新写的专属登录接口
    const res: any = await request.post('/user/adminLogin', loginForm)
    
    // 保存后端签发的 JWT 和管理员资料
    adminStore.setAdminLogin(res.token, res.user)
    
    ElMessage.success(`欢迎回来，${res.user.nickname || res.user.username}！`)
    router.push('/') 
  } catch (error: any) {
    // 捕获并显示后端抛出的 "权限不足" 或 "密码错误"
    ElMessage.error(error.response?.data || error.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-container">
    <div class="login-box">
      <div class="login-header">
        <h2>E-MALL 管理后台</h2>
        <p>电商平台核心控制中枢</p>
      </div>
      
      <el-form :model="loginForm" class="login-form" @keyup.enter="handleLogin">
        <el-form-item>
          <el-input 
            v-model="loginForm.username" 
            placeholder="管理员账号" 
            size="large" 
            :prefix-icon="User" 
          />
        </el-form-item>
        
        <el-form-item>
          <el-input 
            v-model="loginForm.password" 
            type="password" 
            placeholder="管理员密码" 
            size="large" 
            :prefix-icon="Lock" 
            show-password 
          />
        </el-form-item>

        <el-button 
          type="primary" 
          class="login-btn" 
          size="large" 
          :loading="loading" 
          @click="handleLogin"
        >
          安全登录
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<style scoped>
/* 样式保持原样完全不变 */
.login-container { height: 100vh; display: flex; justify-content: center; align-items: center; background: linear-gradient(135deg, #e0f2fe 0%, #bae6fd 100%); }
.login-box { width: 420px; background: rgba(255, 255, 255, 0.9); padding: 40px 50px; border-radius: 16px; box-shadow: 0 10px 25px rgba(56, 189, 248, 0.2); backdrop-filter: blur(10px); }
.login-header { text-align: center; margin-bottom: 40px; }
.login-header h2 { margin: 0; font-size: 28px; color: #0369a1; letter-spacing: 2px; }
.login-header p { margin: 10px 0 0; color: #7dd3fc; font-size: 14px; }
.login-btn { width: 100%; margin-top: 20px; border-radius: 8px; font-size: 16px; font-weight: bold; letter-spacing: 4px; background-color: #0284c7; border-color: #0284c7; }
.login-btn:hover { background-color: #0369a1; border-color: #0369a1; }
</style>
