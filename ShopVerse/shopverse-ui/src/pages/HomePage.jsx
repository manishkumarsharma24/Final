import { useNavigate } from 'react-router-dom';
import { useState, useEffect } from 'react';
import { api } from '../api/client';
import ProductCard from '../components/ProductCard';

const CATEGORIES = [
  { key: 'electronics', label: 'Electronics', emoji: '⚡' },
  { key: 'clothing',    label: 'Clothing',    emoji: '👕' },
  { key: 'kitchen',     label: 'Kitchen',     emoji: '🍳' },
  { key: 'furniture',   label: 'Furniture',   emoji: '🪑' },
  { key: 'accessories', label: 'Accessories', emoji: '👜' },
  { key: 'lifestyle',   label: 'Lifestyle',   emoji: '✨' },
];

export default function HomePage() {
  const navigate = useNavigate();
  const [featured, setFeatured] = useState([]);

  useEffect(() => {
    api.getProducts({ size: 4 }).then(data => {
      const items = data?.content ?? data ?? [];
      setFeatured(items.slice(0, 4));
    });
  }, []);

  return (
    <div className="page home-page">
      {/* Hero */}
      <section className="hero">
        <div className="hero-content">
          <h1 className="hero-title">
            Everything you need,<br />
            <span className="hero-accent">delivered fast.</span>
          </h1>
          <p className="hero-subtitle">
            Discover thousands of products across electronics, fashion, home & more.
          </p>
          <div className="hero-actions">
            <button className="btn-primary" onClick={() => navigate('/products')}>
              Shop Now →
            </button>
            <button className="btn-ghost" onClick={() => navigate('/products?category=electronics')}>
              Browse Electronics
            </button>
          </div>
        </div>
        <div className="hero-visual">
          <div className="hero-emoji-grid">
            {['⚡','📱','🎧','💻','🖥️','⌨️','🖱️','📷'].map((e, i) => (
              <span key={i} className="hero-emoji" style={{ animationDelay: `${i * 0.15}s` }}>{e}</span>
            ))}
          </div>
        </div>
      </section>

      {/* Categories */}
      <section className="section">
        <h2 className="section-title">Shop by Category</h2>
        <div className="category-grid">
          {CATEGORIES.map(cat => (
            <button
              key={cat.key}
              className="category-card"
              onClick={() => navigate(`/products?category=${cat.key}`)}
            >
              <span className="cat-emoji">{cat.emoji}</span>
              <span className="cat-label">{cat.label}</span>
            </button>
          ))}
        </div>
      </section>

      {/* Featured */}
      {featured.length > 0 && (
        <section className="section">
          <div className="section-header">
            <h2 className="section-title">Featured Products</h2>
            <button className="link-btn" onClick={() => navigate('/products')}>View all →</button>
          </div>
          <div className="product-grid">
            {featured.map(p => <ProductCard key={p.id} product={p} />)}
          </div>
        </section>
      )}

      {/* Banner */}
      <section className="promo-banner">
        <div className="promo-content">
          <h3>🚀 Free shipping on orders over $50</h3>
          <p>Use code <strong>SHOPVERSE10</strong> for 10% off your first order</p>
        </div>
        <button className="btn-primary" onClick={() => navigate('/products')}>
          Start Shopping
        </button>
      </section>
    </div>
  );
}
