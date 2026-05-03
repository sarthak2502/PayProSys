import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api from '../api';
import { getUser } from '../api';

function formatYearMonth(ym) {
  if (ym == null) return '-';
  const s = String(ym);
  if (s.length === 6) return `${s.slice(0, 4)}-${s.slice(4)}`;
  return s;
}

function csvEscape(val) {
  if (val == null || val === '') return '';
  const s = String(val);
  if (/[",\n\r]/.test(s)) return `"${s.replace(/"/g, '""')}"`;
  return s;
}

function downloadCsv(filename, headerRow, dataRows) {
  const lines = [headerRow.map(csvEscape).join(',')];
  for (const row of dataRows) {
    lines.push(row.map(csvEscape).join(','));
  }
  const blob = new Blob([lines.join('\r\n')], { type: 'text/csv;charset=utf-8;' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = filename;
  a.click();
  URL.revokeObjectURL(url);
}

export default function EmployeePaymentsPage() {
  const user = getUser();
  const roles = user?.roles ?? [];
  const corporateId = user?.corporateId ?? null;
  const isCorp = roles.includes('CORP_ADMIN') || roles.includes('CORP_USER');
  const isBankUser = roles.includes('BANK_ADMIN') || roles.includes('BANK_USER');
  const navigate = useNavigate();

  const [tab, setTab] = useState('pending');
  const [batches, setBatches] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const [viewerOpen, setViewerOpen] = useState(false);
  const [viewerBatch, setViewerBatch] = useState(null);
  const [viewerRecords, setViewerRecords] = useState([]);
  const [viewerLoading, setViewerLoading] = useState(false);

  useEffect(() => {
    if (!isCorp && !isBankUser) {
      navigate('/', { replace: true });
    }
  }, [isCorp, isBankUser, navigate]);

  useEffect(() => {
    if (isCorp && !corporateId) {
      setBatches([]);
      return;
    }
    if (isCorp && corporateId) {
      loadCorpBatches();
    } else if (isBankUser && !isCorp) {
      loadBankBatches();
    }
  }, [isCorp, isBankUser, corporateId, tab]);

  const loadCorpBatches = async () => {
    setLoading(true);
    setError('');
    try {
      const status = tab === 'pending' ? 'PENDING' : 'SUBMITTED';
      const { data } = await api.get('/payroll/batches', { params: { corporateId, status } });
      if (data?.success && data?.data) setBatches(data.data);
      else setBatches([]);
    } catch (e) {
      setError(e.response?.data?.message ?? 'Failed to load batches');
      setBatches([]);
    } finally {
      setLoading(false);
    }
  };

  const loadBankBatches = async () => {
    setLoading(true);
    setError('');
    try {
      const { data } = await api.get('/payroll/batches/submitted-for-bank');
      if (data?.success && data?.data) setBatches(data.data);
      else setBatches([]);
    } catch (e) {
      setError(e.response?.data?.message ?? 'Failed to load batches');
      setBatches([]);
    } finally {
      setLoading(false);
    }
  };

  const openViewer = async (batch) => {
    setViewerBatch(batch);
    setViewerOpen(true);
    setViewerLoading(true);
    setViewerRecords([]);
    try {
      const { data } = await api.get('/payroll/records/by-batch', { params: { batchId: batch.id } });
      if (data?.success && data?.data) setViewerRecords(data.data);
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

  const exportBatchesCsv = () => {
    if (!batches.length) return;
    const bankCols = isBankOnly;
    const headers = bankCols
      ? ['Corporate', 'Month', 'File', 'Status', 'Records', 'Total amount', 'Created', 'Batch id']
      : ['Month', 'File', 'Status', 'Records', 'Total amount', 'Created', 'Batch id'];
    const rows = batches.map((b) => {
      const base = [
        formatYearMonth(b.yearMonth),
        b.fileName,
        b.batchStatus ?? '',
        b.totalRecords,
        b.totalAmount != null ? b.totalAmount : '',
        b.createdAt ? new Date(b.createdAt).toISOString() : '',
        b.id,
      ];
      return bankCols ? [b.corporateName ?? '', ...base] : base;
    });
    const prefix = isBankOnly ? 'bank-submitted' : `corp-${tab}`;
    downloadCsv(`payprosys-batches-${prefix}.csv`, headers, rows);
  };

  const exportViewerRecordsCsv = () => {
    if (!viewerBatch || !viewerRecords.length) return;
    const headers = [
      'Batch id',
      'Month',
      'File',
      'Employee name',
      'Account number',
      'Joining date',
      'CPR ID',
      'Amount',
      'Payment for',
    ];
    const rows = viewerRecords.map((r) => [
      viewerBatch.id,
      formatYearMonth(viewerBatch.yearMonth),
      viewerBatch.fileName ?? '',
      r.employeeName,
      r.accountNumber,
      r.joiningDate ?? '',
      r.cprId,
      r.amount != null ? r.amount : '',
      r.paymentFor ?? '',
    ]);
    const safe = String(viewerBatch.fileName ?? 'batch').replace(/[^\w.-]+/g, '_').slice(0, 40);
    downloadCsv(`payprosys-payments-${safe}.csv`, headers, rows);
  };

  if (!isCorp && !isBankUser) return null;

  const isBankOnly = isBankUser && !isCorp;

  return (
    <>
      <div className="card">
        <h2>Employee Payments</h2>
        {!isBankOnly ? (
          <p className="mb-2">Pending batches are internal until you submit them. Banks only see submitted batches.</p>
        ) : (
          <p className="mb-2">Submitted payroll batches from corporates linked to your bank.</p>
        )}

        {!isBankOnly && (
          <div className="tabs-row mb-2">
            <button
              type="button"
              className={tab === 'pending' ? 'tab-active' : 'tab-btn'}
              onClick={() => setTab('pending')}
            >
              Pending
            </button>
            <button
              type="button"
              className={tab === 'submitted' ? 'tab-active' : 'tab-btn'}
              onClick={() => setTab('submitted')}
            >
              Submitted
            </button>
          </div>
        )}

        {error && <p className="error-msg">{error}</p>}

        {isCorp && !corporateId && (
          <p className="error-msg">Your user is not linked to a corporate.</p>
        )}

        {!loading && batches.length > 0 && (
          <p className="mb-2">
            <button type="button" className="secondary" onClick={exportBatchesCsv}>
              Download batch list (CSV)
            </button>
          </p>
        )}

        {loading ? (
          <p>Loading...</p>
        ) : batches.length === 0 ? (
          <p>No batches.</p>
        ) : (
          <table>
            <thead>
              <tr>
                {isBankOnly && <th>Corporate</th>}
                <th>Month</th>
                <th>File</th>
                <th>Status</th>
                <th>Records</th>
                <th>Total amount</th>
                <th>Created</th>
                <th />
              </tr>
            </thead>
            <tbody>
              {batches.map((b) => (
                <tr key={b.id}>
                  {isBankOnly && <td>{b.corporateName ?? '-'}</td>}
                  <td>{formatYearMonth(b.yearMonth)}</td>
                  <td>{b.fileName}</td>
                  <td>{b.batchStatus ?? '-'}</td>
                  <td>{b.totalRecords}</td>
                  <td>{b.totalAmount != null ? Number(b.totalAmount).toLocaleString() : '-'}</td>
                  <td>{b.createdAt ? new Date(b.createdAt).toLocaleString() : '-'}</td>
                  <td>
                    <button type="button" className="btn-linkish" onClick={() => openViewer(b)}>
                      View payments
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {viewerOpen && (
        <div
          className="modal-overlay"
          role="presentation"
          onClick={closeViewer}
        >
          <div className="modal-panel card" role="dialog" onClick={(e) => e.stopPropagation()}>
            <div className="modal-head">
              <h3 style={{ margin: 0 }}>
                Payments — {viewerBatch?.fileName} ({viewerBatch?.batchStatus})
              </h3>
              <button type="button" className="modal-close" onClick={closeViewer} aria-label="Close">
                ×
              </button>
            </div>
            <p style={{ color: 'var(--text-secondary)', marginBottom: '0.75rem' }}>
              Month {formatYearMonth(viewerBatch?.yearMonth)} · {viewerRecords.length} record(s)
            </p>
            {viewerLoading ? (
              <p>Loading...</p>
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
                        <td>{r.joiningDate ?? '-'}</td>
                        <td>{r.cprId}</td>
                        <td>{r.amount != null ? Number(r.amount).toLocaleString() : '-'}</td>
                        <td>{r.paymentFor ?? '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
            <div className="modal-footer-actions">
              <div>
                {!viewerLoading && viewerRecords.length > 0 && (
                  <button type="button" className="secondary" onClick={exportViewerRecordsCsv}>
                    Download payments (CSV)
                  </button>
                )}
              </div>
              <button type="button" className="secondary" onClick={closeViewer}>Close</button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}
