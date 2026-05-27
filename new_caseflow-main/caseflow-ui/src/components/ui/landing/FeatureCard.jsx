// Icon + title + description tile. The `tone` prop chooses a pastel chip color.
// Hovering the card animates the chip via the parent .cf-card-hover trigger.
export default function FeatureCard({ icon, title, desc, tone = 'blue' }) {
  return (
    <div className="cf-card cf-card-hover p-4 h-100">
      <div className={`cf-chip cf-chip--${tone} mb-3`}>
        <i className={`bi ${icon}`} />
      </div>
      <div className="fw-semibold mb-1" style={{ fontSize: 15 }}>{title}</div>
      <p className="cf-muted small mb-0">{desc}</p>
    </div>
  )
}
