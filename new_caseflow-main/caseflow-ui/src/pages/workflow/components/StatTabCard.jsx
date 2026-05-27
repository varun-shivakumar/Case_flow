export default function StatTabCard({ label, value, active, onClick, danger = false }) {
  return (
    <button className={`cf-card p-3 w-100 text-start ${active ? 'border-dark' : ''}`} onClick={onClick}>
      <div className="cf-muted small">{label}</div>
      <div className={`h3 fw-semibold mb-0 ${danger && value > 0 ? 'text-danger' : ''}`}>{value}</div>
    </button>
  )
}
