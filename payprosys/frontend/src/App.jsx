import { Routes, Route, Navigate } from 'react-router-dom';
import Layout from './Layout';
import LoginPage from './pages/LoginPage';
import DashboardPage from './pages/DashboardPage';
import BanksPage from './pages/BanksPage';
import CorporatesPage from './pages/CorporatesPage';
import UsersPage from './pages/UsersPage';
import PayrollUploadPage from './pages/PayrollUploadPage';
import EmployeePaymentsPage from './pages/EmployeePaymentsPage';
import { getUser } from './api';

function PrivateRoute({ children }) {
  const token = localStorage.getItem('token');
  if (!token) return <Navigate to="/login" replace />;
  return children;
}

function PayrollLanding() {
  const u = getUser();
  const roles = u?.roles ?? [];
  const isCorp = roles.includes('CORP_ADMIN') || roles.includes('CORP_USER');
  const isBankUser = roles.includes('BANK_ADMIN') || roles.includes('BANK_USER');
  if (isCorp) return <Navigate to="/payroll/upload" replace />;
  if (isBankUser) return <Navigate to="/payroll/employee-payments" replace />;
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
        <Route path="payroll/upload" element={<PayrollUploadPage />} />
        <Route path="payroll/employee-payments" element={<EmployeePaymentsPage />} />
        <Route path="payroll" element={<PayrollLanding />} />
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
