// Inline Bootstrap alert. Use this for form-level validation only —
// prefer toast notifications for action results.
export default function AlertMessage({ error, success, info }) {
  if (error)   return <div className="alert alert-danger py-2 small mb-3">{error}</div>
  if (success) return <div className="alert alert-success py-2 small mb-3">{success}</div>
  if (info)    return <div className="alert alert-info py-2 small mb-3">{info}</div>
  return null
}
