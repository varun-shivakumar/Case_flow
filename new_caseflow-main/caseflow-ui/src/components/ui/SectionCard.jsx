// Bordered card wrapper. Use for any boxed section in the UI.
export default function SectionCard({ title, action, padded = true, children, className = '' }) {
  return (
    <div className={`cf-card ${className}`}>
      {(title || action) && (
        <div className="d-flex justify-content-between align-items-center px-3 py-2 border-bottom">
          {title && <h6 className="mb-0 fw-semibold">{title}</h6>}
          {action}
        </div>
      )}
      <div className={padded ? 'p-3' : ''}>{children}</div>
    </div>
  )
}
