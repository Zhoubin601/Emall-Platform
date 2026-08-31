const http = require('http')
const url = require('url')

const PORT = 8080

const mockCategories = [
  { id: 1, name: '📱 智能数码', parentId: 0, level: 1, sort: 1 },
  { id: 2, name: '💻 电脑办公', parentId: 0, level: 1, sort: 2 },
  { id: 3, name: '👕 潮流服饰', parentId: 0, level: 1, sort: 3 },
  { id: 4, name: '🥤 美味零食', parentId: 0, level: 1, sort: 4 },
  { id: 5, name: '🏠 家居家电', parentId: 0, level: 1, sort: 5 },
  { id: 6, name: '💄 美妆个护', parentId: 0, level: 1, sort: 6 }
]

const mockAds = [
  {
    id: 1,
    title: '新品首发·钛金属极简美学',
    picUrl: 'https://images.unsplash.com/photo-1607082348824-0a96f2a4b9da?auto=format&fit=crop&w=1400&q=80',
    linkUrl: '/'
  },
  {
    id: 2,
    title: '极客装备·静享纯粹原音',
    picUrl: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=1400&q=80',
    linkUrl: '/'
  },
  {
    id: 3,
    title: '潮流生活季·大额立减券限时领',
    picUrl: 'https://images.unsplash.com/photo-1441986300917-64674bd600d8?auto=format&fit=crop&w=1400&q=80',
    linkUrl: '/'
  }
]

const mockCoupons = [
  { id: 1, name: '新人专享神券', minAmount: 100, discountAmount: 20, startTime: '2026-01-01', endTime: '2026-12-31' },
  { id: 2, name: '满减大促特权', minAmount: 300, discountAmount: 50, startTime: '2026-01-01', endTime: '2026-12-31' },
  { id: 3, name: '数码高额满减', minAmount: 1000, discountAmount: 150, startTime: '2026-01-01', endTime: '2026-12-31' }
]

const mockHotSearches = [
  { id: 1, keyword: 'iPhone 16 Pro', searchCount: 1999 },
  { id: 2, keyword: '华为 Mate 70', searchCount: 1888 },
  { id: 3, keyword: '降噪无线耳机', searchCount: 1560 },
  { id: 4, keyword: '客制化机械键盘', searchCount: 1210 },
  { id: 5, keyword: '氮化镓快充', searchCount: 980 }
]

const mockProducts = [
  {
    id: 101,
    name: 'iPhone 16 Pro Max 钛金属极光灰',
    description: '全新 A18 Pro 仿生芯片，超视网膜 XDR 全面屏，4800万像素超广角，钛金属流线机身',
    price: 8999,
    promoPrice: 8499,
    promoStartTime: '2026-01-01 00:00:00',
    promoEndTime: '2026-12-31 23:59:59',
    stock: 68,
    sales: 420,
    categoryId: 1,
    picUrl: 'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?auto=format&fit=crop&w=600&q=80',
    subImages: [
      'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?auto=format&fit=crop&w=600&q=80',
      'https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?auto=format&fit=crop&w=600&q=80'
    ],
    detailHtml: '<p>旗舰级专业影像与超长续航体验，采用航空级五级钛金属材质打造。</p>'
  },
  {
    id: 102,
    name: '极简主动降噪无线头戴式耳机',
    description: 'Hi-Res 高清音频金标认证，数字主动混合降噪，40小时持久长续航，记忆海绵耳罩',
    price: 1299,
    promoPrice: 1099,
    promoStartTime: '2026-01-01 00:00:00',
    promoEndTime: '2026-12-31 23:59:59',
    stock: 150,
    sales: 780,
    categoryId: 1,
    picUrl: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=600&q=80',
    subImages: ['https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=600&q=80'],
    detailHtml: '<p>沉浸式环绕立体声，支持通透模式与多设备无缝切换。</p>'
  },
  {
    id: 103,
    name: '极客客制化机械键盘 87键 RGB',
    description: '全铝合金机身，热插拔客制化线性轴体，全键无冲，PBT热升华键帽，低延迟无线三模',
    price: 499,
    stock: 95,
    sales: 340,
    categoryId: 2,
    picUrl: 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=600&q=80',
    subImages: ['https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=600&q=80'],
    detailHtml: '<p>手感扎实清脆，多层消音棉设计，打造纯粹的打字音质。</p>'
  },
  {
    id: 104,
    name: '极速磁吸无线充电底座 15W',
    description: 'Qi2 官方认证，15W 满速快充，智能温控不发烫，异物检测多重安全防护',
    price: 199,
    stock: 260,
    sales: 620,
    categoryId: 1,
    picUrl: 'https://images.unsplash.com/photo-1583863788434-e58a36330cf0?auto=format&fit=crop&w=600&q=80',
    subImages: ['https://images.unsplash.com/photo-1583863788434-e58a36330cf0?auto=format&fit=crop&w=600&q=80'],
    detailHtml: '<p>一贴即充，告别线缆繁琐，桌面整洁新体验。</p>'
  },
  {
    id: 105,
    name: '纯棉极简高质感微弹圆领T恤',
    description: '100% 精梳长绒棉，240g 重磅微弹透气面料，领口加固抗变形工艺',
    price: 129,
    stock: 310,
    sales: 890,
    categoryId: 3,
    picUrl: 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=600&q=80',
    subImages: ['https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=600&q=80'],
    detailHtml: '<p>百搭舒适，亲肤透气，适合四季日常穿搭。</p>'
  },
  {
    id: 106,
    name: '原叶现萃高山冷泡大红袍乌龙茶',
    description: '核心产区原叶采摘，低温慢速冷萃工艺，0糖0脂0卡，清爽回甘',
    price: 69,
    stock: 500,
    sales: 1420,
    categoryId: 4,
    picUrl: 'https://images.unsplash.com/photo-1556679343-c7306c1976bc?auto=format&fit=crop&w=600&q=80',
    subImages: ['https://images.unsplash.com/photo-1556679343-c7306c1976bc?auto=format&fit=crop&w=600&q=80'],
    detailHtml: '<p>纯粹茶香，一口清润，健康无负担。</p>'
  }
]

const mockSkus = {
  101: [
    { id: 1, productId: 101, specName: '256GB / 钛原色', price: 8999, stock: 30 },
    { id: 2, productId: 101, specName: '512GB / 极光灰', price: 9999, stock: 25 },
    { id: 3, productId: 101, specName: '1TB / 暗夜黑', price: 11999, stock: 13 }
  ],
  102: [
    { id: 4, productId: 102, specName: '曜石黑 / 旗舰降噪版', price: 1299, stock: 80 },
    { id: 5, productId: 102, specName: '晨雾银 / 旗舰降噪版', price: 1299, stock: 70 }
  ],
  103: [
    { id: 6, productId: 103, specName: '红轴 / 极夜黑RGB', price: 499, stock: 50 },
    { id: 7, productId: 103, specName: '茶轴 / 冰川白RGB', price: 499, stock: 45 }
  ]
}

const mockOrders = [
  {
    id: 1,
    orderSn: 'EMALL202608310001',
    totalAmount: '8499.00',
    payAmount: '8499.00',
    status: 2,
    createTime: '2026-08-31 10:30:00',
    items: [
      {
        id: 1,
        productId: 101,
        productName: 'iPhone 16 Pro Max 钛金属极光灰',
        productPrice: 8499,
        quantity: 1,
        productPic: 'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?auto=format&fit=crop&w=600&q=80',
        spec: '256GB / 极光灰'
      }
    ]
  },
  {
    id: 2,
    orderSn: 'EMALL202608310002',
    totalAmount: '1299.00',
    payAmount: '1299.00',
    status: 1,
    createTime: '2026-08-31 12:15:00',
    items: [
      {
        id: 2,
        productId: 102,
        productName: '极简主动降噪无线头戴式耳机',
        productPrice: 1299,
        quantity: 1,
        productPic: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=600&q=80',
        spec: '曜石黑 / 旗舰降噪版'
      }
    ]
  },
  {
    id: 3,
    orderSn: 'EMALL202608310003',
    totalAmount: '499.00',
    payAmount: '499.00',
    status: 3,
    commentStatus: 0,
    createTime: '2026-08-30 16:40:00',
    items: [
      {
        id: 3,
        productId: 103,
        productName: '极客客制化机械键盘 87键 RGB',
        productPrice: 499,
        quantity: 1,
        productPic: 'https://images.unsplash.com/photo-1587829741301-dc798b83add3?auto=format&fit=crop&w=600&q=80',
        spec: '红轴 / 极夜黑RGB'
      }
    ]
  }
]

const server = http.createServer((req, res) => {
  const parsedUrl = url.parse(req.url, true)
  const pathname = parsedUrl.pathname
  const method = req.method

  // Set CORS headers
  res.setHeader('Access-Control-Allow-Origin', '*')
  res.setHeader('Access-Control-Allow-Methods', 'GET, POST, PUT, DELETE, OPTIONS')
  res.setHeader('Access-Control-Allow-Headers', 'Content-Type, Authorization, token')
  res.setHeader('Content-Type', 'application/json; charset=utf-8')

  if (method === 'OPTIONS') {
    res.writeHead(204)
    return res.end()
  }

  // 1. Categories
  if (pathname === '/api/category/list') {
    res.writeHead(200)
    return res.end(JSON.stringify(mockCategories))
  }

  // 2. Ads
  if (pathname === '/api/ad/active') {
    res.writeHead(200)
    return res.end(JSON.stringify(mockAds))
  }

  // 3. Coupons
  if (pathname === '/api/coupon/list' || pathname === '/api/coupon/my') {
    res.writeHead(200)
    return res.end(JSON.stringify(mockCoupons))
  }

  // 4. Hot searches
  if (pathname === '/api/product/hotSearches') {
    res.writeHead(200)
    return res.end(JSON.stringify(mockHotSearches))
  }

  // 5. Product list
  if (pathname === '/api/product/list') {
    const categoryId = parsedUrl.query.categoryId
    const keyword = parsedUrl.query.keyword
    let list = [...mockProducts]
    if (categoryId) {
      list = list.filter(p => p.categoryId === Number(categoryId))
    }
    if (keyword) {
      list = list.filter(p => p.name.includes(keyword) || p.description.includes(keyword))
    }
    res.writeHead(200)
    return res.end(JSON.stringify(list))
  }

  // 6. Product batch
  if (pathname === '/api/product/batch') {
    res.writeHead(200)
    return res.end(JSON.stringify(mockProducts))
  }

  // 7. Product detail
  const detailMatch = pathname.match(/^\/api\/product\/detail\/(\d+)$/)
  if (detailMatch) {
    const id = Number(detailMatch[1])
    const product = mockProducts.find(p => p.id === id) || mockProducts[0]
    res.writeHead(200)
    return res.end(JSON.stringify(product))
  }

  // 8. Product SKUs
  const skusMatch = pathname.match(/^\/api\/product\/skus\/(\d+)$/)
  if (skusMatch) {
    const id = Number(skusMatch[1])
    const skus = mockSkus[id] || [
      { id: 1001, productId: id, specName: '标准款 / 曜石黑', price: 999, stock: 50 }
    ]
    res.writeHead(200)
    return res.end(JSON.stringify(skus))
  }

  // 9. User login
  if (pathname === '/api/user/login' || pathname === '/api/admin/login') {
    res.writeHead(200)
    return res.end(JSON.stringify({
      token: 'emall-live-session-token',
      user: {
        id: 7,
        username: 'buyer',
        nickname: 'VIP星选体验官',
        role: pathname.includes('admin') ? 1 : 0,
        status: 1
      }
    }))
  }

  // 10. User profile
  if (pathname === '/api/user/info' || pathname === '/api/user/profile') {
    res.writeHead(200)
    return res.end(JSON.stringify({
      id: 7,
      username: 'buyer',
      nickname: 'VIP星选体验官',
      avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80',
      email: 'vip@emall.com',
      phone: '13800138000',
      role: 0
    }))
  }

  // 11. User orders
  if (pathname === '/api/order/my') {
    res.writeHead(200)
    return res.end(JSON.stringify(mockOrders))
  }

  // 12. Dashboard stats
  if (pathname === '/api/dashboard/data' || pathname === '/api/dashboard/stats') {
    res.writeHead(200)
    return res.end(JSON.stringify({
      metrics: {
        totalUsers: 12800,
        totalOrders: 3450,
        totalSales: 588690.0,
        todaySales: 18450.0
      },
      trend: {
        dates: ['08-25', '08-26', '08-27', '08-28', '08-29', '08-30', '08-31'],
        sales: [42000, 58000, 61000, 78000, 89000, 112000, 148690]
      },
      statusData: [
        { name: '待支付', value: 120 },
        { name: '已支付/待发货', value: 450 },
        { name: '已发货/运输中', value: 780 },
        { name: '已完成', value: 2100 }
      ],
      rank: {
        names: ['iPhone 16 Pro Max', '降噪头戴耳机', '机械键盘', '磁吸充电底座', '微弹圆领T恤'],
        sales: [980, 750, 620, 510, 430]
      }
    }))
  }

  // Default fallback for any other GET/POST endpoint
  res.writeHead(200)
  res.end(JSON.stringify({ code: 200, message: 'success', data: [] }))
})

server.listen(PORT, '127.0.0.1', () => {
  console.log(`E-MALL Mock Backend Service is running on http://127.0.0.1:${PORT}`)
})
