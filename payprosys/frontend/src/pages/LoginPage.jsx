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
      const { data } = await api.post('/auth/login', { email, password });
      if (data?.success && data?.data) {
        setAuthToken(data.data.token);
        setUser({
          email: data.data.email,
          roles: data.data.roles ?? [],
          bankId: data.data.bankId ?? null,
          corporateId: data.data.corporateId ?? null,
          bankName: data.data.bankName ?? null,
          corporateName: data.data.corporateName ?? null,
        });
        navigate('/', { replace: true });
      } else {
        setError(data?.message ?? 'Login failed');
      }
    } catch (err) {
      setError(err.response?.data?.message ?? 'Invalid email or password');
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
