import { useState, useEffect, useCallback } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../api/client';
import ProductCard from '../components/ProductCard';
import SkeletonCard from '../components/SkeletonCard';
import { useCart } from '../context/CartContext';
import { useToast } from '../context/ToastContext';
import { useSessionId } from '../hooks/useSessionId';

const CATEGORIES = ['electronics', 'furniture', 'kitchen', 'clothing', 'lifestyle', 'accessories'];

export default function ProductsPage() {
  const [params, setParams] = useSearchParams();
  const { addToCart } = useCart();
  const { add: toast } = useToast();
  const sessionId = useSessionId();

  const [products, setProducts] = useState([]);
  const [total, setTotal] = useState(0);
  const [page, setPage] = useState(0);
  const [loading, setLoading] = useState(true);
  const [isSearch, setIsSearch] = useState(false);

  const category = params.get('category') || '';
  const keyword  = params.get('q') || '';
  const [localKw, setLocalKw] = useState(keyword);

  const load = useCallback(async (cat, kw, pg) => {
    setLoading(true);
    setIsSearch(false);
    let data;

    if (kw && kw.trim()) {
      // Elasticsearch search
      data = await api.search(kw.trim());
      if (Array.isArray(data)) {
        setProducts(data);
        setTotal(data.length);
      } else {
        setProducts(data?.content ?? []);
        setTotal(data?.totalElements ?? 0);
      }
      setIsSearch(true);
      api.trackSearch({ sessionId, query: kw, resultsCount: Array.isArray(data) ? data.length : data?.totalElements });
    } else {
      data = await api.getProducts({ category: cat, page: pg, size: 12 });
      setProducts(data?.content ?? []);
      setTotal(data?.totalElements ?? 0);
    }
    setLoading(false);
  }, [sessionId]);

  useEffect(() => {
    setPage(0);
    setLocalKw(keyword);
    load(category, keyword, 0);
  }, [category, keyword]); // eslint-disable-line

  const handleSearch = (e) => {
    e.preventDefault();
    setParams(localKw ? { q: localKw } : {});
  };

  const handleCategory = (cat) => {
    setParams(cat ? { category: cat } : {});
    setLocalKw('');
  };

  const handleAdd = (product) => {
    addToCart(product);
    toast(`${product.name} added to cart`, 'success');
    api.trackAddToCart({ sessionId, productId: product.id, productName: product.name, price: product.price });
  };

  const totalPages = Math.ceil(total / 12);

  return (
    <div className="page">
      <h1 className="page-title">
        {isSearch ? (
          <>Search Results <span className="es-badge">⚡ Elasticsearch</span></>
        ) : category ? (
          <span style={{ textTransform: 'capitalize' }}>{category}</span>
        ) : 'All Products'}
      </h1>

      <div className="products-layout">
        {/* Sidebar */}
        <aside className="sidebar">
          <h3>Filters</h3>

          {/* Keyword search */}
          <div className="filter-group">
            <label>Search</label>
            <form onSubmit={handleSearch} style={{ display: 'flex', gap: 6 }}>
              <input
                type="text"
                placeholder="Search products…"
                value={localKw}
                onChange={e => setLocalKw(e.target.value)}
              />
              <button type="submit" className="btn-primary" style={{ padding: '8px 12px', borderRadius: 8 }}>⚡</button>
            </form>
            {isSearch && (
              <div style={{ fontSize: '.78rem', color: 'var(--text-s)', marginTop: 4 }}>
                Powered by Elasticsearch full-text search
              </div>
            )}
          </div>

          {/* Category filter */}
          <div className="filter-group">
            <label>Category</label>
            <div className="category-filters">
              <button
                className={`cat-filter-btn ${!category ? 'active' : ''}`}
                onClick={() => handleCategory('')}
              >
                🏷️ All
              </button>
              {CATEGORIES.map(c => (
                <button
                  key={c}
                  className={`cat-filter-btn ${category === c ? 'active' : ''}`}
                  onClick={() => handleCategory(c)}
                  style={{ textTransform: 'capitalize' }}
                >
                  {c}
                </button>
              ))}
            </div>
          </div>
        </aside>

        {/* Grid */}
        <div>
          <div className="products-header">
            {isSearch
              ? <span className="search-result-count">{total} result{total !== 1 ? 's' : ''} for "<strong>{keyword}</strong>"</span>
              : <span>{total} product{total !== 1 ? 's' : ''}</span>
            }
          </div>

          <div className="products-grid">
            {loading
              ? Array.from({ length: 12 }, (_, i) => <SkeletonCard key={i} />)
              : products.map(p => <ProductCard key={p.id} product={p} onAdd={handleAdd} />)
            }
          </div>

          {!loading && products.length === 0 && (
            <div className="empty-state">
              <span className="empty-icon">🔍</span>
              <h2>No products found</h2>
              <p>Try a different search term or category</p>
            </div>
          )}

          {totalPages > 1 && !isSearch && (
            <div className="pagination">
              <button disabled={page === 0} onClick={() => { setPage(p => p - 1); load(category, keyword, page - 1); }}>← Prev</button>
              {Array.from({ length: totalPages }, (_, i) => (
                <button key={i} className={i === page ? 'active' : ''} onClick={() => { setPage(i); load(category, keyword, i); }}>{i + 1}</button>
              ))}
              <button disabled={page >= totalPages - 1} onClick={() => { setPage(p => p + 1); load(category, keyword, page + 1); }}>Next →</button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
