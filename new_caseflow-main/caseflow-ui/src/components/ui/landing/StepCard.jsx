// Numbered step block. The badge uses a pastel chip so the steps feel related.
export default function StepCard({ step, title, desc, tone = 'blue' }) {
  return (
    <div className="cf-card cf-card-hover p-3 h-100">
      <div className={`cf-chip cf-chip--${tone} mb-2`} style={{ width: 32, height: 32, fontSize: 12, fontWeight: 600, borderRadius: 999 }}>
        {step}
      </div>
      <div className="fw-semibold small mb-1">{title}</div>
      <p className="cf-muted small mb-0">{desc}</p>
    </div>
  )
}
