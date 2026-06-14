import { Link, NavLink, useNavigate } from 'react-router-dom';
import { useCart } from '../context/CartContext';
import { useAuth } from '../context/AuthContext';
import { useState } from 'react';

export default function Navbar() {
  const { totalItems } = useCart();
  const { user, isLoggedIn, logout } = useAuth();
  const navigate = useNavigate();
  const [search, setSearch] = useState('');

  const handleLogout = async () => {
    await logout();
    navigate('/login');
  };

  const handleSearch = (e) => {
    e.preventDefault();
    if (search.trim()) {
      navigate(`/products?q=${encodeURIComponent(search.trim())}`);
      setSearch('');
    }
  };

  const isAdmin = user?.role === 'ADMIN';

  return (
    <nav className="navbar">
      <Link to="/" className="nav-brand">
        <span className="brand-emoji">🛍️</span>
        <span className="brand-text">Shop<span>Verse</span></span>
      </Link>

      <form className="nav-search" onSubmit={handleSearch}>
        <input
          type="text"
          placeholder="Search products… (Elasticsearch)"
          value={search}
          onChange={e => setSearch(e.target.value)}
        />
        <button type="submit">⚡ Search</button>
      </form>

      <div className="nav-links">
        <NavLink to="/products" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
          Products
        </NavLink>

        {isLoggedIn ? (
          <>
            <NavLink to="/orders" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
              Orders
            </NavLink>
            <NavLink to="/profile" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}>
              👤 {user.name?.split(' ')[0] || 'Account'}
            </NavLink>
            {isAdmin && (
              <NavLink to="/admin" className={({ isActive }) => `nav-link ${isActive ? 'active' : ''}`}
                style={{ color: 'var(--primary)', fontWeight: 700 }}>
                ⚙️ Admin
              </NavLink>
            )}
            <button className="nav-logout-btn" onClick={handleLogout} title="Sign out">
              Logout
            </button>
          </>
        ) : (
          <Link to="/login" className="nav-link">Sign In</Link>
        )}

        <Link to="/cart" className="nav-cart-btn">
          🛒
          {totalItems > 0 && <span className="cart-badge">{totalItems}</span>}
        </Link>
      </div>
    </nav>
  );
}
