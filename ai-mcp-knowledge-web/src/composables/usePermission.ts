import { computed } from 'vue'
import { useAuthStore } from '@/store/auth'

export const usePermission = () => {
  const authStore = useAuthStore()

  const permissions = computed(() => authStore.profile?.permissions || [])
  const isSuperAdmin = computed(() => Boolean(authStore.profile?.superAdmin))

  const hasPermission = (permission: string) => {
    if (!permission) {
      return true
    }
    if (isSuperAdmin.value) {
      return true
    }
    return permissions.value.includes(permission)
  }

  const hasAnyPermission = (required: string[]) => {
    if (!required || required.length === 0) {
      return true
    }
    return required.some(item => hasPermission(item))
  }

  const hasAllPermissions = (required: string[]) => {
    if (!required || required.length === 0) {
      return true
    }
    return required.every(item => hasPermission(item))
  }

  return {
    hasPermission,
    hasAnyPermission,
    hasAllPermissions,
    permissions,
    isSuperAdmin
  }
}
