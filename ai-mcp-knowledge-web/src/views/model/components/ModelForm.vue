<template>
  <el-dialog
    :model-value="visible"
    :title="isEdit ? '编辑模型' : '新增模型'"
    width="600px"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="120px"
    >
      <el-form-item label="模型名称" prop="modelName">
        <el-input v-model="formData.modelName" placeholder="请输入模型名称" />
      </el-form-item>

      <el-form-item label="模型类型" prop="modelType">
        <el-select v-model="formData.modelType" placeholder="请选择模型类型" style="width: 100%">
          <el-option label="OpenAI" value="OPENAI" />
          <el-option label="Anthropic" value="ANTHROPIC" />
          <el-option label="Gemini" value="GEMINI" />
        </el-select>
      </el-form-item>

      <el-form-item label="API Key" prop="apiKey">
        <el-input
          v-model="formData.apiKey"
          type="password"
          placeholder="请输入 API Key"
          show-password
        />
      </el-form-item>

      <el-form-item label="Base URL" prop="baseUrl">
        <el-input v-model="formData.baseUrl" placeholder="请输入 Base URL（选填）" />
      </el-form-item>

      <el-form-item label="优先级" prop="priority">
        <el-input-number v-model="formData.priority" :min="0" :max="100" />
      </el-form-item>

      <el-divider content-position="left">模型能力配置</el-divider>

      <el-form-item label="Max Tokens">
        <el-input-number
          v-model="formData.capability.maxTokens"
          :min="0"
          :step="1000"
          style="width: 100%"
        />
      </el-form-item>

      <el-form-item label="Quality Score">
        <el-input-number
          v-model="formData.capability.qualityScore"
          :min="0"
          :max="100"
          style="width: 100%"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="handleClose">取消</el-button>
      <el-button type="primary" :loading="loading" @click="handleSubmit">
        确定
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createModel, updateModel } from '@/api/model'
import type { ModelConfig } from '@/types/entity'

interface Props {
  visible: boolean
  modelData?: ModelConfig | null
}

interface Emits {
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}

const props = defineProps<Props>()
const emit = defineEmits<Emits>()

const formRef = ref<FormInstance>()
const loading = ref(false)

const isEdit = ref(false)

const formData = reactive({
  id: 0,
  modelName: '',
  modelType: '',
  apiKey: '',
  baseUrl: '',
  priority: 0,
  capability: {
    maxTokens: 4096,
    qualityScore: 80
  }
})

const rules: FormRules = {
  modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  modelType: [{ required: true, message: '请选择模型类型', trigger: 'change' }],
  apiKey: [{ required: true, message: '请输入 API Key', trigger: 'blur' }]
}

// 监听 modelData 变化，填充表单
watch(
  () => props.modelData,
  (data) => {
    if (data) {
      isEdit.value = true
      formData.id = data.id
      formData.modelName = data.modelName
      formData.modelType = data.modelType
      formData.apiKey = ''  // 不回显 API Key
      formData.baseUrl = data.baseUrl || ''
      formData.priority = data.priority
      if (data.capability) {
        formData.capability.maxTokens = data.capability.maxInputTokens || 4096
        formData.capability.qualityScore = data.capability.qualityScore || 80
      }
    } else {
      isEdit.value = false
      resetForm()
    }
  },
  { immediate: true }
)

const resetForm = () => {
  formData.id = 0
  formData.modelName = ''
  formData.modelType = ''
  formData.apiKey = ''
  formData.baseUrl = ''
  formData.priority = 0
  formData.capability.maxTokens = 4096
  formData.capability.qualityScore = 80
  formRef.value?.clearValidate()
}

const handleClose = () => {
  emit('update:visible', false)
  resetForm()
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    loading.value = true
    try {
      const data: Partial<ModelConfig> = {
        modelName: formData.modelName,
        modelType: formData.modelType,
        apiKey: formData.apiKey,
        baseUrl: formData.baseUrl,
        priority: formData.priority,
        capability: {
          maxInputTokens: formData.capability.maxTokens,
          maxOutputTokens: formData.capability.maxTokens,
          supportFunctionCalling: false,
          supportVision: false,
          supportStreaming: false,
          qualityScore: formData.capability.qualityScore
        }
      }

      if (isEdit.value) {
        await updateModel(formData.id, data)
        ElMessage.success('模型更新成功')
      } else {
        await createModel(data)
        ElMessage.success('模型创建成功')
      }

      emit('success')
      handleClose()
    } catch (error: any) {
      ElMessage.error(error.message || '操作失败')
    } finally {
      loading.value = false
    }
  })
}
</script>
