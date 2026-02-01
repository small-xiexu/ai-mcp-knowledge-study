import type { RouteRecordRaw } from 'vue-router'

const routes: RouteRecordRaw[] = [
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
        meta: { title: 'LLM 配置', icon: 'Setting' }
      },
      {
        path: 'ai-chat',
        name: 'AiChat',
        component: () => import('@/views/chat/index.vue'),
        meta: { title: 'AI 对话', icon: 'ChatDotRound' }
      },
      {
        path: 'knowledge',
        name: 'Knowledge',
        component: () => import('@/views/knowledge/index.vue'),
        meta: { title: '知识库管理', icon: 'Collection' }
      },
      {
        path: 'rag-tasks',
        name: 'RagTasks',
        component: () => import('@/views/rag-task/index.vue'),
        meta: { title: '任务进度', icon: 'Clock' }
      },
      {
        path: 'tasks',
        name: 'Tasks',
        component: () => import('@/views/task/index.vue'),
        meta: { title: '任务策略', icon: 'List' }
      },
      {
        path: 'mcp-servers',
        name: 'McpServers',
        component: () => import('@/views/mcp/index.vue'),
        meta: { title: 'MCP 配置', icon: 'Link' }
      },
      {
        path: 'audit',
        name: 'Audit',
        component: () => import('@/views/audit/index.vue'),
        meta: { title: '审计日志', icon: 'Document' }
      }
    ]
  },
  {
    path: '/',
    redirect: '/dashboard'
  }
]

export default routes
