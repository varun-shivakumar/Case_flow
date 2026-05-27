import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { hearings, cases } from '../../api/services'
import { HEARING_STATUS, formatDate } from '../../utils/constants'
import { useAuth } from '../../context/AuthContext'
import PageHeader from '../../components/ui/PageHeader'
import StatusBadge from '../../components/ui/StatusBadge'
import EmptyState from '../../components/ui/EmptyState'
import LoadingState from '../../components/ui/LoadingState'
import DataTable from '../../components/ui/DataTable'
import SectionCard from '../../components/ui/SectionCard'
import { notify } from '../../components/ui/Toast'

const STATUS_TONE = {
  SCHEDULED:   'blue',
  RESCHEDULED: 'peach',
  COMPLETED:   'green',
  CANCELLED:   'rose',
}

export default function HearingList() {
  const { user } = useAuth()
  const [list, setList]       = useState([])
  const [filter, setFilter]   = useState('')
  const [loading, setLoading] = useState(true)

  // Fetch everything once, filter client-side so the summary strip stays accurate.
  const load = async () => {
    setLoading(true)
    try {
      let hearingsData
      if (user?.role === 'JUDGE') {
        hearingsData = await hearings.byJudge(user.userId)
      } else if (user?.role === 'LITIGANT' || user?.role === 'LAWYER') {
        const userCases = user.role === 'LITIGANT'
          ? await cases.byLitigant(user.userId)
          : await cases.byLawyer(user.userId)
        const results = await Promise.all((userCases || []).map(c => hearings.byCase(c.caseId)))
        hearingsData = results.flat()
      } else {
        hearingsData = await hearings.list()
      }
      setList(hearingsData || [])
    }
    catch (e) { notify.error(e.message) }
    finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  const counts = useMemo(() => {
    const c = { TOTAL: list.length }
    HEARING_STATUS.forEach(s => { c[s] = list.filter(r => r.status === s).length })
    return c
  }, [list])

  const visible = filter ? list.filter(h => h.status === filter) : list
  const canSchedule = ['CLERK'].includes(user?.role)

  return (
    <div>
      <PageHeader
        title="Hearings"
        subtitle={`${list.length} hearing${list.length !== 1 ? 's' : ''} on the calendar`}
        action={canSchedule && <Link to="/hearings/schedule" className="btn btn-dark">+ Schedule hearing</Link>}
      />

      {/* Status summary strip */}
      <div className="row g-3 mb-3">
        <div className="col-6 col-md-3 col-lg-2">
          <button onClick={() => setFilter('')}
            className={`cf-stat w-100 text-start border ${filter === '' ? 'border-dark' : ''}`}
            style={{ cursor: 'pointer' }}>
            <div className="cf-stat__value">{counts.TOTAL}</div>
            <div className="cf-stat__label">Total</div>
          </button>
        </div>
        {HEARING_STATUS.map(s => (
          <div className="col-6 col-md-3 col-lg-2" key={s}>
            <button onClick={() => setFilter(filter === s ? '' : s)}
              className={`cf-stat w-100 text-start border ${filter === s ? 'border-dark' : ''}`}
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

      {/* List */}
      <SectionCard padded={false}
        title={filter ? `${filter} hearings` : (['JUDGE', 'LITIGANT', 'LAWYER'].includes(user?.role) ? 'My hearings' : 'All hearings')}
        action={
          <div className="d-flex gap-2">
            {filter && (
              <button className="btn btn-sm btn-outline-secondary" onClick={() => setFilter('')}>
                <i className="bi bi-x-lg me-1" />Clear filter
              </button>
            )}
            <button className="btn btn-sm btn-outline-secondary" onClick={load}>
              <i className="bi bi-arrow-clockwise me-1" />Refresh
            </button>
          </div>
        }
      >
        {loading ? <LoadingState />
          : visible.length === 0 ? <EmptyState icon="bi-calendar2-x"
              title="No hearings found"
              hint={filter ? `No hearings with status "${filter}".` : null}
              action={canSchedule && !filter && <Link to="/hearings/schedule" className="btn btn-sm btn-dark">Schedule hearing</Link>} />
          : <DataTable columns={['ID', 'Case', 'Judge', 'Date', 'Time', 'Status', '']}>
              {visible.map(h => (
                <tr key={h.hearingId}>
                  <td className="fw-semibold">#{h.hearingId}</td>
                  <td><Link to={`/cases/${h.caseId}`} className="text-dark">#{h.caseId}</Link></td>
                  <td className="cf-muted">{h.judgeId}</td>
                  <td>{formatDate(h.hearingDate)}</td>
                  <td className="cf-muted">{h.hearingTime}</td>
                  <td><StatusBadge status={h.status} /></td>
                  <td><Link to={`/hearings/${h.hearingId}`} className="btn btn-sm btn-outline-secondary">Open</Link></td>
                </tr>
              ))}
            </DataTable>}
      </SectionCard>
    </div>
  )
}
