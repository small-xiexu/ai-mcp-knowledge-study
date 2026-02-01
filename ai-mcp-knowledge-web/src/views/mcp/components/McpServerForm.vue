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
        <el-form-item label="JSON 配置" prop="commandJsonText">
          <el-input
            v-model="formData.commandJsonText"
            type="textarea"
            :rows="5"
            placeholder='例如：{"command":"java","args":["-jar","/path/app.jar"],"env":{"KEY":"VALUE"}}'
          />
          <div class="form-tip">
            <el-button type="primary" link @click="fillCommandJsonExample">填入示例</el-button>
            <el-button type="primary" link @click="copyCommandJsonExample">复制示例</el-button>
            <span>示例：{"command":"java","args":["-Dspring.ai.mcp.server.stdio=true","-jar","/path/mcp-server.jar"],"env":{"MCP_ENV":"dev"}}</span>
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
  commandJsonText: '',
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
  commandJsonText: [
    {
      validator: (_, value, callback) => {
        if (isStdio.value) {
          const result = validateStdioJson(value)
          if (result !== true) {
            callback(new Error(result as string))
            return
          }
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

const resetForm = () => {
  formData.id = 0
  formData.serverName = ''
  formData.serverType = ''
  formData.enabled = true
  formData.description = ''
  formData.commandJsonText = ''
  formData.endpoint = ''
  formData.sseEndpoint = ''
  formData.headersText = ''
  formData.connectTimeoutMs = undefined
  formData.requestTimeoutMs = undefined
  formData.initTimeoutMs = undefined
  formRef.value?.clearValidate()
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
      formData.commandJsonText = isStdio.value
        ? JSON.stringify(
          {
            command: data.command || '',
            args: data.args || [],
            env: data.env || undefined
          },
          null,
          2
        )
        : ''
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
    formData.commandJsonText = ''
    formData.endpoint = ''
    formData.sseEndpoint = ''
    formData.headersText = ''
    formRef.value?.clearValidate()
  }
)

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

const fillCommandJsonExample = () => {
  formData.commandJsonText = JSON.stringify(
    {
      command: 'java',
      args: [
        '-Dspring.ai.mcp.server.stdio=true',
        '-jar',
        '/path/mcp-server.jar'
      ],
      env: {
        MCP_ENV: 'dev',
        LOG_LEVEL: 'INFO'
      }
    },
    null,
    2
  )
}

const copyCommandJsonExample = () => {
  copyText('{"command":"java","args":["-Dspring.ai.mcp.server.stdio=true","-jar","/path/mcp-server.jar"],"env":{"MCP_ENV":"dev","LOG_LEVEL":"INFO"}}')
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

const validateHeadersJson = () => {
  validateJsonContent(formData.headersText, 'Header')
}

const validateStdioJson = (value: string) => {
  if (!value || !value.trim()) {
    return 'STDIO 模式需要填写 JSON 配置'
  }
  try {
    const parsed = JSON.parse(value)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return 'JSON 配置需要是对象，例如：{"command":"java","args":["-jar","/path/app.jar"]}'
    }
    if (!parsed.command || typeof parsed.command !== 'string') {
      return 'JSON 配置需要包含 command 字段'
    }
    if (parsed.args !== undefined && !Array.isArray(parsed.args)) {
      return 'args 需要是数组，例如：["-jar","/path/app.jar"]'
    }
    if (parsed.env !== undefined) {
      if (!parsed.env || typeof parsed.env !== 'object' || Array.isArray(parsed.env)) {
        return 'env 需要是 JSON 对象，例如：{"KEY":"VALUE"}'
      }
    }
    return true
  } catch (error) {
    return 'JSON 配置不是有效的 JSON'
  }
}

const parseStdioJson = (value: string) => {
  const result = validateStdioJson(value)
  if (result !== true) {
    ElMessage.error(result as string)
    return null
  }
  const parsed = JSON.parse(value)
  const args = Array.isArray(parsed.args)
    ? parsed.args.map(item => String(item).trim()).filter(item => item)
    : undefined
  const env = parsed.env && typeof parsed.env === 'object' && !Array.isArray(parsed.env)
    ? parsed.env as Record<string, string>
    : undefined
  return {
    command: String(parsed.command).trim(),
    args: args && args.length > 0 ? args : undefined,
    env: env || undefined
  }
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

    const stdioConfig = isStdio.value ? parseStdioJson(formData.commandJsonText) : null
    if (isStdio.value && stdioConfig === null) return
    const headers = parseJsonMap(formData.headersText, 'Header')
    if (headers === null) return

    const payload: McpServerConfigRequest = {
      id: isEdit.value ? formData.id : undefined,
      serverName: formData.serverName,
      serverType: formData.serverType,
      enabled: formData.enabled,
      description: formData.description || undefined,
      command: stdioConfig ? stdioConfig.command : undefined,
      args: stdioConfig ? stdioConfig.args : undefined,
      env: stdioConfig ? stdioConfig.env : undefined,
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
