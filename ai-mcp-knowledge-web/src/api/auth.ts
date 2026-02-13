import request from '@/utils/request'
import type { AuthLoginResult, AuthProfile } from '@/types/entity'

export const loginAuth = (data: { username: string; password: string }) => request.post<AuthLoginResult>('/auth/login', data)

export const logoutAuth = () => request.post<void>('/auth/logout')

export const getAuthMe = () => request.get<AuthProfile>('/auth/me')
