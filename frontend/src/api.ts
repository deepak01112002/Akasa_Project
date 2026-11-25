import axios from 'axios'
export const api = axios.create({ baseURL: import.meta.env.VITE_API_URL || '/api' })
api.interceptors.request.use(config => { const token = localStorage.getItem('token'); if (token) config.headers.Authorization = `Bearer ${token}`; return config })
api.interceptors.response.use(response => response, error => { if (error.response?.status === 401 && !error.config?.url?.startsWith('/auth/')) { localStorage.removeItem('token'); localStorage.removeItem('user'); window.location.assign('/login') } return Promise.reject(error) })
export type User = { id: number; name: string; email: string }
export type Project = { id: number; name: string; description: string | null; owner: User }
export type Status = 'TODO' | 'IN_PROGRESS' | 'DONE'
export type Priority = 'LOW' | 'MEDIUM' | 'HIGH'
export type Task = { id: number; title: string; description: string | null; status: Status; priority: Priority; dueDate: string | null; assignee: User | null }
