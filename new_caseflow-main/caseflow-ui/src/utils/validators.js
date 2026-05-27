/**
 * Field-level validation helpers used across all forms in the application.
 * Each function returns null when the value is valid, or a human-readable
 * error string when it is not.
 */

// ── Primitives ────────────────────────────────────────────────────────────────

export function required(value, label = 'This field') {
  if (value === undefined || value === null) return `${label} is required.`
  if (typeof value === 'string' && value.trim() === '') return `${label} is required.`
  return null
}

export function minLength(value, min, label = 'This field') {
  if (!value) return null
  if (String(value).trim().length < min) return `${label} must be at least ${min} characters.`
  return null
}

export function maxLength(value, max, label = 'This field') {
  if (!value) return null
  if (String(value).trim().length > max) return `${label} must be at most ${max} characters.`
  return null
}

// ── Identity & Auth ───────────────────────────────────────────────────────────

export function validateEmail(email) {
  if (!email || !email.trim()) return 'Email is required.'
  const re = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!re.test(email.trim())) return 'Enter a valid email address.'
  return null
}

export function validatePassword(password) {
  if (!password) return 'Password is required.'
  if (password.length < 8) return 'Password must be at least 8 characters.'
  if (!/[A-Z]/.test(password)) return 'Password must contain at least one uppercase letter.'
  if (!/[a-z]/.test(password)) return 'Password must contain at least one lowercase letter.'
  if (!/[0-9]/.test(password)) return 'Password must contain at least one number.'
  return null
}

export function validatePasswordConfirm(password, confirm) {
  if (!confirm) return 'Please confirm your password.'
  if (password !== confirm) return 'Passwords do not match.'
  return null
}

export function validateName(name, label = 'Name') {
  const r = required(name, label)
  if (r) return r
  if (name.trim().length < 2) return `${label} must be at least 2 characters.`
  if (name.trim().length > 100) return `${label} must be at most 100 characters.`
  return null
}

export function validatePhone(phone, isRequired = false) {
  if (!phone || !phone.trim()) return isRequired ? 'Phone number is required.' : null
  const digits = phone.replace(/[\s\-().+]/g, '')
  if (!/^\d{10}$/.test(digits)) return 'Please enter a valid 10-digit phone number.'
  if (!/^[6-9]/.test(digits)) return 'Phone number must start with 6, 7, 8, or 9.'
  return null
}

// ── Case form ─────────────────────────────────────────────────────────────────

export function validateCaseTitle(title) {
  const r = required(title, 'Case title')
  if (r) return r
  if (title.trim().length < 5) return 'Case title must be at least 5 characters.'
  if (title.trim().length > 255) return 'Case title must be at most 255 characters.'
  return null
}

export function validateUserId(id, label = 'User ID') {
  if (!id || !String(id).trim()) return `${label} is required.`
  return null
}

// ── Hearing form ──────────────────────────────────────────────────────────────

export function validateHearingDate(dateStr) {
  if (!dateStr) return 'Hearing date is required.'
  const d = new Date(dateStr)
  if (isNaN(d.getTime())) return 'Enter a valid date.'
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  if (d < today) return 'Hearing date cannot be in the past.'
  return null
}

export function validateHearingTime(timeStr) {
  if (!timeStr) return 'Hearing time is required.'
  const re = /^([01]\d|2[0-3]):([0-5]\d)$/
  if (!re.test(timeStr)) return 'Enter a valid time in HH:MM format (24-hour).'
  return null
}

// ── Appeal form ───────────────────────────────────────────────────────────────

export function validateAppealReason(reason) {
  const r = required(reason, 'Appeal reason')
  if (r) return r
  if (reason.trim().length < 20) return 'Please provide a detailed reason (at least 20 characters).'
  if (reason.trim().length > 2000) return 'Reason must be at most 2000 characters.'
  return null
}

// ── Document / upload ─────────────────────────────────────────────────────────

export function validateDocumentTitle(title) {
  const r = required(title, 'Document title')
  if (r) return r
  if (title.trim().length > 255) return 'Document title must be at most 255 characters.'
  return null
}

// ── Date range ────────────────────────────────────────────────────────────────

export function validateDateRange(from, to) {
  if (from && to) {
    if (new Date(from) > new Date(to)) return 'Start date must be before end date.'
  }
  return null
}

// ── Generic numeric ID ────────────────────────────────────────────────────────

export function validatePositiveInteger(value, label = 'ID') {
  if (value === undefined || value === null || value === '') return `${label} is required.`
  const n = Number(value)
  if (!Number.isInteger(n) || n <= 0) return `${label} must be a positive whole number.`
  return null
}

// ── Compose: run multiple validators and return first error ───────────────────

/**
 * Run an array of validator functions against a value and return the first
 * error message, or null if all pass.
 *
 * Usage:
 *   const err = compose(title, [
 *     v => required(v, 'Title'),
 *     v => minLength(v, 5, 'Title'),
 *     v => maxLength(v, 255, 'Title'),
 *   ])
 */
export function compose(value, validators) {
  for (const fn of validators) {
    const err = fn(value)
    if (err) return err
  }
  return null
}

/**
 * Validate a whole form object.
 * `schema` is an object mapping field names to arrays of validators.
 * Returns an object of { fieldName: errorString } for every failing field.
 *
 * Usage:
 *   const errors = validateForm(formData, {
 *     email:    [v => validateEmail(v)],
 *     password: [v => validatePassword(v)],
 *   })
 *   const isValid = Object.keys(errors).length === 0
 */
export function validateForm(data, schema) {
  const errors = {}
  for (const [field, validators] of Object.entries(schema)) {
    const err = compose(data[field], validators)
    if (err) errors[field] = err
  }
  return errors
}
