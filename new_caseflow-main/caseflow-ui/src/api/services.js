import { api } from './client'

export const auth = {
  register: (data) => api.post('/api/auth/register', data),
  login: (data, rememberMe = false) => api.post(`/api/auth/login?rememberMe=${rememberMe}`, data),
  me: () => api.get('/api/auth/me'),
  changePassword: (data) => api.put('/api/auth/change-password', data),
  forgotPassword: (data) => api.post('/api/auth/forgot-password', data),
  resetPassword: (data) => api.post('/api/auth/reset-password', data),
  logout: (email) => api.post(`/api/auth/logout?email=${encodeURIComponent(email)}`),
}

export const users = {
  create: (data) => api.post('/api/users/create', data),
  list: () => api.get('/api/users'),
  get: (id) => api.get(`/api/users/${id}`),
  byRole: (role) => api.get(`/api/users/role/${role}`),
  setStatus: (id, status) => api.patch(`/api/users/${id}/status?status=${status}`),
  resetPassword: (id, newPassword) => api.put(`/api/users/${id}/reset-password?newPassword=${encodeURIComponent(newPassword)}`),
  auditLogs: () => api.get('/api/users/audit-logs'),
  auditLogsByUser: (userId) => api.get(`/api/users/audit-logs/${userId}`),
  del: (id) => api.del(`/api/users/${id}`),
  paginated: (page = 0, size = 10, sort = 'userId,asc') => api.get('/api/users/paginated', { page, size, sort }),
}

export const cases = {
  file: (data) => api.post('/api/cases/file', data),
  uploadDoc: (formData) => api.postForm('/api/cases/documents/upload', formData),
  downloadDoc: (docId, role) => api.raw(`/api/cases/documents/${docId}/download?role=${role}`),
  verifyDoc: (docId, data) => api.patch(`/api/cases/documents/${docId}/verify`, data),
  updateStatus: (caseId, newStatus, updatedBy) =>
    api.patch(`/api/cases/${caseId}/status?newStatus=${newStatus}&updatedBy=${encodeURIComponent(updatedBy)}`),
  get: (caseId) => api.get(`/api/cases/${caseId}`),
  list: () => api.get('/api/cases'),
  byLitigant: (litigantId) => api.get(`/api/cases/litigant/${litigantId}`),
  byLawyer: (lawyerId) => api.get(`/api/cases/lawyer/${lawyerId}`),
  byStatus: (status) => api.get(`/api/cases/status/${status}`),
  docs: (caseId) => api.get(`/api/cases/${caseId}/documents`),
  pendingDocs: () => api.get('/api/cases/documents/pending'),
  getDoc: (docId) => api.get(`/api/cases/documents/${docId}`),
  paginated: (page = 0, size = 10, sort = 'caseId,desc') => api.get('/api/cases/paginated', { page, size, sort }),
}

export const hearings = {
  schedule: (data) => api.post('/api/hearings/schedule', data),
  reschedule: (id, data) => api.patch(`/api/hearings/${id}/reschedule`, data),
  complete: (id, data) => api.patch(`/api/hearings/${id}/complete`, data),
  get: (id) => api.get(`/api/hearings/${id}`),
  list: () => api.get('/api/hearings'),
  byCase: (caseId) => api.get(`/api/hearings/case/${caseId}`),
  byJudge: (judgeId) => api.get(`/api/hearings/judge/${judgeId}`),
  byStatus: (status) => api.get(`/api/hearings/status/${status}`),
  paginated: (page = 0, size = 10, sort = 'hearingId,desc') => api.get('/api/hearings/paginated', { page, size, sort }),
}

export const workflow = {
  init: (caseId, data) => api.post(`/api/workflow/lifecycle/${caseId}/initialize`, data),
  stages: (caseId) => api.get(`/api/workflow/cases/${caseId}/stages`),
  current: (caseId) => api.get(`/api/workflow/cases/${caseId}/stages/current`),
  advance: (caseId) => api.post(`/api/workflow/cases/${caseId}/advance`),
  sla: (caseId) => api.get(`/api/workflow/cases/${caseId}/sla`),
  breached: () => api.get('/api/workflow/sla/breached'),
  active: () => api.get('/api/workflow/sla/active'),
  checkSla: () => api.post('/api/workflow/sla/check'),
  warnings: () => api.get('/api/workflow/sla/warnings'),
  rollback: (caseId) => api.post(`/api/workflow/cases/${caseId}/rollback`),
  skip: (caseId, data) => api.post(`/api/workflow/cases/${caseId}/skip`, data),
  extendSla: (caseId, data) => api.patch(`/api/workflow/cases/${caseId}/sla/extend`, data),
  reassign: (caseId, data) => api.patch(`/api/workflow/cases/${caseId}/reassign`, data),
}

export const appeals = {
  // Filing & lifecycle (caller does NOT send filedByUserId — backend resolves it from JWT)
  file:   (data) => api.post('/api/appeals', data),
  cancel: (id)   => api.patch(`/api/appeals/${id}/cancel`),

  // Reads
  get:       (id)      => api.get(`/api/appeals/${id}`),
  mine:      ()        => api.get('/api/appeals/my'),
  byCase:    (caseId)  => api.get(`/api/appeals/case/${caseId}`),
  byUser:    (userId)  => api.get(`/api/appeals/user/${userId}`),
  byStatus:  (status)  => api.get(`/api/appeals/status/${status}`),
  paginated: (page = 0, size = 10, sort = 'appealId,desc') =>
    api.get('/api/appeals/paginated', { page, size, sort }),

  // Audit trail (filer or admin/clerk/judge)
  audit: (id) => api.get(`/api/appeals/${id}/audit`),

  // Reviews
  openReview:     (id, judgeId)      => api.post(`/api/appeals/${id}/review?judgeId=${encodeURIComponent(judgeId)}`),
  getReview:      (id)               => api.get(`/api/appeals/${id}/review`),
  decide:         (id, data)         => api.post(`/api/appeals/${id}/decide`, data),
  updateOutcome:  (reviewId, data)   => api.patch(`/api/appeals/reviews/${reviewId}/outcome`, data),
  reviewsByCase:  (caseId)           => api.get(`/api/appeals/reviews/case/${caseId}`),
  reviewsByJudge: (judgeId)          => api.get(`/api/appeals/reviews/judge/${judgeId}`),
  myReviews:      ()                 => api.get('/api/appeals/reviews/my'),

  // Documents (multipart upload)
  uploadDoc:   (appealId, formData) => api.postForm(`/api/appeals/${appealId}/documents`, formData),
  listDocs:    (appealId)           => api.get(`/api/appeals/${appealId}/documents`),
  getDoc:      (docId)              => api.get(`/api/appeals/documents/${docId}`),
  downloadDoc: (docId)              => api.raw(`/api/appeals/documents/${docId}/download`),
  deleteDoc:   (docId)              => api.del(`/api/appeals/documents/${docId}`),
}

export const compliance = {
  // Run automated compliance check — caseIds optional (empty = all cases)
  runCheck: (data) => api.post('/api/compliance/check', data),
  byCase: (caseId) => api.get(`/api/compliance/case/${caseId}`),
  complianceRecordsPaginated: (page = 0, size = 10) => api.get('/api/compliance/paginated', { page, size }),
  deleteRecord: (id) => api.del(`/api/compliance/${id}`),
  // Compliance check runs — server-side aggregation
  runs: () => api.get('/api/compliance/runs'),
  runRecords: (runId) => api.get(`/api/compliance/runs/${encodeURIComponent(runId)}`),
  // Bulk delete by id list (ADMIN only) — body: { ids: [1,2,3] }
  bulkDeleteRecords: (ids) => api.post('/api/compliance/bulk-delete', { ids }),
  bulkDeleteAudits:  (ids) => api.post('/api/audits/bulk-delete', { ids }),
  // Audits
  createAudit: (data) => api.post('/api/audits', data),
  getAudit: (id) => api.get(`/api/audits/${id}`),
  updateFindings: (id, findings) => api.patch(`/api/audits/${id}/findings`, findings),
  closeAudit: (id) => api.patch(`/api/audits/${id}/close`),   // adminId comes from JWT header
  auditsByAdmin: (adminId) => api.get(`/api/audits/admin/${adminId}`),
  auditsPaginated: (page = 0, size = 10) => api.get('/api/audits/paginated', { page, size }),
  deleteAudit: (id) => api.del(`/api/audits/${id}`),
}

export const notifications = {
  // ── Own notifications (works for ALL roles) ───────────────────────────────
  my: () => api.get('/api/notifications/my'),
  myUnread: () => api.get('/api/notifications/my/unread'),
  myCount: () => api.get('/api/notifications/my/count'),
  myMarkAllRead: () => api.patch('/api/notifications/my/read-all'),
  myByCategory: (category) => api.get(`/api/notifications/my/category/${category}`),

  // ── Admin / CLERK lookups (ADMIN + CLERK only) ────────────────────────────
  create: (data) => api.post('/api/notifications', data),
  get: (id) => api.get(`/api/notifications/${id}`),
  byUser: (userId) => api.get(`/api/notifications/user/${userId}`),
  unread: (userId) => api.get(`/api/notifications/user/${userId}/unread`),
  count: (userId) => api.get(`/api/notifications/user/${userId}/count`),
  byCase: (caseId) => api.get(`/api/notifications/case/${caseId}`),
  markRead: (id) => api.patch(`/api/notifications/${id}/read`),
  markAllRead: (userId) => api.patch(`/api/notifications/user/${userId}/read-all`),
  paginated: (page = 0, size = 20, sort = 'notificationId,desc') => api.get('/api/notifications/paginated', { page, size, sort }),
}

export const reports = {
  // requestedBy is read from the JWT/X-Auth-User-Id header on the backend — do NOT send it
  generate: (data) => api.post('/api/reports', data),
  get: (id) => api.get(`/api/reports/${id}`),
  mine: () => api.get('/api/reports/me'),
  byAdmin: (adminId) => api.get(`/api/reports/admin/${adminId}`),
  byScope: (scope) => api.get(`/api/reports/scope/${scope}`),
  byClerk: (clerkId) => api.get(`/api/reports/clerk/${clerkId}`),
  byLawyer: (lawyerId) => api.get(`/api/reports/lawyer/${lawyerId}`),
  paginated: (page = 0, size = 10, sort = 'reportId,desc') => api.get('/api/reports/paginated', { page, size, sort }),
  del: (id) => api.del(`/api/reports/${id}`),
}
