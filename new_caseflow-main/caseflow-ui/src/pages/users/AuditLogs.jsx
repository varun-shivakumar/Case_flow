import { useEffect, useState } from 'react'
import { useParams } from 'react-router-dom'
import { users } from '../../api/services'
import { formatDateTime } from '../../utils/constants'
import PageHeader from '../../components/ui/PageHeader'
import EmptyState from '../../components/ui/EmptyState'
import LoadingState from '../../components/ui/LoadingState'
import DataTable from '../../components/ui/DataTable'
import { notify } from '../../components/ui/Toast'

export default function AuditLogs() {
  const { userId } = useParams()
  const [logs, setLogs]       = useState([])
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    async function load() {
      setLoading(true)
      try {
        const data = userId ? await users.auditLogsByUser(userId) : await users.auditLogs()
        setLogs(data || [])
      } catch (e) { notify.error(e.message) }
      finally { setLoading(false) }
    }
    load()
  }, [userId])

  const uniqueUsers = new Set((logs || []).map(l => l.userId).filter(Boolean)).size

  return (
    <div>
      <PageHeader
        title="Audit logs"
        subtitle={userId ? `Activity for user ${userId}` : 'Platform-wide audit activity'}
      />

      <div className="row g-3 mb-3">
        <div className="col-md-4">
          <div className="cf-card p-3">
            <div className="cf-muted small">Total logs</div>
            <div className="h4 fw-semibold mb-0">{logs.length}</div>
          </div>
        </div>
        <div className="col-md-4">
          <div className="cf-card p-3">
            <div className="cf-muted small">Users involved</div>
            <div className="h4 fw-semibold mb-0">{userId ? 1 : uniqueUsers}</div>
          </div>
        </div>
      </div>

      <div className="cf-card">
        {loading ? <LoadingState />
          : logs.length === 0 ? <EmptyState icon="bi-journal-text" title="No logs found" />
          : <DataTable columns={['ID', 'User ID', 'Action', 'Details', 'Timestamp']}>
              {logs.map((l, i) => (
                <tr key={l.id || l.auditLogId || i}>
                  <td className="cf-muted">#{l.id || l.auditLogId || i}</td>
                  <td>{l.userId || '—'}</td>
                  <td><span className="badge text-bg-light border">{l.action || l.event || '—'}</span></td>
                  <td style={{ maxWidth: 420 }} className="cf-muted">{l.details || l.description || '—'}</td>
                  <td className="cf-muted">{formatDateTime(l.timestamp || l.createdAt || l.date)}</td>
                </tr>
              ))}
            </DataTable>}
      </div>
    </div>
  )
}
