// Eyebrow + headline + subtitle. Used at the top of each landing-page section.
import Eyebrow from './Eyebrow'

export default function SectionTitle({ eyebrow, title, subtitle, align = 'left' }) {
  return (
    <div className={`mb-4 ${align === 'center' ? 'text-center' : ''}`}>
      {eyebrow && <div className="mb-3"><Eyebrow>{eyebrow}</Eyebrow></div>}
      <h2 className="h3 fw-semibold mb-2" style={{ letterSpacing: '-0.02em' }}>{title}</h2>
      {subtitle && (
        <p className="cf-muted mb-0" style={{ maxWidth: align === 'center' ? 640 : 720, margin: align === 'center' ? '0 auto' : '' }}>
          {subtitle}
        </p>
      )}
    </div>
  )
}
