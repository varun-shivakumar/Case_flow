import axios from 'axios'

const BASE_URL = import.meta.env.VITE_API_URL ?? ''

// ── Auth storage helpers ──────────────────────────────────────────────────────

export function setToken(token) {
  if (token) localStorage.setItem('cf_token', token)
  else localStorage.removeItem('cf_token')
}

export function setUser(user) {
  if (user) localStorage.setItem('cf_user', JSON.stringify(user))
  else localStorage.removeItem('cf_user')
}

export function getUser() {
  const raw = localStorage.getItem('cf_user')
  return raw ? JSON.parse(raw) : null
}

export function clearAuth() {
  localStorage.removeItem('cf_token')
  localStorage.removeItem('cf_user')
}

// ── Axios instance ────────────────────────────────────────────────────────────

const client = axios.create({ baseURL: BASE_URL })

// Attach JWT to every request automatically
client.interceptors.request.use(config => {
  const token = localStorage.getItem('cf_token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

// Normalize backend error messages into a plain Error
client.interceptors.response.use(
  res => res,
  err => {
    const data = err.response?.data
    const msg =
      (data && (data.message || data.error)) ||
      (typeof data === 'string' && data) ||
      `Request failed (${err.response?.status ?? 'network'})`
    const e = new Error(msg)
    e.status = err.response?.status
    e.data = data
    throw e
  }
)

// ── Public API ────────────────────────────────────────────────────────────────

export const api = {
  get:      (path, params, opts) => client.get(path, { params, ...opts }).then(r => r.data),
  post:     (path, body, opts)   => client.post(path, body, opts).then(r => r.data),
  put:      (path, body, opts)   => client.put(path, body, opts).then(r => r.data),
  patch:    (path, body, opts)   => client.patch(path, body, opts).then(r => r.data),
  del:      (path, opts)         => client.delete(path, opts).then(r => r.data),
  postForm: (path, formData)     => client.post(path, formData).then(r => r.data),

  // Returns a fetch-compatible wrapper so download code (res.ok / res.blob()) works unchanged
  raw: async (path, opts) => {
    const res = await client.get(path, { responseType: 'blob', ...opts })
    return {
      ok:      res.status >= 200 && res.status < 300,
      status:  res.status,
      headers: { get: k => res.headers[k.toLowerCase()] },
      blob:    () => Promise.resolve(res.data),
    }
  },

  baseUrl: BASE_URL,
}
