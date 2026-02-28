<template>
  <div class="mapping-tree">
    <el-text type="info" size="small" class="tree-hint">
      填写建议：先填接口字段名；对象/数组可继续点“添加子字段”。
    </el-text>

    <ParamMappingNode
      v-for="node in nodes"
      :key="getNodeKey(node)"
      :node="node"
      :level="0"
      :max-level="5"
      @remove="removeNode"
      @add-child="addChild"
    />

    <div class="tree-actions">
      <el-button type="primary" text @click="addRoot">新增参数行</el-button>
    </div>

    <el-dialog
      v-model="importDialogVisible"
      title="导入请求 JSON"
      width="760px"
      class="gemini-dialog"
      append-to-body
    >
      <el-form label-width="90px">
        <el-form-item label="请求JSON">
          <el-input
            v-model="importJsonText"
            type="textarea"
            :rows="10"
            placeholder='粘贴请求示例 JSON，例如 {"platform":"AI知识库","subject":"测试","traceId":"xxx"}'
          />
        </el-form-item>
      </el-form>

      <el-text type="info" size="small">
        规则：自动按 JSON 结构生成参数树，默认位置为 请求体(body)；对象会展开子字段；同时会把示例值写入“参数说明”。
      </el-text>

      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button type="primary" class="gemini-btn-primary" @click="applyImportJson">生成到表单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import ParamMappingNode from './ParamMappingNode.vue'
import type { ParamMappingNode as MappingNodeType } from '@/types/gateway'

const props = defineProps<{
  modelValue: MappingNodeType[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: MappingNodeType[]): void
}>()

const nodes = computed(() => props.modelValue || [])
const importDialogVisible = ref(false)
const importJsonText = ref('')

const keyMap = new WeakMap<object, string>()

const getNodeKey = (node: MappingNodeType): string => {
  const objectNode = node as object
  let key = keyMap.get(objectNode)
  if (!key) {
    key = `node-${Date.now()}-${Math.random()}`
    keyMap.set(objectNode, key)
  }
  return key
}

const createEmptyNode = (): MappingNodeType => ({
  fieldName: '',
  mcpType: 'string',
  isRequired: false,
  mcpDesc: '',
  httpPath: '',
  httpLocation: 'body',
  children: []
})

const addRoot = () => {
  emit('update:modelValue', [...nodes.value, createEmptyNode()])
}

const openImportDialog = () => {
  importDialogVisible.value = true
}
defineExpose({ openImportDialog })

const inferType = (value: any): MappingNodeType['mcpType'] => {
  if (Array.isArray(value)) {
    return 'array'
  }
  if (value !== null && typeof value === 'object') {
    return 'object'
  }
  if (typeof value === 'boolean') {
    return 'boolean'
  }
  if (typeof value === 'number') {
    return 'number'
  }
  return 'string'
}

const inferArrayItemType = (list: any[]): string | undefined => {
  if (!Array.isArray(list) || list.length === 0) {
    return 'string'
  }
  const sample = list[0]
  const type = inferType(sample)
  return type === 'object' ? 'object' : type
}

const stringifySampleValue = (value: any): string => {
  if (value === null || value === undefined) {
    return 'null'
  }
  if (typeof value === 'string') {
    return value.length > 60 ? `${value.slice(0, 57)}...` : value
  }
  if (typeof value === 'number' || typeof value === 'boolean') {
    return String(value)
  }
  try {
    const text = JSON.stringify(value)
    return text.length > 60 ? `${text.slice(0, 57)}...` : text
  } catch {
    return String(value)
  }
}

const buildNodesFromObject = (obj: Record<string, any>, parentPath = ''): MappingNodeType[] => {
  return Object.entries(obj).map(([name, value]) => {
    const nodeType = inferType(value)
    const path = parentPath ? `${parentPath}.${name}` : name
    const node: MappingNodeType = {
      fieldName: name,
      mcpType: nodeType,
      isRequired: false,
      mcpDesc: '',
      httpLocation: 'body',
      httpPath: nodeType === 'object' ? '' : path,
      children: []
    }

    if (nodeType === 'object') {
      const nested = value as Record<string, any>
      node.children = buildNodesFromObject(nested || {}, path)
      if (!node.children || node.children.length === 0) {
        node.httpPath = path
      }
    }

    if (nodeType === 'array') {
      node.itemType = inferArrayItemType(Array.isArray(value) ? value : [])
      node.httpPath = path
      node.children = []
    }

    if (nodeType === 'object') {
      node.mcpDesc = '对象参数'
    } else if (nodeType === 'array') {
      node.mcpDesc = stringifySampleValue(value)
    } else {
      node.mcpDesc = stringifySampleValue(value)
    }

    return node
  })
}

const applyImportJson = () => {
  const source = String(importJsonText.value || '').trim()
  if (!source) {
    ElMessage.warning('请先粘贴请求 JSON')
    return
  }
  try {
    const parsed = JSON.parse(source)
    if (parsed === null || Array.isArray(parsed) || typeof parsed !== 'object') {
      ElMessage.error('请求 JSON 顶层需要是对象，例如 {"platform":"xxx"}')
      return
    }
    const generated = buildNodesFromObject(parsed as Record<string, any>)
    emit('update:modelValue', generated)
    importDialogVisible.value = false
    ElMessage.success(`已生成 ${generated.length} 条请求参数`)
  } catch (error: any) {
    ElMessage.error(error.message || 'JSON 解析失败，请检查格式')
  }
}

const removeNodeFrom = (nodeList: MappingNodeType[], target: MappingNodeType): boolean => {
  const idx = nodeList.findIndex((item) => item === target)
  if (idx >= 0) {
    nodeList.splice(idx, 1)
    return true
  }
  for (const node of nodeList) {
    const children = node.children || []
    if (removeNodeFrom(children, target)) {
      return true
    }
  }
  return false
}

const removeNode = (target: MappingNodeType) => {
  const next = [...nodes.value]
  if (removeNodeFrom(next, target)) {
    emit('update:modelValue', next)
  }
}

const addChild = (target: MappingNodeType) => {
  const children = [...(target.children || [])]
  children.push(createEmptyNode())
  target.children = children
  emit('update:modelValue', [...nodes.value])
}
</script>

<style scoped>
.mapping-tree {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.tree-hint {
  line-height: 1.2;
}

.tree-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 6px;
  margin-bottom: 8px;
}
</style>
