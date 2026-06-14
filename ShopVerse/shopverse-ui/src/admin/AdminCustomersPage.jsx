import { useState, useEffect } from 'react';
import { api } from '../api/client';

export default function AdminCustomersPage() {
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading]     = useState(true);
  const [search, setSearch]       = useState('');

  useEffect(() => {
    api.getAllCustomers({ size: 100 }).then(data => {
      setCustomers(data?.content ?? []);
      setLoading(false);
    });
  }, []);

  const TIER_STYLE = {
    GOLD:   { background: '#fef3c7', color: '#92400e' },
    SILVER: { background: '#f1f5f9', color: '#475569' },
    BRONZE: { background: '#fef0e6', color: '#9a3412' },
    FREE:   { background: '#ede9fe', color: '#6d28d9' },
  };

  const filtered = customers.filter(c =>
    `${c.firstName} ${c.lastName} ${c.email}`.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div>
      <div className="admin-page-title">Customers</div>
      <div className="admin-page-subtitle">
        Customer data stored in PostgreSQL · Cached in Redis (TTL 60 min)
      </div>

      <div className="admin-table-wrap">
        <div className="admin-table-header">
          <h3>Registered Customers ({customers.length})</h3>
          <input
            className="admin-search-input"
            placeholder="Search by name or email…"
            value={search}
            onChange={e => setSearch(e.target.value)}
          />
        </div>

        {loading ? (
          <div style={{ padding: 24, textAlign: 'center', color: 'var(--text-s)' }}>Loading…</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Email</th>
                <th>Tier</th>
                <th>Member Since</th>
                <th>Cache</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map(c => (
                <tr key={c.id}>
                  <td style={{ color: 'var(--text-s)', fontSize: '.85rem' }}>#{c.id}</td>
                  <td><strong>{c.firstName} {c.lastName}</strong></td>
                  <td style={{ color: 'var(--text-s)' }}>{c.email}</td>
                  <td>
                    <span style={{ ...(TIER_STYLE[c.tier] || TIER_STYLE.FREE), padding: '3px 10px', borderRadius: 10, fontSize: '.78rem', fontWeight: 700 }}>
                      {c.tier || 'FREE'}
                    </span>
                  </td>
                  <td style={{ color: 'var(--text-s)', fontSize: '.85rem' }}>{c.createdAt ? new Date(c.createdAt).toLocaleDateString() : '—'}</td>
                  <td>
                    <span className="tech-label tech-redis" style={{ fontSize: '.68rem' }}>Redis 60m</span>
                  </td>
                </tr>
              ))}
              {!filtered.length && (
                <tr><td colSpan={6} style={{ textAlign: 'center', padding: 24, color: 'var(--text-s)' }}>No customers found</td></tr>
              )}
            </tbody>
          </table>
        )}
      </div>

      <div style={{ marginTop: 20, background: '#fee2e2', border: '1px solid #fca5a5', borderRadius: 10, padding: 14, fontSize: '.82rem', color: '#7f1d1d' }}>
        <strong>⚡ Redis Cache:</strong> Customer data is cached under key <code>customers::{'{'}id{'}'}</code> with a 60-minute TTL configured in CacheConfig. Cache is evicted on customer update via <code>@CacheEvict</code>.
      </div>
    </div>
  );
}
