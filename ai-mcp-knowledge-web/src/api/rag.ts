import request from '@/utils/request'
import type { PageRequest, PageResult } from '@/types/api'
import type { RagTask } from '@/types/entity'

export const listRagTags = () =>
  request.post<string[]>('/ai/rag/tags')

export const deleteRagTag = (ragTag: string) =>
  request.post<boolean>('/ai/rag/delete', null, { params: { ragTag } })

export const countRagTag = (ragTag: string) =>
  request.post<number>('/ai/rag/count', null, { params: { ragTag } })

export const uploadRagFiles = (ragTag: string, files: File[]) => {
  const formData = new FormData()
  formData.append('ragTag', ragTag)
  files.forEach(file => formData.append('file', file))
  return request.post<boolean>('/ai/rag/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

export const analyzeGitRepository = (data: {
  repoUrl: string
  userName?: string
  token?: string
  ragTag?: string
}) => request.post<string>('/ai/rag/analyze', data)

export const queryRagTask = (taskId: string) =>
  request.post<RagTask>('/ai/rag/task/progress', { taskId })

export const cancelRagTask = (taskId: string) =>
  request.post<boolean>('/ai/rag/task/cancel', { taskId })

export const listRagTasks = (data: PageRequest) =>
  request.post<PageResult<RagTask>>('/ai/rag/task/list', data)
