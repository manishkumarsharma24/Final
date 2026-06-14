import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider }  from './context/AuthContext';
import { CartProvider }  from './context/CartContext';
import { ToastProvider } from './context/ToastContext';
import Navbar            from './components/Navbar';
import ProtectedRoute    from './components/ProtectedRoute';

import HomePage          from './pages/HomePage';
import ProductsPage      from './pages/ProductsPage';
import ProductDetailPage from './pages/ProductDetailPage';
import CartPage          from './pages/CartPage';
import CheckoutPage      from './pages/CheckoutPage';
import OrdersPage        from './pages/OrdersPage';
import LoginPage         from './pages/LoginPage';
import ProfilePage       from './pages/ProfilePage';

import AdminLayout            from './admin/AdminLayout';
import AdminDashboard         from './admin/AdminDashboard';
import AdminProductsPage      from './admin/AdminProductsPage';
import AdminOrdersPage        from './admin/AdminOrdersPage';
import AdminCustomersPage     from './admin/AdminCustomersPage';
import AdminNotificationsPage from './admin/AdminNotificationsPage';
import AdminInfraPage         from './admin/AdminInfraPage';

import './index.css';

export default function App() {
  return (
    <AuthProvider>
      <CartProvider>
        <ToastProvider>
          <BrowserRouter>
            <Navbar />

            <Routes>
              {/* Public */}
              <Route path="/"             element={<HomePage />} />
              <Route path="/products"     element={<ProductsPage />} />
              <Route path="/products/:id" element={<ProductDetailPage />} />
              <Route path="/cart"         element={<CartPage />} />
              <Route path="/login"        element={<LoginPage />} />

              {/* Authenticated */}
              <Route path="/checkout" element={<ProtectedRoute><CheckoutPage /></ProtectedRoute>} />
              <Route path="/orders"   element={<ProtectedRoute><OrdersPage /></ProtectedRoute>} />
              <Route path="/profile"  element={<ProtectedRoute><ProfilePage /></ProtectedRoute>} />

              {/* Admin — nested routes rendered via <Outlet> in AdminLayout */}
              <Route path="/admin" element={<ProtectedRoute role="ADMIN"><AdminLayout /></ProtectedRoute>}>
                <Route index                element={<AdminDashboard />} />
                <Route path="products"      element={<AdminProductsPage />} />
                <Route path="orders"        element={<AdminOrdersPage />} />
                <Route path="customers"     element={<AdminCustomersPage />} />
                <Route path="notifications" element={<AdminNotificationsPage />} />
                <Route path="infra"         element={<AdminInfraPage />} />
              </Route>
            </Routes>

            <footer style={{
              textAlign: 'center',
              padding: '24px',
              borderTop: '1px solid var(--border)',
              fontSize: '.82rem',
              color: 'var(--text-s)',
            }}>
              © 2026 ShopVerse · Spring Boot 3.2 · Java 21 · React · Kafka · Redis · Elasticsearch · Neo4j · MongoDB · Cassandra
            </footer>
          </BrowserRouter>
        </ToastProvider>
      </CartProvider>
    </AuthProvider>
  );
}
