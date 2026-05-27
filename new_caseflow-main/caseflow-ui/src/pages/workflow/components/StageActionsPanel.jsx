import { useState } from 'react'
import { workflow } from '../../../api/services'
import { notify } from '../../../components/ui/Toast'
import SectionCard from '../../../components/ui/SectionCard'
import FormField from '../../../components/ui/FormField'

export default function StageActionsPanel({ caseId, onComplete, className = '' }) {
  const [action, setAction] = useState(null)
  const [skipReason, setSkipReason] = useState('')
  const [extForm, setExtForm] = useState({ additionalDays: 1, reason: '' })
  const [reassignForm, setReassignForm] = useState({ stageId: '', newRole: '' })

  const wrap = async (label, fn) => {
    try { await fn(); notify.success(label); onComplete?.() }
    catch (e) { notify.error(e.message) }
  }

  const toggle = (key) => setAction(prev => prev === key ? null : key)

  const skip = (e) => { e.preventDefault(); return wrap('Stage skipped', async () => {
    await workflow.skip(caseId, { reason: skipReason }); setSkipReason(''); setAction(null)
  })}
  const extend = (e) => { e.preventDefault(); return wrap('SLA extended', async () => {
    await workflow.extendSla(caseId, { additionalDays: Number(extForm.additionalDays), reason: extForm.reason })
    setExtForm({ additionalDays: 1, reason: '' }); setAction(null)
  })}
  const reassign = (e) => { e.preventDefault(); return wrap('Reassigned', async () => {
    await workflow.reassign(caseId, { stageId: Number(reassignForm.stageId), newRole: reassignForm.newRole })
    setReassignForm({ stageId: '', newRole: '' }); setAction(null)
  })}

  return (
    <SectionCard title="Stage actions" className={className}>
      <div className="btn-group btn-group-sm mb-3">
        <button className={`btn btn-${action === 'skip' ? 'dark' : 'outline-secondary'}`} onClick={() => toggle('skip')}>Skip stage</button>
        <button className={`btn btn-${action === 'extend' ? 'dark' : 'outline-secondary'}`} onClick={() => toggle('extend')}>Extend SLA</button>
        <button className={`btn btn-${action === 'reassign' ? 'dark' : 'outline-secondary'}`} onClick={() => toggle('reassign')}>Reassign</button>
      </div>

      {action === 'skip' && (
        <form onSubmit={skip}>
          <FormField label="Reason (5–1000 chars)" required>
            <textarea className="form-control form-control-sm" rows={3} minLength={5} maxLength={1000}
              value={skipReason} onChange={e => setSkipReason(e.target.value)} required />
          </FormField>
          <button className="btn btn-sm btn-dark">Skip stage</button>
        </form>
      )}

      {action === 'extend' && (
        <form onSubmit={extend}>
          <div className="row g-2">
            <div className="col-md-4">
              <FormField label="Additional days" required>
                <input type="number" className="form-control form-control-sm" min={1}
                  value={extForm.additionalDays} onChange={e => setExtForm({ ...extForm, additionalDays: e.target.value })} required />
              </FormField>
            </div>
            <div className="col-md-8">
              <FormField label="Reason" required>
                <input className="form-control form-control-sm" value={extForm.reason}
                  onChange={e => setExtForm({ ...extForm, reason: e.target.value })} required />
              </FormField>
            </div>
          </div>
          <button className="btn btn-sm btn-dark">Extend SLA</button>
        </form>
      )}

      {action === 'reassign' && (
        <form onSubmit={reassign}>
          <div className="row g-2">
            <div className="col-md-4">
              <FormField label="Stage ID" required>
                <input type="number" className="form-control form-control-sm"
                  value={reassignForm.stageId} onChange={e => setReassignForm({ ...reassignForm, stageId: e.target.value })} required />
              </FormField>
            </div>
            <div className="col-md-8">
              <FormField label="New role" required>
                <input className="form-control form-control-sm" value={reassignForm.newRole}
                  onChange={e => setReassignForm({ ...reassignForm, newRole: e.target.value })} placeholder="e.g. JUDGE" required />
              </FormField>
            </div>
          </div>
          <button className="btn btn-sm btn-dark">Reassign</button>
        </form>
      )}
    </SectionCard>
  )
}
