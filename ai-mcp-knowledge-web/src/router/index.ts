import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store/auth'
import routes from './routes'

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
    const redirect = typeof to.query.redirect === 'string' ? to.query.redirect : '/dashboard'
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
    return '/dashboard'
  }

  return true
})

export default router
