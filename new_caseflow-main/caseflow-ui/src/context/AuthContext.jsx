import { createContext, useContext, useEffect, useState } from 'react'
import { setToken, setUser, getUser, clearAuth } from '../api/client'
import { auth as authApi } from '../api/services'

const AuthContext = createContext(null)

export function AuthProvider({ children }) {
  const [user, setUserState] = useState(() => getUser())
  const [loading, setLoading] = useState(false)

  const login = async (email, password, rememberMe = false) => {
    setLoading(true)
    try {
      const res = await authApi.login({ email, password }, rememberMe)
      setToken(res.token)
      const u = { email: res.email, name: res.name, role: res.role, userId: res.userId }
      setUser(u)
      setUserState(u)
      return res
    } finally {
      setLoading(false)
    }
  }

  const register = async (data) => {
    return authApi.register(data)
  }

  const logout = async () => {
    try {
      if (user?.email) await authApi.logout(user.email)
    } catch { /* ignore */ }
    clearAuth()
    setUserState(null)
  }

  useEffect(() => {
    // Sync user if changed elsewhere
    setUserState(getUser())
  }, [])

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, setUser: (u) => { setUser(u); setUserState(u) } }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth must be used within AuthProvider')
  return ctx
}
