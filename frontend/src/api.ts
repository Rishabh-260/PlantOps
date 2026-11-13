const BASE = import.meta.env.VITE_API_BASE || '/api'
let token = localStorage.getItem('plantops-token') || ''
export function setToken(value: string) { token = value; if (value) localStorage.setItem('plantops-token', value); else localStorage.removeItem('plantops-token') }
export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const response = await fetch(`${BASE}${path}`, { ...options, headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}), ...options.headers } })
  if (!response.ok) { const body = await response.json().catch(() => null); throw new Error(body?.message || body?.detail || body?.error || `Request failed (${response.status})`) }
  if (response.status === 204) return undefined as T
  return response.json()
}
