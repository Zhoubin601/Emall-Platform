<script setup lang="ts">
import { ref, onMounted, reactive, computed } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { 
  Ticket, View, Search, Refresh, ArrowDown, Download, 
  Check, Close, Van, CircleClose, Setting, Delete, 
  CopyDocument, Box, Warning, Clock, Picture
} from '@element-plus/icons-vue'

interface OrderItem {
  id: number;
  orderId?: number;
  productId: number;
  productName: string;
  productPrice: number;
  productCount?: number;
  quantity?: number;
  productPic?: string;
  spec?: string;
}

interface Order {
  id: number; 
  orderSn: string; 
  userId: number; 
  totalAmount: number | string; 
  payAmount?: number | string;
  status: number; 
  createTime: string;
  items?: OrderItem[]; 
}

const currentPage = ref(1)
const pageSize = ref(10)
const totalOrders = ref(0) 
const orderList = ref<Order[]>([])
const loading = ref(false)
const exportLoading = ref(false)
const searchForm = reactive({
  orderSn: '',
  userId: '',
  status: '' as number | string
})

const activeTab = ref('all')

const orderStats = computed(() => {
  const total = orderList.value.length
  const pendingShip = orderList.value.filter(o => o.status === 1).length
  const shipping = orderList.value.filter(o => o.status === 2).length
  const refundReq = orderList.value.filter(o => o.status === 6).length
  const completed = orderList.value.filter(o => o.status === 3).length
  const pendingPay = orderList.value.filter(o => o.status === 0).length
  return {
    total,
    pendingShip,
    shipping,
    refundReq,
    completed,
    pendingPay
  }
})

const detailVisible = ref(false)
const detailLoading = ref(false)
const currentOrder = ref<Order | null>(null)

const statusVisible = ref(false)
const statusForm = reactive({
  id: 0,
  status: 0
})

const fetchOrders = async () => {
  loading.value = true
  try {
    const res: any = await request.get('/order/list', {
      params: {
        page: currentPage.value,
        size: pageSize.value,
        orderSn: searchForm.orderSn || undefined,
        userId: searchForm.userId || undefined,
        status: searchForm.status !== '' ? searchForm.status : undefined
      }
    })
    orderList.value = res.records || res; 
    totalOrders.value = res.total || (Array.isArray(res) ? res.length : 0);
  } catch (error) {
    ElMessage.error('获取订单列表失败')
  } finally {
    loading.value = false
  }
}

const handleTabChange = (tabName: string) => {
  activeTab.value = tabName
  if (tabName === 'all') {
    searchForm.status = ''
  } else {
    searchForm.status = Number(tabName)
  }
  currentPage.value = 1
  fetchOrders()
}

const handleSearch = () => {
  currentPage.value = 1 
  fetchOrders()
}

const handleReset = () => {
  searchForm.orderSn = ''
  searchForm.userId = ''
  searchForm.status = ''
  activeTab.value = 'all'
  currentPage.value = 1
  fetchOrders()
}

const handleSizeChange = (val: number) => {
  pageSize.value = val; fetchOrders() 
}
const handleCurrentChange = (val: number) => {
  currentPage.value = val; fetchOrders() 
}

const getStatusText = (status: number) => {
  const map: Record<number, string> = { 
    0: '待支付', 1: '待发货', 2: '已发货', 3: '已完成', 4: '已取消', 5: '已退款', 6: '退款申请中' 
  }
  return map[status] || '未知状态'
}


const updateOrderStatus = async (id: number, status: number, successMsg: string) => {
  try {
    await request.put(`/order/status/${id}/${status}`)
    ElMessage.success(successMsg)
    fetchOrders()
  } catch (e) {
    ElMessage.error('操作失败，请重试')
  }
}

const copyOrderSn = (sn: string) => {
  if (navigator.clipboard && navigator.clipboard.writeText) {
    navigator.clipboard.writeText(sn)
    ElMessage.success(`订单号已复制: ${sn}`)
  } else {
    ElMessage.info(`单号: ${sn}`)
  }
}

const handleCommand = (command: { type: string, row?: Order, id?: number }) => {
  const { type, row, id } = command
  
  if (type === 'ship' && id) {
    ElMessageBox.confirm('确认要将该订单标记为已发货吗？', '发货确认', { type: 'warning' })
      .then(() => updateOrderStatus(id, 2, '发货成功！'))
      .catch(() => {})
  } else if (type === 'cancel' && id) {
    ElMessageBox.confirm('确认要强制取消该订单吗？', '取消确认', { type: 'warning' })
      .then(() => updateOrderStatus(id, 4, '订单已取消！'))
      .catch(() => {})
  } 
  else if (type === 'approveRefund' && id) {
    ElMessageBox.confirm('确认已通过支付渠道原路退回款项？点击确认将订单标记为“已退款”。', '同意退款', { confirmButtonText: '确认退款', type: 'warning' })
      .then(() => updateOrderStatus(id, 5, '退款处理完成，买家将收到通知！'))
      .catch(() => {})
  } 
  else if (type === 'rejectRefund' && id) {
    ElMessageBox.confirm('确认拒绝该买家的退款申请吗？订单将恢复为正常状态。', '拒绝退款', { confirmButtonText: '狠心拒绝', type: 'error' })
      .then(() => updateOrderStatus(id, 2, '已拒绝退款申请！'))
      .catch(() => {})
  } 
  else if (type === 'modify' && row) {
    statusForm.id = row.id
    statusForm.status = row.status
    statusVisible.value = true
  } else if (type === 'delete' && id) {
    ElMessageBox.confirm('此操作不可撤销，确定彻底删除？', '警告', { type: 'error', confirmButtonText: '强制删除' })
      .then(async () => {
        await request.delete(`/order/${id}`)
        ElMessage.success('订单已删除')
        fetchOrders()
      })
      .catch(() => {})
  }
}

const submitStatusModify = () => {
  updateOrderStatus(statusForm.id, statusForm.status, '状态修改成功！')
  statusVisible.value = false
}

const handleViewDetail = async (id: number) => {
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res: any = await request.get(`/order/detail/${id}`)
    currentOrder.value = res
  } catch (e) {
    ElMessage.error('获取详情失败')
  } finally {
    detailLoading.value = false
  }
}

const handleExport = async () => {
  exportLoading.value = true
  try {
    ElMessage.info('正在生成订单报表，请稍候...')
    const res: any = await request.get('/order/export', {
      params: {
        orderSn: searchForm.orderSn || undefined,
        userId: searchForm.userId || undefined,
        status: searchForm.status !== '' ? searchForm.status : undefined
      },
      responseType: 'blob'
    })
    const blob = new Blob([res], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    })
    const downloadUrl = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.style.display = 'none'
    link.href = downloadUrl
    link.setAttribute('download', `E-MALL订单报表_${new Date().getTime()}.xlsx`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(downloadUrl)
    ElMessage.success('订单报表导出成功！')
  } catch (error) {
    ElMessage.error('导出失败，请检查网络或权限状态')
  } finally {
    exportLoading.value = false
  }
}

onMounted(() => fetchOrders())
</script>

<template>
  <div class="order-container">
    <!-- 1. 履约调度控制台顶部 Bento 管道指标 -->
    <div class="order-pipeline-grid">
      <div 
        class="pipeline-card" 
        :class="{ active: activeTab === 'all' }" 
        @click="handleTabChange('all')"
      >
        <div class="p-icon blue"><el-icon><Ticket /></el-icon></div>
        <div class="p-body">
          <div class="p-title">全域总订单</div>
          <div class="p-num">{{ orderStats.total }} <span class="unit">单</span></div>
        </div>
        <span class="p-status-pill">全部</span>
      </div>

      <div 
        class="pipeline-card highlight-orange" 
        :class="{ active: activeTab === '1' }" 
        @click="handleTabChange('1')"
      >
        <div class="p-icon orange"><el-icon><Box /></el-icon></div>
        <div class="p-body">
          <div class="p-title">待发货履约</div>
          <div class="p-num">{{ orderStats.pendingShip }} <span class="unit">单</span></div>
        </div>
        <span class="p-status-pill orange-pill" v-if="orderStats.pendingShip > 0">需打单</span>
      </div>

      <div 
        class="pipeline-card" 
        :class="{ active: activeTab === '2' }" 
        @click="handleTabChange('2')"
      >
        <div class="p-icon sky"><el-icon><Van /></el-icon></div>
        <div class="p-body">
          <div class="p-title">干线运输中</div>
          <div class="p-num">{{ orderStats.shipping }} <span class="unit">单</span></div>
        </div>
        <span class="p-status-pill">在途</span>
      </div>

      <div 
        class="pipeline-card highlight-red" 
        :class="{ active: activeTab === '6' }" 
        @click="handleTabChange('6')"
      >
        <div class="p-icon red"><el-icon><Warning /></el-icon></div>
        <div class="p-body">
          <div class="p-title">售后退款审批</div>
          <div class="p-num text-danger">{{ orderStats.refundReq }} <span class="unit">单</span></div>
        </div>
        <span class="p-status-pill red-pill" v-if="orderStats.refundReq > 0">紧急处理</span>
      </div>
    </div>

    <!-- 2. 主卡片与操作工具栏 -->
    <el-card class="main-order-card" shadow="never">
      <!-- 管道式快捷筛选标签栏 -->
      <div class="pipeline-tab-bar">
        <div 
          class="tab-item" 
          :class="{ active: activeTab === 'all' }" 
          @click="handleTabChange('all')"
        >
          全部订单
        </div>
        <div 
          class="tab-item" 
          :class="{ active: activeTab === '1' }" 
          @click="handleTabChange('1')"
        >
          待发货
          <span class="tab-badge" v-if="orderStats.pendingShip > 0">{{ orderStats.pendingShip }}</span>
        </div>
        <div 
          class="tab-item" 
          :class="{ active: activeTab === '2' }" 
          @click="handleTabChange('2')"
        >
          运输中
        </div>
        <div 
          class="tab-item" 
          :class="{ active: activeTab === '3' }" 
          @click="handleTabChange('3')"
        >
          已完成
        </div>
        <div 
          class="tab-item alert-tab" 
          :class="{ active: activeTab === '6' }" 
          @click="handleTabChange('6')"
        >
          退款审批
          <span class="tab-badge red" v-if="orderStats.refundReq > 0">{{ orderStats.refundReq }}</span>
        </div>
        <div 
          class="tab-item" 
          :class="{ active: activeTab === '0' }" 
          @click="handleTabChange('0')"
        >
          待支付
        </div>
      </div>

      <!-- 搜索表单栏 -->
      <div class="filter-toolbar">
        <div class="search-inputs">
          <el-input 
            v-model="searchForm.orderSn" 
            placeholder="订单编号..." 
            clearable 
            :prefix-icon="Search"
            class="input-sn"
          />
          <el-input 
            v-model="searchForm.userId" 
            placeholder="买家 ID" 
            clearable 
            class="input-uid"
          />
          <el-button type="primary" :icon="Search" @click="handleSearch" class="btn-search">查询</el-button>
          <el-button :icon="Refresh" @click="handleReset">重置</el-button>
        </div>

        <div class="export-actions">
          <el-button 
            type="success" 
            plain 
            :icon="Download" 
            @click="handleExport"
            :loading="exportLoading"
            class="btn-export"
          >
            导出 Excel 报表
          </el-button>
        </div>
      </div>

      <!-- 核心订单列表表格 -->
      <el-table :data="orderList" v-loading="loading" class="custom-admin-table" style="width: 100%">
        <!-- 订单单号与下单时间 -->
        <el-table-column label="订单编号 / 下单时间" min-width="210">
          <template #default="{ row }">
            <div class="order-id-cell">
              <div class="sn-row">
                <span class="sn-text">{{ row.orderSn }}</span>
                <el-button 
                  link 
                  :icon="CopyDocument" 
                  class="copy-btn" 
                  title="复制订单号" 
                  @click="copyOrderSn(row.orderSn)" 
                />
              </div>
              <div class="order-time">
                <el-icon><Clock /></el-icon> {{ row.createTime }}
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- 订购商品预览 (核心体验增强：直接看到买的什么) -->
        <el-table-column label="订购商品明细" min-width="280">
          <template #default="{ row }">
            <div class="order-items-preview" v-if="row.items && row.items.length">
              <div class="item-row" v-for="item in row.items.slice(0, 2)" :key="item.id">
                <el-image 
                  :src="item.productPic" 
                  class="order-thumb" 
                  fit="cover"
                  :preview-src-list="item.productPic ? [item.productPic] : []"
                  preview-teleported
                >
                  <template #error>
                    <div class="thumb-err"><el-icon><Picture /></el-icon></div>
                  </template>
                </el-image>
                <div class="item-meta">
                  <div class="item-name" :title="item.productName">{{ item.productName }}</div>
                  <div class="item-sub">
                    <span class="item-spec" v-if="item.spec">{{ item.spec }}</span>
                    <span class="item-qty">× {{ item.quantity || item.productCount || 1 }}</span>
                    <span class="item-price">¥{{ item.productPrice }}</span>
                  </div>
                </div>
              </div>
              <div v-if="row.items.length > 2" class="more-items-tip">
                共 {{ row.items.length }} 种商品，点击详情查看全部
              </div>
            </div>
            <div v-else class="empty-item-tip">
              暂无快照数据
            </div>
          </template>
        </el-table-column>

        <!-- 买家信息 -->
        <el-table-column prop="userId" label="买家" width="100" align="center">
          <template #default="{ row }">
            <span class="user-pill">UID: {{ row.userId }}</span>
          </template>
        </el-table-column>

        <!-- 金额体系 -->
        <el-table-column label="实付金额" width="130" align="center">
          <template #default="{ row }">
            <div class="amount-cell">
              <span class="main-amount">¥ {{ row.totalAmount }}</span>
              <span class="pay-tag" v-if="row.status !== 0">已实付</span>
              <span class="unpaid-tag" v-else>待付款</span>
            </div>
          </template>
        </el-table-column>

        <!-- 履约状态 -->
        <el-table-column label="履约状态" width="130" align="center">
          <template #default="{ row }">
            <div class="status-capsule" :class="'status-' + row.status">
              <span class="status-dot"></span>
              <span class="status-text">{{ getStatusText(row.status) }}</span>
            </div>
          </template>
        </el-table-column>

        <!-- 快捷操作列 -->
        <el-table-column label="履约与管理" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <div class="order-actions">
              <!-- 待发货：一键发货 -->
              <el-button 
                v-if="row.status === 1" 
                type="warning" 
                size="small" 
                class="quick-action-btn"
                :icon="Van" 
                @click="handleCommand({ type: 'ship', id: row.id })"
              >
                立即发货
              </el-button>

              <!-- 退款审批中：同意/拒绝快捷操作 -->
              <template v-else-if="row.status === 6">
                <el-button 
                  type="success" 
                  size="small" 
                  class="quick-action-btn"
                  :icon="Check" 
                  @click="handleCommand({ type: 'approveRefund', id: row.id })"
                >
                  同意退款
                </el-button>
                <el-button 
                  type="danger" 
                  size="small" 
                  class="quick-action-btn"
                  :icon="Close" 
                  @click="handleCommand({ type: 'rejectRefund', id: row.id })"
                >
                  拒绝
                </el-button>
              </template>

              <!-- 查看详情 -->
              <el-button 
                type="primary" 
                plain 
                size="small" 
                :icon="View" 
                @click="handleViewDetail(row.id)"
              >
                详情
              </el-button>

              <!-- 更多下拉操作 -->
              <el-dropdown trigger="hover" @command="handleCommand">
                <el-button link type="info" size="small" class="more-btn">
                  更多 <el-icon><ArrowDown /></el-icon>
                </el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item :command="{type: 'ship', id: row.id}" v-if="row.status === 1">
                      <el-icon><Van /></el-icon> 确认发货
                    </el-dropdown-item>
                    <el-dropdown-item :command="{type: 'cancel', id: row.id}" v-if="[0,1,2].includes(row.status)">
                      <el-icon><CircleClose /></el-icon> 强制取消订单
                    </el-dropdown-item>
                    <el-dropdown-item :command="{type: 'modify', row: row}" divided>
                      <el-icon><Setting /></el-icon> 强制改写状态
                    </el-dropdown-item>
                    <el-dropdown-item :command="{type: 'delete', id: row.id}" v-if="[3,4,5].includes(row.status)" class="danger-text">
                      <el-icon><Delete /></el-icon> 彻底删除记录
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页栏 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[10, 20, 50]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="totalOrders"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="detailVisible" title="订单履约全景详情" width="750px" class="glass-dialog">
      <div v-loading="detailLoading" v-if="currentOrder" class="dialog-content">
        <el-descriptions title="履约核验单" :column="2" border size="default">
          <el-descriptions-item label="订单编号">
            <span class="sn-text">{{ currentOrder.orderSn }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ currentOrder.createTime }}</el-descriptions-item>
          <el-descriptions-item label="买家 ID">
            <span class="user-pill">UID: {{ currentOrder.userId }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <div class="status-capsule" :class="'status-' + currentOrder.status">
              <span class="status-dot"></span>
              <span class="status-text">{{ getStatusText(currentOrder.status) }}</span>
            </div>
          </el-descriptions-item>
        </el-descriptions>

        <h3 class="section-title">商品履约清单</h3>
        <el-table :data="currentOrder.items" border stripe size="small" class="detail-items-table">
          <el-table-column label="商品" min-width="200">
            <template #default="{ row }">
              <div class="detail-item-cell">
                <el-image :src="row.productPic" class="detail-thumb" fit="cover">
                  <template #error>
                    <div class="thumb-err"><el-icon><Picture /></el-icon></div>
                  </template>
                </el-image>
                <div class="detail-item-info">
                  <div class="name">{{ row.productName }}</div>
                  <div class="spec" v-if="row.spec">{{ row.spec }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="单价" width="110" align="center">
            <template #default="{ row }">¥ {{ row.productPrice }}</template>
          </el-table-column>
          <el-table-column label="数量" width="80" align="center">
            <template #default="{ row }">{{ row.quantity || row.productCount || 1 }}</template>
          </el-table-column>
          <el-table-column label="小计" width="110" align="center">
            <template #default="{ row }">
              <span class="subtotal">¥ {{ (row.productPrice * (row.quantity || row.productCount || 1)).toFixed(2) }}</span>
            </template>
          </el-table-column>
        </el-table>

        <div class="total-bar">
          实付款总额：<span class="total-price">¥ {{ currentOrder.totalAmount }}</span>
        </div>
      </div>
    </el-dialog>

    <!-- 强制修改状态弹窗 -->
    <el-dialog v-model="statusVisible" title="强制改写订单状态 (调度后台)" width="420px" class="glass-dialog">
      <el-form :model="statusForm" label-width="90px">
        <el-form-item label="目标状态">
          <el-select v-model="statusForm.status" style="width: 100%">
            <el-option label="0 - 待支付" :value="0" />
            <el-option label="1 - 待发货" :value="1" />
            <el-option label="2 - 已发货" :value="2" />
            <el-option label="3 - 已完成" :value="3" />
            <el-option label="4 - 已取消" :value="4" />
            <el-option label="5 - 已退款" :value="5" />
            <el-option label="6 - 退款申请中" :value="6" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="statusVisible = false">取消</el-button>
        <el-button type="primary" @click="submitStatusModify">确认改写</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.order-container {
  min-height: 100%;
}

/* 1. 顶部 Bento 履约指标看板 */
.order-pipeline-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.pipeline-card {
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 14px;
  padding: 16px 18px;
  display: flex;
  align-items: center;
  position: relative;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.03);
}

.pipeline-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.06);
  border-color: #cbd5e1;
}

.pipeline-card.active {
  border-color: #3b82f6;
  background: linear-gradient(135deg, #f0f7ff 0%, #ffffff 100%);
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.12);
}

.pipeline-card.highlight-orange.active {
  border-color: #f97316;
  background: linear-gradient(135deg, #fff7ed 0%, #ffffff 100%);
}

.pipeline-card.highlight-red.active {
  border-color: #ef4444;
  background: linear-gradient(135deg, #fef2f2 0%, #ffffff 100%);
}

.p-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  margin-right: 14px;
  flex-shrink: 0;
}

.p-icon.blue { background: #eff6ff; color: #2563eb; }
.p-icon.orange { background: #fff7ed; color: #ea580c; }
.p-icon.sky { background: #f0f9ff; color: #0284c7; }
.p-icon.red { background: #fef2f2; color: #dc2626; }

.p-body {
  flex: 1;
  min-width: 0;
}

.p-title {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
  margin-bottom: 4px;
}

.p-num {
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.1;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.p-num .unit {
  font-size: 12px;
  font-weight: normal;
  color: #94a3b8;
  margin-left: 2px;
}

.p-status-pill {
  position: absolute;
  top: 14px;
  right: 14px;
  font-size: 11px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 20px;
  background: #f1f5f9;
  color: #64748b;
}

.orange-pill {
  background: #ffedd5;
  color: #c2410c;
}

.red-pill {
  background: #fee2e2;
  color: #b91c1c;
  animation: pulse 2s infinite;
}

@keyframes pulse {
  0% { transform: scale(1); }
  50% { transform: scale(1.05); }
  100% { transform: scale(1); }
}

/* 2. 主卡片与操作区 */
.main-order-card {
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

/* 管道式选项卡 */
.pipeline-tab-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid #f1f5f9;
  padding-bottom: 14px;
  margin-bottom: 16px;
  overflow-x: auto;
}

.tab-item {
  padding: 6px 16px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #64748b;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s ease;
  user-select: none;
}

.tab-item:hover {
  color: #2563eb;
  background: #f8fafc;
}

.tab-item.active {
  color: #2563eb;
  background: #eff6ff;
}

.tab-badge {
  font-size: 11px;
  font-weight: 700;
  padding: 1px 6px;
  border-radius: 10px;
  background: #e2e8f0;
  color: #334155;
}

.tab-item.active .tab-badge {
  background: #2563eb;
  color: #ffffff;
}

.tab-badge.red {
  background: #ef4444 !important;
  color: #ffffff !important;
}

/* 过滤工具栏 */
.filter-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 18px;
}

.search-inputs {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.input-sn { width: 230px; }
.input-uid { width: 120px; }
.btn-search { font-weight: 600; border-radius: 8px; }

.export-actions {
  display: flex;
  align-items: center;
}

.btn-export {
  border-radius: 8px;
  font-weight: 500;
}

/* 3. 表格自定义样式 */
:deep(.custom-admin-table th.el-table__cell) {
  background-color: #f8fafc;
  color: #334155;
  font-weight: 600;
  font-size: 13px;
  padding: 12px 0;
}

.order-id-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sn-row {
  display: flex;
  align-items: center;
  gap: 4px;
}

.sn-text {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 4px;
}

.copy-btn {
  color: #94a3b8;
  padding: 2px;
}

.copy-btn:hover {
  color: #2563eb;
}

.order-time {
  font-size: 11px;
  color: #94a3b8;
  display: flex;
  align-items: center;
  gap: 4px;
}

/* 商品明细预览 */
.order-items-preview {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.item-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.order-thumb {
  width: 38px;
  height: 38px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
  flex-shrink: 0;
  background: #f8fafc;
}

.thumb-err {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f1f5f9;
  color: #94a3b8;
  font-size: 16px;
}

.item-meta {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.item-name {
  font-size: 12px;
  font-weight: 600;
  color: #1e293b;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-sub {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 11px;
  color: #64748b;
}

.item-spec {
  background: #f1f5f9;
  padding: 1px 5px;
  border-radius: 3px;
  font-size: 10px;
}

.item-qty {
  font-weight: 600;
}

.item-price {
  color: #0f172a;
  font-weight: 600;
}

.more-items-tip {
  font-size: 11px;
  color: #64748b;
  font-style: italic;
}

.empty-item-tip {
  font-size: 12px;
  color: #94a3b8;
}

.user-pill {
  font-family: ui-monospace, SFMono-Regular, monospace;
  font-size: 11px;
  font-weight: 600;
  color: #0369a1;
  background: #e0f2fe;
  padding: 2px 8px;
  border-radius: 4px;
}

.amount-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 2px;
}

.main-amount {
  font-size: 15px;
  font-weight: 800;
  color: #0f172a;
}

.pay-tag {
  font-size: 10px;
  color: #15803d;
  background: #dcfce7;
  padding: 1px 5px;
  border-radius: 3px;
  font-weight: 600;
}

.unpaid-tag {
  font-size: 10px;
  color: #b45309;
  background: #fef3c7;
  padding: 1px 5px;
  border-radius: 3px;
  font-weight: 600;
}

/* 履约状态胶囊 */
.status-capsule {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 600;
}

.status-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

/* 0: 待支付 */
.status-0 { background: #fef3c7; color: #b45309; }
.status-0 .status-dot { background: #f59e0b; }

/* 1: 待发货 */
.status-1 { background: #ffedd5; color: #c2410c; }
.status-1 .status-dot { background: #ea580c; }

/* 2: 已发货/运输中 */
.status-2 { background: #e0f2fe; color: #0369a1; }
.status-2 .status-dot { background: #0284c7; }

/* 3: 已完成 */
.status-3 { background: #dcfce7; color: #15803d; }
.status-3 .status-dot { background: #16a34a; }

/* 4: 已取消 */
.status-4 { background: #f1f5f9; color: #64748b; }
.status-4 .status-dot { background: #94a3b8; }

/* 5: 已退款 */
.status-5 { background: #f1f5f9; color: #64748b; }
.status-5 .status-dot { background: #64748b; }

/* 6: 退款申请中 */
.status-6 { background: #fee2e2; color: #b91c1c; animation: pulse 2s infinite; }
.status-6 .status-dot { background: #ef4444; }

/* 操作按钮 */
.order-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  flex-wrap: wrap;
}

.quick-action-btn {
  font-weight: 600;
  border-radius: 6px;
}

.more-btn {
  margin-left: 2px;
}

.danger-text {
  color: #ef4444 !important;
}

.pagination-wrapper {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}

/* 弹窗内容 */
.dialog-content {
  padding: 6px 0;
}

.section-title {
  margin: 20px 0 10px;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.detail-items-table {
  border-radius: 8px;
  overflow: hidden;
}

.detail-item-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.detail-thumb {
  width: 40px;
  height: 40px;
  border-radius: 6px;
  border: 1px solid #e2e8f0;
}

.detail-item-info .name {
  font-weight: 600;
  font-size: 13px;
  color: #0f172a;
}

.detail-item-info .spec {
  font-size: 11px;
  color: #64748b;
}

.subtotal {
  font-weight: 700;
  color: #0f172a;
}

.total-bar {
  text-align: right;
  margin-top: 18px;
  font-size: 15px;
  color: #475569;
}

.total-price {
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
}

@media (max-width: 1200px) {
  .order-pipeline-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
