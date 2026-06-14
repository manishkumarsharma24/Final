import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import StatusBadge from '../components/StatusBadge';

export default function ProfilePage() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const { add: toast } = useToast();
  const [customer, setCustomer] = useState(null);
  const [orders, setOrders]     = useState([]);
  const [tab, setTab]           = useState('overview');

  useEffect(() => {
    if (!user?.customerId) return;
    Promise.all([
      api.getCustomer(user.customerId),
      api.getOrders(user.customerId),
    ]).then(([c, o]) => {
      setCustomer(c);
      setOrders(Array.isArray(o) ? o : o?.content ?? []);
    });
  }, [user]);

  if (!user) { navigate('/login'); return null; }

  const initials = user.name?.split(' ').map(n => n[0]).join('').toUpperCase() || '?';
  const tier = customer?.tier || 'FREE';
  const totalSpent = orders.reduce((s, o) => s + (Number(o.total) || 0), 0);

  const handleLogout = () => {
    logout();
    toast('Signed out', 'info');
    navigate('/');
  };

  return (
    <div className="page">
      <h1 className="page-title">My Account</h1>
      <div className="profile-layout">
        {/* Sidebar */}
        <aside className="profile-card">
          <div className="profile-avatar">{initials}</div>
          <div className="profile-name">{user.name}</div>
          <div className="profile-email">{user.email}</div>
          <span className={`profile-tier tier-${tier}`}>
            {tier === 'GOLD' ? '🥇' : tier === 'SILVER' ? '🥈' : tier === 'BRONZE' ? '🥉' : '⭐'} {tier} Member
          </span>

          <div className="profile-stats">
            <div className="profile-stat">
              <div className="stat-val">{orders.length}</div>
              <div className="stat-lbl">Orders</div>
            </div>
            <div className="profile-stat">
              <div className="stat-val">${totalSpent.toFixed(0)}</div>
              <div className="stat-lbl">Spent</div>
            </div>
          </div>

          <div className="profile-nav">
            {[
              { key: 'overview', icon: '👤', label: 'Overview' },
              { key: 'orders',   icon: '📦', label: 'Orders' },
              { key: 'security', icon: '🔒', label: 'Security' },
            ].map(t => (
              <button
                key={t.key}
                className={`profile-nav-btn ${tab === t.key ? 'active' : ''}`}
                onClick={() => setTab(t.key)}
              >
                <span>{t.icon}</span> {t.label}
              </button>
            ))}
            <button className="profile-nav-btn" onClick={handleLogout} style={{ color: 'var(--danger)', marginTop: 8 }}>
              <span>🚪</span> Sign Out
            </button>
          </div>
        </aside>

        {/* Content */}
        <div className="profile-content">
          {tab === 'overview' && (
            <>
              <div className="profile-section">
                <h3>Account Details</h3>
                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 16 }}>
                  {[
                    { label: 'Full Name', value: user.name },
                    { label: 'Email', value: user.email },
                    { label: 'Role', value: user.role || 'USER' },
                    { label: 'Tier', value: tier },
                    { label: 'Member Since', value: customer?.createdAt ? new Date(customer.createdAt).toLocaleDateString() : '—' },
                  ].map(({ label, value }) => (
                    <div key={label}>
                      <div style={{ fontSize: '.8rem', color: 'var(--text-s)', fontWeight: 600, marginBottom: 4 }}>{label}</div>
                      <div style={{ fontWeight: 600 }}>{value}</div>
                    </div>
                  ))}
                </div>
              </div>

              <div className="profile-section">
                <h3>Recent Activity</h3>
                {orders.slice(0, 3).map(o => (
                  <div key={o.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '10px 0', borderBottom: '1px solid var(--border)' }}>
                    <div>
                      <div style={{ fontWeight: 600, fontSize: '.9rem' }}>Order #{o.id}</div>
                      <div style={{ color: 'var(--text-s)', fontSize: '.8rem' }}>{new Date(o.createdAt).toLocaleDateString()}</div>
                    </div>
                    <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                      <span style={{ fontWeight: 700 }}>${Number(o.total).toFixed(2)}</span>
                      <StatusBadge status={o.status} />
                    </div>
                  </div>
                ))}
                {!orders.length && <p style={{ color: 'var(--text-s)', fontSize: '.9rem' }}>No orders yet.</p>}
                {orders.length > 0 && (
                  <button className="btn-ghost" style={{ marginTop: 12 }} onClick={() => setTab('orders')}>View all orders →</button>
                )}
              </div>
            </>
          )}

          {tab === 'orders' && (
            <div className="profile-section">
              <h3>All Orders ({orders.length})</h3>
              {orders.map(o => (
                <div key={o.id} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 0', borderBottom: '1px solid var(--border)' }}>
                  <div>
                    <div style={{ fontWeight: 600 }}>Order #{o.id}</div>
                    <div style={{ color: 'var(--text-s)', fontSize: '.85rem' }}>{new Date(o.createdAt).toLocaleDateString()} · {o.items?.length ?? 0} item{o.items?.length !== 1 ? 's' : ''}</div>
                  </div>
                  <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
                    <span style={{ fontWeight: 700 }}>${Number(o.total).toFixed(2)}</span>
                    <StatusBadge status={o.status} />
                  </div>
                </div>
              ))}
              {!orders.length && <p style={{ color: 'var(--text-s)' }}>No orders found.</p>}
            </div>
          )}

          {tab === 'security' && (
            <div className="profile-section">
              <h3>Security</h3>
              <div style={{ display: 'flex', flexDirection: 'column', gap: 16 }}>
                <div style={{ background: 'var(--bg)', borderRadius: 10, padding: 16, fontSize: '.9rem' }}>
                  <div style={{ fontWeight: 700, marginBottom: 4 }}>🔒 JWT Authentication</div>
                  <div style={{ color: 'var(--text-s)' }}>Your session is secured with a stateless JWT token stored in localStorage. Token is sent as Bearer header on every API request.</div>
                </div>
                <div style={{ background: 'var(--bg)', borderRadius: 10, padding: 16, fontSize: '.9rem' }}>
                  <div style={{ fontWeight: 700, marginBottom: 4 }}>⚡ Redis Session Store</div>
                  <div style={{ color: 'var(--text-s)' }}>Tokens are also stored in Redis (key prefix <code>session:</code>) with a configurable TTL for fast invalidation and rate-limiting.</div>
                </div>
                <button className="btn-ghost" style={{ alignSelf: 'flex-start' }} onClick={handleLogout}>Sign Out →</button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
