import { Link } from 'react-router-dom'

// Brand mark — uses CSS tokens so it respects the active theme.
export function BrandMark({ size = 20 }) {
  return (
    <Link to="/" className="d-inline-flex align-items-center gap-2 text-decoration-none" style={{ color: 'var(--cf-text)' }}>
      <i className="bi bi-bank2" style={{ fontSize: size }} />
      <span className="fw-semibold" style={{ fontSize: size * 0.9 }}>CaseFlow</span>
    </Link>
  )
}

// Centered minimal auth layout with a pastel mesh background and a
// persistent "back to home" link in the top-left corner.
export default function AuthLayout({ title, subtitle, children, footer }) {
  return (
    <div className="cf-auth-bg d-flex align-items-center justify-content-center position-relative">
      {/* Back to home — visible on every auth page */}
      <Link
        to="/"
        className="position-absolute d-inline-flex align-items-center gap-2 px-3 py-2 rounded-pill"
        style={{
          top: 20, left: 20,
          background: 'var(--cf-surface)',
          border: '1px solid var(--cf-border)',
          color: 'var(--cf-text-soft)',
          fontSize: 13,
          boxShadow: 'var(--cf-shadow-xs)',
          transition: 'transform 150ms ease, box-shadow 150ms ease',
        }}
        onMouseEnter={e => { e.currentTarget.style.transform = 'translateX(-2px)'; e.currentTarget.style.boxShadow = 'var(--cf-shadow-sm)' }}
        onMouseLeave={e => { e.currentTarget.style.transform = 'translateX(0)';  e.currentTarget.style.boxShadow = 'var(--cf-shadow-xs)' }}
      >
        <i className="bi bi-arrow-left" />
        <span className="d-none d-sm-inline">Back to home</span>
        <span className="d-sm-none">Home</span>
      </Link>

      <div className="cf-card p-4" style={{ width: '100%', maxWidth: 420, margin: '1rem', boxShadow: 'var(--cf-shadow-lg)' }}>
        <div className="mb-4 text-center">
          <BrandMark size={26} />
        </div>
        <h1 className="h4 mb-1 fw-semibold">{title}</h1>
        {subtitle && <p className="cf-muted small mb-4">{subtitle}</p>}
        {children}
        {footer && <div className="mt-3 small text-center cf-muted">{footer}</div>}
      </div>
    </div>
  )
}
