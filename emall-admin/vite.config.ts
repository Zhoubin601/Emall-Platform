import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// https://vite.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      // 保持路径别名同步
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    port: 5174, // 保持你当前的端口
    proxy: {
      // ✨ 关键同步 1：将 /api 开头的请求转给后端
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // ✨ 关键同步 2：将图片资源请求转给后端，解决图片加载失败问题
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})