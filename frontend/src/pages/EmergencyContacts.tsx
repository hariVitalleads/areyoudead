import { useState, useEffect } from 'react';
import {
  getEmergencyContacts,
  addEmergencyContact,
  updateEmergencyContact,
  deleteEmergencyContact,
} from '../api';
import type {
  EmergencyContactResponse,
  EmergencyContactRequest,
} from '../types';

export default function EmergencyContacts() {
  const [contacts, setContacts] = useState<EmergencyContactResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [editingId, setEditingId] = useState<string | null>(null);
  const [adding, setAdding] = useState(false);
  const [form, setForm] = useState<EmergencyContactRequest>({
    mobileNumber: '',
    email: '',
  });

  function load() {
    setLoading(true);
    setError('');
    getEmergencyContacts()
      .then(setContacts)
      .catch((err) => setError(err instanceof Error ? err.message : 'Load failed'))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    load();
  }, []);

  function startAdd() {
    setAdding(true);
    setEditingId(null);
    setForm({ mobileNumber: '', email: '' });
  }

  function startEdit(c: EmergencyContactResponse) {
    setEditingId(c.id);
    setAdding(false);
    setForm({ mobileNumber: c.mobileNumber, email: c.email });
  }

  function cancel() {
    setAdding(false);
    setEditingId(null);
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    try {
      if (editingId) {
        await updateEmergencyContact(editingId, form);
      } else {
        await addEmergencyContact(form);
      }
      setAdding(false);
      setEditingId(null);
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed');
    }
  }

  async function handleDelete(id: string) {
    if (!confirm('Delete this contact?')) return;
    setError('');
    try {
      await deleteEmergencyContact(id);
      load();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Delete failed');
    }
  }

  if (loading) return <div className="page">Loading…</div>;

  return (
    <div className="page">
      <div className="card">
        <h1>Emergency contacts</h1>
        <p className="subtitle">Up to 3 contacts. They will be notified if you become inactive.</p>

        {error && <div className="error">{error}</div>}

        <ul className="contact-list">
          {contacts.map((c) => (
            <li key={c.id}>
              {editingId === c.id ? (
                <form onSubmit={handleSubmit}>
                  <input
                    type="tel"
                    value={form.mobileNumber}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, mobileNumber: e.target.value }))
                    }
                    placeholder="Mobile"
                  />
                  <input
                    type="email"
                    value={form.email}
                    onChange={(e) =>
                      setForm((f) => ({ ...f, email: e.target.value }))
                    }
                    placeholder="Email"
                  />
                  <button type="submit">Save</button>
                  <button type="button" onClick={cancel}>
                    Cancel
                  </button>
                </form>
              ) : (
                <>
                  <span>{c.mobileNumber}</span>
                  <span>{c.email}</span>
                  <button onClick={() => startEdit(c)}>Edit</button>
                  <button onClick={() => handleDelete(c.id)} className="danger">
                    Delete
                  </button>
                </>
              )}
            </li>
          ))}
        </ul>

        {adding ? (
          <form onSubmit={handleSubmit}>
            <input
              type="tel"
              value={form.mobileNumber}
              onChange={(e) =>
                setForm((f) => ({ ...f, mobileNumber: e.target.value }))
              }
              placeholder="Mobile number"
              required
            />
            <input
              type="email"
              value={form.email}
              onChange={(e) =>
                setForm((f) => ({ ...f, email: e.target.value }))
              }
              placeholder="Email"
              required
            />
            <button type="submit">Add</button>
            <button type="button" onClick={cancel}>
              Cancel
            </button>
          </form>
        ) : (
          contacts.length < 3 && (
            <button onClick={startAdd}>Add contact</button>
          )
        )}
      </div>
    </div>
  );
}
