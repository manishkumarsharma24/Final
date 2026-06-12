import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import { api } from '../api/client';
import ProductCard from '../components/ProductCard';

const CATEGORIES = ['', 'electronics', 'clothing', 'kitchen', 'furniture', 'accessories', 'lifestyle'];

export default function ProductsPage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [products, setProducts]   = useState([]);
  const [loading, setLoading]     = useState(true);
  const [keyword, setKeyword]     = useState(searchParams.get('keyword') || '');
  const [category, setCategory]   = useState(searchParams.get('category') || '');

  useEffect(() => {
    setLoading(true);
    api.getProducts({ keyword, category }).then(data => {
      setProducts(data?.content ?? data ?? []);
      setLoading(false);
    });
  }, [keyword, category]);

  const handleSearch = (e) => {
    e.preventDefault();
    const kw = e.target.elements.kw.value.trim();
    setKeyword(kw);
    setSearchParams(kw ? { keyword: kw } : {});
  };

  const handleCategory = (cat) => {
    setCategory(cat);
    setKeyword('');
    setSearchParams(cat ? { category: cat } : {});
  };

  return (
    <div className="page">
      <div className="products-layout">
        {/* Sidebar */}
        <aside className="sidebar">
          <h3 className="sidebar-title">Categories</h3>
          <ul className="cat-list">
            {CATEGORIES.map(cat => (
              <li key={cat}>
                <button
                  className={`cat-item ${category === cat ? 'active' : ''}`}
                  onClick={() => handleCategory(cat)}
                >
                  {cat === '' ? 'All Products' : cat.charAt(0).toUpperCase() + cat.slice(1)}
                </button>
              </li>
            ))}
          </ul>
        </aside>

        {/* Main */}
        <main className="products-main">
          <div className="products-header">
            <h1 className="page-title">
              {category ? category.charAt(0).toUpperCase() + category.slice(1) : 'All Products'}
              {keyword && <span className="search-label"> — "{keyword}"</span>}
            </h1>
            <form onSubmit={handleSearch} className="inline-search">
              <input name="kw" defaultValue={keyword} placeholder="Search…" className="search-input-sm" />
              <button type="submit" className="btn-primary-sm">Search</button>
            </form>
          </div>

          {loading ? (
            <div className="loading-grid">
              {[...Array(8)].map((_, i) => <div key={i} className="skeleton-card" />)}
            </div>
          ) : products.length === 0 ? (
            <div className="empty-state">
              <span className="empty-icon">🔍</span>
              <p>No products found. Try a different search or category.</p>
            </div>
          ) : (
            <>
              <p className="results-count">{products.length} product{products.length !== 1 ? 's' : ''}</p>
              <div className="product-grid">
                {products.map(p => <ProductCard key={p.id} product={p} />)}
              </div>
            </>
          )}
        </main>
      </div>
    </div>
  );
}
