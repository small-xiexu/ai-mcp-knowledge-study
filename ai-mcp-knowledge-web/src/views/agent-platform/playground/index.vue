<template>
  <div class="gemini-container">
    <div class="page-header">
      <div>
        <h2 class="page-title">Agent 调用</h2>
        <p class="subtitle">按 agentCode 调用运行入口（返回 Platform Contract v1）</p>
      </div>
      <div class="header-actions">
        <el-button class="gemini-btn-secondary" @click="loadAgents">
          <el-icon><Refresh /></el-icon>
        </el-button>
      </div>
    </div>

    <div class="gemini-card">
      <el-form :model="form" label-width="120px" class="gemini-form">
        <el-form-item label="agentCode">
          <el-select v-model="form.agentCode" filterable clearable placeholder="选择 Agent" style="width: 100%">
            <el-option v-for="a in agentOptions" :key="a.agentCode" :label="`${a.agentName} (${a.agentCode})`" :value="a.agentCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="content">
          <el-input v-model="form.content" type="textarea" :rows="5" placeholder="输入内容" />
        </el-form-item>
        <el-form-item label="ragTagsJson">
          <el-input v-model="form.ragTagsJson" placeholder='例如：["tag1","tag2"]' />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" class="gemini-btn-primary" :loading="running" @click="run">
            <el-icon><Lightning /></el-icon>
            执行
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <div class="gemini-card" style="margin-top: 14px">
      <div class="result-title">结果</div>
      <div v-if="showPretty && resultObj" class="pretty-box">
        <ContractViewer :contract="resultObj" />
      </div>
      <pre v-else class="result-pre">{{ resultText }}</pre>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { agentChat, listAgents, type Agent, type PlatformContractV1 } from '@/api/agent-platform'
import ContractViewer from '@/components/contract/ContractViewer.vue'

const agentOptions = ref<Agent[]>([])
const running = ref(false)
const resultText = ref('')
const resultObj = ref<PlatformContractV1 | null>(null)
const showPretty = ref(true)

const form = reactive({
  agentCode: '',
  content: '',
  ragTagsJson: '[]'
})

const loadAgents = async () => {
  try {
    const res = await listAgents({ pageNum: 1, pageSize: 200 })
    agentOptions.value = res.data.records || []
  } catch {
    agentOptions.value = []
  }
}

const run = async () => {
  if (!form.agentCode) {
    ElMessage.error('请选择 agentCode')
    return
  }
  if (!form.content) {
    ElMessage.error('content 不能为空')
    return
  }
  running.value = true
  try {
    const res = await agentChat(form.agentCode, {
      content: form.content,
      ragTagsJson: form.ragTagsJson || undefined
    })
    const data = res.data as PlatformContractV1
    resultObj.value = data
    resultText.value = JSON.stringify(data, null, 2)
    if (data?.status === 'PENDING_APPROVAL') {
      ElMessage.warning(`需要审批：approvalRequestId=${data.meta?.approvalRequestId}`)
    }
    if (data?.status === 'FAILED') {
      ElMessage.error('执行失败（详情见 result.error）')
    }
  } finally {
    running.value = false
  }
}

loadAgents()
</script>

<style scoped lang="scss">
.result-title {
  color: var(--gemini-text-primary);
  font-weight: 600;
  margin-bottom: 10px;
}
.pretty-box {
  max-height: 620px;
  overflow: auto;
}
.result-pre {
  max-height: 520px;
  overflow: auto;
  padding: 12px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 10px;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
}
</style>
