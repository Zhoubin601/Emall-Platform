<script setup lang="ts">
import { ref, onMounted, onUnmounted, nextTick } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'
import { Promotion, Service, EditPen, ChatLineRound } from '@element-plus/icons-vue'
import { useAdminStore } from '../../stores/admin'

const adminStore = useAdminStore()
const activeTab = ref('chat') // 控制当前处于哪个功能页

// ================= ✨ 1. 在线会话 (实时聊天) 专属逻辑 =================
const userList = ref<any[]>([])
const chatHistory = ref<any[]>([])
const activeUserId = ref<number | null>(null)
const replyContent = ref('')
const isReplying = ref(false)
const chatBoxRef = ref<HTMLElement | null>(null)

let pollTimer: any = null

const scrollToBottom = () => {
  nextTick(() => { setTimeout(() => { if (chatBoxRef.value) chatBoxRef.value.scrollTop = chatBoxRef.value.scrollHeight }, 100) })
}

const syncChatData = async () => {
  if (activeTab.value !== 'chat') return // ✨ 性能优化：不在聊天 Tab 时停止无意义的轮询刷新
  try {
    const users: any = await request.get('/interaction/chat/user-list')
    userList.value = users
    if (activeUserId.value) {
      const history: any = await request.get('/interaction/chat/history', { params: { userId: activeUserId.value } })
      if (history.length > chatHistory.value.length) {
        chatHistory.value = history
        scrollToBottom() 
      } else {
        chatHistory.value = history
      }
    }
  } catch (error) {}
}

const selectChat = (user: any) => {
  activeUserId.value = user.userId
  chatHistory.value = [] 
  syncChatData()
}

const submitChatReply = async () => {
  if (!replyContent.value.trim()) return ElMessage.warning('不能发送空消息')
  isReplying.value = true
  try {
    await request.post('/interaction/chat/send', {
      userId: activeUserId.value, content: replyContent.value, senderRole: 1 
    })
    replyContent.value = ''
    await syncChatData() 
  } catch (error) { ElMessage.error('发送失败') } 
  finally { isReplying.value = false }
}

// ================= ✨ 2. 意见箱 (正式工单) 专属逻辑 =================
const ticketList = ref<any[]>([])
const ticketLoading = ref(false)
const replyDialogVisible = ref(false)
const currentTicket = ref<any>({})
const ticketReplyContent = ref('')
const isTicketReplying = ref(false)

const fetchTickets = async () => {
  ticketLoading.value = true
  try {
    ticketList.value = await request.get('/interaction/feedback/list')
  } catch (error) {
    ElMessage.error('获取意见箱列表失败')
  } finally {
    ticketLoading.value = false
  }
}

const openReplyDialog = (row: any) => {
  currentTicket.value = { ...row }
  ticketReplyContent.value = row.reply || ''
  replyDialogVisible.value = true
}

const submitTicketReply = async () => {
  if (!ticketReplyContent.value.trim()) return ElMessage.warning('回复内容不能为空')
  isTicketReplying.value = true
  try {
    await request.put('/interaction/feedback/reply', {
      id: currentTicket.value.id, reply: ticketReplyContent.value
    })
    ElMessage.success('工单回复成功！买家可在个人中心查看。')
    replyDialogVisible.value = false
    fetchTickets() 
  } catch (error) { ElMessage.error('回复失败') } 
  finally { isTicketReplying.value = false }
}

// ================= 联动与生命周期 =================
const handleTabChange = (name: any) => {
  if (name === 'ticket') fetchTickets()
  else if (name === 'chat') syncChatData()
}

onMounted(() => {
  syncChatData()
  fetchTickets()
  pollTimer = setInterval(syncChatData, 2000)
})

onUnmounted(() => { if (pollTimer) clearInterval(pollTimer) })
</script>

<template>
  <el-card class="dashboard-wrapper glass-card" shadow="never" :body-style="{ padding: '0px' }">
    <el-tabs v-model="activeTab" class="custom-tabs" @tab-change="handleTabChange">
      
      <el-tab-pane name="chat">
        <template #label>
          <span class="tab-label"><el-icon><Service /></el-icon> 在线会话 (实时)</span>
        </template>
        
        <div class="chat-dashboard">
          <div class="chat-sidebar">
            <div class="sidebar-header">
              <el-input placeholder="搜索买家ID..." size="small" style="width: 100%;" />
            </div>
            <div class="user-list">
              <div v-for="item in userList" :key="item.userId" class="user-item" :class="{ active: activeUserId === item.userId }" @click="selectChat(item)">
                <el-avatar :size="45" style="background:#0ea5e9">买家</el-avatar>
                <div class="u-info">
                  <div class="u-top"><span class="u-type">买家ID: {{ item.userId }}</span><span class="u-time" v-if="item.createTime">{{ item.createTime.substring(5, 16) }}</span></div>
                  <div class="u-msg" :class="{'is-new': item.senderRole === 0}">{{ item.senderRole === 1 ? '我: ' : '' }}{{ item.content }}</div>
                </div>
              </div>
            </div>
          </div>

          <div class="chat-main" v-if="activeUserId">
            <div class="chat-header">
              <span style="display:flex; align-items:center; gap:8px;"><el-icon color="#10b981"><Service /></el-icon> 正在与 买家 [{{ activeUserId }}] 会话中...</span>
            </div>
            <div class="chat-history" ref="chatBoxRef">
              <div v-for="msg in chatHistory" :key="msg.id" class="bubble-row" :class="msg.senderRole === 1 ? 'right-bubble' : 'left-bubble'">
                <el-avatar v-if="msg.senderRole === 0" :size="40" style="background:#0ea5e9">买家</el-avatar>
                <div class="bubble-content" :class="{'align-right': msg.senderRole === 1}">
                  <span class="b-time">{{ msg.createTime }}</span>
                  <div class="b-text" :class="msg.senderRole === 1 ? 'glass-b-right' : 'glass-b-left'">{{ msg.content }}</div>
                </div>
                <el-avatar v-if="msg.senderRole === 1" :size="40" :src="adminStore.adminInfo.avatar || ''">客服</el-avatar>
              </div>
            </div>
            <div class="chat-editor">
              <el-input v-model="replyContent" type="textarea" :rows="4" placeholder="请输入回复内容，按回车键快捷发送..." resize="none" @keyup.enter.prevent="submitChatReply"/>
              <div class="editor-action"><el-button type="primary" :icon="Promotion" :loading="isReplying" @click="submitChatReply">发送 (Enter)</el-button></div>
            </div>
          </div>
          
          <div class="chat-main empty-chat" v-else>
            <el-icon size="60" color="#bae6fd"><Service /></el-icon><p>点击左侧会话列表，开始接待客户吧</p>
          </div>
        </div>
      </el-tab-pane>

      <el-tab-pane name="ticket">
        <template #label>
          <span class="tab-label"><el-icon><ChatLineRound /></el-icon> 意见反馈工单</span>
        </template>
        
        <div class="ticket-dashboard" v-loading="ticketLoading">
          <el-table :data="ticketList" style="width: 100%" class="custom-table" stripe>
            <el-table-column prop="id" label="工单ID" width="80" align="center" />
            <el-table-column prop="userId" label="买家ID" width="100" align="center" />
            <el-table-column label="反馈类型" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="row.type === '报错反馈' ? 'danger' : 'info'" effect="plain">{{ row.type }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="content" label="反馈详情内容" min-width="300" show-overflow-tooltip />
            <el-table-column prop="createTime" label="提交时间" width="170" align="center" />
            <el-table-column label="处理状态" width="120" align="center">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'warning'" effect="dark">
                  {{ row.status === 1 ? '已回复解决' : '待处理' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150" align="center" fixed="right">
              <template #default="{ row }">
                <el-button 
                  :type="row.status === 1 ? 'info' : 'primary'" 
                  link 
                  :icon="EditPen" 
                  @click="openReplyDialog(row)"
                >
                  {{ row.status === 1 ? '查看回复' : '立即处理' }}
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>
      </el-tab-pane>
    </el-tabs>

    <el-dialog 
      v-model="replyDialogVisible" 
      :title="`处理工单 [#${currentTicket.id}]`" 
      width="500px" 
      class="glass-dialog"
    >
      <div class="ticket-detail-box">
        <div class="t-label">买家描述的问题：</div>
        <div class="t-content">{{ currentTicket.content }}</div>
      </div>
      <el-form label-position="top" style="margin-top: 20px;">
        <el-form-item label="官方回复内容：">
          <el-input 
            v-model="ticketReplyContent" 
            type="textarea" 
            :rows="5" 
            placeholder="请输入官方处理结果，安抚买家情绪..." 
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="replyDialogVisible = false" round>取消</el-button>
        <el-button type="primary" @click="submitTicketReply" :loading="isTicketReplying" round>提交处理结果</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<style scoped>
/* 最外层容器 */
.dashboard-wrapper { height: calc(100vh - 100px); display: flex; flex-direction: column; overflow: hidden; }
.glass-card { border-radius: 12px; border: 1px solid rgba(186, 230, 253, 0.5); background: rgba(255, 255, 255, 0.9); box-shadow: 0 4px 20px rgba(0,0,0,0.05); }

/* Tab 样式定制 */
:deep(.custom-tabs > .el-tabs__header) { margin: 0; background: #f8fafc; border-bottom: 1px solid #e2e8f0; padding: 0 20px; }
:deep(.custom-tabs .el-tabs__item) { height: 55px; line-height: 55px; font-size: 15px; color: #64748b; }
:deep(.custom-tabs .el-tabs__item.is-active) { color: #0284c7; font-weight: bold; }
.tab-label { display: flex; align-items: center; gap: 5px; }

/* 聊天区域 (与原逻辑高度一致) */
.chat-dashboard { display: flex; height: calc(100vh - 157px); background: #fff; }
.chat-sidebar { width: 320px; border-right: 1px solid #f1f5f9; display: flex; flex-direction: column; background: #f8fafc; }
.sidebar-header { padding: 15px; border-bottom: 1px solid #e2e8f0; }
.user-list { flex: 1; overflow-y: auto; }
.user-item { display: flex; gap: 12px; padding: 15px 20px; cursor: pointer; transition: background 0.2s; border-bottom: 1px solid #f1f5f9; }
.user-item:hover { background: #f1f5f9; }
.user-item.active { background: #e0f2fe; border-left: 4px solid #0ea5e9; }
.u-info { flex: 1; overflow: hidden; }
.u-top { display: flex; justify-content: space-between; margin-bottom: 5px; }
.u-type { font-weight: bold; color: #1e293b; font-size: 14px; }
.u-time { font-size: 12px; color: #94a3b8; }
.u-msg { font-size: 13px; color: #64748b; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.u-msg.is-new { color: #0ea5e9; font-weight: bold; } 

.chat-main { flex: 1; display: flex; flex-direction: column; background: #f8fafc; }
.empty-chat { justify-content: center; align-items: center; color: #94a3b8; font-weight: bold; background: #fff; }
.chat-header { padding: 15px 25px; border-bottom: 1px solid #e2e8f0; display: flex; justify-content: space-between; align-items: center; font-weight: bold; color: #0f172a; background: #fff; }
.chat-history { flex: 1; padding: 25px; overflow-y: auto; display: flex; flex-direction: column; gap: 25px; background: #f8fafc; scroll-behavior: smooth; }
.bubble-row { display: flex; gap: 15px; max-width: 80%; }
.left-bubble { align-self: flex-start; }
.right-bubble { align-self: flex-end; }
.bubble-content { display: flex; flex-direction: column; gap: 5px; }
.align-right { align-items: flex-end; }
.b-time { font-size: 12px; color: #94a3b8; }
.b-text { padding: 12px 18px; border-radius: 12px; font-size: 14px; line-height: 1.5; color: #1e293b; box-shadow: 0 4px 10px rgba(0,0,0,0.03); word-break: break-all; }
.glass-b-left { background: #fff; border-top-left-radius: 0; }
.glass-b-right { background: #0ea5e9; color: white; border-top-right-radius: 0; }
.chat-editor { padding: 20px; background: #fff; border-top: 1px solid #e2e8f0; }
.editor-action { display: flex; justify-content: flex-end; margin-top: 15px; }

/* 意见箱表格区域 */
.ticket-dashboard { padding: 20px; height: calc(100vh - 197px); overflow-y: auto; }
:deep(.custom-table th.el-table__cell) { background-color: #f0f9ff; color: #0369a1; }

/* 弹窗样式 */
:deep(.glass-dialog) { border-radius: 16px; overflow: hidden; }
:deep(.glass-dialog .el-dialog__header) { background: #f8fafc; margin-right: 0; border-bottom: 1px solid #f1f5f9; padding: 20px; font-weight: bold; color: #0f172a; }
.ticket-detail-box { background: #fff1f2; padding: 15px; border-radius: 12px; border: 1px solid #ffe4e6; }
.t-label { font-size: 12px; color: #e11d48; margin-bottom: 5px; font-weight: bold; }
.t-content { font-size: 14px; color: #1e293b; line-height: 1.5; }
</style>