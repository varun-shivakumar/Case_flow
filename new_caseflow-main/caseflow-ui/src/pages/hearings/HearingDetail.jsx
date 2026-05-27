import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { hearings } from '../../api/services'
import { formatDate } from '../../utils/constants'
import { useAuth } from '../../context/AuthContext'
import PageHeader from '../../components/ui/PageHeader'
import StatusBadge from '../../components/ui/StatusBadge'
import SectionCard from '../../components/ui/SectionCard'
import FormField from '../../components/ui/FormField'
import LoadingState from '../../components/ui/LoadingState'
import { notify } from '../../components/ui/Toast'

const FIXED_SLOTS = [
  '9:00 AM - 10:00 AM',
  '10:00 AM - 11:00 AM',
  '11:00 AM - 12:00 PM',
  '2:00 PM - 3:00 PM',
  '3:00 PM - 4:00 PM',
]

export default function HearingDetail() {
  const { hearingId } = useParams()
  const { user } = useAuth()
  const [h, setH] = useState(null)
  const [resched, setResched] = useState({ newDate: '', newTime: '', rescheduleReason: '' })
  const [complete, setComplete] = useState({ hearingNotes: '' })
  const [submitting, setSubmitting] = useState(false)

  const load = async () => {
    try { setH(await hearings.get(hearingId)) } catch (e) { notify.error(e.message) }
  }
  useEffect(() => { load() }, [hearingId])

  const doReschedule = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    try {
      await hearings.reschedule(hearingId, { ...resched, clerkId: user.userId })
      notify.success('Hearing rescheduled')
      setResched({ newDate: '', newTime: '', rescheduleReason: '' })
      load()
    } catch (e) { notify.error(e.message) }
    finally { setSubmitting(false) }
  }

  const doComplete = async (e) => {
    e.preventDefault()
    setSubmitting(true)
    try {
      await hearings.complete(hearingId, { judgeId: user.userId, hearingNotes: complete.hearingNotes })
      notify.success('Hearing marked as completed')
      load()
    } catch (e) { notify.error(e.message) }
    finally { setSubmitting(false) }
  }

  if (!h) return <div><PageHeader title="Hearing" /><LoadingState /></div>

  const canClerk = ['CLERK', 'ADMIN'].includes(user?.role)
  const canJudge = ['JUDGE', 'ADMIN'].includes(user?.role)
  const isActive = h.status !== 'COMPLETED' && h.status !== 'CANCELLED'

  return (
    <div>
      <PageHeader title={`Hearing #${h.hearingId}`} action={<StatusBadge status={h.status} />} />

      <SectionCard title="Details" className="mb-3">
        <div className="row g-3 small">
          <div className="col-md-3"><div className="cf-muted">Case</div><Link to={`/cases/${h.caseId}`} className="text-dark fw-semibold">#{h.caseId}</Link></div>
          <div className="col-md-3"><div className="cf-muted">Judge</div>{h.judgeId}</div>
          <div className="col-md-3"><div className="cf-muted">Date</div>{formatDate(h.hearingDate)}</div>
          <div className="col-md-3"><div className="cf-muted">Time</div>{h.hearingTime}</div>
          <div className="col-md-3"><div className="cf-muted">Scheduled by</div>{h.scheduledBy}</div>
          <div className="col-md-3"><div className="cf-muted">Status</div><StatusBadge status={h.status} /></div>
        </div>
        {h.rescheduleReason && (
          <div className="alert alert-light border small mt-3 mb-0">
            <strong>Reschedule reason:</strong> {h.rescheduleReason}
          </div>
        )}
        {h.hearingNotes && (
          <div className="alert alert-light border small mt-3 mb-0">
            <strong>Notes:</strong> {h.hearingNotes}
          </div>
        )}
      </SectionCard>

      {canClerk && isActive && (
        <SectionCard title="Reschedule hearing" className="mb-3">
          <form onSubmit={doReschedule}>
            <div className="row g-2">
              <div className="col-md-6">
                <FormField label="New date" required>
                  <input type="date" className="form-control form-control-sm"
                    min={new Date().toISOString().split('T')[0]}
                    value={resched.newDate} onChange={e => setResched({ ...resched, newDate: e.target.value })} required />
                </FormField>
              </div>
              <div className="col-md-6">
                <FormField label="New time slot" required>
                  <select className="form-select form-select-sm" value={resched.newTime}
                    onChange={e => setResched({ ...resched, newTime: e.target.value })} required>
                    <option value="">Select slot...</option>
                    {FIXED_SLOTS.map(s => <option key={s} value={s}>{s}</option>)}
                  </select>
                </FormField>
              </div>
            </div>
            <FormField label="Reason (5–500 chars)" required>
              <textarea className="form-control form-control-sm" rows={3} minLength={5} maxLength={500}
                value={resched.rescheduleReason}
                onChange={e => setResched({ ...resched, rescheduleReason: e.target.value })} required />
            </FormField>
            <button className="btn btn-sm btn-dark" disabled={submitting}>
              {submitting ? 'Rescheduling...' : 'Reschedule'}
            </button>
          </form>
        </SectionCard>
      )}

      {canJudge && isActive && (
        <SectionCard title="Complete hearing">
          <form onSubmit={doComplete}>
            <FormField label="Hearing notes (10–2000 chars)" required>
              <textarea className="form-control form-control-sm" rows={5} minLength={10} maxLength={2000}
                value={complete.hearingNotes}
                onChange={e => setComplete({ hearingNotes: e.target.value })} required />
            </FormField>
            <button className="btn btn-sm btn-success" disabled={submitting}>
              {submitting ? 'Saving...' : 'Mark as completed'}
            </button>
          </form>
        </SectionCard>
      )}
    </div>
  )
}
