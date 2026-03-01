<template>
  <div class="response-extract-form">
    <el-text type="info" size="small">先写“给 AI 的字段名”，再写“从响应哪里取值”。不确定时可以先不配置。</el-text>

    <el-table :data="rules" class="gemini-table" style="width: 100%">
      <el-table-column label="给 AI 的字段名" min-width="180">
        <template #default="{ row }">
          <el-input v-model="row.fieldName" placeholder="如 resultTitle" />
        </template>
      </el-table-column>
      <el-table-column label="必填" width="110">
        <template #default="{ row }">
          <el-select v-model="row.isRequired" class="required-select">
            <el-option label="必填" :value="true" />
            <el-option label="可选" :value="false" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="响应取值路径" min-width="260">
        <template #default="{ row }">
          <el-input v-model="row.httpPath" placeholder="如 data.result / data.list[0].name" />
        </template>
      </el-table-column>
      <el-table-column label="输出类型" width="160">
        <template #default="{ row }">
          <el-select v-model="row.mcpType">
            <el-option label="文本(string)" value="string" />
            <el-option label="数字(number)" value="number" />
            <el-option label="布尔(boolean)" value="boolean" />
            <el-option label="对象(object)" value="object" />
            <el-option label="数组(array)" value="array" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="字段说明（给AI看，可选）" min-width="220">
        <template #default="{ row }">
          <el-input v-model="row.mcpDesc" placeholder="如 发送是否成功" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="100" align="center" header-align="center">
        <template #default="{ $index }">
          <el-button type="danger" text @click="removeRule($index)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="actions">
      <el-button type="primary" text @click="addRule">新增提取行</el-button>
    </div>

    <el-dialog
      v-model="importDialogVisible"
      title="导入响应 JSON"
      width="760px"
      class="gemini-dialog"
      append-to-body
    >
      <el-form label-width="90px">
        <el-form-item label="响应JSON">
          <el-input
            v-model="importJsonText"
            type="textarea"
            :rows="10"
            placeholder='粘贴接口响应 JSON，例如 {"success":true,"data":{"list":[{"name":"张三"}]}}'
          />
        </el-form-item>
      </el-form>

      <div class="import-ops">
        <el-button type="primary" text @click="parseImportJson">解析为树</el-button>
        <el-text type="info" size="small">数组路径默认使用 [*]（如 data.list[*].name）</el-text>
      </div>
      <el-text type="warning" size="small" class="import-tip">
        建议先点“解析为树”，再勾选字段，最后点“生成到表单”。
      </el-text>

      <div v-if="importTreeData.length > 0" class="tree-panel">
        <el-text type="info" size="small">勾选需要给 AI 的字段（可多选）</el-text>
        <el-tree
          ref="treeRef"
          :data="importTreeData"
          node-key="key"
          show-checkbox
          check-strictly
          default-expand-all
          class="import-tree"
        />
      </div>

      <template #footer>
        <el-button @click="importDialogVisible = false">取消</el-button>
        <el-button
          type="primary"
          class="gemini-btn-primary"
          :disabled="importTreeData.length === 0"
          @click="applyImportSelection"
        >
          生成到表单
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { ParamMappingNode } from '@/types/gateway'

const props = defineProps<{
  modelValue: ParamMappingNode[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: ParamMappingNode[]): void
}>()

const rules = computed(() => props.modelValue || [])
const treeRef = ref<any>(null)
const importDialogVisible = ref(false)
const importJsonText = ref('')
const importTreeData = ref<ImportTreeNode[]>([])
const importNodeMap = ref<Record<string, ImportTreeNode>>({})

type MappingType = ParamMappingNode['mcpType']

interface ImportTreeNode {
  key: string
  label: string
  path: string
  mcpType: MappingType
  children?: ImportTreeNode[]
}

const addRule = () => {
  emit('update:modelValue', [...rules.value, {
    fieldName: '',
    mcpType: 'string',
    httpPath: '',
    httpLocation: 'body',
    isRequired: false
  }])
}

const removeRule = (index: number) => {
  const next = [...rules.value]
  next.splice(index, 1)
  emit('update:modelValue', next)
}

const openImportDialog = () => {
  importDialogVisible.value = true
}
defineExpose({ openImportDialog })

const inferType = (value: any): MappingType => {
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

const buildNode = (name: string, value: any, path: string): ImportTreeNode => {
  const type = inferType(value)
  if (type === 'array') {
    const wildcardPath = path ? `${path}[*]` : '[*]'
    const arrayNode: ImportTreeNode = {
      key: wildcardPath,
      label: `${name} (array)`,
      path: wildcardPath,
      mcpType: 'array',
      children: []
    }
    const list = Array.isArray(value) ? value : []
    if (list.length > 0) {
      const sample = list[0]
      const sampleType = inferType(sample)
      if (sampleType === 'object') {
        const entries = Object.entries(sample as Record<string, any>)
        arrayNode.children = entries.map(([childName, childValue]) =>
          buildNode(childName, childValue, `${wildcardPath}.${childName}`)
        )
      } else if (sampleType === 'array') {
        arrayNode.children = [buildNode('[*]', sample, wildcardPath)]
      }
    }
    return arrayNode
  }

  if (type === 'object') {
    const objectNode: ImportTreeNode = {
      key: path,
      label: `${name} (object)`,
      path,
      mcpType: 'object',
      children: []
    }
    const entries = Object.entries((value || {}) as Record<string, any>)
    objectNode.children = entries.map(([childName, childValue]) =>
      buildNode(childName, childValue, path ? `${path}.${childName}` : childName)
    )
    return objectNode
  }

  return {
    key: path,
    label: `${name} (${type})`,
    path,
    mcpType: type
  }
}

const buildTree = (value: any): ImportTreeNode[] => {
  const type = inferType(value)
  if (type === 'object') {
    return Object.entries(value as Record<string, any>).map(([name, child]) => buildNode(name, child, name))
  }
  if (type === 'array') {
    return [buildNode('root', value, '')]
  }
  return [buildNode('value', value, 'value')]
}

const indexTree = (nodes: ImportTreeNode[]): Record<string, ImportTreeNode> => {
  const map: Record<string, ImportTreeNode> = {}
  const visit = (nodeList: ImportTreeNode[]) => {
    for (const node of nodeList) {
      map[node.key] = node
      if (node.children && node.children.length > 0) {
        visit(node.children)
      }
    }
  }
  visit(nodes)
  return map
}

const parseImportJson = () => {
  const source = String(importJsonText.value || '').trim()
  if (!source) {
    ElMessage.warning('请先粘贴响应 JSON')
    return
  }
  try {
    const parsed = JSON.parse(source)
    const tree = buildTree(parsed)
    if (!tree.length) {
      ElMessage.warning('JSON 解析后没有可用字段')
      return
    }
    importTreeData.value = tree
    importNodeMap.value = indexTree(tree)
    ElMessage.success('解析成功，请勾选需要给 AI 的字段')
  } catch (error: any) {
    ElMessage.error(error.message || 'JSON 解析失败，请检查格式')
  }
}

const guessFieldName = (path: string): string => {
  const parts = String(path || '').split('.')
  let last = parts[parts.length - 1] || 'value'
  last = last.replace(/\[\*\]/g, '').replace(/\[\d+\]/g, '')
  if (!last) {
    return 'items'
  }
  return last
}

const createUniqueFieldName = (base: string, used: Set<string>): string => {
  let next = base || 'value'
  let counter = 1
  while (used.has(next)) {
    next = `${base || 'value'}_${counter}`
    counter += 1
  }
  used.add(next)
  return next
}

const applyImportSelection = () => {
  if (!treeRef.value) {
    ElMessage.warning('请先解析 JSON')
    return
  }
  const selectedKeys = (treeRef.value.getCheckedKeys(false) as string[]) || []
  if (!selectedKeys.length) {
    ElMessage.warning('请至少勾选一个字段')
    return
  }

  const usedNames = new Set<string>()
  const nextRules: ParamMappingNode[] = selectedKeys
    .map((key) => importNodeMap.value[key])
    .filter((node): node is ImportTreeNode => Boolean(node && node.path))
    .map((node) => {
      const baseName = guessFieldName(node.path)
      return {
        fieldName: createUniqueFieldName(baseName, usedNames),
        mcpType: node.mcpType,
        httpPath: node.path,
        httpLocation: 'body',
        isRequired: false,
        mcpDesc: ''
      } as ParamMappingNode
    })

  emit('update:modelValue', nextRules)
  importDialogVisible.value = false
  ElMessage.success(`已生成 ${nextRules.length} 条响应提取规则`)
}
</script>

<style scoped>
.response-extract-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.actions {
  display: flex;
  justify-content: flex-end;
}

.import-ops {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 4px 0 8px 0;
}

.import-tip {
  display: block;
  margin-bottom: 8px;
}

.tree-panel {
  border: 1px solid var(--gemini-border);
  border-radius: 8px;
  padding: 12px;
  max-height: 320px;
  overflow: auto;
}

.import-tree {
  margin-top: 8px;
}

.required-select {
  width: 90px;
}
</style>
