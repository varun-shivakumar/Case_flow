import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { cases } from '../../api/services'
import { formatDate } from '../../utils/constants'
import { useAuth } from '../../context/AuthContext'
import PageHeader from '../../components/ui/PageHeader'
import EmptyState from '../../components/ui/EmptyState'
import LoadingState from '../../components/ui/LoadingState'
import DataTable from '../../components/ui/DataTable'
import { notify } from '../../components/ui/Toast'

export default function PendingDocuments() {
  const { user } = useAuth()
  const [docs, setDocs] = useState([])
  const [loading, setLoading] = useState(true)
  const [downloadingId, setDownloadingId] = useState(null)

  const load = async () => {
    setLoading(true)
    try { setDocs(await cases.pendingDocs() || []) }
    catch (e) { notify.error(e.message) }
    finally { setLoading(false) }
  }
  useEffect(() => { load() }, [])

  const verify = async (d, status) => {
    let rejectionReason = ''
    if (status === 'REJECTED') {
      rejectionReason = prompt('Reason for rejection?') || ''
      if (!rejectionReason) return
    }
    try {
      await cases.verifyDoc(d.documentId, { status, rejectionReason, clerkId: user.userId || user.email })
      notify.success(`Document ${status.toLowerCase()}`); load()
    } catch (e) { notify.error(e.message) }
  }

  const downloadDoc = async (d) => {
    setDownloadingId(d.documentId)
    try {
      const res = await cases.downloadDoc(d.documentId, user.role)
      if (!res.ok) throw new Error(`Download failed (${res.status})`)
      const blob = await res.blob()
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url; a.download = d.originalFileName || `document_${d.documentId}`
      document.body.appendChild(a); a.click(); document.body.removeChild(a)
      URL.revokeObjectURL(url)
    } catch (e) { notify.error(e.message) }
    finally { setDownloadingId(null) }
  }

  return (
    <div>
      <PageHeader
        title="Pending documents"
        subtitle={`${docs.length} awaiting verification`}
        action={<button className="btn btn-sm btn-outline-secondary" onClick={load}>
          <i className="bi bi-arrow-clockwise me-1" />Refresh
        </button>}
      />

      <div className="cf-card">
        {loading ? <LoadingState />
          : docs.length === 0 ? <EmptyState icon="bi-check2-circle" title="All caught up" hint="No documents are pending verification." />
          : <DataTable columns={['ID', 'Case', 'Title', 'Type', 'Uploaded', 'By', 'Actions']}>
              {docs.map(d => (
                <tr key={d.documentId}>
                  <td>#{d.documentId}</td>
                  <td><Link to={`/cases/${d.caseId}`} className="fw-semibold text-dark">#{d.caseId}</Link></td>
                  <td>{d.title}</td>
                  <td><span className="badge text-bg-light border">{d.type}</span></td>
                  <td className="cf-muted">{formatDate(d.uploadedDate)}</td>
                  <td className="cf-muted">{d.uploadedBy}</td>
                  <td>
                    <div className="d-flex gap-1 flex-wrap">
                      <Link to={`/cases/${d.caseId}`} className="btn btn-sm btn-outline-secondary">Case</Link>
                      <button className="btn btn-sm btn-outline-secondary" disabled={downloadingId === d.documentId} onClick={() => downloadDoc(d)}>
                        {downloadingId === d.documentId ? <span className="spinner-border spinner-border-sm" /> : 'Download'}
                      </button>
                      <button className="btn btn-sm btn-success" onClick={() => verify(d, 'VERIFIED')}>Verify</button>
                      <button className="btn btn-sm btn-outline-danger" onClick={() => verify(d, 'REJECTED')}>Reject</button>
                    </div>
                  </td>
                </tr>
              ))}
            </DataTable>}
      </div>
    </div>
  )
}
