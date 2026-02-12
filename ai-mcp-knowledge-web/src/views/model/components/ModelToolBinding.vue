<template>
  <el-card class="gemini-card">
    <template #header>
      <div class="header-row">
        <span>工具绑定</span>
        <el-button type="primary" text @click="dialogVisible = true">配置模型工具绑定</el-button>
      </div>
    </template>

    <el-text type="info">未配置绑定时，模型默认可见所有已发布工具。</el-text>

    <ToolBindingDialog v-model:visible="dialogVisible" @success="refreshHint" />
  </el-card>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import ToolBindingDialog from '@/views/gateway/binding/ToolBindingDialog.vue'

const dialogVisible = ref(false)

const refreshHint = () => {
  ElMessage.success('模型工具绑定已更新')
}
</script>

<style scoped>
.header-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
