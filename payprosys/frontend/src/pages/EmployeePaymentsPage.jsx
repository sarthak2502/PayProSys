import { createPortal } from 'react-dom';
import { useCallback, useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import api, { getUser } from '../api';

function formatYearMonth(ym) {
  if (ym == null) return '-';
  const s = String(ym);
  if (s.length === 6) return `${s.slice(0, 4)}-${s.slice(4)}`;
  return s;
}

function applyFilters(batches, kindFilter, selectedMonths, corporateIdFilter) {
  let list = batches ?? [];
  if (kindFilter === 'PAYROLL') {
    list = list.filter((b) => (b.paymentBatchKind || 'PAYROLL') === 'PAYROLL');
  } else if (kindFilter === 'VENDOR') {
    list = list.filter((b) => (b.paymentBatchKind || '') === 'VENDOR');
  }
  if (corporateIdFilter) {
    list = list.filter((b) => String(b.corporateId || '') === corporateIdFilter);
  }
  if (selectedMonths.size > 0) {
    list = list.filter((b) => b.yearMonth != null && selectedMonths.has(b.yearMonth));
  }
  return list;
}

function monthFilterSummary(selectedMonths) {
  if (selectedMonths.size === 0) return 'All months';
  if (selectedMonths.size === 1) {
    const [only] = [...selectedMonths];
    return formatYearMonth(only);
  }
  return `${selectedMonths.size} months selected`;
}

export default function EmployeePaymentsPage() {
  const user = getUser();
  const roles = user?.roles ?? [];
  const corporateId = user?.corporateId ?? null;
  const isCorp = roles.includes('CORP_ADMIN') || roles.includes('CORP_USER');
  const isBankUser = roles.includes('BANK_ADMIN') || roles.includes('BANK_USER');
  const navigate = useNavigate();

  const [rawBatches, setRawBatches] = useState([]);
  const [kindFilter, setKindFilter] = useState('all');
  const [selectedMonths, setSelectedMonths] = useState(() => new Set());
  const [corporateIdFilter, setCorporateIdFilter] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [monthMenuOpen, setMonthMenuOpen] = useState(false);
  const [monthPanelPos, setMonthPanelPos] = useState(null);
  const monthTriggerRef = useRef(null);
  const monthPanelRef = useRef(null);

  const [viewerOpen, setViewerOpen] = useState(false);
  const [viewerBatch, setViewerBatch] = useState(null);
  const [viewerRecords, setViewerRecords] = useState([]);
  const [viewerLoading, setViewerLoading] = useState(false);
  const [viewerError, setViewerError] = useState('');

  const monthOptions = useMemo(() => {
    const ys = new Set((rawBatches || []).map((b) => b.yearMonth).filter((ym) => ym != null));
    return Array.from(ys).sort((a, b) => b - a);
  }, [rawBatches]);

  const corporateOptions = useMemo(() => {
    const m = new Map();
    (rawBatches || []).forEach((b) => {
      if (b.corporateId && !m.has(String(b.corporateId))) {
        m.set(String(b.corporateId), b.corporateName ?? String(b.corporateId));
      }
    });
    return Array.from(m.entries()).sort((a, b) => (a[1] || '').localeCompare(b[1] || '', undefined, { sensitivity: 'base' }));
  }, [rawBatches]);

  const batches = applyFilters(rawBatches, kindFilter, selectedMonths, corporateIdFilter);

  const updateMonthPanelPosition = useCallback(() => {
    const el = monthTriggerRef.current;
    if (!el || !monthMenuOpen) {
      setMonthPanelPos(null);
      return;
    }
    const r = el.getBoundingClientRect();
    const margin = 8;
    const panelWidth = Math.max(r.width, 240);
    let left = r.left;
    if (left + panelWidth > window.innerWidth - margin) {
      left = Math.max(margin, window.innerWidth - margin - panelWidth);
    }
    const spaceBelow = window.innerHeight - r.bottom - margin;
    const maxHeight = Math.max(140, Math.min(320, spaceBelow));
    setMonthPanelPos({
      top: r.bottom + 6,
      left,
      width: panelWidth,
      maxHeight,
    });
  }, [monthMenuOpen]);

  useLayoutEffect(() => {
    updateMonthPanelPosition();
  }, [updateMonthPanelPosition, monthMenuOpen, monthOptions.length, selectedMonths]);

  useEffect(() => {
    if (!monthMenuOpen) return;
    const on = () => updateMonthPanelPosition();
    window.addEventListener('resize', on);
    window.addEventListener('scroll', on, true);
    return () => {
      window.removeEventListener('resize', on);
      window.removeEventListener('scroll', on, true);
    };
  }, [monthMenuOpen, updateMonthPanelPosition]);

  useEffect(() => {
    const onDocMouseDown = (e) => {
      if (!monthMenuOpen) return;
      if (monthTriggerRef.current?.contains(e.target)) return;
      if (monthPanelRef.current?.contains(e.target)) return;
      setMonthMenuOpen(false);
    };
    document.addEventListener('mousedown', onDocMouseDown);
    return () => document.removeEventListener('mousedown', onDocMouseDown);
  }, [monthMenuOpen]);

  const toggleMonth = (ym) => {
    setSelectedMonths((prev) => {
      const next = new Set(prev);
      if (next.has(ym)) next.delete(ym);
      else next.add(ym);
      return next;
    });
  };

  const clearMonths = () => setSelectedMonths(new Set());
  const selectAllListedMonths = () => setSelectedMonths(new Set(monthOptions));

  useEffect(() => {
    if (!isCorp && !isBankUser) {
      navigate('/', { replace: true });
    }
  }, [isCorp, isBankUser, navigate]);

  useEffect(() => {
    if (isCorp && !corporateId) {
      setRawBatches([]);
      return;
    }
    if (isCorp && corporateId) {
      loadCorpBatches();
    } else if (isBankUser && !isCorp) {
      loadBankBatches();
    }
  }, [isCorp, isBankUser, corporateId]);

  const loadCorpBatches = async () => {
    setLoading(true);
    setError('');
    try {
      const { data } = await api.get('/payroll/batches', {
        params: { corporateId, status: 'COMPLETED' },
      });
      if (data?.success && data?.data) setRawBatches(data.data);
      else setRawBatches([]);
    } catch (e) {
      setError(e.response?.data?.message ?? 'Failed to load batches');
      setRawBatches([]);
    } finally {
      setLoading(false);
    }
  };

  const loadBankBatches = async () => {
    setLoading(true);
    setError('');
    try {
      const { data } = await api.get('/payroll/batches/completed-for-bank');
      if (data?.success && data?.data) setRawBatches(data.data);
      else setRawBatches([]);
    } catch (e) {
      setError(e.response?.data?.message ?? 'Failed to load batches');
      setRawBatches([]);
    } finally {
      setLoading(false);
    }
  };

  const openRecordsDialog = async (batch) => {
    setViewerBatch(batch);
    setViewerOpen(true);
    setViewerError('');
    setViewerRecords([]);
    setViewerLoading(true);
    try {
      const { data } = await api.get('/payroll/records/by-batch', { params: { batchId: batch.id } });
      if (data?.success && data?.data) setViewerRecords(data.data);
      else {
        setViewerRecords([]);
        setViewerError(data?.message ?? 'Could not load records');
      }
    } catch (e) {
      setViewerRecords([]);
      setViewerError(e.response?.data?.message ?? 'Could not load records');
    } finally {
      setViewerLoading(false);
    }
  };

  const closeRecordsDialog = () => {
    setViewerOpen(false);
    setViewerBatch(null);
    setViewerRecords([]);
    setViewerError('');
  };

  if (!isCorp && !isBankUser) return null;

  const isBankOnly = isBankUser && !isCorp;

  const monthPanelPortal =
    monthMenuOpen &&
    monthPanelPos &&
    createPortal(
      <div
        ref={monthPanelRef}
        className="multi-select-panel multi-select-panel--portal"
        role="listbox"
        aria-multiselectable="true"
        style={{
          position: 'fixed',
          top: monthPanelPos.top,
          left: monthPanelPos.left,
          width: monthPanelPos.width,
          maxHeight: monthPanelPos.maxHeight,
          zIndex: 4000,
        }}
      >
        <div className="multi-select-panel-head">
          <button type="button" className="btn-linkish" onClick={clearMonths}>
            Clear
          </button>
          <button type="button" className="btn-linkish" onClick={selectAllListedMonths}>
            Select all
          </button>
        </div>
        <div className="multi-select-options">
          {monthOptions.length === 0 ? (
            <p className="text-muted small pad-sm">No months in data yet.</p>
          ) : (
            monthOptions.map((ym) => (
              <label key={ym} className="multi-select-option">
                <input type="checkbox" checked={selectedMonths.has(ym)} onChange={() => toggleMonth(ym)} />
                <span>{formatYearMonth(ym)}</span>
              </label>
            ))
          )}
        </div>
      </div>,
      document.body
    );

  return (
    <div className="card payments-page">
      <div className="payments-page-header">
        <h2>Payments</h2>
        <p className="payments-page-lead text-muted">
          Completed batches (bank marked <strong>process payment</strong>). Filter the list, then use <strong>View</strong>{' '}
          to see payment lines in a dialog.
        </p>
      </div>

      <div className="payments-toolbar">
        <div className="payments-toolbar-fields">
          <div className="payments-field payments-field--narrow">
            <label htmlFor="ep-kind">Batch type</label>
            <select id="ep-kind" value={kindFilter} onChange={(e) => setKindFilter(e.target.value)} className="payments-select">
              <option value="all">All types</option>
              <option value="PAYROLL">Payroll</option>
              <option value="VENDOR">Vendor</option>
            </select>
          </div>

          {isBankOnly && (
            <div className="payments-field payments-field--grow">
              <label htmlFor="ep-corp">Corporate</label>
              <select
                id="ep-corp"
                value={corporateIdFilter}
                onChange={(e) => setCorporateIdFilter(e.target.value)}
                className="payments-select"
              >
                <option value="">All corporates</option>
                {corporateOptions.map(([id, name]) => (
                  <option key={id} value={id}>
                    {name}
                  </option>
                ))}
              </select>
            </div>
          )}

          <div className="payments-field payments-field--months">
            <label htmlFor="ep-months-trigger" className="payments-field-label" id="ep-months-label">
              Months
            </label>
            <div className="multi-select-dropdown">
              <button
                type="button"
                id="ep-months-trigger"
                ref={monthTriggerRef}
                className="multi-select-trigger"
                aria-haspopup="listbox"
                aria-expanded={monthMenuOpen}
                aria-labelledby="ep-months-label ep-months-trigger"
                onClick={() => setMonthMenuOpen((o) => !o)}
              >
                <span className="multi-select-trigger-text">{monthFilterSummary(selectedMonths)}</span>
                <span className="multi-select-chevron" aria-hidden>
                  ▾
                </span>
              </button>
            </div>
          </div>
        </div>
      </div>

      {monthPanelPortal}

      {error && <p className="error-msg">{error}</p>}

      {isCorp && !corporateId && <p className="error-msg">Your user is not linked to a corporate.</p>}

      {loading ? (
        <p className="payments-table-wrap">Loading…</p>
      ) : rawBatches.length === 0 ? (
        <p className="payments-table-wrap">No completed payment batches yet.</p>
      ) : batches.length === 0 ? (
        <p className="payments-table-wrap">No batches match the current filters ({rawBatches.length} completed total).</p>
      ) : (
        <div className="payments-table-wrap table-scroll">
          <table className="payments-table">
            <thead>
              <tr>
                {isBankOnly && <th>Corporate</th>}
                <th>Month</th>
                <th>Type</th>
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
                  {isBankOnly && <td>{b.corporateName ?? '—'}</td>}
                  <td>{formatYearMonth(b.yearMonth)}</td>
                  <td>{b.paymentBatchKindLabel ?? b.paymentBatchKind ?? '—'}</td>
                  <td className="payments-col-file">{b.fileName}</td>
                  <td>{b.batchStatusLabel ?? b.batchStatus ?? '—'}</td>
                  <td>{b.totalRecords}</td>
                  <td className="numeric">{b.totalAmount != null ? Number(b.totalAmount).toLocaleString() : '—'}</td>
                  <td className="nowrap">{b.createdAt ? new Date(b.createdAt).toLocaleString() : '—'}</td>
                  <td>
                    <button type="button" className="link-btn payments-link-view" onClick={() => openRecordsDialog(b)}>
                      View
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {viewerOpen && (
        <div className="modal-overlay" role="presentation" onClick={closeRecordsDialog}>
          <div
            className="modal-panel card payments-records-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="payments-records-title"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="modal-head">
              <h3 id="payments-records-title" style={{ margin: 0 }}>
                Payment lines
                {viewerBatch?.fileName ? (
                  <span className="text-muted" style={{ fontWeight: 400, display: 'block', fontSize: '0.9rem', marginTop: '0.25rem' }}>
                    {viewerBatch.fileName}
                  </span>
                ) : null}
              </h3>
              <button type="button" className="modal-close" onClick={closeRecordsDialog} aria-label="Close">
                ×
              </button>
            </div>

            <dl className="payments-records-summary detail-dl">
              <dt>Total records</dt>
              <dd>{viewerBatch?.totalRecords ?? '—'}</dd>
              <dt>Total amount</dt>
              <dd>
                {viewerBatch?.totalAmount != null ? Number(viewerBatch.totalAmount).toLocaleString() : '—'}
              </dd>
            </dl>

            {viewerError && <p className="error-msg">{viewerError}</p>}
            {viewerLoading ? (
              <p>Loading lines…</p>
            ) : (
              <div className="table-scroll payments-records-table-wrap">
                <table className="payments-records-table">
                  <thead>
                    <tr>
                      <th>Employee</th>
                      <th>Account</th>
                      <th>Amount</th>
                      <th>Payment for</th>
                    </tr>
                  </thead>
                  <tbody>
                    {viewerRecords.length === 0 ? (
                      <tr>
                        <td colSpan={4} className="text-muted">
                          No lines returned.
                        </td>
                      </tr>
                    ) : (
                      viewerRecords.map((r) => (
                        <tr key={r.id}>
                          <td>{r.employeeName}</td>
                          <td>{r.accountNumber}</td>
                          <td className="numeric">{r.amount != null ? Number(r.amount).toLocaleString() : '—'}</td>
                          <td>{r.paymentFor ?? '—'}</td>
                        </tr>
                      ))
                    )}
                  </tbody>
                </table>
              </div>
            )}

            <div className="modal-footer-actions" style={{ justifyContent: 'flex-end' }}>
              <button type="button" className="secondary" onClick={closeRecordsDialog}>
                Close
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
