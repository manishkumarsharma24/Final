import { Link, useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { useState } from 'react';

export default function Navbar() {
  const { totalItems } = useCart();
  const { user, logout, isLoggedIn } = useAuth();
  const navigate = useNavigate();
  const [search, setSearch] = useState('');

  const handleSearch = (e) => {
    e.preventDefault();
    if (search.trim()) navigate(`/products?keyword=${encodeURIComponent(search.trim())}`);
  };

  return (
    <nav className="navbar">
      <div className="navbar-inner">
        <Link to="/" className="navbar-brand">
          <span className="brand-icon">🛍️</span>
          <span className="brand-name">ShopVerse</span>
        </Link>

        <form className="search-form" onSubmit={handleSearch}>
          <input
            type="text"
            placeholder="Search products…"
            value={search}
            onChange={e => setSearch(e.target.value)}
            className="search-input"
          />
          <button type="submit" className="search-btn">🔍</button>
        </form>

        <div className="navbar-actions">
          <Link to="/products" className="nav-link">Products</Link>

          {isLoggedIn ? (
            <>
              <Link to="/orders" className="nav-link">My Orders</Link>
              <span className="nav-user">👤 {user.name?.split(' ')[0]}</span>
              <button className="btn-outline-sm" onClick={() => { logout(); navigate('/'); }}>Logout</button>
            </>
          ) : (
            <Link to="/login" className="btn-outline-sm">Sign In</Link>
          )}

          <Link to="/cart" className="cart-btn">
            🛒
            {totalItems > 0 && <span className="cart-badge">{totalItems}</span>}
          </Link>
        </div>
      </div>
    </nav>
  );
}
