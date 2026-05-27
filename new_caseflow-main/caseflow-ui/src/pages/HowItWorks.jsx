import { Link } from 'react-router-dom'

const STEPS = [
  { n: '01', title: 'Register & sign in',  desc: 'Litigants self-register; lawyers, judges, clerks and admins are provisioned by ADMIN.' },
  { n: '02', title: 'File a case',         desc: 'Litigants or clerks file a new case with title, parties and supporting documents.' },
  { n: '03', title: 'Document verification', desc: 'Clerks review uploaded documents and either verify or reject with a reason.' },
  { n: '04', title: 'Workflow initialization', desc: 'When all documents are verified, the case status moves to ACTIVE and SLA stages start.' },
  { n: '05', title: 'Schedule hearings',   desc: 'Clerks pick a judge and a conflict-free time slot. All parties are notified.' },
  { n: '06', title: 'SLA monitoring',      desc: 'Stages are tracked in real time. Warnings fire before deadlines; breaches are flagged.' },
  { n: '07', title: 'Appeals',             desc: 'Closed cases can be appealed within 90 days. Judges review and issue outcomes.' },
  { n: '08', title: 'Compliance & audit',  desc: 'Automated checks run at every critical stage. Admins maintain audit trails.' },
  { n: '09', title: 'Reports & analytics', desc: 'Role-specific KPIs across cases, hearings, appeals, compliance and SLA performance.' },
]

const ROLES = [
  { role: 'LITIGANT', capabilities: 'File cases, upload documents, view their own cases, file appeals.' },
  { role: 'LAWYER',   capabilities: 'File cases for clients, upload documents, view assigned cases, file appeals.' },
  { role: 'JUDGE',    capabilities: 'Schedule hearings, review appeals, issue decisions, view workflow.' },
  { role: 'CLERK',    capabilities: 'Verify documents, advance workflow, run compliance checks, manage hearings.' },
  { role: 'ADMIN',    capabilities: 'Full access — user management, audits, reports, system configuration.' },
]

const STACK = [
  { title: 'Backend',    items: ['Spring Boot 3', 'Spring Cloud Gateway', 'Eureka Discovery', 'Spring Security + JWT', 'JPA / Hibernate', 'MySQL'] },
  { title: 'Frontend',   items: ['React 18', 'React Router v6', 'Vite', 'Bootstrap 5', 'Axios', 'React Toastify'] },
  { title: 'Practices',  items: ['Per-service database', 'REST + event bus', 'Role-based authorization', 'Audit logging', 'SLA engine', 'OpenAPI docs'] },
]

export default function HowItWorks() {
  return (
    <section className="container py-5">
      <span className="badge text-bg-light border mb-3 px-3 py-2 small">How it works</span>
      <h1 className="h2 fw-semibold mb-3">The case lifecycle, end-to-end</h1>
      <p className="cf-muted mb-5" style={{ maxWidth: 720 }}>
        From the moment a case is filed to its final audit record, every step is tracked, timed and visible
        to authorised stakeholders.
      </p>

      <div className="row g-3 mb-5">
        {STEPS.map(s => (
          <div className="col-md-6 col-lg-4" key={s.n}>
            <div className="cf-card p-3 h-100">
              <div className="cf-muted small fw-semibold mb-1">{s.n}</div>
              <div className="fw-semibold small mb-1">{s.title}</div>
              <div className="cf-muted small">{s.desc}</div>
            </div>
          </div>
        ))}
      </div>

      <h3 className="h5 fw-semibold mb-3">User roles</h3>
      <div className="cf-card mb-5">
        <div className="table-responsive">
          <table className="table mb-0 small align-middle">
            <thead className="cf-muted">
              <tr><th className="py-2 ps-3">Role</th><th className="py-2">Capabilities</th></tr>
            </thead>
            <tbody>
              {ROLES.map(r => (
                <tr key={r.role}><td className="fw-semibold ps-3">{r.role}</td><td className="cf-muted">{r.capabilities}</td></tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>

      <h3 className="h5 fw-semibold mb-3">Tech stack</h3>
      <div className="row g-3 mb-5">
        {STACK.map(group => (
          <div className="col-md-4" key={group.title}>
            <div className="cf-card p-3 h-100">
              <div className="fw-semibold small mb-2">{group.title}</div>
              <ul className="list-unstyled small cf-muted mb-0 d-flex flex-column gap-1">
                {group.items.map(i => <li key={i}>{i}</li>)}
              </ul>
            </div>
          </div>
        ))}
      </div>

      <div className="cf-card p-4 text-center">
        <h4 className="h5 fw-semibold mb-2">See it in action</h4>
        <p className="cf-muted small mb-3">Create an account and explore the platform with sample data.</p>
        <Link to="/register" className="btn btn-dark">Get started</Link>
      </div>
    </section>
  )
}
