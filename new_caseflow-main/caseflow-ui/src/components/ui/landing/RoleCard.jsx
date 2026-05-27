// Persona card — pastel chip + role name + short responsibility line.
export default function RoleCard({ icon, role, desc, tone = 'blue' }) {
  return (
    <div className="cf-card cf-card-hover p-3 h-100 d-flex align-items-start gap-3">
      <div className={`cf-chip cf-chip--${tone} flex-shrink-0`} style={{ width: 36, height: 36, fontSize: 16 }}>
        <i className={`bi ${icon}`} />
      </div>
      <div>
        <div className="fw-semibold small mb-1">{role}</div>
        <p className="cf-muted small mb-0">{desc}</p>
      </div>
    </div>
  )
}
