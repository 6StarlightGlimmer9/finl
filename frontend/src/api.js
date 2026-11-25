const STORAGE_KEY = 'screenroom-user'

export function getStoredUser() {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null')
  } catch {
    return null
  }
}

export function setStoredUser(user) {
  if (user) localStorage.setItem(STORAGE_KEY, JSON.stringify(user))
  else localStorage.removeItem(STORAGE_KEY)
}

export function clearStoredUser() {
  localStorage.removeItem(STORAGE_KEY)
}

export function getToken() {
  return getStoredUser()?.token || ''
}

export function isAdmin() {
  return getStoredUser()?.role === 'ADMIN'
}

export function showToast(message, type = 'success') {
  window.dispatchEvent(new CustomEvent('screenroom:toast', { detail: { message, type } }))
}

export async function apiFetch(path, options = {}) {
  const user = getStoredUser()
  const headers = { ...(options.headers || {}) }
  if (user?.token) headers.Authorization = `Bearer ${user.token}`
  const hasBody = options.body !== undefined && options.body !== null
  const hasContentType = Object.keys(headers).some(key => key.toLowerCase() === 'content-type')
  if (hasBody && !(typeof FormData !== 'undefined' && options.body instanceof FormData) && !hasContentType) {
    headers['Content-Type'] = 'application/json'
  }
  const response = await fetch(path, { ...options, headers })
  if (response.status === 401 && !path.includes('/auth/login') && !path.includes('/auth/register')) {
    clearStoredUser()
    window.dispatchEvent(new CustomEvent('screenroom:auth-expired'))
  }
  return response
}

export async function apiJson(path, options = {}) {
  const response = await apiFetch(path, options)
  const text = await response.text()
  let data = {}
  try { data = text ? JSON.parse(text) : {} } catch { /* 非 JSON 响应保留空对象 */ }
  if (!response.ok) throw new Error(data.message || data.error || `请求失败（HTTP ${response.status}）`)
  return data
}
