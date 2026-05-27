import { useState, useEffect } from 'react'
import { Link, useLocation, NavLink } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { BrandMark } from './ui/AuthLayout'
import ThemeToggle from './ui/ThemeToggle'

const LINKS = [
  { to: '/',             label: 'Home' },
  { to: '/about',        label: 'About' },
  { to: '/how-it-works', label: 'How It Works' },
  { to: '/contact',      label: 'Contact' },
]

export default function Navbar() {
  const { user } = useAuth()
  const [open, setOpen] = useState(false)
  const location = useLocation()

  // Close mobile menu on route change
  useEffect(() => { setOpen(false) }, [location])

  return (
    <nav className="cf-public-bar sticky-top" style={{ zIndex: 100 }}>
      <div className="container d-flex align-items-center justify-content-between py-3">
        <BrandMark size={22} />

        {/* Desktop links */}
        <div className="d-none d-md-flex align-items-center gap-4">
          {LINKS.map(l => (
            <NavLink
              key={l.to}
              to={l.to}
              end={l.to === '/'}
              className={({ isActive }) => `cf-public-link text-decoration-none ${isActive ? 'active' : ''}`}
            >
              {l.label}
            </NavLink>
          ))}
          <ThemeToggle />
          <Link to={user ? '/dashboard' : '/login'} className="btn btn-dark btn-sm px-3">
            {user ? 'Dashboard' : 'Sign In'}
          </Link>
        </div>

        {/* Mobile actions */}
        <div className="d-flex d-md-none align-items-center gap-2">
          <ThemeToggle />
          <button className="btn btn-sm" onClick={() => setOpen(!open)} aria-label="Menu" style={{ color: 'var(--cf-text)' }}>
            <i className={`bi ${open ? 'bi-x-lg' : 'bi-list'} fs-5`} />
          </button>
        </div>
      </div>

      {/* Mobile menu */}
      {open && (
        <div className="d-md-none cf-public-bar py-2">
          <div className="container d-flex flex-column gap-2">
            {LINKS.map(l => (
              <NavLink key={l.to} to={l.to} end={l.to === '/'}
                className={({ isActive }) => `cf-public-link text-decoration-none py-1 ${isActive ? 'active' : ''}`}>
                {l.label}
              </NavLink>
            ))}
            <Link to={user ? '/dashboard' : '/login'} className="btn btn-dark btn-sm align-self-start mt-1">
              {user ? 'Dashboard' : 'Sign In'}
            </Link>
          </div>
        </div>
      )}
    </nav>
  )
}
