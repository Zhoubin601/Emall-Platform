export interface DashboardData {
  metrics: {
    totalUsers: number
    totalOrders: number
    totalSales: number
    todaySales: number
  }
  trend: { dates: string[]; sales: number[] }
  statusData: Array<{ name: string; value: number }>
  rank: { names: string[]; sales: number[] }
}

const finiteNumber = (value: unknown): number => {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : 0
}

export const normalizeDashboardData = (raw: unknown): DashboardData => {
  const data = raw && typeof raw === 'object' ? raw as Record<string, any> : {}
  const metrics = data.metrics && typeof data.metrics === 'object' ? data.metrics : {}
  const trend = data.trend && typeof data.trend === 'object' ? data.trend : {}
  const rank = data.rank && typeof data.rank === 'object' ? data.rank : {}

  return {
    metrics: {
      totalUsers: finiteNumber(metrics.totalUsers),
      totalOrders: finiteNumber(metrics.totalOrders),
      totalSales: finiteNumber(metrics.totalSales),
      todaySales: finiteNumber(metrics.todaySales)
    },
    trend: {
      dates: Array.isArray(trend.dates) ? trend.dates.map(String) : [],
      sales: Array.isArray(trend.sales) ? trend.sales.map(finiteNumber) : []
    },
    statusData: Array.isArray(data.statusData)
      ? data.statusData.map((item: any) => ({
          name: String(item?.name ?? ''),
          value: finiteNumber(item?.value)
        }))
      : [],
    rank: {
      names: Array.isArray(rank.names) ? rank.names.map(String) : [],
      sales: Array.isArray(rank.sales) ? rank.sales.map(finiteNumber) : []
    }
  }
}
