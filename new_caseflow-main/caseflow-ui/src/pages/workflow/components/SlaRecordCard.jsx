import StatusBadge from '../../../components/ui/StatusBadge'
import { formatDate } from '../../../utils/constants'
import SlaProgressBar from './SlaProgressBar'

export default function SlaRecordCard({ record: r }) {
  const p = r.slaUsagePercent || 0
  return (
    <div className="cf-card p-3">
      <div className="d-flex justify-content-between small mb-2">
        <span className="fw-semibold">Stage #{r.stageId}</span>
        <StatusBadge status={r.status} />
      </div>
      <SlaProgressBar percent={p} height={4} className="mb-2" />
      <div className="d-flex justify-content-between small cf-muted">
        <span>{formatDate(r.startDate)} → {formatDate(r.endDate)}</span>
        <span>{p.toFixed(0)}%</span>
      </div>
    </div>
  )
}
