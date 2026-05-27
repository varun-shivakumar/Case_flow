// Standard page header: title (and optional subtitle) on left, action on right.
export default function PageHeader({ title, subtitle, action }) {
  return (
    <div className="d-flex justify-content-between align-items-start mb-4 flex-wrap gap-2">
      <div>
        <h1 className="page-title h4 mb-1">{title}</h1>
        {subtitle && <p className="cf-muted small mb-0">{subtitle}</p>}
      </div>
      {action && <div>{action}</div>}
    </div>
  )
}
