import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { cases, hearings, appeals, compliance, workflow } from '../../api/services'
import {
  CASE_STATUS, DOC_TYPES, REVIEW_OUTCOME_LABELS,
  formatDate, formatDateTime,
  UPLOAD_ALLOWED_EXTENSIONS, UPLOAD_ACCEPT_ATTR, validateUpload, formatBytes,
} from '../../utils/constants'
import { useAuth } from '../../context/AuthContext'
import PageHeader from '../../components/ui/PageHeader'
import StatusBadge from '../../components/ui/StatusBadge'
import SectionCard from '../../components/ui/SectionCard'
import EmptyState from '../../components/ui/EmptyState'
import LoadingState from '../../components/ui/LoadingState'
import DataTable from '../../components/ui/DataTable'
import FormField from '../../components/ui/FormField'
import { notify } from '../../components/ui/Toast'

export default function CaseDetail() {
  const { caseId } = useParams()
  const { user } = useAuth()

  const [c, setCase] = useState(null)
  const [docs, setDocs] = useState([])
  const [hrgs, setHrgs] = useState([])
  const [apps, setApps] = useState([])
  const [reviews, setReviews] = useState([])
  const [compl, setCompl] = useState([])
  const [stages, setStages] = useState([])
  const [uploadForm, setUploadForm] = useState({ title: '', type: 'PETITION', uri: '', file: null })
  const [statusUpdate, setStatusUpdate] = useState('')

  const load = async () => {
    const canLoadReviews = ['ADMIN', 'CLERK', 'JUDGE'].includes(user?.role)
    const [cs, ds, hs, as, cm, st, rv] = await Promise.allSettled([
      cases.get(caseId), cases.docs(caseId), hearings.byCase(caseId),
      appeals.byCase(caseId), compliance.byCase(caseId), workflow.stages(caseId),
      canLoadReviews ? appeals.reviewsByCase(caseId) : Promise.resolve([]),
    ])
    if (cs.status === 'fulfilled') setCase(cs.value)
    if (ds.status === 'fulfilled') setDocs(ds.value || [])
    if (hs.status === 'fulfilled') setHrgs(hs.value || [])
    if (as.status === 'fulfilled') setApps(as.value || [])
    if (cm.status === 'fulfilled') setCompl(cm.value || [])
    if (st.status === 'fulfilled') setStages(st.value || [])
    if (rv.status === 'fulfilled') setReviews(rv.value || [])
    if (cs.status === 'rejected') notify.error(cs.reason.message)
  }
  useEffect(() => { load() }, [caseId])

  const uploadDoc = async (e) => {
    e.preventDefault()
    if (!uploadForm.file && !uploadForm.uri) { notify.error('Attach a file or provide a URI.'); return }
    if (uploadForm.file) {
      const v = validateUpload(uploadForm.file)
      if (v) { notify.error(v); return }
    }
    try {
      const fd = new FormData()
      fd.append('caseId', caseId); fd.append('title', uploadForm.title); fd.append('type', uploadForm.type)
      fd.append('uploadedBy', user.email)
      if (uploadForm.uri)  fd.append('uri',  uploadForm.uri)
      if (uploadForm.file) fd.append('file', uploadForm.file)
      await cases.uploadDoc(fd)
      notify.success('Document uploaded')
      setUploadForm({ title: '', type: 'PETITION', uri: '', file: null })
      const fi = document.getElementById('case-doc-file'); if (fi) fi.value = ''
      load()
    } catch (e) { notify.error(e.message || 'Upload failed.') }
  }

  const updateStatus = async () => {
    if (!statusUpdate) return
    try {
      await cases.updateStatus(caseId, statusUpdate, user.email)
      notify.success('Status updated'); load()
    } catch (e) { notify.error(e.message) }
  }

  const verifyDoc = async (doc, status) => {
    let rejectionReason = ''
    if (status === 'REJECTED') {
      rejectionReason = prompt('Reason for rejection?') || ''
      if (!rejectionReason) return
    }
    try {
      await cases.verifyDoc(doc.documentId, { status, rejectionReason, clerkId: user.userId || user.email })
      notify.success(`Document ${status.toLowerCase()}`)
      load()
    } catch (e) { notify.error(e.message) }
  }

  const downloadDoc = async (docId) => {
    try {
      const res = await cases.downloadDoc(docId, user.role)
      if (!res.ok) throw new Error('Download failed')
      const blob = await res.blob()
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url; a.download = `document_${docId}`; a.click()
      URL.revokeObjectURL(url)
    } catch (e) { notify.error(e.message) }
  }

  if (!c) return <div><PageHeader title="Case" /><LoadingState /></div>

  const canUpload =
    user?.role === 'ADMIN' ||
    (user?.role === 'LAWYER' && c.lawyerId && (c.lawyerId === user?.userId || c.lawyerId === user?.email)) ||
    (user?.role === 'LITIGANT' && !c.lawyerId)
  const canVerify = ['CLERK', 'ADMIN'].includes(user?.role)
  const canUpdateStatus = ['CLERK', 'JUDGE', 'ADMIN'].includes(user?.role)

  return (
    <div>
      <PageHeader
        title={`Case #${c.caseId}`}
        subtitle={c.title}
        action={
          <div className="d-flex gap-2 flex-wrap">
            {['ADMIN','CLERK'].includes(user?.role) &&
              <Link to={`/workflow/${c.caseId}`} className="btn btn-sm btn-outline-secondary">Workflow</Link>}
            {['LITIGANT','LAWYER','ADMIN'].includes(user?.role) && c.status === 'CLOSED' &&
              <Link to={`/appeals/file?caseId=${c.caseId}`} className="btn btn-sm btn-outline-secondary">File appeal</Link>}
          </div>
        }
      />

      {/* Info */}
      <SectionCard title="Overview" className="mb-3">
        <div className="row g-3 small">
          <div className="col-md-3"><div className="cf-muted">Status</div><StatusBadge status={c.status} /></div>
          <div className="col-md-3"><div className="cf-muted">Filed</div>{formatDateTime(c.filedDate)}</div>
          <div className="col-md-3"><div className="cf-muted">Litigant</div>{c.litigantId}</div>
          <div className="col-md-3"><div className="cf-muted">Lawyer</div>{c.lawyerId || '—'}</div>
        </div>
        {canUpdateStatus && (
          <div className="d-flex gap-2 mt-3 flex-wrap">
            <select className="form-select form-select-sm w-auto" value={statusUpdate} onChange={e => setStatusUpdate(e.target.value)}>
              <option value="">Change status…</option>
              {CASE_STATUS.map(s => <option key={s} value={s}>{s}</option>)}
            </select>
            <button className="btn btn-sm btn-dark" onClick={updateStatus} disabled={!statusUpdate}>Update</button>
          </div>
        )}
      </SectionCard>

      {/* Documents */}
      <SectionCard title={`Documents (${docs.length})`} padded={false} className="mb-3">
        {docs.length === 0 ? <EmptyState icon="bi-file-earmark" title="No documents" />
          : <DataTable columns={['ID', 'Title', 'Type', 'Status', 'Uploaded', 'By', '']}>
              {docs.map(d => (
                <tr key={d.documentId}>
                  <td>#{d.documentId}</td>
                  <td>{d.title}</td>
                  <td>{d.type}</td>
                  <td>
                    <StatusBadge status={d.verificationStatus} />
                    {d.rejectionReason && <div className="cf-muted small mt-1">{d.rejectionReason}</div>}
                  </td>
                  <td className="cf-muted">{formatDate(d.uploadedDate)}</td>
                  <td className="cf-muted">{d.uploadedBy}</td>
                  <td>
                    <div className="d-flex gap-1 flex-wrap">
                      <button className="btn btn-sm btn-outline-secondary" onClick={() => downloadDoc(d.documentId)}>Download</button>
                      {canVerify && d.verificationStatus === 'PENDING' && <>
                        <button className="btn btn-sm btn-success" onClick={() => verifyDoc(d, 'VERIFIED')}>Verify</button>
                        <button className="btn btn-sm btn-outline-danger" onClick={() => verifyDoc(d, 'REJECTED')}>Reject</button>
                      </>}
                    </div>
                  </td>
                </tr>
              ))}
            </DataTable>}

        {canUpload && (
          <div className="p-3 border-top">
            <h6 className="fw-semibold mb-3 small">Upload document</h6>
            <form onSubmit={uploadDoc}>
              <div className="row g-2">
                <div className="col-md-6">
                  <FormField label="Title" required>
                    <input className="form-control form-control-sm" value={uploadForm.title}
                      onChange={e => setUploadForm({ ...uploadForm, title: e.target.value })}
                      minLength={2} maxLength={255} required />
                  </FormField>
                </div>
                <div className="col-md-6">
                  <FormField label="Type">
                    <select className="form-select form-select-sm" value={uploadForm.type}
                      onChange={e => setUploadForm({ ...uploadForm, type: e.target.value })}>
                      {DOC_TYPES.map(t => <option key={t} value={t}>{t}</option>)}
                    </select>
                  </FormField>
                </div>
                <div className="col-md-6">
                  <FormField label="External URI">
                    <input className="form-control form-control-sm" value={uploadForm.uri}
                      onChange={e => setUploadForm({ ...uploadForm, uri: e.target.value })} />
                  </FormField>
                </div>
                <div className="col-md-6">
                  <FormField label="File" hint={`Max 10 MB. Allowed: ${UPLOAD_ALLOWED_EXTENSIONS.join(', ')}`}>
                    <input id="case-doc-file" className="form-control form-control-sm" type="file" accept={UPLOAD_ACCEPT_ATTR}
                      onChange={e => {
                        const f = e.target.files[0] || null
                        if (f) {
                          const v = validateUpload(f)
                          if (v) { notify.error(v); e.target.value = ''; setUploadForm({ ...uploadForm, file: null }); return }
                        }
                        setUploadForm({ ...uploadForm, file: f })
                      }} />
                    {uploadForm.file && <div className="small cf-muted mt-1">{uploadForm.file.name} ({formatBytes(uploadForm.file.size)})</div>}
                  </FormField>
                </div>
              </div>
              <button className="btn btn-dark btn-sm"><i className="bi bi-upload me-1" />Upload</button>
            </form>
          </div>
        )}

        {!canUpload && user?.role === 'LITIGANT' && c.lawyerId && (
          <div className="p-3 border-top small cf-muted">
            <i className="bi bi-info-circle me-1" />Lawyer <strong>{c.lawyerId}</strong> is handling document uploads for this case.
          </div>
        )}
      </SectionCard>

      {/* Hearings */}
      <SectionCard title={`Hearings (${hrgs.length})`} padded={false} className="mb-3">
        {hrgs.length === 0 ? <EmptyState icon="bi-calendar2" title="No hearings scheduled" />
          : <DataTable columns={['ID', 'Date', 'Time', 'Judge', 'Status', '']}>
              {hrgs.map(h => (
                <tr key={h.hearingId}>
                  <td>#{h.hearingId}</td>
                  <td>{formatDate(h.hearingDate)}</td>
                  <td>{h.hearingTime}</td>
                  <td className="cf-muted">{h.judgeId}</td>
                  <td><StatusBadge status={h.status} /></td>
                  <td><Link to={`/hearings/${h.hearingId}`} className="btn btn-sm btn-outline-secondary">Open</Link></td>
                </tr>
              ))}
            </DataTable>}
      </SectionCard>

      {/* Workflow */}
      <SectionCard title={`Workflow stages (${stages.length})`} padded={false} className="mb-3">
        {stages.length === 0 ? <EmptyState icon="bi-diagram-3" title="No workflow initialized"
          action={['ADMIN','CLERK'].includes(user?.role) && <Link to={`/workflow/${c.caseId}`} className="btn btn-sm btn-dark">Initialize</Link>} />
          : <DataTable columns={['Seq', 'Stage', 'Role', 'SLA days', 'Active', 'Skipped']}>
              {stages.map(s => (
                <tr key={s.stageId}>
                  <td>{s.sequenceNumber}</td>
                  <td>{s.stageName}</td>
                  <td className="cf-muted">{s.roleResponsible}</td>
                  <td>{s.slaDays}</td>
                  <td>{s.active ? <i className="bi bi-check" /> : '—'}</td>
                  <td>{s.skipped ? <i className="bi bi-check" /> : '—'}</td>
                </tr>
              ))}
            </DataTable>}
      </SectionCard>

      {/* Appeals */}
      <SectionCard title={`Appeals (${apps.length})`} padded={false} className="mb-3">
        {apps.length === 0 ? <EmptyState icon="bi-arrow-counterclockwise" title="No appeals" />
          : <DataTable columns={['ID', 'Filed', 'By', 'Reason', 'Status', '']}>
              {apps.map(a => (
                <tr key={a.appealId}>
                  <td>#{a.appealId}</td>
                  <td className="cf-muted">{formatDate(a.filedDate)}</td>
                  <td className="cf-muted">{a.filedByUserId}</td>
                  <td>{a.reason}</td>
                  <td><StatusBadge status={a.status} /></td>
                  <td><Link to={`/appeals/${a.appealId}`} className="btn btn-sm btn-outline-secondary">Open</Link></td>
                </tr>
              ))}
            </DataTable>}
      </SectionCard>

      {/* Reviews */}
      {['ADMIN','CLERK','JUDGE'].includes(user?.role) && (
        <SectionCard title={`Appeal reviews (${reviews.length})`} padded={false} className="mb-3">
          {reviews.length === 0 ? <EmptyState icon="bi-clipboard-check" title="No reviews recorded" />
            : <DataTable columns={['Review', 'Appeal', 'Judge', 'Assigned', 'Outcome', '']}>
                {reviews.map(r => (
                  <tr key={r.reviewId}>
                    <td>#{r.reviewId}</td>
                    <td><Link to={`/appeals/${r.appealId}`} className="text-dark">#{r.appealId}</Link></td>
                    <td className="cf-muted">{r.judgeId}</td>
                    <td className="cf-muted">{formatDateTime(r.reviewDate)}</td>
                    <td>{r.outcome ? <StatusBadge status={REVIEW_OUTCOME_LABELS[r.outcome] || r.outcome} /> : <StatusBadge status="PENDING" />}</td>
                    <td><Link to={`/appeals/${r.appealId}`} className="btn btn-sm btn-outline-secondary">Open</Link></td>
                  </tr>
                ))}
              </DataTable>}
        </SectionCard>
      )}

      {/* Compliance */}
      <SectionCard title={`Compliance records (${compl.length})`} padded={false}>
        {compl.length === 0 ? <EmptyState icon="bi-shield-check" title="No compliance records" />
          : <DataTable columns={['ID', 'Type', 'Result', 'Date', 'Notes']}>
              {compl.map(r => (
                <tr key={r.complianceId}>
                  <td>#{r.complianceId}</td>
                  <td>{r.type}</td>
                  <td><StatusBadge status={r.result} /></td>
                  <td className="cf-muted">{formatDate(r.date)}</td>
                  <td className="cf-muted">{r.notes}</td>
                </tr>
              ))}
            </DataTable>}
      </SectionCard>
    </div>
  )
}
