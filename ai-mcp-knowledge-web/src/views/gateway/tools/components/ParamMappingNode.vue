<template>
  <div class="mapping-node" :style="{ marginLeft: `${level * 16}px` }">
    <div class="mapping-row">
      <el-input v-model="apiFieldName" placeholder="接口字段名，如 user_id / title" class="cell-input" />
      <el-select v-model="node.isRequired" class="cell-input required-select">
        <el-option label="必填" :value="true" />
        <el-option label="可选" :value="false" />
      </el-select>
      <el-select v-model="node.mcpType" class="cell-input short">
        <el-option
          v-for="item in typeOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
      <el-select v-model="node.httpLocation" class="cell-input short">
        <el-option
          v-for="item in locationOptions"
          :key="item.value"
          :label="item.label"
          :value="item.value"
        />
      </el-select>
      <el-input v-model="node.mcpDesc" placeholder="参数说明（给 AI 看）" class="cell-input desc-input" />
      <el-input
        v-if="node.mcpType === 'array'"
        v-model="node.itemType"
        placeholder="数组元素类型，如 string/object"
        class="cell-input short"
      />
      <el-button
        v-if="canAddChild"
        type="primary"
        text
        @click="$emit('add-child', node)"
      >
        添加子字段
      </el-button>
      <el-button type="danger" text @click="$emit('remove', node)">删除</el-button>
    </div>

    <ParamMappingNode
      v-for="(child, index) in node.children || []"
      :key="getChildKey(child, index)"
      :node="child"
      :level="level + 1"
      :max-level="maxLevel"
      @remove="$emit('remove', $event)"
      @add-child="$emit('add-child', $event)"
    />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ParamMappingNode as MappingNodeType } from '@/types/gateway'

defineOptions({ name: 'ParamMappingNode' })

const props = defineProps<{
  node: MappingNodeType & { __localKey?: string }
  level: number
  maxLevel: number
}>()

defineEmits<{
  (e: 'remove', node: MappingNodeType): void
  (e: 'add-child', node: MappingNodeType): void
}>()

const canAddChild = computed(() => {
  if (props.level >= props.maxLevel) {
    return false
  }
  return props.node.mcpType === 'object' || props.node.mcpType === 'array'
})

const typeOptions = [
  { label: '文本(string)', value: 'string' },
  { label: '数字(number)', value: 'number' },
  { label: '布尔(boolean)', value: 'boolean' },
  { label: '对象(object)', value: 'object' },
  { label: '数组(array)', value: 'array' }
]

const locationOptions = [
  { label: '请求体(body)', value: 'body' },
  { label: '查询参数(query)', value: 'query' },
  { label: '路径参数(path)', value: 'path' },
  { label: '请求头(header)', value: 'header' }
]

const childKeyMap = new WeakMap<object, string>()
const getChildKey = (node: MappingNodeType, index: number): string => {
  const objectNode = node as object
  let key = childKeyMap.get(objectNode)
  if (!key) {
    key = `child-${Date.now()}-${Math.random()}-${index}`
    childKeyMap.set(objectNode, key)
  }
  return key
}

const apiFieldName = computed({
  get: () => String(props.node.httpPath || props.node.fieldName || ''),
  set: (value: string) => {
    const normalized = String(value || '')
    props.node.httpPath = normalized
    props.node.fieldName = normalized
  }
})
</script>

<style scoped>
.mapping-node {
  margin-top: 8px;
}

.mapping-row {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  padding: 8px;
  border: 1px solid var(--gemini-border);
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.02);
}

.cell-input {
  width: 170px;
}

.cell-input.short {
  width: 150px;
}

.desc-input {
  width: 220px;
}

.required-select {
  width: 90px;
}
</style>
