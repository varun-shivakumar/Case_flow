export default function SlaProgressBar({ percent = 0, height = 4, className = '', inline = false }) {
  const p = Math.min(percent, 100)
  const barCls = percent >= 80 ? 'bg-danger' : percent >= 50 ? 'bg-warning' : 'bg-success'

  if (inline) {
    return (
      <div className={`d-flex align-items-center gap-2 ${className}`}>
        <div className="progress flex-grow-1" style={{ height }}>
          <div className={`progress-bar ${barCls}`} style={{ width: `${p}%` }} />
        </div>
        <span className="small cf-muted">{percent.toFixed(0)}%</span>
      </div>
    )
  }

  return (
    <div className={`progress ${className}`} style={{ height }}>
      <div className={`progress-bar ${barCls}`} style={{ width: `${p}%` }} />
    </div>
  )
}
