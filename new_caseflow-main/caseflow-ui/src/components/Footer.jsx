import { Link } from 'react-router-dom'

const COLUMNS = [
  { heading: 'Product', links: [
    { to: '/how-it-works', label: 'How It Works' },
    { to: '/about',        label: 'About' },
    { to: '/contact',      label: 'Contact' },
  ]},
  { heading: 'Modules', links: [
    { to: '/cases',    label: 'Case Filing' },
    { to: '/hearings', label: 'Hearings' },
    { to: '/workflow', label: 'Workflow' },
    { to: '/appeals',  label: 'Appeals' },
  ]},
]

export default function Footer() {
  return (
    <footer className="cf-footer mt-4">
      <div className="container py-4">
        <div className="row g-4 mb-3">
          <div className="col-12 col-md-5">
            <div className="d-flex align-items-center gap-2 mb-2">
              <i className="bi bi-bank2" />
              <span className="fw-semibold">CaseFlow</span>
            </div>
            <p className="cf-muted small mb-0" style={{ maxWidth: 280 }}>
              Modernizing legal case management with intelligent workflow automation.
            </p>
          </div>

          {COLUMNS.map(col => (
            <div className="col-6 col-md-3" key={col.heading}>
              <h6 className="small fw-semibold text-uppercase mb-2" style={{ letterSpacing: '0.06em' }}>{col.heading}</h6>
              <ul className="list-unstyled small d-flex flex-column gap-1">
                {col.links.map(l => (
                  <li key={l.to}><Link to={l.to} className="cf-muted">{l.label}</Link></li>
                ))}
              </ul>
            </div>
          ))}
        </div>
        <div className="pt-3 d-flex flex-column flex-md-row justify-content-between gap-1 small cf-muted"
          style={{ borderTop: '1px solid var(--cf-border)' }}>
          <span>© 2026 CaseFlow — Cognizant Internship Project</span>
          <span>Built with Spring Boot · React · Microservices</span>
        </div>
      </div>
    </footer>
  )
}
