import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { getUser } from './api';

/** Super admin: no payroll. CORP_ADMIN / BANK_ADMIN: workflow settings only (no inbox, batches, etc.). */
export default function PayrollGuard() {
  const u = getUser();
  const roles = u?.roles ?? [];
  const loc = useLocation();

  if (roles.includes('SUPER_ADMIN')) {
    return <Navigate to="/" replace />;
  }

  const isCorp = roles.includes('CORP_ADMIN') || roles.includes('CORP_USER');
  const isBank = roles.includes('BANK_ADMIN') || roles.includes('BANK_USER');
  if (!isCorp && !isBank) {
    return <Navigate to="/" replace />;
  }

  const payrollAdminOnly = roles.includes('CORP_ADMIN') || roles.includes('BANK_ADMIN');
  const onWorkflowSettings =
    loc.pathname === '/payroll/workflow-settings' || loc.pathname.endsWith('/payroll/workflow-settings');

  if (payrollAdminOnly) {
    if (onWorkflowSettings) return <Outlet />;
    return <Navigate to="/payroll/workflow-settings" replace />;
  }

  return <Outlet />;
}
