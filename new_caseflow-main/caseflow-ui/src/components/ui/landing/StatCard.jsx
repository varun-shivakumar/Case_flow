// Numeric stat with caption. Variant 'plain' has no border for inline rows.
export default function StatCard({ value, label, variant = 'card' }) {
  if (variant === 'plain') {
    return (
      <div>
        <div className="fw-semibold" style={{ fontSize: 26, letterSpacing: '-0.02em' }}>{value}</div>
        <div className="cf-muted small">{label}</div>
      </div>
    )
  }
  return (
    <div className="cf-stat">
      <div className="cf-stat__value">{value}</div>
      <div className="cf-stat__label">{label}</div>
    </div>
  )
}
