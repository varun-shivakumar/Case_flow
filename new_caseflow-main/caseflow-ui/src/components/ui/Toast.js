import { toast } from 'react-toastify'

// Single source of toast notifications — keeps the API tiny and consistent.
// error() uses the message as toastId so identical errors (e.g. from React 18
// StrictMode double-invoking effects) only appear once.
export const notify = {
  success: (msg) => toast.success(msg),
  error:   (msg) => {
    const text = typeof msg === 'string' ? msg : msg?.message || 'Something went wrong'
    toast.error(text, { toastId: text })
  },
  info:    (msg) => toast.info(msg),
  warn:    (msg) => toast.warn(msg),
}
