<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { 
  Search, Plus, Picture, Operation, Clock, 
  Download, Upload, Goods, Discount, Warning, 
  Coin, Refresh, Edit, Delete
} from '@element-plus/icons-vue'
import request from '../../utils/request'
import { ElMessage, ElMessageBox } from 'element-plus'

interface Category { id: number; name: string }

interface Product {
  id?: number; name: string; categoryId?: number; price: number;
  stock: number; picUrl?: string; description: string;
  status: number; sales?: number; createTime?: string;
  promoPrice?: number; promoStartTime?: string; promoEndTime?: string; 
}

interface Sku { id?: number; productId: number; specName: string; price: number; stock: number; picUrl?: string; }

const productList = ref<Product[]>([])
const categoryList = ref<Category[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const isEdit = ref(false)

const searchQuery = ref('')
const searchCategory = ref<number | ''>('')
const statusFilter = ref<'all' | 'active' | 'inactive' | 'promo' | 'lowStock'>('all')

const productStats = computed(() => {
  const total = productList.value.length
  const activeCount = productList.value.filter(p => p.status === 1).length
  const promoCount = productList.value.filter(p => !!p.promoStartTime).length
  const lowStockCount = productList.value.filter(p => (p.stock || 0) <= 10).length
  const totalStock = productList.value.reduce((acc, p) => acc + (p.stock || 0), 0)
  return {
    total,
    activeCount,
    promoCount,
    lowStockCount,
    totalStock
  }
})

const getStockLevelClass = (stock: number) => {
  if (stock <= 0) return 'soldout'
  if (stock <= 10) return 'critical'
  if (stock <= 50) return 'moderate'
  return 'healthy'
}

const getStockLevelText = (stock: number) => {
  if (stock <= 0) return '售罄'
  if (stock <= 10) return '告急'
  if (stock <= 50) return '正常'
  return '充足'
}

const productForm = reactive<Product>({
  name: '', categoryId: undefined, price: 0, stock: 0, picUrl: '', description: '', status: 1
})

const fetchProducts = async () => {
  loading.value = true
  try {
    const res: any = await request.get<any, Product[]>('/product/list')
    const list = Array.isArray(res) ? res : (res?.records || res?.data || [])
    productList.value = list.map((p: any) => ({
      ...p,
      status: p.status !== undefined && p.status !== null ? Number(p.status) : 1
    }))
  } catch (error) {
    productList.value = []
  } finally {
    loading.value = false
  }
}

const fetchCategories = async () => {
  try { categoryList.value = await request.get<any, Category[]>('/category/list') } 
  catch (error) { categoryList.value = [{ id: 1, name: '数码3C' }, { id: 2, name: '服装服饰' }, { id: 3, name: '家居日用' }] }
}

const filteredProducts = computed(() => {
  return productList.value.filter(p => {
    const matchName = p.name.toLowerCase().includes(searchQuery.value.toLowerCase())
    const matchCat = searchCategory.value === '' || p.categoryId === searchCategory.value
    let matchStatus = true
    if (statusFilter.value === 'active') matchStatus = p.status === 1
    else if (statusFilter.value === 'inactive') matchStatus = p.status === 0
    else if (statusFilter.value === 'promo') matchStatus = !!p.promoStartTime
    else if (statusFilter.value === 'lowStock') matchStatus = (p.stock || 0) <= 10
    return matchName && matchCat && matchStatus
  })
})

const handleAdd = () => {
  isEdit.value = false
  Object.assign(productForm, { id: undefined, name: '', categoryId: undefined, price: 0, stock: 0, picUrl: '', description: '', status: 1 })
  dialogVisible.value = true
}

const handleEdit = (row: Product) => {
  isEdit.value = true
  Object.assign(productForm, row)
  dialogVisible.value = true
}

const handleDelete = (id: number) => {
  ElMessageBox.confirm('确定要彻底删除该商品吗？此操作不可逆！', '高危操作', { type: 'error' }).then(async () => {
    await request.delete(`/product/delete/${id}`)
    ElMessage.success('商品已彻底删除！')
    fetchProducts()
  }).catch(() => {})
}

const handleStatusChange = async (row: Product) => {
  try {
    await request.put('/product/update', { id: row.id, status: row.status })
    ElMessage.success(row.status === 1 ? '商品已上架' : '商品已下架')
  } catch (error) {
    row.status = row.status === 1 ? 0 : 1
    ElMessage.error('状态切换失败')
  }
}

const handleSubmit = async () => {
  if (!productForm.name || !productForm.categoryId) return ElMessage.warning('必填项不能为空')
  submitLoading.value = true
  try {
    if (isEdit.value) await request.put('/product/update', productForm)
    else await request.post('/product/add', productForm)
    ElMessage.success('操作成功！')
    dialogVisible.value = false
    fetchProducts()
  } finally { submitLoading.value = false }
}

const getCategoryName = (cId: number) => {
  const cat = categoryList.value.find(c => c.id === cId)
  return cat ? cat.name : '未分类'
}

// ================= ✨ 数据导入导出模块 =================
const handleExport = async () => {
  try {
    ElMessage.info('正在生成商品报表，请稍候...')
    const res: any = await request.get('/product/export', { responseType: 'blob' })
    const url = window.URL.createObjectURL(new Blob([res]))
    const link = document.createElement('a')
    link.href = url
    link.setAttribute('download', `E-MALL商品数据_${new Date().getTime()}.xlsx`)
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    ElMessage.success('导出成功！')
  } catch (error) {
    ElMessage.error('导出失败，请检查网络或数据异常')
  }
}

const handleImport = async (options: any) => {
  const formData = new FormData()
  formData.append('file', options.file)
  try {
    const res: any = await request.post('/product/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    ElMessage.success(res || '批量导入成功！')
    fetchProducts() 
  } catch (error) {
    ElMessage.error('导入失败，请检查 Excel 格式是否正确')
  }
}

// ================= ✨ 强化版：规格管理模块 =================
const skuDrawerVisible = ref(false)
const activeProduct = ref<Product | null>(null)
const skuList = ref<Sku[]>([])
const skuLoading = ref(false)
const skuDialogVisible = ref(false)
const isSkuEdit = ref(false)
const skuForm = reactive<Sku>({ productId: 0, specName: '', price: 0, stock: 0, picUrl: '' })

const openSkuDrawer = async (row: Product) => { activeProduct.value = row; skuDrawerVisible.value = true; fetchSkus(row.id!) }
const fetchSkus = async (pId: number) => { skuLoading.value = true; try { skuList.value = await request.get<any, Sku[]>(`/sku/list/${pId}`) } finally { skuLoading.value = false } }
const handleAddSku = () => { isSkuEdit.value = false; Object.assign(skuForm, { id: undefined, productId: activeProduct.value?.id, specName: '', price: activeProduct.value?.price || 0, stock: 0, picUrl: activeProduct.value?.picUrl || '' }); skuDialogVisible.value = true }
const handleEditSku = (row: Sku) => { isSkuEdit.value = true; Object.assign(skuForm, row); skuDialogVisible.value = true }

// 💥 强化：保存规格后触发全量同步
const submitSkuForm = async () => { 
  if (!skuForm.specName) return ElMessage.warning('请填写规格名称')
  if (!skuForm.picUrl && activeProduct.value?.picUrl) skuForm.picUrl = activeProduct.value.picUrl
  try { 
    if (isSkuEdit.value) await request.put('/sku/update', skuForm)
    else await request.post('/sku/add', skuForm)
    
    await request.get('/product/sync-inventory') // 强制重算总库存并清理缓存
    
    ElMessage.success('保存成功！外层总库存已自动对齐')
    skuDialogVisible.value = false
    fetchSkus(activeProduct.value!.id!)
    fetchProducts() // 刷新外层列表，保证数字绝对一致
  } catch (e) {} 
}

// ================= 秒杀设置模块 =================
const promoDialogVisible = ref(false)
const promoSkus = ref<any[]>([])
const selectedProductStock = ref(0)
const promoForm = reactive({
  id: 0,
  promoSkuId: 0,
  promoPrice: 0,
  promoStock: 10,
  timeRange: [] as string[]
})

const openPromoDialog = async (row: Product) => {
  promoForm.id = row.id!
  promoForm.promoPrice = row.promoPrice || row.price
  promoForm.promoStock = (row as any).promoStock || 10
  promoForm.promoSkuId = (row as any).promoSkuId || 0
  promoForm.timeRange = (row.promoStartTime && row.promoEndTime) ? [row.promoStartTime, row.promoEndTime] : []
  selectedProductStock.value = row.stock || 0

  promoSkus.value = []
  try {
    const res: any = await request.get('/product/skus/' + row.id)
    promoSkus.value = Array.isArray(res) ? res : (res?.data || [])
  } catch (e) {}

  promoDialogVisible.value = true
}

const currentAvailableStock = computed(() => {
  if (promoForm.promoSkuId && promoForm.promoSkuId > 0) {
    const sku = promoSkus.value.find(s => s.id === promoForm.promoSkuId)
    return sku ? (sku.stock || 0) : selectedProductStock.value
  }
  return selectedProductStock.value
})

const submitPromoForm = async () => {
  if (!promoForm.promoPrice || promoForm.promoPrice <= 0) {
    return ElMessage.warning('请输入有效的秒杀特惠价格')
  }
  if (!promoForm.promoStock || promoForm.promoStock <= 0) {
    return ElMessage.warning('请设置秒杀限量库存（必须大于 0）')
  }
  if (promoForm.promoStock > currentAvailableStock.value) {
    return ElMessage.error(`⚠️ 秒杀限量库存 (${promoForm.promoStock}件) 不能超过当前所选规格的可用库存 (${currentAvailableStock.value}件)！`)
  }
  if (!promoForm.timeRange || promoForm.timeRange.length !== 2) {
    return ElMessage.warning('请选择完整的秒杀起止时间')
  }
  try {
    submitLoading.value = true
    await request.put('/product/update', {
      id: promoForm.id,
      promoPrice: promoForm.promoPrice,
      promoStock: promoForm.promoStock,
      promoSkuId: promoForm.promoSkuId > 0 ? promoForm.promoSkuId : null,
      promoStartTime: promoForm.timeRange[0],
      promoEndTime: promoForm.timeRange[1]
    })
    ElMessage.success('秒杀活动与限量库存设置成功！')
    promoDialogVisible.value = false
    await fetchProducts()
  } finally { submitLoading.value = false }
}

const clearPromo = async () => {
  try {
    submitLoading.value = true
    await request.put(`/product/cancelPromo/${promoForm.id}`)
    ElMessage.success('已成功取消该商品的秒杀活动！')
    promoDialogVisible.value = false
    await fetchProducts()
  } catch (e: any) {
    ElMessage.error(e?.message || '取消秒杀失败，请重试')
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => { fetchCategories(); fetchProducts() })
</script>

<template>
  <div class="product-container">
    <!-- 1. 顶部商品资产小看板 (Mini Bento Metrics) -->
    <div class="product-stats-grid">
      <div 
        class="stat-bento-card" 
        :class="{ active: statusFilter === 'all' }"
        @click="statusFilter = 'all'"
        title="点击查看全部商品"
      >
        <div class="stat-icon-wrap blue">
          <el-icon><Goods /></el-icon>
        </div>
        <div class="stat-meta">
          <div class="stat-label">全店商品储备</div>
          <div class="stat-val">{{ productStats.total }} <span class="unit">款</span></div>
        </div>
        <span class="stat-tag">全景</span>
      </div>

      <div 
        class="stat-bento-card" 
        :class="{ active: statusFilter === 'active' }"
        @click="statusFilter = statusFilter === 'active' ? 'all' : 'active'"
        title="点击仅看在架销售商品"
      >
        <div class="stat-icon-wrap green">
          <el-icon><Coin /></el-icon>
        </div>
        <div class="stat-meta">
          <div class="stat-label">正常在架流通</div>
          <div class="stat-val">{{ productStats.activeCount }} <span class="unit">款</span></div>
        </div>
        <span class="stat-tag tag-green">在架率 {{ productStats.total ? Math.round((productStats.activeCount / productStats.total) * 100) : 0 }}%</span>
      </div>

      <div 
        class="stat-bento-card" 
        :class="{ active: statusFilter === 'promo' }"
        @click="statusFilter = statusFilter === 'promo' ? 'all' : 'promo'"
        title="点击仅看秒杀促销商品"
      >
        <div class="stat-icon-wrap orange">
          <el-icon><Discount /></el-icon>
        </div>
        <div class="stat-meta">
          <div class="stat-label">秒杀特惠商品</div>
          <div class="stat-val">{{ productStats.promoCount }} <span class="unit">款</span></div>
        </div>
        <span class="stat-tag tag-orange">官方直补</span>
      </div>

      <div 
        class="stat-bento-card" 
        :class="{ active: statusFilter === 'lowStock' }"
        @click="statusFilter = statusFilter === 'lowStock' ? 'all' : 'lowStock'"
        title="点击仅看库存告急商品"
      >
        <div class="stat-icon-wrap red">
          <el-icon><Warning /></el-icon>
        </div>
        <div class="stat-meta">
          <div class="stat-label">库存紧张预警</div>
          <div class="stat-val" :class="{ 'text-danger': productStats.lowStockCount > 0 }">
            {{ productStats.lowStockCount }} <span class="unit">款</span>
          </div>
        </div>
        <span class="stat-tag tag-red" v-if="productStats.lowStockCount > 0">需补货</span>
        <span class="stat-tag tag-green" v-else>储备充沛</span>
      </div>
    </div>

    <!-- 2. 主商品管理表格卡片 -->
    <el-card class="glass-card main-table-card" shadow="never">
      <div class="toolbar-header">
        <div class="filter-controls">
          <el-input 
            v-model="searchQuery" 
            placeholder="搜索商品名称、型号..." 
            class="search-input" 
            clearable 
            :prefix-icon="Search" 
          />
          <el-select v-model="searchCategory" placeholder="全部分类" clearable class="filter-select">
            <el-option v-for="cat in categoryList" :key="cat.id" :label="cat.name" :value="cat.id" />
          </el-select>
          <el-button 
            v-if="searchQuery || searchCategory !== '' || statusFilter !== 'all'"
            link 
            type="primary"
            :icon="Refresh"
            @click="searchQuery = ''; searchCategory = ''; statusFilter = 'all'"
          >
            重置筛选
          </el-button>
        </div>
        
        <div class="action-controls">
          <el-button type="success" plain :icon="Download" @click="handleExport" class="action-btn">
            导出 Excel
          </el-button>
          
          <el-upload
            action="#"
            :show-file-list="false"
            :http-request="handleImport"
            accept=".xlsx, .xls"
          >
            <el-button type="warning" plain :icon="Upload" class="action-btn">批量导入</el-button>
          </el-upload>

          <el-button type="primary" :icon="Plus" class="btn-create-product" @click="handleAdd">
            发布新商品
          </el-button>
        </div>
      </div>

      <el-table :data="filteredProducts" style="width: 100%" v-loading="loading" class="custom-admin-table">
        <el-table-column prop="id" label="ID" width="75" align="center">
          <template #default="{ row }">
            <span class="row-id">#{{ row.id }}</span>
          </template>
        </el-table-column>

        <el-table-column label="商品信息" min-width="280">
          <template #default="{ row }">
            <div class="product-cell">
              <el-image 
                :src="row.picUrl" 
                class="p-img" 
                fit="cover" 
                :preview-src-list="row.picUrl ? [row.picUrl] : []"
                preview-teleported
              >
                <template #error>
                  <div class="img-error"><el-icon><Picture /></el-icon></div>
                </template>
              </el-image>
              <div class="p-info">
                <div class="p-name" :title="row.name">{{ row.name }}</div>
                <div class="p-tags-row">
                  <span class="cat-pill">{{ getCategoryName(row.categoryId) }}</span>
                  <span v-if="row.promoStartTime" class="promo-pill">
                    <el-icon><Clock /></el-icon> 秒杀进行中
                  </span>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="单价体系" width="140" align="center">
          <template #default="{ row }">
            <div class="price-cell">
              <span class="base-price">¥ {{ Number(row.price).toFixed(2) }}</span>
              <span v-if="row.promoPrice" class="promo-price-tag">
                秒杀 ¥{{ Number(row.promoPrice).toFixed(2) }}
              </span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="库存健康度" width="140" align="center">
          <template #default="{ row }">
            <div class="stock-pill" :class="getStockLevelClass(row.stock)">
              <span class="stock-dot"></span>
              <span class="stock-num">{{ row.stock }} 件</span>
              <span class="stock-desc">{{ getStockLevelText(row.stock) }}</span>
            </div>
          </template>
        </el-table-column>

        <el-table-column label="销售状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch 
              v-model="row.status" 
              :active-value="1" 
              :inactive-value="0" 
              active-color="#10b981" 
              @change="handleStatusChange(row)" 
            />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="260" fixed="right" align="center">
          <template #default="{ row }"> 
            <div class="table-actions">
              <el-button type="primary" plain size="small" class="action-pill-btn" :icon="Operation" @click="openSkuDrawer(row)">
                规格/补货
              </el-button>
              <el-button type="danger" plain size="small" class="action-pill-btn" :icon="Clock" @click="openPromoDialog(row)">
                秒杀
              </el-button>
              <el-dropdown trigger="hover">
                <el-button link type="info" size="small">更多</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="handleEdit(row)">
                      <el-icon><Edit /></el-icon> 编辑基本信息
                    </el-dropdown-item>
                    <el-dropdown-item @click="handleDelete(row.id)" class="danger-item">
                      <el-icon><Delete /></el-icon> 彻底删除商品
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="promoDialogVisible" title="设置限时秒杀活动与限量库存" width="520px" class="glass-dialog">
      <el-form label-width="100px" class="rich-form">
        <el-form-item label="参与规格">
          <el-select v-model="promoForm.promoSkuId" placeholder="选择指定规格（留空为全规格适用）" style="width: 100%;">
            <el-option :value="0" :label="`【全规格适用】(总库存: ${selectedProductStock} 件)`" />
            <el-option v-for="sku in promoSkus" :key="sku.id" :label="`${sku.specName} (库存: ${sku.stock}件 / 现价: ¥${sku.price})`" :value="sku.id" />
          </el-select>
          <div style="font-size: 12px; color: #0284c7; line-height: 1.4; margin-top: 4px;">
            当前所选规格可用库存上限：<strong>{{ currentAvailableStock }}</strong> 件
          </div>
        </el-form-item>

        <el-form-item label="秒杀特价" required>
          <el-input-number v-model="promoForm.promoPrice" :min="0.01" :precision="2" style="width: 100%;" />
          <div style="font-size: 12px; color: #94a3b8; line-height: 1.2; margin-top: 4px;">
            活动期间，买家享受此直降特惠价格。
          </div>
        </el-form-item>

        <el-form-item label="秒杀限量" required>
          <el-input-number v-model="promoForm.promoStock" :min="1" :max="currentAvailableStock" style="width: 100%;" />
          <div style="font-size: 12px; color: #e11d48; line-height: 1.2; margin-top: 4px;">
            秒杀限量库存必须 ≤ 当前规格可用库存 (当前最大: {{ currentAvailableStock }} 件)。
          </div>
        </el-form-item>

        <el-form-item label="起止时间" required>
          <el-date-picker
            v-model="promoForm.timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%;"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="clearPromo" type="danger" plain round style="float: left;" :loading="submitLoading">
          彻底取消此商品秒杀
        </el-button>
        <el-button @click="promoDialogVisible = false" round>关闭窗口</el-button>
        <el-button type="primary" @click="submitPromoForm" :loading="submitLoading" round>保存秒杀设置</el-button>
      </template>
    </el-dialog>

    <el-drawer v-model="skuDrawerVisible" :title="`[ ${activeProduct?.name} ] 规格配置管理`" size="55%" class="cute-drawer">
      <div class="drawer-toolbar"><el-button type="primary" :icon="Plus" round @click="handleAddSku">添加新规格</el-button></div>
      <el-table :data="skuList" v-loading="skuLoading" stripe border size="small" class="sku-table">
        <el-table-column prop="specName" label="规格名称" min-width="160" />
        <el-table-column prop="price" label="价格" width="110" align="center" />
        <el-table-column prop="stock" label="库存" width="100" align="center" />
        <el-table-column label="操作" width="140" align="center"><template #default="{ row }"><el-button type="primary" link size="small" @click="handleEditSku(row)">改价/补货</el-button></template></el-table-column>
      </el-table>
    </el-drawer>
    
    <el-dialog v-model="skuDialogVisible" title="管理规格" width="450px" class="glass-dialog">
      <el-form :model="skuForm" label-width="90px">
        <el-form-item label="规格名称"><el-input v-model="skuForm.specName" /></el-form-item>
        <el-form-item label="专属单价"><el-input-number v-model="skuForm.price" style="width: 100%;" /></el-form-item>
        <el-form-item label="补货库存"><el-input-number v-model="skuForm.stock" style="width: 100%;" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="submitSkuForm" type="primary">保存</el-button></template>
    </el-dialog>

    <el-dialog v-model="dialogVisible" title="商品信息" width="550px" class="glass-dialog">
      <el-form :model="productForm" label-width="90px">
        <el-form-item label="名称"><el-input v-model="productForm.name"/></el-form-item>
        <el-form-item label="单价"><el-input-number v-model="productForm.price"/></el-form-item>
        <el-form-item label="总库存">
          <el-input-number v-model="productForm.stock" disabled />
          <div style="font-size: 12px; color: #94a3b8; line-height: 1.2; margin-top: 5px;">
            系统已开启库存强一致性联动。总库存由 [规格/补货] 自动求和计算，禁止手动修改。
          </div>
        </el-form-item>
      </el-form>
      <template #footer><el-button @click="handleSubmit" type="primary">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<style scoped>
.product-container {
  min-height: 100%;
}

/* 1. Mini Bento Metrics Grid */
.product-stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
  margin-bottom: 20px;
}

.stat-bento-card {
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

.stat-bento-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.06);
  border-color: #cbd5e1;
}

.stat-bento-card.active {
  border-color: #3b82f6;
  background: linear-gradient(135deg, #f0f7ff 0%, #ffffff 100%);
  box-shadow: 0 4px 16px rgba(59, 130, 246, 0.12);
}

.stat-icon-wrap {
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

.stat-icon-wrap.blue { background: #eff6ff; color: #2563eb; }
.stat-icon-wrap.green { background: #ecfdf5; color: #059669; }
.stat-icon-wrap.orange { background: #fff7ed; color: #ea580c; }
.stat-icon-wrap.red { background: #fef2f2; color: #dc2626; }

.stat-meta {
  flex: 1;
  min-width: 0;
}

.stat-label {
  font-size: 12px;
  color: #64748b;
  font-weight: 500;
  margin-bottom: 4px;
}

.stat-val {
  font-size: 22px;
  font-weight: 800;
  color: #0f172a;
  line-height: 1.1;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.stat-val .unit {
  font-size: 12px;
  font-weight: normal;
  color: #94a3b8;
  margin-left: 2px;
}

.stat-val.text-danger {
  color: #ef4444;
}

.stat-tag {
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

.stat-tag.tag-green { background: #dcfce7; color: #15803d; }
.stat-tag.tag-orange { background: #ffedd5; color: #c2410c; }
.stat-tag.tag-red { background: #fee2e2; color: #b91c1c; }

/* 2. 主卡片与操作工具栏 */
.main-table-card {
  border-radius: 14px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.toolbar-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 14px;
  margin-bottom: 18px;
}

.filter-controls {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.search-input {
  width: 260px;
}

.filter-select {
  width: 150px;
}

.action-controls {
  display: flex;
  align-items: center;
  gap: 10px;
}

.action-btn {
  border-radius: 8px;
  font-weight: 500;
}

.btn-create-product {
  background: linear-gradient(135deg, #2563eb 0%, #1d4ed8 100%);
  border: none;
  font-weight: 600;
  border-radius: 8px;
  padding: 8px 18px;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
  transition: all 0.2s ease;
}

.btn-create-product:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.35);
}

/* 3. 表格内细节元素 */
:deep(.custom-admin-table th.el-table__cell) {
  background-color: #f8fafc;
  color: #334155;
  font-weight: 600;
  font-size: 13px;
  padding: 12px 0;
}

:deep(.custom-admin-table .el-table__row) {
  transition: background-color 0.15s ease;
}

:deep(.custom-admin-table .el-table__row:hover) {
  background-color: #f8fafc;
}

.row-id {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  background: #f1f5f9;
  padding: 2px 6px;
  border-radius: 4px;
}

.product-cell {
  display: flex;
  align-items: center;
  gap: 14px;
}

.p-img {
  width: 54px;
  height: 54px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  flex-shrink: 0;
  background: #f8fafc;
  cursor: pointer;
  transition: transform 0.2s ease;
}

.p-img:hover {
  transform: scale(1.05);
}

.img-error {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f1f5f9;
  color: #94a3b8;
  font-size: 20px;
}

.p-info {
  display: flex;
  flex-direction: column;
  gap: 5px;
  min-width: 0;
}

.p-name {
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.p-tags-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.cat-pill {
  font-size: 11px;
  font-weight: 500;
  color: #475569;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 4px;
}

.promo-pill {
  font-size: 11px;
  font-weight: 600;
  color: #e11d48;
  background: #fff1f2;
  border: 1px solid #fecdd3;
  padding: 1px 7px;
  border-radius: 4px;
  display: inline-flex;
  align-items: center;
  gap: 3px;
}

.price-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
}

.base-price {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.promo-price-tag {
  font-size: 11px;
  font-weight: 600;
  color: #dc2626;
  background: #fee2e2;
  padding: 1px 6px;
  border-radius: 4px;
}

/* 库存健康度胶囊 */
.stock-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 3px 10px;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 600;
}

.stock-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  flex-shrink: 0;
}

.stock-desc {
  font-size: 10px;
  padding: 1px 5px;
  border-radius: 3px;
  font-weight: 600;
}

.stock-pill.healthy {
  background: #f0fdf4;
  color: #166534;
}
.stock-pill.healthy .stock-dot { background: #22c55e; }
.stock-pill.healthy .stock-desc { background: #dcfce7; color: #15803d; }

.stock-pill.moderate {
  background: #f8fafc;
  color: #334155;
  border: 1px solid #e2e8f0;
}
.stock-pill.moderate .stock-dot { background: #64748b; }
.stock-pill.moderate .stock-desc { background: #e2e8f0; color: #475569; }

.stock-pill.critical {
  background: #fff7ed;
  color: #c2410c;
  border: 1px solid #fed7aa;
}
.stock-pill.critical .stock-dot { background: #f97316; }
.stock-pill.critical .stock-desc { background: #ffedd5; color: #9a3412; }

.stock-pill.soldout {
  background: #fef2f2;
  color: #991b1b;
  border: 1px solid #fecaca;
}
.stock-pill.soldout .stock-dot { background: #ef4444; }
.stock-pill.soldout .stock-desc { background: #fee2e2; color: #7f1d1d; }

.table-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.action-pill-btn {
  border-radius: 6px;
  font-weight: 500;
}

:deep(.danger-item) {
  color: #ef4444 !important;
}

/* 4. 抽屉与弹窗增强 */
.drawer-toolbar {
  margin-bottom: 16px;
}

:deep(.glass-dialog) {
  border-radius: 16px;
  background: #ffffff;
}

:deep(.glass-dialog .el-dialog__header) {
  border-bottom: 1px solid #f1f5f9;
  padding: 18px 24px;
  font-weight: 700;
  color: #0f172a;
}

:deep(.glass-dialog .el-dialog__body) {
  padding: 20px 24px;
}

:deep(.glass-dialog .el-dialog__footer) {
  border-top: 1px solid #f1f5f9;
  padding: 14px 24px;
}

@media (max-width: 1200px) {
  .product-stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }
}
</style>
