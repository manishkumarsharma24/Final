import { useState } from 'react';
import { api } from '../api/client';
import { useToast } from '../context/ToastContext';

const NOTIF_TYPES = [
  { value: 'ORDER_PLACED',    label: 'Order Placed',    routingKey: 'order.placed',    queue: 'shopverse.email' },
  { value: 'ORDER_SHIPPED',   label: 'Order Shipped',   routingKey: 'order.shipped',   queue: 'shopverse.sms'   },
  { value: 'ORDER_DELIVERED', label: 'Order Delivered', routingKey: 'order.delivered', queue: 'shopverse.push'  },
  { value: 'PAYMENT_SUCCESS', label: 'Payment Success', routingKey: 'payment.success', queue: 'shopverse.email' },
  { value: 'PROMOTION',       label: 'Promotion',       routingKey: 'promo.*',         queue: 'shopverse.push'  },
];

export default function AdminNotificationsPage() {
  const { add: toast } = useToast();
  const [form, setForm] = useState({ type: 'ORDER_PLACED', recipient: '', orderId: '', message: '' });
  const [sending, setSending] = useState(false);
  const [history, setHistory] = useState([]);

  const selected = NOTIF_TYPES.find(t => t.value === form.type) || NOTIF_TYPES[0];

  const handleSend = async (e) => {
    e.preventDefault();
    if (!form.recipient) { toast('Recipient email is required', 'warning'); return; }
    setSending(true);
    const result = await api.sendNotification({
      type: form.type,
      recipient: form.recipient,
      orderId: form.orderId || undefined,
      message: form.message || `${selected.label} notification`,
    });
    setHistory(prev => [{
      id: Date.now(),
      type: form.type,
      recipient: form.recipient,
      routingKey: selected.routingKey,
      queue: selected.queue,
      sentAt: new Date().toISOString(),
      status: result?.sent ? 'SENT' : 'FAILED',
    }, ...prev.slice(0, 9)]);
    setSending(false);
    toast(`Notification published to RabbitMQ (${selected.routingKey})`, 'success');
  };

  return (
    <div>
      <div className="admin-page-title">Notifications</div>
      <div className="admin-page-subtitle">
        Publish notifications via RabbitMQ TopicExchange → routed to email/sms/push queues
      </div>

      {/* RabbitMQ explanation */}
      <div className="rabbit-info-box" style={{ marginBottom: 20 }}>
        <h4>🐰 RabbitMQ Architecture</h4>
        <p>
          Notifications are published to a <strong>TopicExchange</strong> (<code>shopverse.notifications</code>).
          Routing keys like <code>order.placed</code> are matched by queue bindings:
          <code>order.*</code> → email queue · <code>payment.*</code> → email + SMS · <code>promo.*</code> → push queue.
          DLX (<code>shopverse.dlx</code>) handles failed deliveries with a 24h TTL.
        </p>
      </div>

      {/* RabbitMQ Exchange diagram */}
      <div style={{ background: 'var(--surface)', border: '1.5px solid var(--border)', borderRadius: 12, padding: 20, marginBottom: 24 }}>
        <h3 style={{ fontSize: '1rem', fontWeight: 700, marginBottom: 16 }}>Exchange → Queue Routing</h3>
        <div style={{ display: 'grid', gridTemplateColumns: 'auto 1fr auto', gap: 12, alignItems: 'center', fontSize: '.85rem' }}>
          {/* Exchange */}
          <div style={{ background: '#fff7ed', border: '2px solid #ff6600', borderRadius: 10, padding: '12px 20px', textAlign: 'center', fontWeight: 700 }}>
            <div style={{ fontSize: '.7rem', color: '#ff6600', marginBottom: 2 }}>TOPIC EXCHANGE</div>
            shopverse.notifications
          </div>
          {/* Routing keys */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 6 }}>
            {[
              { key: 'order.*',   queue: 'shopverse.email',  desc: 'Email queue' },
              { key: 'payment.*', queue: 'shopverse.email + shopverse.sms', desc: 'Email + SMS' },
              { key: 'promo.*',   queue: 'shopverse.push',   desc: 'Push queue' },
            ].map(r => (
              <div key={r.key} style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <span style={{ fontFamily: 'monospace', background: '#0f172a', color: '#38bdf8', padding: '2px 8px', borderRadius: 5, fontSize: '.78rem', whiteSpace: 'nowrap' }}>
                  {r.key}
                </span>
                <span style={{ color: 'var(--border)', fontSize: '1.2rem' }}>→</span>
                <span style={{ color: 'var(--text-s)', fontSize: '.82rem' }}>{r.desc}</span>
              </div>
            ))}
          </div>
          {/* Queues */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: 8 }}>
            {['shopverse.email', 'shopverse.sms', 'shopverse.push'].map(q => (
              <div key={q} style={{ background: '#fef3c7', border: '1px solid #fde68a', borderRadius: 6, padding: '5px 12px', fontSize: '.78rem', fontWeight: 600 }}>{q}</div>
            ))}
          </div>
        </div>
      </div>

      {/* Send form */}
      <div className="notif-form" style={{ marginBottom: 24 }}>
        <h3>Send Notification <span className="tech-label tech-rabbit">RabbitMQ</span></h3>
        <form onSubmit={handleSend}>
          <div className="notif-form-grid">
            <div>
              <label>Notification Type</label>
              <select value={form.type} onChange={e => setForm(f => ({ ...f, type: e.target.value }))}>
                {NOTIF_TYPES.map(t => <option key={t.value} value={t.value}>{t.label}</option>)}
              </select>
              <div className="routing-key-display">routing key: {selected.routingKey}</div>
            </div>
            <div>
              <label>Target Queue</label>
              <input readOnly value={selected.queue} style={{ background: 'var(--bg)', color: 'var(--text-s)', cursor: 'not-allowed' }} />
            </div>
            <div>
              <label>Recipient Email</label>
              <input type="email" placeholder="user@example.com" value={form.recipient} onChange={e => setForm(f => ({ ...f, recipient: e.target.value }))} />
            </div>
            <div>
              <label>Order ID (optional)</label>
              <input placeholder="e.g. 1001" value={form.orderId} onChange={e => setForm(f => ({ ...f, orderId: e.target.value }))} />
            </div>
            <div className="full-col">
              <label>Message (optional)</label>
              <textarea rows={2} placeholder="Custom message…" value={form.message} onChange={e => setForm(f => ({ ...f, message: e.target.value }))} />
            </div>
          </div>
          <button type="submit" className="btn-primary" disabled={sending} style={{ marginTop: 12 }}>
            {sending ? 'Sending…' : '📤 Publish to RabbitMQ'}
          </button>
        </form>
      </div>

      {/* History */}
      {history.length > 0 && (
        <div className="admin-table-wrap">
          <div className="admin-table-header"><h3>Sent Notifications</h3></div>
          <table>
            <thead>
              <tr><th>Type</th><th>Recipient</th><th>Routing Key</th><th>Queue</th><th>Sent At</th><th>Status</th></tr>
            </thead>
            <tbody>
              {history.map(h => (
                <tr key={h.id}>
                  <td><strong>{h.type}</strong></td>
                  <td style={{ color: 'var(--text-s)' }}>{h.recipient}</td>
                  <td><code style={{ fontSize: '.8rem', background: '#0f172a', color: '#38bdf8', padding: '1px 6px', borderRadius: 4 }}>{h.routingKey}</code></td>
                  <td style={{ fontSize: '.82rem', color: 'var(--text-s)' }}>{h.queue}</td>
                  <td style={{ fontSize: '.82rem', color: 'var(--text-s)' }}>{new Date(h.sentAt).toLocaleTimeString()}</td>
                  <td><span style={{ background: '#d1fae5', color: '#065f46', padding: '2px 10px', borderRadius: 10, fontSize: '.78rem', fontWeight: 700 }}>{h.status}</span></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </div>
  );
}
