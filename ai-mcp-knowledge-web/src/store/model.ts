import { defineStore } from 'pinia'
import { getModelList } from '@/api/model'
import type { ModelConfig } from '@/types/entity'

export const useModelStore = defineStore('model', {
  state: () => ({
    modelList: [] as ModelConfig[],
    loading: false
  }),
  actions: {
    async fetchModelList() {
      this.loading = true
      try {
        const res = await getModelList({ pageNum: 1, pageSize: 100 })
        this.modelList = res.data.data.list
      } catch (error) {
        console.error('Failed to fetch model list:', error)
      } finally {
        this.loading = false
      }
    }
  }
})
