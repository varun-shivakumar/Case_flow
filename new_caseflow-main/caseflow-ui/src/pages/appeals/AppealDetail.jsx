import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { appeals, users, hearings as hearingsApi } from '../../api/services'
import {
  REVIEW_OUTCOME, REVIEW_OUTCOME_LABELS,
  APPEAL_DOC_TYPES, APPEAL_AUDIT_ACTIONS,
  formatDateTime,
  UPLOAD_ALLOWED_EXTENSIONS, UPLOAD_ACCEPT_ATTR, validateUpload, formatBytes,
} from '../../utils/constants'
import { useAuth } from '../../context/AuthContext'
import PageHeader from '../../components/ui/PageHeader'
import StatusBadge from '../../components/ui/StatusBadge'
import SectionCard from '../../components/ui/SectionCard'
import EmptyState from '../../components/ui/EmptyState'
import LoadingState from '../../components/ui/LoadingState'
import FormField from '../../components/ui/FormField'
import { notify } from '../../components/ui/Toast'

export default function AppealDetail() {
  const { appealId } = useParams()
  const { user } = useAuth()
  const nav = useNavigate()

  const [a, setA]              = useState(null)
  const [review, setReview]    = useState(null)
  const [docs, setDocs]        = useState([])
  const [audit, setAudit]      = useState([])
  const [showAudit, setShowAudit] = useState(false)
  const [busy, setBusy]        = useState(false)

  const [judgeId, setJudgeId]   = useState('')
  const [judges, setJudges]     = useState([])
  const [excluded, setExcluded] = useState(new Set())
  const [decision, setDecision] = useState({ outcome: 'APPEAL_UPHELD', remarks: '' })
  const [draftOutcome, setDraftOutcome] = useState('')
  const [uploadForm, setUploadForm] = useState({ title: '', type: 'PETITION', file: null })

  const load = async () => {
    try {
      const appeal = await appeals.get(appealId)
      setA(appeal)
      const [r, d] = await Promise.allSettled([appeals.getReview(appealId), appeals.listDocs(appealId)])
      setReview(r.status === 'fulfilled' ? r.value : null)
      setDocs(d.status === 'fulfilled' ? (d.value || []) : [])
    } catch (e) { notify.error(e.message) }
  }
  useEffect(() => { load() }, [appealId])

  useEffect(() => {
    users.byRole('JUDGE').then(d => setJudges(Array.isArray(d) ? d : [])).catch(() => setJudges([]))
  }, [])

  // Judges who presided over hearings on this case are excluded from review assignment
  useEffect(() => {
    if (!a?.caseId) return
    let active = true
    hearingsApi.byCase(a.caseId).then(list => {
      if (!active) return
      const ids = new Set()
      ;(list || []).forEach(h => h?.judgeId && ids.add(String(h.judgeId)))
      setExcluded(ids)
    }).catch(() => { if (active) setExcluded(new Set()) })
    return () => { active = false }
  }, [a?.caseId])

  useEffect(() => {
    if (!showAudit) return
    appeals.audit(appealId).then(r => setAudit(r || [])).catch(e => notify.error(e.message))
  }, [showAudit, appealId])

  const eligibleJudges = judges.filter(j => !excluded.has(String(j.userId)))

  const wrap = async (label, fn) => {
    setBusy(true)
    try { await fn(); notify.success(label); load() }
    catch (e) { notify.error(e.message) }
    finally { setBusy(false) }
  }

  const openReview = () => wrap('Review opened', () => appeals.openReview(appealId, judgeId.trim()))
  const cancelAppeal = () => {
    if (!confirm(`Cancel appeal #${appealId}?`)) return
    return wrap('Appeal cancelled', () => appeals.cancel(appealId))
  }
  const issueDecision = (e) => { e.preventDefault(); return wrap('Decision issued', () => appeals.decide(appealId, decision)) }
  const updateDraft = () => wrap('Draft updated', () => appeals.updateOutcome(review.reviewId, { outcome: draftOutcome }))

  const uploadDoc = async (e) => {
    e.preventDefault()
    if (!uploadForm.file) { notify.error('Select a file to upload'); return }
    const v = validateUpload(uploadForm.file); if (v) { notify.error(v); return }
    setBusy(true)
    try {
      const fd = new FormData()
      fd.append('title', uploadForm.title.trim()); fd.append('type', uploadForm.type); fd.append('file', uploadForm.file)
      await appeals.uploadDoc(appealId, fd)
      notify.success('Document uploaded')
      setUploadForm({ title: '', type: 'PETITION', file: null })
      load()
    } catch (e) { notify.error(e.message) }
    finally { setBusy(false) }
  }
  const deleteDoc = (id) => {
    if (!confirm(`Delete document #${id}?`)) return
    return wrap('Document deleted', () => appeals.deleteDoc(id))
  }
  const downloadDoc = async (id, filename) => {
    try {
      const res = await appeals.downloadDoc(id)
      if (!res.ok) throw new Error('Download failed')
      const blob = await res.blob()
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url; link.download = filename || `appeal-document-${id}`; link.click()
      URL.revokeObjectURL(url)
    } catch (e) { notify.error(e.message) }
  }

  if (!a) return <div><PageHeader title="Appeal" /><LoadingState /></div>

  // Permissions
  const isAdmin = user?.role === 'ADMIN'
  const isClerk = user?.role === 'CLERK'
  const isJudge = user?.role === 'JUDGE'
  const isFiler = a.filedByUserId === user?.userId || a.filedByUserId === user?.email
  const isAssignedJudge = review && (review.judgeId === user?.userId || review.judgeId === user?.email)

  const canCancel = (isFiler && a.status === 'SUBMITTED') || (isAdmin && ['SUBMITTED','REVIEWED'].includes(a.status))
  const canOpenReview = a.status === 'SUBMITTED' && (isAdmin || isClerk || isJudge)
  const canDecide = a.status === 'REVIEWED' && (isAdmin || isAssignedJudge)
  const canEditDraft = a.status === 'REVIEWED' && (isAdmin || isAssignedJudge)
  const canUploadDoc = isAdmin || (isFiler && a.status === 'SUBMITTED')
  const canDeleteDoc = isAdmin || (isFiler && a.status === 'SUBMITTED')
  const canViewAudit = isFiler || isAdmin || isClerk || isJudge

  return (
    <div>
      <PageHeader
        title={`Appeal #${a.appealId}`}
        subtitle={<>On case <Link to={`/cases/${a.caseId}`} className="text-dark">#{a.caseId}</Link> · Filed by {a.filedByUserId} · {formatDateTime(a.filedDate)}</>}
        action={
          <div className="d-flex gap-2 align-items-center">
            <StatusBadge status={a.status} />
            <button className="btn btn-sm btn-outline-secondary" onClick={() => nav(-1)}>Back</button>
            {canCancel && <button className="btn btn-sm btn-outline-danger" onClick={cancelAppeal} disabled={busy}>Cancel appeal</button>}
          </div>
        }
      />

      <SectionCard title="Details" className="mb-3">
        <div className="row g-3 small">
          <div className="col-md-4"><div className="cf-muted">Case</div><Link to={`/cases/${a.caseId}`} className="text-dark">#{a.caseId}</Link></div>
          <div className="col-md-4"><div className="cf-muted">Filed by</div>{a.filedByUserId}</div>
          <div className="col-md-4"><div className="cf-muted">Filed on</div>{formatDateTime(a.filedDate)}</div>
          <div className="col-12"><div className="cf-muted">Reason</div><div style={{ whiteSpace: 'pre-wrap' }}>{a.reason}</div></div>
        </div>
      </SectionCard>

      <SectionCard title="Review" className="mb-3">
        {!review && a.status === 'SUBMITTED' && !canOpenReview && (
          <EmptyState icon="bi-hourglass-split" title="Awaiting judge assignment" hint="A clerk or judge will pick this up." />
        )}

        {!review && canOpenReview && (
          <form onSubmit={e => { e.preventDefault(); openReview() }}>
            <div className="alert alert-light border small">
              Assign a judge to start the review. Judges who presided over hearings on this case are excluded.
              {excluded.size > 0 && <div className="cf-muted mt-1">Excluded: {[...excluded].join(', ')}</div>}
            </div>
            <FormField label="Judge" required>
              <select className="form-select" value={judgeId} onChange={e => setJudgeId(e.target.value)} disabled={busy} required>
                <option value="">{eligibleJudges.length === 0 ? 'No eligible judges' : 'Select a judge...'}</option>
                {eligibleJudges.map(j => <option key={j.userId} value={j.userId}>{j.name || j.email} — {j.userId}</option>)}
              </select>
            </FormField>
            <button className="btn btn-dark" disabled={!judgeId.trim() || busy}>{busy ? 'Assigning...' : 'Open review'}</button>
          </form>
        )}

        {review && (
          <>
            <div className="row g-3 small mb-3">
              <div className="col-md-3"><div className="cf-muted">Review ID</div>#{review.reviewId}</div>
              <div className="col-md-3"><div className="cf-muted">Judge</div>{review.judgeId}</div>
              <div className="col-md-3"><div className="cf-muted">Assigned</div>{formatDateTime(review.reviewDate)}</div>
              <div className="col-md-3"><div className="cf-muted">Outcome</div><StatusBadge status={review.outcome ? (REVIEW_OUTCOME_LABELS[review.outcome] || review.outcome) : 'PENDING'} /></div>
              {review.remarks && <div className="col-12"><div className="cf-muted">Remarks</div><div style={{ whiteSpace: 'pre-wrap' }}>{review.remarks}</div></div>}
            </div>

            {canDecide && (
              <form onSubmit={issueDecision} className="border-top pt-3">
                <h6 className="fw-semibold small mb-2">Issue decision</h6>
                <FormField label="Outcome">
                  <select className="form-select" value={decision.outcome} onChange={e => setDecision({ ...decision, outcome: e.target.value })}>
                    {REVIEW_OUTCOME.map(o => <option key={o} value={o}>{REVIEW_OUTCOME_LABELS[o] || o}</option>)}
                  </select>
                </FormField>
                <FormField label="Remarks">
                  <textarea className="form-control" rows={3} value={decision.remarks}
                    onChange={e => setDecision({ ...decision, remarks: e.target.value })} />
                </FormField>
                <button className="btn btn-dark" disabled={busy}>{busy ? 'Issuing...' : 'Issue decision'}</button>
              </form>
            )}

            {canEditDraft && (
              <div className="border-top pt-3 mt-3">
                <h6 className="fw-semibold small mb-2">Update draft outcome</h6>
                <div className="d-flex gap-2 flex-wrap">
                  <select className="form-select form-select-sm w-auto" value={draftOutcome} onChange={e => setDraftOutcome(e.target.value)}>
                    <option value="">Select outcome...</option>
                    {REVIEW_OUTCOME.map(o => <option key={o} value={o}>{REVIEW_OUTCOME_LABELS[o] || o}</option>)}
                  </select>
                  <button className="btn btn-sm btn-outline-dark" onClick={updateDraft} disabled={!draftOutcome || busy}>Update draft</button>
                </div>
              </div>
            )}
          </>
        )}
      </SectionCard>

      <SectionCard title={`Documents (${docs.length})`} className="mb-3">
        {docs.length === 0 && !canUploadDoc && <EmptyState icon="bi-file-earmark" title="No documents attached" />}
        {docs.length > 0 && (
          <div className="row g-2 mb-3">
            {docs.map(d => (
              <div className="col-md-6" key={d.documentId}>
                <div className="cf-card p-3">
                  <div className="d-flex align-items-center gap-2 mb-1">
                    <i className="bi bi-file-earmark" />
                    <span className="fw-semibold small">{d.title}</span>
                    <span className="badge text-bg-light border small ms-auto">{d.type.replace('_', ' ')}</span>
                  </div>
                  {d.originalFileName && <div className="cf-muted small text-truncate">{d.originalFileName}</div>}
                  <div className="cf-muted small">{formatDateTime(d.uploadedDate)} · {d.uploadedBy}</div>
                  <div className="d-flex gap-1 mt-2">
                    <button className="btn btn-sm btn-outline-secondary" onClick={() => downloadDoc(d.documentId, d.originalFileName)}>Download</button>
                    {canDeleteDoc && <button className="btn btn-sm btn-outline-danger" onClick={() => deleteDoc(d.documentId)} disabled={busy}>Delete</button>}
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}

        {canUploadDoc && (
          <form onSubmit={uploadDoc} className="border-top pt-3">
            <h6 className="fw-semibold small mb-2">Attach document</h6>
            <div className="row g-2">
              <div className="col-md-7">
                <FormField label="Title" required>
                  <input className="form-control form-control-sm" value={uploadForm.title}
                    onChange={e => setUploadForm({ ...uploadForm, title: e.target.value })}
                    minLength={2} maxLength={255} required />
                </FormField>
              </div>
              <div className="col-md-5">
                <FormField label="Type">
                  <select className="form-select form-select-sm" value={uploadForm.type}
                    onChange={e => setUploadForm({ ...uploadForm, type: e.target.value })}>
                    {APPEAL_DOC_TYPES.map(t => <option key={t} value={t}>{t.replace('_', ' ')}</option>)}
                  </select>
                </FormField>
              </div>
            </div>
            <FormField label="File" hint={`Max 10 MB. Allowed: ${UPLOAD_ALLOWED_EXTENSIONS.join(', ')}`}>
              <input type="file" className="form-control form-control-sm" accept={UPLOAD_ACCEPT_ATTR}
                onChange={e => {
                  const f = e.target.files?.[0]
                  if (f) { const v = validateUpload(f); if (v) { notify.error(v); e.target.value = ''; return } }
                  setUploadForm({ ...uploadForm, file: f })
                }} />
              {uploadForm.file && <div className="cf-muted small mt-1">{uploadForm.file.name} ({formatBytes(uploadForm.file.size)})</div>}
            </FormField>
            <button className="btn btn-dark btn-sm" disabled={busy || !uploadForm.title.trim() || !uploadForm.file}>
              <i className="bi bi-upload me-1" />{busy ? 'Uploading...' : 'Upload'}
            </button>
          </form>
        )}
      </SectionCard>

      {canViewAudit && (
        <SectionCard
          title="Audit trail"
          action={<button className="btn btn-sm btn-outline-secondary" onClick={() => setShowAudit(s => !s)}>{showAudit ? 'Hide' : 'Show'}</button>}
        >
          {showAudit && (audit.length === 0
            ? <EmptyState icon="bi-clock-history" title="No audit entries" />
            : <ul className="list-unstyled small mb-0 d-flex flex-column gap-2">
                {audit.map(row => (
                  <li key={row.auditId} className="border-bottom pb-2">
                    <div className="d-flex justify-content-between">
                      <span className="fw-semibold">{APPEAL_AUDIT_ACTIONS[row.action] || row.action}</span>
                      <span className="cf-muted">{formatDateTime(row.timestamp)}</span>
                    </div>
                    <div className="cf-muted">
                      By {row.actorUserId}{row.actorRole && ` (${row.actorRole})`}
                      {(row.fromStatus || row.toStatus) && ` · ${row.fromStatus || '—'} → ${row.toStatus || '—'}`}
                    </div>
                    {row.metadata && <div className="cf-muted">{row.metadata}</div>}
                  </li>
                ))}
              </ul>
          )}
        </SectionCard>
      )}
    </div>
  )
}
