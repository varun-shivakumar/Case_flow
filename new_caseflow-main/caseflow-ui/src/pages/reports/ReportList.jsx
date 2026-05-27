import { useEffect, useMemo, useState } from 'react'
import { reports, users } from '../../api/services'
import { REPORT_SCOPE, REPORT_SCOPE_HELP, formatDate } from '../../utils/constants'
import { useAuth } from '../../context/AuthContext'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import EmptyState from '../../components/ui/EmptyState'
import LoadingState from '../../components/ui/LoadingState'
import DataTable from '../../components/ui/DataTable'
import FormField from '../../components/ui/FormField'
import { notify } from '../../components/ui/Toast'

// Scopes shown in the generation UI. CASE/COMPLIANCE are kept in REPORT_SCOPE
// for the list filter but hidden from the dropdown.
const VISIBLE_SCOPES = REPORT_SCOPE.filter(s => s !== 'CASE' && s !== 'COMPLIANCE')

// Which role's user-list to populate when the scope picks a person.
const ROLE_DROPDOWN = { JUDGE: 'JUDGE', CLERK: 'CLERK', LAWYER: 'LAWYER' }

// Pastel-tone per metric group so the viewer reads cleanly.
const GROUP_META = {
  summary:    { title: 'Cases',       icon: 'bi-folder2-open',          tone: 'blue'   },
  documents:  { title: 'Documents',   icon: 'bi-file-earmark-check',    tone: 'purple' },
  hearings:   { title: 'Hearings',    icon: 'bi-calendar2-event',       tone: 'peach'  },
  sla:        { title: 'SLA',         icon: 'bi-stopwatch',             tone: 'mint'   },
  appeals:    { title: 'Appeals',     icon: 'bi-arrow-counterclockwise', tone: 'pink'  },
  compliance: { title: 'Compliance',  icon: 'bi-shield-check',          tone: 'green'  },
}

// Keys that should render as percentages with a trailing %.
function isPercent(key) {
  const k = key.toLowerCase()
  return k.endsWith('rate') || k.endsWith('percent')
}

// Format any leaf metric value sensibly.
function formatValue(key, value) {
  if (value == null) return '—'
  if (typeof value === 'number') {
    if (isPercent(key)) return `${Math.round(value * 10) / 10}%`
    return Number.isInteger(value) ? value.toLocaleString() : value.toFixed(2)
  }
  return String(value)
}

// Convert a camelCase key to a human label
function humanize(key) {
  return key
    .replace(/([A-Z])/g, ' $1')
    .replace(/^./, c => c.toUpperCase())
    .trim()
}

// Backend stores metrics as a JSON string. Be defensive — fall back gracefully.
function parseMetrics(raw) {
  if (!raw) return null
  if (typeof raw === 'object') return raw
  try { return JSON.parse(raw) } catch { return null }
}

// One metric tile (label + value)
function MetricTile({ label, value }) {
  return (
    <div className="cf-card p-3 h-100" style={{ background: 'var(--cf-surface-alt)' }}>
      <div className="cf-muted small mb-1">{label}</div>
      <div className="fw-semibold" style={{ fontSize: 20, letterSpacing: '-0.01em' }}>{value}</div>
    </div>
  )
}

// Pure-SVG donut chart — zero external dependencies.
function DonutChart({ slices, size = 140 }) {
  const total = slices.reduce((s, d) => s + (d.value || 0), 0)
  if (total === 0) return null
  const cx = size / 2, cy = size / 2, R = size * 0.42, r = size * 0.26
  let angle = -Math.PI / 2
  const paths = slices.filter(d => d.value > 0).map(d => {
    const sweep = (d.value / total) * 2 * Math.PI
    const x1o = cx + R * Math.cos(angle), y1o = cy + R * Math.sin(angle)
    const x2o = cx + R * Math.cos(angle + sweep), y2o = cy + R * Math.sin(angle + sweep)
    const x1i = cx + r * Math.cos(angle + sweep), y1i = cy + r * Math.sin(angle + sweep)
    const x2i = cx + r * Math.cos(angle), y2i = cy + r * Math.sin(angle)
    const large = sweep > Math.PI ? 1 : 0
    const path = `M ${x1o} ${y1o} A ${R} ${R} 0 ${large} 1 ${x2o} ${y2o} L ${x1i} ${y1i} A ${r} ${r} 0 ${large} 0 ${x2i} ${y2i} Z`
    angle += sweep
    return { ...d, path }
  })
  return (
    <div className="d-flex align-items-center gap-3 flex-wrap">
      <div className="position-relative" style={{ width: size, height: size, flexShrink: 0 }}>
        <svg width={size} height={size} viewBox={`0 0 ${size} ${size}`}>
          {paths.map((s, i) => <path key={i} d={s.path} fill={s.color} />)}
        </svg>
        <div className="position-absolute top-50 start-50 translate-middle text-center" style={{ pointerEvents: 'none' }}>
          <div className="fw-semibold" style={{ fontSize: 17, lineHeight: 1 }}>{total.toLocaleString()}</div>
          <div className="cf-muted" style={{ fontSize: 10 }}>total</div>
        </div>
      </div>
      <div className="d-flex flex-column gap-1">
        {slices.filter(s => s.value > 0).map((s, i) => (
          <div key={i} className="d-flex align-items-center gap-2 small">
            <span style={{ width: 8, height: 8, borderRadius: '50%', background: s.color, flexShrink: 0, display: 'inline-block' }} />
            <span className="cf-muted">{s.label}</span>
            <span className="fw-semibold ms-1">{s.value.toLocaleString()}</span>
            <span className="cf-muted" style={{ fontSize: 11 }}>({Math.round((s.value / total) * 100)}%)</span>
          </div>
        ))}
      </div>
    </div>
  )
}

function extractChartSlices(keyName, data) {
  if (!data) return null
  switch (keyName) {
    case 'summary':    return [
      { label: 'Active',    value: data.casesActive    || 0, color: '#3b82f6' },
      { label: 'Closed',    value: data.casesClosed    || 0, color: '#22c55e' },
      { label: 'Adjourned', value: data.casesAdjourned || 0, color: '#f59e0b' },
      { label: 'Appealed',  value: data.casesAppealed  || 0, color: '#ec4899' },
    ]
    case 'documents':  return [
      { label: 'Verified', value: data.verifiedDocuments || 0, color: '#22c55e' },
      { label: 'Pending',  value: data.pendingDocuments  || 0, color: '#f59e0b' },
      { label: 'Rejected', value: data.rejectedDocuments || 0, color: '#ef4444' },
    ]
    case 'hearings':   return [
      { label: 'Completed',   value: data.hearingsCompleted   || 0, color: '#22c55e' },
      { label: 'Scheduled',   value: data.hearingsScheduled   || 0, color: '#3b82f6' },
      { label: 'Rescheduled', value: data.hearingsRescheduled || 0, color: '#f59e0b' },
      { label: 'Cancelled',   value: data.hearingsCancelled   || 0, color: '#ef4444' },
    ]
    case 'sla':        return [
      { label: 'On Time',  value: data.slaClosed   || 0, color: '#22c55e' },
      { label: 'Active',   value: data.slaActive   || 0, color: '#3b82f6' },
      { label: 'Warning',  value: data.slaWarnings || 0, color: '#f59e0b' },
      { label: 'Breached', value: data.slaBreaches || 0, color: '#ef4444' },
    ]
    case 'appeals':    return [
      { label: 'Submitted',    value: data.appealsFiled       || 0, color: '#3b82f6' },
      { label: 'Under Review', value: data.appealsUnderReview || 0, color: '#f59e0b' },
      { label: 'Decided',      value: data.appealsDecided     || 0, color: '#22c55e' },
    ]
    case 'compliance': return [
      { label: 'Pass', value: data.compliancePasses   || 0, color: '#22c55e' },
      { label: 'Fail', value: data.complianceFailures || 0, color: '#ef4444' },
    ]
    default: return null
  }
}

// Render one metric group (summary / hearings / sla / etc.) with pastel chip header.
// Nested objects inside the group (e.g. appeals.outcomes) are rendered as a labelled subgroup.
function MetricGroup({ keyName, data }) {
  if (!data || typeof data !== 'object' || Object.keys(data).length === 0) return null
  const meta = GROUP_META[keyName] || { title: humanize(keyName), icon: 'bi-bar-chart', tone: 'blue' }

  const leafEntries = Object.entries(data).filter(([, v]) => v == null || typeof v !== 'object')
  const nestedEntries = Object.entries(data).filter(([, v]) => v != null && typeof v === 'object')
  const chartSlices = extractChartSlices(keyName, data)
  const hasChart = chartSlices && chartSlices.some(s => s.value > 0)

  return (
    <div className="cf-card p-4 mb-3">
      <div className="d-flex align-items-center gap-2 mb-3">
        <span className={`cf-chip cf-chip--${meta.tone}`} style={{ width: 36, height: 36, fontSize: 16 }}>
          <i className={`bi ${meta.icon}`} />
        </span>
        <h6 className="fw-semibold mb-0">{meta.title}</h6>
      </div>

      {hasChart && (
        <div className="mb-4 p-3 rounded" style={{ background: 'var(--cf-surface-alt)' }}>
          <DonutChart slices={chartSlices} size={130} />
        </div>
      )}

      {leafEntries.length > 0 && (
        <div className="row g-2 mb-2">
          {leafEntries.map(([k, v]) => (
            <div className="col-6 col-md-4 col-lg-3" key={k}>
              <MetricTile label={humanize(k)} value={formatValue(k, v)} />
            </div>
          ))}
        </div>
      )}

      {nestedEntries.map(([nk, nv]) => {
        const nestedSlices = nk === 'outcomes' && keyName === 'appeals'
          ? [
              { label: 'Approved', value: nv.approved || 0, color: '#22c55e' },
              { label: 'Rejected', value: nv.rejected || 0, color: '#ef4444' },
            ]
          : null
        const hasNestedChart = nestedSlices && nestedSlices.some(s => s.value > 0)
        return (
          <div key={nk} className="mt-3">
            <div className="cf-muted small fw-semibold mb-2 text-uppercase" style={{ letterSpacing: '0.06em', fontSize: 11 }}>
              {humanize(nk)}
            </div>
            {hasNestedChart && (
              <div className="mb-3 p-3 rounded" style={{ background: 'var(--cf-surface-alt)' }}>
                <DonutChart slices={nestedSlices} size={110} />
              </div>
            )}
            <div className="row g-2">
              {Object.entries(nv).map(([k, v]) => (
                <div className="col-6 col-md-4 col-lg-3" key={k}>
                  <MetricTile label={humanize(k)} value={formatValue(k, v)} />
                </div>
              ))}
            </div>
          </div>
        )
      })}
    </div>
  )
}

// CSV export for a generated report
function exportCsv(report) {
  const m = parseMetrics(report.metrics) || {}
  const rows = [['Section', 'Metric', 'Value']]
  const flatten = (section, obj) => Object.entries(obj || {}).forEach(([k, v]) => {
    if (v != null && typeof v === 'object') flatten(`${section}.${k}`, v)
    else rows.push([section, k, v == null ? '' : String(v)])
  })
  Object.entries(m).forEach(([s, o]) => {
    if (o != null && typeof o === 'object') flatten(s, o)
  })
  const csv = rows.map(r => r.map(c => `"${String(c).replace(/"/g, '""')}"`).join(',')).join('\n')
  const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `report-${report.reportId}-${report.scope}.csv`
  a.click()
  URL.revokeObjectURL(url)
}

// ── Page ───────────────────────────────────────────────────────────────────
export default function ReportList() {
  const { user } = useAuth()

  const [list, setList]         = useState([])
  const [loading, setLoading]   = useState(true)
  const [selected, setSelected] = useState(null)
  const [filter, setFilter]     = useState('')
  const [generating, setGenerating] = useState(false)

  const [form, setForm] = useState({ scope: 'COURT', scopeValue: 'ALL', dateFrom: '', dateTo: '' })
  const [roleUsers, setRoleUsers] = useState([])

  // Reports list
  const load = async () => {
    setLoading(true)
    try {
      const p = await reports.paginated(0, 50)
      // /paginated returns a Spring Page; /me returns a List. Support both.
      setList(Array.isArray(p) ? p : (p?.content || []))
    } catch (e) {
      notify.error(e.message || 'Failed to load reports')
    } finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  // When scope changes, reset scopeValue and (for person scopes) load that role's users
  useEffect(() => {
    setForm(f => ({ ...f, scopeValue: f.scope === 'COURT' ? 'ALL' : '' }))
    setRoleUsers([])
    const role = ROLE_DROPDOWN[form.scope]
    if (role) {
      users.byRole(role).then(d => setRoleUsers(Array.isArray(d) ? d : [])).catch(() => {})
    }
  }, [form.scope])

  const generate = async (e) => {
    e.preventDefault()
    setGenerating(true)
    try {
      const res = await reports.generate({
        scope: form.scope,
        scopeValue: form.scopeValue?.trim() || 'ALL',
        dateFrom: form.dateFrom || null,
        dateTo:   form.dateTo   || null,
      })
      notify.success(`Report #${res.reportId} generated`)
      setSelected(res)
      load()
    } catch (e) {
      notify.error(e.message || 'Generation failed')
    } finally { setGenerating(false) }
  }

  const del = async (id) => {
    if (!confirm(`Delete report #${id}?`)) return
    try {
      await reports.del(id)
      notify.success('Report deleted')
      setList(prev => prev.filter(r => r.reportId !== id))
      if (selected?.reportId === id) setSelected(null)
    } catch (e) { notify.error(e.message) }
  }

  const filteredList = filter ? list.filter(r => r.scope === filter) : list
  const metrics = useMemo(() => selected ? parseMetrics(selected.metrics) : null, [selected])
  const help = REPORT_SCOPE_HELP[form.scope] || {}

  // Order matters — render groups in this sequence for consistency.
  const GROUP_ORDER = ['summary', 'documents', 'hearings', 'sla', 'appeals', 'compliance']

  return (
    <div>
      <PageHeader
        title="Reports & analytics"
        subtitle={`${list.length} report${list.length !== 1 ? 's' : ''} generated · Aggregated across cases, hearings, SLA, appeals & compliance`}
      />

      {/* Generate report */}
      <SectionCard title="Generate report" className="mb-3">
        <form onSubmit={generate}>
          <div className="row g-3">
            <div className="col-md-3">
              <FormField label="Scope" required>
                <select className="form-select" value={form.scope}
                  onChange={e => setForm({ ...form, scope: e.target.value })}>
                  {VISIBLE_SCOPES.map(s => <option key={s} value={s}>{s}</option>)}
                </select>
              </FormField>
            </div>
            <div className="col-md-5">
              <FormField label={help.label || 'Scope value'} hint={help.hint}>
                {roleUsers.length > 0 ? (
                  <select className="form-select" value={form.scopeValue}
                    onChange={e => setForm({ ...form, scopeValue: e.target.value })} required>
                    <option value="">Select {form.scope.toLowerCase()}…</option>
                    {roleUsers.map(u => <option key={u.userId} value={u.userId}>{u.name} — {u.userId}</option>)}
                  </select>
                ) : (
                  <input className="form-control" value={form.scopeValue}
                    onChange={e => setForm({ ...form, scopeValue: e.target.value })}
                    placeholder={help.placeholder || 'ALL'} required />
                )}
              </FormField>
            </div>
            <div className="col-md-2">
              <FormField label="Date from">
                <input type="date" className="form-control" value={form.dateFrom}
                  onChange={e => setForm({ ...form, dateFrom: e.target.value })} />
              </FormField>
            </div>
            <div className="col-md-2">
              <FormField label="Date to">
                <input type="date" className="form-control" value={form.dateTo}
                  onChange={e => setForm({ ...form, dateTo: e.target.value })} />
              </FormField>
            </div>
          </div>
          <button className="btn btn-dark" disabled={generating}>
            {generating ? <><span className="spinner-border spinner-border-sm me-2" />Generating…</> : <><i className="bi bi-bar-chart-line me-1" />Generate report</>}
          </button>
        </form>
      </SectionCard>

      {/* Report viewer — only when one is selected */}
      {selected && metrics && (
        <SectionCard
          title={`Report #${selected.reportId} — ${selected.scope}`}
          className="mb-3"
          action={
            <div className="d-flex gap-2">
              <button className="btn btn-sm btn-outline-secondary" onClick={() => exportCsv(selected)}>
                <i className="bi bi-download me-1" />Export CSV
              </button>
              <button className="btn btn-sm btn-outline-secondary" onClick={() => setSelected(null)}>
                <i className="bi bi-x-lg me-1" />Close
              </button>
            </div>
          }
        >
          <div className="d-flex flex-wrap gap-3 mb-3 small">
            <span><span className="cf-muted">Scope value: </span><strong>{selected.scopeValue}</strong></span>
            {selected.dateFrom && (
              <span><span className="cf-muted">Period: </span><strong>{formatDate(selected.dateFrom)} → {formatDate(selected.dateTo)}</strong></span>
            )}
            <span><span className="cf-muted">Generated: </span><strong>{formatDate(selected.generatedDate)}</strong></span>
            <span><span className="cf-muted">By: </span><strong>{selected.requestedBy || '—'}</strong></span>
          </div>

          {GROUP_ORDER.map(k => <MetricGroup key={k} keyName={k} data={metrics[k]} />)}

          {GROUP_ORDER.every(k => !metrics[k] || Object.keys(metrics[k] || {}).length === 0) && (
            <EmptyState icon="bi-bar-chart" title="No metrics in this report"
              hint="The scope produced no data — try widening the date range or selecting a different scope." />
          )}
        </SectionCard>
      )}

      {/* History */}
      <SectionCard title="Report history" padded={false}
        action={
          <div className="d-flex gap-2 align-items-center">
            <select className="form-select form-select-sm w-auto" value={filter}
              onChange={e => setFilter(e.target.value)}>
              <option value="">All scopes</option>
              {REPORT_SCOPE.map(s => <option key={s} value={s}>{s}</option>)}
            </select>
            <button className="btn btn-sm btn-outline-secondary" onClick={load}>
              <i className="bi bi-arrow-clockwise me-1" />Refresh
            </button>
          </div>
        }
      >
        {loading ? <LoadingState />
          : filteredList.length === 0 ? <EmptyState icon="bi-bar-chart" title="No reports yet"
              hint="Generate your first report using the form above." />
          : <DataTable columns={['ID', 'Scope', 'Value', 'Period', 'Generated', 'By', '']}>
              {filteredList.map(r => {
                const isSelected = selected?.reportId === r.reportId
                return (
                  <tr key={r.reportId} style={isSelected ? { background: 'var(--cf-surface-alt)' } : undefined}>
                    <td className="fw-semibold">#{r.reportId}</td>
                    <td><span className="badge text-bg-light border">{r.scope}</span></td>
                    <td className="cf-muted">{r.scopeValue}</td>
                    <td className="cf-muted">
                      {r.dateFrom ? `${formatDate(r.dateFrom)} → ${formatDate(r.dateTo)}` : '—'}
                    </td>
                    <td className="cf-muted">{formatDate(r.generatedDate)}</td>
                    <td className="cf-muted">{r.requestedBy || '—'}</td>
                    <td>
                      <div className="d-flex gap-1 flex-wrap">
                        <button className="btn btn-sm btn-outline-secondary" onClick={() => setSelected(r)}>
                          <i className="bi bi-eye me-1" />{isSelected ? 'Viewing' : 'View'}
                        </button>
                        <button className="btn btn-sm btn-outline-secondary" onClick={() => exportCsv(r)}>
                          <i className="bi bi-download me-1" />CSV
                        </button>
                        {user?.role === 'ADMIN' && (
                          <button className="btn btn-sm btn-outline-danger" onClick={() => del(r.reportId)}>
                            <i className="bi bi-trash3" />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                )
              })}
            </DataTable>}
      </SectionCard>
    </div>
  )
}
