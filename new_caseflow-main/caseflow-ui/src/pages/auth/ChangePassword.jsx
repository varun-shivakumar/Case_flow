import { useState } from 'react'
import { useAuth } from '../../context/AuthContext'
import { auth } from '../../api/services'
import PageHeader from '../../components/ui/PageHeader'
import FormField from '../../components/ui/FormField'
import AlertMessage from '../../components/ui/AlertMessage'
import { notify } from '../../components/ui/Toast'

export default function ChangePassword() {
  const { user } = useAuth()
  const [form, setForm]       = useState({ oldPassword: '', newPassword: '' })
  const [error, setError]     = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError(''); setLoading(true)
    try {
      const res = await auth.changePassword({ email: user.email, ...form })
      notify.success(typeof res === 'string' ? res : 'Password changed')
      setForm({ oldPassword: '', newPassword: '' })
    } catch (e) { setError(e.message || 'Change failed') }
    finally { setLoading(false) }
  }

  return (
    <div>
      <PageHeader title="Change password" />
      <div className="cf-card p-4" style={{ maxWidth: 500 }}>
        <AlertMessage error={error} />
        <form onSubmit={submit}>
          <FormField label="Email"><input className="form-control" value={user?.email || ''} disabled /></FormField>
          <FormField label="Current password" required>
            <input className="form-control" type="password" value={form.oldPassword}
              onChange={e => setForm({ ...form, oldPassword: e.target.value })} required />
          </FormField>
          <FormField label="New password" required>
            <input className="form-control" type="password" minLength={6} value={form.newPassword}
              onChange={e => setForm({ ...form, newPassword: e.target.value })} required />
          </FormField>
          <button className="btn btn-dark" disabled={loading}>{loading ? 'Saving...' : 'Change password'}</button>
        </form>
      </div>
    </div>
  )
}
