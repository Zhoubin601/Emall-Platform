<script setup lang="ts">
import { ref, nextTick } from 'vue'
import { ChatDotRound, Position } from '@element-plus/icons-vue'
import { useUserStore } from './stores/user'
import { ElMessage } from 'element-plus'
import request from './utils/request'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()

const chatDrawerVisible = ref(false)
const chatInput = ref('')
const isSending = ref(false)

// ✨ 聊天核心状态
const chatList = ref<any[]>([])
const chatBoxRef = ref<HTMLElement | null>(null)
let pollTimer: any = null

const scrollToBottom = () => {
  nextTick(() => {
    setTimeout(() => {
      if (chatBoxRef.value) {
        chatBoxRef.value.scrollTop = chatBoxRef.value.scrollHeight
      }
    }, 100)
  })
}

// 获取对话历史
const loadChatHistory = async () => {
  if (!userStore.userInfo) return
  try {
    const res: any = await request.get('/interaction/chat/history', { params: { userId: userStore.userInfo.id } })
    // 如果消息变多了，才触发滚动到底部
    if (res.length > chatList.value.length) {
      chatList.value = res
      scrollToBottom()
    } else {
      chatList.value = res
    }
  } catch (e) {
    console.error('刷新消息失败')
  }
}

const openChat = () => {
  if (!userStore.userInfo) {
    ElMessage.warning('请先登录后再联系客服哦~')
    router.push('/login')
    return
  }
  chatDrawerVisible.value = true
  loadChatHistory()
  // ✨ 开启短轮询：每3秒刷新一次消息
  pollTimer = setInterval(loadChatHistory, 3000)
}

const closeChat = () => {
  if (pollTimer) clearInterval(pollTimer) // ✨ 关闭时必须销毁定时器
}

// 发送消息
const sendMessage = async () => {
  if (!chatInput.value.trim()) return ElMessage.warning('不能发送空消息哦')
  
  isSending.value = true
  try {
    await request.post('/interaction/chat/send', {
      userId: userStore.userInfo?.id,
      content: chatInput.value,
      senderRole: 0 // ✨ 0 代表买家发送
    })
    chatInput.value = ''
    await loadChatHistory() // 发完立刻刷新
  } catch (error) {
    ElMessage.error('发送失败，请检查网络')
  } finally {
    isSending.value = false
  }
}
</script>

<template>
  <div id="app-wrapper">
    <router-view />

    <div class="global-chat-widget" @click="openChat">
      <div class="chat-btn glass-effect">
        <el-icon size="28" color="#0ea5e9"><ChatDotRound /></el-icon>
      </div>
      <div class="chat-tooltip glass-effect">联系客服</div>
    </div>

    <el-drawer
      v-model="chatDrawerVisible"
      title="专属客服 - 在线沟通"
      size="380px"
      class="cute-chat-drawer"
      @close="closeChat"
      destroy-on-close
    >
      <div class="chat-container">
        <div class="chat-history-box" ref="chatBoxRef">
          <div class="chat-tips">
            欢迎来到 E-MALL！有问题随时问我哦，小E正在为您服务。💖
          </div>
          
          <div 
            v-for="msg in chatList" 
            :key="msg.id" 
            class="chat-bubble-row"
            :class="msg.senderRole === 0 ? 'is-me' : 'is-admin'"
          >
            <el-avatar v-if="msg.senderRole === 1" :size="35" style="background:#f43f5e" class="b-avatar">客</el-avatar>
            <div class="bubble-content">
              <span class="b-time" v-if="msg.senderRole === 1">{{ msg.createTime?.substring(11,16) }}</span>
              <div class="b-text">{{ msg.content }}</div>
              <span class="b-time" v-if="msg.senderRole === 0">{{ msg.createTime?.substring(11,16) }}</span>
            </div>
            <el-avatar v-if="msg.senderRole === 0" :size="35" style="background:#0ea5e9" class="b-avatar">我</el-avatar>
          </div>
        </div>

        <div class="chat-input-box">
          <el-input 
            v-model="chatInput" 
            type="textarea" 
            :rows="3" 
            placeholder="说点什么吧..." 
            class="cute-textarea"
            @keyup.enter.prevent="sendMessage"
          />
          <el-button type="primary" class="send-btn" circle :icon="Position" :loading="isSending" @click="sendMessage" />
        </div>
      </div>
    </el-drawer>
  </div>
</template>

<style>
/* 基础与悬浮按钮样式保留不变 */
body { margin: 0; padding: 0; background-color: #f8fafc; font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB', 'Microsoft YaHei', Arial, sans-serif; }
.glass-effect { background: rgba(255, 255, 255, 0.7); backdrop-filter: blur(12px); border: 1px solid rgba(255, 255, 255, 0.8); box-shadow: 0 8px 32px rgba(14, 165, 233, 0.15); }
.global-chat-widget { position: fixed; bottom: 40px; right: 40px; z-index: 9999; display: flex; align-items: center; gap: 15px; cursor: pointer; animation: floatUpDown 3s ease-in-out infinite; }
@keyframes floatUpDown { 0%, 100% { transform: translateY(0); } 50% { transform: translateY(-10px); } }
.chat-btn { width: 60px; height: 60px; border-radius: 50%; display: flex; justify-content: center; align-items: center; transition: all 0.3s; }
.global-chat-widget:hover .chat-btn { transform: scale(1.1); background: #e0f2fe; }
.chat-tooltip { padding: 8px 15px; border-radius: 20px; font-size: 13px; font-weight: bold; color: #0284c7; opacity: 0; transform: translateX(10px); transition: all 0.3s; pointer-events: none; }
.global-chat-widget:hover .chat-tooltip { opacity: 1; transform: translateX(0); }

/* ✨ 全新聊天抽屉专属样式 */
.cute-chat-drawer .el-drawer__header { margin-bottom: 0; padding: 20px; font-weight: bold; color: #0369a1; border-bottom: 1px solid #e0f2fe; background: #f0f9ff; }
.cute-chat-drawer .el-drawer__body { padding: 0; }
.chat-container { display: flex; flex-direction: column; height: 100%; }

.chat-history-box { flex: 1; padding: 20px; overflow-y: auto; display: flex; flex-direction: column; gap: 20px; background: #f8fafc; }
.chat-tips { text-align: center; background: #fff1f2; color: #e11d48; padding: 8px 12px; border-radius: 12px; font-size: 12px; line-height: 1.5; align-self: center; margin-bottom: 10px; border: 1px solid #ffe4e6; }

.chat-bubble-row { display: flex; gap: 10px; align-items: flex-start; max-width: 90%; }
.chat-bubble-row.is-admin { align-self: flex-start; }
.chat-bubble-row.is-me { align-self: flex-end; }

.b-avatar { flex-shrink: 0; font-size: 12px; font-weight: bold; }
.bubble-content { display: flex; flex-direction: column; gap: 4px; }
.is-admin .bubble-content { align-items: flex-start; }
.is-me .bubble-content { align-items: flex-end; }

.b-text { padding: 10px 15px; border-radius: 12px; font-size: 14px; line-height: 1.5; box-shadow: 0 2px 8px rgba(0,0,0,0.05); }
.is-admin .b-text { background: #fff; color: #1e293b; border-top-left-radius: 0; }
.is-me .b-text { background: #0ea5e9; color: #fff; border-top-right-radius: 0; }

.b-time { font-size: 11px; color: #cbd5e1; }

.chat-input-box { padding: 15px; background: #fff; border-top: 1px solid #e2e8f0; display: flex; gap: 10px; align-items: flex-end; }
.cute-textarea { flex: 1; }
.cute-textarea :deep(.el-textarea__inner) { border-radius: 12px; background: #f8fafc; border: 1px solid #e2e8f0; padding: 10px; box-shadow: none; }
.cute-textarea :deep(.el-textarea__inner:focus) { border-color: #0ea5e9; background: #fff; }
.send-btn { width: 40px; height: 40px; background: linear-gradient(135deg, #0ea5e9, #0284c7); border: none; box-shadow: 0 4px 10px rgba(14, 165, 233, 0.3); }
</style>