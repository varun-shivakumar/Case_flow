import { createContext, useContext, useEffect, useState } from 'react'

const ThemeContext = createContext(null)

// Applies the chosen theme to <html> for both our custom tokens and Bootstrap 5.3
// native color-mode switching.
function applyTheme(theme) {
  const el = document.documentElement
  el.setAttribute('data-theme', theme)
  el.setAttribute('data-bs-theme', theme)
}

export function ThemeProvider({ children }) {
  const [theme, setTheme] = useState(() => localStorage.getItem('cf-theme') || 'light')

  useEffect(() => {
    localStorage.setItem('cf-theme', theme)
    applyTheme(theme)
  }, [theme])

  const toggle = () => setTheme(t => t === 'light' ? 'dark' : 'light')

  return (
    <ThemeContext.Provider value={{ theme, setTheme, toggle }}>
      {children}
    </ThemeContext.Provider>
  )
}

export function useTheme() {
  const ctx = useContext(ThemeContext)
  if (!ctx) throw new Error('useTheme must be used within ThemeProvider')
  return ctx
}
