import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
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

function isPending(b) {
  return (b.batchStatus || '').toUpperCase() === 'PENDING';
}

function isRejected(b) {
  return (b.corporateFlowState || '').toUpperCase() === 'CORP_REJECTED';
}

function isDraftBatch(b) {
  return (
    isPending(b) &&
    (b.corporateFlowState || '').toUpperCase() === 'CORP_NEW' &&
    (b.currentCorporateReviewLevel == null || b.currentCorporateReviewLevel === undefined)
  );
}

export default function PayrollUploadPage() {
  const user = getUser();
  const roles = user?.roles ?? [];
  const corporateId = user?.corporateId ?? null;
  const isCorp = roles.includes('CORP_ADMIN') || roles.includes('CORP_USER');
  const isCorpAdmin = roles.includes('CORP_ADMIN');
  const canUpload = roles.includes('CORP_USER') && !isCorpAdmin;
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
  const [paymentBatchKind, setPaymentBatchKind] = useState('PAYROLL');

  const [viewerOpen, setViewerOpen] = useState(false);
  const [viewerBatch, setViewerBatch] = useState(null);
  const [viewerRecords, setViewerRecords] = useState([]);
  const [viewerLoading, setViewerLoading] = useState(false);

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

  const openViewer = async (batch) => {
    setError('');
    setViewerBatch(batch);
    setViewerOpen(true);
    setViewerRecords([]);
    setViewerLoading(true);
    try {
      const { data } = await api.get('/payroll/records/by-batch', { params: { batchId: batch.id } });
      if (data?.success && data?.data) setViewerRecords(data.data);
      else setViewerRecords([]);
    } catch (e) {
      setViewerRecords([]);
    } finally {
      setViewerLoading(false);
    }
  };

  const closeViewer = () => {
    setViewerOpen(false);
    setViewerBatch(null);
    setViewerRecords([]);
  };

  const handleSubmitBatch = async (batch) => {
    const draft = isDraftBatch(batch);
    if (!draft) {
      setError('Submit is only available for draft batches. After submission, use Inbox (and Send to bank from there when on approved hold).');
      return;
    }
    const msg = 'Submit this batch for corporate review? It will appear in the payroll inbox at L1.';
    if (!window.confirm(msg)) return;
    setBusyId(batch.id);
    try {
      const { data } = await api.post(`/payroll/batches/${batch.id}/submit`);
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
    const ok = window.confirm('Delete this draft batch permanently?');
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
      setError('Choose year, month, and an Excel file.');
      return;
    }
    if (!monthStr || monthStr.length !== 6) {
      setError('Select a valid month for the chosen year.');
      return;
    }
    setError('');
    setSummary(null);
    setLoading(true);
    const formData = new FormData();
    formData.append('file', file);
    formData.append('month', monthStr);
    formData.append('paymentBatchKind', paymentBatchKind);
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

  const showSubmit = (b) => !isRejected(b) && isDraftBatch(b);
  const showDelete = (b) => !isRejected(b) && isDraftBatch(b);

  return (
    <>
      {!!error && (
        <div className="card">
          <p className="error-msg">{error}</p>
        </div>
      )}
      {canUpload && (
        <div className="card">
          <h2>Upload (Excel)</h2>
          <p className="mb-2">
            Choose <strong>year</strong> and <strong>month</strong>, then pick batch type and your .xlsx file. Columns:{' '}
            <strong>Employee Name</strong>, <strong>Account Number</strong>, <strong>Joining Date</strong>,{' '}
            <strong>CPR ID</strong>, <strong>Amount</strong>, <strong>Payment for</strong>. New batches stay <strong>drafts</strong>{' '}
            until you <strong>Submit</strong> below; then they appear in <Link to="/payroll/inbox">Inbox</Link>. Send to bank
            from <Link to="/payroll/inbox">Inbox</Link> when on approved hold.
          </p>
          <form onSubmit={handleUpload} className="upload-payroll-form">
            <div className="form-group upload-field-year">
              <label htmlFor="upload-year">Year</label>
              <select id="upload-year" value={year} onChange={(e) => { setYear(Number(e.target.value)); setMonth(''); }}>
                {years.map((y) => (
                  <option key={y} value={y}>{y}</option>
                ))}
              </select>
            </div>
            <div className="form-group upload-field-month">
              <label htmlFor="upload-month">Month</label>
              <select id="upload-month" value={month} onChange={(e) => setMonth(e.target.value)}>
                <option value="">— Select month —</option>
                {months.map((m) => (
                  <option key={m} value={toYearMonth(year, m)}>
                    {new Date(2000, m - 1, 1).toLocaleString('default', { month: 'long' })}
                  </option>
                ))}
              </select>
              <small className="text-muted">Payroll period is year + month (e.g. March {year}).</small>
            </div>
            <div className="upload-actions-row">
              <div className="form-group upload-field-kind">
                <label htmlFor="upload-kind">Batch type</label>
                <select id="upload-kind" value={paymentBatchKind} onChange={(e) => setPaymentBatchKind(e.target.value)}>
                  <option value="PAYROLL">Payroll payment</option>
                  <option value="VENDOR">Vendor payment</option>
                </select>
              </div>
              <div className="form-group upload-field-file">
                <label htmlFor="upload-file">File (.xlsx)</label>
                <input
                  id="upload-file"
                  type="file"
                  accept=".xlsx"
                  onChange={(e) => setFile(e.target.files?.[0] ?? null)}
                />
              </div>
              <div className="upload-submit-wrap">
                <button type="submit" disabled={loading}>
                  {loading ? 'Uploading…' : 'Upload'}
                </button>
              </div>
            </div>
          </form>
        </div>
      )}
      {isCorpAdmin && !canUpload && (
        <div className="card">
          <h2>Payroll batches</h2>
          <p className="mb-2 text-muted">Corporate admins cannot upload files. Open a batch with <strong>View</strong> to see lines (read-only).</p>
        </div>
      )}
      {summary && canUpload && (
        <div className="card">
          <h2>Upload summary</h2>
          <p><strong>Month:</strong> {formatYearMonth(summary.yearMonth)}</p>
          <p>
            <strong>Batch type:</strong>{' '}
            {summary.paymentBatchKind === 'VENDOR' ? 'Vendor payment' : 'Payroll payment'}
          </p>
          <p><strong>Status:</strong> {summary.batchStatus ?? 'PENDING'}</p>
          <p><strong>Total records:</strong> {summary.totalRecords}</p>
          <p><strong>Total amount:</strong> {summary.totalAmount != null ? Number(summary.totalAmount).toLocaleString() : '-'}</p>
          <p><strong>File:</strong> {summary.fileName}</p>
        </div>
      )}
      <div className="card">
        <h2>Batches (drafts only)</h2>
        <p className="mb-2">
          Only <strong>draft</strong> uploads appear here. In-flight, completed, and rejected batches are in{' '}
          <Link to="/payroll/inbox">Inbox</Link> or <Link to="/payroll/history">History</Link>. Use <strong>Submit</strong> to send a draft for L1 review.
          <strong> View</strong> opens payroll lines read-only.
        </p>
        {batches.filter((b) => isDraftBatch(b)).length === 0 ? (
          <p>No draft batches.</p>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Month</th>
                <th>Type</th>
                <th>Status</th>
                <th>File</th>
                <th>Records</th>
                <th>Total amount</th>
                <th>Created</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {batches.filter((b) => isDraftBatch(b)).map((b) => (
                <tr key={b.id}>
                  <td>{formatYearMonth(b.yearMonth)}</td>
                  <td>{b.paymentBatchKindLabel ?? b.paymentBatchKind ?? '—'}</td>
                  <td>{b.batchStatusLabel ?? b.batchStatus ?? '-'}</td>
                  <td>{b.fileName}</td>
                  <td>{b.totalRecords}</td>
                  <td>{b.totalAmount != null ? Number(b.totalAmount).toLocaleString() : '-'}</td>
                  <td>{b.createdAt ? new Date(b.createdAt).toLocaleString() : '-'}</td>
                  <td>
                    <button type="button" className="btn-inline" style={{ marginRight: '0.5rem' }} onClick={() => openViewer(b)}>
                      View
                    </button>
                    {showSubmit(b) ? (
                      <button
                        type="button"
                        className="btn-inline"
                        disabled={busyId === b.id}
                        onClick={() => handleSubmitBatch(b)}
                      >
                        {busyId === b.id ? '…' : 'Submit'}
                      </button>
                    ) : null}
                    {showSubmit(b) && showDelete(b) ? <span style={{ margin: '0 0.25rem' }} /> : null}
                    {showDelete(b) ? (
                      <button
                        type="button"
                        className="btn-inline btn-danger-soft"
                        disabled={busyId === b.id}
                        onClick={() => handleDeleteBatch(b.id)}
                      >
                        Delete
                      </button>
                    ) : null}
                    {!showSubmit(b) && !showDelete(b) ? (
                      <span style={{ color: 'var(--text-secondary)' }}>—</span>
                    ) : null}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {viewerOpen && (
        <div className="modal-overlay" role="presentation" onClick={closeViewer}>
          <div className="modal-panel card" role="dialog" aria-modal="true" onClick={(e) => e.stopPropagation()}>
            <div className="modal-head">
              <h3 style={{ margin: 0 }}>
                Batch lines — {viewerBatch?.fileName}
              </h3>
              <button type="button" className="modal-close" onClick={closeViewer} aria-label="Close">
                ×
              </button>
            </div>
            <p style={{ color: 'var(--text-secondary)', marginBottom: '0.75rem' }}>
              Month {formatYearMonth(viewerBatch?.yearMonth)} · {viewerRecords.length} record(s) · read-only
            </p>
            {viewerLoading ? (
              <p>Loading…</p>
            ) : viewerRecords.length === 0 ? (
              <p>No records.</p>
            ) : (
              <div className="table-scroll">
                <table>
                  <thead>
                    <tr>
                      <th>Employee name</th>
                      <th>Account number</th>
                      <th>Joining date</th>
                      <th>CPR ID</th>
                      <th>Amount</th>
                      <th>Payment for</th>
                    </tr>
                  </thead>
                  <tbody>
                    {viewerRecords.map((r) => (
                      <tr key={r.id}>
                        <td>{r.employeeName}</td>
                        <td>{r.accountNumber}</td>
                        <td>{r.joiningDate ?? '—'}</td>
                        <td>{r.cprId}</td>
                        <td>{r.amount != null ? Number(r.amount).toLocaleString() : '—'}</td>
                        <td>{r.paymentFor ?? '—'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <div className="modal-footer-actions">
              <button type="button" className="secondary" onClick={closeViewer}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
