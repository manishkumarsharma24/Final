import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import StatusBadge from '../components/StatusBadge';

const CANCELLABLE = ['PENDING', 'CONFIRMED'];

export default function OrdersPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const { add: toast } = useToast();

  const [orders, setOrders] = useState([]);
  const [loading, setLoading] = useState(true);
  const [expandedActivity, setExpandedActivity] = useState({});
  const [activity, setActivity] = useState({});
  const [loadingActivity, setLoadingActivity] = useState({});

  useEffect(() => {
    if (!user?.customerId) return;
    api.getOrders(user.customerId).then(data => {
      setOrders(Array.isArray(data) ? data : data?.content ?? []);
      setLoading(false);
    });
  }, [user]);

  const toggleActivity = async (orderId) => {
    const open = !expandedActivity[orderId];
    setExpandedActivity(prev => ({ ...prev, [orderId]: open }));
    if (open && !activity[orderId]) {
      setLoadingActivity(prev => ({ ...prev, [orderId]: true }));
      try {
        const acts = await api.getOrderActivity(orderId);
        setActivity(prev => ({ ...prev, [orderId]: Array.isArray(acts) ? acts : [] }));
      } catch {
        setActivity(prev => ({ ...prev, [orderId]: [] }));
      } finally {
        setLoadingActivity(prev => ({ ...prev, [orderId]: false }));
      }
    }
  };

  const handleCancel = async (orderId) => {
    if (!confirm('Cancel this order?')) return;
    await api.cancelOrder(orderId);
    setOrders(prev => prev.map(o => o.id === orderId ? { ...o, status: 'CANCELLED' } : o));
    toast('Order cancelled', 'info');
  };

  if (loading) return <div className="page"><div className="loading-state">Loading orders…</div></div>;

  if (!orders.length) {
    return (
      <div className="page">
        <h1 className="page-title">My Orders</h1>
        <div className="empty-state">
          <span className="empty-icon">📦</span>
          <h2>No orders yet</h2>
          <p>Your orders will appear here once you place them</p>
          <button className="btn-primary-lg" onClick={() => navigate('/products')}>Start Shopping</button>
        </div>
      </div>
    );
  }

  return (
    <div className="page">
      <h1 className="page-title">My Orders</h1>

      <div className="orders-list">
        {orders.map(order => (
          <div key={order.id} className="order-card">
            <div className="order-header">
              <div>
                <span className="order-id">Order #{order.id}</span>
                <span className="order-date">{new Date(order.createdAt).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' })}</span>
              </div>
              <StatusBadge status={order.status} />
            </div>

            {order.items?.length > 0 && (
              <div className="order-items">
                {order.items.map((item, i) => (
                  <div key={i} className="order-item-row">
                    <span>{item.productName} × {item.quantity}</span>
                    <span>${(item.unitPrice * item.quantity).toFixed(2)}</span>
                  </div>
                ))}
              </div>
            )}

            <div className="order-footer">
              <span className="order-total">
                Total: <strong>${typeof order.total === 'number' ? order.total.toFixed(2) : order.total}</strong>
              </span>
              <span className="tracking" style={{ fontSize: '.8rem', color: 'var(--text-s)' }}>
                {order.status === 'SHIPPED' && '📦 In transit'}
                {order.status === 'DELIVERED' && '✓ Delivered'}
              </span>
            </div>

            <div className="order-card-actions">
              {/* Cassandra activity log toggle */}
              <button className="activity-toggle" onClick={() => toggleActivity(order.id)}>
                {expandedActivity[order.id] ? '▲' : '▼'} Activity Log
                <span className="activity-label">Cassandra</span>
              </button>

              {CANCELLABLE.includes(order.status) && (
                <button className="btn-cancel" onClick={() => handleCancel(order.id)}>✕ Cancel Order</button>
              )}

              <button className="btn-ghost" style={{ padding: '7px 14px', fontSize: '.85rem' }}
                onClick={() => navigate(`/products/${order.items?.[0]?.productId}`)}>
                Reorder
              </button>
            </div>

            {/* Activity Timeline — data from Cassandra */}
            {expandedActivity[order.id] && (
              <div className="activity-panel">
                <div style={{ fontSize: '.75rem', color: 'var(--text-s)', marginBottom: 10 }}>
                  📊 Cassandra · partition key: <code>customerId={order.customerId}</code>
                </div>
                <div className="activity-timeline">
                  {loadingActivity[order.id] && (
                    <div style={{ color: 'var(--text-s)', fontSize: '.85rem' }}>Fetching from Cassandra…</div>
                  )}
                  {!loadingActivity[order.id] && (activity[order.id] ?? []).map((act, i) => (
                    <div key={String(act.eventId ?? i)} className="timeline-item">
                      <div className="timeline-dot">{i + 1}</div>
                      <div className="timeline-content">
                        <div className="timeline-type">{String(act.eventType ?? '').replace(/_/g, ' ')}</div>
                        <div className="timeline-desc">{act.details}</div>
                        <div className="timeline-time">{act.eventTime ? new Date(act.eventTime).toLocaleString() : ''}</div>
                      </div>
                    </div>
                  ))}
                  {!loadingActivity[order.id] && activity[order.id] !== undefined && !(activity[order.id]?.length) && (
                    <div style={{ color: 'var(--text-s)', fontSize: '.85rem' }}>No activity logged yet for this order.</div>
                  )}
                </div>
              </div>
            )}
          </div>
        ))}
      </div>
    </div>
  );
}
