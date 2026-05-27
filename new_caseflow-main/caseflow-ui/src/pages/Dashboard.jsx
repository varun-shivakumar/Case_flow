import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { useAuth } from '../context/AuthContext'
import { cases, hearings, appeals, workflow } from '../api/services'
import { formatDate } from '../utils/constants'
import StatusBadge from '../components/ui/StatusBadge'
import EmptyState from '../components/ui/EmptyState'
import DataTable from '../components/ui/DataTable'

const STAT_CARDS = [
  { key: 'cases',    to: '/cases',    label: 'Cases',        icon: 'bi-folder2' },
  { key: 'hearings', to: '/hearings', label: 'Hearings',     icon: 'bi-calendar2' },
  { key: 'appeals',  to: '/appeals',  label: 'Appeals',      icon: 'bi-arrow-counterclockwise' },
  { key: 'breached', to: '/workflow', label: 'SLA breached', icon: 'bi-exclamation-circle', danger: true },
]

const MONTHS = ['Jan','Feb','Mar','Apr','May','Jun','Jul','Aug','Sep','Oct','Nov','Dec']

function getGreeting() {
  const h = new Date().getHours()
  if (h < 12) return 'Good morning'
  if (h < 17) return 'Good afternoon'
  return 'Good evening'
}

// SVG line chart for cases filed per month — ADMIN only.
function CasesLineChart({ data }) {
  const W = 520, H = 130, padL = 28, padR = 12, padT = 18, padB = 22
  const innerW = W - padL - padR
  const innerH = H - padT - padB
  const max = Math.max(...data.map(d => d.value), 1)

  const px = i => padL + (i / (data.length - 1)) * innerW
  const py = v => padT + innerH - (v / max) * innerH

  const points = data.map((d, i) => `${px(i)},${py(d.value)}`).join(' ')
  const areaPath =
    `M ${px(0)} ${py(data[0].value)} ` +
    data.slice(1).map((d, i) => `L ${px(i + 1)} ${py(d.value)}`).join(' ') +
    ` L ${px(data.length - 1)} ${padT + innerH} L ${padL} ${padT + innerH} Z`

  return (
    <svg width="100%" viewBox={`0 0 ${W} ${H}`} style={{ display: 'block', overflow: 'visible' }}>
      <defs>
        <linearGradient id="lgGrad" x1="0" y1="0" x2="0" y2="1">
          <stop offset="0%" stopColor="var(--cf-muted, #8b827a)" stopOpacity="0.08" />
          <stop offset="100%" stopColor="var(--cf-muted, #8b827a)" stopOpacity="0" />
        </linearGradient>
      </defs>

      {/* Subtle horizontal guides — only at 0, 50%, 100% */}
      {[0, 0.5, 1].map(pct => {
        const y = padT + innerH - pct * innerH
        return (
          <g key={pct}>
            <line x1={padL} y1={y} x2={W - padR} y2={y}
              stroke="var(--cf-border, #ebe4d8)" strokeWidth={1}
              strokeDasharray={pct === 0 ? '0' : '3 4'} />
            <text x={padL - 4} y={y + 3} textAnchor="end" fontSize={7.5}
              fill="var(--cf-muted, #8b827a)">
              {Math.round(max * pct)}
            </text>
          </g>
        )
      })}

      {/* Filled area */}
      <path d={areaPath} fill="url(#lgGrad)" />

      {/* Line — uses muted color so it stays soft */}
      <polyline points={points} fill="none"
        stroke="var(--cf-muted, #8b827a)" strokeWidth={1.5}
        strokeLinejoin="round" strokeLinecap="round" />

      {/* Dots + labels */}
      {data.map((d, i) => {
        const x = px(i), y = py(d.value)
        return (
          <g key={i}>
            <circle cx={x} cy={y} r={d.current ? 4 : 2.5}
              fill={d.current ? 'var(--cf-text, #2d2a26)' : 'var(--cf-surface, #fff)'}
              stroke="var(--cf-muted, #8b827a)"
              strokeWidth={d.current ? 0 : 1} />
            {d.current && d.value > 0 && (
              <text x={x} y={y - 8} textAnchor="middle" fontSize={8.5}
                fontWeight={600} fill="var(--cf-text, #2d2a26)">
                {d.value}
              </text>
            )}
            <text x={x} y={H - 3} textAnchor="middle" fontSize={8.5}
              fill={d.current ? 'var(--cf-text-soft, #4d4943)' : 'var(--cf-muted, #8b827a)'}
              fontWeight={d.current ? 600 : 400}>
              {d.label}
            </text>
          </g>
        )
      })}
    </svg>
  )
}

export default function Dashboard() {
  const { user } = useAuth()
  const [stats, setStats]             = useState({ cases: 0, hearings: 0, appeals: 0, breached: 0 })
  const [recentCases, setRecentCases] = useState([])
  const [monthlyData, setMonthlyData] = useState([])

  useEffect(() => {
    let active = true
    async function load() {
      const casesFetch =
        user?.role === 'LITIGANT' ? cases.byLitigant(user.userId) :
        user?.role === 'LAWYER'   ? cases.byLawyer(user.userId || user.email) :
                                    cases.list()

      const appealsFetch =
        user?.role === 'LITIGANT' ? appeals.byUser(user.userId) :
                                    appeals.paginated(0, 5)

      const hearingsFetch =
        user?.role === 'JUDGE' ? hearings.byJudge(user.userId) :
                                 hearings.list()

      const [c, h, a, sla] = await Promise.allSettled([
        casesFetch, hearingsFetch, appealsFetch, workflow.breached(),
      ])
      if (!active) return

      setStats({
        cases:    c.status === 'fulfilled' ? (c.value?.length || 0) : 0,
        hearings: h.status === 'fulfilled' ? (h.value?.length || 0) : 0,
        appeals:  a.status === 'fulfilled' ? (a.value?.length || a.value?.totalElements || a.value?.content?.length || 0) : 0,
        breached: sla.status === 'fulfilled' ? (sla.value?.length || 0) : 0,
      })

      if (c.status === 'fulfilled') {
        const allCases = c.value || []
        const sorted = allCases.slice().sort((x, y) => {
          const dx = x.filedDate ? new Date(x.filedDate).getTime() : 0
          const dy = y.filedDate ? new Date(y.filedDate).getTime() : 0
          return dy !== dx ? dy - dx : (y.caseId || 0) - (x.caseId || 0)
        })
        setRecentCases(sorted.slice(0, 5))

        // Aggregate cases filed per month for the current year
        const year = new Date().getFullYear()
        const currentMonth = new Date().getMonth()
        const counts = Array(12).fill(0)
        allCases.forEach(cs => {
          if (!cs.filedDate) return
          const d = new Date(cs.filedDate)
          if (d.getFullYear() === year) counts[d.getMonth()]++
        })
        setMonthlyData(MONTHS.map((label, i) => ({ label, value: counts[i], current: i === currentMonth })))
      }
    }
    load()
    return () => { active = false }
  }, [user])

  return (
    <div>
      <div className="mb-4">
        <h1 className="page-title h4 mb-1">{getGreeting()}, {user?.name}</h1>
        <p className="cf-muted small mb-0">Here's a snapshot of your case activity.</p>
      </div>

      <div className="row g-3 mb-4">
        {STAT_CARDS.filter(s => !(s.key === 'breached' && user?.role === 'LITIGANT')).map(s => (
          <div className="col-6 col-md-3" key={s.key}>
            <Link to={s.to} className="text-decoration-none">
              <div className="cf-card p-3 h-100">
                <div className="d-flex align-items-center justify-content-between mb-2">
                  <i className={`bi ${s.icon} cf-muted`} />
                  <span className="cf-muted small">{s.label}</span>
                </div>
                <div className={`h3 fw-semibold mb-0 ${s.danger && stats[s.key] > 0 ? 'text-danger' : ''}`}>
                  {stats[s.key]}
                </div>
              </div>
            </Link>
          </div>
        ))}
      </div>


      <div className="cf-card">
        <div className="d-flex justify-content-between align-items-center px-3 py-2 border-bottom">
          <h6 className="mb-0 fw-semibold">Recent cases</h6>
          <Link to="/cases" className="small cf-muted">View all</Link>
        </div>
        {recentCases.length === 0 ? (
          <EmptyState icon="bi-folder2-open" title="No cases yet"
            hint={user?.role === 'LITIGANT' ? 'File your first case to get started.' : 'Cases will appear here once filed.'}
            action={user?.role === 'LITIGANT' && <Link to="/cases/file" className="btn btn-dark btn-sm">File a case</Link>} />
        ) : (
          <DataTable columns={['ID', 'Title', 'Status', 'Filed', '']}>
            {recentCases.map(c => (
              <tr key={c.caseId}>
                <td className="fw-semibold">#{c.caseId}</td>
                <td>{c.title}</td>
                <td><StatusBadge status={c.status} /></td>
                <td className="cf-muted">{formatDate(c.filedDate)}</td>
                <td><Link to={`/cases/${c.caseId}`} className="btn btn-sm btn-outline-secondary">Open</Link></td>
              </tr>
            ))}
          </DataTable>
        )}
      </div>
    </div>
  )
}
