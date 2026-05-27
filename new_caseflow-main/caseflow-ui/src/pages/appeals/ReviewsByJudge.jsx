import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { appeals, users } from '../../api/services'
import { REVIEW_OUTCOME_LABELS, formatDateTime } from '../../utils/constants'
import PageHeader from '../../components/ui/PageHeader'
import StatusBadge from '../../components/ui/StatusBadge'
import EmptyState from '../../components/ui/EmptyState'
import LoadingState from '../../components/ui/LoadingState'
import DataTable from '../../components/ui/DataTable'
import FormField from '../../components/ui/FormField'
import { notify } from '../../components/ui/Toast'

export default function ReviewsByJudge() {
  const [judgeId, setJudgeId] = useState('')
  const [judges, setJudges]   = useState([])
  const [list, setList]       = useState(null)
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    users.byRole('JUDGE').then(d => setJudges(Array.isArray(d) ? d : [])).catch(() => setJudges([]))
  }, [])

  const search = async (selectedJudgeId) => {
    if (!selectedJudgeId) return
    setLoading(true); setList(null)
    try { setList(await appeals.reviewsByJudge(selectedJudgeId) || []) }
    catch (e) { notify.error(e.message) }
    finally { setLoading(false) }
  }

  const handleJudgeChange = (e) => {
    const selectedId = e.target.value
    setJudgeId(selectedId)
    if (selectedId) search(selectedId)
  }

  return (
    <div>
      <PageHeader
        title="Lookup judge"
        subtitle="View every review assigned to a judge"
        action={<Link to="/appeals" className="btn btn-sm btn-outline-secondary">Back</Link>}
      />

      <div className="cf-card p-3 mb-3">
        <form className="row g-2 align-items-end">
          <div className="col-md-12">
            <FormField label="Judge">
              <select className="form-select form-select-sm" value={judgeId}
                onChange={handleJudgeChange} autoFocus>
                <option value="">Select a judge...</option>
                {judges.map(j => (
                  <option key={j.userId} value={j.userId}>{j.name} ({j.userId})</option>
                ))}
              </select>
            </FormField>
          </div>
{/*           <div className="col-md-3"> */}
{/*             <button className="btn btn-dark btn-sm w-100 mb-3" disabled={loading || !judgeId}> */}
{/*               {loading ? 'Searching...' : 'Search'} */}
{/*             </button> */}
{/*           </div> */}
        </form>
      </div>

      {list !== null && (
        <div className="cf-card">
          {loading ? <LoadingState />
            : list.length === 0 ? <EmptyState icon="bi-clipboard-x" title="No reviews found" hint={`Selected judge has no recorded reviews.`} />
            : <DataTable columns={['Review', 'Appeal', 'Case', 'Assigned', 'Outcome', '']}>
                {list.map(r => (
                  <tr key={r.reviewId}>
                    <td>#{r.reviewId}</td>
                    <td><Link to={`/appeals/${r.appealId}`} className="text-dark">#{r.appealId}</Link></td>
                    <td><Link to={`/cases/${r.caseId}`} className="text-dark">#{r.caseId}</Link></td>
                    <td className="cf-muted">{formatDateTime(r.reviewDate)}</td>
                    <td><StatusBadge status={r.outcome ? (REVIEW_OUTCOME_LABELS[r.outcome] || r.outcome) : 'PENDING'} /></td>
                    <td><Link to={`/appeals/${r.appealId}`} className="btn btn-sm btn-outline-secondary">Open</Link></td>
                  </tr>
                ))}
              </DataTable>}
        </div>
      )}
    </div>
  )
}
