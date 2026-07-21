<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'
// 引入 Download 图标
import { Ticket, View, Search, Refresh, ArrowDown, Download } from '@element-plus/icons-vue'
interface OrderItem {
  id: number;
  orderId: number;
  productId: number;
  productName: string;
  productPrice: number;
  productCount: number;
}

interface Order {
  id: number; 
  orderSn: string; 
  userId: number; 
  totalAmount: number; 
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
  status: '' 
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
    totalOrders.value = res.total || 0;
  } catch (error) {
    ElMessage.error('获取订单列表失败')
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1 
  fetchOrders()
}

const handleReset = () => {
  searchForm.orderSn = ''
  searchForm.userId = ''
  searchForm.status = ''
  currentPage.value = 1
  fetchOrders()
}

const handleSizeChange = (val: number) => {
  pageSize.value = val; fetchOrders() 
}
const handleCurrentChange = (val: number) => {
  currentPage.value = val; fetchOrders() 
}

// ✨ 核心升级 1：状态字典新增了 6: 退款申请中
const getStatusText = (status: number) => {
  const map: Record<number, string> = { 
    0: '待支付', 1: '待发货', 2: '已发货', 3: '已完成', 4: '已取消', 5: '已退款', 6: '退款申请中' 
  }
  return map[status] || '未知状态'
}

// ✨ 核心升级 2：为退款申请中配置醒目的红色警告色
const getStatusTagType = (status: number) => {
  const map: Record<number, string> = { 
    0: 'danger', 1: 'warning', 2: 'primary', 3: 'success', 4: 'info', 5: 'info', 6: 'danger' 
  }
  return map[status] || 'info'
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

// ✨ 核心升级 3：增加对买家退款申请的审批逻辑
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
  // 审批：同意退款 (状态变为 5)
  else if (type === 'approveRefund' && id) {
    ElMessageBox.confirm('确认已通过支付渠道原路退回款项？点击确认将订单标记为“已退款”。', '同意退款', { confirmButtonText: '确认退款', type: 'warning' })
      .then(() => updateOrderStatus(id, 5, '退款处理完成，买家将收到通知！'))
      .catch(() => {})
  } 
  // 审批：拒绝退款 (为了安全，默认退回“已发货”状态，也可按需修改)
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
// ✨ 核心导出逻辑
const handleExport = () => {
  exportLoading.value = true
  
  // 1. 拼接带参数的下载地址
  const baseUrl = '/api/order/export'
  const params = new URLSearchParams()
  if (searchForm.orderSn) params.append('orderSn', searchForm.orderSn)
  if (searchForm.userId) params.append('userId', searchForm.userId)
  if (searchForm.status !== '') params.append('status', searchForm.status.toString())
  
  const downloadUrl = `${baseUrl}?${params.toString()}`

  // 2. 模拟点击下载
  // 注意：这种方式最简单，且能触发浏览器的下载器
  const link = document.createElement('a')
  link.style.display = 'none'
  link.href = downloadUrl
  link.setAttribute('download', `订单报表_${new Date().getTime()}.xlsx`)
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  
  setTimeout(() => {
    exportLoading.value = false
    ElMessage.success('报表生成中，请查看浏览器下载记录')
  }, 1000)
}
onMounted(() => fetchOrders())
</script>

<template>
  <div class="order-container">
    <el-card class="glass-card" shadow="hover">
      <div class="header-title">
        <el-icon><Ticket /></el-icon> 实时订单管理台
      </div>

      <div class="filter-container">
        <el-form :inline="true" :model="searchForm" class="demo-form-inline">
          <el-form-item label="订单编号">
            <el-input v-model="searchForm.orderSn" placeholder="完整或部分单号" clearable />
          </el-form-item>
          <el-form-item label="买家ID">
            <el-input v-model="searchForm.userId" placeholder="精准匹配" clearable />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 130px">
              <el-option label="待支付" :value="0" />
              <el-option label="待发货" :value="1" />
              <el-option label="已发货" :value="2" />
              <el-option label="已完成" :value="3" />
              <el-option label="已取消" :value="4" />
              <el-option label="已退款" :value="5" />
              <el-option label="退款申请中" :value="6" /> </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" :icon="Search" @click="handleSearch">搜索</el-button>
            <el-button :icon="Refresh" @click="handleReset">重置</el-button>
            <el-button 
              type="success" 
              plain 
              :icon="Download" 
              @click="handleExport"
              :loading="exportLoading"
            >
              导出 Excel
            </el-button>
          </el-form-item>
        </el-form>
      </div>

      <el-table :data="orderList" v-loading="loading" stripe class="custom-table">
        <el-table-column prop="orderSn" label="订单编号" min-width="180">
          <template #default="{ row }"><code class="order-sn">{{ row.orderSn }}</code></template>
        </el-table-column>
        <el-table-column prop="userId" label="买家ID" width="90" align="center" />
        <el-table-column prop="totalAmount" label="订单金额" width="120" align="center">
          <template #default="{ row }"><span class="price-text">¥ {{ row.totalAmount }}</span></template>
        </el-table-column>
        <el-table-column prop="status" label="当前状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag 
              :type="getStatusTagType(row.status)" 
              effect="light" 
              round
              :class="{'blink-warning': row.status === 6}"
            >
              {{ getStatusText(row.status) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="170" align="center" />
        
        <el-table-column label="操作" width="160" fixed="right" align="center">
          <template #default="{ row }">
            <el-button type="primary" link :icon="View" @click="handleViewDetail(row.id)">详情</el-button>
            
            <el-dropdown trigger="click" @command="handleCommand" style="margin-left: 12px;">
              <el-button type="primary" link>
                管理<el-icon class="el-icon--right"><arrow-down /></el-icon>
              </el-button>
              <template #dropdown>
                <el-dropdown-item :command="{type: 'approveRefund', id: row.id}" v-if="row.status === 6" class="success-text">✅ 同意退款</el-dropdown-item>
                <el-dropdown-item :command="{type: 'rejectRefund', id: row.id}" v-if="row.status === 6" class="danger-text">❌ 拒绝退款</el-dropdown-item>
                
                <el-dropdown-item :command="{type: 'ship', id: row.id}" v-if="row.status === 1">🚀 确认发货</el-dropdown-item>
                <el-dropdown-item :command="{type: 'cancel', id: row.id}" v-if="[0,1,2].includes(row.status)">⭕ 取消订单</el-dropdown-item>
                <el-dropdown-item :command="{type: 'modify', row: row}" divided>⚙️ 强制改状态</el-dropdown-item>
                <el-dropdown-item :command="{type: 'delete', id: row.id}" v-if="[3,4,5].includes(row.status)" class="danger-text">🗑️ 彻底删除</el-dropdown-item>
              </template>
            </el-dropdown>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-sizes="[5, 10, 20, 50]"
          background
          layout="total, sizes, prev, pager, next, jumper"
          :total="totalOrders"
          @size-change="handleSizeChange"
          @current-change="handleCurrentChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="订单详情" width="700px" custom-class="glass-dialog">
      <div v-loading="detailLoading" v-if="currentOrder">
        <el-descriptions title="基础数据" :column="2" border>
          <el-descriptions-item label="订单编号">{{ currentOrder.orderSn }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ currentOrder.createTime }}</el-descriptions-item>
          <el-descriptions-item label="买家 ID">{{ currentOrder.userId }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="getStatusTagType(currentOrder.status)" size="small" :class="{'blink-warning': currentOrder.status === 6}">
              {{ getStatusText(currentOrder.status) }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
        <h3 class="section-title">商品清单</h3>
        <el-table :data="currentOrder.items" border stripe>
          <el-table-column prop="productName" label="商品名称" />
          <el-table-column label="单价" width="120" align="center">
            <template #default="{ row }">¥ {{ row.productPrice }}</template>
          </el-table-column>
          <el-table-column prop="productCount" label="购买数量" width="100" align="center" />
          <el-table-column label="小计" width="120" align="center">
            <template #default="{ row }"><span class="subtotal">¥ {{ row.productPrice * row.productCount }}</span></template>
          </el-table-column>
        </el-table>
        <div class="total-bar">实付款金额：<span class="total-price">¥ {{ currentOrder.totalAmount }}</span></div>
      </div>
    </el-dialog>

    <el-dialog v-model="statusVisible" title="强制修改订单状态" width="400px" custom-class="glass-dialog">
      <el-form :model="statusForm" label-width="100px">
        <el-form-item label="目标状态：">
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
        <span class="dialog-footer">
          <el-button @click="statusVisible = false">取消</el-button>
          <el-button type="primary" @click="submitStatusModify">确认修改</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.order-container { height: 100%; }

.glass-card {
  border-radius: 12px;
  border: 1px solid rgba(186, 230, 253, 0.5);
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(10px);
  padding-bottom: 20px;
}

.header-title { font-size: 18px; font-weight: bold; color: #0369a1; margin-bottom: 20px; display: flex; align-items: center; gap: 8px; }
.filter-container { background: rgba(240, 249, 255, 0.5); padding: 18px 18px 0 18px; border-radius: 8px; margin-bottom: 20px; border: 1px dashed #bae6fd; }
.order-sn { background: #f0f9ff; color: #0c4a6e; padding: 2px 6px; border-radius: 4px; font-family: monospace; }
.price-text { color: #f43f5e; font-weight: 800; }
:deep(.el-table th.el-table__cell) { background-color: #f0f9ff; color: #0369a1; }
.pagination-wrapper { margin-top: 25px; display: flex; justify-content: flex-end; padding-right: 20px; }

/* 详情弹窗样式 */
.section-title { margin: 20px 0 10px; color: #0369a1; font-size: 16px; border-left: 4px solid #0369a1; padding-left: 10px; }
.subtotal { font-weight: bold; color: #f43f5e; }
.total-bar { text-align: right; margin-top: 20px; font-size: 16px; padding-top: 15px; border-top: 1px dashed #e2e8f0; }
.total-price { color: #f43f5e; font-size: 20px; font-weight: bold; }

/* 下拉菜单强调色 */
.danger-text { color: #f56c6c !important; }
.success-text { color: #67c23a !important; font-weight: bold; }

/* 呼吸闪烁动画：吸引管理员注意未处理的退款申请 */
@keyframes blink {
  0% { opacity: 1; }
  50% { opacity: 0.5; box-shadow: 0 0 8px #f56c6c; }
  100% { opacity: 1; }
}
.blink-warning {
  animation: blink 2s infinite;
  font-weight: bold;
}
</style>
