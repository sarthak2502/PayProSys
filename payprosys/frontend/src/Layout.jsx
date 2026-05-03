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
          email: data.data.principal,
          roles: data.data.authorities ?? [],
          bankId: data.data.bankId ?? null,
          corporateId: data.data.corporateId ?? null,
          bankName: data.data.bankName ?? null,
          corporateName: data.data.corporateName ?? null,
        };
        setUser(u);
        setUserState(u);
      }
    }).catch(() => {});
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
        <nav>
          <NavLink to="/" end className={({ isActive }) => isActive ? 'active' : ''}>Dashboard</NavLink>
          {isSuperAdmin && <NavLink to="/banks" className={({ isActive }) => isActive ? 'active' : ''}>Banks</NavLink>}
          {isBankUser && <NavLink to="/corporates" className={({ isActive }) => isActive ? 'active' : ''}>Corporates</NavLink>}
          {(isSuperAdmin || isBankUser || isCorpUser) && <NavLink to="/users" className={({ isActive }) => isActive ? 'active' : ''}>Users</NavLink>}
          {isCorpUser && <NavLink to="/payroll/upload" className={({ isActive }) => isActive ? 'active' : ''}>Payroll Upload</NavLink>}
          {(isBankUser || isCorpUser) && (
            <NavLink to="/payroll/employee-payments" className={({ isActive }) => isActive ? 'active' : ''}>Employee Payments</NavLink>
          )}
        </nav>
      </aside>
      <div className="main-content">
        <header className="header">
            <h1>
            PayProSys
            {userDisplay?.bankName && <span className="header-context"> (Bank: {userDisplay.bankName})</span>}
            {userDisplay?.corporateName && !userDisplay?.bankName && <span className="header-context"> (Corporate: {userDisplay.corporateName})</span>}
          </h1>
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
