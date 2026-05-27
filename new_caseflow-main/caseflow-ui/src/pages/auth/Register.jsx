import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../../context/AuthContext'
import AuthLayout from '../../components/ui/AuthLayout'
import FormField from '../../components/ui/FormField'
import AlertMessage from '../../components/ui/AlertMessage'
import { notify } from '../../components/ui/Toast'
import {
  validateName,
  validateEmail,
  validatePhone,
  validatePassword,
  validatePasswordConfirm,
} from '../../utils/validators'

const INITIAL = { name: '', email: '', phone: '', password: '', confirmPassword: '', role: 'LITIGANT' }

export default function Register() {
  const { register } = useAuth()
  const navigate = useNavigate()

  const [form, setForm]         = useState(INITIAL)
  const [showPass, setShow]     = useState(false)
  const [showConfirm, setShowC] = useState(false)
  const [fieldErrors, setFErrs] = useState({})
  const [error, setError]       = useState('')
  const [loading, setLoading]   = useState(false)

  const set = (k) => (e) => {
    const val = e.target.value
    setForm(f => ({ ...f, [k]: val }))
    // clear field error on change
    if (fieldErrors[k]) setFErrs(fe => ({ ...fe, [k]: null }))
  }

  // Phone: digits only, first digit must be 6–9
  const setPhone = (e) => {
    let digits = e.target.value.replace(/\D/g, '')
    if (digits.length > 0 && !/^[6-9]/.test(digits)) digits = digits.slice(1)
    digits = digits.slice(0, 10)
    setForm(f => ({ ...f, phone: digits }))
    if (fieldErrors.phone) setFErrs(fe => ({ ...fe, phone: null }))
  }

  const validate = () => {
    const errs = {}
    const nameErr = validateName(form.name, 'Full name')
    if (nameErr) errs.name = nameErr
    const emailErr = validateEmail(form.email)
    if (emailErr) errs.email = emailErr
    const phoneErr = validatePhone(form.phone, true)
    if (phoneErr) errs.phone = phoneErr
    const passErr = validatePassword(form.password)
    if (passErr) errs.password = passErr
    const confirmErr = validatePasswordConfirm(form.password, form.confirmPassword)
    if (confirmErr) errs.confirmPassword = confirmErr
    return errs
  }

  const submit = async (e) => {
    e.preventDefault()
    setError('')

    const errs = validate()
    if (Object.keys(errs).length) {
      setFErrs(errs)
      return
    }

    setLoading(true)
    try {
      const { confirmPassword: _, ...payload } = form
      await register(payload)
      notify.success('Account created. Redirecting to sign in...')
      setTimeout(() => navigate('/login'), 1200)
    } catch (err) {
      setError(err.message || 'Registration failed')
    } finally { setLoading(false) }
  }

  const fe = (k) => fieldErrors[k]
    ? <div className="text-danger small mt-1">{fieldErrors[k]}</div>
    : null

  return (
    <AuthLayout
      title="Create account"
      subtitle="Register as a litigant on CaseFlow"
      footer={<>Already have an account? <Link to="/login" className="text-dark fw-semibold">Sign in</Link></>}
    >
      <AlertMessage error={error} />
      <form onSubmit={submit} noValidate>

        <FormField label="Full name" required>
          <input
            className={`form-control${fe('name') ? ' is-invalid' : ''}`}
            value={form.name}
            onChange={set('name')}
            autoComplete="name"
          />
          {fe('name')}
        </FormField>

        <FormField label="Email" required>
          <input
            className={`form-control${fe('email') ? ' is-invalid' : ''}`}
            type="email"
            value={form.email}
            onChange={set('email')}
            autoComplete="email"
          />
          {fe('email')}
        </FormField>

        <FormField label="Phone (10 digits, numbers only)" required>
          <input
            className={`form-control${fe('phone') ? ' is-invalid' : ''}`}
            inputMode="numeric"
            value={form.phone}
            onChange={setPhone}
            maxLength={10}
            placeholder="10-digit number"
          />
          {fe('phone')}
        </FormField>

        <FormField label="Password" required>
          <div className="input-group">
            <input
              className={`form-control${fe('password') ? ' is-invalid' : ''}`}
              type={showPass ? 'text' : 'password'}
              value={form.password}
              onChange={set('password')}
              autoComplete="new-password"
            />
            <button type="button" className="btn btn-outline-secondary" onClick={() => setShow(s => !s)}>
              <i className={`bi ${showPass ? 'bi-eye-slash' : 'bi-eye'}`} />
            </button>
          </div>
          <div className="form-text text-muted">Min 8 chars · uppercase · lowercase · number</div>
          {fe('password')}
        </FormField>

        <FormField label="Confirm password" required>
          <div className="input-group">
            <input
              className={`form-control${fe('confirmPassword') ? ' is-invalid' : ''}`}
              type={showConfirm ? 'text' : 'password'}
              value={form.confirmPassword}
              onChange={set('confirmPassword')}
              autoComplete="new-password"
            />
            <button type="button" className="btn btn-outline-secondary" onClick={() => setShowC(s => !s)}>
              <i className={`bi ${showConfirm ? 'bi-eye-slash' : 'bi-eye'}`} />
            </button>
          </div>
          {fe('confirmPassword')}
        </FormField>

        <button type="submit" className="btn btn-dark w-100" disabled={loading}>
          {loading
            ? <><span className="spinner-border spinner-border-sm me-2" />Creating...</>
            : 'Create account'}
        </button>
      </form>
    </AuthLayout>
  )
}
