<script setup lang="ts">
import { ref, onMounted, onUnmounted, markRaw } from 'vue'
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
import { User, ShoppingCart, Money, TrendCharts } from '@element-plus/icons-vue'
import request from '../../utils/request' // ✨ 新增引入 request
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

// ✨ 核心数据指标 (初始化为空)
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

// ✨ 新增：拉取后端真实看板数据
const fetchDashboardData = async () => {
  try {
    const res = normalizeDashboardData(await request.get('/dashboard/data'))
    
    // 1. 渲染四大核心指标
    coreMetrics.value = {
      totalUsers: res.metrics.totalUsers,
      totalOrders: res.metrics.totalOrders,
      totalSales: res.metrics.totalSales,
      todaySales: res.metrics.todaySales
    }

    // 2. 将数据扔给图表引擎进行动态绘制
    initCharts(res)
  } catch (error) {
    console.error('获取看板数据失败', error)
  }
}

// ✨ 修改：接收后端数据并绘制 ECharts
const initCharts = (data: DashboardData) => {
  // 1. 销量趋势折线图 (近7天)
  if (trendChartRef.value) {
    const trendChart = markRaw(echarts.init(trendChartRef.value))
    trendChart.setOption({
      title: { text: '近7天销售趋势', textStyle: { color: '#0369a1', fontSize: 16 } },
      tooltip: { trigger: 'axis', backgroundColor: 'rgba(255,255,255,0.9)' },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      // ✨ 动态注入日期
      xAxis: { type: 'category', boundaryGap: false, data: data.trend.dates },
      yAxis: { type: 'value' },
      series: [
        {
          name: '销售额 (元)',
          type: 'line',
          smooth: true,
          lineStyle: { width: 4, color: '#38bdf8' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(56, 189, 248, 0.5)' },
              { offset: 1, color: 'rgba(56, 189, 248, 0.05)' }
            ])
          },
          itemStyle: { color: '#0284c7' },
          // ✨ 动态注入近7天销售额
          data: data.trend.sales
        }
      ]
    })
    charts.push(trendChart)
  }

  // 2. 订单状态分布饼图
  if (statusChartRef.value) {
    const statusChart = markRaw(echarts.init(statusChartRef.value))
    statusChart.setOption({
      title: { text: '订单状态分布', left: 'center', textStyle: { color: '#0369a1', fontSize: 16 } },
      tooltip: { trigger: 'item' },
      legend: { bottom: '0', left: 'center' },
      color: ['#fb7185', '#38bdf8', '#34d399', '#94a3b8'], 
      series: [
        {
          name: '订单数',
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: { borderRadius: 10, borderColor: '#fff', borderWidth: 2 },
          label: { show: false, position: 'center' },
          emphasis: { label: { show: true, fontSize: '18', fontWeight: 'bold' } },
          labelLine: { show: false },
          // ✨ 动态注入状态数据
          data: data.statusData
        }
      ]
    })
    charts.push(statusChart)
  }

  // 3. 热销商品排行榜柱状图
  if (rankChartRef.value) {
    const rankChart = markRaw(echarts.init(rankChartRef.value))
    rankChart.setOption({
      title: { text: 'TOP 5 热销商品', textStyle: { color: '#0369a1', fontSize: 16 } },
      tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
      grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
      xAxis: { type: 'value' },
      yAxis: { 
        type: 'category', 
        // ✨ 动态注入商品名
        data: data.rank.names,
      },
      series: [
        {
          name: '销量 (件)',
          type: 'bar',
          barWidth: '20px',
          itemStyle: {
            color: new echarts.graphic.LinearGradient(1, 0, 0, 0, [
              { offset: 0, color: '#38bdf8' },
              { offset: 1, color: '#818cf8' }
            ]),
            borderRadius: [0, 10, 10, 0]
          },
          // ✨ 动态注入商品销量
          data: data.rank.sales
        }
      ]
    })
    charts.push(rankChart)
  }
}

// 监听窗口大小改变，实现图表自适应
const handleResize = () => {
  charts.forEach(chart => chart.resize())
}

onMounted(() => {
  fetchDashboardData() // ✨ 组件挂载时拉取真实数据
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach(chart => chart.dispose())
})
</script>

<template>
  <div class="dashboard-container">
    <el-row :gutter="20" class="metric-row">
      <el-col :span="6">
        <el-card class="glass-card metric-card" shadow="hover">
          <div class="metric-icon" style="background: rgba(56, 189, 248, 0.1); color: #0284c7;">
            <el-icon><User /></el-icon>
          </div>
          <div class="metric-info">
            <div class="label">总买家数</div>
            <div class="value">{{ coreMetrics.totalUsers.toLocaleString() }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="glass-card metric-card" shadow="hover">
          <div class="metric-icon" style="background: rgba(52, 211, 153, 0.1); color: #059669;">
            <el-icon><ShoppingCart /></el-icon>
          </div>
          <div class="metric-info">
            <div class="label">总订单数</div>
            <div class="value">{{ coreMetrics.totalOrders.toLocaleString() }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="glass-card metric-card" shadow="hover">
          <div class="metric-icon" style="background: rgba(251, 191, 36, 0.1); color: #d97706;">
            <el-icon><Money /></el-icon>
          </div>
          <div class="metric-info">
            <div class="label">累计总销售额 (元)</div>
            <div class="value highlight">¥ {{ coreMetrics.totalSales.toLocaleString() }}</div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="glass-card metric-card" shadow="hover">
          <div class="metric-icon" style="background: rgba(251, 113, 133, 0.1); color: #e11d48;">
            <el-icon><TrendCharts /></el-icon>
          </div>
          <div class="metric-info">
            <div class="label">今日销售额 (元)</div>
            <div class="value highlight">¥ {{ coreMetrics.todaySales.toLocaleString() }}</div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row class="chart-row">
      <el-col :span="24">
        <el-card class="glass-card chart-card" shadow="never">
          <div ref="trendChartRef" style="height: 350px; width: 100%;"></div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="10">
        <el-card class="glass-card chart-card" shadow="never">
          <div ref="statusChartRef" style="height: 350px; width: 100%;"></div>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card class="glass-card chart-card" shadow="never">
          <div ref="rankChartRef" style="height: 350px; width: 100%;"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.dashboard-container { padding: 10px; }
.glass-card { border-radius: 16px; border: 1px solid rgba(186, 230, 253, 0.6); background: rgba(255, 255, 255, 0.85); backdrop-filter: blur(12px); box-shadow: 0 8px 30px rgba(125, 211, 252, 0.1); transition: transform 0.3s ease; }
.glass-card:hover { transform: translateY(-3px); box-shadow: 0 12px 40px rgba(125, 211, 252, 0.2); }
.metric-row { margin-bottom: 20px; }
.chart-row { margin-bottom: 20px; }
.metric-card :deep(.el-card__body) { display: flex; align-items: center; padding: 25px 20px; }
.metric-icon { width: 60px; height: 60px; border-radius: 16px; display: flex; align-items: center; justify-content: center; font-size: 28px; margin-right: 20px; }
.metric-info { flex: 1; }
.metric-info .label { font-size: 14px; color: #64748b; margin-bottom: 8px; font-weight: bold; }
.metric-info .value { font-size: 26px; font-weight: 900; color: #0f172a; }
.metric-info .value.highlight { color: #0284c7; }
.chart-card { padding: 10px; }
</style>
