import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { appeals } from '../../api/services'
import { APPEAL_STATUS, REVIEW_OUTCOME_LABELS, formatDateTime } from '../../utils/constants'
import { useAuth } from '../../context/AuthContext'
import PageHeader from '../../components/ui/PageHeader'
import StatusBadge from '../../components/ui/StatusBadge'
import EmptyState from '../../components/ui/EmptyState'
import LoadingState from '../../components/ui/LoadingState'
import DataTable from '../../components/ui/DataTable'
import SectionCard from '../../components/ui/SectionCard'
import { notify } from '../../components/ui/Toast'

const STATUS_TONE = {
  SUBMITTED: 'blue',
  REVIEWED:  'peach',
  DECIDED:   'green',
  CANCELLED: 'rose',
}

export default function AppealList() {
  const { user } = useAuth()
  const isJudge = user?.role === 'JUDGE'
  const isPrivileged = ['ADMIN', 'CLERK', 'JUDGE'].includes(user?.role)

  const defaultScope = isJudge ? 'JUDGE' : (isPrivileged ? 'ALL' : 'MY')
  const [scope, setScope]               = useState(defaultScope)
  const [statusFilter, setStatusFilter] = useState('')
  const [allRows, setAllRows]           = useState([])
  const [loading, setLoading]           = useState(true)

  const canFile = ['LITIGANT', 'LAWYER'].includes(user?.role)

  const load = async () => {
    setLoading(true)
    try {
      if (scope === 'JUDGE') {
        setAllRows(await appeals.myReviews() || [])
      } else if (scope === 'MY') {
        setAllRows(await appeals.mine() || [])
      } else {
        const p = await appeals.paginated(0, 200)
        setAllRows(Array.isArray(p) ? p : (p?.content || []))
      }
    } catch (e) { notify.error(e.message) }
    finally { setLoading(false) }
  }
  useEffect(() => { load() }, [scope])

  const counts = useMemo(() => {
    const c = { TOTAL: allRows.length }
    APPEAL_STATUS.forEach(s => { c[s] = allRows.filter(r => r.status === s).length })
    return c
  }, [allRows])

  const judgeTab = statusFilter || 'ALL'
  const visibleJudge = useMemo(() => {
    if (!isJudge) return []
    if (judgeTab === 'PENDING') return allRows.filter(r => !r.outcome)
    if (judgeTab === 'DECIDED') return allRows.filter(r => r.outcome)
    return allRows
  }, [allRows, judgeTab, isJudge])

  const visible = isJudge ? visibleJudge : (statusFilter ? allRows.filter(a => a.status === statusFilter) : allRows)

  const cancelAppeal = async (id) => {
    if (!confirm(`Cancel appeal #${id}?`)) return
    try {
      await appeals.cancel(id)
      notify.success(`Appeal #${id} cancelled`)
      load()
    } catch (e) { notify.error(e.message) }
  }

  const isMine = (a) => a.filedByUserId === user?.userId || a.filedByUserId === user?.email
  const canCancel = (a) =>
    (isMine(a) && a.status === 'SUBMITTED') ||
    (user?.role === 'ADMIN' && ['SUBMITTED','REVIEWED'].includes(a.status))

  const pending = isJudge ? allRows.filter(r => !r.outcome) : []
  const decided = isJudge ? allRows.filter(r => r.outcome)  : []

  return (
    <div>
      <PageHeader
        title="Appeals"
        subtitle={isJudge ? 'Appeals assigned to you' : scope === 'MY' ? 'Appeals you have filed' : 'All appeals in the system'}
        action={
          <div className="d-flex gap-2 flex-wrap">
{/*             {(user?.role === 'CLERK') && isPrivileged && */}
{/*               <Link to="/appeals/reviews/judge" className="btn btn-sm btn-outline-secondary">Lookup judge</Link>} */}
            {canFile && <Link to="/appeals/file" className="btn btn-dark">+ File appeal</Link>}
          </div>
        }
      />

      {/* Scope toggle — hidden for judges (they always see their assigned appeals) */}
      {isPrivileged && !isJudge && (
        <div className="d-flex gap-2 mb-3">
          <button className={`btn btn-sm btn-${scope === 'ALL' ? 'dark' : 'outline-secondary'}`}
            onClick={() => { setScope('ALL'); setStatusFilter('') }}>All appeals</button>
        </div>
      )}

      {/* Judge: summary tiles */}
      {isJudge && (
        <div className="row g-3 mb-3">
          <div className="col-6 col-md-3 col-lg-2">
            <button onClick={() => setStatusFilter('')}
              className={`cf-stat w-100 text-start border ${judgeTab === 'ALL' ? 'border-dark' : ''}`}
              style={{ cursor: 'pointer' }}>
              <div className="cf-stat__value">{allRows.length}</div>
              <div className="cf-stat__label">Total</div>
            </button>
          </div>
          <div className="col-6 col-md-3 col-lg-2">
            <button onClick={() => setStatusFilter(judgeTab === 'PENDING' ? '' : 'PENDING')}
              className={`cf-stat w-100 text-start border ${judgeTab === 'PENDING' ? 'border-dark' : ''}`}
              style={{ cursor: 'pointer' }}>
              <div className="d-flex justify-content-between align-items-start">
                <div className="cf-stat__value">{pending.length}</div>
                <span className="cf-chip cf-chip--peach"
                  style={{ width: 24, height: 24, fontSize: 11, borderRadius: 999 }}>
                  <i className="bi bi-circle-fill" style={{ fontSize: 6 }} />
                </span>
              </div>
              <div className="cf-stat__label">Pending</div>
            </button>
          </div>
          <div className="col-6 col-md-3 col-lg-2">
            <button onClick={() => setStatusFilter(judgeTab === 'DECIDED' ? '' : 'DECIDED')}
              className={`cf-stat w-100 text-start border ${judgeTab === 'DECIDED' ? 'border-dark' : ''}`}
              style={{ cursor: 'pointer' }}>
              <div className="d-flex justify-content-between align-items-start">
                <div className="cf-stat__value">{decided.length}</div>
                <span className="cf-chip cf-chip--green"
                  style={{ width: 24, height: 24, fontSize: 11, borderRadius: 999 }}>
                  <i className="bi bi-circle-fill" style={{ fontSize: 6 }} />
                </span>
              </div>
              <div className="cf-stat__label">Decided</div>
            </button>
          </div>
        </div>
      )}

      {/* Status summary strip — hidden for judges */}
      {!isJudge && (
        <div className="row g-3 mb-3">
          <div className="col-6 col-md-3 col-lg-2">
            <button onClick={() => setStatusFilter('')}
              className={`cf-stat w-100 text-start border ${statusFilter === '' ? 'border-dark' : ''}`}
              style={{ cursor: 'pointer' }}>
              <div className="cf-stat__value">{counts.TOTAL}</div>
              <div className="cf-stat__label">Total</div>
            </button>
          </div>
          {APPEAL_STATUS.map(s => (
            <div className="col-6 col-md-3 col-lg-2" key={s}>
              <button onClick={() => setStatusFilter(statusFilter === s ? '' : s)}
                className={`cf-stat w-100 text-start border ${statusFilter === s ? 'border-dark' : ''}`}
                style={{ cursor: 'pointer' }}>
                <div className="d-flex justify-content-between align-items-start">
                  <div className="cf-stat__value">{counts[s]}</div>
                  <span className={`cf-chip cf-chip--${STATUS_TONE[s] || 'blue'}`}
                    style={{ width: 24, height: 24, fontSize: 11, borderRadius: 999 }}>
                    <i className="bi bi-circle-fill" style={{ fontSize: 6 }} />
                  </span>
                </div>
                <div className="cf-stat__label">{s}</div>
              </button>
            </div>
          ))}
        </div>
      )}

      {/* List */}
      <SectionCard padded={false}
        title={isJudge ? 'Assigned appeals' : statusFilter ? `${statusFilter} appeals` : 'All appeals'}
        action={
          <div className="d-flex gap-2">
            <button className="btn btn-sm btn-outline-secondary" onClick={load}>
              <i className="bi bi-arrow-clockwise me-1" />Refresh
            </button>
          </div>
        }
      >
        {loading ? <LoadingState />
          : visible.length === 0
            ? <EmptyState icon="bi-clipboard-check"
                title={isJudge ? (judgeTab === 'PENDING' ? 'No pending reviews' : 'No appeals assigned') : scope === 'MY' ? 'No appeals filed yet' : 'No appeals found'}
                hint={!isJudge && statusFilter ? `No appeals with status "${statusFilter}".` : null}
                action={canFile && !statusFilter && <Link to="/appeals/file" className="btn btn-sm btn-dark">File appeal</Link>} />
          : isJudge
            ? <DataTable columns={['Review', 'Appeal', 'Case', 'Assigned', 'Outcome', 'Remarks', '']}>
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
              </DataTable>
            : <DataTable columns={['ID', 'Case', 'Filed by', 'Filed on', 'Reason', 'Status', '']}>
                {visible.map(a => (
                  <tr key={a.appealId}>
                    <td className="fw-semibold">#{a.appealId}</td>
                    <td><Link to={`/cases/${a.caseId}`} className="text-dark">#{a.caseId}</Link></td>
                    <td className="cf-muted">{a.filedByUserId}</td>
                    <td className="cf-muted">{formatDateTime(a.filedDate)}</td>
                    <td style={{ maxWidth: 280 }}><div className="text-truncate" title={a.reason}>{a.reason}</div></td>
                    <td><StatusBadge status={a.status} /></td>
                    <td>
                      <div className="d-flex gap-1 flex-wrap">
                        <Link to={a.status === 'SUBMITTED' ? `/cases/${a.caseId}` : `/appeals/${a.appealId}`}
                          className="btn btn-sm btn-outline-secondary">Open</Link>
                        {canCancel(a) && (
                          <button className="btn btn-sm btn-outline-danger" onClick={() => cancelAppeal(a.appealId)}>Cancel</button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </DataTable>}
      </SectionCard>
    </div>
  )
}
