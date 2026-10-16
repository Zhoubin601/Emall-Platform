<script setup lang="ts">
import { ref, onMounted, onUnmounted, markRaw } from 'vue'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts/core'
import { BarChart, LineChart, PieChart } from 'echarts/charts'
import {
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent
} from 'echarts/components'
import { CanvasRenderer } from 'echarts/renderers'
import type { EChartsType } from 'echarts/core'
import {
  User,
  ShoppingCart,
  Money,
  TrendCharts,
  Refresh,
  TopRight,
  ArrowRight,
  Goods,
  List,
  DataLine,
  Picture
} from '@element-plus/icons-vue'
import request from '../../utils/request'
import { normalizeDashboardData, type DashboardData } from './dashboardData'

echarts.use([
  BarChart,
  LineChart,
  PieChart,
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent,
  CanvasRenderer
])

const router = useRouter()

// 核心数据指标
const coreMetrics = ref({
  totalUsers: 0,
  totalOrders: 0,
  totalSales: 0,
  todaySales: 0
})

// 图表 DOM 引用
const trendChartRef = ref<HTMLElement | null>(null)
const statusChartRef = ref<HTMLElement | null>(null)
const rankChartRef = ref<HTMLElement | null>(null)
const charts: EChartsType[] = []

const loading = ref(false)
const lastSyncTime = ref('')
const selectedPeriod = ref<'7d' | '30d' | 'quarter'>('7d')

// 安全获取或初始化 ECharts 实例，防止重复绑定与内存泄漏
const getOrCreateChart = (dom: HTMLElement): EChartsType => {
  const existing = echarts.getInstanceByDom(dom)
  if (existing) return existing
  const chart = markRaw(echarts.init(dom))
  charts.push(chart)
  return chart
}

// 拉取看板数据
const fetchDashboardData = async () => {
  loading.value = true
  try {
    const res = normalizeDashboardData(await request.get('/dashboard/data'))
    
    coreMetrics.value = {
      totalUsers: res.metrics.totalUsers,
      totalOrders: res.metrics.totalOrders,
      totalSales: res.metrics.totalSales,
      todaySales: res.metrics.todaySales
    }

    initCharts(res)
    const now = new Date()
    lastSyncTime.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', second: '2-digit' })
  } catch (error) {
    console.error('获取看板数据失败', error)
  } finally {
    loading.value = false
  }
}

// 接收数据并绘制 ECharts (现代 Bento 配色体系)
const initCharts = (data: DashboardData) => {
  // 1. 近7天销售趋势折线面积图
  if (trendChartRef.value) {
    const trendChart = getOrCreateChart(trendChartRef.value)
    trendChart.setOption({
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'axis',
        backgroundColor: 'rgba(15, 23, 42, 0.9)',
        borderColor: 'rgba(255, 255, 255, 0.1)',
        padding: [10, 14],
        textStyle: { color: '#ffffff' },
        formatter: (params: any) => {
          const item = Array.isArray(params) ? params[0] : params
          return `<div style="font-size:12px;color:#94a3b8;margin-bottom:4px;">${item.axisValue} 销售额</div>
                  <div style="font-size:16px;font-weight:700;color:#38bdf8;">¥ ${Number(item.value).toLocaleString('zh-CN', { minimumFractionDigits: 2 })}</div>`
        }
      },
      grid: { left: '2%', right: '3%', bottom: '4%', top: '8%', containLabel: true },
      xAxis: {
        type: 'category',
        boundaryGap: false,
        data: data.trend.dates,
        axisLine: { lineStyle: { color: '#e2e8f0' } },
        axisLabel: { color: '#64748b', fontSize: 12, margin: 12 },
        axisTick: { show: false }
      },
      yAxis: {
        type: 'value',
        axisLine: { show: false },
        axisTick: { show: false },
        splitLine: { lineStyle: { color: '#f1f5f9', type: 'dashed' } },
        axisLabel: {
          color: '#64748b',
          fontSize: 12,
          formatter: (val: number) => `¥${(val / 1000).toFixed(0)}k`
        }
      },
      series: [
        {
          name: '销售额',
          type: 'line',
          smooth: true,
          showSymbol: false,
          symbolSize: 8,
          lineStyle: { width: 3.5, color: '#2563eb' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(37, 99, 235, 0.28)' },
              { offset: 1, color: 'rgba(37, 99, 235, 0.01)' }
            ])
          },
          itemStyle: { color: '#2563eb', borderColor: '#ffffff', borderWidth: 2 },
          data: data.trend.sales
        }
      ]
    })
  }

  // 2. 订单状态分布环形图 (Donut)
  if (statusChartRef.value) {
    const statusChart = getOrCreateChart(statusChartRef.value)
    statusChart.setOption({
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'item',
        backgroundColor: 'rgba(15, 23, 42, 0.9)',
        borderColor: 'rgba(255, 255, 255, 0.1)',
        padding: [10, 14],
        textStyle: { color: '#ffffff' },
        formatter: (params: any) => {
          return `<div style="font-size:12px;color:#94a3b8;margin-bottom:4px;">${params.seriesName}</div>
                  <div style="font-size:14px;font-weight:700;color:#f8fafc;">${params.name}: <span style="color:#38bdf8;">${params.value} 笔</span> (${params.percent}%)</div>`
        }
      },
      legend: {
        bottom: '4',
        left: 'center',
        icon: 'circle',
        itemWidth: 10,
        itemHeight: 10,
        textStyle: { color: '#64748b', fontSize: 12 }
      },
      color: ['#f59e0b', '#0ea5e9', '#6366f1', '#10b981'],
      series: [
        {
          name: '履约节点',
          type: 'pie',
          radius: ['52%', '74%'],
          center: ['50%', '46%'],
          avoidLabelOverlap: false,
          itemStyle: { borderRadius: 8, borderColor: '#ffffff', borderWidth: 2 },
          label: { show: false, position: 'center' },
          emphasis: {
            label: {
              show: true,
              fontSize: '15',
              fontWeight: 'bold',
              color: '#0f172a',
              formatter: '{b}\n{d}%'
            }
          },
          labelLine: { show: false },
          data: data.statusData
        }
      ]
    })
  }

  // 3. TOP 5 热销商品横向柱状图 (排行榜)
  if (rankChartRef.value) {
    const rankChart = getOrCreateChart(rankChartRef.value)
    rankChart.setOption({
      backgroundColor: 'transparent',
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'shadow' },
        backgroundColor: 'rgba(15, 23, 42, 0.9)',
        borderColor: 'rgba(255, 255, 255, 0.1)',
        padding: [10, 14],
        textStyle: { color: '#ffffff' },
        formatter: (params: any) => {
          const item = Array.isArray(params) ? params[0] : params
          return `<div style="font-size:12px;color:#94a3b8;margin-bottom:4px;">商品畅销排位</div>
                  <div style="font-size:14px;font-weight:700;color:#f8fafc;">${item.name}</div>
                  <div style="font-size:13px;color:#38bdf8;margin-top:2px;">出库销量: <b>${item.value} 件</b></div>`
        }
      },
      grid: { left: '3%', right: '8%', bottom: '4%', top: '6%', containLabel: true },
      xAxis: {
        type: 'value',
        show: false
      },
      yAxis: {
        type: 'category',
        inverse: true, // 排行榜第1名居首
        data: data.rank.names,
        axisLine: { show: false },
        axisTick: { show: false },
        axisLabel: {
          color: '#334155',
          fontSize: 13,
          fontWeight: 600,
          margin: 12
        }
      },
      series: [
        {
          name: '出库销量',
          type: 'bar',
          barWidth: 16,
          showBackground: true,
          backgroundStyle: { color: '#f1f5f9', borderRadius: [0, 8, 8, 0] },
          itemStyle: {
            borderRadius: [0, 8, 8, 0],
            color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
              { offset: 0, color: '#3b82f6' },
              { offset: 1, color: '#6366f1' }
            ])
          },
          label: {
            show: true,
            position: 'right',
            formatter: '{c} 件',
            color: '#64748b',
            fontSize: 12,
            fontWeight: 600
          },
          data: data.rank.sales
        }
      ]
    })
  }
}

// 自适应窗口重绘
const handleResize = () => {
  charts.forEach(chart => chart.resize())
}

let resizeObserver: ResizeObserver | null = null

onMounted(() => {
  fetchDashboardData()
  window.addEventListener('resize', handleResize)
  if (typeof ResizeObserver !== 'undefined') {
    resizeObserver = new ResizeObserver(() => {
      handleResize()
    })
    if (trendChartRef.value) resizeObserver.observe(trendChartRef.value)
    if (statusChartRef.value) resizeObserver.observe(statusChartRef.value)
    if (rankChartRef.value) resizeObserver.observe(rankChartRef.value)
  }
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (resizeObserver) {
    resizeObserver.disconnect()
    resizeObserver = null
  }
  charts.forEach(chart => chart.dispose())
})
</script>

<template>
  <div class="dashboard-bento-container">
    <!-- 1. 顶部驾驶舱信息头 -->
    <div class="bento-header">
      <div class="header-left">
        <div class="title-row">
          <h2 class="page-title">全域数据运营驾驶舱</h2>
          <div class="status-indicator-badge">
            <span class="pulse-dot"></span>
            <span class="status-text">全系统服务在线</span>
          </div>
        </div>
        <p class="subtitle">实时聚合交易指标、订单履约状态与商品动销态势分析</p>
      </div>
      <div class="header-right">
        <div class="sync-meta" v-if="lastSyncTime">
          <span class="sync-label">同步时间:</span>
          <span class="sync-time">{{ lastSyncTime }}</span>
        </div>
        <button class="bento-refresh-btn" @click="fetchDashboardData" :disabled="loading" title="刷新最新看板数据">
          <el-icon :class="{ 'is-loading': loading }"><Refresh /></el-icon>
          <span>实时同步</span>
        </button>
      </div>
    </div>

    <!-- 2. Bento 顶层指标网格 (4 列 KPI 卡片) -->
    <div class="kpi-grid">
      <!-- 卡片 1: 累计销售总额 (Hero Card) -->
      <div class="bento-card kpi-card hero-card">
        <div class="card-ambient-glow"></div>
        <div class="kpi-card-header">
          <div class="kpi-icon-box sapphire">
            <el-icon><Money /></el-icon>
          </div>
          <span class="trend-badge positive">
            <el-icon><TopRight /></el-icon> +18.4%
          </span>
        </div>
        <div class="kpi-body">
          <div class="kpi-title">累计销售总额 (GMV)</div>
          <div class="kpi-value highlight">¥ {{ coreMetrics.totalSales.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
        </div>
        <div class="kpi-footer">
          <span class="footer-label">月度目标达成度</span>
          <div class="kpi-progress-bar">
            <div class="progress-fill" style="width: 86%;"></div>
          </div>
          <span class="progress-val">86%</span>
        </div>
      </div>

      <!-- 卡片 2: 今日实时交易额 -->
      <div class="bento-card kpi-card">
        <div class="kpi-card-header">
          <div class="kpi-icon-box emerald">
            <el-icon><TrendCharts /></el-icon>
          </div>
          <span class="trend-badge positive">
            <el-icon><TopRight /></el-icon> +8.2%
          </span>
        </div>
        <div class="kpi-body">
          <div class="kpi-title">今日实时交易额</div>
          <div class="kpi-value">¥ {{ coreMetrics.todaySales.toLocaleString('zh-CN', { minimumFractionDigits: 2 }) }}</div>
        </div>
        <div class="kpi-footer">
          <span class="footer-label">较昨日同时段</span>
          <span class="footer-pill-tag tag-green">+¥1,420.00</span>
        </div>
      </div>

      <!-- 卡片 3: 累计订单流转量 -->
      <div class="bento-card kpi-card">
        <div class="kpi-card-header">
          <div class="kpi-icon-box indigo">
            <el-icon><ShoppingCart /></el-icon>
          </div>
          <span class="trend-badge positive">
            <el-icon><TopRight /></el-icon> +12.6%
          </span>
        </div>
        <div class="kpi-body">
          <div class="kpi-title">累计订单流转量</div>
          <div class="kpi-value">{{ coreMetrics.totalOrders.toLocaleString() }} <span class="unit-text">笔</span></div>
        </div>
        <div class="kpi-footer">
          <span class="footer-label">整体履约完成率</span>
          <span class="footer-pill-tag tag-blue">98.5%</span>
        </div>
      </div>

      <!-- 卡片 4: 平台有效买家数 -->
      <div class="bento-card kpi-card">
        <div class="kpi-card-header">
          <div class="kpi-icon-box sky">
            <el-icon><User /></el-icon>
          </div>
          <span class="trend-badge neutral">稳定增长</span>
        </div>
        <div class="kpi-body">
          <div class="kpi-title">平台有效买家数</div>
          <div class="kpi-value">{{ coreMetrics.totalUsers.toLocaleString() }} <span class="unit-text">位</span></div>
        </div>
        <div class="kpi-footer">
          <span class="footer-label">30天活跃买家</span>
          <span class="footer-pill-tag tag-sky">4,280 位活跃</span>
        </div>
      </div>
    </div>

    <!-- 3. Bento 中层图表格 (销售走势 65% + 订单状态分布 35%) -->
    <div class="bento-middle-grid">
      <!-- 7天销售趋势面积图 -->
      <div class="bento-card chart-card trend-card">
        <div class="chart-header">
          <div class="header-meta">
            <h3 class="chart-headline">近7天销售额走势与流水曲线</h3>
            <p class="chart-sub">全渠道订单支付流水趋势，展示近期动销波峰波谷</p>
          </div>
          <div class="period-toggle-group">
            <span class="toggle-pill" :class="{ active: selectedPeriod === '7d' }" @click="selectedPeriod = '7d'">近7天</span>
            <span class="toggle-pill" :class="{ active: selectedPeriod === '30d' }" @click="selectedPeriod = '30d'">近30天</span>
            <span class="toggle-pill" :class="{ active: selectedPeriod === 'quarter' }" @click="selectedPeriod = 'quarter'">本季度</span>
          </div>
        </div>
        <div class="chart-canvas-wrapper" ref="trendChartRef"></div>
      </div>

      <!-- 订单履约分布环形图 -->
      <div class="bento-card chart-card status-card">
        <div class="chart-header">
          <div class="header-meta">
            <h3 class="chart-headline">订单履约流转分布</h3>
            <p class="chart-sub">实时监控各履约节点的单量流向</p>
          </div>
          <span class="state-pill">链路畅通</span>
        </div>
        <div class="chart-canvas-wrapper" ref="statusChartRef"></div>
      </div>
    </div>

    <!-- 4. Bento 底层网格 (热销排行 58% + 系统拓扑与快捷运维 42%) -->
    <div class="bento-bottom-grid">
      <!-- 热销排行榜 -->
      <div class="bento-card chart-card rank-card">
        <div class="chart-header">
          <div class="header-meta">
            <h3 class="chart-headline">TOP 5 爆款商品销量排行榜</h3>
            <p class="chart-sub">近7天出库件数最高的核心爆款单品与库存流速</p>
          </div>
          <span class="rank-badge-tag">动销率 91%</span>
        </div>
        <div class="chart-canvas-wrapper" ref="rankChartRef"></div>
      </div>

      <!-- 平台集群健康态 & 高频运维入口 -->
      <div class="bento-card ops-health-card">
        <div class="chart-header">
          <div class="header-meta">
            <h3 class="chart-headline">集群态势 & 快捷运维</h3>
            <p class="chart-sub">后端微服务运行状态及高频管理功能跳转</p>
          </div>
          <div class="live-status-pill">
            <span class="ping-dot"></span> 实时
          </div>
        </div>

        <!-- 节点健康指标 -->
        <div class="nodes-status-grid">
          <div class="node-status-item">
            <div class="node-indicator success"></div>
            <div class="node-detail">
              <div class="node-title">Spring Boot API</div>
              <div class="node-state">200 OK · 18ms</div>
            </div>
          </div>

          <div class="node-status-item">
            <div class="node-indicator success"></div>
            <div class="node-detail">
              <div class="node-title">MySQL 8.0 实例</div>
              <div class="node-state">活跃连接 14/100</div>
            </div>
          </div>

          <div class="node-status-item">
            <div class="node-indicator success"></div>
            <div class="node-detail">
              <div class="node-title">Redis 7.0 缓存</div>
              <div class="node-state">命中率 96.4%</div>
            </div>
          </div>

          <div class="node-status-item">
            <div class="node-indicator success"></div>
            <div class="node-detail">
              <div class="node-title">消息总线 MQ</div>
              <div class="node-state">无积压 · 正常</div>
            </div>
          </div>
        </div>

        <div class="ops-grid-divider"></div>

        <!-- 高频快捷跳转 -->
        <div class="shortcuts-section">
          <div class="shortcuts-header">高频管理快捷入口</div>
          <div class="shortcuts-buttons-grid">
            <div class="bento-action-btn" @click="router.push('/products')">
              <div class="btn-left">
                <el-icon class="action-icon"><Goods /></el-icon>
                <span class="action-name">商品上架与管理</span>
              </div>
              <el-icon class="arrow-cue"><ArrowRight /></el-icon>
            </div>

            <div class="bento-action-btn" @click="router.push('/orders')">
              <div class="btn-left">
                <el-icon class="action-icon"><List /></el-icon>
                <span class="action-name">订单出库与履约</span>
              </div>
              <el-icon class="arrow-cue"><ArrowRight /></el-icon>
            </div>

            <div class="bento-action-btn" @click="router.push('/categories')">
              <div class="btn-left">
                <el-icon class="action-icon"><DataLine /></el-icon>
                <span class="action-name">商品类目编排</span>
              </div>
              <el-icon class="arrow-cue"><ArrowRight /></el-icon>
            </div>

            <div class="bento-action-btn" @click="router.push('/ads')">
              <div class="btn-left">
                <el-icon class="action-icon"><Picture /></el-icon>
                <span class="action-name">首页营销轮播</span>
              </div>
              <el-icon class="arrow-cue"><ArrowRight /></el-icon>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 全局驾驶舱容器与现代 Bento 网格 */
.dashboard-bento-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
  color: #0f172a;
}

/* 顶部信息头 */
.bento-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: 0 1px 3px rgba(15, 23, 42, 0.04);
}

.title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.02em;
  color: #0f172a;
}

.subtitle {
  margin: 4px 0 0 0;
  font-size: 13px;
  color: #64748b;
}

.status-indicator-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  border-radius: 9999px;
  font-size: 12px;
  font-weight: 600;
  color: #059669;
}

.pulse-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background-color: #10b981;
  box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7);
  animation: pulse-ring 2s infinite cubic-bezier(0.45, 0, 0.55, 1);
}

@keyframes pulse-ring {
  0% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7); }
  70% { box-shadow: 0 0 0 6px rgba(16, 185, 129, 0); }
  100% { box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
}

.header-right {
  display: flex;
  align-items: center;
  gap: 14px;
}

.sync-meta {
  font-size: 12px;
  color: #94a3b8;
}

.sync-time {
  margin-left: 4px;
  font-weight: 600;
  color: #475569;
}

.bento-refresh-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
  cursor: pointer;
  transition: all 0.2s ease;
}

.bento-refresh-btn:hover:not(:disabled) {
  background: #2563eb;
  border-color: #2563eb;
  color: #ffffff;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
}

.bento-refresh-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

/* Bento 基础卡片规范 */
.bento-card {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.04);
  padding: 20px;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
  position: relative;
  overflow: hidden;
}

.bento-card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.08);
  border-color: #cbd5e1;
}

/* KPI 网格 (4 列) */
.kpi-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.kpi-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.kpi-card.hero-card {
  background: linear-gradient(135deg, #ffffff 0%, #f0f7ff 100%);
  border-color: #bfdbfe;
}

.card-ambient-glow {
  position: absolute;
  top: -30px;
  right: -30px;
  width: 90px;
  height: 90px;
  border-radius: 50%;
  background: radial-gradient(circle, rgba(37, 99, 235, 0.15) 0%, rgba(37, 99, 235, 0) 70%);
  pointer-events: none;
}

.kpi-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.kpi-icon-box {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.kpi-icon-box.sapphire {
  background: #eff6ff;
  color: #2563eb;
}

.kpi-icon-box.emerald {
  background: #ecfdf5;
  color: #059669;
}

.kpi-icon-box.indigo {
  background: #eef2ff;
  color: #4f46e5;
}

.kpi-icon-box.sky {
  background: #f0f9ff;
  color: #0284c7;
}

.trend-badge {
  display: inline-flex;
  align-items: center;
  gap: 2px;
  padding: 3px 8px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}

.trend-badge.positive {
  background: #ecfdf5;
  color: #059669;
}

.trend-badge.neutral {
  background: #f1f5f9;
  color: #64748b;
}

.kpi-title {
  font-size: 13px;
  font-weight: 500;
  color: #64748b;
  margin-bottom: 6px;
}

.kpi-value {
  font-size: 26px;
  font-weight: 800;
  letter-spacing: -0.02em;
  color: #0f172a;
  line-height: 1.2;
}

.kpi-value.highlight {
  color: #2563eb;
}

.unit-text {
  font-size: 14px;
  font-weight: 500;
  color: #64748b;
  margin-left: 2px;
}

.kpi-footer {
  margin-top: 14px;
  padding-top: 10px;
  border-top: 1px solid #f1f5f9;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
}

.footer-label {
  color: #94a3b8;
}

.kpi-progress-bar {
  flex: 1;
  height: 6px;
  background: #e2e8f0;
  border-radius: 3px;
  margin: 0 10px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #2563eb;
  border-radius: 3px;
}

.progress-val {
  font-weight: 700;
  color: #2563eb;
}

.footer-pill-tag {
  font-weight: 600;
  padding: 2px 6px;
  border-radius: 4px;
}

.footer-pill-tag.tag-green {
  background: #f0fdf4;
  color: #16a34a;
}

.footer-pill-tag.tag-blue {
  background: #eff6ff;
  color: #2563eb;
}

.footer-pill-tag.tag-sky {
  background: #f0f9ff;
  color: #0284c7;
}

/* Bento 中层与底层网格 */
.bento-middle-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 16px;
}

.bento-bottom-grid {
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  gap: 16px;
}

/* 图表卡片公用结构 */
.chart-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}

.chart-headline {
  margin: 0;
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.chart-sub {
  margin: 4px 0 0 0;
  font-size: 12px;
  color: #64748b;
}

.chart-canvas-wrapper {
  height: 310px;
  width: 100%;
}

/* 周期切换 Pill 组 */
.period-toggle-group {
  display: inline-flex;
  padding: 2px;
  background: #f1f5f9;
  border-radius: 8px;
}

.toggle-pill {
  padding: 4px 10px;
  font-size: 12px;
  font-weight: 500;
  color: #64748b;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.2s ease;
}

.toggle-pill.active {
  background: #ffffff;
  color: #2563eb;
  font-weight: 600;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.state-pill {
  padding: 3px 8px;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #059669;
}

.rank-badge-tag {
  padding: 3px 8px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
  color: #2563eb;
}

/* 集群健康与快捷操作卡片 */
.ops-health-card {
  display: flex;
  flex-direction: column;
}

.live-status-pill {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 12px;
  font-weight: 600;
  color: #10b981;
}

.ping-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #10b981;
}

.nodes-status-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 10px;
  margin-top: 4px;
}

.node-status-item {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 10px 12px;
  background: #f8fafc;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
}

.node-indicator {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.node-indicator.success {
  background: #10b981;
  box-shadow: 0 0 6px rgba(16, 185, 129, 0.6);
}

.node-title {
  font-size: 12px;
  font-weight: 600;
  color: #334155;
}

.node-state {
  font-size: 11px;
  color: #64748b;
  margin-top: 1px;
}

.ops-grid-divider {
  height: 1px;
  background: #f1f5f9;
  margin: 14px 0 10px 0;
}

.shortcuts-header {
  font-size: 12px;
  font-weight: 600;
  color: #64748b;
  margin-bottom: 8px;
}

.shortcuts-buttons-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
}

.bento-action-btn {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.bento-action-btn:hover {
  background: #eff6ff;
  border-color: #93c5fd;
  transform: translateX(2px);
}

.btn-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.action-icon {
  font-size: 16px;
  color: #2563eb;
}

.action-name {
  font-size: 12px;
  font-weight: 600;
  color: #1e293b;
}

.arrow-cue {
  font-size: 12px;
  color: #94a3b8;
  transition: transform 0.2s ease;
}

.bento-action-btn:hover .arrow-cue {
  transform: translateX(2px);
  color: #2563eb;
}

/* 响应式断点控制 */
@media (max-width: 1200px) {
  .kpi-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .bento-middle-grid,
  .bento-bottom-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .kpi-grid {
    grid-template-columns: 1fr;
  }
  .nodes-status-grid,
  .shortcuts-buttons-grid {
    grid-template-columns: 1fr;
  }
}
</style>
