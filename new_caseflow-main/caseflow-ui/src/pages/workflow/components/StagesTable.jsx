import SectionCard from '../../../components/ui/SectionCard'
import DataTable from '../../../components/ui/DataTable'
import StatusBadge from '../../../components/ui/StatusBadge'
import { formatDate } from '../../../utils/constants'

function StageStatus({ s }) {
  if (s.active) return <StatusBadge status="ACTIVE" />
  if (s.skipped) return <StatusBadge status="SKIPPED" />
  if (s.completedAt) return <StatusBadge status="COMPLETED" />
  return <StatusBadge status="PENDING" />
}

export default function StagesTable({ stages }) {
  return (
    <SectionCard title="All stages" padded={false}>
      <DataTable columns={['ID', 'Seq', 'Stage', 'Role', 'SLA', 'Started', 'Completed', 'Status']}>
        {stages.map(s => (
          <tr key={s.stageId}>
            <td className="cf-muted">#{s.stageId}</td>
            <td>{s.sequenceNumber}</td>
            <td className="fw-semibold">{s.stageName}</td>
            <td className="cf-muted">{s.roleResponsible}</td>
            <td>{s.slaDays}d</td>
            <td className="cf-muted">{formatDate(s.startedAt)}</td>
            <td className="cf-muted">{formatDate(s.completedAt)}</td>
            <td><StageStatus s={s} /></td>
          </tr>
        ))}
      </DataTable>
    </SectionCard>
  )
}
