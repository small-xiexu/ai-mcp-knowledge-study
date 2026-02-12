<template>
  <el-dialog
    :model-value="visible"
    title="工具测试"
    width="920px"
    class="gemini-dialog"
    @close="$emit('update:visible', false)"
  >
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card class="gemini-card">
          <template #header>
            <div class="request-header">
              <span>请求参数</span>
              <el-button type="primary" text @click="formatArguments">格式化JSON</el-button>
            </div>
          </template>
          <el-input
            v-model="argumentsText"
            type="textarea"
            :rows="14"
            placeholder="系统已按参数映射自动生成模板，请补充真实值后执行测试"
          />
          <el-text type="info" size="small">已按请求参数映射自动生成模板，可直接修改后测试。</el-text>
          <el-button class="gemini-btn-primary" style="margin-top: 12px" @click="doDebug">执行测试</el-button>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card class="gemini-card">
          <template #header>响应预览</template>
          <el-alert v-if="result && !result.success" :title="result.errorCode || '调用失败'" type="error" :closable="false" />
          <pre class="preview">{{ resultText }}</pre>
        </el-card>
      </el-col>
    </el-row>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { debugGatewayTool } from '@/api/gateway'
import type { ParamMappingNode, ToolDebugResult } from '@/types/gateway'

const props = defineProps<{
  visible: boolean
  gatewayId: string
  toolName: string
  requestMappings?: ParamMappingNode[]
}>()

const argumentsText = ref('{}')
const result = ref<ToolDebugResult | null>(null)

const sortByOrder = (nodes: ParamMappingNode[] | undefined): ParamMappingNode[] => {
  const list = [...(nodes || [])]
  list.sort((a, b) => (a.sortOrder ?? Number.MAX_SAFE_INTEGER) - (b.sortOrder ?? Number.MAX_SAFE_INTEGER))
  return list
}

const normalizeType = (type: string | undefined): ParamMappingNode['mcpType'] => {
  const value = String(type || 'string').toLowerCase()
  if (value === 'number') return 'number'
  if (value === 'boolean') return 'boolean'
  if (value === 'object') return 'object'
  if (value === 'array') return 'array'
  return 'string'
}

const defaultByType = (type: string | undefined): any => {
  const normalized = normalizeType(type)
  if (normalized === 'number') return 0
  if (normalized === 'boolean') return false
  if (normalized === 'object') return {}
  if (normalized === 'array') return []
  return ''
}

const buildObjectFromChildren = (children: ParamMappingNode[] | undefined): Record<string, any> => {
  const resultMap: Record<string, any> = {}
  for (const child of sortByOrder(children)) {
    const name = String(child.fieldName || '').trim()
    if (!name) {
      continue
    }
    resultMap[name] = buildValueByNode(child)
  }
  return resultMap
}

const buildValueByNode = (node: ParamMappingNode): any => {
  const nodeType = normalizeType(node.mcpType)
  if (nodeType === 'object') {
    return buildObjectFromChildren(node.children)
  }
  if (nodeType === 'array') {
    const childObject = buildObjectFromChildren(node.children)
    if (Object.keys(childObject).length > 0) {
      return [childObject]
    }
    const itemType = node.itemType ? normalizeType(node.itemType) : null
    if (itemType === 'object') {
      return [{}]
    }
    if (itemType && itemType !== 'array') {
      return [defaultByType(itemType)]
    }
    return []
  }
  return defaultByType(nodeType)
}

const buildTreeFromFlatMappings = (mappings: ParamMappingNode[]): ParamMappingNode[] => {
  const hasParentRef = mappings.some((item) => item.parentId !== null && item.parentId !== undefined)
  if (!hasParentRef) {
    return mappings
  }

  const cloned = mappings.map((item) => ({
    ...item,
    children: []
  }))
  const idMap = new Map<number, ParamMappingNode>()
  for (const item of cloned) {
    if (item.id !== null && item.id !== undefined) {
      idMap.set(item.id, item)
    }
  }

  const roots: ParamMappingNode[] = []
  for (const item of cloned) {
    if (item.parentId !== null && item.parentId !== undefined && idMap.has(item.parentId)) {
      const parent = idMap.get(item.parentId)!
      parent.children = [...(parent.children || []), item]
    } else {
      roots.push(item)
    }
  }
  return roots
}

const buildArgumentsTemplate = (mappings: ParamMappingNode[] | undefined): Record<string, any> => {
  const source = Array.isArray(mappings) ? mappings : []
  if (source.length === 0) {
    return {}
  }

  const roots = sortByOrder(buildTreeFromFlatMappings(source))
  const onlyRoot = roots.length === 1 ? roots[0] : null
  const effectiveRoots =
    onlyRoot && String(onlyRoot.fieldName || '').trim().toLowerCase() === 'arguments' && (onlyRoot.children || []).length > 0
      ? onlyRoot.children || []
      : roots
  return buildObjectFromChildren(effectiveRoots)
}

watch(
  [() => props.visible, () => props.requestMappings],
  ([val]) => {
    if (val) {
      const template = buildArgumentsTemplate(props.requestMappings)
      argumentsText.value = JSON.stringify(template, null, 2)
      result.value = null
    }
  },
  { deep: true }
)

const resultText = computed(() => {
  if (!result.value) {
    return '等待执行...'
  }
  return result.value.content || ''
})

const doDebug = async () => {
  try {
    const parsed = JSON.parse(argumentsText.value || '{}')
    const res = await debugGatewayTool({
      gatewayId: props.gatewayId,
      toolName: props.toolName,
      arguments: parsed
    })
    result.value = res.data
  } catch (error: any) {
    ElMessage.error(error.message || '测试失败')
  }
}

const formatArguments = () => {
  try {
    const parsed = JSON.parse(argumentsText.value || '{}')
    argumentsText.value = JSON.stringify(parsed, null, 2)
  } catch {
    ElMessage.warning('当前请求参数不是合法 JSON，无法格式化')
  }
}
</script>

<style scoped>
.request-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.preview {
  min-height: 280px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 8px;
  color: var(--gemini-text-primary);
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
