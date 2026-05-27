import { useState } from 'react'
import { notify } from '../components/ui/Toast'
import FormField from '../components/ui/FormField'

export default function Contact() {
  const [form, setForm] = useState({ name: '', email: '', message: '' })
  const set = (k) => (e) => setForm(f => ({ ...f, [k]: e.target.value }))

  const submit = (e) => {
    e.preventDefault()
    // No backend endpoint — pretend success and reset.
    notify.success('Thanks for reaching out. We will get back to you shortly.')
    setForm({ name: '', email: '', message: '' })
  }

  return (
    <section className="container py-5">
      <span className="badge text-bg-light border mb-3 px-3 py-2 small">Contact</span>
      <h1 className="h2 fw-semibold mb-3">Get in touch</h1>
      <p className="cf-muted mb-5" style={{ maxWidth: 640 }}>
        Questions about the platform or interested in deploying CaseFlow for your court? Drop us a note.
      </p>

      <div className="row g-4">
        <div className="col-lg-7">
          <div className="cf-card p-4">
            <form onSubmit={submit}>
              <FormField label="Your name" required>
                <input className="form-control" value={form.name} onChange={set('name')} required />
              </FormField>
              <FormField label="Email" required>
                <input className="form-control" type="email" value={form.email} onChange={set('email')} required />
              </FormField>
              <FormField label="Message" required>
                <textarea className="form-control" rows={5} value={form.message} onChange={set('message')} required />
              </FormField>
              <button type="submit" className="btn btn-dark">Send message</button>
            </form>
          </div>
        </div>
        <div className="col-lg-5">
          <div className="cf-card p-4 h-100">
            <h5 className="fw-semibold mb-3">Project details</h5>
            <ul className="list-unstyled small cf-muted d-flex flex-column gap-2 mb-0">
              <li><strong className="text-dark">Built for:</strong> Cognizant Internship Project</li>
              <li><strong className="text-dark">Stack:</strong> Spring Boot · React · MySQL · Microservices</li>
              <li><strong className="text-dark">Status:</strong> Active development</li>
              <li><strong className="text-dark">License:</strong> Internal / Academic</li>
            </ul>
          </div>
        </div>
      </div>
    </section>
  )
}
