import { useState } from 'react';
import { useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../context/AuthContext';
import { api } from '../api/client';

export default function LoginPage() {
  const { login }       = useAuth();
  const navigate        = useNavigate();
  const [mode, setMode] = useState('login');
  const [form, setForm] = useState({ firstName:'', lastName:'', email:'', password:'' });
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const set = k => e => setForm(f => ({ ...f, [k]: e.target.value }));

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      let result;
      if (mode === 'login') {
        result = await api.login(form.email, form.password);
      } else {
        result = await api.register(form.firstName, form.lastName, form.email, form.password);
      }
      login(result);
      navigate('/');
    } catch (err) {
      setError(err.response?.data?.message || 'Something went wrong. Please try again.');
    }
    setLoading(false);
  };

  return (
    <div className="page auth-page">
      <div className="auth-card">
        <div className="auth-header">
          <span className="auth-logo">🛍️</span>
          <h1>{mode === 'login' ? 'Welcome back' : 'Create account'}</h1>
          <p>{mode === 'login' ? 'Sign in to your ShopVerse account' : 'Join ShopVerse today'}</p>
        </div>

        <div className="auth-tabs">
          <button className={mode === 'login' ? 'active' : ''} onClick={() => setMode('login')}>Sign In</button>
          <button className={mode === 'register' ? 'active' : ''} onClick={() => setMode('register')}>Register</button>
        </div>

        <form onSubmit={handleSubmit} className="auth-form">
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

          <label>Email Address</label>
          <input type="email" required value={form.email} onChange={set('email')} placeholder="john@example.com" />

          <label>Password</label>
          <input type="password" required value={form.password} onChange={set('password')} placeholder="••••••••" minLength={6} />

          {error && <div className="auth-error">⚠ {error}</div>}

          <button type="submit" className="btn-primary-full" disabled={loading}>
            {loading ? 'Please wait…' : mode === 'login' ? 'Sign In' : 'Create Account'}
          </button>
        </form>

        <div className="auth-demo">
          <p>🧪 <strong>Demo mode:</strong> any email/password works — no backend required</p>
        </div>
      </div>
    </div>
  );
}
