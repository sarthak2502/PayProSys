import { useEffect, useState } from 'react';
import api, { getUser, setUser } from '../api';

export default function ProfilePage() {
  const [org, setOrg] = useState(null);
  const [logoInput, setLogoInput] = useState('');
  const [brandingMsg, setBrandingMsg] = useState('');
  const [brandingBusy, setBrandingBusy] = useState(false);

  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [pwMsg, setPwMsg] = useState('');
  const [pwBusy, setPwBusy] = useState(false);

  const user = getUser();
  const hasOrg = Boolean(user?.bankId || user?.corporateId);

  const loadOrg = async () => {
    if (!hasOrg) {
      setOrg(null);
      return;
    }
    try {
      const { data } = await api.get('/profile/organization');
      if (data?.success && data?.data) {
        setOrg(data.data);
        setLogoInput(data.data.logoUrl ?? '');
      }
    } catch {
      setOrg(null);
    }
  };

  useEffect(() => {
    loadOrg();
  }, [hasOrg]);

  const saveBranding = async (e) => {
    e.preventDefault();
    setBrandingMsg('');
    setBrandingBusy(true);
    try {
      const { data } = await api.patch('/profile/organization/branding', { logoUrl: logoInput });
      if (data?.success) {
        setBrandingMsg('Saved.');
        if (data?.data) setOrg(data.data);
        const u = getUser();
        if (u) {
          const next = { ...u };
          if (data.data?.organizationType === 'BANK') next.bankLogoUrl = data.data.logoUrl ?? null;
          if (data.data?.organizationType === 'CORPORATE') next.corporateLogoUrl = data.data.logoUrl ?? null;
          setUser(next);
          window.dispatchEvent(new Event('payprosys-user-storage'));
        }
      } else setBrandingMsg(data?.message ?? 'Save failed');
    } catch (err) {
      setBrandingMsg(err.response?.data?.message ?? 'Save failed');
    } finally {
      setBrandingBusy(false);
    }
  };

  const changePassword = async (e) => {
    e.preventDefault();
    setPwMsg('');
    setPwBusy(true);
    try {
      const { data } = await api.post('/profile/change-password', {
        currentPassword,
        newPassword,
      });
      if (data?.success) {
        setPwMsg('Password updated.');
        setCurrentPassword('');
        setNewPassword('');
      } else setPwMsg(data?.message ?? 'Update failed');
    } catch (err) {
      setPwMsg(err.response?.data?.message ?? 'Update failed');
    } finally {
      setPwBusy(false);
    }
  };

  const orgTitle =
    org?.organizationType === 'BANK'
      ? 'Bank profile'
      : org?.organizationType === 'CORPORATE'
        ? 'Corporate profile'
        : 'Profile';

  return (
    <div className="profile-page">
      <div className="card">
        <h2>{orgTitle}</h2>
        <p className="text-muted mb-2">
          Signed in as <strong>{user?.email ?? '—'}</strong>
        </p>

        {hasOrg && org && (
          <div className="mb-3">
            <h3>Branding</h3>
            <p className="text-muted small mb-2">
              Organization: <strong>{org.organizationName ?? '—'}</strong>. Set a public <code>https://</code> image URL
              for your logo (or a small <code>data:image/…</code> data URL). Leave blank to clear.
            </p>
            {org.logoUrl ? (
              <div className="profile-logo-preview mb-2">
                <img src={org.logoUrl} alt="" />
              </div>
            ) : null}
            <form onSubmit={saveBranding}>
              <div className="form-group">
                <label htmlFor="logo-url">Logo URL</label>
                <input
                  id="logo-url"
                  type="text"
                  value={logoInput}
                  onChange={(e) => setLogoInput(e.target.value)}
                  placeholder="https://…"
                  autoComplete="off"
                />
              </div>
              {brandingMsg && <p className={brandingMsg.includes('fail') ? 'error-msg' : 'success-msg'}>{brandingMsg}</p>}
              <button type="submit" disabled={brandingBusy}>
                {brandingBusy ? 'Saving…' : 'Save branding'}
              </button>
            </form>
          </div>
        )}

        {!hasOrg && (
          <p className="text-muted mb-2">Organization branding is available for bank and corporate users.</p>
        )}

        <h3>Password</h3>
        <form onSubmit={changePassword} className="profile-password-form">
          <div className="form-group">
            <label htmlFor="cur-pw">Current password</label>
            <input
              id="cur-pw"
              type="password"
              value={currentPassword}
              onChange={(e) => setCurrentPassword(e.target.value)}
              autoComplete="current-password"
              required
            />
          </div>
          <div className="form-group">
            <label htmlFor="new-pw">New password</label>
            <input
              id="new-pw"
              type="password"
              value={newPassword}
              onChange={(e) => setNewPassword(e.target.value)}
              autoComplete="new-password"
              required
              minLength={6}
            />
          </div>
          {pwMsg && <p className={pwMsg.includes('fail') || pwMsg.includes('incorrect') ? 'error-msg' : 'success-msg'}>{pwMsg}</p>}
          <button type="submit" disabled={pwBusy}>
            {pwBusy ? 'Updating…' : 'Change password'}
          </button>
        </form>
      </div>
    </div>
  );
}
