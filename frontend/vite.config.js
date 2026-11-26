import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// 非默认端口：前端 5174，API 代理到后端 8081；strictPort 防止回退到 Vite 默认 5173
const frontendPort = Number(process.env.VITE_PORT || 5174)
const backendPort = Number(process.env.VITE_BACKEND_PORT || 8081)

export default defineConfig({
  plugins: [vue()],
  server: {
    port: frontendPort,
    strictPort: true,
    proxy: { '/api': { target: `http://localhost:${backendPort}`, changeOrigin: true } }
  }
})
