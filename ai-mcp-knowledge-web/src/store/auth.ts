import { defineStore } from 'pinia'
import { getAuthMe, loginAuth, logoutAuth } from '@/api/auth'
import type { AuthLoginResult, AuthProfile } from '@/types/entity'
import {
  clearAuthStorage,
  getAuthProfileFromStorage,
  getAuthToken,
  saveAuthProfile,
  saveAuthToken
} from '@/utils/auth'

interface LoginPayload {
  tenantId?: string
  username: string
  password: string
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    tokenName: '',
    tokenValue: '',
    tokenTimeout: undefined as number | undefined,
    profile: null as AuthProfile | null,
    initialized: false
  }),

  getters: {
    isLoggedIn: (state) => Boolean(state.tokenName && state.tokenValue),
    hasPermission: (state) => (permission: string) => {
      if (state.profile?.superAdmin) {
        return true
      }
      const permissions = state.profile?.permissions || []
      return permissions.includes(permission)
    }
  },

  actions: {
    initialize() {
      if (this.initialized) {
        return
      }
      const token = getAuthToken()
      const profile = getAuthProfileFromStorage()
      if (token) {
        this.tokenName = token.tokenName
        this.tokenValue = token.tokenValue
        this.tokenTimeout = token.tokenTimeout
      }
      this.profile = profile
      this.initialized = true
    },

    setLogin(loginResult: AuthLoginResult) {
      this.tokenName = loginResult.tokenName
      this.tokenValue = loginResult.tokenValue
      this.tokenTimeout = loginResult.tokenTimeout
      this.profile = loginResult.profile
      saveAuthToken({
        tokenName: loginResult.tokenName,
        tokenValue: loginResult.tokenValue,
        tokenTimeout: loginResult.tokenTimeout
      })
      saveAuthProfile(loginResult.profile)
    },

    setProfile(profile: AuthProfile) {
      this.profile = profile
      saveAuthProfile(profile)
    },

    clearAuth() {
      this.tokenName = ''
      this.tokenValue = ''
      this.tokenTimeout = undefined
      this.profile = null
      clearAuthStorage()
    },

    async login(payload: LoginPayload) {
      const res = await loginAuth(payload)
      this.setLogin(res.data)
      return res.data
    },

    async fetchProfile() {
      const res = await getAuthMe()
      this.setProfile(res.data)
      return res.data
    },

    async logout() {
      try {
        await logoutAuth()
      } finally {
        this.clearAuth()
      }
    }
  }
})
