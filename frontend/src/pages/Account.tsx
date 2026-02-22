import { useState, useEffect } from 'react';
import { getAccount, updateAccountDetails } from '../api';
import type {
  AccountDetailsResponse,
  UpdateDetailsRequest,
  AlertChannelPreference,
} from '../types';

function derivePreferenceFromCheckboxes(
  email: boolean,
  sms: boolean
): AlertChannelPreference | undefined {
  if (email && sms) return 'BOTH';
  if (email) return 'EMAIL';
  if (sms) return 'SMS';
  return undefined;
}

function isEmailChecked(pref: AlertChannelPreference | undefined): boolean {
  return pref === 'EMAIL' || pref === 'BOTH';
}

function isSmsChecked(pref: AlertChannelPreference | undefined): boolean {
  return pref === 'SMS' || pref === 'BOTH';
}

export default function Account() {
  const [data, setData] = useState<AccountDetailsResponse | null>(null);
  const [form, setForm] = useState<UpdateDetailsRequest>({});
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');
  const [success, setSuccess] = useState('');

  useEffect(() => {
    getAccount()
      .then((d) => {
        setData(d);
        setForm({
          email: d.email,
          firstName: d.firstName ?? '',
          lastName: d.lastName ?? '',
          mobileNumber: d.mobileNumber ?? '',
          inactivityThresholdDays: d.inactivityThresholdDays ?? undefined,
          alertChannelPreference: d.alertChannelPreference ?? undefined,
        });
      })
      .catch((err) => setError(err instanceof Error ? err.message : 'Load failed'))
      .finally(() => setLoading(false));
  }, []);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setSuccess('');
    setSaving(true);
    try {
      const payload: UpdateDetailsRequest = {
        email: form.email || undefined,
        firstName: form.firstName || undefined,
        lastName: form.lastName || undefined,
        mobileNumber: form.mobileNumber || undefined,
        inactivityThresholdDays:
          form.inactivityThresholdDays != null && form.inactivityThresholdDays > 0
            ? form.inactivityThresholdDays
            : undefined,
        alertChannelPreference: form.alertChannelPreference,
      };
      const updated = await updateAccountDetails(payload);
      setData(updated);
      setSuccess('Details updated.');
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Update failed');
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <div className="page">Loading…</div>;
  if (!data) return <div className="page">Could not load account.</div>;

  return (
    <div className="page">
      <div className="card">
        <h1>Account details</h1>
        <form onSubmit={handleSubmit}>
          {error && <div className="error">{error}</div>}
          {success && <div className="success">{success}</div>}

          <label>Email</label>
          <input
            type="email"
            value={form.email ?? ''}
            onChange={(e) => setForm((f) => ({ ...f, email: e.target.value }))}
          />

          <label>First name</label>
          <input
            value={form.firstName ?? ''}
            onChange={(e) =>
              setForm((f) => ({ ...f, firstName: e.target.value }))
            }
          />

          <label>Last name</label>
          <input
            value={form.lastName ?? ''}
            onChange={(e) =>
              setForm((f) => ({ ...f, lastName: e.target.value }))
            }
          />

          <label>Mobile number</label>
          <input
            type="tel"
            value={form.mobileNumber ?? ''}
            placeholder="e.g. +1234567890"
            onChange={(e) =>
              setForm((f) => ({ ...f, mobileNumber: e.target.value }))
            }
          />

          <label>Inactivity threshold (days)</label>
          <input
            type="number"
            min={1}
            max={90}
            value={form.inactivityThresholdDays ?? ''}
            placeholder="Leave empty for default"
            onChange={(e) => {
              const v = e.target.value;
              setForm((f) => ({
                ...f,
                inactivityThresholdDays: v === '' ? undefined : parseInt(v, 10),
              }));
            }}
          />
          <small className="form-hint">
            Days of inactivity before emergency contacts are notified (1–90). Empty = use system default.
          </small>

          <label>Alert channel preference</label>
          <div className="checkbox-group">
            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={isEmailChecked(form.alertChannelPreference)}
                onChange={(e) =>
                  setForm((f) => ({
                    ...f,
                    alertChannelPreference: derivePreferenceFromCheckboxes(
                      e.target.checked,
                      isSmsChecked(f.alertChannelPreference)
                    ),
                  }))
                }
              />
              <span>Notify by email</span>
            </label>
            <label className="checkbox-label">
              <input
                type="checkbox"
                checked={isSmsChecked(form.alertChannelPreference)}
                onChange={(e) =>
                  setForm((f) => ({
                    ...f,
                    alertChannelPreference: derivePreferenceFromCheckboxes(
                      isEmailChecked(f.alertChannelPreference),
                      e.target.checked
                    ),
                  }))
                }
              />
              <span>Notify by SMS</span>
            </label>
          </div>
          <small className="form-hint">
            How to notify your emergency contacts when you are inactive. Uncheck both to use default (both).
          </small>

          <button type="submit" disabled={saving}>
            {saving ? 'Saving…' : 'Save'}
          </button>
        </form>
      </div>
    </div>
  );
}
