import { useState, useEffect } from 'react';
import { api } from '../api/client';
import { useToast } from '../context/ToastContext';
import StatusBadge from '../components/StatusBadge';

// Kafka event published per action — must match domain OrderStatus.canTransitionTo()
// PENDING → CONFIRMED → PROCESSING → SHIPPED → DELIVERED → REFUNDED
// PENDING / CONFIRMED / PROCESSING → CANCELLED
const TRANSITIONS = {
  PENDING:    [
    { action: 'confirm',  label: 'Confirm',  cls: 'tbl-btn-primary', kafka: 'OrderConfirmed'  },
    { action: 'cancel',   label: 'Cancel',   cls: 'tbl-btn-danger',  kafka: 'OrderCancelled'  },
  ],
  CONFIRMED:  [
    { action: 'process',  label: 'Process',  cls: 'tbl-btn-primary', kafka: 'OrderProcessing' },
    { action: 'cancel',   label: 'Cancel',   cls: 'tbl-btn-danger',  kafka: 'OrderCancelled'  },
  ],
  PROCESSING: [
    { action: 'ship',     label: 'Ship',     cls: 'tbl-btn-success', kafka: 'OrderShipped'    },
    { action: 'cancel',   label: 'Cancel',   cls: 'tbl-btn-danger',  kafka: 'OrderCancelled'  },
  ],
  SHIPPED:    [{ action: 'deliver', label: 'Deliver', cls: 'tbl-btn-success', kafka: 'OrderDelivered' }],
  DELIVERED:  [{ action: 'refund',  label: 'Refund',  cls: 'tbl-btn-warn',   kafka: 'OrderRefunded'  }],
  CANCELLED:  [],
  REFUNDED:   [],
};

export default function AdminOrdersPage() {
  const { add: toast } = useToast();
  const [orders, setOrders]   = useState([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter]   = useState('');
  const [page, setPage]       = useState(0);
  const [total, setTotal]     = useState(0);

  const load = (p = 0, status = '') => {
    setLoading(true);
    api.getAllOrders({ page: p, size: 15, status }).then(data => {
      // Backend returns a plain array; mock fallback returns { content, totalElements }
      const list = Array.isArray(data) ? data : (data?.content ?? []);
      setOrders(list);
      setTotal(Array.isArray(data) ? data.length : (data?.totalElements ?? 0));
      setLoading(false);
    });
  };

  useEffect(() => { load(0, filter); }, [filter]);

  const handleAction = async (orderId, action, kafkaEvent) => {
    await api.updateOrderStatus(orderId, action);
    setOrders(prev => prev.map(o => {
      if (o.id !== orderId) return o;
      const statusMap = { confirm: 'CONFIRMED', process: 'PROCESSING', ship: 'SHIPPED', deliver: 'DELIVERED', cancel: 'CANCELLED', refund: 'REFUNDED' };
      return { ...o, status: statusMap[action] };
    }));
    toast(`✓ ${kafkaEvent} → Kafka shopverse.orders`, 'success');
  };

  return (
    <div>
      <div className="admin-page-title">Orders</div>
      <div className="admin-page-subtitle">
        Status transitions publish Kafka events → <code>shopverse.orders</code> topic
      </div>

      {/* Kafka education box */}
      <div className="kafka-info-box" style={{ marginBottom: 20 }}>
        <h4>🎯 Kafka Event Flow</h4>
        <p>Each action below calls PATCH /api/orders/:id/:action → UpdateOrderStatusUseCase → EventPublisher → Kafka.</p>
        <div className="kafka-event-flow">
          <span className="kafka-event-pill">PATCH /confirm</span><span className="arrow">→</span>
          <span className="kafka-event-pill">OrderConfirmed</span><span className="arrow">→</span>
          <span className="kafka-event-pill">shopverse.orders</span><span className="arrow">→</span>
          <span className="kafka-event-pill">Consumer</span><span className="arrow">→</span>
          <span className="kafka-event-pill">Cassandra log</span>
        </div>
      </div>

      <div className="admin-table-wrap">
        <div className="admin-table-header">
          <h3>All Orders ({total})</h3>
          <div className="admin-table-filters">
            <select className="admin-filter-select" value={filter} onChange={e => { setFilter(e.target.value); setPage(0); }}>
              <option value="">All Statuses</option>
              {['PENDING','CONFIRMED','SHIPPED','DELIVERED','CANCELLED','REFUNDED'].map(s => (
                <option key={s} value={s}>{s}</option>
              ))}
            </select>
          </div>
        </div>

        {loading ? (
          <div style={{ padding: 24, textAlign: 'center', color: 'var(--text-s)' }}>Loading…</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>Order ID</th>
                <th>Customer</th>
                <th>Total</th>
                <th>Status</th>
                <th>Date</th>
                <th>Actions (→ Kafka)</th>
              </tr>
            </thead>
            <tbody>
              {orders.map(o => {
                const actions = TRANSITIONS[o.status] ?? [];
                return (
                  <tr key={o.id}>
                    <td><strong>#{o.id}</strong></td>
                    <td>#{o.customerId}</td>
                    <td><strong>${Number(o.total).toFixed(2)}</strong></td>
                    <td><StatusBadge status={o.status} /></td>
                    <td style={{ color: 'var(--text-s)', fontSize: '.82rem' }}>{new Date(o.createdAt).toLocaleDateString()}</td>
                    <td>
                      <div className="table-actions">
                        {actions.map(({ action, label, cls, kafka }) => (
                          <button
                            key={action}
                            className={`tbl-btn ${cls}`}
                            title={`Publishes ${kafka} to Kafka`}
                            onClick={() => handleAction(o.id, action, kafka)}
                          >
                            {label}
                            <span className="tech-label tech-kafka" style={{ fontSize: '.65rem', padding: '1px 5px', marginLeft: 4 }}>K</span>
                          </button>
                        ))}
                        {!actions.length && <span style={{ color: 'var(--text-s)', fontSize: '.8rem' }}>—</span>}
                      </div>
                    </td>
                  </tr>
                );
              })}
              {!orders.length && (
                <tr><td colSpan={6} style={{ textAlign: 'center', padding: 32, color: 'var(--text-s)' }}>No orders found</td></tr>
              )}
            </tbody>
          </table>
        )}
      </div>

      {/* Pagination */}
      {total > 15 && (
        <div className="pagination">
          <button disabled={page === 0} onClick={() => { setPage(p => p - 1); load(page - 1, filter); }}>← Prev</button>
          <span style={{ padding: '0 12px', color: 'var(--text-s)', fontSize: '.9rem' }}>Page {page + 1}</span>
          <button disabled={(page + 1) * 15 >= total} onClick={() => { setPage(p => p + 1); load(page + 1, filter); }}>Next →</button>
        </div>
      )}
    </div>
  );
}
