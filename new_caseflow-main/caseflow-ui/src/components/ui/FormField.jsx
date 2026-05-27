// Labeled form field with optional inline hint. Pure Bootstrap classes only.
export default function FormField({ label, hint, required, children }) {
  return (
    <div className="mb-3">
      {label && (
        <label className="form-label small fw-semibold mb-1">
          {label} {required && <span className="text-danger">*</span>}
        </label>
      )}
      {children}
      {hint && <div className="form-text small">{hint}</div>}
    </div>
  )
}
