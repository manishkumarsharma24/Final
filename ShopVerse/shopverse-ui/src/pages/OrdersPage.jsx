import { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { api } from '../api/client';

const STATUS_COLOR = {
  PENDING: '#f59e0b', CONFIRMED: '#3b82f6', PROCESSING: '#8b5cf6',
  SHIPPED: '#06b6d4', DELIVERED: '#10b981', CANCELLED: '#ef4444', REFUNDED: '#6b7280',
};

export default function OrdersPage() {
  const { user, isLoggedIn } = useAuth();
  const navigate             = useNavigate();
  const [orders, setOrders]  = useState([]);
  const [loading, setLoading]= useState(true);

  useEffect(() => {
    if (!isLoggedIn) { navigate('/login'); return; }
    api.getOrders(user?.customerId ?? 1).then(data => {
      setOrders(Array.isArray(data) ? data : []);
      setLoading(false);
    });
  }, [isLoggedIn]);

  if (loading) return <div className="page"><div className="loading-state">Loading orders…</div></div>;

  return (
    <div className="page">
      <h1 className="page-title">My Orders</h1>

      {orders.length === 0 ? (
        <div className="empty-state">
          <span className="empty-icon">📦</span>
          <p>You haven't placed any orders yet.</p>
          <button className="btn-primary" onClick={() => navigate('/products')}>Start Shopping</button>
        </div>
      ) : (
        <div className="orders-list">
          {orders.map(order => (
            <div key={order.id} className="order-card">
              <div className="order-header">
                <div>
                  <span className="order-id">Order #{order.id}</span>
                  <span className="order-date">
                    {order.createdAt ? new Date(order.createdAt).toLocaleDateString('en-US',
                      { year:'numeric', month:'short', day:'numeric' }) : ''}
                  </span>
                </div>
                <span className="order-status" style={{ background: STATUS_COLOR[order.status] + '22', color: STATUS_COLOR[order.status] }}>
                  {order.status}
                </span>
              </div>

              <div className="order-items">
                {(order.items || []).map((item, i) => (
                  <div key={i} className="order-item-row">
                    <span>{item.quantity}× {item.productName}</span>
                    <span>${(item.unitPrice * item.quantity).toFixed(2)}</span>
                  </div>
                ))}
              </div>

              <div className="order-footer">
                <span className="order-total">
                  Total: <strong>${(order.total ?? order.items?.reduce((s,i) => s + i.unitPrice * i.quantity, 0) ?? 0).toFixed(2)}</strong>
                </span>
                {order.trackingNumber && (
                  <span className="tracking">🚚 Tracking: {order.trackingNumber}</span>
                )}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
