import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { api } from '../api/client';

// Backend admin secret (default from application.yml: shopverse.admin.secret)
const DEFAULT_ADMIN_SECRET = 'shopverse-admin-secret';

export default function LoginPage() {
  const { login }   = useAuth();
  const navigate    = useNavigate();

  const [mode, setMode]       = useState('login');   // 'login' | 'register'
  const [role, setRole]       = useState('customer'); // 'customer' | 'admin'
  const [loading, setLoading] = useState(false);
  const [error, setError]     = useState('');

  const [form, setForm] = useState({
    firstName: '', lastName: '', email: '', password: '', adminSecret: DEFAULT_ADMIN_SECRET,
  });

  const set = k => e => setForm(f => ({ ...f, [k]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);

    try {
      let result;
      if (mode === 'login') {
        result = role === 'admin'
          ? await api.loginAdmin(form.email, form.password, form.adminSecret)
          : await api.login(form.email, form.password);
      } else {
        result = role === 'admin'
          ? await api.registerAdmin(form.firstName, form.lastName, form.email, form.password, form.adminSecret)
          : await api.register(form.firstName, form.lastName, form.email, form.password);
      }

      if (!result?.token) throw new Error('No token returned from server');
      login(result);
      navigate(role === 'admin' ? '/admin' : '/');
    } catch (err) {
      const msg = err.response?.data?.message
        ?? err.response?.data?.error
        ?? err.message
        ?? 'Login failed. Check that the backend is running on port 8080.';
      setError(msg);
    }

    setLoading(false);
  };

  const isAdmin = role === 'admin';

  return (
    <div className="page auth-page">
      <div className="auth-card">
        <div className="auth-header">
          <span className="auth-logo">{isAdmin ? '⚙️' : '🛍️'}</span>
          <h1>{mode === 'login' ? 'Welcome back' : 'Create account'}</h1>
          <p>{isAdmin ? 'ShopVerse Admin Panel' : 'Sign in to your ShopVerse account'}</p>
        </div>

        {/* Role toggle */}
        <div className="auth-tabs" style={{ marginBottom: 8 }}>
          <button className={!isAdmin ? 'active' : ''} onClick={() => { setRole('customer'); setError(''); }}>
            👤 Customer
          </button>
          <button className={isAdmin ? 'active' : ''} onClick={() => { setRole('admin'); setError(''); }}>
            ⚙️ Admin
          </button>
        </div>

        {/* Mode toggle */}
        <div className="auth-tabs">
          <button className={mode === 'login' ? 'active' : ''} onClick={() => { setMode('login'); setError(''); }}>
            Sign In
          </button>
          <button className={mode === 'register' ? 'active' : ''} onClick={() => { setMode('register'); setError(''); }}>
            Register
          </button>
        </div>

        <form onSubmit={handleSubmit} className="auth-form" style={{ marginTop: 20 }}>
          {mode === 'register' && (
            <div className="form-row">
              <div>
                <label>First Name</label>
                <input required value={form.firstName} onChange={set('firstName')} placeholder="John" />
              </div>
              <div>
                <label>Last Name</label>
                <input required value={form.lastName} onChange={set('lastName')} placeholder="Doe" />
              </div>
            </div>
          )}

          <div>
            <label>Email Address</label>
            <input type="email" required value={form.email} onChange={set('email')} placeholder="john@example.com" />
          </div>

          <div>
            <label>Password</label>
            <input type="password" required value={form.password} onChange={set('password')}
              placeholder="••••••••" minLength={6} />
          </div>

          {isAdmin && (
            <div>
              <label>
                Admin Secret
                <span style={{ fontWeight: 400, color: 'var(--text-s)', marginLeft: 6, fontSize: '.8rem' }}>
                  (from application.yml → shopverse.admin.secret)
                </span>
              </label>
              <input
                required
                value={form.adminSecret}
                onChange={set('adminSecret')}
                placeholder="shopverse-admin-secret"
                style={{ fontFamily: 'monospace' }}
              />
            </div>
          )}

          {error && (
            <div className="auth-error">
              ⚠ {error}
              {error.includes('8080') && (
                <div style={{ marginTop: 6, fontSize: '.82rem' }}>
                  Start the backend: <code>./mvnw spring-boot:run -pl shopverse-web</code>
                </div>
              )}
            </div>
          )}

          <button type="submit" className="btn-primary-full" disabled={loading}>
            {loading ? 'Signing in…' : mode === 'login' ? `Sign In as ${isAdmin ? 'Admin' : 'Customer'}` : 'Create Account'}
          </button>
        </form>

        {/* Hint box */}
        <div className="auth-demo" style={{
          background: isAdmin ? '#fef0e6' : '#f0f9ff',
          color: isAdmin ? '#9a3412' : '#0c4a6e',
          marginTop: 20,
        }}>
          {isAdmin ? (
            <>
              <strong>⚙️ Admin credentials:</strong><br />
              Any email · any password · secret: <code>shopverse-admin-secret</code><br />
              <span style={{ fontSize: '.8rem', marginTop: 4, display: 'block' }}>
                POST /api/auth/admin/login → JWT with role=ADMIN
              </span>
            </>
          ) : (
            <>
              <strong>👤 Customer login:</strong><br />
              Any email · any password (backend skips hash in demo mode)<br />
              <span style={{ fontSize: '.8rem', marginTop: 4, display: 'block' }}>
                POST /api/auth/login → JWT with role=USER
              </span>
            </>
          )}
        </div>
      </div>
    </div>
  );
}
