<template>
  <el-dialog
    :model-value="visible"
    :title="isEdit ? '编辑 MCP Server' : '新增 MCP Server'"
    width="720px"
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="130px"
    >
      <el-form-item label="名称" prop="serverName">
        <el-input v-model="formData.serverName" placeholder="请输入 MCP Server 名称" />
      </el-form-item>

      <el-form-item label="类型" prop="serverType">
        <el-select v-model="formData.serverType" placeholder="请选择类型" style="width: 100%">
          <el-option label="STDIO" value="STDIO" />
          <el-option label="HTTP" value="HTTP" />
          <el-option label="SSE" value="SSE" />
        </el-select>
      </el-form-item>

      <el-form-item label="启用状态">
        <el-switch v-model="formData.enabled" active-text="启用" inactive-text="禁用" />
      </el-form-item>

      <el-form-item label="描述">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="2"
          placeholder="请输入描述（选填）"
        />
      </el-form-item>

      <template v-if="isStdio">
        <el-form-item label="命令" prop="command">
          <el-input v-model="formData.command" placeholder="例如：java" />
        </el-form-item>

        <el-form-item label="参数">
          <el-input
            v-model="formData.argsText"
            placeholder="用逗号分隔，例如：-jar,/path/app.jar"
          />
          <div class="form-tip">
            <el-button type="primary" link @click="fillArgsExample">填入示例</el-button>
            <el-button type="primary" link @click="copyArgsExample">复制示例</el-button>
            <span>示例：-jar,/path/mcp-server.jar,-Dspring.ai.mcp.server.stdio=true</span>
          </div>
        </el-form-item>

        <el-form-item label="环境变量" prop="envText">
          <el-input
            v-model="formData.envText"
            type="textarea"
            :rows="3"
            placeholder='JSON 对象，例如：{"KEY":"VALUE"}'
          />
          <div class="form-tip">
            <el-button type="primary" link @click="fillEnvExample">填入示例</el-button>
            <el-button type="primary" link @click="copyEnvExample">复制示例</el-button>
            <el-button type="primary" link @click="validateEnvJson">校验 JSON</el-button>
            <span>示例：{"MCP_ENV":"dev","LOG_LEVEL":"INFO"}</span>
          </div>
        </el-form-item>
      </template>

      <template v-else>
        <el-form-item label="服务地址" prop="endpoint">
          <el-input v-model="formData.endpoint" placeholder="例如：http://localhost:8080" />
        </el-form-item>

        <el-form-item v-if="isSse" label="SSE 路径" prop="sseEndpoint">
          <el-input v-model="formData.sseEndpoint" placeholder="例如：/sse" />
        </el-form-item>

        <el-form-item label="Header" prop="headersText">
          <el-input
            v-model="formData.headersText"
            type="textarea"
            :rows="3"
            placeholder='JSON 对象，例如：{"Authorization":"Bearer xxx"}'
          />
          <div class="form-tip">
            <el-button type="primary" link @click="fillHeadersExample">填入示例</el-button>
            <el-button type="primary" link @click="copyHeadersExample">复制示例</el-button>
            <el-button type="primary" link @click="validateHeadersJson">校验 JSON</el-button>
            <span>示例：{"Authorization":"Bearer your-token"}</span>
          </div>
        </el-form-item>
      </template>

      <el-form-item label="连接超时(ms)">
        <el-input-number v-model="formData.connectTimeoutMs" :min="1000" :step="1000" />
      </el-form-item>

      <el-form-item label="请求超时(ms)">
        <el-input-number v-model="formData.requestTimeoutMs" :min="1000" :step="1000" />
      </el-form-item>

      <el-form-item label="初始化超时(ms)">
        <el-input-number v-model="formData.initTimeoutMs" :min="1000" :step="1000" />
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
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createMcpServer, updateMcpServer } from '@/api/mcp'
import type { McpServerConfig, McpServerConfigRequest } from '@/types/entity'

interface Props {
  visible: boolean
  configData?: McpServerConfig | null
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
  serverName: '',
  serverType: '',
  enabled: true,
  description: '',
  command: '',
  argsText: '',
  envText: '',
  endpoint: '',
  sseEndpoint: '',
  headersText: '',
  connectTimeoutMs: undefined as number | undefined,
  requestTimeoutMs: undefined as number | undefined,
  initTimeoutMs: undefined as number | undefined
})

const isStdio = computed(() => formData.serverType === 'STDIO')
const isSse = computed(() => formData.serverType === 'SSE')

const validateJsonObject = (value: string, label: string) => {
  if (!value) return true
  try {
    const parsed = JSON.parse(value)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return `${label} 必须是 JSON 对象`
    }
    return true
  } catch (error) {
    return `${label} 不是有效的 JSON`
  }
}

const rules: FormRules = {
  serverName: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  serverType: [{ required: true, message: '请选择类型', trigger: 'change' }],
  command: [
    {
      validator: (_, value, callback) => {
        if (isStdio.value && (!value || !value.trim())) {
          callback(new Error('STDIO 模式需要填写命令'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  endpoint: [
    {
      validator: (_, value, callback) => {
        if (!isStdio.value && (!value || !value.trim())) {
          callback(new Error('远程模式需要填写服务地址'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  sseEndpoint: [
    {
      validator: (_, value, callback) => {
        if (isSse.value && (!value || !value.trim())) {
          callback(new Error('SSE 模式建议填写 SSE 路径'))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  envText: [
    {
      validator: (_, value, callback) => {
        const result = validateJsonObject(value, '环境变量')
        if (result !== true) {
          callback(new Error(result as string))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ],
  headersText: [
    {
      validator: (_, value, callback) => {
        const result = validateJsonObject(value, 'Header')
        if (result !== true) {
          callback(new Error(result as string))
          return
        }
        callback()
      },
      trigger: 'blur'
    }
  ]
}

watch(
  () => props.configData,
  (data) => {
    if (data) {
      isEdit.value = true
      formData.id = data.id
      formData.serverName = data.serverName
      formData.serverType = data.serverType
      formData.enabled = data.enabled
      formData.description = data.description || ''
      formData.command = data.command || ''
      formData.argsText = (data.args || []).join(',')
      formData.envText = data.env ? JSON.stringify(data.env, null, 2) : ''
      formData.endpoint = data.endpoint || ''
      formData.sseEndpoint = data.sseEndpoint || ''
      formData.headersText = data.headers ? JSON.stringify(data.headers, null, 2) : ''
      formData.connectTimeoutMs = data.connectTimeoutMs
      formData.requestTimeoutMs = data.requestTimeoutMs
      formData.initTimeoutMs = data.initTimeoutMs
    } else {
      isEdit.value = false
      resetForm()
    }
  },
  { immediate: true }
)

watch(
  () => formData.serverType,
  () => {
    formData.command = ''
    formData.argsText = ''
    formData.envText = ''
    formData.endpoint = ''
    formData.sseEndpoint = ''
    formData.headersText = ''
    formRef.value?.clearValidate()
  }
)

const resetForm = () => {
  formData.id = 0
  formData.serverName = ''
  formData.serverType = ''
  formData.enabled = true
  formData.description = ''
  formData.command = ''
  formData.argsText = ''
  formData.envText = ''
  formData.endpoint = ''
  formData.sseEndpoint = ''
  formData.headersText = ''
  formData.connectTimeoutMs = undefined
  formData.requestTimeoutMs = undefined
  formData.initTimeoutMs = undefined
  formRef.value?.clearValidate()
}

const handleClose = () => {
  emit('update:visible', false)
  resetForm()
}

const copyText = async (text: string) => {
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch (error) {
    ElMessage.error('复制失败，请手动复制')
  }
}

const fillArgsExample = () => {
  formData.argsText = '-jar,/path/mcp-server.jar,-Dspring.ai.mcp.server.stdio=true'
}

const copyArgsExample = () => {
  copyText('-jar,/path/mcp-server.jar,-Dspring.ai.mcp.server.stdio=true')
}

const fillEnvExample = () => {
  formData.envText = JSON.stringify(
    {
      MCP_ENV: 'dev',
      LOG_LEVEL: 'INFO'
    },
    null,
    2
  )
}

const copyEnvExample = () => {
  copyText('{"MCP_ENV":"dev","LOG_LEVEL":"INFO"}')
}

const fillHeadersExample = () => {
  formData.headersText = JSON.stringify(
    {
      Authorization: 'Bearer your-token'
    },
    null,
    2
  )
}

const copyHeadersExample = () => {
  copyText('{"Authorization":"Bearer your-token"}')
}

const validateJsonContent = (text: string, label: string) => {
  if (!text) {
    ElMessage.warning(`${label} 为空`)
    return
  }
  try {
    const parsed = JSON.parse(text)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      ElMessage.error(`${label} 必须是 JSON 对象`)
      return
    }
    ElMessage.success(`${label} 格式正确`)
  } catch (error) {
    ElMessage.error(`${label} 不是有效的 JSON`)
  }
}

const validateEnvJson = () => {
  validateJsonContent(formData.envText, '环境变量')
}

const validateHeadersJson = () => {
  validateJsonContent(formData.headersText, 'Header')
}

const parseArgs = () => {
  if (!formData.argsText) return undefined
  const args = formData.argsText
    .split(',')
    .map(item => item.trim())
    .filter(item => item)
  return args.length > 0 ? args : undefined
}

const parseJsonMap = (text: string, label: string) => {
  if (!text) return undefined
  try {
    const value = JSON.parse(text)
    if (value && typeof value === 'object' && !Array.isArray(value)) {
      return value as Record<string, string>
    }
    ElMessage.error(`${label} 需要是 JSON 对象，例如：{"KEY":"VALUE"}`)
    return null
  } catch (error) {
    ElMessage.error(`${label} 不是有效的 JSON，请检查格式`)
    return null
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    const env = parseJsonMap(formData.envText, '环境变量')
    if (env === null) return
    const headers = parseJsonMap(formData.headersText, 'Header')
    if (headers === null) return

    const payload: McpServerConfigRequest = {
      id: isEdit.value ? formData.id : undefined,
      serverName: formData.serverName,
      serverType: formData.serverType,
      enabled: formData.enabled,
      description: formData.description || undefined,
      command: formData.command || undefined,
      args: parseArgs(),
      env: env || undefined,
      endpoint: formData.endpoint || undefined,
      sseEndpoint: formData.sseEndpoint || undefined,
      headers: headers || undefined,
      connectTimeoutMs: formData.connectTimeoutMs,
      requestTimeoutMs: formData.requestTimeoutMs,
      initTimeoutMs: formData.initTimeoutMs
    }

    loading.value = true
    try {
      if (isEdit.value) {
        await updateMcpServer(payload)
        ElMessage.success('MCP Server 更新成功')
      } else {
        await createMcpServer(payload)
        ElMessage.success('MCP Server 创建成功')
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

<style scoped>
.form-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #909399;
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
