import { useEffect, useState } from 'react'
import { compliance } from '../../api/services'
import { formatDate } from '../../utils/constants'
import { useAuth } from '../../context/AuthContext'
import PageHeader from '../../components/ui/PageHeader'
import StatusBadge from '../../components/ui/StatusBadge'
import SectionCard from '../../components/ui/SectionCard'
import EmptyState from '../../components/ui/EmptyState'
import LoadingState from '../../components/ui/LoadingState'
import DataTable from '../../components/ui/DataTable'
import FormField from '../../components/ui/FormField'
import { notify } from '../../components/ui/Toast'

export default function AuditList() {
  const { user } = useAuth()
  const isAdmin = user?.role === 'ADMIN'

  const [audits, setAudits]   = useState([])
  const [loading, setLoading] = useState(true)
  const [form, setForm]       = useState({ scope: '', findings: '' })
  const [creating, setCreating] = useState(false)
  const [editFindings, setEdit] = useState({})
  const [selected, setSelected] = useState(new Set())
  const [busy, setBusy] = useState(false)

  const load = async () => {
    setLoading(true)
    try { const p = await compliance.auditsPaginated(0, 50); setAudits(p?.content || []) }
    catch (e) { notify.error(e.message) }
    finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  const toggle = (id) => setSelected(s => { const n = new Set(s); n.has(id) ? n.delete(id) : n.add(id); return n })

  const create = async (e) => {
    e.preventDefault(); setCreating(true)
    try {
      await compliance.createAudit({ scope: form.scope, findings: form.findings || undefined })
      notify.success('Audit created'); setForm({ scope: '', findings: '' }); load()
    } catch (e) { notify.error(e.message) }
    finally { setCreating(false) }
  }

  const saveFindings = async (id) => {
    try {
      await compliance.updateFindings(id, editFindings[id])
      notify.success('Findings updated')
      setEdit(prev => { const n = { ...prev }; delete n[id]; return n })
      load()
    } catch (e) { notify.error(e.message) }
  }
  const close = async (id) => {
    if (!confirm(`Close audit #${id}?`)) return
    try { await compliance.closeAudit(id); notify.success('Closed'); load() }
    catch (e) { notify.error(e.message) }
  }
  const del = async (id) => {
    if (!confirm(`Delete audit #${id}?`)) return
    try { await compliance.deleteAudit(id); notify.success('Deleted'); setAudits(a => a.filter(x => x.auditId !== id)) }
    catch (e) { notify.error(e.message) }
  }
  const bulkDelete = async () => {
    if (!selected.size) return
    if (!confirm(`Delete ${selected.size} audits?`)) return
    setBusy(true)
    try { await compliance.bulkDeleteAudits([...selected]); notify.success('Deleted'); setSelected(new Set()); load() }
    catch (e) { notify.error(e.message) }
    finally { setBusy(false) }
  }

  return (
    <div>
      <PageHeader
        title="Audits"
        subtitle={`${audits.length} record${audits.length !== 1 ? 's' : ''}`}
        action={isAdmin && selected.size > 0 && (
          <button className="btn btn-sm btn-outline-danger" onClick={bulkDelete} disabled={busy}>Delete ({selected.size})</button>
        )}
      />

      {isAdmin && (
        <SectionCard title="Create audit" className="mb-3">
          <form onSubmit={create}>
            <div className="row g-2">
              <div className="col-md-4">
                <FormField label="Scope" required>
                  <input className="form-control form-control-sm" value={form.scope}
                    onChange={e => setForm({ ...form, scope: e.target.value })} required />
                </FormField>
              </div>
              <div className="col-md-8">
                <FormField label="Findings">
                  <input className="form-control form-control-sm" value={form.findings}
                    onChange={e => setForm({ ...form, findings: e.target.value })} />
                </FormField>
              </div>
            </div>
            <button className="btn btn-sm btn-dark" disabled={creating || !form.scope.trim()}>
              {creating ? 'Creating...' : 'Create audit'}
            </button>
          </form>
        </SectionCard>
      )}

      <div className="cf-card">
        {loading ? <LoadingState />
          : audits.length === 0 ? <EmptyState icon="bi-clipboard-data" title="No audits yet" />
          : <DataTable columns={['', 'ID', 'Scope', 'Findings', 'Status', 'Opened', 'Closed', 'Actions']}>
              {audits.map(a => (
                <tr key={a.auditId}>
                  <td>{isAdmin && <input type="checkbox" className="form-check-input" checked={selected.has(a.auditId)} onChange={() => toggle(a.auditId)} />}</td>
                  <td className="fw-semibold">#{a.auditId}</td>
                  <td>{a.scope}</td>
                  <td style={{ minWidth: 220 }}>
                    {editFindings[a.auditId] != null ? (
                      <div className="d-flex gap-1">
                        <input className="form-control form-control-sm" value={editFindings[a.auditId]}
                          onChange={e => setEdit({ ...editFindings, [a.auditId]: e.target.value })} />
                        <button className="btn btn-sm btn-success" onClick={() => saveFindings(a.auditId)}>Save</button>
                      </div>
                    ) : (
                      <span className="cf-muted">{a.findings || '—'}</span>
                    )}
                  </td>
                  <td><StatusBadge status={a.status} /></td>
                  <td className="cf-muted">{formatDate(a.openedAt)}</td>
                  <td className="cf-muted">{formatDate(a.closedAt)}</td>
                  <td>
                    <div className="d-flex gap-1 flex-wrap">
                      {a.status === 'OPEN' && isAdmin && (
                        <button className="btn btn-sm btn-outline-secondary" onClick={() => setEdit({ ...editFindings, [a.auditId]: a.findings || '' })}>Edit</button>
                      )}
                      {a.status === 'OPEN' && isAdmin && <button className="btn btn-sm btn-success" onClick={() => close(a.auditId)}>Close</button>}
                      {isAdmin && <button className="btn btn-sm btn-outline-danger" onClick={() => del(a.auditId)}>Delete</button>}
                    </div>
                  </td>
                </tr>
              ))}
            </DataTable>}
      </div>
    </div>
  )
}
