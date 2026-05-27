// Centered loading spinner with optional label.
export default function LoadingState({ label = 'Loading...' }) {
  return (
    <div className="text-center py-5 cf-muted">
      <div className="spinner-border spinner-border-sm me-2" role="status" />
      <span className="small">{label}</span>
    </div>
  )
}
