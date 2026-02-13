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
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/index.vue'),
        meta: { title: '监控看板', icon: 'DataAnalysis' }
      },
      {
        path: 'models',
        name: 'Models',
        component: () => import('@/views/model/index.vue'),
        meta: { title: 'LLM 配置', icon: 'Setting', permission: 'agent:read' }
      },
      {
        path: 'ai-chat',
        name: 'AiChat',
        component: () => import('@/views/chat/index.vue'),
        meta: { title: 'AI 对话', icon: 'ChatDotRound', permission: 'agent:read' }
      },
      {
        path: 'knowledge',
        name: 'Knowledge',
        component: () => import('@/views/knowledge/index.vue'),
        meta: { title: '知识库管理', icon: 'Collection', permission: 'agent:read' }
      },
      {
        path: 'rag-tasks',
        name: 'RagTasks',
        component: () => import('@/views/rag-task/index.vue'),
        meta: { title: '任务进度', icon: 'Clock', permission: 'agent:read' }
      },
      {
        path: 'tasks',
        name: 'Tasks',
        component: () => import('@/views/task/index.vue'),
        meta: { title: '任务策略', icon: 'List', permission: 'workflow:read' }
      },
      {
        path: 'xxl',
        name: 'XxlAdmin',
        component: () => import('@/views/xxl/index.vue'),
        meta: { title: '任务中心', icon: 'Timer', permission: 'workflow:read' }
      },
      {
        path: 'mcp-servers',
        name: 'McpServers',
        component: () => import('@/views/mcp/index.vue'),
        meta: { title: 'MCP 配置', icon: 'Link', permission: 'tool:read' }
      },
      {
        path: 'gateway-tools',
        name: 'GatewayTools',
        component: () => import('@/views/gateway/tools/index.vue'),
        meta: { title: '网关工具', icon: 'Operation', permission: 'tool:read' }
      },
      {
        path: 'gateway-tools/:gatewayId/tools',
        name: 'GatewayToolList',
        component: () => import('@/views/gateway/tools/index.vue'),
        meta: { title: '工具配置', icon: 'Tools', hidden: true, permission: 'tool:read' }
      },
      {
        path: 'users',
        name: 'Users',
        component: () => import('@/views/user/index.vue'),
        meta: { title: '用户管理', icon: 'User', permission: 'user:read' }
      },
      {
        path: 'roles',
        name: 'Roles',
        component: () => import('@/views/role/index.vue'),
        meta: { title: '角色管理', icon: 'Avatar', permission: 'role:read' }
      },
      {
        path: 'orgs',
        name: 'Orgs',
        component: () => import('@/views/org/index.vue'),
        meta: { title: '组织管理', icon: 'OfficeBuilding', permission: 'user:read' }
      },
      {
        path: 'mcp-credentials',
        name: 'McpCredentials',
        component: () => import('@/views/gateway/credential/index.vue'),
        meta: { title: '凭证管理', icon: 'Key', hidden: true, permission: 'tool:read' }
      },
      {
        path: 'audit-events',
        name: 'AuditEvents',
        component: () => import('@/views/audit-event/index.vue'),
        meta: { title: '身份审计', icon: 'Tickets', permission: 'audit:read' }
      },
      {
        path: 'audit',
        name: 'Audit',
        component: () => import('@/views/audit/index.vue'),
        meta: { title: '审计日志', icon: 'Document', permission: 'audit:read' }
      }
    ]
  },
  {
    path: '/',
    redirect: '/dashboard'
  }
]

export default routes
