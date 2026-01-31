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
        meta: { title: '模型管理', icon: 'Setting' }
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
      },
      {
        path: 'playground',
        name: 'Playground',
        component: () => import('@/views/playground/index.vue'),
        meta: { title: '调试演练', icon: 'ChatDotRound' }
      }
    ]
  },
  {
    path: '/',
    redirect: '/dashboard'
  }
]

export default routes
