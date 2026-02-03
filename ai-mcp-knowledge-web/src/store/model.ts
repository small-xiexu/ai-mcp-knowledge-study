import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getModelList } from '@/api/model'
import type { ModelConfig } from '@/types/entity'

export const useModelStore = defineStore('model', () => {
  const modelList = ref<ModelConfig[]>([])
  const loading = ref(false)

  const fetchModelList = async () => {
    loading.value = true
    try {
      const res = await getModelList({ pageNum: 1, pageSize: 100 })
      modelList.value = res.data.records
    } catch (error) {
      console.error('Failed to fetch model list:', error)
    } finally {
      loading.value = false
    }
  }

  return {
    modelList,
    loading,
    fetchModelList
  }
})
