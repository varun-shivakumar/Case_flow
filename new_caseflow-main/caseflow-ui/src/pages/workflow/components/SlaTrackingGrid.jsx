import SectionCard from '../../../components/ui/SectionCard'
import SlaRecordCard from './SlaRecordCard'

export default function SlaTrackingGrid({ records, className = '' }) {
  if (!records?.length) return null
  return (
    <SectionCard title="SLA tracking" className={className}>
      <div className="row g-2">
        {records.map(r => (
          <div key={r.slaRecordId} className="col-md-6">
            <SlaRecordCard record={r} />
          </div>
        ))}
      </div>
    </SectionCard>
  )
}
