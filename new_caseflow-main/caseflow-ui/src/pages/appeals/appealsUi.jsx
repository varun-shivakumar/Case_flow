import { Link } from 'react-router-dom'

/* ──────────────────────────────────────────────────────────
   Small re-usable building blocks for the appeals module.
   Centralised so AppealList, AppealDetail, MyReviews, and
   ReviewsByJudge stay consistent.
   ────────────────────────────────────────────────────────── */

/** Coloured outcome chip for ReviewOutcome enums. */
export function OutcomeChip({ outcome }) {
  if (!outcome) {
    return <span className="appeal-outcome-chip appeal-outcome-chip--pending">
      <i className="bi bi-hourglass-split" /> Pending
    </span>
  }
  const map = {
    APPEAL_UPHELD:    { mod: 'upheld',    icon: 'bi-check-circle-fill',   label: 'Upheld' },
    APPEAL_DISMISSED: { mod: 'dismissed', icon: 'bi-x-circle-fill',       label: 'Dismissed' },
    PARTIALLY_UPHELD: { mod: 'partial',   icon: 'bi-pie-chart-fill',      label: 'Partially upheld' },
    REMANDED:         { mod: 'remanded',  icon: 'bi-arrow-90deg-left',    label: 'Remanded' },
    RETRIAL_ORDERED:  { mod: 'retrial',   icon: 'bi-arrow-counterclockwise', label: 'Retrial ordered' },
  }
  const cfg = map[outcome] || { mod: 'pending', icon: 'bi-circle', label: outcome }
  return (
    <span className={`appeal-outcome-chip appeal-outcome-chip--${cfg.mod}`}>
      <i className={`bi ${cfg.icon}`} /> {cfg.label}
    </span>
  )
}

/** Lifecycle stepper for an Appeal. */
export function AppealStepper({ status }) {
  const cancelled = status === 'CANCELLED'
  // Index of current step in the happy path
  const idx = status === 'SUBMITTED' ? 0
            : status === 'REVIEWED'  ? 1
            : status === 'DECIDED'   ? 2
            : cancelled ? 0 : 0

  const STEPS = [
    { label: 'Submitted',     icon: 'bi-file-earmark-text' },
    { label: 'Under Review',  icon: 'bi-search' },
    { label: 'Decided',       icon: 'bi-stamp' },
  ]
  const stepClass = (i) => {
    if (cancelled) return ''
    if (i <  idx) return 'appeal-stepper__step--reached'
    if (i === idx && status === 'DECIDED') return 'appeal-stepper__step--reached'
    if (i === idx) return 'appeal-stepper__step--current'
    return ''
  }
  const lineClass = (i) => {
    if (cancelled) return ''
    return i < idx ? 'appeal-stepper__line--reached' : ''
  }

  return (
    <div>
      <div className={`appeal-stepper ${cancelled ? 'appeal-stepper--cancelled' : ''}`}>
        <div className={`appeal-stepper__step ${stepClass(0)}`}>
          <div className="appeal-stepper__circle"><i className={`bi ${STEPS[0].icon}`} /></div>
          <div className="appeal-stepper__label">{STEPS[0].label}</div>
        </div>
        <div className={`appeal-stepper__line ${lineClass(0)}`} />
        <div className={`appeal-stepper__step ${stepClass(1)}`}>
          <div className="appeal-stepper__circle"><i className={`bi ${STEPS[1].icon}`} /></div>
          <div className="appeal-stepper__label">{STEPS[1].label}</div>
        </div>
        <div className={`appeal-stepper__line ${lineClass(1)}`} />
        <div className={`appeal-stepper__step ${stepClass(2)}`}>
          <div className="appeal-stepper__circle"><i className={`bi ${STEPS[2].icon}`} /></div>
          <div className="appeal-stepper__label">{STEPS[2].label}</div>
        </div>
      </div>
      {cancelled && (
        <div className="text-center">
          <span className="appeal-stepper__cancel-flag">
            <i className="bi bi-x-octagon-fill" /> Appeal cancelled
          </span>
        </div>
      )}
    </div>
  )
}

/** Generic empty state with optional CTA. */
export function EmptyState({ icon = 'bi-inbox', title, hint, cta }) {
  return (
    <div className="appeal-empty">
      <div className="appeal-empty__icon"><i className={`bi ${icon}`} /></div>
      {title && <div className="appeal-empty__title">{title}</div>}
      {hint  && <div>{hint}</div>}
      {cta   && <div className="mt-3">{cta}</div>}
    </div>
  )
}

/** Section card header with icon. */
export function SectionHead({ icon, title, right }) {
  return (
    <div className="appeal-card__head">
      <div className="appeal-card__title">
        <span className="appeal-card__title-icon"><i className={`bi ${icon}`} /></span>
        {title}
      </div>
      {right && <div>{right}</div>}
    </div>
  )
}

/** Stats card row used by AppealList. */
export function StatCard({ status, count, icon, hint, active, onClick }) {
  const mod = status.toLowerCase()
  return (
    <button
      type="button"
      onClick={onClick}
      className={`appeal-stat appeal-stat--${mod} ${active ? 'appeal-stat--active' : ''}`}
    >
      <div className="appeal-stat__label">
        <span>{status}</span>
        <span className="appeal-stat__icon"><i className={`bi ${icon}`} /></span>
      </div>
      <div className="appeal-stat__value">{count}</div>
      {hint && <div className="appeal-stat__hint">{hint}</div>}
    </button>
  )
}

/** File-extension-aware icon for documents. */
export function DocIcon({ filename }) {
  const ext = (filename || '').split('.').pop().toLowerCase()
  const iconByExt = {
    pdf:  'bi-file-earmark-pdf',
    doc:  'bi-file-earmark-word', docx: 'bi-file-earmark-word',
    xls:  'bi-file-earmark-spreadsheet', xlsx: 'bi-file-earmark-spreadsheet',
    ppt:  'bi-file-earmark-ppt',  pptx: 'bi-file-earmark-ppt',
    txt:  'bi-file-earmark-text',
    jpg:  'bi-file-earmark-image', jpeg: 'bi-file-earmark-image',
    png:  'bi-file-earmark-image', gif:  'bi-file-earmark-image',
    zip:  'bi-file-earmark-zip',  rar:  'bi-file-earmark-zip',
  }
  return (
    <span className="appeal-doc__icon" data-ext={ext}>
      <i className={`bi ${iconByExt[ext] || 'bi-file-earmark'}`} />
    </span>
  )
}

/** Page header strip. */
export function PageHeader({ title, subtitle, actions }) {
  return (
    <div className="appeal-pageheader">
      <div>
        <h1 className="h3 mb-0">{title}</h1>
        {subtitle && <div className="appeal-pageheader__sub">{subtitle}</div>}
      </div>
      {actions && <div className="appeal-pageheader__actions d-flex gap-2 flex-wrap">{actions}</div>}
    </div>
  )
}

/** Crumb link for navigating back. */
export function BackLink({ to = -1, children = 'Back' }) {
  if (typeof to === 'string') {
    return (
      <Link to={to} className="btn btn-outline-light btn-sm">
        <i className="bi bi-arrow-left me-1" /> {children}
      </Link>
    )
  }
  return (
    <button type="button" className="btn btn-outline-light btn-sm" onClick={() => history.go(to)}>
      <i className="bi bi-arrow-left me-1" /> {children}
    </button>
  )
}

/** Skeleton row for list loading. */
export function SkeletonList({ rows = 5 }) {
  return (
    <div>
      {Array.from({ length: rows }).map((_, i) => (
        <div className="appeal-skeleton appeal-skeleton--card" key={i} />
      ))}
    </div>
  )
}

/** Skeleton stat-strip placeholder. */
export function SkeletonStats() {
  return (
    <div className="row g-3 mb-3">
      {[0,1,2,3].map(i => (
        <div className="col-6 col-md-3" key={i}>
          <div className="appeal-skeleton appeal-skeleton--stat" />
        </div>
      ))}
    </div>
  )
}
