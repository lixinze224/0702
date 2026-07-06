import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  server: {
    port: 3000,
    open: true,
    proxy: {
      '/api': {
        target: 'http://localhost:9999',
        changeOrigin: true
      },
      '/jenkinslog': {
        target: 'http://localhost:9999',
        changeOrigin: true
      },
      '/jenkins': {
        target: 'http://localhost:9999',
        changeOrigin: true
      }
    }
  }
})