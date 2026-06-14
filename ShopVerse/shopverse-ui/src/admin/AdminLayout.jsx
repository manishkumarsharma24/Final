import { NavLink, Outlet, useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';

const NAV = [
  { to: '/admin',              icon: '📊', label: 'Dashboard'     },
  { to: '/admin/products',     icon: '📦', label: 'Products'      },
  { to: '/admin/orders',       icon: '🛒', label: 'Orders'        },
  { to: '/admin/customers',    icon: '👥', label: 'Customers'     },
  { to: '/admin/notifications',icon: '🔔', label: 'Notifications' },
  { to: '/admin/infra',        icon: '🖥️', label: 'Infrastructure'},
];

export default function AdminLayout() {
  const { logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => { logout(); navigate('/'); };

  return (
    <div className="admin-root">
      <nav className="admin-sidebar">
        <div className="admin-sidebar-header">
          <h3>Admin Panel</h3>
        </div>
        {NAV.map(({ to, icon, label }) => (
          <NavLink
            key={to}
            to={to}
            end={to === '/admin'}
            className={({ isActive }) => `admin-nav-item ${isActive ? 'active' : ''}`}
          >
            <span className="nav-icon">{icon}</span>
            {label}
          </NavLink>
        ))}
        <div style={{ marginTop: 'auto', borderTop: '1px solid rgba(255,255,255,.08)', padding: '12px 0' }}>
          <button
            onClick={handleLogout}
            style={{ display: 'flex', alignItems: 'center', gap: 10, padding: '11px 20px', color: 'rgba(255,255,255,.55)', background: 'none', border: 'none', cursor: 'pointer', width: '100%', fontSize: '.9rem' }}
          >
            <span>🚪</span> Sign Out
          </button>
        </div>
      </nav>
      <main className="admin-main">
        <Outlet />
      </main>
    </div>
  );
}
