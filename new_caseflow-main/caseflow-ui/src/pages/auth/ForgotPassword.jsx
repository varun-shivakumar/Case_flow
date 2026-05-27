import { useState } from 'react'
import { Link } from 'react-router-dom'
import { auth } from '../../api/services'
import AuthLayout from '../../components/ui/AuthLayout'
import FormField from '../../components/ui/FormField'
import AlertMessage from '../../components/ui/AlertMessage'
import { notify } from '../../components/ui/Toast'

export default function ForgotPassword() {
  const [email, setEmail]     = useState('')
  const [error, setError]     = useState('')
  const [loading, setLoading] = useState(false)

  const submit = async (e) => {
    e.preventDefault()
    setError(''); setLoading(true)
    try {
      const res = await auth.forgotPassword({ email })
      notify.success(typeof res === 'string' ? res : 'Reset link sent to your email')
    } catch (e) {
      setError(e.message || 'Request failed')
    } finally { setLoading(false) }
  }

  return (
    <AuthLayout
      title="Reset password"
      subtitle="Enter the email associated with your account"
      footer={
        <div className="d-flex justify-content-between">
          <Link to="/login" className="cf-muted">Back to sign in</Link>
          <Link to="/reset-password" className="cf-muted">Have a token?</Link>
        </div>
      }
    >
      <AlertMessage error={error} />
      <form onSubmit={submit}>
        <FormField label="Email" required>
          <input className="form-control" type="email" value={email} onChange={e => setEmail(e.target.value)} required />
        </FormField>
        <button type="submit" className="btn btn-dark w-100" disabled={loading}>
          {loading ? <><span className="spinner-border spinner-border-sm me-2" />Sending...</> : 'Send reset link'}
        </button>
      </form>
    </AuthLayout>
  )
}
