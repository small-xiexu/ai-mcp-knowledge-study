<template>
  <el-dialog
    :model-value="visible"
    :title="isEdit ? '编辑 MCP Server' : '新增 MCP Server'"
    width="720px"
    class="gemini-dialog"
    align-center
    @close="handleClose"
  >
    <el-form
      ref="formRef"
      :model="formData"
      :rules="rules"
      label-width="130px"
    >
      <el-form-item
        label="名称"
        prop="serverName"
      >
        <el-input
          v-model="formData.serverName"
          placeholder="请输入 MCP Server 名称"
          class="gemini-input"
        />
      </el-form-item>

      <el-form-item
        label="类型"
        prop="serverType"
      >
        <el-select
          v-model="formData.serverType"
          placeholder="请选择类型"
          style="width: 100%"
          class="gemini-select"
          popper-class="gemini-select-dropdown"
        >
          <el-option
            label="STDIO"
            value="STDIO"
          />
          <el-option
            label="HTTP"
            value="HTTP"
          />
          <el-option
            label="SSE"
            value="SSE"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="启用状态">
        <el-switch
          v-model="formData.enabled"
          active-text="启用"
          inactive-text="禁用"
          style="--el-switch-on-color: var(--gemini-success);"
        />
      </el-form-item>

      <el-form-item label="描述">
        <el-input
          v-model="formData.description"
          type="textarea"
          :rows="2"
          placeholder="请输入描述（选填）"
          class="gemini-input"
        />
      </el-form-item>

      <template v-if="isStdio">
        <el-form-item
          label="JSON 配置"
          prop="commandJsonText"
        >
          <el-input
            v-model="formData.commandJsonText"
            type="textarea"
            :rows="5"
            placeholder="例如：{&quot;command&quot;:&quot;java&quot;,&quot;args&quot;:[&quot;-jar&quot;,&quot;/path/app.jar&quot;],&quot;env&quot;:{&quot;KEY&quot;:&quot;VALUE&quot;}}"
            class="gemini-input"
          />
          <div class="form-tip">
            <el-button
              type="primary"
              link
              @click="fillCommandJsonExample"
            >
              填入示例
            </el-button>
            <el-button
              type="primary"
              link
              @click="formatCommandJson"
            >
              格式化
            </el-button>
            <span>示例：{"command":"java","args":["-Dspring.ai.mcp.server.stdio=true","-jar","/path/mcp-server.jar"],"env":{"MCP_ENV":"dev"}}</span>
          </div>
        </el-form-item>
      </template>

      <template v-else>
        <el-form-item
          label="JSON 配置"
          prop="httpJsonText"
        >
          <el-input
            v-model="formData.httpJsonText"
            type="textarea"
            :rows="3"
            placeholder="例如：{&quot;endpoint&quot;:&quot;http://localhost:8080&quot;,&quot;sseEndpoint&quot;:&quot;/sse&quot;,&quot;headers&quot;:{&quot;Authorization&quot;:&quot;Bearer xxx&quot;}}"
            class="gemini-input"
          />
          <div class="form-tip">
            <el-button
              type="primary"
              link
              @click="fillHttpJsonExample"
            >
              填入示例
            </el-button>
            <el-button
              type="primary"
              link
              @click="formatHttpJson"
            >
              格式化
            </el-button>
            <el-button
              type="primary"
              link
              @click="validateHttpJson"
            >
              校验 JSON
            </el-button>
            <span>示例：{"endpoint":"http://localhost:8080","sseEndpoint":"/sse","headers":{"Authorization":"Bearer your-token"}}</span>
          </div>
        </el-form-item>
      </template>

      <el-form-item label="连接超时(ms)">
        <el-input-number
          v-model="formData.connectTimeoutMs"
          :min="1000"
          :step="1000"
          class="gemini-input-number"
        />
      </el-form-item>

      <el-form-item label="请求超时(ms)">
        <el-input-number
          v-model="formData.requestTimeoutMs"
          :min="1000"
          :step="1000"
          class="gemini-input-number"
        />
      </el-form-item>

      <el-form-item label="初始化超时(ms)">
        <el-input-number
          v-model="formData.initTimeoutMs"
          :min="1000"
          :step="1000"
          class="gemini-input-number"
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <div class="dialog-footer">
        <el-button @click="handleClose" text class="cancel-btn">
          取消
        </el-button>
        <el-button
          type="primary"
          :loading="loading"
          class="gemini-btn-primary"
          @click="handleSubmit"
        >
          确定
        </el-button>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
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
const isInitializing = ref(false)

const formData = reactive({
  id: 0,
  serverName: '',
  serverType: '',
  enabled: true,
  description: '',
  commandJsonText: '',
  httpJsonText: '',
  connectTimeoutMs: undefined as number | undefined,
  requestTimeoutMs: undefined as number | undefined,
  initTimeoutMs: undefined as number | undefined
})

const isStdio = computed(() => formData.serverType === 'STDIO')

const validateHttpJsonObject = (value: string) => {
  if (!value || !value.trim()) {
    return 'HTTP/SSE 模式需要填写 JSON 配置'
  }
  try {
    const parsed = JSON.parse(value)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      return 'JSON 配置需要是对象，例如：{"endpoint":"http://localhost:8080"}'
    }
    const endpointValue = parsed.endpoint || parsed.url
    if (!endpointValue || typeof endpointValue !== 'string') {
      return 'JSON 配置需要包含 endpoint 或 url 字段'
    }
    if (parsed.sseEndpoint !== undefined && typeof parsed.sseEndpoint !== 'string') {
      return 'sseEndpoint 需要是字符串，例如："/sse"'
    }
    if (parsed.headers !== undefined) {
      if (!parsed.headers || typeof parsed.headers !== 'object' || Array.isArray(parsed.headers)) {
        return 'headers 需要是 JSON 对象，例如：{"Authorization":"Bearer xxx"}'
      }
    }
    return true
  } catch (error) {
    return 'JSON 配置不是有效的 JSON'
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
  httpJsonText: [
    {
      validator: (_, value, callback) => {
        if (!isStdio.value) {
          const result = validateHttpJsonObject(value)
          if (result !== true) {
            callback(new Error(result as string))
            return
          }
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
  formData.httpJsonText = ''
  formData.connectTimeoutMs = undefined
  formData.requestTimeoutMs = undefined
  formData.initTimeoutMs = undefined
  formRef.value?.clearValidate()
}

watch(
  () => props.configData,
  (data) => {
    isInitializing.value = true
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
      formData.httpJsonText = !isStdio.value
        ? JSON.stringify(
          {
            endpoint: data.endpoint || '',
            sseEndpoint: data.sseEndpoint || undefined,
            headers: data.headers || undefined
          },
          null,
          2
        )
        : ''
      formData.connectTimeoutMs = data.connectTimeoutMs
      formData.requestTimeoutMs = data.requestTimeoutMs
      formData.initTimeoutMs = data.initTimeoutMs
    } else {
      isEdit.value = false
      resetForm()
    }

    nextTick(() => {
      isInitializing.value = false
    })
  },
  { immediate: true }
)

watch(
  () => formData.serverType,
  () => {
    if (isInitializing.value) {
      return
    }
    formData.commandJsonText = ''
    formData.httpJsonText = ''
    formRef.value?.clearValidate()
  }
)

const handleClose = () => {
  emit('update:visible', false)
  resetForm()
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

const formatCommandJson = () => {
  const formatted = tryFormatJson(formData.commandJsonText, 'JSON 配置')
  if (formatted) {
    formData.commandJsonText = formatted
  }
}

const fillHttpJsonExample = () => {
  formData.httpJsonText = JSON.stringify(
    {
      endpoint: 'http://localhost:8080',
      sseEndpoint: '/sse',
      headers: {
        Authorization: 'Bearer your-token'
      }
    },
    null,
    2
  )
}

const tryFormatJson = (text: string, label: string) => {
  if (!text || !text.trim()) {
    ElMessage.warning(`${label} 为空`)
    return null
  }
  try {
    const parsed = JSON.parse(text)
    if (!parsed || typeof parsed !== 'object' || Array.isArray(parsed)) {
      ElMessage.error(`${label} 必须是 JSON 对象`)
      return null
    }
    return JSON.stringify(parsed, null, 2)
  } catch (error) {
    ElMessage.error(`${label} 不是有效的 JSON`)
    return null
  }
}

const formatHttpJson = () => {
  const formatted = tryFormatJson(formData.httpJsonText, 'JSON 配置')
  if (formatted) {
    formData.httpJsonText = formatted
  }
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

const validateHttpJson = () => {
  validateJsonContent(formData.httpJsonText, 'JSON 配置')
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
    ? parsed.args.map((item: any) => String(item).trim()).filter((item: string) => item)
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

const parseHttpJson = (text: string) => {
  const result = validateHttpJsonObject(text)
  if (result !== true) {
    ElMessage.error(result as string)
    return null
  }
  const parsed = JSON.parse(text)
  return {
    endpoint: String(parsed.endpoint || parsed.url).trim(),
    sseEndpoint: parsed.sseEndpoint ? String(parsed.sseEndpoint).trim() : undefined,
    headers: parsed.headers && typeof parsed.headers === 'object' && !Array.isArray(parsed.headers)
      ? parsed.headers as Record<string, string>
      : undefined
  }
}

const handleSubmit = async () => {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    const stdioConfig = isStdio.value ? parseStdioJson(formData.commandJsonText) : null
    if (isStdio.value && stdioConfig === null) return
    const httpConfig = !isStdio.value ? parseHttpJson(formData.httpJsonText) : null
    if (!isStdio.value && httpConfig === null) return

    const payload: McpServerConfigRequest = {
      id: isEdit.value ? formData.id : undefined,
      serverName: formData.serverName,
      serverType: formData.serverType,
      enabled: formData.enabled,
      description: formData.description || undefined,
      command: stdioConfig ? stdioConfig.command : undefined,
      args: stdioConfig ? stdioConfig.args : undefined,
      env: stdioConfig ? stdioConfig.env : undefined,
      endpoint: httpConfig ? httpConfig.endpoint : undefined,
      sseEndpoint: httpConfig ? httpConfig.sseEndpoint : undefined,
      headers: httpConfig ? httpConfig.headers : undefined,
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
