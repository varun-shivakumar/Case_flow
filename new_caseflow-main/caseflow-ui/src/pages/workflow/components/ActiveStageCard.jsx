import SectionCard from '../../../components/ui/SectionCard'
import { formatDate } from '../../../utils/constants'

export default function ActiveStageCard({ current, onAdvance, onRollback, showActions = true, className = '' }) {
  if (!current) return null
  return (
    <SectionCard
      title="Active stage"
      className={className}
      action={showActions && (
        <div className="d-flex gap-2">
          <button className="btn btn-sm btn-dark" onClick={onAdvance}>Advance</button>
          <button className="btn btn-sm btn-outline-secondary" onClick={onRollback}>Rollback</button>
        </div>
      )}
    >
      <div className="row g-3 small">
        <div className="col-md-4"><div className="cf-muted">Stage</div><span className="fw-semibold">{current.stageName}</span></div>
        <div className="col-md-3"><div className="cf-muted">Role</div>{current.roleResponsible}</div>
        <div className="col-md-2"><div className="cf-muted">SLA days</div>{current.slaDays}d</div>
        <div className="col-md-3"><div className="cf-muted">Started</div>{formatDate(current.startedAt)}</div>
      </div>
    </SectionCard>
  )
}
