import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { notifications as notifApi } from '../../api/services'
import { formatDateTime } from '../../utils/constants'
import { useAuth } from '../../context/AuthContext'
import PageHeader from '../../components/ui/PageHeader'
import StatusBadge from '../../components/ui/StatusBadge'
import EmptyState from '../../components/ui/EmptyState'
import LoadingState from '../../components/ui/LoadingState'
import DataTable from '../../components/ui/DataTable'
import { notify } from '../../components/ui/Toast'

export default function NotificationList() {
  const { user } = useAuth()
  const isPrivileged = user?.role === 'ADMIN' || user?.role === 'CLERK'

  const [lookupId, setLookupId] = useState('')
  const [list, setList]         = useState([])
  const [showAll, setShowAll]   = useState(false)
  const [loading, setLoading]   = useState(false)

  const load = async () => {
    setLoading(true)
    try {
      let data
      if (isPrivileged && lookupId.trim()) {
        data = showAll ? await notifApi.byUser(lookupId.trim()) : await notifApi.unread(lookupId.trim())
      } else {
        data = showAll ? await notifApi.my() : await notifApi.myUnread()
      }
      setList(Array.isArray(data) ? data : [])
    } catch (e) { notify.error(e.message) }
    finally { setLoading(false) }
  }
  useEffect(() => { load() }, [showAll, lookupId])

  const markRead = async (id) => {
    try {
      await notifApi.markRead(id)
      if (showAll) setList(prev => prev.map(n => n.notificationId === id ? { ...n, status: 'READ' } : n))
      else setList(prev => prev.filter(n => n.notificationId !== id))
    } catch (e) { notify.error(e.message) }
  }

  const markAll = async () => {
    try {
      if (isPrivileged && lookupId.trim()) await notifApi.markAllRead(lookupId.trim())
      else await notifApi.myMarkAllRead()
      notify.success('All notifications marked as read')
      load()
    } catch (e) { notify.error(e.message) }
  }

  const unread = list.filter(n => n.status === 'UNREAD').length

  return (
    <div>
      <PageHeader
        title="Notifications"
        subtitle={unread > 0 ? `${unread} unread` : 'All caught up'}
        action={
          <div className="d-flex gap-2">
            <button className="btn btn-sm btn-outline-secondary" onClick={markAll} disabled={unread === 0}>
              Mark all read
            </button>
            {isPrivileged && <Link to="/notifications/create" className="btn btn-sm btn-dark">Send</Link>}
          </div>
        }
      />

      <div className="cf-card">
        <div className="d-flex gap-2 align-items-center p-3 border-bottom flex-wrap">
          <div className="btn-group btn-group-sm">
            <button className={`btn btn-${!showAll ? 'dark' : 'outline-secondary'}`} onClick={() => setShowAll(false)}>Unread</button>
            <button className={`btn btn-${showAll ? 'dark' : 'outline-secondary'}`} onClick={() => setShowAll(true)}>All</button>
          </div>
          {isPrivileged && (
            <input className="form-control form-control-sm w-auto" placeholder="Lookup user ID"
              value={lookupId} onChange={e => setLookupId(e.target.value)} />
          )}
          <button className="btn btn-sm btn-outline-secondary ms-auto" onClick={load}>
            <i className="bi bi-arrow-clockwise me-1" />Refresh
          </button>
        </div>

        {loading ? <LoadingState />
          : list.length === 0 ? <EmptyState icon={showAll ? 'bi-inbox' : 'bi-bell-slash'} title={showAll ? 'No notifications' : "You're all caught up"} />
          : <DataTable columns={['ID', 'Category', 'Case', 'Message', 'Status', 'Date', '']}>
              {list.map(n => (
                <tr key={n.notificationId} className={n.status === 'UNREAD' ? 'fw-semibold' : ''}>
                  <td className="cf-muted">#{n.notificationId}</td>
                  <td><span className="badge text-bg-light border">{n.category}</span></td>
                  <td>{n.caseId ? <Link to={`/cases/${n.caseId}`} className="text-dark">#{n.caseId}</Link> : <span className="cf-muted">—</span>}</td>
                  <td style={{ maxWidth: 360 }}>{n.message}</td>
                  <td><StatusBadge status={n.status} /></td>
                  <td className="cf-muted text-nowrap">{formatDateTime(n.createdDate)}</td>
                  <td>{n.status === 'UNREAD' && (
                    <button className="btn btn-sm btn-outline-secondary" onClick={() => markRead(n.notificationId)}>
                      Mark read
                    </button>
                  )}</td>
                </tr>
              ))}
            </DataTable>}
      </div>
    </div>
  )
}
