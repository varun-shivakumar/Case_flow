import { useEffect, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { appeals, cases } from '../../api/services'
import { formatDateTime } from '../../utils/constants'
import { useAuth } from '../../context/AuthContext'
import PageHeader from '../../components/ui/PageHeader'
import FormField from '../../components/ui/FormField'
import { notify } from '../../components/ui/Toast'
import { validatePositiveInteger, validateAppealReason } from '../../utils/validators'

const FILING_DEADLINE_DAYS = 90

export default function FileAppeal() {
  const { user } = useAuth()
  const [sp] = useSearchParams()
  const nav = useNavigate()

  const [form, setForm]     = useState({ caseId: sp.get('caseId') || '', reason: '' })
  const [errors, setErrors]     = useState({})
  const [caseInfo, setCaseInfo] = useState(null)
  const [caseErr, setCaseErr]   = useState('')
  const [loading, setLoading]   = useState(false)
  const [lookingUp, setLookup]  = useState(false)
  const [activeAppeal, setActiveAppeal] = useState(null)

  // Debounced case lookup
  useEffect(() => {
    let cancelled = false
    setCaseErr(''); setCaseInfo(null); setActiveAppeal(null)
    if (!form.caseId || isNaN(Number(form.caseId))) return
    setLookup(true)
    const id = Number(form.caseId)
    const t = setTimeout(async () => {
      try {
        const [c, existing] = await Promise.all([
          cases.get(id),
          appeals.byCase(id).catch(() => []),
        ])
        if (!cancelled) {
          setCaseInfo(c)
          const active = (existing || []).find(a => a.status === 'SUBMITTED' || a.status === 'REVIEWED')
          setActiveAppeal(active || null)
        }
      } catch (e) { if (!cancelled) setCaseErr(e.message) }
      finally { if (!cancelled) setLookup(false) }
    }, 350)
    return () => { cancelled = true; clearTimeout(t) }
  }, [form.caseId])

  const set = (k) => (e) => {
    setForm(f => ({ ...f, [k]: e.target.value }))
    setErrors(prev => ({ ...prev, [k]: null }))
  }

  const blur = (field) => (e) => {
    let err = null
    if (field === 'caseId') err = validatePositiveInteger(e.target.value, 'Case ID')
    if (field === 'reason') err = validateAppealReason(e.target.value)
    setErrors(prev => ({ ...prev, [field]: err }))
  }

  const submit = async (e) => {
    e.preventDefault()
    const errs = {
      caseId: validatePositiveInteger(form.caseId, 'Case ID'),
      reason: validateAppealReason(form.reason),
    }
    setErrors(errs)
    if (Object.values(errs).some(Boolean)) return

    setLoading(true)
    try {
      const res = await appeals.file({ caseId: Number(form.caseId), reason: form.reason.trim() })
      notify.success('Appeal filed')
      nav(`/appeals/${res.appealId}`)
    } catch (e) { notify.error(e.message) }
    finally { setLoading(false) }
  }

  const checks = (() => {
    if (!caseInfo) return null
    const isClosed = caseInfo.status === 'CLOSED'
    let withinDeadline = true, daysLeft = null
    if (isClosed && caseInfo.closedDate) {
      const closed = new Date(caseInfo.closedDate)
      const deadline = new Date(closed.getTime() + FILING_DEADLINE_DAYS * 86400000)
      withinDeadline = new Date() <= deadline
      daysLeft = Math.ceil((deadline - new Date()) / 86400000)
    }
    let ownership = true
    if (user?.role === 'LITIGANT')
      ownership = caseInfo.litigantId === user.userId || caseInfo.litigantId === user.email
    else if (user?.role === 'LAWYER')
      ownership = caseInfo.lawyerId && (caseInfo.lawyerId === user.userId || caseInfo.lawyerId === user.email)
    const noActiveAppeal = !activeAppeal
    return { isClosed, withinDeadline, daysLeft, ownership, noActiveAppeal }
  })()

  const Check = ({ ok, label }) => (
    <div className={`small ${ok ? 'text-success' : 'text-danger'}`}>
      <i className={`bi ${ok ? 'bi-check-circle' : 'bi-x-circle'} me-1`} />{label}
    </div>
  )

  const ic = (f) => `form-control${errors[f] ? ' is-invalid' : ''}`

  return (
    <div>
      <PageHeader
        title="File appeal"
        subtitle={`Appeals can only be filed on CLOSED cases within ${FILING_DEADLINE_DAYS} days of closure.`}
        action={<Link to="/appeals" className="btn btn-sm btn-outline-secondary">Back</Link>}
      />

      <div className="cf-card p-4" style={{ maxWidth: 720 }}>
        <form onSubmit={submit} noValidate>

          <FormField label="Case ID" required error={errors.caseId}>
            <div className="input-group">
              <input className={ic('caseId')} type="number" min="1" value={form.caseId}
                onChange={set('caseId')} onBlur={blur('caseId')} />
              {lookingUp && <span className="input-group-text"><span className="spinner-border spinner-border-sm" /></span>}
            </div>
            {caseErr && <div className="d-flex align-items-center gap-1 mt-1" style={{ fontSize: 12, color: '#dc3545' }}>
              <i className="bi bi-exclamation-circle-fill" style={{ fontSize: 11 }} />{caseErr}
            </div>}
          </FormField>

          {caseInfo && checks && (
            <div className="cf-card p-3 mb-3" style={{ background: '#f8f9fa' }}>
              <div className="fw-semibold small mb-2">Case #{caseInfo.caseId}: {caseInfo.title}</div>
              <div className="row g-2 mb-2 small cf-muted">
                <div className="col-6 col-md-3"><div>Status</div><strong className="text-dark">{caseInfo.status}</strong></div>
                <div className="col-6 col-md-3"><div>Closed</div><strong className="text-dark">{formatDateTime(caseInfo.closedDate)}</strong></div>
                <div className="col-6 col-md-3"><div>Litigant</div><strong className="text-dark">{caseInfo.litigantId}</strong></div>
                <div className="col-6 col-md-3"><div>Lawyer</div><strong className="text-dark">{caseInfo.lawyerId || '—'}</strong></div>
              </div>
              <Check ok={checks.isClosed} label="Case is closed and eligible" />
              {checks.isClosed && (
                <Check ok={checks.withinDeadline} label={`Within ${FILING_DEADLINE_DAYS}-day window${checks.daysLeft != null && checks.withinDeadline ? ` (${checks.daysLeft} days left)` : ''}`} />
              )}
              <Check ok={checks.ownership} label="You are authorised to file" />
              <Check ok={checks.noActiveAppeal} label={checks.noActiveAppeal ? 'No existing appeal on this case' : `Appeal #${activeAppeal?.appealId} already ${activeAppeal?.status?.toLowerCase()} — only one active appeal allowed`} />
            </div>
          )}

          <FormField label="Reason for appeal" required error={errors.reason}
            hint={!errors.reason ? `${form.reason.length} / 2000 characters (min 20)` : undefined}>
            <textarea className={ic('reason')} rows={6} value={form.reason}
              onChange={set('reason')} onBlur={blur('reason')} />
          </FormField>

          <div className="d-flex gap-2">
            <button className="btn btn-dark" disabled={loading || !form.caseId || form.reason.length < 10 ||!!activeAppeal}>
              {loading ? 'Filing...' : 'File appeal'}
            </button>
            <Link to="/appeals" className="btn btn-outline-secondary">Cancel</Link>
          </div>
        </form>
      </div>
    </div>
  )
}
