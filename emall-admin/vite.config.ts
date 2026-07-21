import { fileURLToPath, URL } from 'node:url'
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    Components({
      resolvers: [ElementPlusResolver({ directives: true })],
      dts: false
    })
  ],
  resolve: {
    alias: {
      // 保持路径别名同步
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  },
  build: {
    rolldownOptions: {
      output: {
        codeSplitting: {
          groups: [
            {
              name: 'echarts',
              test: /node_modules[\\/](echarts|zrender)[\\/]/,
              maxSize: 1_000_000
            }
          ]
        }
      }
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
