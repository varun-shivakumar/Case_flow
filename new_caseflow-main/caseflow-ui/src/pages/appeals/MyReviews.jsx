import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { appeals } from '../../api/services'
import { REVIEW_OUTCOME_LABELS, formatDateTime } from '../../utils/constants'
import PageHeader from '../../components/ui/PageHeader'
import StatusBadge from '../../components/ui/StatusBadge'
import EmptyState from '../../components/ui/EmptyState'
import LoadingState from '../../components/ui/LoadingState'
import DataTable from '../../components/ui/DataTable'
import { notify } from '../../components/ui/Toast'

export default function MyReviews() {
  const [list, setList]       = useState([])
  const [tab, setTab]         = useState('PENDING')
  const [loading, setLoading] = useState(true)

  const load = async () => {
    setLoading(true)
    try { setList(await appeals.myReviews() || []) }
    catch (e) { notify.error(e.message) }
    finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  const pending = list.filter(r => !r.outcome)
  const decided = list.filter(r => r.outcome)
  const visible = tab === 'PENDING' ? pending : tab === 'DECIDED' ? decided : list

  return (
    <div>
      <PageHeader
        title="My reviews"
        subtitle="Appeals assigned to you"
        action={
          <div className="d-flex gap-2">
            <Link to="/appeals" className="btn btn-sm btn-outline-secondary">All appeals</Link>
            <button className="btn btn-sm btn-outline-secondary" onClick={load}>Refresh</button>
          </div>
        }
      />

      <div className="cf-card">
        <div className="p-3 border-bottom">
          <div className="btn-group btn-group-sm">
            <button className={`btn btn-${tab === 'PENDING' ? 'dark' : 'outline-secondary'}`} onClick={() => setTab('PENDING')}>
              Pending ({pending.length})
            </button>
            <button className={`btn btn-${tab === 'DECIDED' ? 'dark' : 'outline-secondary'}`} onClick={() => setTab('DECIDED')}>
              Decided ({decided.length})
            </button>
            <button className={`btn btn-${tab === 'ALL' ? 'dark' : 'outline-secondary'}`} onClick={() => setTab('ALL')}>
              All ({list.length})
            </button>
          </div>
        </div>

        {loading ? <LoadingState />
          : visible.length === 0 ? <EmptyState icon="bi-clipboard-check" title={tab === 'PENDING' ? 'No pending reviews' : 'No reviews'} />
          : <DataTable columns={['Review', 'Appeal', 'Case', 'Assigned', 'Outcome', 'Remarks', '']}>
              {visible.map(r => (
                <tr key={r.reviewId}>
                  <td>#{r.reviewId}</td>
                  <td><Link to={`/appeals/${r.appealId}`} className="text-dark">#{r.appealId}</Link></td>
                  <td><Link to={`/cases/${r.caseId}`} className="text-dark">#{r.caseId}</Link></td>
                  <td className="cf-muted">{formatDateTime(r.reviewDate)}</td>
                  <td><StatusBadge status={r.outcome ? (REVIEW_OUTCOME_LABELS[r.outcome] || r.outcome) : 'PENDING'} /></td>
                  <td className="cf-muted" style={{ maxWidth: 240 }}>
                    <div className="text-truncate" title={r.remarks}>{r.remarks || '—'}</div>
                  </td>
                  <td><Link to={`/appeals/${r.appealId}`} className="btn btn-sm btn-outline-secondary">{r.outcome ? 'View' : 'Decide'}</Link></td>
                </tr>
              ))}
            </DataTable>}
      </div>
    </div>
  )
}
