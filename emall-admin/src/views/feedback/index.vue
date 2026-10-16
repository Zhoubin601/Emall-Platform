<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import request from '../../utils/request'
import { ElMessage } from 'element-plus'
import { 
  Promotion, 
  Service, 
  EditPen, 
  ChatLineRound, 
  Search, 
  Refresh, 
  Check, 
  Clock, 
  DocumentChecked, 
  Tickets,
  ChatDotRound
} from '@element-plus/icons-vue'
import { useAdminStore } from '../../stores/admin'

const adminStore = useAdminStore()
const activeTab = ref('chat') // 'chat' | 'ticket'

// ================= ✨ 1. 在线会话 (实时聊天) 专属逻辑 =================
const userList = ref<any[]>([])
const chatHistory = ref<any[]>([])
const activeUserId = ref<number | null>(null)
const replyContent = ref('')
const isReplying = ref(false)
const chatBoxRef = ref<HTMLElement | null>(null)
const userSearchKeyword = ref('')

const quickReplies = [
  '您好！欢迎咨询 E-MALL 官方客服，请问有什么可以帮您？',
  '亲，您的订单已进入急速出库通道，预计今日安排顺丰特快揽收！',
  '全场自营商品支持 7 天无理由退换并附赠运费险，请放心选购。',
  '请提供一下商品外观及物流单号照片，我们立即为您核实补发或退款。'
]

const applyQuickReply = (phrase: string) => {
  if (replyContent.value) {
    replyContent.value += ' ' + phrase
  } else {
    replyContent.value = phrase
  }
}

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

const syncChatData = async () => {
  if (activeTab.value !== 'chat') return
  try {
    const users: any = await request.get('/interaction/chat/user-list')
    userList.value = Array.isArray(users) ? users : (users?.data || [])
    if (!activeUserId.value && userList.value.length > 0) {
      activeUserId.value = userList.value[0].userId
    }
    if (activeUserId.value) {
      const history: any = await request.get('/interaction/chat/history', { params: { userId: activeUserId.value } })
      const list = Array.isArray(history) ? history : (history?.data || [])
      if (list.length > chatHistory.value.length) {
        chatHistory.value = list
        scrollToBottom() 
      } else {
        chatHistory.value = list
      }
    }
  } catch (error) {}
}

const filteredUserList = computed(() => {
  if (!userSearchKeyword.value.trim()) return userList.value
  const kw = userSearchKeyword.value.trim().toLowerCase()
  return userList.value.filter((item: any) => 
    String(item.userId).includes(kw) || 
    (item.content && String(item.content).toLowerCase().includes(kw))
  )
})

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
      userId: activeUserId.value, 
      content: replyContent.value, 
      senderRole: 1 
    })
    replyContent.value = ''
    await syncChatData() 
  } catch (error) { 
    ElMessage.error('发送失败') 
  } finally { 
    isReplying.value = false 
  }
}

// ================= ✨ 2. 意见箱 (工单系统) 专属逻辑 =================
const ticketList = ref<any[]>([])
const ticketLoading = ref(false)
const replyDialogVisible = ref(false)
const currentTicket = ref<any>({})
const ticketReplyContent = ref('')
const isTicketReplying = ref(false)
const ticketSearchKeyword = ref('')
const ticketStatusFilter = ref<'all' | 'pending' | 'resolved'>('all')

const fetchTickets = async () => {
  ticketLoading.value = true
  try {
    const res: any = await request.get('/interaction/feedback/list')
    ticketList.value = Array.isArray(res) ? res : (res?.data || [])
  } catch (error) {
    ElMessage.error('获取意见箱列表失败')
  } finally {
    ticketLoading.value = false
  }
}

// 工单 Bento 指标计算
const totalTicketsCount = computed(() => ticketList.value.length)
const pendingTicketsCount = computed(() => ticketList.value.filter((t: any) => t.status === 0).length)
const resolvedTicketsCount = computed(() => ticketList.value.filter((t: any) => t.status === 1).length)
const completionRate = computed(() => {
  if (totalTicketsCount.value === 0) return '100%'
  return Math.round((resolvedTicketsCount.value / totalTicketsCount.value) * 100) + '%'
})

const filteredTickets = computed(() => {
  let list = ticketList.value
  if (ticketStatusFilter.value === 'pending') {
    list = list.filter((t: any) => t.status === 0)
  } else if (ticketStatusFilter.value === 'resolved') {
    list = list.filter((t: any) => t.status === 1)
  }
  if (ticketSearchKeyword.value.trim()) {
    const kw = ticketSearchKeyword.value.trim().toLowerCase()
    list = list.filter((t: any) => 
      String(t.id).includes(kw) || 
      String(t.userId).includes(kw) || 
      (t.content && t.content.toLowerCase().includes(kw)) ||
      (t.type && t.type.toLowerCase().includes(kw))
    )
  }
  return list
})

const openReplyDialog = (row: any) => {
  currentTicket.value = { ...row }
  ticketReplyContent.value = row.reply || ''
  replyDialogVisible.value = true
}

const applyTicketQuickReply = (text: string) => {
  ticketReplyContent.value = text
}

const submitTicketReply = async () => {
  if (!ticketReplyContent.value.trim()) return ElMessage.warning('回复内容不能为空')
  isTicketReplying.value = true
  try {
    await request.put('/interaction/feedback/reply', {
      id: currentTicket.value.id, 
      reply: ticketReplyContent.value
    })
    ElMessage.success('工单回复成功！买家可在个人中心查看最新答复。')
    replyDialogVisible.value = false
    fetchTickets() 
  } catch (error) { 
    ElMessage.error('回复失败') 
  } finally { 
    isTicketReplying.value = false 
  }
}

const handleTabChange = (name: any) => {
  if (name === 'ticket') fetchTickets()
  else if (name === 'chat') syncChatData()
}

onMounted(() => {
  syncChatData()
  fetchTickets()
  pollTimer = setInterval(syncChatData, 2500)
})

onUnmounted(() => { 
  if (pollTimer) clearInterval(pollTimer) 
})
</script>

<template>
  <div class="feedback-page-wrapper">
    <el-card class="dashboard-wrapper glass-card" shadow="never" :body-style="{ padding: '0px' }">
      <el-tabs v-model="activeTab" class="custom-tabs" @tab-change="handleTabChange">
        
        <!-- Tab 1: 在线会话 (实时聊天) -->
        <el-tab-pane name="chat">
          <template #label>
            <div class="tab-label">
              <el-icon><Service /></el-icon>
              <span>在线会话客服工作台</span>
              <span class="active-badge" v-if="userList.length > 0">{{ userList.length }}</span>
            </div>
          </template>
          
          <div class="chat-dashboard">
            <!-- 左侧会话买家列表 -->
            <div class="chat-sidebar">
              <div class="sidebar-header">
                <el-input 
                  v-model="userSearchKeyword" 
                  placeholder="搜索买家ID或最近消息..." 
                  size="default" 
                  clearable
                  :prefix-icon="Search"
                />
              </div>

              <div class="user-list">
                <div 
                  v-for="item in filteredUserList" 
                  :key="item.userId" 
                  class="user-item" 
                  :class="{ active: activeUserId === item.userId }" 
                  @click="selectChat(item)"
                >
                  <div class="avatar-box">
                    <el-avatar :size="42" class="buyer-avatar">买家</el-avatar>
                    <span class="online-dot" title="在线会话接入中"></span>
                  </div>
                  <div class="u-info">
                    <div class="u-top">
                      <span class="u-type">买家 #{{ item.userId }}</span>
                      <span class="u-time" v-if="item.createTime">{{ item.createTime.substring(5, 16) }}</span>
                    </div>
                    <div class="u-msg" :class="{'is-new': item.senderRole === 0}">
                      <span v-if="item.senderRole === 1" class="self-tag">我: </span>
                      {{ item.content }}
                    </div>
                  </div>
                </div>

                <div v-if="filteredUserList.length === 0" class="no-user-found">
                  <el-icon :size="32" color="#94a3b8"><ChatDotRound /></el-icon>
                  <p>无匹配的会话买家</p>
                </div>
              </div>
            </div>

            <!-- 右侧对话详情主体 -->
            <div class="chat-main" v-if="activeUserId">
              <!-- 顶部对话会话信息条 -->
              <div class="chat-header">
                <div class="header-left">
                  <span class="pulse-status-dot"></span>
                  <span class="buyer-title">买家用户 [#{{ activeUserId }}]</span>
                  <span class="vip-capsule">VIP 会员</span>
                </div>
                <div class="header-actions">
                  <el-button size="small" :icon="Refresh" @click="syncChatData" round plain>刷新同步</el-button>
                </div>
              </div>

              <!-- 消息历史气泡列表 -->
              <div class="chat-history" ref="chatBoxRef">
                <div 
                  v-for="msg in chatHistory" 
                  :key="msg.id" 
                  class="bubble-row" 
                  :class="msg.senderRole === 1 ? 'right-bubble' : 'left-bubble'"
                >
                  <el-avatar v-if="msg.senderRole === 0" :size="38" class="buyer-avatar-bubble">买家</el-avatar>
                  <div class="bubble-content" :class="{'align-right': msg.senderRole === 1}">
                    <div class="bubble-meta">
                      <span class="b-sender-name">{{ msg.senderRole === 1 ? (adminStore.adminInfo?.nickname || '官方客服') : `买家 #${msg.userId}` }}</span>
                      <span class="b-time">{{ msg.createTime }}</span>
                    </div>
                    <div class="b-text" :class="msg.senderRole === 1 ? 'glass-b-right' : 'glass-b-left'">
                      {{ msg.content }}
                    </div>
                  </div>
                  <el-avatar 
                    v-if="msg.senderRole === 1" 
                    :size="38" 
                    :src="adminStore.adminInfo?.avatar || ''"
                    class="cs-avatar-bubble"
                  >
                    客服
                  </el-avatar>
                </div>
              </div>

              <!-- 快捷回复工具栏 -->
              <div class="quick-reply-bar">
                <span class="quick-title">快捷回复：</span>
                <div class="quick-chips">
                  <span 
                    v-for="(phrase, idx) in quickReplies" 
                    :key="idx" 
                    class="reply-chip"
                    @click="applyQuickReply(phrase)"
                    title="点击插入回复框"
                  >
                    {{ phrase }}
                  </span>
                </div>
              </div>

              <!-- 输入框与发送区 -->
              <div class="chat-editor">
                <el-input 
                  v-model="replyContent" 
                  type="textarea" 
                  :rows="3" 
                  placeholder="请输入对买家的回复，按 Enter 键发送，Ctrl+Enter 换行..." 
                  resize="none" 
                  @keydown.enter.exact.prevent="submitChatReply"
                />
                <div class="editor-action">
                  <span class="hint-text">按 Enter 快捷发送</span>
                  <el-button 
                    type="primary" 
                    :icon="Promotion" 
                    :loading="isReplying" 
                    @click="submitChatReply"
                    class="send-btn"
                  >
                    发送消息
                  </el-button>
                </div>
              </div>
            </div>
            
            <!-- 空状态 -->
            <div class="chat-main empty-chat" v-else>
              <div class="empty-chat-card">
                <el-icon size="64" color="#0284c7"><Service /></el-icon>
                <h3>客服接待就绪</h3>
                <p>请点击左侧买家列表中的任意会话，开启一对一实时服务</p>
              </div>
            </div>
          </div>
        </el-tab-pane>

        <!-- Tab 2: 意见反馈工单中心 -->
        <el-tab-pane name="ticket">
          <template #label>
            <div class="tab-label">
              <el-icon><ChatLineRound /></el-icon>
              <span>意见反馈工单中心</span>
              <span class="pending-badge" v-if="pendingTicketsCount > 0">{{ pendingTicketsCount }} 待办</span>
            </div>
          </template>
          
          <div class="ticket-tab-container">
            <!-- 4 Mini Bento 统计指标卡 -->
            <div class="ticket-bento-grid">
              <div class="bento-metric-card metric-all">
                <div class="metric-icon-box"><el-icon><Tickets /></el-icon></div>
                <div class="metric-info">
                  <div class="metric-num">{{ totalTicketsCount }}</div>
                  <div class="metric-label">反馈工单总量</div>
                </div>
              </div>

              <div class="bento-metric-card metric-pending">
                <div class="metric-icon-box"><el-icon><Clock /></el-icon></div>
                <div class="metric-info">
                  <div class="metric-num">{{ pendingTicketsCount }}</div>
                  <div class="metric-label">待回复待处理</div>
                </div>
              </div>

              <div class="bento-metric-card metric-resolved">
                <div class="metric-icon-box"><el-icon><DocumentChecked /></el-icon></div>
                <div class="metric-info">
                  <div class="metric-num">{{ resolvedTicketsCount }}</div>
                  <div class="metric-label">已闭环解决</div>
                </div>
              </div>

              <div class="bento-metric-card metric-rate">
                <div class="metric-icon-box"><el-icon><Check /></el-icon></div>
                <div class="metric-info">
                  <div class="metric-num">{{ completionRate }}</div>
                  <div class="metric-label">工单闭环率</div>
                </div>
              </div>
            </div>

            <!-- 工单过滤工具栏 -->
            <div class="ticket-filter-toolbar">
              <div class="filter-left">
                <el-radio-group v-model="ticketStatusFilter" size="default">
                  <el-radio-button label="all">全部工单 ({{ totalTicketsCount }})</el-radio-button>
                  <el-radio-button label="pending">待处理 ({{ pendingTicketsCount }})</el-radio-button>
                  <el-radio-button label="resolved">已解决 ({{ resolvedTicketsCount }})</el-radio-button>
                </el-radio-group>

                <el-input 
                  v-model="ticketSearchKeyword" 
                  placeholder="搜索工单ID / 买家ID / 描述关键词..." 
                  size="default" 
                  clearable
                  :prefix-icon="Search"
                  style="width: 320px;"
                />
              </div>

              <div class="filter-right">
                <el-button :icon="Refresh" @click="fetchTickets" size="default" plain>刷新工单</el-button>
              </div>
            </div>

            <!-- 工单表格 -->
            <div class="ticket-table-card" v-loading="ticketLoading">
              <el-table :data="filteredTickets" style="width: 100%" class="custom-admin-table" stripe>
                <el-table-column prop="id" label="工单ID" width="100" align="center">
                  <template #default="{ row }">
                    <span class="ticket-id-tag">#{{ row.id }}</span>
                  </template>
                </el-table-column>
                
                <el-table-column prop="userId" label="买家账号" width="120" align="center">
                  <template #default="{ row }">
                    <span class="user-id-pill">买家 #{{ row.userId }}</span>
                  </template>
                </el-table-column>

                <el-table-column label="反馈类别" width="130" align="center">
                  <template #default="{ row }">
                    <el-tag 
                      :type="row.type === '报错反馈' ? 'danger' : (row.type === '售后咨询' ? 'warning' : 'primary')" 
                      effect="light"
                      class="type-tag"
                    >
                      {{ row.type || '日常建议' }}
                    </el-tag>
                  </template>
                </el-table-column>

                <el-table-column prop="content" label="反馈详情与诉求" min-width="280">
                  <template #default="{ row }">
                    <div class="ticket-content-cell">
                      <div class="feedback-text">{{ row.content }}</div>
                      <div v-if="row.reply" class="feedback-reply-preview">
                        <span class="reply-tag">已答复:</span>
                        <span class="reply-preview-text">{{ row.reply }}</span>
                      </div>
                    </div>
                  </template>
                </el-table-column>

                <el-table-column prop="createTime" label="提交时间" width="170" align="center">
                  <template #default="{ row }">
                    <span class="time-col">{{ row.createTime || '-' }}</span>
                  </template>
                </el-table-column>

                <el-table-column label="处理状态" width="120" align="center">
                  <template #default="{ row }">
                    <span v-if="row.status === 1" class="status-capsule resolved">
                      <span class="dot"></span> 已答复
                    </span>
                    <span v-else class="status-capsule pending">
                      <span class="dot"></span> 待处理
                    </span>
                  </template>
                </el-table-column>

                <el-table-column label="操作" width="140" align="center" fixed="right">
                  <template #default="{ row }">
                    <el-button 
                      :type="row.status === 1 ? 'info' : 'primary'" 
                      link 
                      :icon="EditPen" 
                      @click="openReplyDialog(row)"
                      class="ticket-op-btn"
                    >
                      {{ row.status === 1 ? '查看与更新回复' : '立即处理答复' }}
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>
        </el-tab-pane>
      </el-tabs>

      <!-- 工单回复弹窗 -->
      <el-dialog 
        v-model="replyDialogVisible" 
        :title="`处理买家反馈工单 [#${currentTicket.id || ''}]`" 
        width="560px" 
        class="glass-dialog custom-reply-dialog"
      >
        <div class="ticket-detail-box">
          <div class="t-top-row">
            <span class="t-label">买家 [#{{ currentTicket.userId }}] 反馈诉求：</span>
            <el-tag size="small" type="danger" effect="plain">{{ currentTicket.type || '工单建议' }}</el-tag>
          </div>
          <div class="t-content">{{ currentTicket.content }}</div>
          <div class="t-time">提交于：{{ currentTicket.createTime }}</div>
        </div>

        <div class="quick-preset-box">
          <div class="preset-title">推荐标准答复用语：</div>
          <div class="preset-items">
            <span 
              class="preset-chip" 
              @click="applyTicketQuickReply('感谢您的反馈与支持！我们已对相关流程进行深度优化，欢迎您继续监督体验。')"
            >
              通用感谢采纳
            </span>
            <span 
              class="preset-chip" 
              @click="applyTicketQuickReply('亲爱的买家您好，相关售后专员已为您开辟快速核验绿色通道，请放心！')"
            >
              售后安抚保障
            </span>
            <span 
              class="preset-chip" 
              @click="applyTicketQuickReply('该问题已反馈至技术工程团队，我们将在近期版本更新中优化修复。')"
            >
              技术优化回复
            </span>
          </div>
        </div>

        <el-form label-position="top" style="margin-top: 16px;">
          <el-form-item label="官方正式回复（买家可在个人中心实时查看）：">
            <el-input 
              v-model="ticketReplyContent" 
              type="textarea" 
              :rows="5" 
              placeholder="请输入清晰、诚恳的官方处理答复..." 
            />
          </el-form-item>
        </el-form>

        <template #footer>
          <div class="dialog-footer">
            <el-button @click="replyDialogVisible = false" round>取消</el-button>
            <el-button type="primary" @click="submitTicketReply" :loading="isTicketReplying" round>
              提交处理并答复买家
            </el-button>
          </div>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<style scoped>
.feedback-page-wrapper {
  padding: 0;
}

/* 最外层容器 */
.dashboard-wrapper { 
  height: calc(100vh - 90px); 
  display: flex; 
  flex-direction: column; 
  overflow: hidden; 
  border-radius: 16px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.04);
}

/* Tab 样式定制 */
:deep(.custom-tabs > .el-tabs__header) { 
  margin: 0; 
  background: #f8fafc; 
  border-bottom: 1px solid #e2e8f0; 
  padding: 0 24px; 
}

:deep(.custom-tabs .el-tabs__item) { 
  height: 56px; 
  line-height: 56px; 
  font-size: 15px; 
  color: #64748b; 
  font-weight: 500;
  transition: all 0.2s;
}

:deep(.custom-tabs .el-tabs__item.is-active) { 
  color: #0284c7; 
  font-weight: 700; 
}

.tab-label { 
  display: flex; 
  align-items: center; 
  gap: 8px; 
}

.active-badge {
  background: #e0f2fe;
  color: #0284c7;
  font-size: 11px;
  padding: 1px 7px;
  border-radius: 10px;
  font-weight: 700;
}

.pending-badge {
  background: #fee2e2;
  color: #ef4444;
  font-size: 11px;
  padding: 1px 7px;
  border-radius: 10px;
  font-weight: 700;
}

/* ================= 在线会话 ================= */
.chat-dashboard { 
  display: flex; 
  height: calc(100vh - 150px); 
  background: #ffffff; 
}

.chat-sidebar { 
  width: 320px; 
  border-right: 1px solid #e2e8f0; 
  display: flex; 
  flex-direction: column; 
  background: #f8fafc; 
}

.sidebar-header { 
  padding: 16px; 
  border-bottom: 1px solid #e2e8f0; 
  background: #ffffff;
}

.user-list { 
  flex: 1; 
  overflow-y: auto; 
}

.user-item { 
  display: flex; 
  gap: 12px; 
  padding: 14px 18px; 
  cursor: pointer; 
  transition: all 0.2s; 
  border-bottom: 1px solid #f1f5f9; 
  align-items: center;
}

.user-item:hover { 
  background: #f1f5f9; 
}

.user-item.active { 
  background: #e0f2fe; 
  border-left: 4px solid #0284c7; 
}

.avatar-box {
  position: relative;
  display: inline-flex;
}

.buyer-avatar {
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
  color: #ffffff;
  font-size: 13px;
  font-weight: 600;
  border: 2px solid #ffffff;
  box-shadow: 0 2px 6px rgba(14, 165, 233, 0.2);
}

.online-dot {
  position: absolute;
  bottom: 0px;
  right: 0px;
  width: 10px;
  height: 10px;
  background: #10b981;
  border: 2px solid #ffffff;
  border-radius: 50%;
}

.u-info { 
  flex: 1; 
  overflow: hidden; 
}

.u-top { 
  display: flex; 
  justify-content: space-between; 
  align-items: center;
  margin-bottom: 4px; 
}

.u-type { 
  font-weight: 700; 
  color: #1e293b; 
  font-size: 14px; 
}

.u-time { 
  font-size: 11px; 
  color: #94a3b8; 
}

.u-msg { 
  font-size: 13px; 
  color: #64748b; 
  white-space: nowrap; 
  overflow: hidden; 
  text-overflow: ellipsis; 
}

.u-msg.is-new { 
  color: #0284c7; 
  font-weight: 600; 
}

.self-tag {
  color: #94a3b8;
  font-weight: normal;
}

.no-user-found {
  padding: 40px 20px;
  text-align: center;
  color: #94a3b8;
  font-size: 13px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

/* 聊天主体 */
.chat-main { 
  flex: 1; 
  display: flex; 
  flex-direction: column; 
  background: #f8fafc; 
}

.chat-header { 
  padding: 16px 24px; 
  border-bottom: 1px solid #e2e8f0; 
  display: flex; 
  justify-content: space-between; 
  align-items: center; 
  background: #ffffff; 
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.pulse-status-dot {
  width: 10px;
  height: 10px;
  background: #10b981;
  border-radius: 50%;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
}

.buyer-title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.vip-capsule {
  font-size: 11px;
  font-weight: 700;
  color: #d97706;
  background: #fef3c7;
  padding: 2px 8px;
  border-radius: 12px;
  border: 1px solid #fde68a;
}

.chat-history { 
  flex: 1; 
  padding: 24px; 
  overflow-y: auto; 
  display: flex; 
  flex-direction: column; 
  gap: 20px; 
  background: #f8fafc; 
  scroll-behavior: smooth; 
}

.bubble-row { 
  display: flex; 
  gap: 12px; 
  max-width: 78%; 
  align-items: flex-start;
}

.left-bubble { 
  align-self: flex-start; 
}

.right-bubble { 
  align-self: flex-end; 
}

.buyer-avatar-bubble {
  background: #0ea5e9;
  color: #ffffff;
  font-size: 12px;
  flex-shrink: 0;
}

.cs-avatar-bubble {
  background: #0284c7;
  color: #ffffff;
  font-size: 12px;
  flex-shrink: 0;
}

.bubble-content { 
  display: flex; 
  flex-direction: column; 
  gap: 4px; 
}

.bubble-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  color: #94a3b8;
}

.b-sender-name {
  font-weight: 600;
  color: #64748b;
}

.align-right { 
  align-items: flex-end; 
}

.b-text { 
  padding: 12px 18px; 
  border-radius: 14px; 
  font-size: 14px; 
  line-height: 1.55; 
  word-break: break-all; 
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
}

.glass-b-left { 
  background: #ffffff; 
  color: #1e293b; 
  border: 1px solid #e2e8f0;
  border-top-left-radius: 2px; 
}

.glass-b-right { 
  background: linear-gradient(135deg, #0284c7, #0369a1); 
  color: #ffffff; 
  border-top-right-radius: 2px; 
}

/* 快捷回复条 */
.quick-reply-bar {
  padding: 10px 20px;
  background: #f1f5f9;
  border-top: 1px solid #e2e8f0;
  display: flex;
  align-items: center;
  gap: 10px;
  overflow-x: auto;
}

.quick-title {
  font-size: 12px;
  color: #64748b;
  font-weight: 600;
  white-space: nowrap;
}

.quick-chips {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  white-space: nowrap;
}

.reply-chip {
  font-size: 12px;
  color: #0369a1;
  background: #ffffff;
  border: 1px solid #bae6fd;
  padding: 4px 10px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  user-select: none;
}

.reply-chip:hover {
  background: #0284c7;
  color: #ffffff;
  border-color: #0284c7;
}

.chat-editor { 
  padding: 16px 20px; 
  background: #ffffff; 
  border-top: 1px solid #e2e8f0; 
}

.editor-action { 
  display: flex; 
  justify-content: space-between; 
  align-items: center;
  margin-top: 10px; 
}

.hint-text {
  font-size: 12px;
  color: #94a3b8;
}

.send-btn {
  padding: 8px 20px;
  font-weight: 600;
  border-radius: 8px;
}

.empty-chat { 
  justify-content: center; 
  align-items: center; 
  background: #f8fafc; 
}

.empty-chat-card {
  text-align: center;
  background: #ffffff;
  padding: 40px 60px;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
}

.empty-chat-card h3 {
  margin: 16px 0 8px;
  font-size: 18px;
  color: #0f172a;
}

.empty-chat-card p {
  color: #64748b;
  font-size: 14px;
}

/* ================= 意见反馈工单中心 ================= */
.ticket-tab-container {
  padding: 24px;
  height: calc(100vh - 150px);
  overflow-y: auto;
  background: #f8fafc;
}

/* 4 Mini Bento 统计卡 */
.ticket-bento-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.bento-metric-card {
  background: #ffffff;
  border-radius: 12px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
  transition: transform 0.2s, box-shadow 0.2s;
}

.bento-metric-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.05);
}

.metric-icon-box {
  width: 46px;
  height: 46px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.metric-all .metric-icon-box { background: #e0f2fe; color: #0284c7; }
.metric-pending .metric-icon-box { background: #fee2e2; color: #ef4444; }
.metric-resolved .metric-icon-box { background: #dcfce7; color: #16a34a; }
.metric-rate .metric-icon-box { background: #fef3c7; color: #d97706; }

.metric-info {
  display: flex;
  flex-direction: column;
}

.metric-num {
  font-size: 24px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.2;
}

.metric-label {
  font-size: 13px;
  color: #64748b;
  margin-top: 2px;
}

/* 过滤工具栏 */
.ticket-filter-toolbar {
  background: #ffffff;
  padding: 14px 20px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  margin-bottom: 16px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-left {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

/* 表格卡片 */
.ticket-table-card {
  background: #ffffff;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  padding: 8px 16px 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.02);
}

.ticket-id-tag {
  font-weight: 700;
  color: #0284c7;
  font-family: monospace;
}

.user-id-pill {
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.type-tag {
  font-weight: 600;
  border-radius: 6px;
}

.ticket-content-cell {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.feedback-text {
  font-size: 14px;
  color: #1e293b;
  line-height: 1.5;
}

.feedback-reply-preview {
  font-size: 12px;
  background: #f0fdf4;
  border: 1px solid #bbf7d0;
  border-radius: 6px;
  padding: 4px 8px;
  display: flex;
  gap: 6px;
  align-items: baseline;
}

.reply-tag {
  font-weight: 700;
  color: #16a34a;
  flex-shrink: 0;
}

.reply-preview-text {
  color: #166534;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.time-col {
  font-size: 12px;
  color: #64748b;
}

/* 状态胶囊 */
.status-capsule {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 600;
}

.status-capsule .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

.status-capsule.resolved {
  background: #dcfce7;
  color: #15803d;
}

.status-capsule.resolved .dot {
  background: #22c55e;
}

.status-capsule.pending {
  background: #fee2e2;
  color: #b91c1c;
}

.status-capsule.pending .dot {
  background: #ef4444;
}

.ticket-op-btn {
  font-weight: 600;
}

/* 工单回复弹窗 */
.custom-reply-dialog :deep(.el-dialog__header) {
  padding: 20px 24px;
  border-bottom: 1px solid #f1f5f9;
  font-size: 16px;
  font-weight: 700;
}

.ticket-detail-box {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  padding: 16px;
}

.t-top-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.t-label {
  font-size: 13px;
  font-weight: 700;
  color: #334155;
}

.t-content {
  font-size: 14px;
  color: #1e293b;
  line-height: 1.6;
}

.t-time {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 8px;
}

.quick-preset-box {
  margin-top: 14px;
  background: #eff6ff;
  border: 1px solid #dbeafe;
  border-radius: 8px;
  padding: 12px;
}

.preset-title {
  font-size: 12px;
  font-weight: 600;
  color: #1d4ed8;
  margin-bottom: 8px;
}

.preset-items {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.preset-chip {
  font-size: 12px;
  color: #1e40af;
  background: #ffffff;
  border: 1px solid #bfdbfe;
  padding: 3px 10px;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.preset-chip:hover {
  background: #1d4ed8;
  color: #ffffff;
  border-color: #1d4ed8;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>