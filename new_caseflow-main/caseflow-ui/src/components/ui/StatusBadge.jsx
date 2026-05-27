import { statusBadgeClass } from '../../utils/constants'

// Reusable status pill. Uses Bootstrap utility classes for color.
export default function StatusBadge({ status }) {
  if (!status) return <span className="badge rounded-pill text-bg-light">—</span>
  return <span className={`badge rounded-pill ${statusBadgeClass(status)}`}>{status}</span>
}
