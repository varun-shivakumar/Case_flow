import { useState } from 'react'
import { Link } from 'react-router-dom'
import { workflow } from '../../api/services'
import { useAuth } from '../../context/AuthContext'
import PageHeader from '../../components/ui/PageHeader'
import EmptyState from '../../components/ui/EmptyState'
import { notify } from '../../components/ui/Toast'
import StageProgressCard from './components/StageProgressCard'
import ActiveStageCard from './components/ActiveStageCard'
import StageActionsPanel from './components/StageActionsPanel'
import SlaTrackingGrid from './components/SlaTrackingGrid'
import StagesTable from './components/StagesTable'

export default function WorkflowDashboard() {
  const { user } = useAuth()
  const isAdmin = ['ADMIN', 'CLERK'].includes(user?.role)

  const [caseIdInput, setCaseIdInput] = useState('')
  const [loadedCaseId, setLoadedCaseId] = useState(null)
  const [stages, setStages]   = useState([])
  const [current, setCurrent] = useState(null)
  const [slaRecords, setSla]  = useState([])
  const [loading, setLoading] = useState(false)

  const loadCase = async (id) => {
    if (!id) return
    setLoading(true)
    try {
      const [s, c, r] = await Promise.allSettled([workflow.stages(id), workflow.current(id), workflow.sla(id)])
      setStages(s.status === 'fulfilled' ? (s.value || []) : [])
      setCurrent(c.status === 'fulfilled' ? c.value : null)
      setSla(r.status === 'fulfilled' ? (r.value || []) : [])
      setLoadedCaseId(id)
      if (s.status === 'rejected') notify.error(s.reason?.message || 'Failed to load workflow')
    } catch (e) { notify.error(e.message) }
    finally { setLoading(false) }
  }

  const handleSearch = (e) => { e.preventDefault(); if (caseIdInput.trim()) loadCase(caseIdInput.trim()) }

  const wrap = async (label, fn) => {
    try { await fn(); notify.success(label); loadCase(loadedCaseId) }
    catch (e) { notify.error(e.message) }
  }
  const advance  = () => wrap('Advanced',    () => workflow.advance(loadedCaseId))
  const rollback = () => wrap('Rolled back', () => workflow.rollback(loadedCaseId))

  return (
    <div>
      <PageHeader
        title="Workflow"
        subtitle="Search a case to view and manage its workflow"
        action={isAdmin && <Link to="/workflow/sla" className="btn btn-sm btn-outline-secondary">SLA monitoring</Link>}
      />

      <div className="cf-card p-3 mb-3">
        <form onSubmit={handleSearch} className="d-flex gap-2 align-items-center">
          <input className="form-control form-control-sm" placeholder="Enter Case ID..."
            value={caseIdInput} onChange={e => setCaseIdInput(e.target.value)} style={{ maxWidth: 300 }} />
          <button type="submit" className="btn btn-sm btn-dark" disabled={loading}>
            {loading && <span className="spinner-border spinner-border-sm me-1" />}Load
          </button>
        </form>
      </div>

      {!loadedCaseId && <EmptyState icon="bi-diagram-3" title="Enter a case ID to view its workflow" />}

      {loadedCaseId && stages.length === 0 && (
        <EmptyState icon="bi-diagram-3" title={`No workflow for case #${loadedCaseId}`}
          action={isAdmin && <Link to={`/workflow/${loadedCaseId}`} className="btn btn-sm btn-dark">Initialize</Link>} />
      )}

      {loadedCaseId && stages.length > 0 && <>
        <StageProgressCard stages={stages} title={`Case #${loadedCaseId} progress`} showBadges className="mb-3" />
        <ActiveStageCard current={current} onAdvance={advance} onRollback={rollback} showActions={isAdmin} className="mb-3" />
        {isAdmin && <StageActionsPanel caseId={loadedCaseId} onComplete={() => loadCase(loadedCaseId)} className="mb-3" />}
        <SlaTrackingGrid records={slaRecords} className="mb-3" />
        <StagesTable stages={stages} />
      </>}
    </div>
  )
}
