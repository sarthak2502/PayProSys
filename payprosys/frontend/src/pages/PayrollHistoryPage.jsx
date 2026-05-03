import { useEffect, useMemo, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import api from '../api';
import { getUser } from '../api';

function formatYearMonth(ym) {
  if (ym == null) return '-';
  const s = String(ym);
  if (s.length === 6) return `${s.slice(0, 4)}-${s.slice(4)}`;
  return s;
}

export default function PayrollHistoryPage() {
  const user = getUser();
  const roles = user?.roles ?? [];
  const isCorp = roles.includes('CORP_ADMIN') || roles.includes('CORP_USER');
  const isBank = roles.includes('BANK_ADMIN') || roles.includes('BANK_USER');
  const navigate = useNavigate();
  const [items, setItems] = useState([]);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(true);
  const [monthKey, setMonthKey] = useState('');
  const [corporateIdFilter, setCorporateIdFilter] = useState('');

  useEffect(() => {
    if (!isCorp && !isBank) {
      navigate('/', { replace: true });
      return;
    }
    (async () => {
      setLoading(true);
      setError('');
      try {
        const { data } = await api.get('/payroll/workflow/history');
        if (data?.success && data?.data) setItems(data.data);
        else setItems([]);
      } catch (e) {
        setError(e.response?.data?.message ?? 'Failed to load history');
        setItems([]);
      } finally {
        setLoading(false);
      }
    })();
  }, [isCorp, isBank, navigate]);

  const monthChoices = useMemo(() => {
    const ys = new Set((items || []).map((b) => b.yearMonth).filter((ym) => ym != null));
    return Array.from(ys).sort((a, b) => b - a);
  }, [items]);

  const corporateChoices = useMemo(() => {
    const m = new Map();
    (items || []).forEach((b) => {
      if (b.corporateId && !m.has(String(b.corporateId))) {
        m.set(String(b.corporateId), b.corporateName ?? String(b.corporateId));
      }
    });
    return Array.from(m.entries()).sort((a, b) => (a[1] || '').localeCompare(b[1] || '', undefined, { sensitivity: 'base' }));
  }, [items]);

  const filteredItems = useMemo(() => {
    let list = items ?? [];
    if (monthKey) {
      const ym = Number(monthKey);
      list = list.filter((b) => b.yearMonth === ym);
    }
    if (isBank && corporateIdFilter) {
      list = list.filter((b) => String(b.corporateId || '') === corporateIdFilter);
    }
    return list;
  }, [items, monthKey, corporateIdFilter, isBank]);

  if (!isCorp && !isBank) return null;

  return (
    <div className="card history-page">
      <div className="history-page-header">
        <h2>History</h2>
        <p className="mb-2 text-muted">
          Closed batches: corporate rejected, or bank marked <strong>process payment</strong>.
        </p>
      </div>

      <div className="history-toolbar">
        <div className="history-toolbar-fields">
          <div className="history-field">
            <label htmlFor="hist-month">Month</label>
            <select
              id="hist-month"
              value={monthKey}
              onChange={(e) => setMonthKey(e.target.value)}
              className="history-select"
            >
              <option value="">All months</option>
              {monthChoices.map((ym) => (
                <option key={ym} value={String(ym)}>
                  {formatYearMonth(ym)}
                </option>
              ))}
            </select>
          </div>
          {isBank && (
            <div className="history-field history-field--wide">
              <label htmlFor="hist-corp">Corporate</label>
              <select
                id="hist-corp"
                value={corporateIdFilter}
                onChange={(e) => setCorporateIdFilter(e.target.value)}
                className="history-select"
              >
                <option value="">All corporates</option>
                {corporateChoices.map(([id, name]) => (
                  <option key={id} value={id}>
                    {name}
                  </option>
                ))}
              </select>
            </div>
          )}
        </div>
      </div>

      {error && <p className="error-msg">{error}</p>}
      {loading ? (
        <p>Loading…</p>
      ) : items.length === 0 ? (
        <p>No history yet.</p>
      ) : filteredItems.length === 0 ? (
        <p>No rows match the filters ({items.length} in history).</p>
      ) : (
        <div className="table-scroll">
          <table className="history-table">
            <thead>
              <tr>
                <th>Month</th>
                {isBank && <th>Corporate</th>}
                <th>File</th>
                <th>Status &amp; workflow</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {filteredItems.map((b) => (
                <tr key={b.id}>
                  <td>{formatYearMonth(b.yearMonth)}</td>
                  {isBank && <td>{b.corporateName ?? '—'}</td>}
                  <td>{b.fileName}</td>
                  <td>{b.workflowListSummary ?? b.batchStatusLabel ?? b.batchStatus ?? '—'}</td>
                  <td>
                    <Link to={`/payroll/batches/${b.id}`}>View</Link>
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
