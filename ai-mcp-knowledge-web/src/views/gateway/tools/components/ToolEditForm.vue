<template>
  <el-dialog
    :model-value="visible"
    :title="form.id ? '编辑工具' : '新增工具'"
    width="1080px"
    class="gemini-dialog"
    @close="$emit('update:visible', false)"
  >
    <el-form :model="form" label-width="110px">
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="工具名称">
            <el-input v-model="form.toolName" placeholder="请输入工具名称" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="HTTP 方法">
            <el-select v-model="form.httpMethod">
              <el-option label="GET" value="GET" />
              <el-option label="POST" value="POST" />
              <el-option label="PUT" value="PUT" />
              <el-option label="DELETE" value="DELETE" />
              <el-option label="PATCH" value="PATCH" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="工具描述">
        <el-input v-model="form.toolDescription" placeholder="请输入工具描述" />
      </el-form-item>
      <el-form-item label="HTTP URL">
        <el-input v-model="form.httpUrl" placeholder="https://example.com/api" />
      </el-form-item>
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="超时(ms)">
            <el-input-number v-model="form.timeout" :min="1000" :step="1000" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="重试次数">
            <el-input-number v-model="form.retryTimes" :min="0" :max="5" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="请求头 JSON">
        <el-input v-model="form.httpHeaders" type="textarea" :rows="3" placeholder='{"Authorization":"Bearer xxx"}' />
      </el-form-item>

      <div class="section-header">
        <span class="section-title">请求参数映射</span>
        <el-button type="primary" text @click="openRequestImport">导入JSON</el-button>
      </div>
      <el-divider />
      <el-alert
        type="info"
        :closable="false"
        show-icon
        description="按顺序填写：接口字段名 -> 必填 -> 参数类型 -> 放到请求的哪个位置。"
        style="margin-bottom: 12px"
      />
      <ParamMappingTree ref="requestMappingRef" v-model="requestMappingsValue" />

      <div class="section-header section-header-secondary">
        <span class="section-title">响应提取规则</span>
        <el-button type="primary" text @click="openResponseImport">导入JSON</el-button>
      </div>
      <el-divider />
      <el-alert
        type="info"
        :closable="false"
        show-icon
        description="可选配置：告诉系统从下游响应里取哪些字段给 AI。比如 httpPath= data.list[0].name。"
        style="margin-bottom: 12px"
      />
      <ResponseExtractForm ref="responseExtractRef" v-model="responseMappingsValue" />
    </el-form>

    <template #footer>
      <el-button @click="$emit('update:visible', false)">取消</el-button>
      <el-button type="primary" class="gemini-btn-primary" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { saveGatewayTool } from '@/api/gateway'
import type { SaveGatewayToolRequest } from '@/types/gateway'
import type { ParamMappingNode } from '@/types/gateway'
import ParamMappingTree from './ParamMappingTree.vue'
import ResponseExtractForm from './ResponseExtractForm.vue'

const props = defineProps<{
  visible: boolean
  gatewayId: string
  toolData?: any
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}>()

const requestMappingRef = ref<{ openImportDialog?: () => void } | null>(null)
const responseExtractRef = ref<{ openImportDialog?: () => void } | null>(null)

const form = reactive<SaveGatewayToolRequest>({
  gatewayId: '',
  toolName: '',
  toolDescription: '',
  httpUrl: '',
  httpMethod: 'POST',
  httpHeaders: '',
  timeout: 30000,
  retryTimes: 0,
  status: 1,
  requestMappings: [],
  responseMappings: []
})

const requestMappingsValue = computed({
  get: () => form.requestMappings || [],
  set: (value: ParamMappingNode[]) => {
    form.requestMappings = value
  }
})

const responseMappingsValue = computed({
  get: () => form.responseMappings || [],
  set: (value: ParamMappingNode[]) => {
    form.responseMappings = value
  }
})

watch(
  () => props.visible,
  (visible) => {
    if (!visible) {
      return
    }

    form.id = props.toolData?.tool?.id
    form.gatewayId = props.gatewayId
    form.toolName = props.toolData?.tool?.toolName || ''
    form.toolDescription = props.toolData?.tool?.toolDescription || ''
    form.httpUrl = props.toolData?.tool?.httpUrl || ''
    form.httpMethod = props.toolData?.tool?.httpMethod || 'POST'
    form.httpHeaders = props.toolData?.tool?.httpHeaders || ''
    form.timeout = props.toolData?.tool?.timeout || 30000
    form.retryTimes = props.toolData?.tool?.retryTimes || 0
    form.status = props.toolData?.tool?.status ?? 1
    form.requestMappings = props.toolData?.requestMappings || []
    form.responseMappings = props.toolData?.responseMappings || []
  }
)

const isBlank = (value: unknown): boolean => String(value ?? '').trim().length === 0

const validateRequestMappings = (nodes: ParamMappingNode[] | undefined, prefix = '请求参数'): string | null => {
  if (!nodes || nodes.length === 0) {
    return null
  }
  for (let i = 0; i < nodes.length; i += 1) {
    const node = nodes[i]
    const rowText = `${prefix}第${i + 1}行`
    if (isBlank(node.httpPath) && isBlank(node.fieldName)) {
      return `${rowText}：接口字段名不能为空`
    }
    if (isBlank(node.httpLocation)) {
      return `${rowText}：请先选择放到请求的哪个位置（body/query/header/path）`
    }
    const childError = validateRequestMappings(node.children, `${rowText}子字段`)
    if (childError) {
      return childError
    }
  }
  return null
}

const validateResponseMappings = (nodes: ParamMappingNode[] | undefined): string | null => {
  if (!nodes || nodes.length === 0) {
    return null
  }
  for (let i = 0; i < nodes.length; i += 1) {
    const node = nodes[i]
    const rowText = `响应提取第${i + 1}行`
    if (isBlank(node.fieldName)) {
      return `${rowText}：输出字段名不能为空`
    }
    if (isBlank(node.httpPath)) {
      return `${rowText}：响应取值路径不能为空`
    }
  }
  return null
}

const submit = async () => {
  try {
    const requestError = validateRequestMappings(form.requestMappings)
    if (requestError) {
      ElMessage.error(requestError)
      return
    }
    const responseError = validateResponseMappings(form.responseMappings)
    if (responseError) {
      ElMessage.error(responseError)
      return
    }

    await saveGatewayTool({
      ...form,
      gatewayId: props.gatewayId,
      requestMappings: form.requestMappings || [],
      responseMappings: form.responseMappings || []
    })
    ElMessage.success('保存成功')
    emit('success')
    emit('update:visible', false)
  } catch (error: any) {
    ElMessage.error(error.message || '保存失败')
  }
}

const openRequestImport = () => {
  requestMappingRef.value?.openImportDialog?.()
}

const openResponseImport = () => {
  responseExtractRef.value?.openImportDialog?.()
}
</script>

<style scoped>
.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 4px;
}

.section-header-secondary {
  margin-top: 14px;
}

.section-title {
  font-weight: 600;
  color: var(--gemini-text-primary);
}
</style>
