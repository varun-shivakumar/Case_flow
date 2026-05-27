import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { cases, users, auth as authApi } from '../../api/services'
import { useAuth } from '../../context/AuthContext'
import { UPLOAD_ALLOWED_EXTENSIONS, UPLOAD_ACCEPT_ATTR, validateUpload, formatBytes } from '../../utils/constants'
import PageHeader from '../../components/ui/PageHeader'
import FormField from '../../components/ui/FormField'
import AlertMessage from '../../components/ui/AlertMessage'
import { notify } from '../../components/ui/Toast'

export default function FileCase() {
  const { user, setUser } = useAuth()
  const nav = useNavigate()
  const isLitigant = user?.role === 'LITIGANT'

  const [form, setForm]       = useState({ title: '', litigantId: '', lawyerId: '' })
  const [docs, setDocs]       = useState([])
  const [lawyers, setLawyers] = useState([])
  const [idLoading, setIdLoading] = useState(false)
  const [idErr, setIdErr]     = useState('')
  const [error, setError]     = useState('')
  const [loading, setLoading] = useState(false)

  // Resolve numeric userId from /me when the session only has an email.
  useEffect(() => {
    let active = true
    async function resolveUserId() {
      if (user?.userId && !user.userId.includes('@')) {
        if (isLitigant) setForm(f => ({ ...f, litigantId: user.userId }))
        return
      }
      setIdLoading(true)
      try {
        const me = await authApi.me()
        if (!active) return
        if (isLitigant) setForm(f => ({ ...f, litigantId: me.userId }))
        if (user && setUser) setUser({ ...user, userId: me.userId })
      } catch (e) {
        if (active) setIdErr('Could not resolve your user ID. Please sign out and back in.')
      } finally { if (active) setIdLoading(false) }
    }
    resolveUserId()
    return () => { active = false }
  }, [])

  useEffect(() => {
    let active = true
    users.byRole('LAWYER').then(d => { if (active) setLawyers(d || []) }).catch(() => {})
    return () => { active = false }
  }, [])

  const withLawyer = !!form.lawyerId

  const handleFiles = (e) => {
    const files = Array.from(e.target.files || [])
    for (const f of files) {
      const v = validateUpload(f)
      if (v) { notify.error(`${f.name}: ${v}`); setDocs([]); e.target.value = ''; return }
    }
    setDocs(files)
  }

  const submit = async (e) => {
    e.preventDefault()
    setError('')
    setLoading(true)
    try {
      const res = await cases.file({ title: form.title, litigantId: form.litigantId, lawyerId: form.lawyerId || null })
      if (!withLawyer) {
        for (const file of docs) {
          const fd = new FormData()
          fd.append('file', file); fd.append('caseId', res.caseId)
          fd.append('uploadedBy', form.litigantId); fd.append('title', file.name); fd.append('type', 'PETITION')
          await cases.uploadDoc(fd)
        }
      }
      notify.success('Case filed successfully')
      nav(`/cases/${res.caseId}`)
    } catch (e) {
      const m = e?.message || 'Could not file the case.'
      setError(m); notify.error(m)
    } finally { setLoading(false) }
  }

  return (
    <div>
      <PageHeader title="File new case" />
      <div className="cf-card p-4" style={{ maxWidth: 680 }}>
        <AlertMessage error={error} />
        <form onSubmit={submit}>
          <FormField label="Case title" required>
            <input className="form-control" value={form.title}
              onChange={e => setForm({ ...form, title: e.target.value })}
              minLength={3} maxLength={255} required />
          </FormField>

          <FormField label="Litigant ID" required hint={idErr || (isLitigant ? 'Auto-filled from your account' : null)}>
            <input className="form-control" value={idLoading ? '' : form.litigantId}
              onChange={e => !isLitigant && setForm({ ...form, litigantId: e.target.value })}
              readOnly={isLitigant} required
              placeholder={idLoading ? 'Resolving your user ID…' : 'Enter litigant user ID'} />
          </FormField>

          <FormField label="Lawyer ID" hint="Leave blank to file without a lawyer.">
            <input className="form-control" value={form.lawyerId}
              onChange={e => setForm({ ...form, lawyerId: e.target.value.trim() })}
              list="lawyer-suggestions" autoComplete="off" />
            <datalist id="lawyer-suggestions">
              {lawyers.map(l => <option key={l.userId} value={l.userId}>{l.name} — {l.userId}</option>)}
            </datalist>
          </FormField>

          <div className="alert alert-light border small mb-3">
            <i className="bi bi-info-circle me-1" />
            {withLawyer
              ? 'Your assigned lawyer will handle document uploads. Both of you will be notified on status changes.'
              : 'Filing without a lawyer — you will upload documents and receive all notifications.'}
          </div>

          {!withLawyer && (
            <FormField label="Documents"
              hint={`Max 10 MB per file. Allowed: ${UPLOAD_ALLOWED_EXTENSIONS.join(', ')}`}>
              <input className="form-control" type="file" multiple accept={UPLOAD_ACCEPT_ATTR} onChange={handleFiles} />
              {docs.length > 0 && (
                <ul className="list-unstyled small mt-2 mb-0">
                  {docs.map((f, i) => (
                    <li key={i} className="cf-muted">
                      <i className="bi bi-file-earmark me-1" />{f.name} ({formatBytes(f.size)})
                    </li>
                  ))}
                </ul>
              )}
            </FormField>
          )}

          <button className="btn btn-dark" disabled={loading || idLoading}>
            {loading ? <><span className="spinner-border spinner-border-sm me-2" />Filing…</> : 'File case'}
          </button>
        </form>
      </div>
    </div>
  )
}
