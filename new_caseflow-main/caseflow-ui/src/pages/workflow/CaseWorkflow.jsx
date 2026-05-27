import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { workflow } from '../../api/services'
import { CASE_TYPES } from '../../utils/constants'
import PageHeader from '../../components/ui/PageHeader'
import SectionCard from '../../components/ui/SectionCard'
import FormField from '../../components/ui/FormField'
import { notify } from '../../components/ui/Toast'
import StageProgressCard from './components/StageProgressCard'
import ActiveStageCard from './components/ActiveStageCard'
import StageActionsPanel from './components/StageActionsPanel'
import SlaTrackingGrid from './components/SlaTrackingGrid'
import StagesTable from './components/StagesTable'

const CASE_TYPE_STAGES = {
  civil:     ['A civil case usually involves disputes between individuals or organizations over rights, property, or contracts, and the goal is compensation or resolution rather than punishment.'],
  criminal:  ['A criminal case is brought by the state against an individual accused of breaking the law, with penalties such as fines, imprisonment, or community service if found guilty.'],
  corporate: ['A  corporate case deals with issues related to businesses, such as mergers, fraud, or shareholder disputes, often focusing on protecting economic interests and maintaining fair practices.'],
}

export default function CaseWorkflow() {
  const { caseId } = useParams()
  const [stages, setStages]   = useState([])
  const [current, setCurrent] = useState(null)
  const [slaRecords, setSla]  = useState([])

  const [initForm, setInitForm] = useState({ caseType: 'civil', mode: 'auto' })
  const [manualStages, setManual] = useState([{ sequenceNumber: 1, stageName: '', roleResponsible: '', slaDays: 7 }])

  const load = async () => {
    const [s, c, r] = await Promise.allSettled([workflow.stages(caseId), workflow.current(caseId), workflow.sla(caseId)])
    setStages(s.status === 'fulfilled' ? (s.value || []) : [])
    setCurrent(c.status === 'fulfilled' ? c.value : null)
    setSla(r.status === 'fulfilled' ? (r.value || []) : [])
  }
  useEffect(() => { load() }, [caseId])

  const wrap = async (label, fn) => {
    try { await fn(); notify.success(label); load() }
    catch (e) { notify.error(e.message) }
  }

  const initialize = (e) => {
    e.preventDefault()
    return wrap('Workflow initialized', () => {
      const body = { caseType: initForm.caseType, mode: initForm.mode }
      if (initForm.mode === 'manual') {
        body.stages = manualStages.map(s => ({ ...s, slaDays: Number(s.slaDays), sequenceNumber: Number(s.sequenceNumber) }))
      }
      return workflow.init(caseId, body)
    })
  }
  const advance  = () => wrap('Advanced',    () => workflow.advance(caseId))
  const rollback = () => wrap('Rolled back', () => workflow.rollback(caseId))

  return (
    <div>
      <PageHeader
        title={`Workflow — Case #${caseId}`}
        action={<Link to={`/cases/${caseId}`} className="btn btn-sm btn-outline-secondary">Back to case</Link>}
      />

      {stages.length === 0 ? (
        <form onSubmit={initialize}>
          <SectionCard title="Initialize workflow" className="mb-3">
            <div className="row g-3 mb-3">
              {[
                { key: 'auto',   title: 'Automatic', desc: 'Use predefined stage templates based on case type.' },
                { key: 'manual', title: 'Manual',    desc: 'Define custom stages, roles and SLA timelines.' },
              ].map(m => (
                <div key={m.key} className="col-md-6">
                  <button type="button" onClick={() => setInitForm({ ...initForm, mode: m.key })}
                    className={`cf-card p-3 w-100 text-start border-${initForm.mode === m.key ? 'dark' : ''}`}
                    style={{ background: initForm.mode === m.key ? '#f8f9fa' : '#fff' }}>
                    <div className="fw-semibold small">{m.title}</div>
                    <div className="cf-muted small">{m.desc}</div>
                  </button>
                </div>
              ))}
            </div>

            <FormField label="Case type">
              <select className="form-select form-select-sm w-auto" value={initForm.caseType}
                onChange={e => setInitForm({ ...initForm, caseType: e.target.value })}>
                {CASE_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
              </select>
            </FormField>

            {initForm.mode === 'auto' && (
              <div className="alert alert-light border small">
                <strong>Description :</strong>
                <div className="mt-1">
                  {CASE_TYPE_STAGES[initForm.caseType].map(s => (
                    <span key={s} className="badge text-bg-light border me-1 mb-1">{s}</span>
                  ))}
                </div>
              </div>
            )}

            {initForm.mode === 'manual' && (
              <div>
                <label className="form-label small fw-semibold mb-2">Define stages</label>
                {manualStages.map((s, i) => (
                  <div key={i} className="row g-2 mb-2">
                    <div className="col-2">
                      <input type="number" className="form-control form-control-sm" value={s.sequenceNumber}
                        onChange={e => setManual(m => m.map((x, j) => j === i ? { ...x, sequenceNumber: e.target.value } : x))} />
                    </div>
                    <div className="col-4">
                      <input className="form-control form-control-sm" placeholder="Stage name" value={s.stageName}
                        onChange={e => setManual(m => m.map((x, j) => j === i ? { ...x, stageName: e.target.value } : x))} required />
                    </div>
                    <div className="col-3">
                      <input className="form-control form-control-sm" placeholder="Role" value={s.roleResponsible}
                        onChange={e => setManual(m => m.map((x, j) => j === i ? { ...x, roleResponsible: e.target.value } : x))} required />
                    </div>
                    <div className="col-2">
                      <input type="number" className="form-control form-control-sm" placeholder="SLA" value={s.slaDays}
                        onChange={e => setManual(m => m.map((x, j) => j === i ? { ...x, slaDays: e.target.value } : x))} required />
                    </div>
                    <div className="col-1">
                      <button type="button" className="btn btn-sm btn-outline-danger"
                        onClick={() => setManual(m => m.filter((_, j) => j !== i))}>
                        <i className="bi bi-x" />
                      </button>
                    </div>
                  </div>
                ))}
                <button type="button" className="btn btn-sm btn-outline-secondary"
                  onClick={() => setManual(m => [...m, { sequenceNumber: m.length + 1, stageName: '', roleResponsible: '', slaDays: 7 }])}>
                  <i className="bi bi-plus me-1" />Add stage
                </button>
              </div>
            )}
          </SectionCard>
          <button type="submit" className="btn btn-dark">Initialize workflow</button>
        </form>
      ) : (
        <>
          <StageProgressCard stages={stages} className="mb-3" />
          <ActiveStageCard current={current} onAdvance={advance} onRollback={rollback} className="mb-3" />
          <StageActionsPanel caseId={caseId} onComplete={load} className="mb-3" />
          <SlaTrackingGrid records={slaRecords} className="mb-3" />
          <StagesTable stages={stages} />
        </>
      )}
    </div>
  )
}
