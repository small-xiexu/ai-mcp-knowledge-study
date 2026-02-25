import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/index.vue'),
    meta: { title: '登录', hidden: true }
  },
  {
    path: '/',
    component: () => import('@/components/Layout/MainLayout.vue'),
    children: [
      {
        path: 'workbench',
        name: 'Workbench',
        component: () => import('@/views/workbench/index.vue'),
        meta: { title: '工作台', icon: 'House', group: 'common', order: 1 }
      },
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '监控看板', icon: 'DataAnalysis', group: 'common', order: 60 }
      },
      {
        path: 'models',
        name: 'Models',
        component: () => import('@/views/model/index.vue'),
        meta: { title: 'LLM 配置', icon: 'DataLine', permission: 'agent:read', group: 'integration', order: 10 }
      },
      {
        path: 'client-profiles',
        name: 'ClientProfiles',
        component: () => import('@/views/client-profile/index.vue'),
        meta: { title: 'Client 配置', icon: 'SetUp', permission: 'agent:read', group: 'integration', order: 20 }
      },
      {
        path: 'ai-chat',
        name: 'AiChat',
        component: () => import('@/views/chat/index.vue'),
        meta: { title: 'AI 对话', icon: 'ChatDotRound', permission: 'agent:read', group: 'common', order: 10 }
      },
      {
        path: 'agents',
        name: 'Agents',
        component: () => import('@/views/agent-platform/agents/index.vue'),
        meta: { title: 'Agent 管理', icon: 'Operation', permission: 'agent:read', group: 'agent', order: 10 }
      },
      {
        path: 'agents/:agentCode/versions',
        name: 'AgentVersions',
        component: () => import('@/views/agent-platform/agents/versions.vue'),
        meta: { title: 'Agent 版本', icon: 'List', hidden: true, permission: 'agent:read', group: 'agent', order: 11 }
      },
      {
        path: 'workflows',
        name: 'Workflows',
        component: () => import('@/views/workflow/index.vue'),
        meta: { title: 'Workflow 管理', icon: 'Connection', permission: 'workflow:read', group: 'agent', order: 15 }
      },
      {
        path: 'workflows/:workflowCode/versions',
        name: 'WorkflowVersions',
        component: () => import('@/views/workflow/versions.vue'),
        meta: { title: 'Workflow 版本', icon: 'List', hidden: true, permission: 'workflow:read', group: 'agent', order: 16 }
      },
      {
        path: 'workflows/:workflowCode/editor',
        name: 'WorkflowEditor',
        component: () => import('@/views/workflow/editor.vue'),
        meta: { title: 'Workflow 编辑', icon: 'EditPen', hidden: true, permission: 'workflow:write', group: 'agent', order: 17 }
      },
      {
        path: 'workflow-playground',
        name: 'WorkflowPlayground',
        component: () => import('@/views/workflow/playground.vue'),
        meta: { title: 'Workflow 调用', icon: 'ChatDotRound', permission: 'workflow:invoke', group: 'agent', order: 18 }
      },
      {
        path: 'workflow-runs',
        name: 'WorkflowRuns',
        component: () => import('@/views/workflow/runs.vue'),
        meta: { title: 'Workflow 运行记录', icon: 'Timer', permission: 'workflow:read', group: 'agent', order: 19 }
      },
      {
        path: 'workflow-runs/:runId',
        name: 'WorkflowRunDetail',
        component: () => import('@/views/run/workflow-run-detail.vue'),
        meta: { title: '运行详情', icon: 'Document', hidden: true, permission: 'workflow:read', group: 'agent', order: 19 }
      },
      {
        path: 'templates',
        name: 'PromptTemplates',
        component: () => import('@/views/agent-platform/templates/index.vue'),
        meta: { title: 'Prompt 模板', icon: 'Document', permission: 'agent:read', group: 'agent', order: 20 }
      },
      {
        path: 'approvals',
        name: 'ToolApprovals',
        component: () => import('@/views/agent-platform/approvals/index.vue'),
        meta: { title: '工具审批', icon: 'Tickets', permission: 'tool:approve', group: 'agent', order: 30 }
      },
      {
        path: 'schedules',
        name: 'AgentSchedules',
        component: () => import('@/views/agent-platform/schedules/index.vue'),
        meta: { title: 'Agent 调度', icon: 'Timer', permission: 'agent:read', group: 'agent', order: 50 }
      },
      {
        path: 'agent-playground',
        name: 'AgentPlayground',
        component: () => import('@/views/agent-platform/playground/index.vue'),
        meta: { title: 'Agent 调用', icon: 'ChatDotRound', permission: 'agent:invoke', group: 'agent', order: 60 }
      },
      {
        path: 'knowledge',
        name: 'Knowledge',
        component: () => import('@/views/knowledge/index.vue'),
        meta: { title: '知识库管理', icon: 'Collection', permission: 'agent:read', group: 'knowledge', order: 10 }
      },
      {
        path: 'rag-tasks',
        name: 'RagTasks',
        component: () => import('@/views/rag-task/index.vue'),
        meta: { title: '导入任务', icon: 'Clock', permission: 'agent:read', group: 'knowledge', order: 20 }
      },
      {
        path: 'agent-enhancers',
        name: 'AgentEnhancers',
        component: () => import('@/views/agent-enhancer/index.vue'),
        meta: { title: 'Agent 增强器配置', icon: 'Tools', permission: 'agent-enhancer:read', group: 'integration', order: 25 }
      },
      {
        path: 'xxl',
        name: 'XxlAdmin',
        component: () => import('@/views/xxl/index.vue'),
        meta: { title: '任务中心', icon: 'Timer', permission: 'workflow:read', group: 'common', order: 40 }
      },
      {
        path: 'mcp-servers',
        name: 'McpServers',
        component: () => import('@/views/mcp/index.vue'),
        meta: { title: 'MCP 工具配置', icon: 'Link', permission: 'tool:read', group: 'integration', order: 30 }
      },
      {
        path: 'gateway-tools',
        name: 'GatewayTools',
        component: () => import('@/views/gateway/tools/index.vue'),
        meta: { title: 'HTTP 工具配置', icon: 'Operation', permission: 'tool:read', group: 'integration', order: 40 }
      },
      {
        path: 'gateway-tools/:gatewayId/tools',
        name: 'GatewayToolList',
        component: () => import('@/views/gateway/tools/index.vue'),
        meta: { title: '工具配置', icon: 'Tools', hidden: true, permission: 'tool:read', group: 'integration', order: 41 }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/user/index.vue'),
        meta: { title: '用户管理', icon: 'User', permission: 'user:read', group: 'security', order: 10 }
      },
      {
        path: 'roles',
        name: 'Roles',
        component: () => import('@/views/role/index.vue'),
        meta: { title: '角色管理', icon: 'Avatar', permission: 'role:read', group: 'security', order: 20 }
      },
      {
        path: 'credentials',
        name: 'Credentials',
        component: () => import('@/views/gateway/credential/index.vue'),
        meta: { title: '凭证管理', icon: 'Key', permission: 'tool:read', group: 'integration', order: 50 }
      },
      {
        path: 'audit-events',
        name: 'AuditEvents',
        component: () => import('@/views/audit-event/index.vue'),
        meta: { title: '身份审计', icon: 'Tickets', permission: 'audit:read', group: 'security', order: 40 }
      }
    ]
  },
  {
    path: '/',
    redirect: '/workbench'
  }
]

export default routes
