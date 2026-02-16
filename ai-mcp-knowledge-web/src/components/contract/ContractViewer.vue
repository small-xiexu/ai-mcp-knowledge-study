<template>
  <div class="contract-viewer">
    <div class="meta-row" v-if="contract?.meta">
      <div class="meta-item"><span class="k">runId</span><span class="v">{{ contract.meta.runId || '-' }}</span></div>
      <div class="meta-item"><span class="k">status</span><span class="v">{{ contract.status || '-' }}</span></div>
      <div class="meta-item" v-if="contract.meta.modelUsed"><span class="k">model</span><span class="v">{{ contract.meta.modelUsed }}</span></div>
      <div class="meta-item" v-if="contract.meta.costMs != null"><span class="k">cost</span><span class="v">{{ contract.meta.costMs }}ms</span></div>
      <div class="meta-item" v-if="contract.meta.approvalRequestId"><span class="k">approval</span><span class="v">#{{ contract.meta.approvalRequestId }}</span></div>
      <div class="meta-item" v-if="contract.meta.pendingToolKey"><span class="k">tool</span><span class="v">{{ contract.meta.pendingToolKey }}</span></div>
      <div class="meta-item" v-if="contract.meta.riskLevel"><span class="k">risk</span><span class="v">{{ contract.meta.riskLevel }}</span></div>
    </div>

    <el-alert
      v-if="contract?.status === 'FAILED'"
      type="error"
      :closable="false"
      show-icon
      style="margin-bottom: 10px"
    >
      <template #title>执行失败</template>
      <div class="err-detail">
        {{ contract?.error?.message || contract?.error?.detail || '未知错误' }}
      </div>
    </el-alert>

    <el-alert
      v-if="contract?.status === 'PENDING_APPROVAL'"
      type="warning"
      :closable="false"
      show-icon
      style="margin-bottom: 10px"
    >
      <template #title>需要工具审批</template>
      <div class="err-detail">
        approvalRequestId={{ contract?.meta?.approvalRequestId || '-' }}
        <span v-if="contract?.meta?.pendingToolKey">, tool={{ contract?.meta?.pendingToolKey }}</span>
      </div>
    </el-alert>

    <div class="answer-block">
      <div class="section-title">Answer</div>
      <div class="answer-text">{{ contract?.answer || '' }}</div>
    </div>

    <div class="steps-block">
      <div class="section-title">
        Steps
        <span class="muted">({{ (contract?.steps || []).length }})</span>
      </div>
      <el-empty v-if="!contract?.steps || contract.steps.length === 0" description="无运行明细" />
      <el-collapse v-else accordion>
        <el-collapse-item v-for="(s, idx) in contract.steps" :key="`${s.nodeKey || idx}`" :name="String(idx)">
          <template #title>
            <div class="step-title">
              <span class="step-no">#{{ idx + 1 }}</span>
              <span class="step-name">{{ s.nodeName || s.nodeKey || '-' }}</span>
              <span class="step-type">{{ s.nodeType || '-' }}</span>
              <el-tag size="small" :type="tagType(s.status)">{{ s.status || '-' }}</el-tag>
              <span v-if="s.costMs != null" class="step-metric">{{ s.costMs }}ms</span>
              <span v-if="s.totalTokens != null" class="step-metric">{{ s.totalTokens }} tok</span>
              <span v-if="s.toolCallCount != null && s.toolCallCount > 0" class="step-metric">tool={{ s.toolCallCount }}</span>
              <span v-if="s.toolDeniedCount != null && s.toolDeniedCount > 0" class="step-metric">deny={{ s.toolDeniedCount }}</span>
              <span v-if="s.outputTruncated" class="step-metric warn">truncated</span>
            </div>
          </template>

          <div class="step-body">
            <div class="kv">
              <div class="kv-item" v-if="s.inputDigest"><span class="k">inputDigest</span><span class="v mono">{{ s.inputDigest }}</span></div>
              <div class="kv-item" v-if="s.outputDigest"><span class="k">outputDigest</span><span class="v mono">{{ s.outputDigest }}</span></div>
              <div class="kv-item" v-if="s.approvalRequestId"><span class="k">approvalRequestId</span><span class="v mono">{{ s.approvalRequestId }}</span></div>
            </div>
            <div v-if="s.errorMessage" class="step-err">{{ s.errorMessage }}</div>
            <pre class="step-output">{{ s.outputText || '' }}</pre>
          </div>
        </el-collapse-item>
      </el-collapse>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { PlatformContractV1 } from '@/types/workflow'

defineProps<{
  contract: PlatformContractV1 | null
}>()

const tagType = (status?: string) => {
  const s = (status || '').toUpperCase()
  if (s === 'SUCCESS') return 'success'
  if (s === 'FAILED') return 'danger'
  if (s === 'RUNNING') return 'info'
  if (s === 'PENDING_APPROVAL') return 'warning'
  return ''
}
</script>

<style scoped lang="scss">
.contract-viewer {
  color: var(--gemini-text-primary);
}
.meta-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}
.meta-item {
  display: inline-flex;
  gap: 6px;
  align-items: center;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.08);
  padding: 6px 10px;
  border-radius: 10px;
}
.k {
  color: var(--gemini-text-secondary);
  font-size: 12px;
}
.v {
  font-size: 12px;
}
.mono {
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, "Liberation Mono", "Courier New", monospace;
}
.section-title {
  font-weight: 700;
  margin: 2px 0 10px;
}
.muted {
  color: var(--gemini-text-secondary);
  font-weight: 500;
  margin-left: 6px;
  font-size: 12px;
}
.answer-block {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  padding: 12px;
  margin-bottom: 12px;
}
.answer-text {
  white-space: pre-wrap;
  word-break: break-word;
  line-height: 1.6;
  font-size: 13px;
}
.steps-block {
  background: rgba(255, 255, 255, 0.04);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  padding: 12px;
}
.step-title {
  display: flex;
  align-items: center;
  gap: 8px;
  width: 100%;
}
.step-no {
  color: var(--gemini-text-secondary);
  font-size: 12px;
  width: 44px;
}
.step-name {
  font-weight: 650;
  font-size: 13px;
}
.step-type {
  color: var(--gemini-text-secondary);
  font-size: 12px;
  margin-left: 6px;
}
.step-metric {
  color: var(--gemini-text-secondary);
  font-size: 12px;
}
.warn {
  color: #ffcc66;
}
.step-body {
  padding: 6px 0;
}
.kv {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 8px;
}
.kv-item {
  display: inline-flex;
  gap: 6px;
  align-items: center;
}
.step-err {
  color: #ff8a8a;
  margin-bottom: 8px;
  white-space: pre-wrap;
}
.step-output {
  max-height: 420px;
  overflow: auto;
  padding: 12px;
  background: rgba(0, 0, 0, 0.16);
  border-radius: 10px;
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
  line-height: 1.5;
}
.err-detail {
  white-space: pre-wrap;
  word-break: break-word;
  font-size: 12px;
}
</style>

