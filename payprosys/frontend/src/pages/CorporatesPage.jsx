import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import { getUser } from '../api';

export default function CorporatesPage() {
  const user = getUser();
  const roles = user?.roles ?? [];
  const bankId = user?.bankId ?? null;
  const isBankAdmin = roles.includes('BANK_ADMIN');
  const isBankUser = roles.includes('BANK_ADMIN') || roles.includes('BANK_USER');
  const navigate = useNavigate();

  const [corporates, setCorporates] = useState([]);
  const [bankUsers, setBankUsers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [name, setName] = useState('');
  const [adminEmail, setAdminEmail] = useState('');
  const [adminPassword, setAdminPassword] = useState('');
  const [adminFirstName, setAdminFirstName] = useState('');
  const [adminLastName, setAdminLastName] = useState('');
  const [assignedBankUserIds, setAssignedBankUserIds] = useState([]);
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');
  const [createdCredentials, setCreatedCredentials] = useState(null);
  const [manageCorporateId, setManageCorporateId] = useState(null);
  const [assignedUsers, setAssignedUsers] = useState({});
  const [assignUserId, setAssignUserId] = useState('');

  useEffect(() => {
    if (!isBankUser) {
      navigate('/', { replace: true });
      return;
    }
    loadCorporates();
  }, [isBankUser, navigate]);

  useEffect(() => {
    if (isBankAdmin && bankId) {
      api.get('/users/by-bank', { params: { bankId } }).then(({ data }) => {
        if (data?.success && data?.data) setBankUsers(data.data);
      }).catch(() => {});
    }
  }, [isBankAdmin, bankId]);

  const loadCorporates = async () => {
    setLoading(true);
    try {
      const { data } = await api.get('/corporates');
      if (data?.success && data?.data) setCorporates(data.data);
    } catch (e) {
      setError(e.response?.data?.message ?? 'Failed to load corporates');
    } finally {
      setLoading(false);
    }
  };

  const loadAssignedBankUsers = async (corporateId) => {
    try {
      const { data } = await api.get(`/corporates/${corporateId}/bank-users`);
      if (data?.success && data?.data) setAssignedUsers((prev) => ({ ...prev, [corporateId]: data.data }));
} catch (e) {
        setAssignedUsers((prev) => ({ ...prev, [corporateId]: [] }));
      }
  };

  const toggleManage = (corporateId) => {
    if (manageCorporateId === corporateId) {
      setManageCorporateId(null);
      return;
    }
    setManageCorporateId(corporateId);
    loadAssignedBankUsers(corporateId);
  };

  const handleAssign = async (corporateId) => {
    if (!assignUserId) return;
    try {
      await api.post(`/corporates/${corporateId}/bank-users`, { userId: assignUserId });
      setAssignUserId('');
      loadAssignedBankUsers(corporateId);
    } catch (e) {
      setError(e.response?.data?.message ?? 'Failed to assign');
    }
  };

  const handleUnassign = async (corporateId, userId) => {
    try {
      await api.delete(`/corporates/${corporateId}/bank-users/${userId}`);
      loadAssignedBankUsers(corporateId);
    } catch (e) {
      setError(e.response?.data?.message ?? 'Failed to unassign');
    }
  };

  const toggleAssignedBankUser = (id) => {
    setAssignedBankUserIds((prev) => prev.includes(id) ? prev.filter((x) => x !== id) : [...prev, id]);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    setCreatedCredentials(null);
    try {
      const { data } = await api.post('/corporates', {
        name,
        adminEmail,
        adminPassword,
        adminFirstName,
        adminLastName,
        assignedBankUserIds: assignedBankUserIds.length ? assignedBankUserIds : undefined,
      });
      if (data?.success && data?.data) {
        setMessage('Corporate created. Share these login details with the corporate admin:');
        setCreatedCredentials(data.data);
        setName('');
        setAdminEmail('');
        setAdminPassword('');
        setAdminFirstName('');
        setAdminLastName('');
        setAssignedBankUserIds([]);
        setShowForm(false);
        loadCorporates();
      }
    } catch (e) {
      setError(e.response?.data?.message ?? 'Failed to create corporate');
    }
  };

  if (!isBankUser) return null;

  return (
    <>
      <div className="card flex justify-between items-center">
        <h2>Corporates</h2>
        {roles.includes('BANK_ADMIN') && (
          <button type="button" onClick={() => setShowForm(!showForm)}>
            {showForm ? 'Cancel' : 'Add corporate'}
          </button>
        )}
      </div>
      {showForm && (
        <div className="card">
          <h2>Add corporate</h2>
          <p className="text-muted">Create a corporate and its first corporate admin. Share the login details with the corporate admin.</p>
          <form onSubmit={handleSubmit}>
            <div className="form-group">
              <label>Corporate name</label>
              <input value={name} onChange={(e) => setName(e.target.value)} required />
            </div>
            <div className="form-group">
              <label>Corporate admin email</label>
              <input type="email" value={adminEmail} onChange={(e) => setAdminEmail(e.target.value)} required />
            </div>
            <div className="form-group">
              <label>Corporate admin password</label>
              <input type="text" value={adminPassword} onChange={(e) => setAdminPassword(e.target.value)} required />
            </div>
            <div className="form-group">
              <label>Corporate admin first name</label>
              <input value={adminFirstName} onChange={(e) => setAdminFirstName(e.target.value)} required />
            </div>
            <div className="form-group">
              <label>Corporate admin last name</label>
              <input value={adminLastName} onChange={(e) => setAdminLastName(e.target.value)} required />
            </div>
            {isBankAdmin && bankUsers.length > 0 && (
              <div className="form-group">
                <label>Assign bank user(s) to this corporate (optional)</label>
                <div className="flex gap-2 flex-wrap">
                  {bankUsers.map((u) => (
                    <label key={u.id}>
                      <input
                        type="checkbox"
                        checked={assignedBankUserIds.includes(u.id)}
                        onChange={() => toggleAssignedBankUser(u.id)}
                      />
                      {u.firstName} {u.lastName} ({u.email})
                    </label>
                  ))}
                </div>
              </div>
            )}
            {error && <p className="error-msg">{error}</p>}
            {message && <p className="success-msg">{message}</p>}
            <button type="submit">Create</button>
          </form>
        </div>
      )}
      {createdCredentials && (
        <div className="card highlight">
          <h3>Corporate admin login (share with corporate admin)</h3>
          <p><strong>Corporate:</strong> {createdCredentials.corporate?.name}</p>
          <p><strong>Email:</strong> {createdCredentials.adminEmail}</p>
          <p><strong>Password:</strong> {createdCredentials.adminPassword}</p>
          <p><strong>Name:</strong> {createdCredentials.adminFirstName} {createdCredentials.adminLastName}</p>
        </div>
      )}
      <div className="card">
        <h3>List of corporates (with admin login details)</h3>
        {loading ? (
          <p>Loading...</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Corporate name</th>
                <th>Admin email</th>
                <th>Admin password</th>
                <th>Admin name</th>
                <th>Status</th>
                <th>Created</th>
                {isBankAdmin && <th>Bank users</th>}
              </tr>
            </thead>
            <tbody>
              {corporates.map((c) => (
                <React.Fragment key={c.id}>
                  <tr>
                    <td>{c.name}</td>
                    <td>{c.adminEmail ?? '-'}</td>
                    <td>{c.adminPassword ?? '-'}</td>
                    <td>{c.adminFirstName && c.adminLastName ? `${c.adminFirstName} ${c.adminLastName}` : '-'}</td>
                    <td>{c.status ?? '-'}</td>
                    <td>{c.createdAt ? new Date(c.createdAt).toLocaleString() : '-'}</td>
                    {isBankAdmin && (
                      <td>
                        <button type="button" className="link-btn" onClick={() => toggleManage(c.id)}>
                          {manageCorporateId === c.id ? 'Hide' : 'Manage'}
                        </button>
                      </td>
                    )}
                  </tr>
                  {isBankAdmin && manageCorporateId === c.id && (
                    <tr>
                      <td colSpan={isBankAdmin ? 7 : 6}>
                        <div className="card sub-card">
                          <h4>Assigned bank users</h4>
                          <ul>
                            {(assignedUsers[c.id] ?? []).map((u) => (
                              <li key={u.id} className="flex justify-between items-center">
                                <span>{u.firstName} {u.lastName} ({u.email})</span>
                                <button type="button" onClick={() => handleUnassign(c.id, u.id)}>Remove</button>
                              </li>
                            ))}
                          </ul>
                          <div className="flex gap-2 items-center">
                            <select value={assignUserId} onChange={(e) => setAssignUserId(e.target.value)}>
                              <option value="">Select bank user to assign</option>
                              {bankUsers.filter((bu) => !(assignedUsers[c.id] ?? []).some((a) => a.id === bu.id)).map((bu) => (
                                <option key={bu.id} value={bu.id}>{bu.firstName} {bu.lastName} ({bu.email})</option>
                              ))}
                            </select>
                            <button type="button" onClick={() => handleAssign(c.id)} disabled={!assignUserId}>Assign</button>
                          </div>
                        </div>
                      </td>
                    </tr>
                  )}
                </React.Fragment>
              ))}
            </tbody>
          </table>
        )}
        {!loading && corporates.length === 0 && <p>No corporates for your bank yet.</p>}
      </div>
    </>
  );
}
