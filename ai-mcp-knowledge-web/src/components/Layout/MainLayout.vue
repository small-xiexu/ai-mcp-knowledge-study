<template>
  <div class="gemini-main-layout">
    <!-- 系统侧边栏 -->
    <aside
      class="app-sidebar"
      :class="{ collapsed: appStore.sidebarCollapsed }"
    >
      <div class="sidebar-top">
        <el-button 
          link 
          class="menu-toggle-btn" 
          @click="appStore.toggleSidebar"
        >
          <el-icon><MenuIcon /></el-icon>
        </el-button>
        
        <div v-show="!appStore.sidebarCollapsed" class="logo-text">
          <span class="logo-main">MASP</span>
          <span class="logo-sub">Multi-Agent Scheduling Platform</span>
        </div>
      </div>

      <nav class="nav-menu">
        <!-- 折叠态：只展示扁平菜单（图标） -->
        <template v-if="appStore.sidebarCollapsed">
          <el-tooltip
            v-for="r in flatMenuRoutes"
            :key="r.path"
            :content="String(r.meta?.title || '未命名菜单')"
            placement="right"
            :show-after="120"
            :hide-after="0"
            :teleported="false"
            popper-class="sidebar-nav-tooltip"
          >
            <span class="nav-tooltip-trigger">
              <router-link
                :to="resolveMenuPath(r.path)"
                class="nav-item"
                :class="{ active: activeMenu === resolveMenuPath(r.path) }"
              >
                <div class="nav-icon-container">
                  <el-icon><component :is="r.meta?.icon" /></el-icon>
                </div>
              </router-link>
            </span>
          </el-tooltip>
        </template>

        <!-- 展开态：按分组展示，默认只展开“常用” -->
        <template v-else>
          <div
            v-for="g in menuGroups"
            :key="g.key"
            class="menu-group"
          >
            <div class="menu-group-header" @click="toggleGroup(g.key)">
              <span class="menu-group-title">{{ g.title }}</span>
              <el-icon class="menu-group-arrow" :class="{ open: isGroupOpen(g.key) }">
                <CaretBottom />
              </el-icon>
            </div>
            <div v-show="isGroupOpen(g.key)" class="menu-group-items">
              <router-link
                v-for="r in g.routes"
                :key="r.path"
                :to="resolveMenuPath(r.path)"
                class="nav-item"
                :class="{ active: activeMenu === resolveMenuPath(r.path) }"
              >
                <div class="nav-icon-container">
                  <el-icon><component :is="r.meta?.icon" /></el-icon>
                </div>
                <span class="nav-label">
                  {{ r.meta?.title }}
                </span>
              </router-link>
            </div>
          </div>
        </template>
      </nav>
    </aside>

    <!-- 主内容区 -->
    <main class="app-content">
      <!-- 顶部中央标题 (Gemini Style) -->
      <header class="app-header">
        <div class="header-center">
          <span class="page-title">{{ currentPageTitle }}</span>
        </div>
        <div class="header-actions">
          <el-dropdown trigger="click" popper-class="gemini-dropdown" @command="handleUserCommand">
            <div class="user-profile">
              <span class="pro-tag">{{ currentUserLabel }}</span>
              <el-avatar :size="32" class="user-avatar">
                {{ currentUserInitial }}
              </el-avatar>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">
                  <el-icon><SwitchButton /></el-icon>
                  退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <div class="page-container">
        <section v-if="currentFeatureGuide && !activeFeatureGuideTarget" class="feature-guide-wrap is-fallback">
          <div class="feature-guide">
            <div class="feature-guide-label">如何使用</div>
            <div class="feature-guide-content">
              <p class="feature-guide-summary">{{ currentFeatureGuide.summary }}</p>
              <div class="feature-guide-steps">
                <span v-for="(step, index) in currentFeatureGuide.steps" :key="step" class="step-chip">
                  <span class="step-index">{{ index + 1 }}</span>
                  <span class="step-text">{{ step }}</span>
                </span>
              </div>
            </div>
          </div>
        </section>

        <router-view v-slot="{ Component }">
          <transition name="fade" mode="out-in" @after-enter="handleRouteViewAfterEnter">
            <component :is="Component" />
          </transition>
        </router-view>

        <teleport v-if="currentFeatureGuide && activeFeatureGuideTarget" :to="activeFeatureGuideTarget">
          <section class="feature-guide-wrap">
            <div class="feature-guide">
              <div class="feature-guide-label">如何使用</div>
              <div class="feature-guide-content">
                <p class="feature-guide-summary">{{ currentFeatureGuide.summary }}</p>
                <div class="feature-guide-steps">
                  <span v-for="(step, index) in currentFeatureGuide.steps" :key="step" class="step-chip">
                    <span class="step-index">{{ index + 1 }}</span>
                    <span class="step-text">{{ step }}</span>
                  </span>
                </div>
              </div>
            </div>
          </section>
        </teleport>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAppStore } from '@/store/app'
import { useAuthStore } from '@/store/auth'
import { usePermission } from '@/composables/usePermission'
import routes from '@/router/routes'
import { Menu as MenuIcon, SwitchButton, CaretBottom } from '@element-plus/icons-vue'

const appStore = useAppStore()
const authStore = useAuthStore()
const { hasPermission } = usePermission()
const route = useRoute()
const router = useRouter()

type MenuGroupKey = 'common' | 'agent' | 'knowledge' | 'integration' | 'security'
type FeatureGuide = { summary: string; steps: string[] }

const groupDefs: Array<{ key: MenuGroupKey; title: string; defaultOpen: boolean; order: number }> = [
  { key: 'common', title: '常用', defaultOpen: true, order: 1 },
  { key: 'agent', title: 'Agent 平台', defaultOpen: true, order: 2 },
  { key: 'knowledge', title: '知识库', defaultOpen: false, order: 3 },
  { key: 'integration', title: '配置与集成', defaultOpen: false, order: 4 },
  { key: 'security', title: '用户与审计', defaultOpen: false, order: 5 }
]

const featureGuideMap: Record<string, FeatureGuide> = {
  Workbench: {
    summary: '这里是运营总览入口，先看健康度，再进入具体模块处理。',
    steps: ['查看关键指标与待办提醒', '按卡片跳转到目标模块', '完成处理后回到此页复核状态']
  },
  Dashboard: {
    summary: '用于观测模型与任务运行状态，定位异常优先从趋势图入手。',
    steps: ['先看失败率和耗时波动', '按时间窗口筛选异常段', '结合明细页定位具体原因']
  },
  XxlAdmin: {
    summary: '统一管理调度任务与执行日志，适合排查定时任务问题。',
    steps: ['先筛选任务名称定位目标', '检查 CRON 与启停状态', '进入日志查看执行结果与报错']
  },
  Models: {
    summary: '管理模型接入、能力开关与全局状态，是 AI 能力可用性的基础配置。',
    steps: ['先新增模型并校验 Base URL', '开启对话/嵌入/工具能力', '确认全局状态后再绑定工具']
  },
  ClientProfiles: {
    summary: '维护可复用的 Client 调用链，供 Agent/Workflow 统一引用。',
    steps: ['先新建 Profile 与基础信息', '配置步骤链并保存', '在上游 Agent 或 Workflow 中引用']
  },
  AgentEnhancers: {
    summary: '配置 Agent 增强器（AgentEnhancer）处理链，用于统一扩展请求前后处理逻辑。',
    steps: ['先定义 Agent 增强器参数', '按顺序组织处理链路', '在 Client Profile 中挂载并验证']
  },
  McpServers: {
    summary: '管理外部 MCP Server 的连接与启停，控制工具来源。',
    steps: ['新增服务并填写连接方式', '校验连接参数与状态', '启用后在工具页确认可见']
  },
  GatewayTools: {
    summary: '管理网关工具与参数映射，确保 AI 调用外部能力可控。',
    steps: ['先定位网关实例', '配置工具参数与响应提取', '调试通过后再发布使用']
  },
  GatewayToolList: {
    summary: '管理指定网关下的工具清单与配置明细。',
    steps: ['先确认目标网关', '逐项维护工具参数映射', '调试通过后再启用到生产流量']
  },
  Credentials: {
    summary: '管理网关访问凭证，供外部调用方鉴权使用。',
    steps: ['选择网关并创建凭证', '复制并安全分发 API Key', '定期轮换并禁用过期凭证']
  },
  Agents: {
    summary: '统一管理 Agent 元数据，后续版本发布、调用和调度都依赖这里。',
    steps: ['先创建 Agent 基础信息', '发布可用版本', '在调用或调度页面按 agentCode 使用']
  },
  AgentVersions: {
    summary: '查看与管理指定 Agent 的版本生命周期。',
    steps: ['确认当前发布版本', '按需发布新版本', '回退时先验证兼容性']
  },
  AgentPlayground: {
    summary: '按 agentCode 在线调试 Agent 运行结果。',
    steps: ['输入 agentCode 与请求内容', '执行并查看返回结构', '根据结果优化模板或工具配置']
  },
  AgentSchedules: {
    summary: '为 Agent 配置定时执行计划，执行时默认读取当前发布版本。',
    steps: ['先创建调度并填写 CRON', '设置启停状态', '通过运行记录确认任务执行成功']
  },
  ToolApprovals: {
    summary: '管理高风险工具审批单，审批后任务可自动续跑。',
    steps: ['筛选待处理审批单', '核对风险说明与调用上下文', '通过或拒绝并记录处理结论']
  },
  PromptTemplates: {
    summary: '集中维护 Prompt 模板，减少重复编写并保证输出风格一致。',
    steps: ['先创建模板与变量占位', '在调试页验证模板效果', '在 Agent/Workflow 中引用模板']
  },
  Workflows: {
    summary: '管理 Workflow 资产与版本，是复杂流程编排的入口。',
    steps: ['新建 Workflow 并维护元数据', '进入画布编排节点', '发布后通过调用页或调度页执行']
  },
  WorkflowVersions: {
    summary: '查看单个 Workflow 的版本列表与发布状态。',
    steps: ['确认目标版本配置', '发布稳定版本', '需要回退时切换到已验证版本']
  },
  WorkflowEditor: {
    summary: '在画布中编排节点执行顺序和参数流转。',
    steps: ['先补齐入口参数', '连接节点并设置映射', '保存后执行一次联调验证']
  },
  WorkflowPlayground: {
    summary: '按 workflowCode 快速验证流程输入输出。',
    steps: ['输入 workflowCode 与请求体', '执行并查看步骤明细', '根据失败节点回到画布调整']
  },
  WorkflowRuns: {
    summary: '查询 Workflow 运行历史，用于追踪失败和耗时问题。',
    steps: ['按状态或时间筛选记录', '打开单条记录查看步骤详情', '定位异常后回改流程配置']
  },
  WorkflowRunDetail: {
    summary: '展示单次运行的完整链路，便于问题定位。',
    steps: ['查看总体状态与耗时', '逐节点检查输入输出', '根据错误信息回到流程修复']
  },
  Knowledge: {
    summary: '管理知识库标签与文档，是 RAG 检索的核心入口。',
    steps: ['先新建标签分类', '上传文档触发异步处理', '在对话页绑定标签验证召回']
  },
  RagTasks: {
    summary: '跟踪知识库导入任务，失败任务可直接重试。',
    steps: ['筛选目标任务', '检查失败原因', '修复后点击重试并确认完成状态']
  },
  Users: {
    summary: '维护登录账号并分配角色，控制功能访问范围。',
    steps: ['新建用户并设置基础信息', '分配角色集合', '用目标账号验证权限是否生效']
  },
  Roles: {
    summary: '维护角色与权限集合，是权限体系的核心配置。',
    steps: ['创建角色定义职责边界', '勾选完整权限集合', '将角色分配给用户并验证']
  },
  AuditEvents: {
    summary: '查询身份与权限相关操作日志，满足审计追踪要求。',
    steps: ['按时间或资源筛选事件', '查看事件详情与上下文', '导出关键记录用于留档']
  }
}

const buildGroupOpen = (): Record<MenuGroupKey, boolean> => {
  const defaults = groupDefs.reduce((acc, g) => {
    acc[g.key] = g.defaultOpen
    return acc
  }, {} as Record<MenuGroupKey, boolean>)

  const raw = localStorage.getItem('menuGroupOpen')
  if (!raw) {
    return defaults
  }
  try {
    const parsed = JSON.parse(raw) as Partial<Record<MenuGroupKey, boolean>>
    return { ...defaults, ...parsed }
  } catch {
    return defaults
  }
}

const groupOpen = ref<Record<MenuGroupKey, boolean>>(buildGroupOpen())

const toggleGroup = (key: MenuGroupKey) => {
  groupOpen.value[key] = !groupOpen.value[key]
  localStorage.setItem('menuGroupOpen', JSON.stringify(groupOpen.value))
}

const isGroupOpen = (key: MenuGroupKey) => {
  return Boolean(groupOpen.value[key])
}

const normalizeGroupKey = (raw: unknown): MenuGroupKey => {
  if (raw === 'integration' || raw === 'security' || raw === 'knowledge' || raw === 'agent' || raw === 'common') {
    return raw
  }
  return 'common'
}

const allMenuRoutes = computed(() => {
  const layoutRoute = routes.find(r => r.path === '/')
  return (layoutRoute?.children || []).filter(item => {
    if (item.meta?.hidden) {
      return false
    }
    const permission = typeof item.meta?.permission === 'string' ? item.meta.permission : ''
    if (!permission) {
      return true
    }
    return hasPermission(permission)
  })
})

const menuGroups = computed(() => {
  const byKey: Record<MenuGroupKey, any[]> = {
    common: [],
    agent: [],
    knowledge: [],
    integration: [],
    security: []
  }

  for (const r of allMenuRoutes.value) {
    const raw = typeof r.meta?.group === 'string' ? r.meta.group : 'common'
    const key = normalizeGroupKey(raw)
    byKey[key].push(r)
  }

  const toOrder = (meta: any) => {
    const v = meta?.order
    return typeof v === 'number' ? v : 999
  }

  return groupDefs
    .slice()
    .sort((a, b) => a.order - b.order)
    .map(def => ({
      key: def.key,
      title: def.title,
      routes: byKey[def.key].slice().sort((a, b) => toOrder(a.meta) - toOrder(b.meta))
    }))
    .filter(g => g.routes.length > 0)
})

const flatMenuRoutes = computed(() => {
  return menuGroups.value.flatMap(g => g.routes)
})

const activeMenu = computed(() => {
  if (route.path.startsWith('/gateway-tools/')) {
    return '/gateway-tools'
  }
  return route.path
})

const ensureActiveGroupOpen = () => {
  const active = activeMenu.value
  const matched = allMenuRoutes.value.find(r => resolveMenuPath(r.path) === active)
  if (!matched) {
    return
  }
  const key = normalizeGroupKey(typeof matched.meta?.group === 'string' ? matched.meta.group : 'common')
  if (!groupOpen.value[key]) {
    groupOpen.value[key] = true
    localStorage.setItem('menuGroupOpen', JSON.stringify(groupOpen.value))
  }
}

watch(
  () => route.path,
  () => {
    if (!appStore.sidebarCollapsed) {
      ensureActiveGroupOpen()
    }
  },
  { immediate: true }
)

const currentPageTitle = computed(() => {
  return route.meta.title || '工作台'
})

const currentFeatureGuide = computed(() => {
  const routeName = typeof route.name === 'string' ? route.name : ''
  return routeName ? featureGuideMap[routeName] : undefined
})

const featureGuideTarget = ref<HTMLElement | null>(null)
const activeFeatureGuideTarget = computed(() => {
  const target = featureGuideTarget.value
  return target && target.isConnected ? target : null
})
let guideRetryTimer: number | null = null
let guideTargetCheckTimer: number | null = null

const clearGuideRetry = () => {
  if (guideRetryTimer !== null) {
    window.clearTimeout(guideRetryTimer)
    guideRetryTimer = null
  }
}

const clearGuideTargetCheck = () => {
  if (guideTargetCheckTimer !== null) {
    window.clearTimeout(guideTargetCheckTimer)
    guideTargetCheckTimer = null
  }
}

const monitorGuideTargetHealth = (target: HTMLElement, retry: number, attempt = 0) => {
  clearGuideTargetCheck()
  guideTargetCheckTimer = window.setTimeout(() => {
    if (featureGuideTarget.value !== target) {
      return
    }
    if (!target.isConnected) {
      featureGuideTarget.value = null
      if (currentFeatureGuide.value && retry < 40) {
        void resolveFeatureGuideTarget(retry + 1)
      }
      return
    }
    if (attempt < 16) {
      monitorGuideTargetHealth(target, retry, attempt + 1)
    }
  }, 100)
}

const resolveFeatureGuideTarget = async (retry = 0) => {
  await nextTick()
  const pageContainer = document.querySelector('.page-container')
  const headers = Array.from(pageContainer?.querySelectorAll('.gemini-container .page-header') || []) as HTMLElement[]
  const header = headers.length > 0 ? headers[headers.length - 1] : null
  if (!header) {
    featureGuideTarget.value = null
    if (retry < 40) {
      guideRetryTimer = window.setTimeout(() => {
        void resolveFeatureGuideTarget(retry + 1)
      }, 80)
    }
    return
  }

  const next = header.nextElementSibling as HTMLElement | null
  if (next && next.classList.contains('feature-guide-anchor') && next.isConnected) {
    featureGuideTarget.value = next
    monitorGuideTargetHealth(next, retry)
    return
  }

  const anchor = document.createElement('div')
  anchor.className = 'feature-guide-anchor'
  header.insertAdjacentElement('afterend', anchor)
  featureGuideTarget.value = anchor
  monitorGuideTargetHealth(anchor, retry)
}

watch(
  () => route.fullPath,
  () => {
    clearGuideRetry()
    clearGuideTargetCheck()
    featureGuideTarget.value = null
    if (!currentFeatureGuide.value) {
      return
    }
    void resolveFeatureGuideTarget()
  },
  { immediate: true }
)

const handleRouteViewAfterEnter = () => {
  if (!currentFeatureGuide.value) {
    return
  }
  clearGuideRetry()
  clearGuideTargetCheck()
  featureGuideTarget.value = null
  void resolveFeatureGuideTarget()
}

onBeforeUnmount(() => {
  clearGuideRetry()
  clearGuideTargetCheck()
})

function resolveMenuPath(path?: string) {
  if (!path) return '/'
  return path.startsWith('/') ? path : `/${path}`
}

const currentUserLabel = computed(() => {
  if (authStore.profile?.displayName) {
    return authStore.profile.displayName
  }
  if (authStore.profile?.username) {
    return authStore.profile.username
  }
  return '未登录'
})

const currentUserInitial = computed(() => {
  const label = currentUserLabel.value
  if (!label || label === '未登录') {
    return 'U'
  }
  return label.substring(0, 1).toUpperCase()
})

const handleUserCommand = async (command: string | number | object) => {
  if (command !== 'logout') {
    return
  }
  await authStore.logout()
  await router.replace('/login')
}
</script>

<style scoped>
.gemini-main-layout {
  display: flex;
  height: 100vh;
  width: 100vw;
  background-color: var(--gemini-bg-primary);
  color: var(--gemini-text-primary);
  overflow: hidden;
  font-family: var(--gemini-font);
}

/* Sidebar */
.app-sidebar {
  width: 260px;
  background-color: var(--gemini-bg-primary);
  display: flex;
  flex-direction: column;
  padding: 12px 16px;
  transition: width 0.3s cubic-bezier(0.2, 0, 0, 1), transform 0.3s ease;
  flex-shrink: 0;
  z-index: 100;
  /* Keep layout stable; let the menu area scroll instead of the whole page. */
  overflow: hidden;
}

.app-sidebar.collapsed {
  width: 72px;
  padding: 12px 14px;
}

.sidebar-top {
  height: 60px;
  display: flex;
  align-items: center;
  margin-bottom: 24px;
  gap: 12px;
}

.menu-toggle-btn {
  font-size: 24px;
  color: var(--gemini-text-secondary);
  padding: 8px;
  border-radius: 50%;
  transition: background-color 0.2s;
}

.menu-toggle-btn:hover {
  background-color: rgba(255, 255, 255, 0.05);
}

.logo-text {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
  gap: 2px;
  min-width: 0;
}

.logo-main {
  font-size: 22px;
  font-weight: 600;
  color: #e8eaed;
  letter-spacing: -0.5px;
  line-height: 1;
}

.logo-sub {
  font-size: 9px;
  font-weight: 500;
  color: var(--gemini-text-secondary);
  line-height: 1.1;
  white-space: nowrap;
  opacity: 0.86;
}

.new-chat-wrapper {
  margin-bottom: 32px;
}

.new-chat-btn {
  height: 48px;
  width: 140px;
  border-radius: 24px;
  background-color: #1e1f20;
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: var(--gemini-text-primary);
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 0 16px;
  gap: 12px;
  font-size: 14px;
  transition: all 0.2s;
}

.new-chat-btn.only-icon {
  width: 44px;
  padding: 0;
  justify-content: center;
  border-radius: 16px;
}

.new-chat-btn:hover {
  background-color: #2a2b2e;
  border-color: rgba(255, 255, 255, 0.2);
}

.nav-menu {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
  /* Make sidebar scrollable when menu exceeds viewport height */
  min-height: 0;
  overflow-y: auto;
  overscroll-behavior: contain;
  padding-right: 4px;
}

.menu-group {
  display: flex;
  flex-direction: column;
}

.menu-group-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 12px;
  margin: 6px 6px 2px;
  border-radius: 14px;
  color: var(--gemini-text-secondary);
  font-size: 12px;
  font-weight: 600;
  cursor: pointer;
  user-select: none;
  transition: background-color 0.18s ease, color 0.18s ease;
}

.menu-group-header:hover {
  background-color: rgba(255, 255, 255, 0.06);
  color: var(--gemini-text-primary);
}

.menu-group-arrow {
  transition: transform 0.18s ease;
  opacity: 0.9;
}

.menu-group-arrow.open {
  transform: rotate(180deg);
}

.menu-group-items {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.nav-item {
  display: flex;
  align-items: center;
  height: 48px;
  padding: 0 16px;
  border-radius: 24px;
  text-decoration: none;
  color: #e8eaed; /* Enhanced contrast */
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  cursor: pointer;
  position: relative;
  overflow: hidden;
}

.app-sidebar.collapsed .nav-item {
  width: 48px;
  padding: 0;
  justify-content: center;
}

.nav-item:hover {
  background-color: rgba(255, 255, 255, 0.08); /* More subtle hover */
}

.nav-item.active {
  background-color: rgba(138, 180, 248, 0.15); /* More vibrant active state */
  color: var(--gemini-accent);
}

.nav-icon-container {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.app-sidebar:not(.collapsed) .nav-icon-container {
  margin-right: 12px;
}

.app-sidebar.collapsed .nav-icon-container {
  margin-right: 0;
}

.nav-label {
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  color: inherit;
}

.nav-tooltip-trigger {
  display: block;
}

:deep(.sidebar-nav-tooltip) {
  max-width: 220px;
  animation: none !important;
}

/* Content Area */
.app-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  background-color: #0e0e10; /* Surface-dim look */
  border-top-left-radius: 28px;
  border-bottom-left-radius: 28px;
  overflow: hidden;
  position: relative;
}

.app-header {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  z-index: 10;
}

.header-center {
  background-color: rgba(30, 31, 32, 0.7);
  padding: 8px 16px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.page-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--gemini-text-primary);
}

.header-actions {
  display: flex;
  align-items: center;
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}

.pro-tag {
  color: var(--gemini-text-primary);
  font-weight: 600;
  font-size: 13px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  background: rgba(255, 255, 255, 0.06);
  padding: 4px 10px;
  border-radius: 12px;
  max-width: 180px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-avatar {
  background: rgba(138, 180, 248, 0.25);
  color: #d3e3fd;
  font-weight: 600;
}

.page-container {
  flex: 1;
  overflow: auto;
  padding: 0;
  position: relative;
}

.feature-guide-wrap {
  margin: -10px 0 18px;
  padding: 0;
  box-sizing: border-box;
}

.feature-guide-wrap.is-fallback {
  max-width: var(--gemini-content-max-width);
  margin: 0 auto;
  padding: 6px 24px 0;
}

.feature-guide {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  width: fit-content;
  max-width: 100%;
  padding: 12px 14px;
  border: 1px solid rgba(138, 180, 248, 0.25);
  background: rgba(138, 180, 248, 0.07);
  border-radius: 14px;
}

.feature-guide-label {
  flex: 0 0 auto;
  color: var(--gemini-accent);
  font-size: 12px;
  font-weight: 600;
  line-height: 22px;
}

.feature-guide-content {
  min-width: 0;
}

.feature-guide-summary {
  margin: 0;
  font-size: 13px;
  line-height: 1.45;
  color: var(--gemini-text-primary);
}

.feature-guide-steps {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.step-chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px 4px 6px;
  border-radius: 999px;
  border: 1px solid rgba(138, 180, 248, 0.25);
  background: rgba(18, 19, 20, 0.55);
  color: var(--gemini-text-secondary);
  font-size: 12px;
  line-height: 1.4;
}

.step-index {
  width: 16px;
  height: 16px;
  border-radius: 999px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  background: rgba(138, 180, 248, 0.18);
  color: var(--gemini-accent);
}

.step-text {
  white-space: normal;
  word-break: break-word;
}

@media (max-width: 1280px) {
  .feature-guide {
    width: 100%;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
