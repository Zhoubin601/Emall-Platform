import { describe, expect, it } from 'vitest'
import { normalizeDashboardData } from './dashboardData'

describe('normalizeDashboardData', () => {
  it('returns render-safe defaults for an empty response', () => {
    expect(normalizeDashboardData([])).toEqual({
      metrics: { totalUsers: 0, totalOrders: 0, totalSales: 0, todaySales: 0 },
      trend: { dates: [], sales: [] },
      statusData: [],
      rank: { names: [], sales: [] }
    })
  })

  it('normalizes numeric strings and malformed chart values', () => {
    const result = normalizeDashboardData({
      metrics: { totalUsers: '12', totalOrders: null, totalSales: 'bad', todaySales: 9 },
      trend: { dates: ['07-22'], sales: ['10'] },
      statusData: [{ name: '已完成', value: '3' }],
      rank: { names: ['商品'], sales: [undefined] }
    })

    expect(result.metrics).toEqual({ totalUsers: 12, totalOrders: 0, totalSales: 0, todaySales: 9 })
    expect(result.trend.sales).toEqual([10])
    expect(result.statusData).toEqual([{ name: '已完成', value: 3 }])
    expect(result.rank.sales).toEqual([0])
  })
})
