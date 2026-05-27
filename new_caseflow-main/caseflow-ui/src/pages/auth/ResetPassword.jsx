import { useState } from 'react'
import { Link, useSearchParams, useNavigate } from 'react-router-dom'
import { auth } from '../../api/services'
import AuthLayout from '../../components/ui/AuthLayout'
import FormField from '../../components/ui/FormField'
import AlertMessage from '../../components/ui/AlertMessage'
import { notify } from '../../components/ui/Toast'

export default function ResetPassword() {
  const [params] = useSearchParams()
  const nav = useNavigate()

  const [form, setForm] = useState({
    token: params.get('token') || '',
    newPassword: '',
    confirmPassword: '',
  })
  const [showPass, setShow]   = useState(false)
  const [error, setError]     = useState('')
  const [loading, setLoading] = useState(false)

  const set = (k) => (e) => setForm(f => ({ ...f, [k]: e.target.value }))

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    if (form.newPassword !== form.confirmPassword) {
      setError('Passwords do not match'); return
    }
    setLoading(true)
    try {
      const res = await auth.resetPassword(form)
      notify.success(typeof res === 'string' ? res : 'Password reset. Redirecting...')
      setTimeout(() => nav('/login'), 1200)
    } catch (e) { setError(e.message || 'Reset failed') }
    finally { setLoading(false) }
  }

  return (
    <AuthLayout
      title="Reset password"
      subtitle="Enter the token from your email and choose a new password"
      footer={<Link to="/login" className="cf-muted">Back to sign in</Link>}
    >
      <AlertMessage error={error} />
      <form onSubmit={submit}>
        <FormField label="Reset token" required>
          <input className="form-control" value={form.token} onChange={set('token')} required />
        </FormField>
        <FormField label="New password" required>
          <div className="input-group">
            <input className="form-control" type={showPass ? 'text' : 'password'}
              value={form.newPassword} onChange={set('newPassword')} minLength={6} required />
            <button type="button" className="btn btn-outline-secondary" onClick={() => setShow(!showPass)}>
              <i className={`bi ${showPass ? 'bi-eye-slash' : 'bi-eye'}`} />
            </button>
          </div>
        </FormField>
        <FormField label="Confirm password" required>
          <input className="form-control" type={showPass ? 'text' : 'password'}
            value={form.confirmPassword} onChange={set('confirmPassword')} minLength={6} required />
        </FormField>
        <button type="submit" className="btn btn-dark w-100" disabled={loading}>
          {loading ? <><span className="spinner-border spinner-border-sm me-2" />Resetting...</> : 'Reset password'}
        </button>
      </form>
    </AuthLayout>
  )
}
