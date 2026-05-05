import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api, { setAuthToken, setUser } from '../api';

export default function LoginPage() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const navigate = useNavigate();

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const cleanEmail = email.trim();
      const cleanPassword = password.trim();
      const { data } = await api.post('/auth/login', {
        email: cleanEmail,
        password: cleanPassword,
      });
      if (data?.success && data?.data) {
        setAuthToken(data.data.token);
        setUser({
          userId: data.data.userId ?? null,
          email: data.data.email,
          roles: data.data.roles ?? [],
          bankId: data.data.bankId ?? null,
          corporateId: data.data.corporateId ?? null,
          bankName: data.data.bankName ?? null,
          corporateName: data.data.corporateName ?? null,
          bankLogoUrl: data.data.bankLogoUrl ?? null,
          corporateLogoUrl: data.data.corporateLogoUrl ?? null,
        });
        navigate('/', { replace: true });
      } else {
        setError(data?.message ?? 'Login failed');
      }
    } catch (err) {
      const body = err.response?.data;
      const msg =
        (typeof body === 'object' && body?.message) ||
        (typeof body === 'string' && body) ||
        err.message;
      setError(
        msg ||
          `Could not reach the server (HTTP ${err.response?.status ?? '—'}). Open DevTools → Network and confirm POST /api/auth/login hits the API.`
      );
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="login-page">
      <div className="login-box">
        <h1>PayProSys</h1>
        <form onSubmit={handleSubmit}>
          <div className="form-group">
            <label htmlFor="email">Email</label>
            <input
              id="email"
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              required
              autoComplete="email"
            />
          </div>
          <div className="form-group">
            <label htmlFor="password">Password</label>
            <input
              id="password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              required
              autoComplete="current-password"
            />
          </div>
          {error && <p className="error-msg">{error}</p>}
          <button type="submit" disabled={loading}>
            {loading ? 'Signing in...' : 'Sign in'}
          </button>
        </form>
      </div>
    </div>
  );
}
