<template>
  <div class="gemini-container workbench">
    <div class="page-header">
      <div class="header-content">
        <h2 class="page-title">工作台</h2>
        <div class="header-subtitle">从这里开始：对话、知识库、任务</div>
      </div>
    </div>

    <el-row :gutter="20" class="quick-actions">
      <el-col :xs="24" :sm="12" :md="6">
        <div class="action-card primary" @click="go('/ai-chat')">
          <div class="card-top">
            <div class="card-icon">
              <el-icon><ChatDotRound /></el-icon>
            </div>
            <div class="card-title">开始 AI 对话</div>
          </div>
          <div class="card-desc">直接提问、测试工具调用效果、验证模型体验。</div>
          <div class="card-cta">
            <el-button type="primary" class="gemini-btn-primary" @click.stop="go('/ai-chat')">进入</el-button>
          </div>
        </div>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <div class="action-card" @click="go('/agents')">
          <div class="card-top">
            <div class="card-icon">
              <el-icon><Operation /></el-icon>
            </div>
            <div class="card-title">Agent 管理</div>
          </div>
          <div class="card-desc">
            当前组织共 {{ summary?.agent?.total ?? 0 }} 个 Agent，已发布 {{ summary?.agent?.published ?? 0 }} 个。
          </div>
          <div class="card-cta">
            <el-button class="gemini-btn-secondary" @click.stop="go('/agents')">进入</el-button>
          </div>
        </div>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <div class="action-card" @click="go('/knowledge')">
          <div class="card-top">
            <div class="card-icon">
              <el-icon><Collection /></el-icon>
            </div>
            <div class="card-title">管理知识库</div>
          </div>
          <div class="card-desc">创建知识库、导入资料、为对话提供可检索的企业知识。</div>
          <div class="card-cta">
            <el-button class="gemini-btn-secondary" @click.stop="go('/knowledge')">进入</el-button>
          </div>
        </div>
      </el-col>

      <el-col :xs="24" :sm="12" :md="6">
        <div class="action-card" @click="go('/rag-tasks')">
          <div class="card-top">
            <div class="card-icon">
              <el-icon><Clock /></el-icon>
            </div>
            <div class="card-title">查看导入任务</div>
          </div>
          <div class="card-desc">
            任务 {{ summary?.knowledge?.ragTaskTotal ?? 0 }}，处理中 {{ summary?.knowledge?.ragTaskProcessing ?? 0 }}，
            近 7 天失败 {{ summary?.knowledge?.ragTaskFailedRecent ?? 0 }}。
          </div>
          <div class="card-cta">
            <el-button class="gemini-btn-secondary" @click.stop="go('/rag-tasks')">进入</el-button>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :xs="24" :md="14">
        <el-card class="gemini-card guide-card" shadow="never">
          <template #header>
            <div class="guide-header">
              <el-icon><Connection /></el-icon>
              <span>多 Agent 治理闭环</span>
              <span v-if="summaryLoading" class="loading-hint">加载中...</span>
            </div>
          </template>

          <div class="governance-stats">
            <div class="stat">
              <div class="stat-label">工具策略</div>
              <div class="stat-value">{{ summary?.tool?.toolPolicyEnabled ?? 0 }} / {{ summary?.tool?.toolPolicyTotal ?? 0 }}</div>
            </div>
            <div class="stat">
              <div class="stat-label">待审批</div>
              <div class="stat-value" :class="{ danger: (summary?.tool?.approvalsPending ?? 0) > 0 }">
                {{ summary?.tool?.approvalsPending ?? 0 }}
              </div>
            </div>
            <div class="stat">
              <div class="stat-label">启用调度</div>
              <div class="stat-value">{{ summary?.schedule?.enabled ?? 0 }} / {{ summary?.schedule?.total ?? 0 }}</div>
            </div>
            <div class="stat">
              <div class="stat-label">GLOBAL 模板</div>
              <div class="stat-value">{{ summary?.prompt?.globalPublished ?? 0 }}</div>
            </div>
          </div>

          <div class="guide-steps v2">
            <div v-for="s in (summary?.guideSteps || [])" :key="s.key" class="step v2" :class="stepClass(s.status)">
              <div class="step-left">
                <div class="step-badge">{{ stepBadge(s.status) }}</div>
                <div class="step-body">
                  <div class="step-title">{{ s.title }}</div>
                  <div v-if="s.message" class="step-desc">{{ s.message }}</div>
                </div>
              </div>
              <el-button
                v-if="s.actionPath && s.actionLabel"
                text
                class="link-btn"
                @click="go(s.actionPath)"
              >
                {{ s.actionLabel }}
              </el-button>
            </div>

            <div v-if="(summary?.guideSteps || []).length === 0" class="empty-guide">
              暂无数据
            </div>
          </div>
        </el-card>
      </el-col>

      <el-col :xs="24" :md="10">
        <el-card class="gemini-card guide-card" shadow="never">
          <template #header>
            <div class="guide-header">
              <el-icon><Connection /></el-icon>
              <span>常见入口</span>
            </div>
          </template>

          <div class="link-grid">
            <el-button class="gemini-btn-secondary" @click="go('/models')">LLM 配置</el-button>
            <el-button class="gemini-btn-secondary" @click="go('/templates')">Prompt 模板</el-button>
            <el-button class="gemini-btn-secondary" @click="go('/tool-policies')">工具策略</el-button>
            <el-button class="gemini-btn-secondary" @click="go('/approvals')">工具审批</el-button>
            <el-button class="gemini-btn-secondary" @click="go('/schedules')">Agent 调度</el-button>
            <el-button class="gemini-btn-secondary" @click="go('/agent-playground')">Agent 调用</el-button>
            <el-button class="gemini-btn-secondary" @click="go('/xxl')">任务中心</el-button>
            <el-button class="gemini-btn-secondary" @click="go('/dashboard')">监控看板</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="gemini-card guide-card legacy" shadow="never">
      <template #header>
        <div class="guide-header">
          <el-icon><Connection /></el-icon>
          <span>知识库与集成（补充）</span>
        </div>
      </template>

      <div class="guide-steps">
        <div class="step">
          <div class="step-num">1</div>
          <div class="step-body">
            <div class="step-title">先能对话</div>
            <div class="step-desc">如果还没配置模型，去「LLM 配置」启用可用模型。</div>
          </div>
          <el-button text class="link-btn" @click="go('/models')">去配置</el-button>
        </div>

        <div class="step">
          <div class="step-num">2</div>
          <div class="step-body">
            <div class="step-title">再接知识</div>
            <div class="step-desc">去「知识库管理」导入资料，让对话具备企业上下文。</div>
          </div>
          <el-button text class="link-btn" @click="go('/knowledge')">去导入</el-button>
        </div>

        <div class="step">
          <div class="step-num">3</div>
          <div class="step-body">
            <div class="step-title">需要工具再集成</div>
            <div class="step-desc">外部 MCP、网关工具与凭证集中在「配置与集成」。</div>
          </div>
          <el-button text class="link-btn" @click="expandAdvanced = true">展开入口</el-button>
        </div>
      </div>

      <el-collapse v-model="advancedOpen" class="advanced-collapse">
        <el-collapse-item name="integration" title="配置与集成">
          <div class="link-grid">
            <el-button class="gemini-btn-secondary" @click="go('/models')">LLM 配置</el-button>
            <el-button class="gemini-btn-secondary" @click="go('/tasks')">策略配置</el-button>
            <el-button class="gemini-btn-secondary" @click="go('/mcp-servers')">MCP 配置</el-button>
            <el-button class="gemini-btn-secondary" @click="go('/gateway-tools')">网关工具</el-button>
            <el-button class="gemini-btn-secondary" @click="go('/credentials')">凭证管理</el-button>
          </div>
        </el-collapse-item>
        <el-collapse-item name="org" title="组织与审计">
          <div class="link-grid">
            <el-button class="gemini-btn-secondary" @click="go('/users')">用户管理</el-button>
            <el-button class="gemini-btn-secondary" @click="go('/roles')">角色管理</el-button>
            <el-button class="gemini-btn-secondary" @click="go('/audit-events')">身份审计</el-button>
            <el-button class="gemini-btn-secondary" @click="go('/audit')">审计日志</el-button>
          </div>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ChatDotRound, Clock, Collection, Connection, Operation } from '@element-plus/icons-vue'
import { getWorkbenchSummary } from '@/api/workbench'
import type { WorkbenchSummary } from '@/types/workbench'

const router = useRouter()

const expandAdvanced = ref(false)
const advancedOpen = ref<string[]>([])

const summaryLoading = ref(false)
const summary = ref<WorkbenchSummary | null>(null)

watch(
  () => expandAdvanced.value,
  (val) => {
    if (!val) {
      return
    }
    if (!advancedOpen.value.includes('integration')) {
      advancedOpen.value = ['integration']
    }
  }
)

const go = async (path: string) => {
  await router.push(path)
}

const loadSummary = async () => {
  summaryLoading.value = true
  try {
    const res = await getWorkbenchSummary()
    summary.value = res.data || null
  } finally {
    summaryLoading.value = false
  }
}

const stepClass = (status?: string) => {
  if (status === 'DONE') return 'done'
  if (status === 'BLOCKED') return 'blocked'
  return 'todo'
}

const stepBadge = (status?: string) => {
  if (status === 'DONE') return '已完成'
  if (status === 'BLOCKED') return '已拦截'
  return '待处理'
}

onMounted(() => {
  loadSummary()
})
</script>

<style scoped>
.workbench .header-subtitle {
  color: var(--gemini-text-secondary);
  font-size: 14px;
}

.quick-actions {
  margin-bottom: 24px;
}

.action-card {
  border: 1px solid var(--gemini-border);
  background: rgba(255, 255, 255, 0.03);
  border-radius: 18px;
  padding: 18px 18px 16px;
  cursor: pointer;
  transition: transform 0.18s ease, border-color 0.18s ease, background 0.18s ease;
  min-height: 150px;
  position: relative;
  overflow: hidden;
}

.action-card:hover {
  transform: translateY(-2px);
  border-color: rgba(138, 180, 248, 0.45);
  background: rgba(138, 180, 248, 0.06);
}

.action-card.primary {
  background: radial-gradient(1200px 600px at 20% -20%, rgba(138, 180, 248, 0.22), transparent 55%),
    rgba(255, 255, 255, 0.03);
}

.card-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}

.card-icon {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.card-icon :deep(svg) {
  width: 22px;
  height: 22px;
  color: var(--gemini-accent);
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: var(--gemini-text-primary);
}

.card-desc {
  color: var(--gemini-text-secondary);
  font-size: 13px;
  line-height: 1.6;
  margin-bottom: 14px;
}

.card-cta {
  display: flex;
  justify-content: flex-start;
}

.guide-card :deep(.el-card__body) {
  padding: 18px 20px 20px;
}

.guide-header {
  display: flex;
  align-items: center;
  gap: 8px;
}

.guide-steps {
  display: flex;
  flex-direction: column;
  gap: 14px;
  margin-bottom: 12px;
}

.guide-steps.v2 {
  margin-bottom: 0;
}

.step.v2 {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  padding: 12px 12px;
  background: rgba(255, 255, 255, 0.02);
}

.step-left {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.step-badge {
  flex: 0 0 auto;
  font-size: 12px;
  font-weight: 700;
  padding: 6px 10px;
  border-radius: 999px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: var(--gemini-text-secondary);
  background: rgba(255, 255, 255, 0.04);
}

.step.todo .step-badge {
  color: var(--gemini-accent);
  border-color: rgba(138, 180, 248, 0.35);
  background: rgba(138, 180, 248, 0.10);
}

.step.done .step-badge {
  color: var(--gemini-success);
  border-color: rgba(129, 201, 149, 0.35);
  background: rgba(129, 201, 149, 0.10);
}

.step.blocked {
  border-color: rgba(242, 139, 130, 0.30);
  background: rgba(242, 139, 130, 0.07);
}

.step.blocked .step-badge {
  color: var(--gemini-danger);
  border-color: rgba(242, 139, 130, 0.35);
  background: rgba(242, 139, 130, 0.10);
}

.empty-guide {
  padding: 12px 2px;
  color: var(--gemini-text-secondary);
  font-size: 13px;
}

.step {
  display: flex;
  align-items: center;
  gap: 12px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  padding: 12px 12px;
  background: rgba(255, 255, 255, 0.02);
}

.step-num {
  width: 26px;
  height: 26px;
  border-radius: 10px;
  background: rgba(138, 180, 248, 0.14);
  color: var(--gemini-accent);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  flex: 0 0 auto;
}

.step-body {
  flex: 1;
  min-width: 0;
}

.step-title {
  font-weight: 600;
  color: var(--gemini-text-primary);
  margin-bottom: 2px;
}

.step-desc {
  color: var(--gemini-text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.link-btn {
  color: var(--gemini-accent);
}

.governance-stats {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.stat {
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 14px;
  padding: 10px 10px;
  background: rgba(255, 255, 255, 0.02);
}

.stat-label {
  font-size: 12px;
  color: var(--gemini-text-secondary);
  margin-bottom: 4px;
}

.stat-value {
  font-size: 16px;
  font-weight: 700;
  color: var(--gemini-text-primary);
}

.stat-value.danger {
  color: var(--gemini-danger);
}

.loading-hint {
  margin-left: 10px;
  font-size: 12px;
  color: var(--gemini-text-secondary);
  font-weight: 400;
}

.advanced-collapse {
  --el-collapse-header-bg-color: transparent;
  --el-collapse-content-bg-color: transparent;
  --el-collapse-border-color: rgba(255, 255, 255, 0.08);
}

.link-grid {
  display: grid;
  width: 100%;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  justify-items: stretch;
}

.link-grid :deep(.el-button) {
  width: 100%;
  justify-content: center;
}

.link-grid :deep(.el-button + .el-button) {
  margin-left: 0;
}

@media (max-width: 960px) {
  .link-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .governance-stats {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 520px) {
  .link-grid {
    grid-template-columns: 1fr;
  }

  .governance-stats {
    grid-template-columns: 1fr;
  }
}
</style>
