import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { cases } from '../../api/services'
import { CASE_STATUS, formatDate } from '../../utils/constants'
import { useAuth } from '../../context/AuthContext'
import PageHeader from '../../components/ui/PageHeader'
import StatusBadge from '../../components/ui/StatusBadge'
import EmptyState from '../../components/ui/EmptyState'
import LoadingState from '../../components/ui/LoadingState'
import DataTable from '../../components/ui/DataTable'
import SectionCard from '../../components/ui/SectionCard'
import { notify } from '../../components/ui/Toast'

// Pastel tone per case status — keeps the summary strip visually distinct.
const STATUS_TONE = {
  FILED:     'blue',
  ACTIVE:    'green',
  ADJOURNED: 'peach',
  CLOSED:    'purple',
}

export default function CaseList() {
  const { user } = useAuth()
  const [list, setList]       = useState([])
  const [filter, setFilter]   = useState('')
  const [loading, setLoading] = useState(true)

  const isLitigant = user?.role === 'LITIGANT'
  const isLawyer   = user?.role === 'LAWYER'

  // Always load the full set the user can see — we filter client-side so the
  // status-count strip stays accurate.
  const load = async () => {
    setLoading(true)
    try {
      const data = isLitigant
        ? await cases.byLitigant(encodeURIComponent(user.userId))
        : isLawyer
          ? await cases.byLawyer(user.userId || user.email)
          : await cases.list()
      const sorted = (data || []).slice().sort((a, b) => {
        const da = a.filedDate ? new Date(a.filedDate).getTime() : 0
        const db = b.filedDate ? new Date(b.filedDate).getTime() : 0
        return db !== da ? db - da : (b.caseId || 0) - (a.caseId || 0)
      })
      setList(sorted)
    } catch (e) { notify.error(e.message) }
    finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  const counts = useMemo(() => {
    const c = { TOTAL: list.length }
    CASE_STATUS.forEach(s => { c[s] = list.filter(r => r.status === s).length })
    return c
  }, [list])

  const visible = filter ? list.filter(c => c.status === filter) : list
  const canFile = isLitigant || user?.role === 'CLERK'

  return (
    <div>
      <PageHeader
        title="Cases"
        subtitle={`${list.length} case${list.length !== 1 ? 's' : ''} ${isLitigant ? 'filed by you' : isLawyer ? 'assigned to you' : 'in the system'}`}
        action={canFile && <Link to="/cases/file" className="btn btn-dark">+ File case</Link>}
      />

      {/* Status summary strip — click a tile to filter */}
      <div className="row g-3 mb-3">
        <div className="col-6 col-md-3 col-lg-2">
          <button onClick={() => setFilter('')}
            className={`cf-stat w-100 text-start border ${filter === '' ? 'border-dark' : ''}`}
            style={{ cursor: 'pointer' }}>
            <div className="cf-stat__value">{counts.TOTAL}</div>
            <div className="cf-stat__label">Total</div>
          </button>
        </div>
        {CASE_STATUS.map(s => (
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
        title={filter ? `${filter} cases` : 'All cases'}
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
          : visible.length === 0 ? <EmptyState icon="bi-folder2-open"
              title="No cases found"
              hint={filter ? `No cases with status "${filter}".` : null}
              action={canFile && !filter && <Link to="/cases/file" className="btn btn-sm btn-dark">File a case</Link>} />
          : <DataTable columns={['ID', 'Title', 'Litigant', 'Lawyer', 'Status', 'Filed', '']}>
              {visible.map(c => (
                <tr key={c.caseId}>
                  <td className="fw-semibold">#{c.caseId}</td>
                  <td>{c.title}</td>
                  <td className="cf-muted">{c.litigantId}</td>
                  <td className="cf-muted">{c.lawyerId || '—'}</td>
                  <td><StatusBadge status={c.status} /></td>
                  <td className="cf-muted">{formatDate(c.filedDate)}</td>
                  <td><Link to={`/cases/${c.caseId}`} className="btn btn-sm btn-outline-secondary">Open</Link></td>
                </tr>
              ))}
            </DataTable>}
      </SectionCard>
    </div>
  )
}
