<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { 
  Calendar, ShoppingBag, Delete, Van, 
  Wallet, DocumentCopy, ChatLineSquare
} from '@element-plus/icons-vue'
import request from '../../utils/request'
import { useUserStore } from '../../stores/user'
import { ElMessage, ElMessageBox } from 'element-plus'
import ClientHeader from '../../components/ClientHeader.vue'
import ClientFooter from '../../components/ClientFooter.vue'

const router = useRouter()
const userStore = useUserStore()
const orders = ref<any[]>([])
const loading = ref(false)

const activeTab = ref('all')

const fetchMyOrders = async () => {
  if (!userStore.userInfo) return router.push('/login')
  loading.value = true
  try {
    const res = await request.get('/order/my', { params: { userId: userStore.userInfo.id } })
    orders.value = res || []
  } finally {
    loading.value = false
  }
}

const filteredOrders = computed(() => {
  if (activeTab.value === 'all') return orders.value
  if (activeTab.value === 'refund') {
    return orders.value.filter(order => order.status === 5 || order.status === 6)
  }
  return orders.value.filter(order => order.status === Number(activeTab.value))
})

const getStatusText = (status: number) => {
  const map: Record<number, string> = { 
    0: '待付款', 1: '待发货', 2: '已发货 (待收货)', 3: '交易完成', 4: '已取消', 5: '已退款', 6: '退款审核中' 
  }
  return map[status] || '未知状态'
}

const getStatusTagType = (status: number) => {
  const map: Record<number, string> = { 
    0: 'danger', 1: 'warning', 2: 'primary', 3: 'success', 4: 'info', 5: 'info', 6: 'danger' 
  }
  return map[status] || 'info'
}

const getStepActive = (status: number) => {
  if (status === 0) return 1
  if (status === 1) return 2
  if (status === 2) return 3
  if (status === 3) return 4
  return 0
}

const copyOrderSn = (sn: string) => {
  navigator.clipboard.writeText(sn)
  ElMessage.success('订单编号已复制到剪贴板！')
}

// 订单操作
const handleCancel = (order: any) => {
  ElMessageBox.confirm('确定要取消这笔订单吗？取消后库存将自动解冻回补。', '取消订单确认', {
    confirmButtonText: '确定取消',
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
  ElMessage.success('支付成功，顺丰仓配将以最快时效安排发货！')
  fetchMyOrders()
}

const handleRefund = (order: any) => {
  ElMessageBox.confirm('确定要申请退款吗？提交后将由运营客服进行审核，审核通过后资金将原路返回。', '退款申请', {
    confirmButtonText: '提交申请',
    cancelButtonText: '暂不退款',
    type: 'warning'
  }).then(async () => {
    await request.put(`/order/status/${order.id}/6`)
    ElMessage.success('退款申请已提交，请耐心等待审核')
    fetchMyOrders()
  }).catch(() => {})
}

const handleRemindDelivery = () => {
  ElMessage.success('已为您催促仓库优先分拣，顺丰快递将以最快速度揽收发运！')
}

const handleConfirmReceipt = (order: any) => {
  ElMessageBox.confirm('确认已经收到包裹并检查无误了吗？确认后款项将结算给商家。', '确认收货', {
    confirmButtonText: '确认收货',
    cancelButtonText: '取消',
    type: 'success'
  }).then(async () => {
    await request.put(`/order/status/${order.id}/3`)
    ElMessage.success('交易完成！快去给心仪宝贝写个好评晒单吧~')
    fetchMyOrders()
  })
}

const goToComment = (order: any) => {
  const pId = order.productId || order.product_id || (order.items && order.items[0]?.productId)
  if (!pId) return ElMessage.error("订单数据异常，无法获取商品信息")
  router.push({ path: '/order/comment', query: { orderId: order.id, productId: pId } })
}

const handleDelete = (order: any) => {
  ElMessageBox.confirm('确定要彻底删除这条订单记录吗？删除后将不可恢复。', '删除记录警告', {
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

// 物流查看
const logisticsDialogVisible = ref(false)
const currentLogisticsSn = ref('')

const mockLogisticsData = ref([
  { content: '【顺丰速运】派件中，快递员已出发。联系电话：95338', timestamp: '今天 09:30', color: '#0ea5e9' },
  { content: '到达【您所在城市分拨中心】，准备发往就近网点', timestamp: '昨天 23:15' },
  { content: '快件离开【北京顺义转运中心】，发往目的地', timestamp: '昨天 16:45' },
  { content: '顺丰速运 已在 E-MALL 官方旗舰仓完成揽收', timestamp: '昨天 14:20' },
  { content: '商品出库打包完成，等待快递交接', timestamp: '昨天 13:00' }
])

const handleViewLogistics = (order: any) => {
  currentLogisticsSn.value = order.orderSn
  logisticsDialogVisible.value = true
}

onMounted(() => fetchMyOrders())
</script>

<template>
  <div class="orders-page-layout">
    <ClientHeader />

    <main class="orders-container">
      <!-- 面包屑导航 -->
      <nav class="breadcrumb-bar">
        <span class="crumb-link" @click="router.push('/')">首页</span>
        <span class="crumb-sep">/</span>
        <span class="crumb-link" @click="router.push('/profile')">个人中心</span>
        <span class="crumb-sep">/</span>
        <span class="crumb-current">我的订单</span>
      </nav>

      <!-- 状态筛选 Tab 栏 -->
      <div class="tabs-filter-card">
        <div 
          class="tab-pill"
          :class="{ active: activeTab === 'all' }"
          @click="activeTab = 'all'"
        >
          全部订单 <span class="badge" v-if="orders.length > 0">{{ orders.length }}</span>
        </div>
        <div 
          class="tab-pill"
          :class="{ active: activeTab === '0' }"
          @click="activeTab = '0'"
        >
          待付款 <span class="badge red" v-if="orders.filter(o => o.status === 0).length > 0">{{ orders.filter(o => o.status === 0).length }}</span>
        </div>
        <div 
          class="tab-pill"
          :class="{ active: activeTab === '1' }"
          @click="activeTab = '1'"
        >
          待发货 <span class="badge blue" v-if="orders.filter(o => o.status === 1).length > 0">{{ orders.filter(o => o.status === 1).length }}</span>
        </div>
        <div 
          class="tab-pill"
          :class="{ active: activeTab === '2' }"
          @click="activeTab = '2'"
        >
          待收货 <span class="badge blue" v-if="orders.filter(o => o.status === 2).length > 0">{{ orders.filter(o => o.status === 2).length }}</span>
        </div>
        <div 
          class="tab-pill"
          :class="{ active: activeTab === '3' }"
          @click="activeTab = '3'"
        >
          已完成
        </div>
        <div 
          class="tab-pill"
          :class="{ active: activeTab === 'refund' }"
          @click="activeTab = 'refund'"
        >
          退款 / 售后 <span class="badge red" v-if="orders.filter(o => o.status === 5 || o.status === 6).length > 0">{{ orders.filter(o => o.status === 5 || o.status === 6).length }}</span>
        </div>
      </div>

      <!-- 订单列表 -->
      <div v-loading="loading" class="order-list-box">
        <!-- 空状态 -->
        <div v-if="filteredOrders.length === 0 && !loading" class="empty-orders-view">
          <div class="empty-icon-circle">
            <el-icon :size="64" color="#cbd5e1"><ShoppingBag /></el-icon>
          </div>
          <h3>暂无相关订单记录</h3>
          <p>当前筛选状态下没有找到任何订单，去发现更多严选好物吧~</p>
          <el-button type="primary" round class="go-shop-btn" @click="router.push('/')">
            去商城逛逛
          </el-button>
        </div>

        <!-- 订单卡片列表 -->
        <div v-for="order in filteredOrders" :key="order.id" class="order-box-card">
          <!-- 卡片头部 -->
          <div class="card-header-bar">
            <div class="header-left">
              <span class="order-time"><el-icon><Calendar /></el-icon> {{ order.createTime }}</span>
              <span class="order-sn">
                订单号：<strong>{{ order.orderSn }}</strong>
                <el-icon class="copy-btn" @click="copyOrderSn(order.orderSn)" title="复制单号"><DocumentCopy /></el-icon>
              </span>
              <span class="vendor-tag">E-MALL 官方自营旗舰</span>
            </div>
            <div class="header-right">
              <el-tag :type="getStatusTagType(order.status)" effect="dark" class="status-pill">
                {{ getStatusText(order.status) }}
              </el-tag>
            </div>
          </div>

          <!-- 进度步骤指示条 (正常流转订单显示) -->
          <div class="order-timeline-steps" v-if="order.status <= 3">
            <el-steps :active="getStepActive(order.status)" finish-status="success" align-center size="small">
              <el-step title="提交订单" />
              <el-step title="付款成功" />
              <el-step title="顺丰速运出库" />
              <el-step title="确认收货完成" />
            </el-steps>
          </div>

          <!-- 订单内容与价格明细 -->
          <div class="card-body-content">
            <div class="order-goods-summary">
              <div class="goods-desc-line">
                <span class="store-badge">自营仓直发</span>
                <span class="goods-name">商品订单共包含所选精选商品，全链路正品溯源保障</span>
              </div>
            </div>

            <div class="order-price-summary">
              <span class="total-label">实付款：</span>
              <span class="total-curr">¥</span>
              <span class="total-price">{{ order.totalAmount }}</span>
              <span class="freight-hint">(含顺丰保价运费)</span>
            </div>

            <!-- 操作按钮组 -->
            <div class="order-actions-zone">
              <!-- 待付款 -->
              <template v-if="order.status === 0">
                <el-button type="danger" size="default" class="act-btn btn-primary-red" :icon="Wallet" @click="handlePay(order)">
                  立即付款
                </el-button>
                <el-button size="default" class="act-btn" @click="handleCancel(order)">
                  取消订单
                </el-button>
              </template>

              <!-- 待发货 -->
              <template v-if="order.status === 1">
                <el-button size="default" class="act-btn" @click="handleRemindDelivery">
                  催促发货
                </el-button>
                <el-button size="default" class="act-btn" @click="handleRefund(order)">
                  申请退款
                </el-button>
              </template>

              <!-- 待收货 -->
              <template v-if="order.status === 2">
                <el-button type="primary" size="default" class="act-btn" :icon="Van" @click="handleViewLogistics(order)">
                  查看物流
                </el-button>
                <el-button type="success" size="default" class="act-btn" @click="handleConfirmReceipt(order)">
                  确认收货
                </el-button>
                <el-button size="default" class="act-btn" @click="handleRefund(order)">
                  申请售后退款
                </el-button>
              </template>

              <!-- 已完成 -->
              <template v-if="order.status === 3">
                <el-button type="primary" size="default" class="act-btn" :icon="ChatLineSquare" @click="goToComment(order)">
                  评价晒单
                </el-button>
                <el-button size="default" class="act-btn" :icon="Van" @click="handleViewLogistics(order)">
                  物流记录
                </el-button>
                <el-button size="default" class="act-btn" @click="router.push('/')">
                  再次购买
                </el-button>
                <el-button link type="danger" :icon="Delete" @click="handleDelete(order)">
                  删除
                </el-button>
              </template>

              <!-- 已取消 / 已退款 -->
              <template v-if="order.status === 4 || order.status === 5">
                <el-button size="default" class="act-btn" @click="router.push('/')">
                  重新选购
                </el-button>
                <el-button link type="danger" :icon="Delete" @click="handleDelete(order)">
                  删除记录
                </el-button>
              </template>

              <!-- 退款审核中 -->
              <template v-if="order.status === 6">
                <el-tag type="warning" effect="plain" class="audit-tag">
                  运营管家正在加急审核您的退款
                </el-tag>
              </template>
            </div>
          </div>
        </div>
      </div>
    </main>

    <!-- 顺丰速运物流时效模态框 -->
    <el-dialog v-model="logisticsDialogVisible" title="顺丰速运 · 真实轨迹跟踪" width="560px" class="sf-logistics-dialog" append-to-body>
      <div class="logistics-modal-content">
        <div class="sf-header-card">
          <div class="sf-brand">
            <span class="sf-logo">SF</span>
            <span class="sf-name">顺丰特快 (冷链温控 / 易碎保价)</span>
          </div>
          <div class="sf-sn">运单号：SF{{ currentLogisticsSn?.replace(/[^0-9]/g, '').slice(-12) || '102938475612' }}</div>
        </div>

        <el-timeline class="sf-timeline">
          <el-timeline-item
            v-for="(activity, index) in mockLogisticsData"
            :key="index"
            :type="index === 0 ? 'primary' : 'info'"
            :color="activity.color"
            :timestamp="activity.timestamp"
            placement="top"
          >
            <div class="timeline-activity-text" :class="{ highlight: index === 0 }">
              {{ activity.content }}
            </div>
          </el-timeline-item>
        </el-timeline>
      </div>
    </el-dialog>

    <ClientFooter />
  </div>
</template>

<style scoped>
.orders-page-layout {
  min-height: 100vh;
  background-color: #f8fafc;
  display: flex;
  flex-direction: column;
}

.orders-container {
  width: 1220px;
  max-width: 96%;
  margin: 0 auto;
  padding: 24px 0 60px;
  flex: 1;
}

/* 面包屑 */
.breadcrumb-bar {
  padding: 0 0 16px;
  font-size: 13px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 8px;
}

.crumb-link {
  cursor: pointer;
  transition: color 0.2s;
}

.crumb-link:hover {
  color: #0284c7;
}

.crumb-sep {
  color: #cbd5e1;
}

.crumb-current {
  color: #0f172a;
  font-weight: bold;
}

/* 状态 Tab 栏 */
.tabs-filter-card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.02);
  display: flex;
  padding: 6px;
  margin-bottom: 24px;
}

.tab-pill {
  flex: 1;
  text-align: center;
  padding: 12px 10px;
  font-size: 14px;
  font-weight: bold;
  color: #64748b;
  border-radius: 12px;
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}

.tab-pill:hover {
  color: #0284c7;
}

.tab-pill.active {
  background: linear-gradient(135deg, #0284c7, #0369a1);
  color: #ffffff;
}

.badge {
  background: #f1f5f9;
  color: #475569;
  font-size: 11px;
  padding: 1px 7px;
  border-radius: 10px;
}

.tab-pill.active .badge {
  background: rgba(255, 255, 255, 0.3);
  color: #ffffff;
}

.badge.red { background-color: #ffe4e6; color: #e11d48; }
.badge.blue { background-color: #e0f2fe; color: #0284c7; }

/* 订单卡片 */
.order-list-box {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.order-box-card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.02);
  overflow: hidden;
  transition: transform 0.2s, box-shadow 0.2s;
}

.order-box-card:hover {
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.05);
}

.card-header-bar {
  background-color: #f8fafc;
  border-bottom: 1px solid #e2e8f0;
  padding: 14px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 20px;
  font-size: 13px;
  color: #64748b;
}

.order-time {
  display: flex;
  align-items: center;
  gap: 4px;
}

.order-sn {
  display: flex;
  align-items: center;
  gap: 6px;
}

.copy-btn {
  cursor: pointer;
  color: #0ea5e9;
}

.copy-btn:hover {
  color: #0284c7;
}

.vendor-tag {
  background-color: #f0f9ff;
  color: #0284c7;
  font-size: 11px;
  font-weight: bold;
  padding: 2px 8px;
  border-radius: 4px;
}

.status-pill {
  font-weight: bold;
  font-size: 12px;
  border-radius: 12px;
}

/* 流程图 */
.order-timeline-steps {
  padding: 20px 40px;
  border-bottom: 1px solid #f8fafc;
  background-color: #fcfdfe;
}

/* 卡片主体 */
.card-body-content {
  padding: 20px 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.order-goods-summary {
  flex: 1;
}

.goods-desc-line {
  display: flex;
  align-items: center;
  gap: 10px;
}

.store-badge {
  background-color: #ffe4e6;
  color: #e11d48;
  font-size: 11px;
  font-weight: bold;
  padding: 2px 8px;
  border-radius: 4px;
}

.goods-name {
  font-size: 14px;
  color: #334155;
}

.order-price-summary {
  width: 220px;
  display: flex;
  align-items: baseline;
  justify-content: center;
}

.total-label {
  font-size: 12px;
  color: #64748b;
}

.total-curr {
  font-size: 16px;
  font-weight: bold;
  color: #f43f5e;
}

.total-price {
  font-size: 24px;
  font-weight: 900;
  color: #f43f5e;
}

.freight-hint {
  font-size: 11px;
  color: #94a3b8;
  margin-left: 4px;
}

/* 操作按钮 */
.order-actions-zone {
  display: flex;
  align-items: center;
  gap: 10px;
}

.act-btn {
  border-radius: 20px;
  font-weight: bold;
  font-size: 13px;
}

.btn-primary-red {
  background: linear-gradient(135deg, #0284c7, #0369a1);
  border: none;
  color: #ffffff;
}

.audit-tag {
  border-radius: 12px;
  font-size: 12px;
}

/* 空状态 */
.empty-orders-view {
  text-align: center;
  padding: 80px 20px;
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid #e2e8f0;
}

.empty-icon-circle {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  background: #f8fafc;
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 0 auto 16px;
}

.empty-orders-view h3 {
  margin: 0 0 8px;
  font-size: 18px;
  color: #0f172a;
}

.empty-orders-view p {
  margin: 0 0 24px;
  font-size: 13px;
  color: #64748b;
}

.go-shop-btn {
  background: linear-gradient(135deg, #0284c7, #0369a1);
  border: none;
  font-size: 14px;
  font-weight: bold;
  padding: 0 32px;
}

/* 顺丰模态框 */
.sf-header-card {
  background: linear-gradient(135deg, #0f172a, #1e293b);
  color: #ffffff;
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 24px;
}

.sf-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.sf-logo {
  background-color: #f43f5e;
  color: #ffffff;
  font-weight: 900;
  font-size: 14px;
  padding: 2px 8px;
  border-radius: 4px;
}

.sf-name {
  font-size: 15px;
  font-weight: bold;
}

.sf-sn {
  font-size: 12px;
  color: #94a3b8;
}

.timeline-activity-text {
  font-size: 13px;
  color: #475569;
  line-height: 1.5;
}

.timeline-activity-text.highlight {
  color: #0ea5e9;
  font-weight: bold;
}
</style>