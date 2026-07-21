import { createApp } from 'vue'
import './style.css'
import App from './App.vue'
import 'element-plus/theme-chalk/base.css'
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'
import router from './router'
// ✨ 引入 Pinia
import { createPinia } from 'pinia'

const app = createApp(App)

app.use(createPinia()) // ✨ 挂载 Pinia
app.use(router)
app.mount('#app')
