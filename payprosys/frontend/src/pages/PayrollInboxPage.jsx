import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../api';
import { getUser } from '../api';

function formatYearMonth(ym) {
  if (ym == null) return '-';
  const s = String(ym);
  if (s.length === 6) return `${s.slice(0, 4)}-${s.slice(4)}`;
  return s;
}

export default function PayrollInboxPage() {
  const user = getUser();
  const roles = user?.roles ?? [];
  const isCorp = roles.includes('CORP_ADMIN') || roles.includes('CORP_USER');
  const isBank = roles.includes('BANK_ADMIN') || roles.includes('BANK_USER');
  const navigate = useNavigate();
  const [items, setItems] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!isCorp && !isBank) {
      navigate('/', { replace: true });
      return;
    }
    (async () => {
      setLoading(true);
      setError('');
      try {
        const { data } = await api.get('/payroll/workflow/inbox');
        if (data?.success && data?.data) setItems(data.data);
        else setItems([]);
      } catch (e) {
        setError(e.response?.data?.message ?? 'Failed to load inbox');
        setItems([]);
      } finally {
        setLoading(false);
      }
    })();
  }, [isCorp, isBank, navigate]);

  if (!isCorp && !isBank) return null;

  return (
    <div className="card">
      <h2>Inbox</h2>
      <p className="mb-2 text-muted">
        Batches that need your action; corporate batches you already passed or that sit below your step after a send-back (view only); after send-to-bank, the same batch for all corporate users (view only) until the bank finishes; and for banks, batches awaiting corporate clarification after a bank send-back (view only). Open a row for the timeline; actions are hidden when you are not the current step owner.
      </p>
      {error && <p className="error-msg">{error}</p>}
      {loading ? (
        <p>Loading…</p>
      ) : items.length === 0 ? (
        <p>No items in your inbox.</p>
      ) : (
        <table>
          <thead>
            <tr>
              <th>Month</th>
              <th>Corporate</th>
              <th>File</th>
              <th>Status &amp; workflow</th>
              <th />
            </tr>
          </thead>
          <tbody>
            {items.map((b) => (
              <tr key={b.id}>
                <td>{formatYearMonth(b.yearMonth)}</td>
                <td>{b.corporateName ?? '—'}</td>
                <td>{b.fileName}</td>
                <td>{b.workflowListSummary ?? b.batchStatusLabel ?? b.batchStatus ?? '—'}</td>
                <td>
                  <Link to={`/payroll/batches/${b.id}`}>{b.viewOnly ? 'View' : 'Take Action'}</Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
}
