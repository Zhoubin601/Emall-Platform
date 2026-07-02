import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  server: {
    proxy: {
      // ✨ 关键：将所有 /api 请求转发到后端
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      // ✨ 关键：将所有 /uploads 请求转发到后端，用于回显图片
      '/uploads': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})