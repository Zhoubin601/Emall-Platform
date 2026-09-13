<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const openCustomerService = () => {
  window.dispatchEvent(new CustomEvent('open-customer-service'))
}
</script>

<template>
  <footer class="emall-footer">
    <!-- 1. 四大核心品质保障 -->
    <div class="footer-guarantee-bar">
      <div class="emall-footer-container guarantee-inner">
        <div class="guarantee-item">
          <div class="g-circle">品</div>
          <div class="g-text">
            <div class="g-title">品类齐全</div>
            <div class="g-sub">全球严选 · 畅选无忧</div>
          </div>
        </div>
        <div class="guarantee-item">
          <div class="g-circle">快</div>
          <div class="g-text">
            <div class="g-title">多仓直发</div>
            <div class="g-sub">顺丰速运 · 极速达</div>
          </div>
        </div>
        <div class="guarantee-item">
          <div class="g-circle">好</div>
          <div class="g-text">
            <div class="g-title">正品保障</div>
            <div class="g-sub">自营正品 · 假一赔十</div>
          </div>
        </div>
        <div class="guarantee-item">
          <div class="g-circle">省</div>
          <div class="g-text">
            <div class="g-title">天天特惠</div>
            <div class="g-sub">大额神券 · 惊喜秒杀</div>
          </div>
        </div>
      </div>
    </div>

    <!-- 2. 多列服务导航 -->
    <div class="emall-footer-container footer-links-zone">
      <div class="link-col">
        <div class="col-title">购物指南</div>
        <div class="col-link" @click="router.push('/')">购物流程</div>
        <div class="col-link" @click="router.push('/profile')">会员权益</div>
        <div class="col-link" @click="router.push('/orders')">订单查询</div>
        <div class="col-link" @click="openCustomerService()">常见问题</div>
      </div>
      <div class="link-col">
        <div class="col-title">配送方式</div>
        <div class="col-link">顺丰速运</div>
        <div class="col-link">就近仓配</div>
        <div class="col-link">配送服务查询</div>
        <div class="col-link">运费免减标准</div>
      </div>
      <div class="link-col">
        <div class="col-title">支付方式</div>
        <div class="col-link">微信支付</div>
        <div class="col-link">支付宝支付</div>
        <div class="col-link">大额对公转账</div>
        <div class="col-link">优惠券抵扣</div>
      </div>
      <div class="link-col">
        <div class="col-title">售后服务</div>
        <div class="col-link">7天无理由退货</div>
        <div class="col-link">价格保护规则</div>
        <div class="col-link" @click="router.push('/orders')">售后退款申请</div>
        <div class="col-link" @click="router.push('/comments')">我的评价晒单</div>
      </div>
      <div class="link-col contact-col">
        <div class="col-title">专属管家服务</div>
        <div class="phone-number">400-888-2026</div>
        <div class="service-time">周一至周日 09:00 - 22:00</div>
        <button class="online-kf-btn" @click="openCustomerService()">
          <span>💬</span> 在线人工客服
        </button>
      </div>
    </div>

    <!-- 3. 版权与合规备案 -->
    <div class="footer-copyright-bar">
      <div class="emall-footer-container copyright-inner">
        <div class="copyright-nav">
          <span @click="router.push('/')">关于我们</span>
          <span class="sep">|</span>
          <span @click="router.push('/')">联系我们</span>
          <span class="sep">|</span>
          <span @click="openCustomerService()">商家入驻</span>
          <span class="sep">|</span>
          <span @click="router.push('/')">隐私政策</span>
          <span class="sep">|</span>
          <span @click="router.push('/')">知识产权</span>
        </div>
        <div class="copyright-text">
          Copyright © 2026 E-MALL 全栈现代化电商平台 · 保留所有权利
        </div>
        <div class="copyright-sub">
          增值电信业务经营许可证：京B2-20260901 · 京公网安备 11010802026099号
        </div>
      </div>
    </div>
  </footer>
</template>

<style scoped>
.emall-footer {
  width: 100%;
  background-color: #f8fafc;
  border-top: 1px solid #e2e8f0;
  margin-top: 60px;
}

.emall-footer-container {
  width: 1220px;
  max-width: 96%;
  margin: 0 auto;
}

/* 四大保障条 */
.footer-guarantee-bar {
  border-bottom: 1px solid #e2e8f0;
  padding: 30px 0;
  background-color: #ffffff;
}

.guarantee-inner {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.guarantee-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.g-circle {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  border: 2px solid #0284c7;
  color: #0284c7;
  font-size: 22px;
  font-weight: 900;
  display: flex;
  justify-content: center;
  align-items: center;
  background-color: #f0f9ff;
}

.g-title {
  font-size: 16px;
  font-weight: bold;
  color: #0f172a;
}

.g-sub {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}

/* 多列导航 */
.footer-links-zone {
  display: flex;
  justify-content: space-between;
  padding: 40px 0;
}

.link-col {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.col-title {
  font-size: 14px;
  font-weight: bold;
  color: #1e293b;
  margin-bottom: 6px;
}

.col-link {
  font-size: 12px;
  color: #64748b;
  cursor: pointer;
  transition: color 0.2s;
}

.col-link:hover {
  color: #0284c7;
}

.contact-col {
  align-items: flex-start;
}

.phone-number {
  font-size: 20px;
  font-weight: 900;
  color: #0284c7;
}

.service-time {
  font-size: 11px;
  color: #94a3b8;
}

.online-kf-btn {
  margin-top: 6px;
  padding: 6px 16px;
  background: #f0f9ff;
  border: 1px solid #bae6fd;
  color: #0284c7;
  font-size: 12px;
  font-weight: bold;
  border-radius: 16px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  transition: all 0.2s;
}

.online-kf-btn:hover {
  background: #0284c7;
  color: #ffffff;
  border-color: #0284c7;
}

/* 版权栏 */
.footer-copyright-bar {
  background-color: #0f172a;
  color: #94a3b8;
  padding: 24px 0;
  text-align: center;
  font-size: 12px;
}

.copyright-nav {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
  color: #cbd5e1;
}

.copyright-nav span {
  cursor: pointer;
}

.copyright-nav span:hover {
  color: #ffffff;
}

.copyright-nav .sep {
  color: #475569;
}

.copyright-text {
  margin-bottom: 6px;
  color: #94a3b8;
}

.copyright-sub {
  color: #64748b;
  font-size: 11px;
}
</style>
