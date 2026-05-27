import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { compliance } from '../../api/services'
import { formatDateTime, formatDate } from '../../utils/constants'
import { useAuth } from '../../context/AuthContext'
import PageHeader from '../../components/ui/PageHeader'
import EmptyState from '../../components/ui/EmptyState'
import LoadingState from '../../components/ui/LoadingState'
import DataTable from '../../components/ui/DataTable'
import { notify } from '../../components/ui/Toast'

// Compliance check run history. Click a row to drill into the run's records.
export default function ComplianceList() {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'
  const navigate = useNavigate()

  const [runs, setRuns]       = useState([])
  const [loading, setLoading] = useState(false)
  const [selected, setSelected] = useState(new Set())
  const [busy, setBusy] = useState(false)

  const load = async () => {
    setLoading(true)
    try { setRuns(await compliance.runs() || []) }
    catch (e) { notify.error(e.message) }
    finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  const toggle = (id) => setSelected(s => {
    const n = new Set(s); n.has(id) ? n.delete(id) : n.add(id); return n
  })
  const toggleAll = () => setSelected(s => s.size === runs.length ? new Set() : new Set(runs.map(r => r.runId)))

  const bulkDelete = async () => {
    if (!selected.size) return
    if (!confirm(`Delete ${selected.size} selected runs?`)) return
    setBusy(true)
    try {
      const allIds = []
      for (const run of runs) {
        if (!selected.has(run.runId)) continue
        try {
          const recs = await compliance.runRecords(run.runId)
          ;(recs || []).forEach(r => allIds.push(r.complianceId))
        } catch {}
      }
      if (allIds.length) await compliance.bulkDeleteRecords(allIds)
      notify.success(`Deleted ${selected.size} run${selected.size !== 1 ? 's' : ''}`)
      setSelected(new Set())
      load()
    } catch (e) { notify.error(e.message) }
    finally { setBusy(false) }
  }

  return (
    <div>
      <PageHeader
        title="Compliance checks"
        subtitle={`${runs.length} run${runs.length !== 1 ? 's' : ''} recorded`}
        action={
          <div className="d-flex gap-2">
            {isAdmin && selected.size > 0 && (
              <button className="btn btn-sm btn-outline-danger" onClick={bulkDelete} disabled={busy}>
                Delete ({selected.size})
              </button>
            )}
            <Link to="/compliance/check" className="btn btn-sm btn-dark">Run check</Link>
          </div>
        }
      />

      <div className="cf-card">
        {loading ? <LoadingState />
          : runs.length === 0 ? <EmptyState icon="bi-shield-check" title="No compliance checks yet"
              action={<Link to="/compliance/check" className="btn btn-sm btn-dark">Run first check</Link>} />
          : <DataTable columns={[
              isAdmin ? <input type="checkbox" className="form-check-input" checked={selected.size === runs.length} onChange={toggleAll} /> : '',
              'Checked on', 'Cases', 'Checks', 'Result', '',
            ]}>
              {runs.map(r => {
                const allPass = r.fails === 0
                return (
                  <tr key={r.runId}>
                    {isAdmin && <td><input type="checkbox" className="form-check-input" checked={selected.has(r.runId)} onChange={() => toggle(r.runId)} /></td>}
                    {!isAdmin && <td></td>}
                    <td>
                      <div className="fw-semibold" style={{ fontSize: 13 }}>{formatDateTime(r.runDate) || formatDate(r.date) || '—'}</div>
                      {r.legacy && <span className="badge text-bg-light border mt-1" style={{ fontSize: 10 }}>legacy</span>}
                    </td>
                    <td>{r.cases ?? '—'}</td>
                    <td>
                      <span className="text-success me-1">{r.passes ?? 0} passed</span>
                      {(r.fails ?? 0) > 0 && <span className="text-danger">{r.fails} failed</span>}
                    </td>
                    <td>
                      {allPass
                        ? <span className="badge rounded-pill" style={{ background: '#dcfce7', color: '#16a34a', fontSize: 12 }}>
                            <i className="bi bi-check-circle-fill me-1" />All clear
                          </span>
                        : <span className="badge rounded-pill" style={{ background: '#fee2e2', color: '#dc2626', fontSize: 12 }}>
                            <i className="bi bi-exclamation-triangle-fill me-1" />{r.fails} failure{r.fails !== 1 ? 's' : ''}
                          </span>
                      }
                    </td>
                    <td><button className="btn btn-sm btn-outline-secondary" onClick={() => navigate(`/compliance/runs/${r.runId}`)}>Open</button></td>
                  </tr>
                )
              })}
            </DataTable>}
      </div>
    </div>
  )
}
