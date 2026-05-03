import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import { getUser } from '../api';

function toYearMonth(year, month) {
  const m = String(month).padStart(2, '0');
  return `${year}${m}`;
}

function formatYearMonth(ym) {
  if (ym == null) return '-';
  const s = String(ym);
  if (s.length === 6) return `${s.slice(0, 4)}-${s.slice(4)}`;
  return s;
}

export default function PayrollUploadPage() {
  const user = getUser();
  const roles = user?.roles ?? [];
  const corporateId = user?.corporateId ?? null;
  const isCorp = roles.includes('CORP_ADMIN') || roles.includes('CORP_USER');
  const navigate = useNavigate();

  const currentYear = new Date().getFullYear();
  const [year, setYear] = useState(currentYear);
  const [month, setMonth] = useState('');
  const [file, setFile] = useState(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [summary, setSummary] = useState(null);
  const [batches, setBatches] = useState([]);
  const [busyId, setBusyId] = useState(null);

  useEffect(() => {
    if (!isCorp) {
      navigate('/', { replace: true });
    }
  }, [isCorp, navigate]);

  useEffect(() => {
    if (corporateId) {
      loadBatches();
    }
  }, [corporateId]);

  const loadBatches = async () => {
    if (!corporateId) return;
    try {
      const { data } = await api.get('/payroll/batches', { params: { corporateId } });
      if (data?.success && data?.data) setBatches(data.data);
    } catch (e) {
      setBatches([]);
    }
  };

  const handleSubmitBatch = async (batchId) => {
    const ok = window.confirm('Submit this batch? Banks will see it after submission.');
    if (!ok) return;
    setBusyId(batchId);
    try {
      const { data } = await api.post(`/payroll/batches/${batchId}/submit`);
      if (data?.success) {
        setError('');
        await loadBatches();
      } else setError(data?.message ?? 'Submit failed');
    } catch (e) {
      setError(e.response?.data?.message ?? 'Submit failed');
    } finally {
      setBusyId(null);
    }
  };

  const handleDeleteBatch = async (batchId) => {
    const ok = window.confirm('Delete this pending batch permanently?');
    if (!ok) return;
    setBusyId(batchId);
    try {
      const { data } = await api.delete(`/payroll/batches/${batchId}`);
      if (data?.success) {
        setError('');
        await loadBatches();
      } else setError(data?.message ?? 'Delete failed');
    } catch (e) {
      setError(e.response?.data?.message ?? 'Delete failed');
    } finally {
      setBusyId(null);
    }
  };

  const handleUpload = async (e) => {
    e.preventDefault();
    const monthStr = month && month.length === 6 ? month : (month ? month.replace('-', '') : null);
    if (!corporateId || !file) {
      setError('Choose a month and an Excel file.');
      return;
    }
    if (!monthStr || monthStr.length !== 6) {
      setError('Select a valid month.');
      return;
    }
    setError('');
    setSummary(null);
    setLoading(true);
    const formData = new FormData();
    formData.append('file', file);
    formData.append('month', monthStr);
    try {
      const { data } = await api.post('/payroll/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      });
      if (data?.success && data?.data) {
        setSummary(data.data);
        setFile(null);
        setMonth('');
        loadBatches();
      } else {
        setError(data?.message ?? 'Upload failed');
      }
    } catch (e) {
      setError(e.response?.data?.message ?? 'Upload failed');
    } finally {
      setLoading(false);
    }
  };

  const years = (() => {
    const y = new Date().getFullYear();
    return [y, y - 1, y - 2];
  })();
  const months = Array.from({ length: 12 }, (_, i) => i + 1);

  if (!isCorp) return null;

  const isPending = (b) => (b.batchStatus || '').toUpperCase() === 'PENDING';

  return (
    <>
      <div className="card">
        <h2>Upload payroll (Excel)</h2>
        <p className="mb-2">
          Select the month this payroll is for, then upload a .xlsx file with columns: <strong>Employee Name</strong>, <strong>Account Number</strong>, <strong>Joining Date</strong>, <strong>CPR ID</strong>, <strong>Amount</strong>, <strong>Payment for</strong> (e.g. salary, reimbursement). New uploads start as <strong>PENDING</strong>; use <strong>Employee Payments</strong> to review submitted vs pending batches.
        </p>
        <form onSubmit={handleUpload}>
          <div className="form-row">
            <div className="form-group">
              <label>Month (for this payroll)</label>
              <div className="flex gap-2">
                <select value={year} onChange={(e) => { setYear(Number(e.target.value)); setMonth(''); }}>
                  {years.map((y) => (
                    <option key={y} value={y}>{y}</option>
                  ))}
                </select>
                <select value={month} onChange={(e) => setMonth(e.target.value)}>
                  <option value="">-- Month --</option>
                  {months.map((m) => (
                    <option key={m} value={toYearMonth(year, m)}>
                      {new Date(2000, m - 1, 1).toLocaleString('default', { month: 'long' })}
                    </option>
                  ))}
                </select>
              </div>
              <small className="text-muted">e.g. March 2025 → 2025-03</small>
            </div>
          </div>
          <div className="form-group">
            <label>Excel file (.xlsx)</label>
            <input
              type="file"
              accept=".xlsx"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
            />
          </div>
          {error && <p className="error-msg">{error}</p>}
          <button type="submit" disabled={loading}>
            {loading ? 'Uploading...' : 'Upload'}
          </button>
        </form>
      </div>
      {summary && (
        <div className="card">
          <h2>Upload summary</h2>
          <p><strong>Month:</strong> {formatYearMonth(summary.yearMonth)}</p>
          <p><strong>Status:</strong> {summary.batchStatus ?? 'PENDING'}</p>
          <p><strong>Total records:</strong> {summary.totalRecords}</p>
          <p><strong>Total amount:</strong> {summary.totalAmount != null ? Number(summary.totalAmount).toLocaleString() : '-'}</p>
          <p><strong>File:</strong> {summary.fileName}</p>
        </div>
      )}
      <div className="card">
        <h2>Batches</h2>
        <p className="mb-2">Submit when ready for your bank to see the batch; only pending batches can be deleted.</p>
        {batches.length === 0 ? (
          <p>No uploads yet.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Month</th>
                <th>Status</th>
                <th>File</th>
                <th>Records</th>
                <th>Total amount</th>
                <th>Created</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {batches.map((b) => (
                <tr key={b.id}>
                  <td>{formatYearMonth(b.yearMonth)}</td>
                  <td>{b.batchStatus ?? '-'}</td>
                  <td>{b.fileName}</td>
                  <td>{b.totalRecords}</td>
                  <td>{b.totalAmount != null ? Number(b.totalAmount).toLocaleString() : '-'}</td>
                  <td>{b.createdAt ? new Date(b.createdAt).toLocaleString() : '-'}</td>
                  <td>
                    {isPending(b) ? (
                      <>
                        <button
                          type="button"
                          className="btn-inline"
                          disabled={busyId === b.id}
                          onClick={() => handleSubmitBatch(b.id)}
                        >
                          {busyId === b.id ? '…' : 'Submit'}
                        </button>
                        <button
                          type="button"
                          className="btn-inline btn-danger-soft"
                          disabled={busyId === b.id}
                          onClick={() => handleDeleteBatch(b.id)}
                        >
                          Delete
                        </button>
                      </>
                    ) : (
                      <span style={{ color: 'var(--text-secondary)' }}>—</span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>
    </>
  );
}
