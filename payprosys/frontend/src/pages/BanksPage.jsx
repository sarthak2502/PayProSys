import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import { getUser } from '../api';

export default function BanksPage() {
  const user = getUser();
  const roles = user?.roles ?? [];
  const isSuperAdmin = roles.includes('SUPER_ADMIN');
  const navigate = useNavigate();

  const [banks, setBanks] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(true); // show form by default so bank admin fields are visible
  const [name, setName] = useState('');
  const [adminEmail, setAdminEmail] = useState('');
  const [adminPassword, setAdminPassword] = useState('');
  const [adminFirstName, setAdminFirstName] = useState('');
  const [adminLastName, setAdminLastName] = useState('');
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [createdCredentials, setCreatedCredentials] = useState(null);

  useEffect(() => {
    if (!isSuperAdmin) {
      navigate('/', { replace: true });
      return;
    }
    loadBanks();
  }, [isSuperAdmin, navigate]);

  const loadBanks = async () => {
    try {
      const { data } = await api.get('/banks');
      if (data?.success && data?.data) setBanks(data.data);
    } catch (e) {
      setError(e.response?.data?.message ?? 'Failed to load banks');
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    setCreatedCredentials(null);
    try {
      const { data } = await api.post('/banks', {
        name,
        adminEmail,
        adminPassword,
        adminFirstName,
        adminLastName,
      });
      if (data?.success && data?.data) {
        setMessage('Bank created. Share these login details with the bank admin:');
        setCreatedCredentials(data.data);
        setName('');
        setAdminEmail('');
        setAdminPassword('');
        setAdminFirstName('');
        setAdminLastName('');
        setShowForm(false);
        loadBanks();
      }
    } catch (e) {
      setError(e.response?.data?.message ?? 'Failed to create bank');
    }
  };

  if (!isSuperAdmin) return null;

  return (
    <>
      <div className="card flex justify-between items-center">
        <h2>Banks</h2>
        <button type="button" onClick={() => setShowForm(!showForm)}>
          {showForm ? 'Cancel' : 'Add bank'}
        </button>
      </div>
      {showForm && (
        <div className="card">
          <h2>Add bank</h2>
          <p className="text-muted">Enter bank name and the first bank admin’s login details (email, password, first name, last name). These credentials can be shared with the bank admin.</p>
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label>Bank name</label>
              <input value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. Acme Bank" required />
            </div>
            <div className="form-group">
              <label>Bank admin email</label>
              <input type="email" value={adminEmail} onChange={(e) => setAdminEmail(e.target.value)} placeholder="e.g. admin@acmebank.com" required />
            </div>
            <div className="form-group">
              <label>Bank admin password</label>
              <input type="text" value={adminPassword} onChange={(e) => setAdminPassword(e.target.value)} placeholder="Login password for bank admin" required />
            </div>
            <div className="form-group">
              <label>Bank admin first name</label>
              <input value={adminFirstName} onChange={(e) => setAdminFirstName(e.target.value)} placeholder="e.g. John" required />
            </div>
            <div className="form-group">
              <label>Bank admin last name</label>
              <input value={adminLastName} onChange={(e) => setAdminLastName(e.target.value)} placeholder="e.g. Doe" required />
            </div>
            {error && <p className="error-msg">{error}</p>}
            {message && <p className="success-msg">{message}</p>}
            <button type="submit">Create</button>
          </form>
        </div>
      )}
      {createdCredentials && (
        <div className="card highlight">
          <h3>Bank admin login (share with bank admin)</h3>
          <p><strong>Bank:</strong> {createdCredentials.bank?.name}</p>
          <p><strong>Email:</strong> {createdCredentials.adminEmail}</p>
          <p><strong>Password:</strong> {createdCredentials.adminPassword}</p>
          <p><strong>Name:</strong> {createdCredentials.adminFirstName} {createdCredentials.adminLastName}</p>
        </div>
      )}
      <div className="card">
        <h3>List of banks (with admin login details)</h3>
        {loading ? (
          <p>Loading...</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Bank name</th>
                <th>Admin email</th>
                <th>Admin password</th>
                <th>Admin name</th>
                <th>Created</th>
              </tr>
            </thead>
            <tbody>
              {banks.map((b) => (
                <tr key={b.id}>
                  <td>{b.name}</td>
                  <td>{b.adminEmail ?? '-'}</td>
                  <td>{b.adminPassword ?? '-'}</td>
                  <td>{b.adminFirstName && b.adminLastName ? `${b.adminFirstName} ${b.adminLastName}` : '-'}</td>
                  <td>{b.createdAt ? new Date(b.createdAt).toLocaleString() : '-'}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
        {!loading && banks.length === 0 && <p>No banks yet.</p>}
      </div>
    </>
  );
}
