import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { AuthProvider } from './context/AuthContext';
import { CartProvider } from './context/CartContext';
import Navbar            from './components/Navbar';
import HomePage          from './pages/HomePage';
import ProductsPage      from './pages/ProductsPage';
import ProductDetailPage from './pages/ProductDetailPage';
import CartPage          from './pages/CartPage';
import CheckoutPage      from './pages/CheckoutPage';
import OrdersPage        from './pages/OrdersPage';
import LoginPage         from './pages/LoginPage';
import './index.css';

export default function App() {
  return (
    <AuthProvider>
      <CartProvider>
        <BrowserRouter>
          <div className="app">
            <Navbar />
            <main className="main-content">
              <Routes>
                <Route path="/"             element={<HomePage />} />
                <Route path="/products"     element={<ProductsPage />} />
                <Route path="/products/:id" element={<ProductDetailPage />} />
                <Route path="/cart"         element={<CartPage />} />
                <Route path="/checkout"     element={<CheckoutPage />} />
                <Route path="/orders"       element={<OrdersPage />} />
                <Route path="/login"        element={<LoginPage />} />
              </Routes>
            </main>
            <footer className="footer">
              <p>© 2026 ShopVerse · Java 21 + Spring Boot 3.2 + React 18 + Vite</p>
            </footer>
          </div>
        </BrowserRouter>
      </CartProvider>
    </AuthProvider>
  );
}
