import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import router from './router' // ✨ 引入路由
import { createPinia } from 'pinia' // ✨ 引入状态管理

const app = createApp(App)

app.use(createPinia()) // 挂载 Pinia
app.use(router)        // 挂载路由
app.use(ElementPlus)
app.mount('#app')