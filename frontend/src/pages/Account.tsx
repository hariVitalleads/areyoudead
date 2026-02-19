import { useState, useEffect } from 'react';
import { getAccount, updateAccountDetails } from '../api';
import type { AccountDetailsResponse, UpdateDetailsRequest } from '../types';

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
      const updated = await updateAccountDetails(form);
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
            value={form.mobileNumber ?? ''}
            onChange={(e) =>
              setForm((f) => ({ ...f, mobileNumber: e.target.value }))
            }
          />
          <button type="submit" disabled={saving}>
            {saving ? 'Saving…' : 'Save'}
          </button>
        </form>
      </div>
    </div>
  );
}
