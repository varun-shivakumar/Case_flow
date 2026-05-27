import { useState } from 'react'
import { Link, useNavigate, useLocation } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import AuthLayout from '../../components/ui/AuthLayout'
import FormField from '../../components/ui/FormField'
import AlertMessage from '../../components/ui/AlertMessage'
import { notify } from '../../components/ui/Toast'

export default function Login() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()

  const [form, setForm]       = useState({ email: '', password: '' })
  const [rememberMe, setRm]   = useState(false)
  const [showPass, setShow]   = useState(false)
  const [error, setError]     = useState('')
  const [loading, setLoading] = useState(false)

  const set = (k) => (e) => setForm(f => ({ ...f, [k]: e.target.value }))

  const submit = async (e) => {
    e.preventDefault()
    setError(''); setLoading(true)
    try {
      await login(form.email, form.password, rememberMe)
      notify.success('Welcome back')
      navigate(location.state?.from?.pathname || '/dashboard', { replace: true })
    } catch (err) {
      setError(err.message || 'Login failed')
    } finally { setLoading(false) }
  }

  return (
    <AuthLayout
      title="Welcome back"
      subtitle="Sign in to your CaseFlow account"
      footer={<>Don't have an account? <Link to="/register" className="text-dark fw-semibold">Create one</Link></>}
    >
      <AlertMessage error={error} />
      <form onSubmit={submit}>
        <FormField label="Email" required>
          <input className="form-control" type="email" value={form.email} onChange={set('email')} required />
        </FormField>

        <FormField label="Password" required>
          <div className="input-group">
            <input className="form-control" type={showPass ? 'text' : 'password'}
              value={form.password} onChange={set('password')} required />
            <button type="button" className="btn btn-outline-secondary" onClick={() => setShow(!showPass)}>
              <i className={`bi ${showPass ? 'bi-eye-slash' : 'bi-eye'}`} />
            </button>
          </div>
        </FormField>

        <div className="d-flex justify-content-between align-items-center mb-3">
          <div className="form-check">
            <input id="rm" className="form-check-input" type="checkbox" checked={rememberMe} onChange={e => setRm(e.target.checked)} />
            <label htmlFor="rm" className="form-check-label small">Remember me</label>
          </div>
          <Link to="/forgot-password" className="small cf-muted">Forgot password?</Link>
        </div>

        <button type="submit" className="btn btn-dark w-100" disabled={loading}>
          {loading ? <><span className="spinner-border spinner-border-sm me-2" />Signing in...</> : 'Sign In'}
        </button>
      </form>
    </AuthLayout>
  )
}
