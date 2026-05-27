import { useState } from 'react'
import { Link } from 'react-router-dom'
import { notifications as notifApi } from '../../api/services'
import { NOTIF_CATEGORY } from '../../utils/constants'
import { useAuth } from '../../context/AuthContext'
import PageHeader from '../../components/ui/PageHeader'
import FormField from '../../components/ui/FormField'
import { notify } from '../../components/ui/Toast'

export default function CreateNotification() {
  const { user } = useAuth()
  const isPrivileged = user?.role === 'ADMIN' || user?.role === 'CLERK'

  const [form, setForm] = useState({ userId: '', caseId: '', message: '', category: 'CASE' })
  const [busy, setBusy] = useState(false)

  const submit = async (e) => {
    e.preventDefault(); setBusy(true)
    try {
      await notifApi.create({
        userId:   form.userId.trim(),
        caseId:   form.caseId ? Number(form.caseId) : null,
        message:  form.message,
        category: form.category,
      })
      notify.success('Notification sent')
      setForm({ userId: '', caseId: '', message: '', category: 'CASE' })
    } catch (e) { notify.error(e.message || 'Failed to send notification') }
    finally { setBusy(false) }
  }

  if (!isPrivileged) return (
    <div>
      <PageHeader title="Send notification" />
      <div className="alert alert-warning small">You don't have permission to send notifications.</div>
      <Link to="/notifications" className="btn btn-sm btn-outline-secondary">Back</Link>
    </div>
  )

  return (
    <div>
      <PageHeader
        title="Send notification"
        subtitle="Compose a notification to a specific user"
        action={<Link to="/notifications" className="btn btn-sm btn-outline-secondary">Back</Link>}
      />

      <div className="cf-card p-4" style={{ maxWidth: 720 }}>
        <form onSubmit={submit}>
          <div className="row g-2">
            <div className="col-md-6">
              <FormField label="Recipient user ID" required>
                <input className="form-control form-control-sm" value={form.userId}
                  onChange={e => setForm({ ...form, userId: e.target.value })} required disabled={busy} />
              </FormField>
            </div>
            <div className="col-md-3">
              <FormField label="Case ID">
                <input className="form-control form-control-sm" type="number" value={form.caseId}
                  onChange={e => setForm({ ...form, caseId: e.target.value })} disabled={busy} />
              </FormField>
            </div>
            <div className="col-md-3">
              <FormField label="Category">
                <select className="form-select form-select-sm" value={form.category}
                  onChange={e => setForm({ ...form, category: e.target.value })} disabled={busy}>
                  {(NOTIF_CATEGORY || ['CASE', 'HEARING', 'APPEAL', 'COMPLIANCE']).map(c => (
                    <option key={c} value={c}>{c}</option>
                  ))}
                </select>
              </FormField>
            </div>
          </div>
          <FormField label="Message" required>
            <textarea className="form-control form-control-sm" rows={4} value={form.message}
              onChange={e => setForm({ ...form, message: e.target.value })} required disabled={busy} />
          </FormField>
          <div className="d-flex gap-2">
            <button className="btn btn-dark btn-sm" disabled={busy}>
              {busy ? 'Sending...' : 'Send notification'}
            </button>
            <Link to="/notifications" className="btn btn-sm btn-outline-secondary">Cancel</Link>
          </div>
        </form>
      </div>
    </div>
  )
}
