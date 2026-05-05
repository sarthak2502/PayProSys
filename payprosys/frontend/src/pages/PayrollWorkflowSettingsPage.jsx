import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import { getUser } from '../api';

export default function PayrollWorkflowSettingsPage() {
  const user = getUser();
  const roles = user?.roles ?? [];
  const isCorpAdmin = roles.includes('CORP_ADMIN');
  const isBankAdmin = roles.includes('BANK_ADMIN');
  const corporateId = user?.corporateId ?? null;
  const bankId = user?.bankId ?? null;
  const navigate = useNavigate();

  const [corpSteps, setCorpSteps] = useState([]);
  const [corpAssignments, setCorpAssignments] = useState([]);
  const [bankSteps, setBankSteps] = useState([]);
  const [bankAssignments, setBankAssignments] = useState([]);
  const [corpUsers, setCorpUsers] = useState([]);
  const [bankUsers, setBankUsers] = useState([]);
  const [corpUserId, setCorpUserId] = useState('');
  const [corpLevel, setCorpLevel] = useState(1);
  const [bankUserId, setBankUserId] = useState('');
  const [bankLevel, setBankLevel] = useState(1);
  const [error, setError] = useState('');
  const [msg, setMsg] = useState('');
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    if (!isCorpAdmin && !isBankAdmin) {
      navigate('/', { replace: true });
    }
  }, [isCorpAdmin, isBankAdmin, navigate]);

  useEffect(() => {
    (async () => {
      setError('');
      try {
        if (isCorpAdmin && corporateId) {
          const [cfg, users] = await Promise.all([
            api.get('/payroll/workflow/config/corporate'),
            api.get('/users/by-corporate', { params: { corporateId } }),
          ]);
          if (cfg.data?.success) {
            setCorpSteps(cfg.data.data?.steps ?? []);
            setCorpAssignments(cfg.data.data?.assignments ?? []);
          }
          if (users.data?.success) setCorpUsers(users.data.data ?? []);
        }
        if (isBankAdmin && bankId) {
          const [cfg, users] = await Promise.all([
            api.get('/payroll/workflow/config/bank'),
            api.get('/users/by-bank', { params: { bankId } }),
          ]);
          if (cfg.data?.success) {
            setBankSteps(cfg.data.data?.steps ?? []);
            setBankAssignments(cfg.data.data?.assignments ?? []);
          }
          if (users.data?.success) setBankUsers(users.data.data ?? []);
        }
      } catch (e) {
        setError(e.response?.data?.message ?? 'Failed to load workflow config');
      }
    })();
  }, [isCorpAdmin, isBankAdmin, corporateId, bankId]);

  const saveCorpSteps = async () => {
    setBusy(true);
    setMsg('');
    setError('');
    try {
      const body = corpSteps.map((s) => ({ stepLevel: s.stepLevel, label: s.label }));
      const { data } = await api.put('/payroll/workflow/config/corporate/steps', body);
      if (data?.success) setMsg('Corporate step labels saved.');
      else setError(data?.message ?? 'Save failed');
    } catch (e) {
      setError(e.response?.data?.message ?? 'Save failed');
    } finally {
      setBusy(false);
    }
  };

  const saveBankSteps = async () => {
    setBusy(true);
    setMsg('');
    setError('');
    try {
      const body = bankSteps.map((s) => ({ stepLevel: s.stepLevel, label: s.label }));
      const { data } = await api.put('/payroll/workflow/config/bank/steps', body);
      if (data?.success) setMsg('Bank step labels saved.');
      else setError(data?.message ?? 'Save failed');
    } catch (e) {
      setError(e.response?.data?.message ?? 'Save failed');
    } finally {
      setBusy(false);
    }
  };

  const addCorpAssignment = async () => {
    if (!corpUserId) return;
    setBusy(true);
    setError('');
    try {
      const { data } = await api.post('/payroll/workflow/config/corporate/assignments', {
        userId: corpUserId,
        reviewLevel: Number(corpLevel),
      });
      if (data?.success && data.data) {
        setCorpAssignments((prev) => [...prev, data.data]);
        setMsg('Assignment added.');
        setCorpUserId('');
      } else setError(data?.message ?? 'Failed');
    } catch (e) {
      setError(e.response?.data?.message ?? 'Failed');
    } finally {
      setBusy(false);
    }
  };

  const removeCorpAssignment = async (assignmentId) => {
    if (!window.confirm('Remove this assignment?')) return;
    setBusy(true);
    setError('');
    try {
      const { data } = await api.delete(`/payroll/workflow/config/corporate/assignments/${assignmentId}`);
      if (data?.success) {
        setCorpAssignments((prev) => prev.filter((a) => a.id !== assignmentId));
        setMsg('Removed.');
      }
    } catch (e) {
      setError(e.response?.data?.message ?? 'Failed');
    } finally {
      setBusy(false);
    }
  };

  const addBankAssignment = async () => {
    if (!bankUserId) return;
    setBusy(true);
    setError('');
    try {
      const { data } = await api.post('/payroll/workflow/config/bank/assignments', {
        userId: bankUserId,
        reviewLevel: Number(bankLevel),
      });
      if (data?.success && data.data) {
        setBankAssignments((prev) => [...prev, data.data]);
        setMsg('Assignment added.');
        setBankUserId('');
      } else setError(data?.message ?? 'Failed');
    } catch (e) {
      setError(e.response?.data?.message ?? 'Failed');
    } finally {
      setBusy(false);
    }
  };

  const removeBankAssignment = async (assignmentId) => {
    if (!window.confirm('Remove this assignment?')) return;
    setBusy(true);
    setError('');
    try {
      const { data } = await api.delete(`/payroll/workflow/config/bank/assignments/${assignmentId}`);
      if (data?.success) {
        setBankAssignments((prev) => prev.filter((a) => a.id !== assignmentId));
        setMsg('Removed.');
      }
    } catch (e) {
      setError(e.response?.data?.message ?? 'Failed');
    } finally {
      setBusy(false);
    }
  };

  const updateCorpLabel = (stepLevel, label) => {
    setCorpSteps((prev) => prev.map((s) => (s.stepLevel === stepLevel ? { ...s, label } : s)));
  };

  const updateBankLabel = (stepLevel, label) => {
    setBankSteps((prev) => prev.map((s) => (s.stepLevel === stepLevel ? { ...s, label } : s)));
  };

  if (!isCorpAdmin && !isBankAdmin) return null;

  return (
    <div>
      <div className="card">
        <h2>Workflow settings</h2>
        <p className="mb-2 text-muted">
          Edit display labels for review levels (L1…Ln) and assign users to levels. If no assignments exist, any corporate or bank user on the tenant may act at the current step (POC fallback).
        </p>
        {error && <p className="error-msg">{error}</p>}
        {msg && <p className="success-msg">{msg}</p>}
      </div>

      {isCorpAdmin && corporateId && (
        <div className="card">
          <h3>Corporate workflow</h3>
          <table className="mb-2">
            <thead>
              <tr>
                <th>Level</th>
                <th>Label</th>
              </tr>
            </thead>
            <tbody>
              {corpSteps.map((s) => (
                <tr key={s.id}>
                  <td>L{s.stepLevel}</td>
                  <td>
                    <input
                      type="text"
                      value={s.label}
                      onChange={(e) => updateCorpLabel(s.stepLevel, e.target.value)}
                      style={{ width: '100%', minWidth: '200px' }}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <button type="button" disabled={busy} onClick={saveCorpSteps}>
            Save corporate labels
          </button>

          <h4 className="mt-2">User ↔ level</h4>
          <div className="flex gap-2 flex-wrap items-center mb-2">
            <select value={corpUserId} onChange={(e) => setCorpUserId(e.target.value)}>
              <option value="">— User —</option>
              {corpUsers.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.email} ({u.roles?.join(', ')})
                </option>
              ))}
            </select>
            <select value={corpLevel} onChange={(e) => setCorpLevel(Number(e.target.value))}>
              {corpSteps.map((s) => (
                <option key={s.stepLevel} value={s.stepLevel}>
                  L{s.stepLevel}
                </option>
              ))}
            </select>
            <button type="button" disabled={busy} onClick={addCorpAssignment}>
              Add assignment
            </button>
          </div>
          <table>
            <thead>
              <tr>
                <th>Email</th>
                <th>Level</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {corpAssignments.map((a) => (
                <tr key={a.id}>
                  <td>{a.userEmail}</td>
                  <td>L{a.reviewLevel}</td>
                  <td>
                    <button type="button" className="btn-danger-soft btn-inline" disabled={busy} onClick={() => removeCorpAssignment(a.id)}>
                      Remove
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {isBankAdmin && bankId && (
        <div className="card">
          <h3>Bank workflow</h3>
          <table className="mb-2">
            <thead>
              <tr>
                <th>Level</th>
                <th>Label</th>
              </tr>
            </thead>
            <tbody>
              {bankSteps.map((s) => (
                <tr key={s.id}>
                  <td>L{s.stepLevel}</td>
                  <td>
                    <input
                      type="text"
                      value={s.label}
                      onChange={(e) => updateBankLabel(s.stepLevel, e.target.value)}
                      style={{ width: '100%', minWidth: '200px' }}
                    />
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
          <button type="button" disabled={busy} onClick={saveBankSteps}>
            Save bank labels
          </button>

          <h4 className="mt-2">User ↔ level</h4>
          <div className="flex gap-2 flex-wrap items-center mb-2">
            <select value={bankUserId} onChange={(e) => setBankUserId(e.target.value)}>
              <option value="">— User —</option>
              {bankUsers.map((u) => (
                <option key={u.id} value={u.id}>
                  {u.email} ({u.roles?.join(', ')})
                </option>
              ))}
            </select>
            <select value={bankLevel} onChange={(e) => setBankLevel(Number(e.target.value))}>
              {bankSteps.map((s) => (
                <option key={s.stepLevel} value={s.stepLevel}>
                  L{s.stepLevel}
                </option>
              ))}
            </select>
            <button type="button" disabled={busy} onClick={addBankAssignment}>
              Add assignment
            </button>
          </div>
          <table>
            <thead>
              <tr>
                <th>Email</th>
                <th>Level</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {bankAssignments.map((a) => (
                <tr key={a.id}>
                  <td>{a.userEmail}</td>
                  <td>L{a.reviewLevel}</td>
                  <td>
                    <button type="button" className="btn-danger-soft btn-inline" disabled={busy} onClick={() => removeBankAssignment(a.id)}>
                      Remove
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
