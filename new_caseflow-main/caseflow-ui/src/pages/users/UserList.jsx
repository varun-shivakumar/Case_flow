import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { users } from '../../api/services'
import { ROLES, USER_STATUS } from '../../utils/constants'
import PageHeader from '../../components/ui/PageHeader'
import StatusBadge from '../../components/ui/StatusBadge'
import SectionCard from '../../components/ui/SectionCard'
import EmptyState from '../../components/ui/EmptyState'
import LoadingState from '../../components/ui/LoadingState'
import DataTable from '../../components/ui/DataTable'
import FormField from '../../components/ui/FormField'
import { notify } from '../../components/ui/Toast'
import { validateName, validateEmail, validatePhone, required } from '../../utils/validators'

const EMPTY_FORM = { name: '', email: '', phone: '', password: '', role: 'CLERK' }

export default function UserList() {
  const [list, setList]         = useState([])
  const [filteredList, setFilteredList] = useState([])
  const [loading, setLoading]   = useState(true)
  const [showPass, setShowPass] = useState(false)
  const [submitting, setSubmitting] = useState(false)
  const [form, setForm]         = useState(EMPTY_FORM)
  const [errors, setErrors]     = useState({})
  const [searchTerm, setSearchTerm] = useState('')

  const load = async () => {
    setLoading(true)
    try { setList(await users.list() || []) }
    catch (e) { notify.error(e.message) }
    finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  const generatePassword = () => {
    const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789'
    let p = ''; for (let i = 0; i < 10; i++) p += chars[Math.floor(Math.random() * chars.length)]
    setForm(f => ({ ...f, password: p }))
    setErrors(prev => ({ ...prev, password: null }))
    setShowPass(true)
  }

  const set = (k) => (e) => {
    setForm(f => ({ ...f, [k]: e.target.value }))
    setErrors(prev => ({ ...prev, [k]: null }))
  }

  const blur = (field) => (e) => {
    const val = e.target.value
    let err = null
    if (field === 'name')     err = validateName(val)
    if (field === 'email')    err = validateEmail(val)
    if (field === 'phone')    err = validatePhone(val, true)
    if (field === 'password') err = required(val, 'Password')
    setErrors(prev => ({ ...prev, [field]: err }))
  }

  const create = async (e) => {
    e.preventDefault()
    if (submitting) return
    const errs = {
      name:     validateName(form.name),
      email:    validateEmail(form.email),
      phone:    validatePhone(form.phone, true),
      password: required(form.password, 'Password'),
    }
    setErrors(errs)
    if (Object.values(errs).some(Boolean)) return

    setSubmitting(true)
    try {
      await users.create(form)
      notify.success('User created')
      setForm(EMPTY_FORM); setErrors({}); setShowPass(false); load()
    } catch (e) { notify.error(e.message) }
    finally { setSubmitting(false) }
  }

  const setStatus = async (id, status) => {
    try { await users.setStatus(id, status); notify.success('Status updated'); load() }
    catch (e) { notify.error(e.message) }
  }
  const resetPwd = async (id) => {
    const p = prompt('New password (min 6 chars):')
    if (!p || p.length < 6) return
    try { await users.resetPassword(id, p); notify.success('Password reset') }
    catch (e) { notify.error(e.message) }
  }
  const del = async (id) => {
    if (!confirm('Delete user?')) return
    try { await users.del(id); notify.success('User deleted'); load() }
    catch (e) { notify.error(e.message) }
  }

  const ic = (f, size = '') => `form-control${size ? ` form-control-${size}` : ''}${errors[f] ? ' is-invalid' : ''}`

  useEffect(() => {
    setFilteredList(list)
  }, [list])

  const handleSearch = (e) => {
    const value = e.target.value
    setSearchTerm(value)
    if (!value) return setFilteredList(list)
    const lowercasedValue = value.toLowerCase()
    setFilteredList(
      list.filter(item => Object.keys(item).some(key =>
        String(item[key]).toLowerCase().includes(lowercasedValue)
      ))
    )
  }

  return (
    <div>
      <PageHeader
        title="Users"
        subtitle={`${filteredList.length} of ${list.length} user${list.length !== 1 ? 's' : ''}${searchTerm ? ' (filtered)' : ''}`}
      />

      <SectionCard title="Create user" className="mb-3">
        <form onSubmit={create} noValidate>
          <div className="row g-2">
            <div className="col-md-6">
              <FormField label="Name" required>
                <input className="form-control form-control-sm" value={form.name}
                  onChange={e => setForm({ ...form, name: e.target.value })} minLength={2} maxLength={100} required />
              </FormField>
            </div>
            <div className="col-md-6">
              <FormField label="Email" required>
                <input className="form-control form-control-sm" type="email" value={form.email}
                  onChange={e => setForm({ ...form, email: e.target.value })} required />
              </FormField>
            </div>
            <div className="col-md-6">
              <FormField label="Phone" required>
                <input className="form-control form-control-sm" value={form.phone}
                  onChange={e => setForm({ ...form, phone: e.target.value })} pattern="\d{10}" maxLength={10} required />
              </FormField>
            </div>
            <div className="col-md-6">
              <FormField label="Password" required error={errors.password}>
                <div className="input-group input-group-sm">
                  <input className="form-control" type={showPass ? 'text' : 'password'} minLength={6}
                    value={form.password} onChange={e => setForm({ ...form, password: e.target.value })} required />
                  <button type="button" className="btn btn-outline-secondary" onClick={() => setShowPass(v => !v)}>
                    <i className={`bi ${showPass ? 'bi-eye-slash' : 'bi-eye'}`} />
                  </button>
                  <button type="button" className="btn btn-outline-dark" onClick={generatePassword}>Generate</button>
                </div>
              </FormField>
            </div>
            <div className="col-md-6">
              <FormField label="Role">
                <select className="form-select form-select-sm" value={form.role}
                  onChange={e => setForm({ ...form, role: e.target.value })}>
                  {ROLES.filter(r => r !== 'LITIGANT').map(r => <option key={r} value={r}>{r}</option>)}
                </select>
              </FormField>
            </div>
          </div>
          <button className="btn btn-dark btn-sm" disabled={submitting}>Create user</button>
        </form>
      </SectionCard>

      <div className="cf-card">
        {loading ? <LoadingState />
          : list.length === 0 ? <EmptyState icon="bi-people" title="No users yet" />
          : <>
              <div className="mb-3 d-flex justify-content-end">
                <div className="input-group input-group-sm" style={{ maxWidth: '300px' }}>
                  <span className="input-group-text">
                    <i className="bi bi-search"></i>
                  </span>
                  <input type="text" className="form-control"
                    value={searchTerm} onChange={handleSearch} placeholder="Search users..." />
                </div>
              </div>
              <DataTable columns={['ID', 'Name', 'Email', 'Phone', 'Role', 'Status', 'Actions']}>
                {filteredList.map(u => (
                  <tr key={u.userId}>
                    <td className="cf-muted">{u.userId}</td>
                    <td className="fw-semibold">{u.name}</td>
                    <td>{u.email}</td>
                    <td className="cf-muted">{u.phone}</td>
                    <td><span className="badge text-bg-light border">{u.role}</span></td>
                    <td><StatusBadge status={u.status} /></td>
                    <td>
                      <div className="d-flex gap-1 flex-wrap align-items-center">
                        <select className="form-select form-select-sm w-auto" value={u.status}
                          onChange={e => setStatus(u.userId, e.target.value)}>
                          {USER_STATUS.map(s => <option key={s} value={s}>{s}</option>)}
                        </select>
                        <button className="btn btn-sm btn-outline-secondary" onClick={() => resetPwd(u.userId)}>Reset pwd</button>
                        <Link to={`/users/audit-logs/${u.userId}`} className="btn btn-sm btn-outline-secondary">Logs</Link>
                        <button className="btn btn-sm btn-outline-danger" onClick={() => del(u.userId)}>Delete</button>
                      </div>
                    </td>
                  </tr>
                ))}
              </DataTable>
            </>}
      </div>
    </div>
  )
}
