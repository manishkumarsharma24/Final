import { useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';

const EMOJI_MAP = { electronics: '💻', furniture: '🪑', kitchen: '🍳', clothing: '👕', lifestyle: '🌿', accessories: '👜' };

export default function CartPage() {
  const { items, removeFromCart, updateQty, clearCart, totalPrice, totalItems } = useCart();
  const { isLoggedIn } = useAuth();
  const navigate = useNavigate();

  const shipping = totalPrice >= 100 ? 0 : 9.99;
  const tax = totalPrice * 0.08;
  const grandTotal = totalPrice + shipping + tax;

  if (items.length === 0) {
    return (
      <div className="page">
        <div className="empty-cart">
          <span className="empty-icon">🛒</span>
          <h2>Your cart is empty</h2>
          <p>Looks like you haven't added anything yet</p>
          <button className="btn-primary-lg" onClick={() => navigate('/products')}>Start Shopping</button>
        </div>
      </div>
    );
  }

  return (
    <div className="page">
      <h1 className="page-title">Shopping Cart ({totalItems} item{totalItems !== 1 ? 's' : ''})</h1>
      <div className="cart-layout">

        {/* Items */}
        <div>
          <div className="cart-items">
            {items.map(item => (
              <div key={item.id} className="cart-item">
                <div className="cart-item-img">{EMOJI_MAP[item.category] || '📦'}</div>
                <div>
                  <div className="cart-item-name">{item.name}</div>
                  <div className="cart-item-price">${item.price?.toFixed(2)} each</div>
                </div>
                <div className="cart-item-controls">
                  <div className="qty-control">
                    <button onClick={() => updateQty(item.id, item.quantity - 1)}>−</button>
                    <span className="qty-value">{item.quantity}</span>
                    <button onClick={() => updateQty(item.id, item.quantity + 1)}>+</button>
                  </div>
                  <span className="cart-item-subtotal">${(item.price * item.quantity).toFixed(2)}</span>
                  <button className="remove-btn" onClick={() => removeFromCart(item.id)}>✕</button>
                </div>
              </div>
            ))}
          </div>
          <div className="cart-footer-actions">
            <button className="btn-ghost" onClick={() => navigate('/products')}>← Continue Shopping</button>
            <button className="link-btn" onClick={clearCart}>🗑 Clear Cart</button>
          </div>
        </div>

        {/* Summary */}
        <div className="cart-summary">
          <h3 className="summary-title">Order Summary</h3>
          <div className="summary-rows">
            <div className="summary-row">
              <span>Subtotal</span>
              <span>${totalPrice.toFixed(2)}</span>
            </div>
            <div className="summary-row">
              <span>Shipping</span>
              {shipping === 0
                ? <span className="free-badge">FREE</span>
                : <span>${shipping.toFixed(2)}</span>
              }
            </div>
            <div className="summary-row">
              <span>Tax (8%)</span>
              <span>${tax.toFixed(2)}</span>
            </div>
            <div className="summary-divider" />
            <div className="summary-row total-row">
              <span>Total</span>
              <span>${grandTotal.toFixed(2)}</span>
            </div>
          </div>

          {shipping > 0 && (
            <div className="free-ship-hint">
              Add ${(100 - totalPrice).toFixed(2)} more for free shipping!
            </div>
          )}

          <button
            className="btn-primary-full"
            onClick={() => isLoggedIn ? navigate('/checkout') : navigate('/login')}
          >
            {isLoggedIn ? 'Proceed to Checkout' : 'Sign in to Checkout'}
          </button>
        </div>
      </div>
    </div>
  );
}
