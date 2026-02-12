<template>
  <el-dialog
    :model-value="visible"
    title="工具-模型绑定"
    width="900px"
    class="gemini-dialog"
    @close="$emit('update:visible', false)"
  >
    <el-form label-width="90px">
      <el-form-item label="目标模型">
        <el-select v-model="selectedModelId" placeholder="请选择模型" style="width: 320px" @change="loadBindings">
          <el-option v-for="model in models" :key="model.id" :label="`${model.modelName} (${model.modelType})`" :value="model.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="工具选择">
        <el-transfer
          v-model="selectedToolIds"
          :data="transferData"
          filterable
          :titles="['可选工具', '已绑定工具']"
          style="width: 100%"
        />
      </el-form-item>
      <el-alert
        v-if="selectedToolIds.length === 0"
        title="当前未配置绑定，模型默认全局可见所有已发布工具"
        type="info"
        :closable="false"
      />
    </el-form>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button class="gemini-btn-primary" type="primary" @click="saveBindings">保存绑定</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  listEnabledGatewayTools,
  listEnabledModels,
  getModelToolBindings,
  saveModelToolBindings
} from '@/api/gateway'
import type { ModelOption, ToolOption } from '@/types/gateway'

const props = defineProps<{
  visible: boolean
  modelId?: number
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}>()

const models = ref<ModelOption[]>([])
const tools = ref<ToolOption[]>([])
const selectedModelId = ref<number>()
const selectedToolIds = ref<number[]>([])

const transferData = computed(() => {
  return tools.value.map((tool) => ({
    key: tool.id,
    label: `${tool.toolName} [${tool.gatewayId}]`,
    disabled: false
  }))
})

const loadBaseData = async () => {
  const [modelRes, toolRes] = await Promise.all([listEnabledModels(), listEnabledGatewayTools()])
  models.value = modelRes.data || []
  tools.value = toolRes.data || []

  if (props.modelId) {
    selectedModelId.value = props.modelId
  } else if (models.value.length > 0) {
    selectedModelId.value = models.value[0].id
  }

  await loadBindings()
}

const loadBindings = async () => {
  if (!selectedModelId.value) {
    selectedToolIds.value = []
    return
  }
  const res = await getModelToolBindings(selectedModelId.value)
  selectedToolIds.value = res.data.toolIds || []
}

const saveBindings = async () => {
  if (!selectedModelId.value) {
    ElMessage.warning('请先选择模型')
    return
  }
  await saveModelToolBindings(selectedModelId.value, selectedToolIds.value)
  ElMessage.success('绑定保存成功')
  emit('success')
  emit('update:visible', false)
}

watch(
  () => props.visible,
  (visible) => {
    if (visible) {
      loadBaseData().catch((error: any) => ElMessage.error(error.message || '加载绑定数据失败'))
    }
  }
)
</script>
