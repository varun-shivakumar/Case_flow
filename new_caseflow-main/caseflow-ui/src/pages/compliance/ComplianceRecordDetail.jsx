import { useEffect, useState } from 'react'
import { Link, useLocation, useParams } from 'react-router-dom'
import { compliance, cases, workflow } from '../../api/services'
import { formatDateTime } from '../../utils/constants'
import PageHeader from '../../components/ui/PageHeader'
import LoadingState from '../../components/ui/LoadingState'
import AlertMessage from '../../components/ui/AlertMessage'
import { CaseSlaSummary } from './ComplianceRunDetail'
import { notify } from '../../components/ui/Toast'

export default function ComplianceRecordDetail() {
  const { complianceId } = useParams()
  const { state }        = useLocation()

  const [record, setRecord]         = useState(state?.record || null)
  const [caseInfo, setCaseInfo]     = useState(null)
  const [stages, setStages]         = useState([])
  const [slaRecords, setSlaRecords] = useState([])
  const [docs, setDocs]             = useState([])
  const [loading, setLoading]       = useState(true)
  const [error, setError]           = useState('')

  useEffect(() => {
    let active = true
    async function load() {
      try {
        let rec = record
        if (!rec) {
          const page = await compliance.complianceRecordsPaginated(0, 500)
          rec = (page?.content || []).find(r => String(r.complianceId) === String(complianceId)) || null
          if (active) setRecord(rec)
        }
        if (!rec) { setError('Compliance record not found.'); return }

        const [c, st, sla, ds] = await Promise.allSettled([
          cases.get(rec.caseId), workflow.stages(rec.caseId), workflow.sla(rec.caseId), cases.docs(rec.caseId),
        ])
        if (!active) return
        if (c.status === 'fulfilled')   setCaseInfo(c.value)
        if (st.status === 'fulfilled')  setStages(st.value || [])
        if (sla.status === 'fulfilled') setSlaRecords(sla.value || [])
        if (ds.status === 'fulfilled')  setDocs(ds.value || [])
      } catch (e) { if (active) notify.error(e.message) }
      finally { if (active) setLoading(false) }
    }
    load()
    return () => { active = false }
  }, [complianceId])

  if (loading) return <div><PageHeader title="Compliance record" /><LoadingState /></div>
  if (error) return <div><PageHeader title="Compliance record" /><AlertMessage error={error} /></div>

  return (
    <div>
      <PageHeader
        title={`Compliance #${complianceId}`}
        subtitle={record ? `Case #${record.caseId} · ${formatDateTime(record.date)}` : null}
        action={<Link to="/compliance" className="btn btn-sm btn-outline-secondary">Back</Link>}
      />

      <CaseSlaSummary
        caseId={record.caseId}
        stages={stages}
        slaRecords={slaRecords}
        docs={docs}
        caseInfo={caseInfo}
      />
    </div>
  )
}
