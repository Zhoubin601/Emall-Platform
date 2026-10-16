const http = require('http')
const url = require('url')

const PORT = 8080

const mockCategories = [
  { id: 1, name: '📱 智能数码', parentId: 0, level: 1, sort: 1 },
  { id: 2, name: '💻 电脑办公', parentId: 0, level: 1, sort: 2 },
  { id: 3, name: '👕 潮流服饰', parentId: 0, level: 1, sort: 3 },
  { id: 4, name: '🥤 美味零食', parentId: 0, level: 1, sort: 4 },
  { id: 5, name: '🏠 家居家电', parentId: 0, level: 1, sort: 5 },
  { id: 6, name: '💄 美妆个护', parentId: 0, level: 1, sort: 6 },
  { id: 11, name: '旗舰手机', parentId: 1, level: 2, sort: 1 },
  { id: 12, name: '声学耳机', parentId: 1, level: 2, sort: 2 },
  { id: 13, name: '快充配件', parentId: 1, level: 2, sort: 3 },
  { id: 21, name: '客制化外设', parentId: 2, level: 2, sort: 1 },
  { id: 22, name: '轻薄笔记本', parentId: 2, level: 2, sort: 2 },
  { id: 31, name: '极简男装', parentId: 3, level: 2, sort: 1 },
  { id: 41, name: '冷萃茶饮', parentId: 4, level: 2, sort: 1 },
  { id: 51, name: '智能小家电', parentId: 5, level: 2, sort: 1 }
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

const mockUsers = [
  {
    id: 1,
    username: 'admin',
    nickname: '超级管理员',
    phone: '13800000000',
    email: 'admin@emall.com',
    avatar: '',
    role: 2,
    status: 1,
    createTime: '2026-08-01 10:00:00'
  },
  {
    id: 2,
    username: 'operator',
    nickname: '电商运营主管',
    phone: '13800000002',
    email: 'operator@emall.com',
    avatar: '',
    role: 1,
    status: 1,
    createTime: '2026-08-10 14:30:00'
  },
  {
    id: 3,
    username: 'service01',
    nickname: '官方金牌客服小E',
    phone: '13800000003',
    email: 'service01@emall.com',
    avatar: '',
    role: 1,
    status: 1,
    createTime: '2026-08-15 09:00:00'
  },
  {
    id: 6,
    username: '603',
    nickname: 'zmjjkk (技术运维)',
    phone: '13800000603',
    email: '3283511301@qq.com',
    avatar: '',
    role: 1,
    status: 1,
    createTime: '2026-05-11 19:58:20'
  },
  {
    id: 7,
    username: 'buyer',
    nickname: 'VIP星选体验官',
    phone: '13800138000',
    email: 'vip@emall.com',
    avatar: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=200&q=80',
    role: 0,
    status: 1,
    createTime: '2026-08-20 11:20:00'
  },
  {
    id: 8,
    username: '605',
    nickname: 'zbb (品质买家)',
    phone: '13800000605',
    email: '3185130953@qq.com',
    avatar: '',
    role: 0,
    status: 1,
    createTime: '2026-05-13 10:54:37'
  },
  {
    id: 9,
    username: 'zhangsan',
    nickname: '张三 (极客发烧友)',
    phone: '13911112222',
    email: 'zhangsan@geek.com',
    avatar: '',
    role: 0,
    status: 1,
    createTime: '2026-08-25 16:40:00'
  },
  {
    id: 10,
    username: 'risk_user',
    nickname: '违规风险账号',
    phone: '13799998888',
    email: 'baduser@test.com',
    avatar: '',
    role: 0,
    status: 0,
    createTime: '2026-08-28 18:10:00'
  }
]

const mockCoupons = [
  { id: 1, name: '新人专享神券', minAmount: 100, discountAmount: 20, startTime: '2026-01-01', endTime: '2026-12-31' },
  { id: 2, name: '满减大促特权', minAmount: 300, discountAmount: 50, startTime: '2026-01-01', endTime: '2026-12-31' },
  { id: 3, name: '数码高额满减', minAmount: 1000, discountAmount: 150, startTime: '2026-01-01', endTime: '2026-12-31' }
]

const mockChatMessages = [
  {
    id: 1,
    userId: 7,
    content: '您好！欢迎来到 E-MALL 全球品质严选商城，我是您的专属智能客服小E。请问有什么可以为您服务？',
    senderRole: 1,
    type: '在线沟通',
    createTime: '2026-09-02 14:00:00'
  },
  {
    id: 2,
    userId: 7,
    content: '你好，请问我刚买的 iPhone 16 Pro Max 钛金属极光灰什么时候能安排顺丰发货呀？',
    senderRole: 0,
    type: '在线沟通',
    createTime: '2026-09-02 14:02:15'
  },
  {
    id: 3,
    userId: 7,
    content: '亲爱的 VIP 星选体验官您好，您的订单已进入高优出库通道，预计今日 16:00 前由顺丰特快揽收发出！',
    senderRole: 1,
    type: '在线沟通',
    createTime: '2026-09-02 14:03:05'
  },
  {
    id: 4,
    userId: 8,
    content: '客服你好，客制化机械键盘支持 Mac 系统的快捷键切换吗？',
    senderRole: 0,
    type: '在线沟通',
    createTime: '2026-09-02 15:18:40'
  },
  {
    id: 5,
    userId: 9,
    content: '请问满 1000 减 150 的数码大额神券还可以和限时秒杀叠加使用吗？',
    senderRole: 0,
    type: '在线沟通',
    createTime: '2026-09-02 16:05:12'
  }
]

const mockFeedbacks = [
  {
    id: 101,
    userId: 7,
    type: '功能建议',
    content: '希望增加更多旗舰数码产品的限时秒杀活动，并且支持以旧换新补贴估算！',
    reply: '感谢您的宝贵建议！下周即将上线秋季数码狂欢季，以旧换新功能已列入研发排期。',
    status: 1,
    createTime: '2026-09-01 14:20:00'
  },
  {
    id: 102,
    userId: 8,
    type: '售后咨询',
    content: '购买的无线降噪耳机想要更换耳罩配色，请问未拆封支持免费换货吗？',
    reply: '',
    status: 0,
    createTime: '2026-09-02 10:15:30'
  },
  {
    id: 103,
    userId: 9,
    type: '报错反馈',
    content: '在移动端 Safari 浏览器查看商品高清大图时偶发加载缓慢，希望优化 CDN 节点。',
    reply: '',
    status: 0,
    createTime: '2026-09-02 11:42:18'
  }
]

const mockNotices = [
  {
    id: 1,
    title: '🎉 欢迎来到 E-MALL 全球品质严选商城！',
    content: '新店开业狂欢盛典，全场限时特惠直降，更有大额神券每日限量领取，祝您购物愉快！',
    isActive: 1,
    createTime: '2026-09-01 10:00:00'
  },
  {
    id: 2,
    title: '📢 关于官方速运次日达配送时效升级公告',
    content: '即日起全国 300+ 核心城市支持官方自营仓储直发，最快次日送达上门，安心售后无忧保障！',
    isActive: 1,
    createTime: '2026-09-02 09:30:00'
  }
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
    status: 1,
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
    status: 1,
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
    status: 1,
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
    status: 1,
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
    status: 1,
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
    status: 1,
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
    userId: 7,
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
    userId: 7,
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
    userId: 8,
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
  },
  {
    id: 4,
    orderSn: 'EMALL202609010004',
    userId: 9,
    totalAmount: '199.00',
    payAmount: '199.00',
    status: 6,
    createTime: '2026-09-01 09:12:00',
    items: [
      {
        id: 4,
        productId: 104,
        productName: '极速磁吸无线充电底座 15W',
        productPrice: 199,
        quantity: 1,
        productPic: 'https://images.unsplash.com/photo-1583863788434-e58a36330cf0?auto=format&fit=crop&w=600&q=80',
        spec: '皓月白 / 15W快充'
      }
    ]
  },
  {
    id: 5,
    orderSn: 'EMALL202609010005',
    userId: 10,
    totalAmount: '129.00',
    payAmount: '129.00',
    status: 0,
    createTime: '2026-09-01 14:05:00',
    items: [
      {
        id: 5,
        productId: 105,
        productName: '纯棉极简高质感微弹圆领T恤',
        productPrice: 129,
        quantity: 1,
        productPic: 'https://images.unsplash.com/photo-1521572267360-ee0c2909d518?auto=format&fit=crop&w=600&q=80',
        spec: '白色 / L码'
      }
    ]
  }
]

const mockComments = [
  {
    id: 1,
    userId: 7,
    productId: 101,
    orderId: 1,
    orderSn: 'EMALL202608300001',
    nickname: '极客星人',
    productName: 'iPhone 16 Pro Max 钛金属极光灰',
    star: 5,
    content: '钛金属手感非常惊艳，沙漠金光泽柔和，相机控制按键抓拍运动物体非常敏捷，日常续航完全撑得住两天！',
    pics: 'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?auto=format&fit=crop&w=600&q=80',
    createTime: '2026-08-31 16:20:00'
  },
  {
    id: 2,
    userId: 7,
    productId: 102,
    orderId: 2,
    orderSn: 'EMALL202608310002',
    nickname: '音乐发烧友',
    productName: '极简主动降噪无线头戴式耳机',
    star: 5,
    content: '降噪效果非常干净，低频下潜有力不轰头，耳罩海绵很亲肤，戴眼镜也不会压耳朵。',
    pics: 'https://images.unsplash.com/photo-1505740420928-5e560c06d30e?auto=format&fit=crop&w=600&q=80',
    createTime: '2026-08-30 11:45:00'
  },
  {
    id: 3,
    userId: 8,
    productId: 103,
    orderId: 3,
    orderSn: 'EMALL202608310003',
    nickname: '码农小李',
    productName: '极客客制化机械键盘 87键 RGB',
    star: 4,
    content: '红轴打字非常顺滑，大键位调校得当没有杂音，驱动灯效可玩性很高，好评！',
    pics: '',
    createTime: '2026-08-29 18:30:00'
  },
  {
    id: 4,
    userId: 9,
    productId: 104,
    orderId: 4,
    orderSn: 'EMALL202609010004',
    nickname: '数码探索者',
    productName: '极速磁吸无线充电底座 15W',
    star: 5,
    content: '桌面瞬间整洁了，吸力很强横屏看视频稳当，发热控制比官方的还好。',
    pics: 'https://images.unsplash.com/photo-1583863788434-e58a36330cf0?auto=format&fit=crop&w=600&q=80',
    createTime: '2026-08-28 09:15:00'
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

  // 0. Comment endpoints
  if (pathname === '/api/comment/list' || pathname === '/api/interaction/comment/list') {
    res.writeHead(200)
    return res.end(JSON.stringify(mockComments))
  }

  if (pathname === '/api/comment/my') {
    res.writeHead(200)
    return res.end(JSON.stringify(mockComments.filter(c => c.userId === 7 || !c.userId)))
  }

  const commentDetailMatch = pathname.match(/^\/api\/comment\/detail\/(\d+)$/)
  if (commentDetailMatch) {
    const id = Number(commentDetailMatch[1])
    const c = mockComments.find(item => item.id === id) || mockComments[0]
    res.writeHead(200)
    return res.end(JSON.stringify(c))
  }

  if (pathname === '/api/comment/add') {
    let body = ''
    req.on('data', chunk => { body += chunk })
    req.on('end', () => {
      let data = {}
      try { data = JSON.parse(body) } catch(e) {}
      const newComment = {
        id: Math.max(...mockComments.map(c => c.id), 0) + 1,
        userId: Number(data.userId || 7),
        productId: Number(data.productId || 101),
        orderId: Number(data.orderId || 1),
        orderSn: 'EMALL20260902' + (Math.floor(Math.random() * 9000) + 1000),
        nickname: 'VIP星选体验官',
        productName: '自营严选商品',
        star: Number(data.star || 5),
        content: data.content || '',
        pics: data.pics || '',
        createTime: '2026-09-02 ' + new Date().toLocaleTimeString('zh-CN', { hour12: false })
      }
      mockComments.unshift(newComment)
      res.writeHead(200)
      res.end(JSON.stringify('评价发布成功'))
    })
    return
  }

  if (pathname === '/api/comment/update') {
    let body = ''
    req.on('data', chunk => { body += chunk })
    req.on('end', () => {
      let data = {}
      try { data = JSON.parse(body) } catch(e) {}
      const c = mockComments.find(item => item.id === Number(data.id))
      if (c) {
        if (data.star !== undefined) c.star = Number(data.star)
        if (data.content !== undefined) c.content = data.content
        if (data.pics !== undefined) c.pics = data.pics
      }
      res.writeHead(200)
      res.end(JSON.stringify('评价修改成功'))
    })
    return
  }

  const commentDeleteMatch = pathname.match(/^\/api\/comment\/(\d+)$/)
  if (req.method === 'DELETE' && commentDeleteMatch) {
    const id = Number(commentDeleteMatch[1])
    const idx = mockComments.findIndex(c => c.id === id)
    if (idx !== -1) mockComments.splice(idx, 1)
    res.writeHead(200)
    return res.end(JSON.stringify('评价已删除'))
  }

  if (pathname === '/api/file/upload') {
    res.writeHead(200)
    return res.end(JSON.stringify({
      code: 200,
      url: 'https://images.unsplash.com/photo-1592750475338-74b7b21085ab?auto=format&fit=crop&w=600&q=80',
      message: '买家秀图片上传成功'
    }))
  }

  // 1. Categories
  if (pathname === '/api/category/list') {
    res.writeHead(200)
    return res.end(JSON.stringify(mockCategories))
  }
  if (pathname === '/api/category/add') {
    let body = ''
    req.on('data', chunk => { body += chunk })
    req.on('end', () => {
      let data = {}
      try { data = JSON.parse(body) } catch(e) {}
      const newCat = {
        id: Math.max(...mockCategories.map(c => c.id), 60) + 1,
        name: data.name || '新分类',
        parentId: Number(data.parentId || 0),
        level: Number(data.level || 1),
        sort: Number(data.sort || 1)
      }
      mockCategories.push(newCat)
      res.writeHead(200)
      res.end(JSON.stringify('分类新增成功'))
    })
    return
  }
  if (pathname === '/api/category/update') {
    let body = ''
    req.on('data', chunk => { body += chunk })
    req.on('end', () => {
      let data = {}
      try { data = JSON.parse(body) } catch(e) {}
      const cat = mockCategories.find(c => c.id === Number(data.id))
      if (cat) {
        if (data.name !== undefined) cat.name = data.name
        if (data.parentId !== undefined) cat.parentId = Number(data.parentId)
        if (data.level !== undefined) cat.level = Number(data.level)
        if (data.sort !== undefined) cat.sort = Number(data.sort)
      }
      res.writeHead(200)
      res.end(JSON.stringify('分类更新成功'))
    })
    return
  }
  const catDeleteMatch = pathname.match(/^\/api\/category\/delete\/(\d+)$/)
  if (catDeleteMatch) {
    const id = Number(catDeleteMatch[1])
    const idx = mockCategories.findIndex(c => c.id === id)
    if (idx !== -1) mockCategories.splice(idx, 1)
    res.writeHead(200)
    return res.end(JSON.stringify('分类已删除'))
  }

  // 2. Ads
  if (pathname === '/api/ad/active' || pathname === '/api/ad/list') {
    res.writeHead(200)
    return res.end(JSON.stringify(mockAds.map((ad, idx) => ({
      sort: ad.sort || idx + 1,
      status: ad.status !== undefined ? ad.status : 1,
      ...ad
    }))))
  }
  if (pathname === '/api/ad/add') {
    let body = ''
    req.on('data', chunk => { body += chunk })
    req.on('end', () => {
      let data = {}
      try { data = JSON.parse(body) } catch(e) {}
      const newAd = {
        id: Math.max(...mockAds.map(a => a.id), 0) + 1,
        title: data.title || '新广告轮播图',
        picUrl: data.picUrl || 'https://images.unsplash.com/photo-1607082348824-0a96f2a4b9da?auto=format&fit=crop&w=1400&q=80',
        linkUrl: data.linkUrl || '/',
        sort: Number(data.sort || mockAds.length + 1),
        status: data.status !== undefined ? Number(data.status) : 1
      }
      mockAds.push(newAd)
      res.writeHead(200)
      res.end(JSON.stringify('广告添加成功'))
    })
    return
  }
  if (pathname === '/api/ad/update') {
    let body = ''
    req.on('data', chunk => { body += chunk })
    req.on('end', () => {
      let data = {}
      try { data = JSON.parse(body) } catch(e) {}
      const ad = mockAds.find(a => a.id === Number(data.id))
      if (ad) {
        if (data.title !== undefined) ad.title = data.title
        if (data.picUrl !== undefined) ad.picUrl = data.picUrl
        if (data.linkUrl !== undefined) ad.linkUrl = data.linkUrl
        if (data.sort !== undefined) ad.sort = Number(data.sort)
        if (data.status !== undefined) ad.status = Number(data.status)
      }
      res.writeHead(200)
      res.end(JSON.stringify('广告更新成功'))
    })
    return
  }
  const adDeleteMatch = pathname.match(/^\/api\/ad\/delete\/(\d+)$/)
  if (adDeleteMatch) {
    const id = Number(adDeleteMatch[1])
    const idx = mockAds.findIndex(a => a.id === id)
    if (idx !== -1) mockAds.splice(idx, 1)
    res.writeHead(200)
    return res.end(JSON.stringify('广告已删除'))
  }

  // 2.1 File Upload Common Mock
  if (pathname === '/api/file/uploadCommon') {
    res.writeHead(200)
    return res.end(JSON.stringify({
      code: 200,
      url: 'https://images.unsplash.com/photo-1607082348824-0a96f2a4b9da?auto=format&fit=crop&w=1400&q=80',
      message: '上传成功'
    }))
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

  // 4.1 Notices
  if (pathname === '/api/interaction/notice/active') {
    res.writeHead(200)
    return res.end(JSON.stringify(mockNotices.filter(n => n.isActive === 1)))
  }
  if (pathname === '/api/interaction/notice/add') {
    let body = ''
    req.on('data', chunk => { body += chunk })
    req.on('end', () => {
      let data = {}
      try { data = JSON.parse(body) } catch(e) {}
      const newNotice = {
        id: Math.max(...mockNotices.map(n => n.id), 0) + 1,
        title: data.title || '新公告通知',
        content: data.content || '',
        isActive: data.isActive !== undefined ? Number(data.isActive) : 1,
        createTime: '2026-09-02 ' + new Date().toLocaleTimeString('zh-CN', { hour12: false })
      }
      mockNotices.unshift(newNotice)
      res.writeHead(200)
      res.end(JSON.stringify('公告发布成功'))
    })
    return
  }
  if (pathname === '/api/interaction/notice/update') {
    let body = ''
    req.on('data', chunk => { body += chunk })
    req.on('end', () => {
      let data = {}
      try { data = JSON.parse(body) } catch(e) {}
      const n = mockNotices.find(item => item.id === Number(data.id))
      if (n) {
        if (data.title !== undefined) n.title = data.title
        if (data.content !== undefined) n.content = data.content
        if (data.isActive !== undefined) n.isActive = Number(data.isActive)
      }
      res.writeHead(200)
      res.end(JSON.stringify('公告更新成功'))
    })
    return
  }
  const noticeDeleteMatch = pathname.match(/^\/api\/interaction\/notice\/delete\/(\d+)$/)
  if (noticeDeleteMatch) {
    const id = Number(noticeDeleteMatch[1])
    const idx = mockNotices.findIndex(n => n.id === id)
    if (idx !== -1) mockNotices.splice(idx, 1)
    res.writeHead(200)
    return res.end(JSON.stringify('公告已删除'))
  }

  // 5. Product list
  if (pathname === '/api/product/list') {
    const categoryId = parsedUrl.query.categoryId
    const keyword = parsedUrl.query.keyword
    let list = [...mockProducts]
    if (categoryId) {
      const cid = Number(categoryId)
      const catObj = mockCategories.find(c => c.id === cid)
      const parentCid = catObj && catObj.parentId ? catObj.parentId : cid
      list = list.filter(p => p.categoryId === cid || p.categoryId === parentCid)
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

  // 9. User and Admin login
  if (pathname === '/api/user/adminLogin' || pathname === '/api/admin/login') {
    let body = ''
    req.on('data', chunk => { body += chunk })
    req.on('end', () => {
      let data = {}
      try { data = JSON.parse(body) } catch(e) {}
      const targetName = data.username || 'admin'
      const matched = mockUsers.find(u => u.username === targetName && u.role >= 1) || mockUsers[0]
      res.writeHead(200)
      res.end(JSON.stringify({
        token: 'emall-admin-jwt-token-' + matched.id,
        user: matched
      }))
    })
    return
  }

  if (pathname === '/api/user/login') {
    let body = ''
    req.on('data', chunk => { body += chunk })
    req.on('end', () => {
      let data = {}
      try { data = JSON.parse(body) } catch(e) {}
      const targetName = data.username || 'buyer'
      const matched = mockUsers.find(u => u.username === targetName) || {
        id: 7,
        username: targetName,
        nickname: targetName,
        role: 0,
        status: 1
      }
      res.writeHead(200)
      res.end(JSON.stringify({
        token: 'emall-user-jwt-token-' + matched.id,
        user: matched
      }))
    })
    return
  }

  // 9.1 User List (Admin)
  if (pathname === '/api/user/list') {
    res.writeHead(200)
    return res.end(JSON.stringify(mockUsers))
  }

  // 9.2 Add User / Admin
  if (pathname === '/api/user/add') {
    let body = ''
    req.on('data', chunk => { body += chunk })
    req.on('end', () => {
      let data = {}
      try { data = JSON.parse(body) } catch(e) {}
      const newUser = {
        id: mockUsers.length + 1,
        username: data.username || 'user_' + Date.now(),
        nickname: data.nickname || data.username,
        phone: data.phone || '13800000000',
        email: data.email || `${data.username}@emall.com`,
        avatar: data.avatar || '',
        role: data.role !== undefined ? Number(data.role) : 0,
        status: data.status !== undefined ? Number(data.status) : 1,
        createTime: '2026-09-02 ' + new Date().toLocaleTimeString('zh-CN', { hour12: false })
      }
      mockUsers.unshift(newUser)
      res.writeHead(200)
      res.end(JSON.stringify('账号创建成功'))
    })
    return
  }

  // 9.3 Update User / Status
  if (pathname === '/api/user/update') {
    let body = ''
    req.on('data', chunk => { body += chunk })
    req.on('end', () => {
      let data = {}
      try { data = JSON.parse(body) } catch(e) {}
      const u = mockUsers.find(item => item.id === Number(data.id))
      if (u) {
        if (data.nickname !== undefined) u.nickname = data.nickname
        if (data.phone !== undefined) u.phone = data.phone
        if (data.email !== undefined) u.email = data.email
        if (data.role !== undefined) u.role = Number(data.role)
        if (data.status !== undefined) u.status = Number(data.status)
      }
      res.writeHead(200)
      res.end(JSON.stringify('用户资料修改成功'))
    })
    return
  }

  // 9.4 Delete User
  const userDeleteMatch = pathname.match(/^\/api\/user\/(\d+)$/)
  if (req.method === 'DELETE' && userDeleteMatch) {
    const id = Number(userDeleteMatch[1])
    const idx = mockUsers.findIndex(u => u.id === id)
    if (idx !== -1) mockUsers.splice(idx, 1)
    res.writeHead(200)
    return res.end(JSON.stringify('账号已彻底删除'))
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

  // 11.0 Admin orders
  if (pathname === '/api/order/list') {
    res.writeHead(200)
    return res.end(JSON.stringify({
      records: mockOrders,
      total: mockOrders.length
    }))
  }

  // 11.1 Admin notices
  if (pathname === '/api/interaction/notice/list' || pathname === '/api/notice/list') {
    res.writeHead(200)
    return res.end(JSON.stringify(mockNotices))
  }

  // 11.2 Admin feedbacks
  if (pathname === '/api/interaction/feedback/list' || pathname === '/api/feedback/list') {
    res.writeHead(200)
    return res.end(JSON.stringify(mockFeedbacks))
  }
  if (pathname === '/api/interaction/feedback/reply') {
    let body = ''
    req.on('data', chunk => { body += chunk })
    req.on('end', () => {
      let data = {}
      try { data = JSON.parse(body) } catch(e) {}
      const fb = mockFeedbacks.find(item => item.id === Number(data.id))
      if (fb) {
        fb.reply = data.reply || ''
        fb.status = 1
      }
      res.writeHead(200)
      res.end(JSON.stringify('工单回复成功'))
    })
    return
  }

  // 11.3 Chat User List for Admin
  if (pathname === '/api/interaction/chat/user-list') {
    const userMap = new Map()
    // 按时间降序遍历获取每个买家的最新一条消息
    for (let i = mockChatMessages.length - 1; i >= 0; i--) {
      const msg = mockChatMessages[i]
      if (!userMap.has(msg.userId)) {
        userMap.set(msg.userId, msg)
      }
    }
    const list = Array.from(userMap.values())
    if (list.length === 0) {
      list.push({
        id: 1,
        userId: 7,
        content: '买家暂未发送新咨询',
        senderRole: 0,
        type: '在线沟通',
        createTime: '2026-09-02 14:00:00'
      })
    }
    res.writeHead(200)
    return res.end(JSON.stringify(list))
  }

  // 11.4 Chat history for specific user
  if (pathname === '/api/interaction/chat/history') {
    const targetUserId = parsedUrl.query.userId || 7
    const history = mockChatMessages.filter(m => String(m.userId) === String(targetUserId))
    res.writeHead(200)
    return res.end(JSON.stringify(history))
  }

  // 11.5 Chat message send (both buyer and admin)
  if (pathname === '/api/interaction/chat/send') {
    let body = ''
    req.on('data', chunk => { body += chunk })
    req.on('end', () => {
      let data = {}
      try { data = JSON.parse(body) } catch(e) {}
      const nowStr = new Date().toLocaleTimeString('zh-CN', { hour12: false })
      const newMsg = {
        id: mockChatMessages.length + 1,
        userId: Number(data.userId || 7),
        content: data.content || '',
        senderRole: data.senderRole !== undefined ? Number(data.senderRole) : 0,
        type: '在线沟通',
        createTime: '2026-09-02 ' + nowStr
      }
      mockChatMessages.push(newMsg)

      res.writeHead(200)
      res.end(JSON.stringify('发送成功'))
    })
    return
  }

  // 11.5 Address List
  if (pathname === '/api/address/list') {
    res.writeHead(200)
    return res.end(JSON.stringify([
      { id: 1, userId: 7, receiverName: '张三 (默认)', receiverPhone: '13800138000', province: '北京市', city: '北京市', district: '海淀区', detailAddress: '中关村南大街 1 号极客科技大厦 A 座 1801', isDefault: 1 },
      { id: 2, userId: 7, receiverName: '李四', receiverPhone: '13911112222', province: '上海市', city: '上海市', district: '浦东新区', detailAddress: '世纪大道 88 号环球金融中心 32 楼', isDefault: 0 }
    ]))
  }

  // 11.6 User Usable Coupons
  if (pathname === '/api/coupon/myUsable') {
    res.writeHead(200)
    return res.end(JSON.stringify([
      { userCouponId: 101, couponId: 1, name: '新人专享直降神券', minAmount: 100, discountAmount: 20, endTime: '2026-12-31' },
      { userCouponId: 102, couponId: 2, name: '满减大促特权神券', minAmount: 300, discountAmount: 50, endTime: '2026-12-31' }
    ]))
  }

  // 11.7 Order Create
  if (pathname === '/api/order/create') {
    let body = ''
    req.on('data', chunk => { body += chunk })
    req.on('end', () => {
      const newId = 8888 + Math.floor(Math.random() * 1000)
      res.writeHead(200)
      res.end(JSON.stringify({
        orderId: newId,
        orderSn: 'EMALL20260902' + newId,
        totalAmount: 1109.00
      }))
    })
    return
  }

  // 11.75 Favorite endpoints
  if (pathname === '/api/favorite/list') {
    res.writeHead(200)
    return res.end(JSON.stringify([mockProducts[0], mockProducts[1]]))
  }
  if (pathname === '/api/favorite/toggle') {
    res.writeHead(200)
    return res.end(JSON.stringify('uncollected'))
  }

  // 11.8 Order Status Update (Pay / Cancel)
  const statusMatch = pathname.match(/^\/api\/order\/status\/(\d+)\/(\d+)$/)
  if (statusMatch) {
    res.writeHead(200)
    return res.end(JSON.stringify({ code: 200, message: '状态更新成功' }))
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
