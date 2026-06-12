import { useCart } from '../context/CartContext';
import { useNavigate } from 'react-router-dom';

const CATEGORY_EMOJI = {
  electronics:'⚡', furniture:'🪑', lifestyle:'✨',
  accessories:'👜', kitchen:'🍳', clothing:'👕', default:'📦',
};

export default function CartPage() {
  const { items, removeFromCart, updateQty, clearCart, totalPrice } = useCart();
  const navigate = useNavigate();

  if (items.length === 0) {
    return (
      <div className="page">
        <div className="empty-cart">
          <span className="empty-icon">🛒</span>
          <h2>Your cart is empty</h2>
          <p>Add some products and come back!</p>
          <button className="btn-primary" onClick={() => navigate('/products')}>Browse Products</button>
        </div>
      </div>
    );
  }

  const shipping = totalPrice >= 50 ? 0 : 4.99;
  const tax      = totalPrice * 0.08;
  const grandTotal = totalPrice + shipping + tax;

  return (
    <div className="page">
      <h1 className="page-title">Shopping Cart</h1>
      <div className="cart-layout">
        <div className="cart-items">
          {items.map(item => (
            <div key={item.id} className="cart-item">
              <div className="cart-item-img">
                {CATEGORY_EMOJI[item.category] || CATEGORY_EMOJI.default}
              </div>
              <div className="cart-item-info">
                <p className="cart-item-name">{item.name}</p>
                <p className="cart-item-price">${item.price.toFixed(2)} each</p>
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

          <div className="cart-footer-actions">
            <button className="link-btn" onClick={clearCart}>🗑 Clear cart</button>
            <button className="link-btn" onClick={() => navigate('/products')}>← Continue shopping</button>
          </div>
        </div>

        <div className="cart-summary">
          <h2 className="summary-title">Order Summary</h2>
          <div className="summary-rows">
            <div className="summary-row">
              <span>Subtotal ({items.reduce((s,i) => s+i.quantity, 0)} items)</span>
              <span>${totalPrice.toFixed(2)}</span>
            </div>
            <div className="summary-row">
              <span>Shipping</span>
              <span>{shipping === 0 ? <span className="free-badge">FREE</span> : `$${shipping.toFixed(2)}`}</span>
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
            <p className="free-ship-hint">
              Add ${(50 - totalPrice).toFixed(2)} more for free shipping!
            </p>
          )}

          <button className="btn-primary-full" onClick={() => navigate('/checkout')}>
            Proceed to Checkout →
          </button>
        </div>
      </div>
    </div>
  );
}
