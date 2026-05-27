// Minimal table wrapper. Pass `columns` (array of header strings) and `children` rows.
export default function DataTable({ columns, children }) {
  return (
    <div className="table-responsive">
      <table className="table align-middle mb-0 small" style={{ minWidth: 540 }}>
        <thead className="text-uppercase cf-muted" style={{ fontSize: 11, letterSpacing: '0.04em' }}>
          <tr>
            {columns.map(c => <th key={c} className="fw-semibold py-2">{c}</th>)}
          </tr>
        </thead>
        <tbody>{children}</tbody>
      </table>
    </div>
  )
}
