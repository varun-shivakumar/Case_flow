import { useState } from 'react'
import { Link } from 'react-router-dom'
import { compliance } from '../../api/services'
import { useAuth } from '../../context/AuthContext'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import FormField from '../../components/ui/FormField'
import EmptyState from '../../components/ui/EmptyState'
import LoadingState from '../../components/ui/LoadingState'
import { CaseSlaSummary, useCaseEnrichment } from './ComplianceRunDetail'
import { notify } from '../../components/ui/Toast'

export default function RunComplianceCheck() {
  const { user } = useAuth()

  const [caseIdsInput, setCaseIdsInput] = useState('')
  const [dateFrom, setDateFrom] = useState('')
  const [dateTo, setDateTo]     = useState('')
  const [running, setRunning]   = useState(false)
  const [results, setResults]   = useState(null)
  const [checkedIds, setCheckedIds] = useState([])

  const [auditScope, setAuditScope]       = useState('')
  const [auditFindings, setAuditFindings] = useState('')
  const [creatingAudit, setCreatingAudit] = useState(false)

  const { data, loading: enriching } = useCaseEnrichment(checkedIds)

  const today = new Date().toISOString().split('T')[0]
  const runCheck = async (e) => {
    e.preventDefault()
    setRunning(true)
    try {
      const caseIds = caseIdsInput.split(/[,\s]+/).map(s => s.trim()).filter(Boolean).map(Number)
      const body = { caseIds, dateFrom: dateFrom || null, dateTo: dateTo || null }
      const res = await compliance.runCheck(body)
      setResults(res)
      setCheckedIds(caseIds.length > 0 ? caseIds : [...new Set((Array.isArray(res) ? res : []).map(r => r.caseId))])
      notify.success('Compliance check completed')
    } catch (e) { notify.error(e.message) }
    finally { setRunning(false) }
  }

  const createAudit = async (e) => {
    e.preventDefault()
    setCreatingAudit(true)
    try {
      await compliance.createAudit({ scope: auditScope, findings: auditFindings || undefined })
      notify.success('Audit created')
      setAuditScope(''); setAuditFindings('')
    } catch (e) { notify.error(e.message) }
    finally { setCreatingAudit(false) }
  }

  return (
    <div>
      <PageHeader
        title="Run compliance check"
        subtitle="Run automated checks on selected cases or all active cases"
        action={<Link to="/compliance" className="btn btn-sm btn-outline-secondary">History</Link>}
      />

      <SectionCard title="Run parameters" className="mb-3">
        <form onSubmit={runCheck}>
          <FormField label="Case IDs" hint="Comma or space separated. Leave blank to check all cases.">
            <input className="form-control form-control-sm" value={caseIdsInput}
              onChange={e => setCaseIdsInput(e.target.value)} placeholder="e.g. 12, 18, 24" />
          </FormField>
          <div className="row g-2">
            <div className="col-md-6">
              <FormField label="Date from"><input type="date" className="form-control form-control-sm" value={dateFrom} onChange={e => setDateFrom(e.target.value)} /></FormField>
            </div>
            <div className="col-md-6">
              <FormField label="Date to"><input type="date" className="form-control form-control-sm" value={dateTo} onChange={e => setDateTo(e.target.value)} /></FormField>
            </div>
          </div>
          <button className="btn btn-dark btn-sm" disabled={running}>
            {running ? <><span className="spinner-border spinner-border-sm me-2" />Running...</> : 'Run check'}
          </button>
        </form>
      </SectionCard>

      {results !== null && (
        <>
          {checkedIds.length === 0
            ? <EmptyState icon="bi-shield-check" title="No cases were checked" />
            : <div className="d-flex flex-column gap-3 mb-3">
                {enriching && <LoadingState label="Loading case data..." />}
                {checkedIds.map(id => {
                  const e = data[id] || { stages: [], slaRecords: [], docs: [], caseInfo: null }
                  return <CaseSlaSummary key={id} caseId={id} {...e} />
                })}
              </div>}

          <SectionCard title="Create audit record">
            <form onSubmit={createAudit}>
              <div className="row g-2">
                <div className="col-md-4">
                  <FormField label="Scope" required>
                    <input className="form-control form-control-sm" value={auditScope}
                      onChange={e => setAuditScope(e.target.value)} placeholder="e.g. Q1 review" required />
                  </FormField>
                </div>
                <div className="col-md-8">
                  <FormField label="Findings">
                    <input className="form-control form-control-sm" value={auditFindings}
                      onChange={e => setAuditFindings(e.target.value)} placeholder="Summary of compliance findings" />
                  </FormField>
                </div>
              </div>
              <button className="btn btn-sm btn-dark" disabled={creatingAudit || !auditScope.trim()}>
                {creatingAudit ? 'Creating...' : 'Create audit'}
              </button>
            </form>
          </SectionCard>
        </>
      )}
    </div>
  )
}
