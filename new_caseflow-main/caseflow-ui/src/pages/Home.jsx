import { Link } from 'react-router-dom'
import Hero          from '../components/ui/landing/Hero'
import SectionTitle  from '../components/ui/landing/SectionTitle'
import FeatureCard   from '../components/ui/landing/FeatureCard'
import StatCard      from '../components/ui/landing/StatCard'
import StepCard      from '../components/ui/landing/StepCard'
import RoleCard      from '../components/ui/landing/RoleCard'

const HERO_STATS = [
  { value: '8',    label: 'Integrated modules' },
  { value: '5',    label: 'User roles' },
  { value: '<1s',  label: 'Notification delivery' },
]

const PROOF_STATS = [
  { value: '96%',   label: 'SLA adherence rate' },
  { value: '70%',   label: 'Faster than paper filing' },
  { value: '100%',  label: 'Audit coverage' },
  { value: '11',    label: 'Microservices' },
]

// Each feature cycles through pastel tones for visual rhythm.
const FEATURES = [
  { icon: 'bi-shield-lock',           title: 'Identity & access',  tone: 'blue',   desc: 'JWT auth with role-based access for litigants, lawyers, judges, clerks and admins.' },
  { icon: 'bi-folder2-open',          title: 'Case filing',        tone: 'purple', desc: 'Digital filing with document verification and automatic status transitions.' },
  { icon: 'bi-diagram-3',             title: 'Workflow & SLA',     tone: 'green',  desc: 'Automatic stage tracking, real-time SLA monitoring and breach detection.' },
  { icon: 'bi-calendar-event',        title: 'Hearings',           tone: 'peach',  desc: 'Judge calendars, conflict-free scheduling and automated party notifications.' },
  { icon: 'bi-arrow-counterclockwise', title: 'Appeals',           tone: 'pink',   desc: 'End-to-end appeal lifecycle from filing to decision with full transparency.' },
  { icon: 'bi-shield-check',          title: 'Compliance & audit', tone: 'mint',   desc: 'Perform compliance checks for cases and start audits.' },
  { icon: 'bi-bell',                  title: 'Notifications',      tone: 'rose',   desc: 'Real-time alerts with per-user feeds, read tracking and bulk actions.' },
  { icon: 'bi-bar-chart-line',        title: 'Reports & analytics', tone: 'lemon', desc: 'Role-specific KPIs with multiple scopes and CSV export.' },
]

const STEPS = [
  { step: '01', title: 'File',     tone: 'blue',   desc: 'Litigants file a case with parties and supporting documents.' },
  { step: '02', title: 'Verify',   tone: 'purple', desc: 'Clerks review and verify each uploaded document or reject with a reason.' },
  { step: '03', title: 'Schedule', tone: 'peach',  desc: 'Workflow stages start; clerks schedule hearings on conflict-free slots.' },
  { step: '04', title: 'Decide',   tone: 'green',  desc: 'Judges issue decisions, appeals run their course and compliance is audited.' },
]

const ROLES = [
  { icon: 'bi-person',          role: 'Litigant', tone: 'blue',   desc: 'File cases, upload documents and track their progress in real time.' },
  { icon: 'bi-briefcase',       role: 'Lawyer',   tone: 'purple', desc: 'Represent clients, manage assigned cases and file appeals.' },
  { icon: 'bi-person-badge',    role: 'Judge',    tone: 'peach',  desc: 'Schedule hearings, review appeals and record decisions.' },
  { icon: 'bi-clipboard-check', role: 'Clerk',    tone: 'green',  desc: 'Verify documents, manage workflow and run compliance checks.' },
  { icon: 'bi-shield-lock',     role: 'Admin',    tone: 'rose',   desc: 'Manage users, audits, system reports and configuration.' },
]

const PRINCIPLES = [
  { icon: 'bi-eye',       title: 'Transparent', tone: 'blue',   desc: 'Every action is timestamped, attributed and visible to authorised parties.' },
  { icon: 'bi-stopwatch', title: 'Timely',      tone: 'peach',  desc: 'SLA stages start automatically and warnings fire before deadlines slip.' },
  { icon: 'bi-lock',      title: 'Secure',      tone: 'green',  desc: 'JWT, BCrypt hashing and role-scoped APIs protect every endpoint.' },
  { icon: 'bi-puzzle',    title: 'Modular',     tone: 'purple', desc: 'Eleven independent services — deploy, scale and replace each on its own.' },
]

export default function Home() {
  return (
    <>
      <Hero stats={HERO_STATS} />

      {/* Trust strip */}
      <section className="container pt-4 pb-2">
        <div className="row g-3">
          {PROOF_STATS.map(s => (
            <div className="col-6 col-md-3" key={s.label}>
              <StatCard value={s.value} label={s.label} />
            </div>
          ))}
        </div>
      </section>

      {/* Features grid */}
      <section className="container pt-4 pb-2">
        <SectionTitle
          eyebrow="What's inside"
          title="Everything you need to run a court"
          subtitle="Eight integrated modules covering the full case lifecycle — file, verify, schedule, decide, appeal and audit."
          align="center"
        />
        <div className="row g-3">
          {FEATURES.map(f => (
            <div className="col-md-6 col-lg-3" key={f.title}>
              <FeatureCard {...f} />
            </div>
          ))}
        </div>
      </section>

      {/* Lifecycle */}
      <section className="container pt-4 pb-2">
        <SectionTitle
          eyebrow="The process"
          title="A clear path from filing to verdict"
          subtitle="Cases move through four broad phases with automatic transitions and stakeholder notifications at every step."
        />
        <div className="row g-3">
          {STEPS.map(s => (
            <div className="col-md-6 col-lg-3" key={s.step}>
              <StepCard {...s} />
            </div>
          ))}
        </div>
      </section>

      {/* Roles */}
      <section className="container pt-4 pb-2">
        <SectionTitle
          eyebrow="Built for everyone"
          title="One platform, five distinct experiences"
          subtitle="Every role sees only what they need. Authorisation is enforced at the API layer — never just the UI."
        />
        <div className="row g-3">
          {ROLES.map(r => (
            <div className="col-md-6 col-lg-4" key={r.role}>
              <RoleCard {...r} />
            </div>
          ))}
        </div>
      </section>

      {/* Principles */}
      <section className="container pt-4 pb-2">
        <SectionTitle
          eyebrow="Principles"
          title="Designed to be calm, not loud"
          subtitle="Legal work is serious work. The interface stays out of the way so the case stays in focus."
          align="center"
        />
        <div className="row g-3">
          {PRINCIPLES.map(p => (
            <div className="col-md-6 col-lg-3" key={p.title}>
              <FeatureCard {...p} />
            </div>
          ))}
        </div>
      </section>

      {/* CTA */}
      <section className="container pt-4 pb-4">
        <div className="cf-card p-4 p-md-5 text-center" style={{
          background: 'linear-gradient(135deg, var(--cf-pastel-purple) 0%, var(--cf-pastel-blue) 50%, var(--cf-pastel-mint) 100%)',
          border: 'none',
          boxShadow: 'var(--cf-shadow-md)',
        }}>
          <h3 className="h4 fw-semibold mb-2" style={{ letterSpacing: '-0.02em', color: 'var(--cf-text)' }}>
            Ready to modernise your legal operations?
          </h3>
          <p className="small mb-4" style={{ maxWidth: 520, margin: '0 auto', color: 'var(--cf-text-soft)' }}>
            Sign up as a litigant or contact us for institutional access. The platform is free to explore.
          </p>
          <div className="d-flex gap-2 justify-content-center flex-wrap">
            <Link to="/register" className="btn btn-dark px-4">Create account</Link>
            <Link to="/contact" className="btn btn-outline-dark px-4" style={{ background: 'var(--cf-surface)' }}>Contact us</Link>
          </div>
        </div>
      </section>
    </>
  )
}
