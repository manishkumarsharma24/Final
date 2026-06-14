import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';

export default function AdminDashboard() {
  const navigate = useNavigate();
  const [stats, setStats]   = useState({ orders: 0, customers: 0, products: 12, revenue: 0 });
  const [health, setHealth] = useState(null);
  const [recentOrders, setRecentOrders] = useState([]);

  useEffect(() => {
    Promise.all([
      api.getAllOrders({ size: 5 }),
      api.getAllCustomers({ size: 100 }),
      api.getHealth(),
    ]).then(([orders, customers, h]) => {
      const orderList = orders?.content ?? [];
      const revenue   = orderList.reduce((s, o) => s + (Number(o.total) || 0), 0);
      setStats({
        orders:    orders?.totalElements ?? orderList.length,
        customers: customers?.totalElements ?? (customers?.content?.length ?? 0),
        products:  12,
        revenue,
      });
      setRecentOrders(orderList.slice(0, 5));
      setHealth(h);
    });
  }, []);

  const upCount = health ? Object.values(health.components || {}).filter(c => c.status === 'UP').length : 0;
  const totalServices = health ? Object.keys(health.components || {}).length : 7;

  return (
    <div>
      <div className="admin-page-title">Dashboard</div>
      <div className="admin-page-subtitle">ShopVerse admin overview</div>

      {/* Stats */}
      <div className="stats-grid">
        <div className="stat-card">
          <div className="stat-card-header"><span className="stat-card-label">Total Orders</span><span className="stat-card-icon">🛒</span></div>
          <div className="stat-card-value">{stats.orders}</div>
          <div className="stat-card-sub">All time</div>
        </div>
        <div className="stat-card">
          <div className="stat-card-header"><span className="stat-card-label">Revenue</span><span className="stat-card-icon">💰</span></div>
          <div className="stat-card-value">${stats.revenue.toFixed(0)}</div>
          <div className="stat-card-sub">Total revenue</div>
        </div>
        <div className="stat-card">
          <div className="stat-card-header"><span className="stat-card-label">Customers</span><span className="stat-card-icon">👥</span></div>
          <div className="stat-card-value">{stats.customers}</div>
          <div className="stat-card-sub">Registered</div>
        </div>
        <div className="stat-card">
          <div className="stat-card-header"><span className="stat-card-label">Services</span><span className="stat-card-icon">🖥️</span></div>
          <div className="stat-card-value" style={{ color: upCount === totalServices ? 'var(--success)' : 'var(--danger)' }}>
            {upCount}/{totalServices}
          </div>
          <div className="stat-card-sub">Healthy</div>
        </div>
      </div>

      {/* Tech Stack Info */}
      <div className="kafka-info-box" style={{ marginBottom: 24 }}>
        <h4>⚡ Event-Driven Architecture</h4>
        <p>Order lifecycle events flow through Kafka. Each status change (confirm → ship → deliver) publishes a sealed-interface event to <code>shopverse.orders</code> topic. The consumer logs activity to Cassandra and triggers notifications via RabbitMQ.</p>
        <div className="kafka-event-flow">
          <span className="kafka-event-pill">OrderPlaced</span><span className="arrow">→</span>
          <span className="kafka-event-pill">OrderConfirmed</span><span className="arrow">→</span>
          <span className="kafka-event-pill">OrderShipped</span><span className="arrow">→</span>
          <span className="kafka-event-pill">OrderDelivered</span>
        </div>
      </div>

      {/* Recent Orders */}
      <div className="admin-table-wrap">
        <div className="admin-table-header">
          <h3>Recent Orders</h3>
          <button className="btn-primary" style={{ padding: '8px 16px', fontSize: '.85rem' }} onClick={() => navigate('/admin/orders')}>View All</button>
        </div>
        <table>
          <thead>
            <tr>
              <th>Order ID</th>
              <th>Customer</th>
              <th>Total</th>
              <th>Status</th>
              <th>Date</th>
            </tr>
          </thead>
          <tbody>
            {recentOrders.length ? recentOrders.map(o => (
              <tr key={o.id}>
                <td><strong>#{o.id}</strong></td>
                <td>Customer #{o.customerId}</td>
                <td><strong>${Number(o.total).toFixed(2)}</strong></td>
                <td><span className="status-badge" style={{ background: '#dbeafe', color: '#1d4ed8' }}>{o.status}</span></td>
                <td style={{ color: 'var(--text-s)', fontSize: '.85rem' }}>{new Date(o.createdAt).toLocaleDateString()}</td>
              </tr>
            )) : (
              <tr><td colSpan={5} style={{ textAlign: 'center', color: 'var(--text-s)', padding: 24 }}>No orders yet</td></tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
