import { Link } from 'react-router-dom'

const HIGHLIGHTS = [
  { icon: 'bi-diagram-3',   title: 'Microservices', desc: '11 independent Spring Boot services, each owning its domain.' },
  { icon: 'bi-shield-lock', title: 'Secure by default', desc: 'JWT auth, BCrypt hashing and role-based authorization on every endpoint.' },
  { icon: 'bi-broadcast',   title: 'Event-driven', desc: 'Services communicate via REST and an internal event bus for near-real-time updates.' },
  { icon: 'bi-database',    title: 'Per-service DB', desc: 'Each service owns its own database — independently deployable and scalable.' },
]

export default function About() {
  return (
    <section className="container py-5">
      <span className="badge text-bg-light border mb-3 px-3 py-2 small">About the project</span>
      <h1 className="h2 fw-semibold mb-3">Modern infrastructure for legal case management</h1>
      <p className="cf-muted" style={{ maxWidth: 720 }}>
        CaseFlow digitizes the entire case lifecycle — filing, document verification, hearing scheduling,
        appeals, compliance and audits — through a clean microservice architecture built with Spring Boot
        and React.
      </p>

      <div className="row g-3 mt-3">
        <div className="col-md-6">
          <div className="cf-card p-4 h-100">
            <h5 className="fw-semibold mb-2">The problem</h5>
            <p className="cf-muted small mb-0">
              Traditional case management relies on paper records, manual scheduling and ad-hoc compliance
              checks. SLA breaches go unnoticed, audits are slow and stakeholders are kept in the dark.
            </p>
          </div>
        </div>
        <div className="col-md-6">
          <div className="cf-card p-4 h-100">
            <h5 className="fw-semibold mb-2">The solution</h5>
            <p className="cf-muted small mb-0">
              A unified platform that automates workflow progression, monitors SLAs in real time, sends
              alerts to every stakeholder, and produces an immutable audit trail of every action taken.
            </p>
          </div>
        </div>
      </div>

      <h3 className="h5 fw-semibold mt-5 mb-3">Architectural highlights</h3>
      <div className="row g-3">
        {HIGHLIGHTS.map(h => (
          <div className="col-md-6 col-lg-3" key={h.title}>
            <div className="cf-card p-3 h-100">
              <i className={`bi ${h.icon} fs-5 mb-2 d-block`} />
              <div className="fw-semibold small mb-1">{h.title}</div>
              <div className="cf-muted small">{h.desc}</div>
            </div>
          </div>
        ))}
      </div>

      <div className="mt-5 d-flex gap-2 flex-wrap">
        <Link to="/how-it-works" className="btn btn-dark">Explore the architecture</Link>
        <Link to="/register" className="btn btn-outline-dark">Try the platform</Link>
      </div>
    </section>
  )
}
