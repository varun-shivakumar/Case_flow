import { useEffect, useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { compliance, cases, workflow } from '../../api/services'
import { formatDate, formatDateTime } from '../../utils/constants'
import PageHeader from '../../components/ui/PageHeader'
import StatusBadge from '../../components/ui/StatusBadge'
import SectionCard from '../../components/ui/SectionCard'
import EmptyState from '../../components/ui/EmptyState'
import LoadingState from '../../components/ui/LoadingState'
import DataTable from '../../components/ui/DataTable'
import { notify } from '../../components/ui/Toast'

export function useCaseEnrichment(caseIds) {
  const [data, setData] = useState({})
  const [loading, setLoading] = useState(false)

  useEffect(() => {
    if (!caseIds || caseIds.length === 0) return
    let active = true
    setLoading(true)
    Promise.all(caseIds.map(async (id) => {
      const [c, st, sla, ds] = await Promise.allSettled([
        cases.get(id), workflow.stages(id), workflow.sla(id), cases.docs(id),
      ])
      return [id, {
        caseInfo:   c.status   === 'fulfilled' ? c.value   : null,
        stages:     st.status  === 'fulfilled' ? (st.value  || []) : [],
        slaRecords: sla.status === 'fulfilled' ? (sla.value || []) : [],
        docs:       ds.status  === 'fulfilled' ? (ds.value  || []) : [],
      }]
    })).then(rows => {
      if (!active) return
      setData(Object.fromEntries(rows))
    }).finally(() => active && setLoading(false))
    return () => { active = false }
  }, [JSON.stringify(caseIds)])

  return { data, loading }
}

export function CaseSlaSummary({ caseId, stages, slaRecords, docs, caseInfo }) {
  const doc = useMemo(() => {
    const total    = docs?.length || 0
    const verified = (docs || []).filter(d => d.verificationStatus === 'VERIFIED').length
    const rejected = (docs || []).filter(d => d.verificationStatus === 'REJECTED').length
    const pending  = (docs || []).filter(d => d.verificationStatus === 'PENDING').length
    return { total, verified, rejected, pending }
  }, [docs])

  const stageState = useMemo(() => {
    if (!stages || stages.length === 0) return { label: 'Workflow not initiated', status: 'PENDING' }
    if ((slaRecords || []).some(s => s.status === 'BREACHED')) return { label: 'Has breached stages', status: 'BREACHED' }
    const active = stages.find(s => s.active)
    if (active) return { label: `In stage: ${active.stageName}`, status: 'ACTIVE' }
    if (stages.every(s => s.completedAt != null || s.skipped)) return { label: 'Workflow complete', status: 'COMPLETED' }
    return { label: 'No active stage', status: 'PENDING' }
  }, [stages, slaRecords])

  return (
    <SectionCard
      title={<>
        <Link to={`/cases/${caseId}`} className="text-dark me-2">Case #{caseId}</Link>
        {caseInfo?.title && <span className="cf-muted small">{caseInfo.title}</span>}
      </>}
      action={<StatusBadge status={stageState.status} />}
    >
      <div className="cf-muted small mb-3">{stageState.label}</div>

      <div className="row g-2 mb-3 small">
        <div className="col"><div className="cf-card p-2 text-center"><div className="cf-muted">Total docs</div><div className="fw-semibold h6 mb-0">{doc.total}</div></div></div>
        <div className="col"><div className="cf-card p-2 text-center"><div className="cf-muted">Verified</div><div className="fw-semibold h6 mb-0 text-success">{doc.verified}</div></div></div>
        <div className="col"><div className="cf-card p-2 text-center"><div className="cf-muted">Rejected</div><div className="fw-semibold h6 mb-0 text-danger">{doc.rejected}</div></div></div>
        <div className="col"><div className="cf-card p-2 text-center"><div className="cf-muted">Pending</div><div className="fw-semibold h6 mb-0 text-warning">{doc.pending}</div></div></div>
      </div>

      {stages.length > 0 && (
        <DataTable columns={['Seq', 'Stage', 'Role', 'SLA', 'Status']}>
          {stages.map(s => {
            const sla = (slaRecords || []).find(r => r.stageId === s.stageId)
            const status = sla?.status || (s.completedAt ? 'COMPLETED' : s.active ? 'ACTIVE' : s.skipped ? 'SKIPPED' : 'PENDING')
            return (
              <tr key={s.stageId}>
                <td>{s.sequenceNumber}</td>
                <td>{s.stageName}</td>
                <td className="cf-muted">{s.roleResponsible}</td>
                <td>{s.slaDays}d</td>
                <td><StatusBadge status={status} /></td>
              </tr>
            )
          })}
        </DataTable>
      )}
    </SectionCard>
  )
}

export default function ComplianceRunDetail() {
  const { runId } = useParams()
  const [records, setRecords] = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    let active = true
    compliance.runRecords(runId)
      .then(r => active && setRecords(r || []))
      .catch(e => active && notify.error(e.message))
      .finally(() => active && setLoading(false))
    return () => { active = false }
  }, [runId])

  const caseIds = useMemo(() => [...new Set((records || []).map(r => r.caseId))], [records])
  const { data, loading: enriching } = useCaseEnrichment(caseIds)

  return (
    <div>
      <PageHeader
        title={`Compliance run`}
        subtitle={`Run ID: ${runId} · ${records.length} record(s)`}
        action={<Link to="/compliance" className="btn btn-sm btn-outline-secondary">Back</Link>}
      />

      {loading ? <LoadingState />
        : records.length === 0 ? <EmptyState icon="bi-shield-x" title="No records in this run" />
        : <div className="d-flex flex-column gap-3">
            {enriching && <LoadingState label="Loading case data..." />}
            {caseIds.map(id => {
              const e = data[id] || { stages: [], slaRecords: [], docs: [], caseInfo: null }
              return <CaseSlaSummary key={id} caseId={id} {...e} />
            })}
          </div>}
    </div>
  )
}
