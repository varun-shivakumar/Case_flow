import { BrowserRouter as Router, Routes, Route } from 'react-router-dom'
import { AuthProvider } from './context/AuthContext'
import { ThemeProvider } from './context/ThemeContext'
import Navbar from './components/Navbar'
import Footer from './components/Footer'
import ProtectedRoute from './components/ProtectedRoute'
import AppLayout from './components/AppLayout'

import Home from './pages/Home'
import About from './pages/About'
import HowItWorks from './pages/HowItWorks'
import Contact from './pages/Contact'

import Login from './pages/auth/Login'
import Register from './pages/auth/Register'
import ForgotPassword from './pages/auth/ForgotPassword'
import ResetPassword from './pages/auth/ResetPassword'
import ChangePassword from './pages/auth/ChangePassword'

import Dashboard from './pages/Dashboard'

import CaseList from './pages/cases/CaseList'
import CaseDetail from './pages/cases/CaseDetail'
import FileCase from './pages/cases/FileCase'
import PendingDocuments from './pages/cases/PendingDocuments'

import HearingList from './pages/hearings/HearingList'
import HearingDetail from './pages/hearings/HearingDetail'
import ScheduleHearing from './pages/hearings/ScheduleHearing'

import WorkflowDashboard from './pages/workflow/WorkflowDashboard'
import CaseWorkflow from './pages/workflow/CaseWorkflow'
import SlaMonitoring from './pages/workflow/SlaMonitoring'

import AppealList from './pages/appeals/AppealList'
import FileAppeal from './pages/appeals/FileAppeal'
import AppealDetail from './pages/appeals/AppealDetail'
import MyReviews from './pages/appeals/MyReviews'
import ReviewsByJudge from './pages/appeals/ReviewsByJudge'

import ComplianceList from './pages/compliance/ComplianceList'
import ComplianceRecordDetail from './pages/compliance/ComplianceRecordDetail'
import ComplianceRunDetail from './pages/compliance/ComplianceRunDetail'
import RunComplianceCheck from './pages/compliance/RunComplianceCheck'
import AuditList from './pages/compliance/AuditList'

import NotificationList from './pages/notifications/NotificationList'
import CreateNotification from './pages/notifications/CreateNotification'

import ReportList from './pages/reports/ReportList'

import UserList from './pages/users/UserList'
import AuditLogs from './pages/users/AuditLogs'

function PublicLayout({ children }) {
  return (
    <>
      <Navbar />
      {children}
      <Footer />
    </>
  )
}

function App() {
  return (
    <ThemeProvider>
    <AuthProvider>
      <Router>
        <Routes>
          {/* Public */}
          <Route path="/"             element={<PublicLayout><Home /></PublicLayout>} />
          <Route path="/about"        element={<PublicLayout><About /></PublicLayout>} />
          <Route path="/how-it-works" element={<PublicLayout><HowItWorks /></PublicLayout>} />
          <Route path="/contact"      element={<PublicLayout><Contact /></PublicLayout>} />

          {/* Auth */}
          <Route path="/login"           element={<Login />} />
          <Route path="/register"        element={<Register />} />
          <Route path="/forgot-password" element={<ForgotPassword />} />
          <Route path="/reset-password"  element={<ResetPassword />} />

          {/* Authenticated app */}
          <Route element={<ProtectedRoute><AppLayout /></ProtectedRoute>}>
            <Route path="/dashboard"       element={<Dashboard />} />
            <Route path="/change-password" element={<ChangePassword />} />

            <Route path="/cases"                   element={<CaseList />} />
            <Route path="/cases/file"              element={<ProtectedRoute roles={['LITIGANT','LAWYER','CLERK']}><FileCase /></ProtectedRoute>} />
            <Route path="/cases/documents/pending" element={<ProtectedRoute roles={['CLERK','ADMIN']}><PendingDocuments /></ProtectedRoute>} />
            <Route path="/cases/:caseId"           element={<CaseDetail />} />

            <Route path="/hearings"             element={<HearingList />} />
            <Route path="/hearings/schedule"    element={<ProtectedRoute roles={['CLERK','JUDGE','ADMIN']}><ScheduleHearing /></ProtectedRoute>} />
            <Route path="/hearings/:hearingId"  element={<HearingDetail />} />

            <Route path="/workflow"          element={<WorkflowDashboard />} />
            <Route path="/workflow/sla"      element={<ProtectedRoute roles={['ADMIN','CLERK']}><SlaMonitoring /></ProtectedRoute>} />
            <Route path="/workflow/:caseId"  element={<ProtectedRoute roles={['ADMIN','CLERK']}><CaseWorkflow /></ProtectedRoute>} />

            <Route path="/appeals"                  element={<AppealList />} />
            <Route path="/appeals/file"             element={<ProtectedRoute roles={['LITIGANT','LAWYER','ADMIN']}><FileAppeal /></ProtectedRoute>} />
            <Route path="/appeals/reviews/my"       element={<ProtectedRoute roles={['JUDGE','ADMIN']}><MyReviews /></ProtectedRoute>} />
            <Route path="/appeals/reviews/judge"    element={<ProtectedRoute roles={['JUDGE','CLERK','ADMIN']}><ReviewsByJudge /></ProtectedRoute>} />
            <Route path="/appeals/:appealId"        element={<AppealDetail />} />

            <Route path="/compliance/check"                 element={<ProtectedRoute roles={['ADMIN','CLERK']}><RunComplianceCheck /></ProtectedRoute>} />
            <Route path="/compliance/runs/:runId"           element={<ProtectedRoute roles={['ADMIN','CLERK']}><ComplianceRunDetail /></ProtectedRoute>} />
            <Route path="/compliance/records/:complianceId" element={<ProtectedRoute roles={['ADMIN','CLERK']}><ComplianceRecordDetail /></ProtectedRoute>} />
            <Route path="/compliance"                       element={<ProtectedRoute roles={['ADMIN','CLERK']}><ComplianceList /></ProtectedRoute>} />
            <Route path="/audits"                           element={<ProtectedRoute roles={['ADMIN','CLERK']}><AuditList /></ProtectedRoute>} />

            <Route path="/notifications"        element={<NotificationList />} />
            <Route path="/notifications/create" element={<ProtectedRoute roles={['ADMIN','CLERK']}><CreateNotification /></ProtectedRoute>} />

            <Route path="/reports" element={<ProtectedRoute roles={['ADMIN','CLERK','LAWYER']}><ReportList /></ProtectedRoute>} />

            <Route path="/users"                       element={<ProtectedRoute roles={['ADMIN']}><UserList /></ProtectedRoute>} />
            <Route path="/users/audit-logs"            element={<ProtectedRoute roles={['ADMIN']}><AuditLogs /></ProtectedRoute>} />
            <Route path="/users/audit-logs/:userId"    element={<ProtectedRoute roles={['ADMIN']}><AuditLogs /></ProtectedRoute>} />
          </Route>
        </Routes>
      </Router>
    </AuthProvider>
    </ThemeProvider>
  )
}

export default App
