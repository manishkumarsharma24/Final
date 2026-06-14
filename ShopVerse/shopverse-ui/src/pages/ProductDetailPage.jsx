import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { useSessionId } from '../hooks/useSessionId';
import StarRating from '../components/StarRating';
import ProductCard from '../components/ProductCard';

const EMOJI_MAP = { electronics: '💻', furniture: '🪑', kitchen: '🍳', clothing: '👕', lifestyle: '🌿', accessories: '👜' };

export default function ProductDetailPage() {
  const { id } = useParams();
  const navigate = useNavigate();
  const { addToCart } = useCart();
  const { user, isLoggedIn } = useAuth();
  const { add: toast } = useToast();
  const sessionId = useSessionId();

  const [product, setProduct]   = useState(null);
  const [reviews, setReviews]   = useState([]);
  const [recs, setRecs]         = useState([]);
  const [qty, setQty]           = useState(1);
  const [loading, setLoading]   = useState(true);
  const [added, setAdded]       = useState(false);

  // Review form
  const [rating, setRating]     = useState(0);
  const [comment, setComment]   = useState('');
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    setLoading(true);
    setAdded(false);
    Promise.all([
      api.getProduct(id),
      api.getReviews(id),
      api.getRecommendations(id),
    ]).then(([p, r, rec]) => {
      setProduct(p);
      setReviews(Array.isArray(r) ? r : []);
      setRecs(Array.isArray(rec) ? rec.slice(0, 4) : (rec?.content ?? []).slice(0, 4));
      setLoading(false);
      if (p) api.trackView({ sessionId, productId: p.id, productName: p.name, category: p.category });
    });
  }, [id, sessionId]);

  if (loading) return <div className="page"><div className="loading-state">Loading product…</div></div>;
  if (!product) return <div className="page"><div className="empty-state"><span className="empty-icon">❌</span><h2>Product not found</h2></div></div>;

  const emoji = EMOJI_MAP[product.category] || '📦';
  const inStock = product.stockQuantity > 0;
  const lowStock = product.stockQuantity > 0 && product.stockQuantity <= 5;

  const handleAdd = () => {
    addToCart({ ...product, quantity: qty });
    setAdded(true);
    toast(`${product.name} added to cart`, 'success');
    api.trackAddToCart({ sessionId, productId: product.id, productName: product.name, price: product.price, quantity: qty });
    setTimeout(() => setAdded(false), 2000);
  };

  const handleSubmitReview = async (e) => {
    e.preventDefault();
    if (!rating) { toast('Please select a star rating', 'warning'); return; }
    setSubmitting(true);
    const newReview = await api.submitReview(id, {
      customerId: user?.customerId,
      rating,
      title: comment.slice(0, 120) || 'Review',
      body: comment,
      tags: [],
      verified: false,
    });
    setReviews(prev => [newReview, ...prev]);
    setRating(0);
    setComment('');
    setSubmitting(false);
    toast('Review submitted!', 'success');
  };

  const avgRating = reviews.length ? (reviews.reduce((s, r) => s + r.rating, 0) / reviews.length).toFixed(1) : null;

  return (
    <div className="page">
      <button className="back-btn" onClick={() => navigate(-1)}>← Back</button>

      <div className="detail-layout">
        {/* Image */}
        <div className="detail-img-panel">
          <span className="detail-emoji">{emoji}</span>
        </div>

        {/* Info */}
        <div>
          <span className="product-category">{product.category}</span>
          <h1 className="detail-name">{product.name}</h1>

          {reviews.length > 0 && (
            <div style={{ display: 'flex', alignItems: 'center', gap: 8, marginBottom: 12 }}>
              <StarRating value={Math.round(avgRating)} readOnly size={18} />
              <span style={{ fontSize: '.85rem', color: 'var(--text-s)' }}>{avgRating} ({reviews.length} review{reviews.length !== 1 ? 's' : ''})</span>
            </div>
          )}

          <p className="detail-desc">{product.description}</p>
          <div className="detail-price">${product.price?.toFixed(2)}</div>

          <div className="detail-stock">
            {!inStock
              ? <span className="out-stock">Out of Stock</span>
              : lowStock
              ? <span className="low-stock">⚠ Only {product.stockQuantity} left</span>
              : <span className="in-stock">✓ In Stock ({product.stockQuantity} available)</span>
            }
          </div>

          {inStock && (
            <div className="qty-row">
              <span className="qty-label">Qty</span>
              <div className="qty-control">
                <button onClick={() => setQty(q => Math.max(1, q - 1))}>−</button>
                <span className="qty-value">{qty}</span>
                <button onClick={() => setQty(q => Math.min(product.stockQuantity, q + 1))}>+</button>
              </div>
            </div>
          )}

          <div className="detail-actions">
            <button className={`btn-primary-lg ${added ? 'btn-success' : ''}`} onClick={handleAdd} disabled={!inStock || added}
              style={added ? { background: 'var(--success)' } : {}}>
              {added ? '✓ Added' : '🛒 Add to Cart'}
            </button>
            <button className="btn-secondary-lg" onClick={() => { handleAdd(); navigate('/cart'); }}>Buy Now</button>
          </div>

          <div className="detail-meta">
            <div className="meta-item">🚚 Free shipping on orders over $100</div>
            <div className="meta-item">🔄 30-day return policy</div>
            <div className="meta-item">🔒 Secure checkout with JWT auth</div>
          </div>
        </div>
      </div>

      {/* Reviews — stored in MongoDB */}
      <div className="reviews-section">
        <h2>
          Customer Reviews
          <span className="tech-label tech-mongo">MongoDB</span>
        </h2>

        {reviews.length > 0 && (
          <div className="review-avg">
            <span className="review-avg-val">{avgRating}</span>
            <div>
              <StarRating value={Math.round(avgRating)} readOnly size={20} />
              <div className="review-avg-sub">{reviews.length} review{reviews.length !== 1 ? 's' : ''}</div>
            </div>
          </div>
        )}

        <div className="reviews-list">
          {reviews.map(r => (
            <div key={r.id} className="review-card">
              <div className="review-header">
                <div>
                  <span className="review-author">{r.customer?.name || r.customerName || 'Anonymous'}</span>
                  <StarRating value={r.rating} readOnly size={14} />
                </div>
                <span className="review-date">{new Date(r.createdAt).toLocaleDateString()}</span>
              </div>
              {(r.body || r.comment) && <p className="review-comment">{r.body || r.comment}</p>}
            </div>
          ))}
          {reviews.length === 0 && <p style={{ color: 'var(--text-s)' }}>No reviews yet. Be the first!</p>}
        </div>

        {isLoggedIn ? (
          <div className="review-form">
            <h3>Write a Review</h3>
            <form onSubmit={handleSubmitReview}>
              <div>Your rating</div>
              <StarRating value={rating} onChange={setRating} size={28} />
              <textarea
                placeholder="Share your experience…"
                value={comment}
                onChange={e => setComment(e.target.value)}
              />
              <button type="submit" className="btn-primary" disabled={submitting}>
                {submitting ? 'Submitting…' : 'Submit Review'}
              </button>
            </form>
          </div>
        ) : (
          <div style={{ color: 'var(--text-s)', fontSize: '.9rem' }}>
            <button className="btn-ghost" onClick={() => navigate('/login')}>Sign in to write a review</button>
          </div>
        )}
      </div>

      {/* Recommendations — powered by Neo4j */}
      {recs.length > 0 && (
        <div className="recommendations-section">
          <h2>
            You Might Also Like
            <span className="tech-label tech-neo4j">Neo4j Graph</span>
          </h2>
          <div className="recommendations-label">🕸️ Graph-based recommendations</div>
          <div className="recommendations-grid">
            {recs.map(p => <ProductCard key={p.id} product={p} onAdd={(prod) => { addToCart(prod); toast(`${prod.name} added`, 'success'); }} />)}
          </div>
        </div>
      )}
    </div>
  );
}
