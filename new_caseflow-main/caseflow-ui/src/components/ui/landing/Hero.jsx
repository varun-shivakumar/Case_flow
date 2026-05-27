import { Link } from 'react-router-dom'
import Eyebrow from './Eyebrow'
import StatCard from './StatCard'

export default function Hero({ stats = [] }) {
  return (
    <section className="cf-hero-bg">
      <div className="container py-5">
        <div className="row py-3">
          <div className="col-lg-8">
            <div className="mb-3">
              <Eyebrow>
                <i className="bi bi-bank2" />
                Legal Case Management
              </Eyebrow>
            </div>
            <h1 className="display-5 fw-semibold mb-3" style={{ letterSpacing: '-0.03em', lineHeight: 1.1 }}>
              Justice deserves<br />modern tools.
            </h1>
            <p className="cf-muted mb-4" style={{ maxWidth: 560, fontSize: 16, lineHeight: 1.65 }}>
              File cases, verify documents, schedule hearings, and track progress — all in one place. Caseflow is a free, open-source case management system built for courts and legal aid organisations.
            </p>
            <div className="d-flex gap-2 flex-wrap">
              <Link to="/register" className="btn btn-dark px-4">
                Get started <i className="bi bi-arrow-right ms-1" />
              </Link>
              <Link to="/how-it-works" className="btn btn-outline-dark px-4">How it works</Link>
            </div>

            <div className="d-flex gap-4 mt-4 flex-wrap">
              {stats.map(s => <StatCard key={s.label} variant="plain" value={s.value} label={s.label} />)}
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
