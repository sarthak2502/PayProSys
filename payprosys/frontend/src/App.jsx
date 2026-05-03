import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from './Layout';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import BanksPage from './pages/BanksPage';
import CorporatesPage from './pages/CorporatesPage';
import UsersPage from './pages/UsersPage';
import PayrollUploadPage from './pages/PayrollUploadPage';
import EmployeePaymentsPage from './pages/EmployeePaymentsPage';
import PayrollInboxPage from './pages/PayrollInboxPage';
import PayrollHistoryPage from './pages/PayrollHistoryPage';
import PayrollBatchDetailPage from './pages/PayrollBatchDetailPage';
import PayrollWorkflowSettingsPage from './pages/PayrollWorkflowSettingsPage';
import ProfilePage from './pages/ProfilePage';
import PayrollGuard from './PayrollGuard';
import { getUser } from './api';

function PrivateRoute({ children }) {
  const token = localStorage.getItem('token');
  if (!token) return <Navigate to="/login" replace />;
  return children;
}

function PayrollLanding() {
  const u = getUser();
  const roles = u?.roles ?? [];
  if (roles.includes('SUPER_ADMIN')) return <Navigate to="/" replace />;
  if (roles.includes('CORP_ADMIN') || roles.includes('BANK_ADMIN')) {
    return <Navigate to="/payroll/workflow-settings" replace />;
  }
  const isCorp = roles.includes('CORP_ADMIN') || roles.includes('CORP_USER');
  const isBankUser = roles.includes('BANK_ADMIN') || roles.includes('BANK_USER');
  if (isCorp || isBankUser) return <Navigate to="/payroll/inbox" replace />;
  return <Navigate to="/" replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/"
        element={
          <PrivateRoute>
            <Layout />
          </PrivateRoute>
        }
      >
        <Route index element={<DashboardPage />} />
        <Route path="banks" element={<BanksPage />} />
        <Route path="corporates" element={<CorporatesPage />} />
        <Route path="users" element={<UsersPage />} />
        <Route path="profile" element={<ProfilePage />} />
        <Route element={<PayrollGuard />}>
          <Route path="payroll/upload" element={<PayrollUploadPage />} />
          <Route path="payroll/inbox" element={<PayrollInboxPage />} />
          <Route path="payroll/history" element={<PayrollHistoryPage />} />
          <Route path="payroll/batches/:id" element={<PayrollBatchDetailPage />} />
          <Route path="payroll/workflow-settings" element={<PayrollWorkflowSettingsPage />} />
          <Route path="payroll/employee-payments" element={<EmployeePaymentsPage />} />
          <Route path="payroll" element={<PayrollLanding />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
