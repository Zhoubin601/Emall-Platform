<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Calendar, Back, ShoppingBag, ChatDotRound, Delete, Box, Select, Wallet, Van } from '@element-plus/icons-vue'
import request from '../../utils/request'
import { useUserStore } from '../../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const orders = ref<any[]>([])
const loading = ref(false)

const activeTab = ref('all')

// === 获取数据 ===
const fetchMyOrders = async () => {
  if (!userStore.userInfo) return router.push('/login')
  loading.value = true
  try {
    const res = await request.get('/order/my', { params: { userId: userStore.userInfo.id } })
    orders.value = res
  } finally {
    loading.value = false
  }
}

// ✨ 核心升级 1：过滤逻辑支持特殊的“退款/售后”分组
const filteredOrders = computed(() => {
  if (activeTab.value === 'all') return orders.value
  if (activeTab.value === 'refund') {
    return orders.value.filter(order => order.status === 5 || order.status === 6)
  }
  return orders.value.filter(order => order.status === Number(activeTab.value))
})

// ✨ 核心升级 2：状态转换字典补齐 5 和 6
const getStatusText = (status: number) => {
  const map: Record<number, string> = { 
    0: '待支付', 1: '待发货', 2: '待收货', 3: '交易完成', 4: '已取消', 5: '已退款', 6: '退款审核中' 
  }
  return map[status] || '未知状态'
}

const getStatusTagType = (status: number) => {
  const map: Record<number, string> = { 
    0: 'danger', 1: 'warning', 2: 'primary', 3: 'success', 4: 'info', 5: 'info', 6: 'danger' 
  }
  return map[status] || 'info'
}

// === 订单操作逻辑 ===

const handleCancel = (order: any) => {
  ElMessageBox.confirm('真的要取消这笔订单吗？', '提示', {
    confirmButtonText: '残忍取消',
    cancelButtonText: '再想想',
    type: 'warning'
  }).then(async () => {
    await request.put(`/order/status/${order.id}/4`)
    ElMessage.success('订单已取消')
    fetchMyOrders() 
  }).catch(() => {})
}

const handlePay = async (order: any) => {
  await request.put(`/order/status/${order.id}/1`)
  ElMessage.success('支付成功，商家将尽快发货！')
  fetchMyOrders()
}

// ✨ 核心升级 3：退款申请真正接入审批流
const handleRefund = (order: any) => {
  ElMessageBox.confirm('确定要申请退款吗？提交后将由商家进行审核，审核通过后资金将原路返回。', '退款申请', {
    confirmButtonText: '提交申请',
    cancelButtonText: '暂不退款',
    type: 'warning'
  }).then(async () => {
    // 将状态从粗暴的 4 改为 6，进入退款冻结状态
    await request.put(`/order/status/${order.id}/6`)
    ElMessage.success('退款申请已提交，请耐心等待商家审核')
    fetchMyOrders()
  }).catch(() => {})
}

const handleConfirmReceipt = (order: any) => {
  ElMessageBox.confirm('确认已经收到宝贝了吗？', '提示', {
    confirmButtonText: '确认收货',
    cancelButtonText: '取消',
    type: 'success'
  }).then(async () => {
    await request.put(`/order/status/${order.id}/3`)
    ElMessage.success('交易完成！快去评价吧~')
    fetchMyOrders()
  })
}

const goToComment = (order: any) => {
  const pId = order.productId || order.product_id || (order.items && order.items[0]?.productId)
  if (!pId) return ElMessage.error("订单数据异常，无法获取商品信息")
  router.push({ path: '/order/comment', query: { orderId: order.id, productId: pId } })
}

const handleDelete = (order: any) => {
  ElMessageBox.confirm('确定要永久删除这条订单记录吗？', '警告', {
    confirmButtonText: '确定删除',
    cancelButtonText: '取消',
    type: 'error'
  }).then(async () => {
    try {
      await request.delete(`/order/${order.id}`)
      ElMessage.success('订单已彻底删除')
      fetchMyOrders() 
    } catch (e) {
      ElMessage.error('删除失败')
    }
  }).catch(() => {})
}

// === 物流查看逻辑 ===
const logisticsDialogVisible = ref(false)
const currentLogisticsSn = ref('')

const mockLogisticsData = ref([
  { content: '派件中，快递员已出发。联系电话：138-0000-8888', timestamp: '今天 09:30', color: '#0ea5e9' },
  { content: '已到达【您所在城市分拨中心】，准备发往营业部', timestamp: '昨天 23:15' },
  { content: '已从【发货地转运中心】发出，正发往目的地', timestamp: '昨天 16:45' },
  { content: '您的包裹已由物流公司揽收', timestamp: '昨天 14:20' },
  { content: '商家已发货，等待快递揽收', timestamp: '昨天 13:00' }
])

const handleViewLogistics = (order: any) => {
  currentLogisticsSn.value = order.orderSn
  logisticsDialogVisible.value = true
}

onMounted(() => fetchMyOrders())
</script>

<template>
  <div class="order-page">
    <header class="glass-header">
      <div class="header-content">
        <el-button link :icon="Back" @click="router.push('/')" class="back-btn">返回</el-button>
        <span class="title">我的订单</span>
      </div>
    </header>

    <main class="container">
      <el-card class="glass-card tabs-card" shadow="never">
        <el-tabs v-model="activeTab" class="custom-tabs">
          <el-tab-pane label="全部" name="all"></el-tab-pane>
          <el-tab-pane label="待支付" name="0"></el-tab-pane>
          <el-tab-pane label="待发货" name="1"></el-tab-pane>
          <el-tab-pane label="待收货" name="2"></el-tab-pane>
          <el-tab-pane label="已完成" name="3"></el-tab-pane>
          <el-tab-pane label="退款/售后" name="refund"></el-tab-pane>
        </el-tabs>
      </el-card>

      <div v-loading="loading">
        <div v-if="filteredOrders.length === 0 && !loading" class="empty-box">
          <el-icon size="64" color="#bae6fd"><ShoppingBag /></el-icon>
          <p>当前状态下没有订单记录哦</p>
        </div>

        <el-card v-for="order in filteredOrders" :key="order.id" class="order-card glass-card">
          <div class="order-header">
            <span class="sn">订单编号：{{ order.orderSn }}</span>
            <el-tag :type="getStatusTagType(order.status)" round effect="light">
              {{ getStatusText(order.status) }}
            </el-tag>
          </div>
          
          <div class="order-body">
            <div class="order-info">
              <div class="total">
                实付金额：<span class="price">¥ {{ order.totalAmount }}</span>
              </div>
              <div class="time"><el-icon><Calendar /></el-icon> 创建时间：{{ order.createTime }}</div>
            </div>

            <div class="order-actions">
              <template v-if="order.status === 0">
                <el-button type="info" plain round size="small" @click="handleCancel(order)">取消订单</el-button>
                <el-button type="danger" round size="small" :icon="Wallet" class="pay-btn" @click="handlePay(order)">立即支付</el-button>
              </template>

              <template v-if="order.status === 1">
                <el-button type="info" plain round size="small" @click="handleRefund(order)">申请退款</el-button>
                <el-button type="primary" plain round size="small">催促发货</el-button>
              </template>

              <template v-if="order.status === 2">
                <el-button type="info" plain round size="small" @click="handleRefund(order)">申请退款</el-button>
                <el-button type="primary" plain round size="small" :icon="Van" @click="handleViewLogistics(order)">查看物流</el-button>
                <el-button type="success" round size="small" :icon="Select" @click="handleConfirmReceipt(order)">确认收货</el-button>
              </template>

              <template v-if="order.status === 3 || order.status === 4">
                <template v-if="order.status === 3">
                  <el-button 
                    v-if="order.commentStatus === 0"
                    type="primary" :icon="ChatDotRound" round size="small" class="comment-btn" @click="goToComment(order)"
                  >
                    评价商品
                  </el-button>
                  <el-button v-else type="info" plain round size="small" disabled>已评价</el-button>
                </template>

                <el-button type="info" plain link :icon="Delete" @click="handleDelete(order)">删除记录</el-button>
              </template>

              <template v-if="order.status === 5">
                <el-button type="info" plain round size="small" disabled>退款已完成</el-button>
                <el-button type="info" plain link :icon="Delete" @click="handleDelete(order)">删除记录</el-button>
              </template>

              <template v-if="order.status === 6">
                <el-button type="danger" plain round size="small" disabled class="auditing-btn">
                  退款审核中...
                </el-button>
              </template>
            </div>
          </div>
        </el-card>
      </div>
    </main>

    <el-dialog 
      v-model="logisticsDialogVisible" 
      title="物流跟踪详情" 
      width="450px" 
      class="cute-dialog"
      destroy-on-close
    >
      <div class="logistics-header">
        <el-icon class="l-icon"><Box /></el-icon>
        <div class="l-info">
          <div class="l-company">顺丰速运</div>
          <div class="l-sn">运单号：SF{{ currentLogisticsSn.substring(0, 12) }}</div>
        </div>
      </div>
      
      <el-timeline class="logistics-timeline">
        <el-timeline-item
          v-for="(activity, index) in mockLogisticsData"
          :key="index"
          :color="activity.color || '#cbd5e1'"
          :timestamp="activity.timestamp"
          placement="top"
        >
          <div class="timeline-content" :style="{ color: index === 0 ? '#0f172a' : '#64748b', fontWeight: index === 0 ? 'bold' : 'normal' }">
            {{ activity.content }}
          </div>
        </el-timeline-item>
      </el-timeline>
      
      <template #footer>
        <el-button type="primary" round class="close-btn" @click="logisticsDialogVisible = false">我知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.order-page { min-height: 100vh; background: #f0f9ff; padding-top: 80px; padding-bottom: 50px; }
.glass-header { position: fixed; top: 0; left: 0; right: 0; height: 60px; background: rgba(255, 255, 255, 0.7); backdrop-filter: blur(12px); display: flex; justify-content: center; z-index: 100; border-bottom: 1px solid #e0f2fe; }
.header-content { width: 800px; display: flex; align-items: center; padding: 0 20px; }
.back-btn { font-size: 14px; font-weight: bold; color: #0369a1; }
.title { margin-left: 20px; font-weight: bold; color: #0369a1; }

.container { width: 800px; max-width: 95%; margin: 0 auto; }
.glass-card { border-radius: 18px; border: 1px solid rgba(186, 230, 253, 0.5); background: rgba(255, 255, 255, 0.85); box-shadow: 0 8px 25px rgba(2, 132, 199, 0.05); margin-bottom: 20px; }
.tabs-card { padding: 5px 20px; margin-bottom: 25px; }

:deep(.custom-tabs .el-tabs__nav-wrap::after) { display: none; }
:deep(.custom-tabs .el-tabs__item) { font-size: 15px; font-weight: bold; color: #64748b; }
:deep(.custom-tabs .el-tabs__item.is-active) { color: #0284c7; }
:deep(.custom-tabs .el-tabs__active-bar) { background-color: #0284c7; border-radius: 4px; }

.order-header { display: flex; justify-content: space-between; border-bottom: 1px dashed #e2e8f0; padding-bottom: 15px; margin-bottom: 15px; }
.sn { color: #475569; font-size: 13px; font-weight: bold; }

.order-body { display: flex; justify-content: space-between; align-items: flex-end; }
.price { color: #f43f5e; font-size: 24px; font-weight: 900; margin-left: 5px; }
.time { color: #94a3b8; font-size: 12px; margin-top: 8px; display: flex; align-items: center; gap: 4px; }

.order-actions { display: flex; gap: 10px; align-items: center; }
.pay-btn { background-color: #f43f5e; border: none; font-weight: bold; }
.pay-btn:hover { background-color: #e11d48; }
.comment-btn { background-color: #0ea5e9; border: none; font-weight: bold; }
.comment-btn:hover { background-color: #0284c7; }
/* 审核中按钮动画 */
.auditing-btn { border-color: #fca5a5; color: #ef4444; background: #fef2f2; opacity: 0.8; }

.empty-box { text-align: center; padding-top: 80px; padding-bottom: 50px; color: #94a3b8; }
.empty-box p { margin-top: 15px; font-weight: bold; }

/* 物流弹窗专属样式 */
:deep(.cute-dialog) { border-radius: 16px; overflow: hidden; }
:deep(.cute-dialog .el-dialog__header) { background: #f8fafc; margin-right: 0; border-bottom: 1px solid #f1f5f9; padding: 20px; font-weight: bold; color: #0f172a; }
.logistics-header { display: flex; align-items: center; gap: 15px; background: #f0f9ff; padding: 15px; border-radius: 12px; margin-bottom: 25px; border: 1px solid #bae6fd; }
.l-icon { font-size: 32px; color: #0ea5e9; }
.l-company { font-weight: bold; color: #0369a1; font-size: 15px; margin-bottom: 4px; }
.l-sn { font-size: 13px; color: #64748b; }
.logistics-timeline { padding-left: 5px; margin-top: 15px; }
.timeline-content { font-size: 14px; line-height: 1.5; margin-top: 5px; }
.close-btn { width: 100%; font-weight: bold; background: #f1f5f9; color: #475569; border: none; }
.close-btn:hover { background: #e2e8f0; }
</style>