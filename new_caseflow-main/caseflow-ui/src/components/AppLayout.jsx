import { Link, NavLink, Outlet, useNavigate, useLocation } from 'react-router-dom'
import { useEffect, useState, useRef } from 'react'
import { useAuth } from '../context/AuthContext'
import { useNotificationCount } from '../hooks/useNotificationCount'
import { notify } from './ui/Toast'
import ThemeToggle from './ui/ThemeToggle'

// Role-based navigation grouped by domain. Items filter themselves by user role.
const NAV_GROUPS = [



  { label: 'Overview', items: [
    { to: '/dashboard', label: 'Dashboard', icon: 'bi-speedometer2', roles: ['ADMIN','CLERK','JUDGE','LAWYER','LITIGANT'] },
  ]},

     { label: 'Admin', items: [
          { to: '/users',            label: 'Users',      icon: 'bi-people',       roles: ['ADMIN'] },
          { to: '/users/audit-logs', label: 'Audit logs', icon: 'bi-journal-text', roles: ['ADMIN'] },
        ]},

  { label: 'Cases', items: [
    { to: '/cases',                    label: 'All cases',    icon: 'bi-folder2',          roles: ['ADMIN','CLERK','JUDGE','LAWYER','LITIGANT'] },
    { to: '/cases/file',               label: 'File case',    icon: 'bi-plus-square',      roles: ['LITIGANT'] },
    { to: '/cases/documents/pending',  label: 'Pending docs', icon: 'bi-file-earmark-check', roles: ['CLERK'] },
  ]},
  { label: 'Hearings', items: [
    { to: '/hearings',          label: 'All hearings', icon: 'bi-calendar2', roles: ['ADMIN','CLERK','JUDGE','LAWYER','LITIGANT'] },
    { to: '/hearings/schedule', label: 'Schedule',     icon: 'bi-calendar2-plus', roles: ['CLERK'] },
  ]},
  { label: 'Appeals', items: [
    { to: '/appeals',               label: 'All appeals', icon: 'bi-arrow-counterclockwise', roles: ['ADMIN','CLERK','JUDGE','LAWYER','LITIGANT'] },
    { to: '/appeals/file',          label: 'File appeal', icon: 'bi-pencil-square', roles: ['LITIGANT','LAWYER'] },
    { to: '/appeals/reviews/judge', label: 'Lookup judge', icon: 'bi-search', roles: ['CLERK'] },
  ]},
  { label: 'Workflow', items: [
    { to: '/workflow',     label: 'Workflow', icon: 'bi-diagram-3', roles: ['ADMIN','CLERK','JUDGE'] },
    { to: '/workflow/sla', label: 'SLA monitoring', icon: 'bi-stopwatch', roles: ['ADMIN','CLERK'] },
  ]},
  { label: 'Governance', items: [
    { to: '/compliance', label: 'Compliance', icon: 'bi-shield-check',    roles: ['ADMIN','CLERK'] },
    { to: '/audits',     label: 'Audits',     icon: 'bi-clipboard-data',  roles: ['ADMIN','CLERK'] },
    { to: '/reports',    label: 'Reports',    icon: 'bi-bar-chart',       roles: ['ADMIN','CLERK'] },
  ]},
]

export default function AppLayout() {
  const { user, logout } = useAuth()
  const navigate         = useNavigate()
  const location         = useLocation()
  const unread           = useNotificationCount(user, location.pathname)

  const [profileOpen, setProfileOpen] = useState(false)
  const [drawerOpen, setDrawerOpen]   = useState(false)
  const profileRef = useRef(null)

  useEffect(() => { setProfileOpen(false); setDrawerOpen(false) }, [location])
  useEffect(() => {
    const onClick = (e) => { if (profileRef.current && !profileRef.current.contains(e.target)) setProfileOpen(false) }
    document.addEventListener('mousedown', onClick)
    return () => document.removeEventListener('mousedown', onClick)
  }, [])

  const handleLogout = async () => {
    await logout()
    notify.info('Signed out')
    navigate('/login')
  }

  // Filter to groups the user can actually see
  const groups = NAV_GROUPS
    .map(g => ({
      ...g,
      items: g.items
        .filter(n => n.roles.includes(user?.role))
        .map(item => ({
          ...item,
          label: item.to === '/hearings' && user?.role === 'JUDGE' ? 'My hearings' : item.label
        }))
    }))
    .filter(g => g.items.length > 0)

  // A nav item needs `end` when another visible item is a direct sub-path of it,
  // so React Router doesn't highlight the parent while the child is active.
  const allVisibleTos = groups.flatMap(g => g.items.map(i => i.to))
  const needsEnd = (to) =>
    to === '/dashboard' || allVisibleTos.some(t => t !== to && t.startsWith(to + '/'))

  const initials = (user?.name || 'U').split(' ').map(w => w[0]).join('').toUpperCase().slice(0, 2)

  // Sidebar nav — used both in desktop sidebar and mobile drawer
  const SidebarNav = (
    <div className="d-flex flex-column p-2" style={{ minHeight: '100%' }}>
      <Link to="/dashboard" className="d-flex align-items-center gap-2 px-2 py-3 text-decoration-none" style={{ color: 'var(--cf-text)' }}>
        <i className="bi bi-bank2 fs-5" />
        <span className="fw-semibold">CaseFlow</span>
      </Link>

      <div className="flex-grow-1">
        {groups.map(g => (
          <div key={g.label} className="mb-2">
            <div className="cf-nav-section">{g.label}</div>
            {g.items.map(item => (
              <NavLink
                key={item.to}
                to={item.to}
                end={needsEnd(item.to)}
                className={({ isActive }) => `cf-nav-link ${isActive ? 'active' : ''}`}
              >
                <i className={`bi ${item.icon}`} />
                <span>{item.label}</span>
                {item.to === '/notifications' && unread > 0 && (
                  <span className="badge text-bg-danger rounded-pill ms-auto">{unread}</span>
                )}
              </NavLink>
            ))}
          </div>
        ))}

        {/* Notifications - hide for ADMIN role */}
        {user?.role !== 'ADMIN' && (
          <div className="mb-2">
            <div className="cf-nav-section">Communication</div>
            <NavLink to="/notifications" className={({ isActive }) => `cf-nav-link ${isActive ? 'active' : ''}`}>
              <i className="bi bi-bell" />
              <span>Notifications</span>
              {unread > 0 && <span className="badge text-bg-danger rounded-pill ms-auto">{unread > 99 ? '99+' : unread}</span>}
            </NavLink>
          </div>
        )}
      </div>
    </div>
  )

  return (
    <div className="cf-shell">
      {/* Desktop sidebar */}
      <aside className="cf-sidebar">{SidebarNav}</aside>

      {/* Mobile drawer */}
      {drawerOpen && (
        <>
          <div className="position-fixed top-0 start-0 w-100 h-100" style={{ background: 'rgba(0,0,0,0.4)', zIndex: 1040 }} onClick={() => setDrawerOpen(false)} />
          <aside className="position-fixed top-0 start-0 h-100" style={{ width: 260, zIndex: 1045, overflowY: 'auto', background: 'var(--cf-surface)' }}>{SidebarNav}</aside>
        </>
      )}

      <div className="cf-main d-flex flex-column">
        {/* Topbar */}
        <header className="cf-topbar px-3 py-2 d-flex align-items-center justify-content-between">
          <div className="d-flex align-items-center gap-2">
            <button className="btn btn-sm btn-light d-md-none" onClick={() => setDrawerOpen(true)}>
              <i className="bi bi-list fs-5" />
            </button>
          </div>

          <div className="d-flex align-items-center gap-2">
            <ThemeToggle />
            {user?.role !== 'ADMIN' && (
              <Link to="/notifications" className="btn btn-sm btn-light position-relative">
                <i className="bi bi-bell" />
                {unread > 0 && (
                  <span className="position-absolute top-0 start-100 translate-middle badge rounded-pill text-bg-danger" style={{ fontSize: 9 }}>
                    {unread > 99 ? '99+' : unread}
                  </span>
                )}
              </Link>
            )}

            <div className="position-relative" ref={profileRef}>
              <button className="btn btn-sm btn-light d-flex align-items-center gap-2" onClick={() => setProfileOpen(!profileOpen)}>
                <span className="d-inline-flex align-items-center justify-content-center rounded-circle text-bg-dark"
                  style={{ width: 24, height: 24, fontSize: 11, fontWeight: 600 }}>
                  {initials}
                </span>
                <span className="d-none d-md-inline small">{user?.name}</span>
                <i className={`bi bi-chevron-${profileOpen ? 'up' : 'down'} small`} />
              </button>
              {profileOpen && (
                <div className="position-absolute end-0 mt-1 cf-card shadow-sm" style={{ minWidth: 220, zIndex: 50 }}>
                  <div className="p-3 border-bottom">
                    <div className="fw-semibold small">{user?.name}</div>
                    <div className="cf-muted small">{user?.email}</div>
                    <span className="badge text-bg-light border mt-2 small">{user?.role}</span>
                  </div>
                  <Link to="/change-password" className="d-block px-3 py-2 small text-dark text-decoration-none">
                    <i className="bi bi-key me-2" />Change password
                  </Link>
                  <button onClick={handleLogout} className="btn btn-sm w-100 text-start px-3 py-2 small text-danger border-0">
                    <i className="bi bi-box-arrow-right me-2" />Sign out
                  </button>
                </div>
              )}
            </div>
          </div>
        </header>

        <main className="cf-content flex-grow-1">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
