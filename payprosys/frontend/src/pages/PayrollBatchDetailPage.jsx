import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import api from '../api';
import { getUser } from '../api';

function formatYearMonth(ym) {
  if (ym == null) return '-';
  const s = String(ym);
  if (s.length === 6) return `${s.slice(0, 4)}-${s.slice(4)}`;
  return s;
}

const EMPTY_HINTS = {
  maxCorporateReviewLevel: 1,
  maxBankReviewLevel: 1,
  showCorporateApprove: false,
  showCorporateReject: false,
  showCorporateSendBack: false,
  showCorporateSendToBank: false,
  showBankApprove: false,
  showBankReject: false,
  showBankSendBack: false,
  showBankProcessPayment: false,
};

/** Completed, rejected, or bank terminal process-payment: no workflow actions or remarks. */
function isWorkflowTerminal(batch) {
  if (!batch) return false;
  const st = (batch.batchStatus || '').toUpperCase();
  const cf = (batch.corporateFlowState || '').toUpperCase();
  const bf = (batch.bankFlowState || '').toUpperCase();
  return st === 'COMPLETED' || cf === 'CORP_REJECTED' || bf === 'BANK_PROCESS_PAYMENT';
}

export default function PayrollBatchDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const user = getUser();
  const roles = user?.roles ?? [];
  const isSuperAdmin = roles.includes('SUPER_ADMIN');
  const isCorp = roles.includes('CORP_ADMIN') || roles.includes('CORP_USER');
  const isCorpAdminOnly = roles.includes('CORP_ADMIN') && !roles.includes('CORP_USER');
  const isBank = roles.includes('BANK_ADMIN') || roles.includes('BANK_USER');
  const corporateId = user?.corporateId ?? null;
  const bankId = user?.bankId ?? null;
  const myUserId = user?.userId != null ? String(user.userId) : null;
  const myEmail = (user?.email && String(user.email).toLowerCase()) || null;

  const [batch, setBatch] = useState(null);
  const [events, setEvents] = useState([]);
  const [records, setRecords] = useState([]);
  const [hints, setHints] = useState(EMPTY_HINTS);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const [remarkBody, setRemarkBody] = useState('');
  const [corpRemarks, setCorpRemarks] = useState('');
  const [bankRemarks, setBankRemarks] = useState('');

  const loadAll = async () => {
    setError('');
    try {
      const [bRes, eRes, rRes, hRes] = await Promise.all([
        api.get(`/payroll/batches/${id}`),
        api.get(`/payroll/workflow/batches/${id}/review-events`),
        api.get('/payroll/records/by-batch', { params: { batchId: id } }).catch(() => ({ data: { success: false } })),
        api.get(`/payroll/workflow/batches/${id}/action-hints`).catch(() => ({ data: { success: false } })),
      ]);
      const nextBatch = bRes.data?.success ? bRes.data.data : null;
      if (nextBatch) setBatch(nextBatch);
      else setBatch(null);
      if (eRes.data?.success) setEvents(eRes.data.data ?? []);
      else setEvents([]);
      if (rRes.data?.success) setRecords(rRes.data.data ?? []);
      else setRecords([]);
      if (hRes.data?.success && hRes.data.data) {
        setHints({ ...EMPTY_HINTS, ...hRes.data.data });
      } else {
        setHints(EMPTY_HINTS);
      }
    } catch (e) {
      setError(e.response?.data?.message ?? 'Failed to load batch');
      setBatch(null);
    }
  };

  useEffect(() => {
    if (isSuperAdmin || (!isCorp && !isBank)) {
      navigate('/', { replace: true });
      return;
    }
    loadAll();
  }, [id, isCorp, isBank, isSuperAdmin, navigate]);

  const postWorkflow = async (path, body) => {
    setBusy(true);
    setError('');
    try {
      const { data } = await api.post(`/payroll/workflow/batches/${id}${path}`, body ?? {});
      if (!data?.success) {
        setError(data?.message ?? 'Action failed');
        return false;
      }
      await loadAll();
      return true;
    } catch (e) {
      setError(e.response?.data?.message ?? e.message ?? 'Action failed');
      return false;
    } finally {
      setBusy(false);
    }
  };

  const handleAddRemark = async (e) => {
    e.preventDefault();
    if (!remarkBody.trim()) return;
    const ok = await postWorkflow('/review-events', {
      body: remarkBody.trim(),
      bankVisible: false,
      eventType: 'REMARK',
    });
    if (ok) setRemarkBody('');
  };

  if (isSuperAdmin || (!isCorp && !isBank)) return null;
  if (!batch && !error) return <div className="card"><p>Loading…</p></div>;
  if (!batch) {
    return (
      <div className="card">
        <p className="error-msg">{error || 'Batch not found'}</p>
        <Link to="/payroll/inbox">Back to inbox</Link>
      </div>
    );
  }

  const st = (batch.batchStatus || '').toUpperCase();
  const showRecords =
    isCorp ||
    (isBank &&
      (((st === 'SUBMITTED' || st === 'COMPLETED') &&
        (batch.corporateFlowState || '') === 'CORP_SENT_TO_BANK') ||
        (st === 'PENDING' &&
          (batch.corporateFlowState || '') === 'CORP_CLARIFICATION' &&
          (batch.bankFlowState || '') === 'BANK_SENT_TO_CORPORATE')));
  const corpAny =
    hints.showCorporateApprove ||
    hints.showCorporateReject ||
    hints.showCorporateSendBack ||
    hints.showCorporateSendToBank;
  const bankAny =
    hints.showBankApprove || hints.showBankSendBack || hints.showBankProcessPayment || hints.showBankReject;
  const terminal = isWorkflowTerminal(batch);

  return (
    <div>
      <div className="card">
        <p className="mb-2">
          <Link to="/payroll/inbox">← Inbox</Link>
          {' · '}
          <Link to="/payroll/history">History</Link>
          {isCorp && !isCorpAdminOnly && (
            <>
              {' · '}
              <Link to="/payroll/upload">Upload</Link>
            </>
          )}
          {isCorp && isCorpAdminOnly && (
            <>
              {' · '}
              <Link to="/payroll/upload">Upload</Link>
            </>
          )}
        </p>
        <h2>Batch detail</h2>
        {error && <p className="error-msg">{error}</p>}
        {batch.viewOnly && (
          <p className="mb-2 text-muted" role="status">
            You are viewing this batch in <strong>read-only</strong> mode (not the current actor on your side, or awaiting
            the other party). Workflow actions are disabled.
          </p>
        )}
        <dl className="detail-dl">
          <dt>Month</dt>
          <dd>{formatYearMonth(batch.yearMonth)}</dd>
          <dt>File</dt>
          <dd>{batch.fileName}</dd>
          <dt>Batch type</dt>
          <dd>{batch.paymentBatchKindLabel ?? batch.paymentBatchKind ?? '—'}</dd>
          <dt>Records / amount</dt>
          <dd>
            {batch.totalRecords} / {batch.totalAmount != null ? Number(batch.totalAmount).toLocaleString() : '—'}
          </dd>
          <dt>Batch status</dt>
          <dd>
            {batch.batchStatusLabel ?? batch.batchStatus ?? '—'}
            {batch.batchStatus && batch.batchStatusLabel && batch.batchStatus !== batch.batchStatusLabel ? (
              <span className="text-muted small"> ({batch.batchStatus})</span>
            ) : null}
          </dd>
          <dt>Corporate flow</dt>
          <dd>{batch.corporateFlowState ?? '—'}</dd>
          <dt>Bank flow</dt>
          <dd>{batch.bankFlowState ?? '—'}</dd>
          <dt>Current corp. review level</dt>
          <dd>{batch.currentCorporateReviewLevel ?? '—'} (max {hints.maxCorporateReviewLevel})</dd>
          <dt>Current bank review level</dt>
          <dd>{batch.currentBankReviewLevel ?? '—'} (max {hints.maxBankReviewLevel})</dd>
          <dt>Remarks for bank (last send)</dt>
          <dd>{batch.remarksForBank ? <span className="bank-visible">{batch.remarksForBank}</span> : '—'}</dd>
        </dl>
      </div>

      {isCorp && corporateId && !batch.viewOnly && !terminal && (
        <div className="card">
          <h3>Corporate actions</h3>
          <p className="mb-2 text-muted">
            One remarks field for every action. <strong>Send to bank</strong> uses this text as bank-visible remarks and
            requires non-empty remarks. It is only available to users on the final corporate step (level {hints.maxCorporateReviewLevel}).
          </p>
          <div className="form-group">
            <label>Remarks</label>
            <textarea
              value={corpRemarks}
              onChange={(e) => setCorpRemarks(e.target.value)}
              rows={3}
              placeholder="Used for Approve, Reject, Send back, or Send to bank…"
            />
          </div>
          <div className="flex gap-2 flex-wrap">
            <button
              type="button"
              className="secondary"
              disabled={busy || !hints.showCorporateApprove}
              title={!hints.showCorporateApprove ? 'Not available at this step or for your role.' : ''}
              onClick={async () => {
                const ok = await postWorkflow('/corporate/approve', {
                  remarks: corpRemarks.trim() || undefined,
                  sendToBankAfter: false,
                });
                if (ok) {
                  setCorpRemarks('');
                  navigate('/payroll/inbox', { replace: true });
                }
              }}
            >
              Approve
            </button>
            <button
              type="button"
              className="btn-danger-soft"
              disabled={busy || !hints.showCorporateReject}
              title={!hints.showCorporateReject ? 'Not available at this step or for your role.' : ''}
              onClick={async () => {
                if (!window.confirm('Reject this batch permanently (corporate terminal)?')) return;
                const ok = await postWorkflow('/corporate/reject', { remarks: corpRemarks.trim() || undefined });
                if (ok) {
                  setCorpRemarks('');
                  navigate('/payroll/inbox', { replace: true });
                }
              }}
            >
              Reject
            </button>
            <button
              type="button"
              disabled={busy || !hints.showCorporateSendBack}
              title={!hints.showCorporateSendBack ? 'Not available at this step or for your role.' : ''}
              onClick={async () => {
                const ok = await postWorkflow('/corporate/send-back', { remarks: corpRemarks.trim() || undefined });
                if (ok) setCorpRemarks('');
              }}
            >
              Send back
            </button>
            <button
              type="button"
              disabled={busy || !hints.showCorporateSendToBank}
              title={!hints.showCorporateSendToBank ? 'Only after full corporate approval, for final-step users.' : ''}
              onClick={async () => {
                if (!corpRemarks.trim()) {
                  setError('Remarks are required to send this batch to the bank.');
                  return;
                }
                const ok = await postWorkflow('/corporate/send-to-bank', { remarksForBank: corpRemarks.trim() });
                if (ok) setCorpRemarks('');
              }}
            >
              Send to bank
            </button>
          </div>
          {!corpAny && <p className="mt-2 text-muted small">No corporate actions for you at this step.</p>}
        </div>
      )}

      {isBank && bankId && !batch.viewOnly && !terminal && (
        <div className="card">
          <h3>Bank actions</h3>
          <p className="mb-2 text-muted">
            Same pattern as corporate: one remarks field. <strong>Reject</strong> is not wired in this POC (no API).
          </p>
          <div className="form-group">
            <label>Remarks</label>
            <textarea
              value={bankRemarks}
              onChange={(e) => setBankRemarks(e.target.value)}
              rows={3}
              placeholder="Used for Approve, Send back, or Mark process payment…"
            />
          </div>
          <div className="flex gap-2 flex-wrap">
            <button
              type="button"
              disabled={busy || !hints.showBankApprove}
              title={!hints.showBankApprove ? 'Not available at this bank step or for your role.' : ''}
              onClick={async () => {
                const ok = await postWorkflow('/bank/approve', { remarks: bankRemarks.trim() || undefined });
                if (ok) setBankRemarks('');
              }}
            >
              Approve
            </button>
            <button
              type="button"
              className="btn-danger-soft"
              disabled
              title="Bank reject is not implemented in this POC."
            >
              Reject
            </button>
            <button
              type="button"
              disabled={busy || !hints.showBankSendBack}
              title={!hints.showBankSendBack ? 'Not available at this bank step or for your role.' : ''}
              onClick={async () => {
                const ok = await postWorkflow('/bank/send-back-to-corporate', { remarks: bankRemarks.trim() || undefined });
                if (ok) setBankRemarks('');
              }}
            >
              Send back
            </button>
            <button
              type="button"
              disabled={busy || !hints.showBankProcessPayment}
              title={!hints.showBankProcessPayment ? 'Available after the final bank approval step.' : ''}
              onClick={async () => {
                if (!window.confirm('Mark this batch as process payment (terminal)?')) return;
                const ok = await postWorkflow('/bank/mark-process-payment', { remarks: bankRemarks.trim() || undefined });
                if (ok) setBankRemarks('');
              }}
            >
              Process payment
            </button>
          </div>
          {!bankAny && <p className="mt-2 text-muted small">No bank actions for you at this step.</p>}
        </div>
      )}

      {(isCorp || isBank) && !batch.viewOnly && !terminal && (
        <div className="card">
          <h3>Add remark</h3>
          <p className="mb-2 text-muted">From this form remarks stay internal (not bank-visible). Bank-visible text is only from send-to-bank.</p>
          <form onSubmit={handleAddRemark}>
            <div className="form-group">
              <textarea value={remarkBody} onChange={(e) => setRemarkBody(e.target.value)} rows={3} placeholder="Add to timeline…" />
            </div>
            <button type="submit" disabled={busy || !remarkBody.trim()}>
              Append remark
            </button>
          </form>
        </div>
      )}

      <div className="card">
        <h3>Remark timeline</h3>
        <p className="mb-2 text-muted">
          <span className="badge badge-cross">Visible to Bank / Visible to Corporate</span> on shared remarks (send-to-bank,
          bank send-back, process payment). Corporate approve remarks stay internal until send-to-bank.{' '}
          <span className="badge badge-internal">Internal</span> stays on one side only.
        </p>
        {events.length === 0 ? (
          <p>No events yet.</p>
        ) : (
          <div className="remark-thread">
            {events.map((ev) => {
              const actorId = ev.actorUserId != null ? String(ev.actorUserId) : null;
              const actorEmail = ev.actorEmail ? String(ev.actorEmail).toLowerCase() : null;
              const mine =
                (myUserId && actorId && myUserId === actorId) ||
                (!myUserId && myEmail && actorEmail && myEmail === actorEmail);
              const chip = ev.actorStepChip || ev.actorEmail || '—';
              const ts = ev.createdAt ? new Date(ev.createdAt).toLocaleString() : '—';
              const vBank = typeof ev.visibleToBank === 'boolean' ? ev.visibleToBank : Boolean(ev.bankVisible);
              const vCorp = typeof ev.visibleToCorporate === 'boolean' ? ev.visibleToCorporate : Boolean(ev.bankVisible);
              return (
                <div key={ev.id} className={`remark-row ${mine ? 'remark-row--mine' : ''}`}>
                  <div className="remark-bubble">
                    <div className="remark-line-meta">
                      <span className="remark-chip">{chip}</span>
                      <span className="remark-sep"> — </span>
                      <span className="remark-ts">{ts}</span>
                      <span className="remark-sep"> — </span>
                      {vBank && vCorp ? (
                        corporateId ? (
                          <span className="badge badge-cross">Visible to Bank</span>
                        ) : bankId ? (
                          <span className="badge badge-cross">Visible to Corporate</span>
                        ) : (
                          <span className="badge badge-cross">Shared</span>
                        )
                      ) : vCorp && !vBank ? (
                        <span className="badge badge-internal">Internal (corporate)</span>
                      ) : vBank && !vCorp ? (
                        <span className="badge badge-internal">Internal (bank)</span>
                      ) : (
                        <span className="badge badge-internal">Internal</span>
                      )}
                    </div>
                    {ev.body ? <div className="remark-body">{ev.body}</div> : null}
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {showRecords && records.length > 0 && (
        <div className="card">
          <h3>Payroll lines</h3>
          <div className="table-scroll">
            <table>
              <thead>
                <tr>
                  <th>Employee</th>
                  <th>Account</th>
                  <th>Amount</th>
                  <th>Payment for</th>
                </tr>
              </thead>
              <tbody>
                {records.map((r) => (
                  <tr key={r.id}>
                    <td>{r.employeeName}</td>
                    <td>{r.accountNumber}</td>
                    <td>{r.amount != null ? Number(r.amount).toLocaleString() : '—'}</td>
                    <td>{r.paymentFor}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
