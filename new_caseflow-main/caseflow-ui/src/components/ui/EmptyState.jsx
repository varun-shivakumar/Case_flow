// Consistent empty state used in lists/tables when there are no items.
export default function EmptyState({ icon = 'bi-inbox', title = 'Nothing here yet', hint, action }) {
  return (
    <div className="text-center py-5">
      <i className={`bi ${icon} d-block mb-2`} style={{ fontSize: 32, opacity: 0.35 }} />
      <div className="fw-semibold">{title}</div>
      {hint && <div className="cf-muted small mt-1">{hint}</div>}
      {action && <div className="mt-3">{action}</div>}
    </div>
  )
}
