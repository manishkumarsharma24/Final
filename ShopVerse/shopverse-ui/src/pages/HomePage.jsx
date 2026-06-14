import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../api/client';
import ProductCard from '../components/ProductCard';
import SkeletonCard from '../components/SkeletonCard';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { useToast } from '../context/ToastContext';
import { useSessionId } from '../hooks/useSessionId';

const CATEGORIES = [
  { key: 'electronics', emoji: '💻', label: 'Electronics' },
  { key: 'furniture',   emoji: '🪑', label: 'Furniture'   },
  { key: 'kitchen',     emoji: '🍳', label: 'Kitchen'     },
  { key: 'clothing',    emoji: '👕', label: 'Clothing'    },
  { key: 'lifestyle',   emoji: '🌿', label: 'Lifestyle'   },
  { key: 'accessories', emoji: '👜', label: 'Accessories' },
];

export default function HomePage() {
  const navigate = useNavigate();
  const { addToCart } = useCart();
  const { user } = useAuth();
  const { add: toast } = useToast();
  const sessionId = useSessionId();
  const [featured, setFeatured] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    api.getProducts({ size: 8 }).then(data => {
      setFeatured(data?.content ?? data ?? []);
      setLoading(false);
    });
  }, []);

  const handleAdd = (product) => {
    addToCart(product);
    toast(`${product.name} added to cart`, 'success');
    api.trackAddToCart({ sessionId, productId: product.id, productName: product.name, price: product.price });
  };

  const handleCategory = (cat) => {
    navigate(`/products?category=${cat}`);
  };

  return (
    <div className="page">
      {/* Hero */}
      <div className="hero">
        <div className="hero-content">
          <span className="hero-badge">🛍️ Full-Stack E-Commerce Demo</span>
          <h1>Shop the Future <span>Today</span></h1>
          <p className="hero-sub">
            Powered by Spring Boot · Kafka · Redis · Elasticsearch · Neo4j · MongoDB · Cassandra
          </p>
          <div className="hero-actions">
            <button className="hero-btn-primary" onClick={() => navigate('/products')}>
              Browse Products
            </button>
            <button className="hero-btn-ghost" onClick={() => navigate(user ? '/orders' : '/login')}>
              {user ? 'My Orders' : 'Sign In'}
            </button>
          </div>
        </div>
        <div className="hero-visual">
          {['💻', '🎧', '🪑', '⌨️'].map(e => (
            <div key={e} className="hero-emoji-card">{e}</div>
          ))}
        </div>
      </div>

      {/* Categories */}
      <h2 className="section-title">Shop by Category</h2>
      <div className="categories-grid">
        {CATEGORIES.map(c => (
          <div key={c.key} className="category-card" onClick={() => handleCategory(c.key)}>
            <span className="cat-emoji">{c.emoji}</span>
            <span className="cat-name">{c.label}</span>
          </div>
        ))}
      </div>

      {/* Featured */}
      <h2 className="section-title">Featured Products</h2>
      <div className="products-grid">
        {loading
          ? Array.from({ length: 8 }, (_, i) => <SkeletonCard key={i} />)
          : featured.map(p => (
              <ProductCard key={p.id} product={p} onAdd={handleAdd} />
            ))
        }
      </div>

      {/* Promo */}
      <div className="promo-banner">
        <div className="promo-text">
          <h2>🚀 Free shipping on orders over $100</h2>
          <p>Use code <strong>SHOPVERSE</strong> at checkout for 10% off your first order</p>
        </div>
        <button className="promo-btn" onClick={() => navigate('/products')}>Shop Now</button>
      </div>
    </div>
  );
}
