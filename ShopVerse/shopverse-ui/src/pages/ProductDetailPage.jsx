import { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useCart } from '../context/CartContext';

const CATEGORY_EMOJI = {
  electronics:'⚡', furniture:'🪑', lifestyle:'✨',
  accessories:'👜', kitchen:'🍳', clothing:'👕', default:'📦',
};

export default function ProductDetailPage() {
  const { id }    = useParams();
  const navigate  = useNavigate();
  const { addToCart, items } = useCart();
  const [product, setProduct] = useState(null);
  const [qty, setQty]         = useState(1);
  const [added, setAdded]     = useState(false);

  useEffect(() => {
    api.getProduct(id).then(setProduct);
  }, [id]);

  if (!product) return <div className="page"><div className="loading-state">Loading…</div></div>;

  const inCart = items.some(i => i.id === product.id);

  const handleAdd = () => {
    for (let i = 0; i < qty; i++) addToCart(product);
    setAdded(true);
    setTimeout(() => setAdded(false), 2000);
  };

  return (
    <div className="page">
      <button className="back-btn" onClick={() => navigate(-1)}>← Back</button>

      <div className="detail-layout">
        <div className="detail-img-panel">
          <div className="detail-img-placeholder">
            <span className="detail-emoji">
              {CATEGORY_EMOJI[product.category] || CATEGORY_EMOJI.default}
            </span>
          </div>
        </div>

        <div className="detail-info">
          <span className="product-category">{product.category}</span>
          <h1 className="detail-name">{product.name}</h1>
          <p className="detail-desc">{product.description}</p>

          <div className="detail-price">${product.price.toFixed(2)}</div>

          <div className="detail-stock">
            {product.stockQuantity > 10
              ? <span className="in-stock">✓ In Stock ({product.stockQuantity} available)</span>
              : product.stockQuantity > 0
              ? <span className="low-stock">⚠ Only {product.stockQuantity} left</span>
              : <span className="out-stock">✗ Out of Stock</span>}
          </div>

          <div className="qty-row">
            <label className="qty-label">Quantity:</label>
            <div className="qty-control">
              <button onClick={() => setQty(q => Math.max(1, q - 1))}>−</button>
              <span className="qty-value">{qty}</span>
              <button onClick={() => setQty(q => Math.min(product.stockQuantity, q + 1))}>+</button>
            </div>
          </div>

          <div className="detail-actions">
            <button
              className={added ? 'btn-success' : 'btn-primary-lg'}
              onClick={handleAdd}
              disabled={product.stockQuantity === 0}
            >
              {added ? '✓ Added to Cart!' : '🛒 Add to Cart'}
            </button>
            <button
              className="btn-secondary-lg"
              onClick={() => { handleAdd(); navigate('/cart'); }}
              disabled={product.stockQuantity === 0}
            >
              Buy Now
            </button>
          </div>

          <div className="detail-meta">
            <div className="meta-item"><span>🚚</span> Free shipping over $50</div>
            <div className="meta-item"><span>↩️</span> 30-day return policy</div>
            <div className="meta-item"><span>🔒</span> Secure checkout</div>
          </div>
        </div>
      </div>
    </div>
  );
}
