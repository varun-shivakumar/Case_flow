import SectionCard from '../../../components/ui/SectionCard'

const BADGE_CLASS = {
  done:    'text-bg-dark',
  active:  'text-bg-secondary',
  skipped: 'text-bg-light border',
  pending: 'text-bg-light border',
}

function stageBadgeClass(s) {
  const isDone = !!s.completedAt && !s.active
  const status = isDone ? 'done' : s.active ? 'active' : s.skipped ? 'skipped' : 'pending'
  return BADGE_CLASS[status]
}

export default function StageProgressCard({ stages, title = 'Progress', showBadges = false, className = '' }) {
  const completed = stages.filter(s => s.completedAt && !s.active).length
  const pct = stages.length ? Math.round((completed / stages.length) * 100) : 0

  return (
    <SectionCard title={title} className={className}>
      <div className="d-flex justify-content-between small mb-2">
        <span className="cf-muted">{completed} of {stages.length} stages</span>
        <span className="fw-semibold">{pct}%</span>
      </div>
      <div className={`progress ${showBadges ? 'mb-3' : ''}`} style={{ height: 6 }}>
        <div className="progress-bar bg-dark" style={{ width: `${pct}%` }} />
      </div>
      {showBadges && (
        <div className="d-flex flex-wrap gap-1">
          {stages.map(s => (
            <span key={s.stageId} className={`badge ${stageBadgeClass(s)} small`}>{s.stageName}</span>
          ))}
        </div>
      )}
    </SectionCard>
  )
}
