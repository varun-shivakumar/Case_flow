import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { hearings, cases, users } from '../../api/services'
import { useAuth } from '../../context/AuthContext'
import PageHeader from '../../components/ui/PageHeader'
import FormField from '../../components/ui/FormField'
import { notify } from '../../components/ui/Toast'

const FIXED_SLOTS = [
  '9:00 AM - 10:00 AM',
  '10:00 AM - 11:00 AM',
  '11:00 AM - 12:00 PM',
  '2:00 PM - 3:00 PM',
  '3:00 PM - 4:00 PM',
]

export default function ScheduleHearing() {
  const { user } = useAuth()
  const nav = useNavigate()

  const [form, setForm]           = useState({ caseId: '', judgeId: '', hearingDate: '', hearingTime: '' })
  const [caseList, setCaseList]   = useState([])
  const [judgeList, setJudgeList] = useState([])
  const [booked, setBooked]       = useState([])
  const [loading, setLoading]     = useState(false)
  const [dateMax, setDateMax]     = useState('')

  useEffect(() => {
    cases.list()
      .then(d => setCaseList((d || []).filter(c => c.status === 'ACTIVE')))
      .catch(() => {})
    users.byRole('JUDGE').then(d => setJudgeList(d || [])).catch(() => {})
  }, [])

  // Recompute the hearing date window whenever the selected case changes.
  // Window: today → case filedDate + 30 days (falls back to today + 30 if that's already past).
  useEffect(() => {
    if (!form.caseId) { setDateMax(''); return }
    const selected = caseList.find(c => String(c.caseId) === String(form.caseId))
    const now = new Date()
    let max = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000)
    if (selected?.filedDate) {
      const window = new Date(selected.filedDate)
      window.setDate(window.getDate() + 30)
      if (window > now) max = window
    }
    setDateMax(max.toISOString().split('T')[0])
    // Clear hearing date if it falls outside the new window
    setForm(f => {
      if (!f.hearingDate) return f
      const d = new Date(f.hearingDate)
      if (d > max) return { ...f, hearingDate: '' }
      return f
    })
  }, [form.caseId, caseList])

  // When judge OR date changes, reload booked times for that judge+date
  useEffect(() => {
    setForm(f => ({ ...f, hearingTime: '' }))
    setBooked([])
    if (!form.judgeId || !form.hearingDate) return
    hearings.byJudge(form.judgeId).then(d => {
      setBooked((d || [])
        .filter(h => h.hearingDate === form.hearingDate && h.status !== 'CANCELLED')
        .map(h => h.hearingTime))
    }).catch(() => {})
  }, [form.judgeId, form.hearingDate])

  const availableSlots = FIXED_SLOTS.filter(s => !booked.includes(s))

  const submit = async (e) => {
    e.preventDefault()
    setLoading(true)
    try {
      const res = await hearings.schedule({
        caseId: Number(form.caseId),
        judgeId: form.judgeId,
        hearingDate: form.hearingDate,
        hearingTime: form.hearingTime,
        scheduledBy: user.userId,
      })
      notify.success('Hearing scheduled')
      nav(`/hearings/${res.hearingId}`)
    } catch (e) { notify.error(e.message) }
    finally { setLoading(false) }
  }

  const today = new Date().toISOString().split('T')[0]

  return (
    <div>
      <PageHeader title="Schedule hearing" subtitle="Select a case, judge and time slot" />

      <div className="cf-card p-4" style={{ maxWidth: 720 }}>
        <form onSubmit={submit}>
          <FormField label="Case" required hint="Only active cases are eligible for hearing scheduling.">
            <select className="form-select" value={form.caseId}
              onChange={e => setForm({ ...form, caseId: e.target.value, hearingDate: '', hearingTime: '' })} required>
              <option value="">Select an active case...</option>
              {caseList.length === 0
                ? <option disabled>No active cases available</option>
                : caseList.map(c => (
                    <option key={c.caseId} value={c.caseId}>
                      #{c.caseId} — {c.title}
                    </option>
                  ))
              }
            </select>
          </FormField>

          <FormField label="Judge" required>
            <select className="form-select" value={form.judgeId}
              onChange={e => setForm({ ...form, judgeId: e.target.value })} required>
              <option value="">Select a judge...</option>
              {judgeList.map(j => <option key={j.userId} value={j.userId}>{j.name} — {j.userId}</option>)}
            </select>
          </FormField>

          <div className="row g-2">
            <div className="col-md-6">
              <FormField label="Hearing date" required
                hint={dateMax ? `Must be scheduled within 30 days of case filing (by ${dateMax})` : 'Select a case first'}>
                <input type="date" className="form-control" min={today} max={dateMax || undefined}
                  value={form.hearingDate} onChange={e => setForm({ ...form, hearingDate: e.target.value })}
                  disabled={!form.caseId} required />
              </FormField>
            </div>
            <div className="col-md-6">
              <FormField label="Time slot" required
                hint={form.judgeId && form.hearingDate
                  ? `${availableSlots.length} of ${FIXED_SLOTS.length} slots available`
                  : 'Select judge & date to see availability'}>
                <select className="form-select" value={form.hearingTime}
                  onChange={e => setForm({ ...form, hearingTime: e.target.value })}
                  disabled={!form.judgeId || !form.hearingDate} required>
                  <option value="">Select slot...</option>
                  {FIXED_SLOTS.map(s => (
                    <option key={s} value={s} disabled={booked.includes(s)}>
                      {s} {booked.includes(s) ? '(booked)' : ''}
                    </option>
                  ))}
                </select>
              </FormField>
            </div>
          </div>

          <button className="btn btn-dark" disabled={loading}>
            {loading ? <><span className="spinner-border spinner-border-sm me-2" />Scheduling...</> : 'Schedule hearing'}
          </button>
        </form>
      </div>
    </div>
  )
}
