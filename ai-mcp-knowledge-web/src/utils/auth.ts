import type { AuthProfile } from '@/types/entity'

const TOKEN_NAME_KEY = 'auth_token_name'
const TOKEN_VALUE_KEY = 'auth_token_value'
const TOKEN_TIMEOUT_KEY = 'auth_token_timeout'
const PROFILE_KEY = 'auth_profile'

export interface StoredAuthToken {
  tokenName: string
  tokenValue: string
  tokenTimeout?: number
}

export const saveAuthToken = (token: StoredAuthToken) => {
  localStorage.setItem(TOKEN_NAME_KEY, token.tokenName)
  localStorage.setItem(TOKEN_VALUE_KEY, token.tokenValue)
  if (token.tokenTimeout !== undefined) {
    localStorage.setItem(TOKEN_TIMEOUT_KEY, String(token.tokenTimeout))
  } else {
    localStorage.removeItem(TOKEN_TIMEOUT_KEY)
  }
}

export const getAuthToken = (): StoredAuthToken | null => {
  const tokenName = localStorage.getItem(TOKEN_NAME_KEY)
  const tokenValue = localStorage.getItem(TOKEN_VALUE_KEY)
  if (!tokenName || !tokenValue) {
    return null
  }
  const timeoutRaw = localStorage.getItem(TOKEN_TIMEOUT_KEY)
  const tokenTimeout = timeoutRaw ? Number(timeoutRaw) : undefined
  return {
    tokenName,
    tokenValue,
    tokenTimeout: Number.isNaN(tokenTimeout) ? undefined : tokenTimeout
  }
}

export const saveAuthProfile = (profile: AuthProfile) => {
  localStorage.setItem(PROFILE_KEY, JSON.stringify(profile))
}

export const getAuthProfileFromStorage = (): AuthProfile | null => {
  const raw = localStorage.getItem(PROFILE_KEY)
  if (!raw) {
    return null
  }
  try {
    return JSON.parse(raw) as AuthProfile
  } catch (error) {
    localStorage.removeItem(PROFILE_KEY)
    return null
  }
}

export const clearAuthStorage = () => {
  localStorage.removeItem(TOKEN_NAME_KEY)
  localStorage.removeItem(TOKEN_VALUE_KEY)
  localStorage.removeItem(TOKEN_TIMEOUT_KEY)
  localStorage.removeItem(PROFILE_KEY)
}
