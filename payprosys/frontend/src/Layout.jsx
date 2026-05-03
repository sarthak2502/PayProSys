import { useState, useEffect } from 'react';
import { Outlet, NavLink, useNavigate } from 'react-router-dom';
import api from './api';
import { getUser, setAuthToken, setUser } from './api';

export default function Layout() {
  const [dropdownOpen, setDropdownOpen] = useState(false);
  const [user, setUserState] = useState(getUser());

  useEffect(() => {
    const token = localStorage.getItem('token');
    if (!token) return;
    api.get('/auth/whoami').then(({ data }) => {
      if (data?.success && data?.data) {
        const u = {
          userId: data.data.userId ?? null,
          email: data.data.principal,
          roles: data.data.authorities ?? [],
          bankId: data.data.bankId ?? null,
          corporateId: data.data.corporateId ?? null,
          bankName: data.data.bankName ?? null,
          corporateName: data.data.corporateName ?? null,
          bankLogoUrl: data.data.bankLogoUrl ?? null,
          corporateLogoUrl: data.data.corporateLogoUrl ?? null,
        };
        setUser(u);
        setUserState(u);
      }
    }).catch(() => {});
  }, []);

  useEffect(() => {
    const syncFromStorage = () => setUserState(getUser());
    window.addEventListener('payprosys-user-storage', syncFromStorage);
    return () => window.removeEventListener('payprosys-user-storage', syncFromStorage);
  }, []);

  const userDisplay = user ?? getUser();
  const roles = userDisplay?.roles ?? [];
  const isSuperAdmin = roles.includes('SUPER_ADMIN');
  const isBankUser = roles.includes('BANK_ADMIN') || roles.includes('BANK_USER');
  const isCorpUser = roles.includes('CORP_ADMIN') || roles.includes('CORP_USER');
  const navigate = useNavigate();

  const handleLogout = () => {
    setAuthToken(null);
    localStorage.removeItem('user');
    setDropdownOpen(false);
    navigate('/login');
  };

  return (
    <div className="app-layout">
      <aside className="sidebar">
        <div className="sidebar-brand">
          <img src="/logo.svg" alt="PayProSys" className="sidebar-brand-logo" width={120} height={32} />
        </div>
        <nav>
          <NavLink to="/" end className={({ isActive }) => isActive ? 'active' : ''}>Dashboard</NavLink>
          {isSuperAdmin && <NavLink to="/banks" className={({ isActive }) => isActive ? 'active' : ''}>Banks</NavLink>}
          {isBankUser && <NavLink to="/corporates" className={({ isActive }) => isActive ? 'active' : ''}>Corporates</NavLink>}
          {(isSuperAdmin || isBankUser || isCorpUser) && <NavLink to="/users" className={({ isActive }) => isActive ? 'active' : ''}>Users</NavLink>}
          {(isCorpUser || isBankUser) && !isSuperAdmin && !roles.includes('CORP_ADMIN') && !roles.includes('BANK_ADMIN') && (
            <NavLink to="/payroll/inbox" className={({ isActive }) => isActive ? 'active' : ''}>Inbox</NavLink>
          )}
          {(isCorpUser || isBankUser) && !isSuperAdmin && !roles.includes('CORP_ADMIN') && !roles.includes('BANK_ADMIN') && (
            <NavLink to="/payroll/history" className={({ isActive }) => isActive ? 'active' : ''}>History</NavLink>
          )}
          {isCorpUser && !isSuperAdmin && !roles.includes('CORP_ADMIN') && (
            <NavLink to="/payroll/upload" className={({ isActive }) => isActive ? 'active' : ''}>Upload</NavLink>
          )}
          {(isBankUser || isCorpUser) && !isSuperAdmin && !roles.includes('CORP_ADMIN') && !roles.includes('BANK_ADMIN') && (
            <NavLink to="/payroll/employee-payments" className={({ isActive }) => isActive ? 'active' : ''}>Payments</NavLink>
          )}
          {(roles.includes('CORP_ADMIN') || roles.includes('BANK_ADMIN')) && !isSuperAdmin && (
            <NavLink to="/payroll/workflow-settings" className={({ isActive }) => isActive ? 'active' : ''}>Workflow settings</NavLink>
          )}
          <NavLink to="/profile" className={({ isActive }) => isActive ? 'active' : ''}>Profile</NavLink>
        </nav>
      </aside>
      <div className="main-content">
        <header className="header">
          <div className="header-title-cluster">
            {userDisplay?.bankLogoUrl ? (
              <img src={userDisplay.bankLogoUrl} alt="" className="header-org-logo" />
            ) : null}
            {userDisplay?.corporateLogoUrl ? (
              <img src={userDisplay.corporateLogoUrl} alt="" className="header-org-logo" />
            ) : null}
            <h1>
              PayProSys
              {userDisplay?.bankName && <span className="header-context"> · {userDisplay.bankName}</span>}
              {userDisplay?.corporateName && !userDisplay?.bankName && (
                <span className="header-context"> · {userDisplay.corporateName}</span>
              )}
            </h1>
          </div>
          <div className="header-actions">
            <span className="notification-placeholder" title="Notifications" />
            <div className="user-dropdown">
              <button
                type="button"
                className="user-dropdown-trigger"
                onClick={() => setDropdownOpen(!dropdownOpen)}
              >
                {userDisplay?.email ?? 'User'}
                <span>▼</span>
              </button>
              {dropdownOpen && (
                <div className="user-dropdown-menu">
                  <button type="button" onClick={() => setDropdownOpen(false)}>
                    {userDisplay?.email}
                  </button>
                  <button
                    type="button"
                    onClick={() => {
                      setDropdownOpen(false);
                      navigate('/profile');
                    }}
                  >
                    Profile
                  </button>
                  <button type="button" onClick={handleLogout}>
                    Logout
                  </button>
                </div>
              )}
            </div>
          </div>
        </header>
        <main className="page-content">
          <Outlet />
        </main>
      </div>
    </div>
  );
}
