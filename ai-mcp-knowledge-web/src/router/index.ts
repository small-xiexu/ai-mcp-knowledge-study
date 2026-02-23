import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import routes from './routes'

const APP_TITLE = '多智能体调度平台'

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const authStore = useAuthStore()
  authStore.initialize()

  if (to.path === '/login') {
    if (!authStore.isLoggedIn) {
      return true
    }
    if (!authStore.profile) {
      try {
        await authStore.fetchProfile()
      } catch (error) {
        authStore.clearAuth()
        return true
      }
    }
    const redirect = typeof to.query.redirect === 'string' ? to.query.redirect : '/workbench'
    return redirect
  }

  if (!authStore.isLoggedIn) {
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    }
  }

  if (!authStore.profile) {
    try {
      await authStore.fetchProfile()
    } catch (error) {
      authStore.clearAuth()
      return {
        path: '/login',
        query: { redirect: to.fullPath }
      }
    }
  }

  const requiredPermission = typeof to.meta.permission === 'string' ? to.meta.permission : ''
  if (requiredPermission && !authStore.hasPermission(requiredPermission)) {
    return '/workbench'
  }

  return true
})

router.afterEach((to) => {
  const routeTitle = typeof to.meta.title === 'string' ? to.meta.title.trim() : ''
  document.title = routeTitle ? `${routeTitle} - ${APP_TITLE}` : APP_TITLE
})

export default router
