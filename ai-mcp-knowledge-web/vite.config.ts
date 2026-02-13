import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src')
    }
  },
  css: {
    preprocessorOptions: {
      scss: {
        silenceDeprecations: ['legacy-js-api']
      }
    }
  },
  build: {
    chunkSizeWarningLimit: 900,
    rollupOptions: {
      output: {
        manualChunks(id) {
          if (!id.includes('node_modules')) {
            return
          }
          if (id.includes('element-plus')) {
            return 'vendor-element-plus'
          }
          if (id.includes('@element-plus/icons-vue')) {
            return 'vendor-element-icons'
          }
          if (id.includes('echarts')) {
            return 'vendor-echarts'
          }
          if (id.includes('markdown-it') || id.includes('highlight.js')) {
            return 'vendor-markdown'
          }
          if (id.includes('vue-router') || id.includes('pinia')) {
            return 'vendor-vue-ecosystem'
          }
          return 'vendor-misc'
        }
      }
    }
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8090',
        changeOrigin: true
      }
    }
  }
})
