import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { useSessionId } from '../hooks/useSessionId';

const STEPS = ['Shipping', 'Payment', 'Confirm'];

export default function CheckoutPage() {
  const navigate = useNavigate();
  const { items, totalPrice, clearCart } = useCart();
  const { user } = useAuth();
  const { add: toast } = useToast();
  const sessionId = useSessionId();
  const [step, setStep] = useState(0);
  const [placing, setPlacing] = useState(false);
  const [orderId, setOrderId] = useState(null);

  const [shipping, setShipping] = useState({
    firstName: user?.name?.split(' ')[0] || '',
    lastName:  user?.name?.split(' ')[1] || '',
    email:     user?.email || '',
    address:   '',
    city:      '',
    state:     '',
    zip:       '',
    country:   'US',
  });

  const [payment, setPayment] = useState({
    cardName:   '',
    cardNumber: '',
    expiry:     '',
    cvv:        '',
  });

  if (!items.length && !orderId) {
    navigate('/cart');
    return null;
  }

  const shippingCost = totalPrice >= 100 ? 0 : 9.99;
  const tax = totalPrice * 0.08;
  const grandTotal = totalPrice + shippingCost + tax;

  const handlePlaceOrder = async () => {
    setPlacing(true);
    // Track checkout event → Kafka analytics topic
    api.trackCheckout({ sessionId, items: items.map(i => ({ productId: i.id, quantity: i.quantity })), total: grandTotal });

    const payload = {
      customerId: user?.customerId,
      street:     shipping.address,
      city:       shipping.city,
      state:      shipping.state || '',
      postalCode: shipping.zip,
      country:    shipping.country,
      items: items.map(i => ({ productId: i.id, quantity: i.quantity })),
    };
    try {
      const order = await api.placeOrder(payload);
      setOrderId(order.id);
      clearCart();
      // Track conversion → Kafka
      api.trackConverted({ sessionId, orderId: order.id, total: grandTotal });
      toast('Order placed successfully!', 'success');
    } catch {
      toast('Failed to place order. Please try again.', 'error');
      setPlacing(false);
    }
  };

  if (orderId) {
    return (
      <div className="page">
        <div className="success-page">
          <span className="success-icon">🎉</span>
          <h1>Order Placed!</h1>
          <p className="success-sub">
            Your order has been placed and a <strong>Kafka event (OrderPlaced)</strong> has been published to the <code>shopverse.orders</code> topic.
          </p>
          <div className="order-summary-card">
            <div><strong>Order ID:</strong> #{orderId}</div>
            <div><strong>Total:</strong> ${grandTotal.toFixed(2)}</div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
              <strong>Status:</strong>
              <span className="status-badge confirmed">Confirmed</span>
            </div>
            <div style={{ fontSize: '.82rem', color: 'var(--text-s)', marginTop: 4 }}>
              📨 Kafka → <code>shopverse.orders</code> → Consumer → Cassandra activity log
            </div>
          </div>
          <div className="success-actions">
            <button className="btn-primary-lg" onClick={() => navigate('/orders')}>View Orders</button>
            <button className="btn-secondary-lg" onClick={() => navigate('/products')}>Keep Shopping</button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="page">
      <h1 className="page-title">Checkout</h1>

      {/* Steps */}
      <div className="checkout-steps">
        {STEPS.map((s, i) => (
          <div key={s} className={`step ${i === step ? 'active' : i < step ? 'done' : ''}`}>
            <div className="step-num">{i < step ? '✓' : i + 1}</div>
            <div className="step-label">{s}</div>
            {i < STEPS.length - 1 && <div style={{ flex: 1, height: 2, background: i < step ? 'var(--success)' : 'var(--border)', margin: '0 12px' }} />}
          </div>
        ))}
      </div>

      <div className="checkout-layout">
        <div className="checkout-form-panel">
          {/* Step 0: Shipping */}
          {step === 0 && (
            <div className="checkout-form">
              <h2>Shipping Address</h2>
              <div className="form-row">
                <div>
                  <label>First Name</label>
                  <input value={shipping.firstName} onChange={e => setShipping(s => ({ ...s, firstName: e.target.value }))} />
                </div>
                <div>
                  <label>Last Name</label>
                  <input value={shipping.lastName} onChange={e => setShipping(s => ({ ...s, lastName: e.target.value }))} />
                </div>
              </div>
              <div>
                <label>Email</label>
                <input type="email" value={shipping.email} onChange={e => setShipping(s => ({ ...s, email: e.target.value }))} />
              </div>
              <div>
                <label>Address</label>
                <input value={shipping.address} onChange={e => setShipping(s => ({ ...s, address: e.target.value }))} />
              </div>
              <div className="form-row">
                <div>
                  <label>City</label>
                  <input value={shipping.city} onChange={e => setShipping(s => ({ ...s, city: e.target.value }))} />
                </div>
                <div>
                  <label>State</label>
                  <input placeholder="CA" value={shipping.state} onChange={e => setShipping(s => ({ ...s, state: e.target.value }))} />
                </div>
                <div>
                  <label>ZIP Code</label>
                  <input value={shipping.zip} onChange={e => setShipping(s => ({ ...s, zip: e.target.value }))} />
                </div>
              </div>
              <div className="form-row-btns">
                <button className="btn-primary-lg" onClick={() => setStep(1)}>Continue to Payment →</button>
              </div>
            </div>
          )}

          {/* Step 1: Payment */}
          {step === 1 && (
            <div className="checkout-form">
              <h2>Payment Details</h2>
              <div className="card-icons">💳 Visa / Mastercard / Amex (Demo — no real charges)</div>
              <div>
                <label>Name on Card</label>
                <input value={payment.cardName} onChange={e => setPayment(p => ({ ...p, cardName: e.target.value }))} />
              </div>
              <div>
                <label>Card Number</label>
                <input placeholder="4242 4242 4242 4242" maxLength={19} value={payment.cardNumber}
                  onChange={e => setPayment(p => ({ ...p, cardNumber: e.target.value }))} />
              </div>
              <div className="form-row">
                <div>
                  <label>Expiry</label>
                  <input placeholder="MM/YY" maxLength={5} value={payment.expiry}
                    onChange={e => setPayment(p => ({ ...p, expiry: e.target.value }))} />
                </div>
                <div>
                  <label>CVV</label>
                  <input placeholder="123" maxLength={4} value={payment.cvv}
                    onChange={e => setPayment(p => ({ ...p, cvv: e.target.value }))} />
                </div>
              </div>
              <div className="secure-note">🔒 Secured with JWT + TLS. Card data is not stored.</div>
              <div className="form-row-btns">
                <button className="btn-ghost" onClick={() => setStep(0)}>← Back</button>
                <button className="btn-primary-lg" onClick={() => setStep(2)}>Review Order →</button>
              </div>
            </div>
          )}

          {/* Step 2: Confirm */}
          {step === 2 && (
            <div className="checkout-form">
              <h2>Confirm Order</h2>
              <div style={{ background: 'var(--bg)', borderRadius: 10, padding: 16, fontSize: '.9rem' }}>
                <div><strong>Ship to:</strong> {shipping.firstName} {shipping.lastName}, {shipping.address}, {shipping.city} {shipping.zip}</div>
                <div style={{ marginTop: 6 }}><strong>Card:</strong> **** **** **** {payment.cardNumber.slice(-4) || '****'}</div>
              </div>
              <div style={{ background: '#fef3c7', border: '1px solid #fde68a', borderRadius: 10, padding: 14, fontSize: '.82rem', color: '#92400e', marginTop: 4 }}>
                🚀 <strong>What happens next:</strong> Placing this order publishes an <strong>OrderPlaced</strong> event to the Kafka <code>shopverse.orders</code> topic. The consumer logs it to Cassandra and triggers inventory + notification events.
              </div>
              <div className="form-row-btns">
                <button className="btn-ghost" onClick={() => setStep(1)}>← Back</button>
                <button className="btn-success" onClick={handlePlaceOrder} disabled={placing}>
                  {placing ? 'Placing…' : '✓ Place Order'}
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Order Summary */}
        <div className="checkout-summary">
          <h3>Your Order</h3>
          {items.map(i => (
            <div key={i.id} className="checkout-item">
              <span className="checkout-item-qty">{i.quantity}×</span>
              <span className="checkout-item-name">{i.name}</span>
              <span className="checkout-item-price">${(i.price * i.quantity).toFixed(2)}</span>
            </div>
          ))}
          <div className="summary-divider" style={{ margin: '12px 0' }} />
          <div className="summary-row"><span>Subtotal</span><span>${totalPrice.toFixed(2)}</span></div>
          <div className="summary-row"><span>Shipping</span><span>{shippingCost === 0 ? 'FREE' : `$${shippingCost.toFixed(2)}`}</span></div>
          <div className="summary-row"><span>Tax (8%)</span><span>${tax.toFixed(2)}</span></div>
          <div className="summary-divider" style={{ margin: '12px 0' }} />
          <div className="summary-row total-row"><span>Total</span><strong>${grandTotal.toFixed(2)}</strong></div>
        </div>
      </div>
    </div>
  );
}
