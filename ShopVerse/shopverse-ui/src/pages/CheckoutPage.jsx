import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { api } from '../api/client';

export default function CheckoutPage() {
  const { items, totalPrice, clearCart } = useCart();
  const { user, isLoggedIn }            = useAuth();
  const navigate                         = useNavigate();
  const [step, setStep]                  = useState(1);      // 1=address 2=payment 3=confirm
  const [loading, setLoading]            = useState(false);
  const [order, setOrder]                = useState(null);
  const [addr, setAddr]                  = useState({ street:'', city:'', state:'', postalCode:'', country:'US' });
  const [pay, setPay]                    = useState({ card:'', expiry:'', cvv:'', name:'' });

  const shipping = totalPrice >= 50 ? 0 : 4.99;
  const tax      = totalPrice * 0.08;
  const grandTotal = totalPrice + shipping + tax;

  if (items.length === 0 && !order) {
    navigate('/cart');
    return null;
  }

  const handleAddrNext = (e) => {
    e.preventDefault();
    setStep(2);
  };

  const handlePlaceOrder = async (e) => {
    e.preventDefault();
    setLoading(true);
    try {
      const payload = {
        customerId: user?.customerId ?? 1,
        street: addr.street, city: addr.city, state: addr.state,
        postalCode: addr.postalCode, country: addr.country,
        items: items.map(i => ({ productId: i.id, quantity: i.quantity })),
      };
      const result = await api.placeOrder(payload);
      setOrder(result);
      clearCart();
      setStep(3);
    } catch (err) {
      alert('Order failed. Please try again.');
    }
    setLoading(false);
  };

  if (step === 3 && order) {
    return (
      <div className="page">
        <div className="success-page">
          <div className="success-icon">🎉</div>
          <h1>Order Placed Successfully!</h1>
          <p className="success-sub">Your order <strong>#{order.id}</strong> is confirmed.</p>
          <div className="order-summary-card">
            <p>Status: <span className="status-badge confirmed">{order.status}</span></p>
            <p>Total: <strong>${grandTotal.toFixed(2)}</strong></p>
            <p>Items: {order.items?.length ?? items.length} products</p>
          </div>
          <div className="success-actions">
            <button className="btn-primary" onClick={() => navigate('/orders')}>View My Orders</button>
            <button className="btn-ghost" onClick={() => navigate('/products')}>Continue Shopping</button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="page">
      <h1 className="page-title">Checkout</h1>

      {/* Progress */}
      <div className="checkout-steps">
        {['Shipping', 'Payment', 'Confirm'].map((s, i) => (
          <div key={s} className={`step ${step > i + 1 ? 'done' : ''} ${step === i + 1 ? 'active' : ''}`}>
            <div className="step-num">{step > i + 1 ? '✓' : i + 1}</div>
            <span className="step-label">{s}</span>
          </div>
        ))}
      </div>

      <div className="checkout-layout">
        <div className="checkout-form-panel">
          {step === 1 && (
            <form onSubmit={handleAddrNext} className="checkout-form">
              <h2>Shipping Address</h2>
              <label>Street Address</label>
              <input required value={addr.street} onChange={e => setAddr({...addr, street: e.target.value})} placeholder="123 Main St" />
              <div className="form-row">
                <div>
                  <label>City</label>
                  <input required value={addr.city} onChange={e => setAddr({...addr, city: e.target.value})} placeholder="New York" />
                </div>
                <div>
                  <label>State</label>
                  <input value={addr.state} onChange={e => setAddr({...addr, state: e.target.value})} placeholder="NY" />
                </div>
              </div>
              <div className="form-row">
                <div>
                  <label>Postal Code</label>
                  <input required value={addr.postalCode} onChange={e => setAddr({...addr, postalCode: e.target.value})} placeholder="10001" />
                </div>
                <div>
                  <label>Country</label>
                  <select value={addr.country} onChange={e => setAddr({...addr, country: e.target.value})}>
                    <option value="US">United States</option>
                    <option value="GB">United Kingdom</option>
                    <option value="IN">India</option>
                    <option value="CA">Canada</option>
                    <option value="DE">Germany</option>
                  </select>
                </div>
              </div>
              <button type="submit" className="btn-primary-full">Continue to Payment →</button>
            </form>
          )}

          {step === 2 && (
            <form onSubmit={handlePlaceOrder} className="checkout-form">
              <h2>Payment Details</h2>
              <div className="card-icons">💳 Visa  Mastercard  Amex</div>
              <label>Name on Card</label>
              <input required value={pay.name} onChange={e => setPay({...pay, name: e.target.value})} placeholder="John Doe" />
              <label>Card Number</label>
              <input required value={pay.card} onChange={e => setPay({...pay, card: e.target.value})} placeholder="4242 4242 4242 4242" maxLength={19} />
              <div className="form-row">
                <div>
                  <label>Expiry</label>
                  <input required value={pay.expiry} onChange={e => setPay({...pay, expiry: e.target.value})} placeholder="MM/YY" maxLength={5} />
                </div>
                <div>
                  <label>CVV</label>
                  <input required value={pay.cvv} onChange={e => setPay({...pay, cvv: e.target.value})} placeholder="123" maxLength={4} type="password" />
                </div>
              </div>
              <div className="secure-note">🔒 Your payment is encrypted and secure</div>
              <div className="form-row-btns">
                <button type="button" className="btn-ghost" onClick={() => setStep(1)}>← Back</button>
                <button type="submit" className="btn-primary-full" disabled={loading}>
                  {loading ? 'Placing Order…' : `Place Order — $${grandTotal.toFixed(2)}`}
                </button>
              </div>
            </form>
          )}
        </div>

        {/* Order summary sidebar */}
        <div className="checkout-summary">
          <h3>Your Order</h3>
          {items.map(i => (
            <div key={i.id} className="checkout-item">
              <span className="checkout-item-qty">{i.quantity}×</span>
              <span className="checkout-item-name">{i.name}</span>
              <span className="checkout-item-price">${(i.price * i.quantity).toFixed(2)}</span>
            </div>
          ))}
          <div className="summary-divider" />
          <div className="summary-row"><span>Subtotal</span><span>${totalPrice.toFixed(2)}</span></div>
          <div className="summary-row"><span>Shipping</span><span>{shipping === 0 ? 'FREE' : `$${shipping.toFixed(2)}`}</span></div>
          <div className="summary-row"><span>Tax</span><span>${tax.toFixed(2)}</span></div>
          <div className="summary-row total-row"><span>Total</span><span>${grandTotal.toFixed(2)}</span></div>
        </div>
      </div>
    </div>
  );
}
