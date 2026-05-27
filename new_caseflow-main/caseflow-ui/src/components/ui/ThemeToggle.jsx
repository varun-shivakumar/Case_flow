import { useTheme } from '../../context/ThemeContext'

// Round icon button that flips between light and dark mode.
// Used in both the public Navbar and the authenticated AppLayout topbar.
export default function ThemeToggle({ className = '' }) {
  const { theme, toggle } = useTheme()
  const isDark = theme === 'dark'
  return (
    <button
      type="button"
      onClick={toggle}
      className={`cf-theme-toggle ${className}`}
      aria-label={`Switch to ${isDark ? 'light' : 'dark'} mode`}
      title={`Switch to ${isDark ? 'light' : 'dark'} mode`}
    >
      <i className={`bi ${isDark ? 'bi-sun' : 'bi-moon-stars'}`} />
    </button>
  )
}
