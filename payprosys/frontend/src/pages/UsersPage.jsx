import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import { getUser } from '../api';

export default function UsersPage() {
  const user = getUser();
  const roles = user?.roles ?? [];
  const bankId = user?.bankId ?? null;
  const corporateId = user?.corporateId ?? null;
  const isSuperAdmin = roles.includes('SUPER_ADMIN');
  const isBankAdmin = roles.includes('BANK_ADMIN');
  const isBankUser = roles.includes('BANK_USER');
  const isCorpAdmin = roles.includes('CORP_ADMIN');
  const isCorpUser = roles.includes('CORP_USER');
  const isBank = isBankAdmin || isBankUser;
  const isCorp = isCorpAdmin || isCorpUser;
  const navigate = useNavigate();

  const [users, setUsers] = useState([]);
  const [rolesList, setRolesList] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState({
    firstName: '',
    lastName: '',
    email: '',
    password: '',
    roleIds: [],
  });
  const [error, setError] = useState('');
  const [message, setMessage] = useState('');

  useEffect(() => {
    if (!isSuperAdmin && !isBank && !isCorp) {
      navigate('/', { replace: true });
      return;
    }
    if (!isSuperAdmin) loadRoles();
  }, [isSuperAdmin, isBank, isCorp, navigate]);

  useEffect(() => {
    if (isSuperAdmin) loadAllUsers();
    else if (isBank && bankId) loadUsersByBank();
    else if (isCorp && corporateId) loadUsersByCorporate();
    else setUsers([]);
  }, [isSuperAdmin, isBank, isCorp, bankId, corporateId]);

  const loadRoles = async () => {
    try {
      const { data } = await api.get('/roles');
      if (data?.success && data?.data) {
        const all = data.data;
        const bankRoles = all.filter((r) => r.name === 'BANK_ADMIN' || r.name === 'BANK_USER');
        const corpRoles = all.filter((r) => r.name === 'CORP_ADMIN' || r.name === 'CORP_USER');
        setRolesList(isBank ? bankRoles : corpRoles);
      }
    } catch (e) {
      setError(e.response?.data?.message ?? 'Failed to load roles');
    }
  };

  const loadAllUsers = async () => {
    setLoading(true);
    try {
      const { data } = await api.get('/users/all');
      setUsers(data?.success ? data.data : []);
    } catch (e) {
      setError(e.response?.data?.message ?? 'Failed to load users');
      setUsers([]);
    } finally {
      setLoading(false);
    }
  };

  const loadUsersByBank = async () => {
    if (!bankId) return;
    setLoading(true);
    try {
      const { data } = await api.get('/users/by-bank', { params: { bankId } });
      setUsers(data?.success ? data.data : []);
    } catch (e) {
      setError(e.response?.data?.message ?? 'Failed to load users');
      setUsers([]);
    } finally {
      setLoading(false);
    }
  };

  const loadUsersByCorporate = async () => {
    if (!corporateId) return;
    setLoading(true);
    try {
      const { data } = await api.get('/users/by-corporate', { params: { corporateId } });
      setUsers(data?.success ? data.data : []);
    } catch (e) {
      setError(e.response?.data?.message ?? 'Failed to load users');
      setUsers([]);
    } finally {
      setLoading(false);
    }
  };

  const loadUsers = () => {
    if (isSuperAdmin) loadAllUsers();
    else if (isBank && bankId) loadUsersByBank();
    else if (isCorp && corporateId) loadUsersByCorporate();
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setMessage('');
    const roleIds = form.roleIds.length ? form.roleIds : (rolesList.length ? [rolesList[0].id] : []);
    if (!roleIds.length) {
      setError('Select at least one role');
      return;
    }
    try {
      if (isBankAdmin) {
        await api.post('/users/bank', {
          firstName: form.firstName,
          lastName: form.lastName,
          email: form.email,
          password: form.password,
          bankId,
          roleIds,
        });
        setMessage('Bank user created successfully');
      } else if (isCorpAdmin) {
        await api.post('/users/corporate-user', {
          firstName: form.firstName,
          lastName: form.lastName,
          email: form.email,
          password: form.password,
          corporateId,
          roleIds,
        });
        setMessage('Corporate user created successfully');
      }
      setForm({ firstName: '', lastName: '', email: '', password: '', roleIds: [] });
      setShowForm(false);
      loadUsers();
    } catch (e) {
      setError(e.response?.data?.message ?? 'Failed to create user');
    }
  };

  const toggleRole = (id) => {
    setForm((f) => ({
      ...f,
      roleIds: f.roleIds.includes(id) ? f.roleIds.filter((r) => r !== id) : [...f.roleIds, id],
    }));
  };

  const canCreateUser = (isBankAdmin || isCorpAdmin) && !isSuperAdmin;
  const contextLabel = isSuperAdmin ? 'All' : (isBank ? 'Bank' : 'Corporate');

  if (!isSuperAdmin && !isBank && !isCorp) return null;

  return (
    <>
      <div className="card flex justify-between items-center">
        <h2>Users ({contextLabel})</h2>
        {canCreateUser && (
          <button type="button" onClick={() => setShowForm(!showForm)}>
            {showForm ? 'Cancel' : 'Create user'}
          </button>
        )}
      </div>
      {showForm && canCreateUser && (
        <div className="card">
          <h2>Create {isBankAdmin ? 'bank' : 'corporate'} user</h2>
          <form onSubmit={handleSubmit}>
            <div className="form-row">
              <div className="form-group">
                <label>First name</label>
                <input value={form.firstName} onChange={(e) => setForm((f) => ({ ...f, firstName: e.target.value }))} required />
              </div>
              <div className="form-group">
                <label>Last name</label>
                <input value={form.lastName} onChange={(e) => setForm((f) => ({ ...f, lastName: e.target.value }))} required />
              </div>
            </div>
            <div className="form-group">
              <label>Email</label>
              <input type="email" value={form.email} onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))} required />
            </div>
            <div className="form-group">
              <label>Password</label>
              <input type="password" value={form.password} onChange={(e) => setForm((f) => ({ ...f, password: e.target.value }))} required minLength={8} />
            </div>
            <div className="form-group">
              <label>Roles</label>
              <div className="flex gap-2">
                {rolesList.map((r) => (
                  <label key={r.id}>
                    <input
                      type="checkbox"
                      checked={form.roleIds.includes(r.id)}
                      onChange={() => toggleRole(r.id)}
                    />
                    {r.name}
                  </label>
                ))}
              </div>
            </div>
            {error && <p className="error-msg">{error}</p>}
            {message && <p className="success-msg">{message}</p>}
            <button type="submit">Create</button>
          </form>
        </div>
      )}
      <div className="card">
        {loading ? (
          <p>Loading...</p>
        ) : (
          <>
            <table>
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Password</th>
                  <th>Status</th>
                  <th>Roles</th>
                  {isSuperAdmin && <th>Bank</th>}
                  {isSuperAdmin && <th>Corporate</th>}
                </tr>
              </thead>
              <tbody>
                {users.map((u) => (
                  <tr key={u.id}>
                    <td>{u.firstName} {u.lastName}</td>
                    <td>{u.email}</td>
                    <td>{isSuperAdmin ? (u.password ?? '-') : '—'}</td>
                    <td>{u.status}</td>
                    <td>{u.roles?.join(', ') ?? '-'}</td>
                    {isSuperAdmin && <td>{u.bankName ?? '-'}</td>}
                    {isSuperAdmin && <td>{u.corporateName ?? '-'}</td>}
                  </tr>
                ))}
              </tbody>
            </table>
            {!loading && users.length === 0 && <p>No users{isSuperAdmin ? '' : ' in this ' + contextLabel.toLowerCase()}.</p>}
          </>
        )}
      </div>
    </>
  );
}
