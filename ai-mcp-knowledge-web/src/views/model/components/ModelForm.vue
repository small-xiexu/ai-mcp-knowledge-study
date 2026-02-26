<template>
  <el-dialog
    :model-value="visible"
    :title="isEdit ? '编辑模型' : '新增模型'"
    width="600px"
    class="gemini-dialog"
    :show-close="false"
    append-to-body
    align-center
    @close="handleClose"
  >
    <template #header>
      <div class="dialog-header">
        <span class="title">{{ isEdit ? '编辑模型' : '新增模型' }}</span>
        <el-button
          circle
          text
          class="close-btn"
          @click="handleClose"
        >
          <el-icon><Close /></el-icon>
        </el-button>
      </div>
    </template>

    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="100px"
      label-position="left"
      class="gemini-form"
    >
      <el-form-item
        label="模型名称"
        prop="modelName"
      >
        <el-input
          v-model="formData.modelName"
          placeholder="请输入模型名称"
          class="gemini-input"
        />
        <div class="form-hint">
          需与接口请求体里的 model 字段保持一致，例如 gpt-4o
        </div>
      </el-form-item>

      <el-form-item
        label="模型类型"
        prop="modelType"
      >
        <el-select
          v-model="formData.modelType"
          placeholder="请选择模型类型"
          style="width: 100%"
          class="gemini-select"
          popper-class="gemini-select-dropdown"
        >
          <el-option
            label="OpenAI"
            value="OPENAI"
          />
          <el-option
            label="Anthropic"
            value="ANTHROPIC"
          />
          <el-option
            label="Gemini"
            value="GEMINI"
          />
          <el-option
            label="Ollama"
            value="OLLAMA"
          />
          <el-option
            label="DeepSeek"
            value="DEEPSEEK"
          />
        </el-select>
      </el-form-item>

      <el-form-item
        label="API Key"
        prop="apiKey"
      >
        <el-input
          v-model="formData.apiKey"
          type="password"
          placeholder="请输入 API Key"
          show-password
          class="gemini-input"
        />
        <div class="form-hint">
          从模型服务商获取的密钥，将用于调用其接口
        </div>
      </el-form-item>

      <el-form-item
        label="Base URL"
        prop="baseUrl"
      >
        <el-input
          v-model="formData.baseUrl"
          placeholder="请输入 Base URL"
          class="gemini-input"
        />
        <div class="form-hint">
          填写完整接口域名，例如 https://apis.itedus.cn
        </div>
      </el-form-item>

      <el-form-item
        label="对话路径"
        prop="completionsPath"
      >
        <el-input
          v-model="formData.completionsPath"
          placeholder="可选，默认 /v1/chat/completions"
          class="gemini-input"
        />
      </el-form-item>

      <el-form-item
        label="嵌入路径"
        prop="embeddingsPath"
      >
        <el-input
          v-model="formData.embeddingsPath"
          placeholder="可选，默认 /v1/embeddings"
          class="gemini-input"
        />
      </el-form-item>

      <el-form-item label="工具调用" class="tool-enabled-item">
        <el-switch 
          v-model="formData.toolEnabled" 
          style="--el-switch-on-color: #8ab4f8; --el-switch-off-color: #5f6368"
        />
        <div class="form-hint inline">
          允许参与 Function Call
        </div>
      </el-form-item>

      <el-form-item
        label="历史预算"
        prop="maxPromptChars"
        class="history-form-item"
      >
        <el-input-number
          v-model="formData.maxPromptChars"
          :min="2000"
          :max="50000"
          :step="500"
          controls-position="right"
          class="gemini-input-number"
        />
        <div class="form-hint">
          当前模型单次 Prompt 可注入历史字符上限
        </div>
      </el-form-item>

      <el-form-item
        label="历史条数"
        prop="maxHistoryMessages"
        class="history-form-item"
      >
        <el-input-number
          v-model="formData.maxHistoryMessages"
          :min="1"
          :max="200"
          :step="1"
          controls-position="right"
          class="gemini-input-number"
        />
        <div class="form-hint">
          当前模型单次 Prompt 可注入历史消息条数上限
        </div>
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button 
          @click="handleClose"
          class="cancel-btn"
          text
        >
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="loading"
          class="submit-btn"
          @click="handleSubmit"
        >
          确定
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createModel, updateModel } from '@/api/model'
import type { ModelConfig, ModelConfigRequest } from '@/types/entity'
import { Close } from '@element-plus/icons-vue'

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
  completionsPath: '',
  embeddingsPath: '',
  enabled: true,
  toolEnabled: true,
  maxPromptChars: 12000,
  maxHistoryMessages: 20
})

const rules: FormRules = {
  modelName: [{ required: true, message: '请输入模型名称', trigger: 'blur' }],
  modelType: [{ required: true, message: '请选择模型类型', trigger: 'change' }],
  apiKey: [{ required: true, message: '请输入 API Key', trigger: 'blur' }],
  baseUrl: [{ required: true, message: '请输入 Base URL', trigger: 'blur' }],
  maxPromptChars: [{ type: 'number', min: 2000, max: 50000, message: '请输入 2000-50000 之间的值', trigger: 'change' }],
  maxHistoryMessages: [{ type: 'number', min: 1, max: 200, message: '请输入 1-200 之间的值', trigger: 'change' }]
}

const resetForm = () => {
  formData.id = 0
  formData.modelName = ''
  formData.modelType = ''
  formData.apiKey = ''
  formData.baseUrl = ''
  formData.completionsPath = ''
  formData.embeddingsPath = ''
  formData.enabled = true
  formData.toolEnabled = true
  formData.maxPromptChars = 12000
  formData.maxHistoryMessages = 20
  formRef.value?.clearValidate()
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
      formData.apiKey = data.apiKey || ''
      formData.baseUrl = data.baseUrl || ''
      formData.completionsPath = data.completionsPath || ''
      formData.embeddingsPath = data.embeddingsPath || ''
      formData.enabled = data.enabled
      formData.toolEnabled = data.toolEnabled !== false
      formData.maxPromptChars = data.maxPromptChars ?? 12000
      formData.maxHistoryMessages = data.maxHistoryMessages ?? 20
    } else {
      isEdit.value = false
      resetForm()
    }
  },
  { immediate: true }
)

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
      const data: ModelConfigRequest = {
        id: isEdit.value ? formData.id : undefined,
        modelName: formData.modelName,
        modelType: formData.modelType,
        apiKey: formData.apiKey,
        baseUrl: formData.baseUrl,
        completionsPath: formData.completionsPath || undefined,
        embeddingsPath: formData.embeddingsPath || undefined,
        enabled: formData.enabled,
        toolEnabled: formData.toolEnabled,
        maxPromptChars: formData.maxPromptChars,
        maxHistoryMessages: formData.maxHistoryMessages
      }

      if (isEdit.value) {
        await updateModel(data)
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

<style scoped lang="scss">
.dialog-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 0px;
  
  .title {
    font-size: 18px;
    font-weight: 500;
    color: #e8eaed;
  }
  
  .close-btn {
    color: #9aa0a6;
    &:hover {
      color: #e8eaed;
      background-color: rgba(255, 255, 255, 0.1);
    }
  }
}

.gemini-form {
  margin-top: 10px;
}

.tool-enabled-item {
  margin-top: 4px;
  margin-bottom: 8px;
}

.history-form-item {
  margin-bottom: 14px;
}

.history-form-item :deep(.el-form-item__content) {
  display: block;
}

.form-hint {
  margin-top: 4px;
  color: #9aa0a6;
  font-size: 12px;
  line-height: 1.4;
  
  &.inline {
    display: inline-block;
    margin-left: 8px;
    margin-top: 0;
    vertical-align: middle;
  }
}

.divider-text {
  margin: 20px 0 16px;
  font-size: 14px;
  color: #9aa0a6;
  border-bottom: 1px solid #3c4043;
  padding-bottom: 8px;
}

:deep(.el-form-item__label) {
  color: #9aa0a6 !important;
}

/* 输入框统一样式 */
.gemini-input,
.gemini-select {
  :deep(.el-input__wrapper) {
    background-color: #202124 !important;
    box-shadow: 0 0 0 1px #3c4043 inset !important;
    
    &.is-focus {
      box-shadow: 0 0 0 2px #8ab4f8 inset !important;
    }
  }
  
  :deep(.el-input__inner) {
    color: #e8eaed !important;
    &::placeholder {
      color: #5f6368;
    }
  }
}

.gemini-input-number {
  width: 100%;
  :deep(.el-input__wrapper) {
    background-color: #202124 !important;
    box-shadow: 0 0 0 1px #3c4043 inset !important;
  }
  :deep(.el-input__inner) {
    color: #e8eaed !important;
    text-align: left !important;
  }
  :deep(.el-input-number__decrease),
  :deep(.el-input-number__increase) {
    background-color: #303134 !important;
    color: #9aa0a6 !important;
    border-color: #3c4043 !important;
    
    &:hover {
      color: #8ab4f8 !important;
    }
  }
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  
  .cancel-btn {
    color: #8ab4f8;
    &:hover {
      color: #aecbfa;
      background: rgba(138, 180, 248, 0.08);
    }
  }
  
  .submit-btn {
    background: #8ab4f8;
    border: none;
    border-radius: 18px;
    padding: 8px 24px;
    color: #202124;
    font-weight: 500;
    
    &:hover {
      background: #aecbfa;
    }
  }
}
</style>

<style lang="scss">
/* Global styles for Dialog popups to support glassmorphism override */
.gemini-dialog {
  background: rgba(32, 33, 36, 0.95) !important;
  border: 1px solid #3c4043 !important;
  border-radius: 16px !important;
  box-shadow: 0 24px 38px 3px rgba(0, 0, 0, 0.14), 
              0 9px 46px 8px rgba(0, 0, 0, 0.12), 
              0 11px 15px -7px rgba(0, 0, 0, 0.2) !important;
  backdrop-filter: blur(20px);
  
  .el-dialog__header {
    margin: 0;
    padding: 20px 24px 10px !important;
  }
  
  .el-dialog__body {
    padding: 10px 24px 20px !important;
  }
  
  .el-dialog__footer {
    padding: 10px 24px 24px !important;
    border-top: none !important;
  }
}

.gemini-select-dropdown {
  background-color: #202124 !important;
  border: 1px solid #3c4043 !important;
  
  .el-select-dropdown__item {
    color: #e8eaed !important;
    &.hover, &:hover {
      background-color: rgba(138, 180, 248, 0.1) !important;
    }
    &.selected {
      color: #8ab4f8 !important;
      font-weight: 500;
      background-color: transparent !important;
    }
  }
}
</style>
