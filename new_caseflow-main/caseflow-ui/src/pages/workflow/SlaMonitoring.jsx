import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { workflow } from '../../api/services'
import { formatDate } from '../../utils/constants'
import PageHeader from '../../components/ui/PageHeader'
import StatusBadge from '../../components/ui/StatusBadge'
import EmptyState from '../../components/ui/EmptyState'
import DataTable from '../../components/ui/DataTable'
import { notify } from '../../components/ui/Toast'
import StatTabCard from './components/StatTabCard'
import SlaProgressBar from './components/SlaProgressBar'

export default function SlaMonitoring() {
  const [breached, setBreached] = useState([])
  const [active, setActive]     = useState([])
  const [warnings, setWarnings] = useState([])
  const [tab, setTab]           = useState('active')

  const load = async () => {
    const [b, a, w] = await Promise.allSettled([workflow.breached(), workflow.active(), workflow.warnings()])
    if (b.status === 'fulfilled') setBreached(b.value || [])
    if (a.status === 'fulfilled') setActive(a.value || [])
    if (w.status === 'fulfilled') setWarnings(w.value || [])
  }
  useEffect(() => { load() }, [])

  const triggerCheck = async () => {
    try { const res = await workflow.checkSla(); notify.success(typeof res === 'string' ? res : 'SLA check triggered'); load() }
    catch (e) { notify.error(e.message) }
  }

  const records = tab === 'breached' ? breached : tab === 'warnings' ? warnings : active

  const STATS = [
    { key: 'active',   label: 'Active SLAs', value: active.length },
    { key: 'warnings', label: 'Warnings',    value: warnings.length },
    { key: 'breached', label: 'Breached',    value: breached.length, danger: true },
  ]

  return (
    <div>
      <PageHeader
        title="SLA monitoring"
        subtitle="Track SLA compliance across active workflows"
        action={
          <div className="d-flex gap-2">
            <Link to="/workflow" className="btn btn-sm btn-outline-secondary">Back</Link>
            <button className="btn btn-sm btn-dark" onClick={triggerCheck}>Trigger check</button>
          </div>
        }
      />

      <div className="row g-3 mb-3">
        {STATS.map(s => (
          <div className="col-md-4" key={s.key}>
            <StatTabCard label={s.label} value={s.value} active={tab === s.key} onClick={() => setTab(s.key)} danger={s.danger} />
          </div>
        ))}
      </div>

      <div className="cf-card">
        {records.length === 0
          ? <EmptyState icon="bi-check-circle" title={`No ${tab} SLA records`} />
          : <DataTable columns={['ID', 'Case', 'Stage', 'Status', 'Started', 'Deadline', 'Usage']}>
              {records.map(r => (
                <tr key={r.slaRecordId}>
                  <td>#{r.slaRecordId}</td>
                  <td><Link to={`/cases/${r.caseId}`} className="text-dark">#{r.caseId}</Link></td>
                  <td className="cf-muted">#{r.stageId}</td>
                  <td><StatusBadge status={r.status} /></td>
                  <td className="cf-muted">{formatDate(r.startDate)}</td>
                  <td className="cf-muted">{formatDate(r.endDate)}</td>
                  <td style={{ minWidth: 120 }}>
                    <SlaProgressBar percent={r.slaUsagePercent || 0} height={4} inline />
                  </td>
                </tr>
              ))}
            </DataTable>
        }
      </div>
    </div>
  )
}
