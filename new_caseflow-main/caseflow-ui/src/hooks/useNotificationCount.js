import { useEffect, useState } from 'react'
import { notifications as notifApi } from '../api/services'

// Fetches and maintains the current unread notification count.
// Polls every 5s while the tab is visible, slows to 60s when hidden.
// Also refreshes on navigation (locationKey) and on cf:notify-bump events.
export function useNotificationCount(user, locationKey) {
  const [unread, setUnread] = useState(0)

  // Helper to fetch and update the count
  async function fetchCount(setFn, guard) {
    try {
      let res
      try { res = await notifApi.myCount() }
      catch { res = await notifApi.count(user.userId || user.email) }
      const n = res?.unreadCount ?? res?.count ?? (Array.isArray(res) ? res.length : 0)
      if (guard()) setFn(Number(n) || 0)
    } catch { /* ignore network errors silently */ }
  }

  // Refresh once on every navigation
  useEffect(() => {
    if (!user) return
    let cancelled = false
    fetchCount(setUnread, () => !cancelled)
    return () => { cancelled = true }
  }, [locationKey, user])

  // Polling loop with visibility-aware rate adjustment
  useEffect(() => {
    if (!user) return
    let active = true
    let interval = 5000
    let tick = setInterval(() => fetchCount(setUnread, () => active), interval)

    const reschedule = (ms) => {
      if (ms === interval) return
      clearInterval(tick)
      interval = ms
      tick = setInterval(() => fetchCount(setUnread, () => active), ms)
    }

    const onVisibility = () => {
      if (document.visibilityState === 'visible') {
        fetchCount(setUnread, () => active)
        reschedule(5000)
      } else {
        reschedule(60000)
      }
    }

    const onBump = () => fetchCount(setUnread, () => active)

    document.addEventListener('visibilitychange', onVisibility)
    window.addEventListener('cf:notify-bump', onBump)
    window.addEventListener('focus', onBump)

    return () => {
      active = false
      clearInterval(tick)
      document.removeEventListener('visibilitychange', onVisibility)
      window.removeEventListener('cf:notify-bump', onBump)
      window.removeEventListener('focus', onBump)
    }
  }, [user])

  return unread
}
