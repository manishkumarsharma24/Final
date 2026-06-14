import { useState, useEffect } from 'react';
import { api } from '../api/client';
import { useToast } from '../context/ToastContext';

const EMPTY_FORM = { name: '', description: '', price: '', category: 'electronics', stockQuantity: '', currency: 'USD' };
const CATEGORIES = ['electronics', 'furniture', 'kitchen', 'clothing', 'lifestyle', 'accessories'];

export default function AdminProductsPage() {
  const { add: toast } = useToast();
  const [products, setProducts] = useState([]);
  const [loading, setLoading]   = useState(true);
  const [modal, setModal]       = useState(false);
  const [form, setForm]         = useState(EMPTY_FORM);
  const [saving, setSaving]     = useState(false);
  const [search, setSearch]     = useState('');

  useEffect(() => {
    api.getProducts({ size: 100 }).then(data => {
      setProducts(data?.content ?? data ?? []);
      setLoading(false);
    });
  }, []);

  const openAdd = () => { setForm(EMPTY_FORM); setModal(true); };
  const closeModal = () => { setModal(false); setForm(EMPTY_FORM); };

  const handleSave = async (e) => {
    e.preventDefault();
    if (!form.name || !form.price) { toast('Name and price are required', 'warning'); return; }
    setSaving(true);
    const payload = { ...form, price: parseFloat(form.price), stockQuantity: parseInt(form.stockQuantity) || 0 };
    const created = await api.createProduct(payload);
    setProducts(prev => [created, ...prev]);
    closeModal();
    setSaving(false);
    toast('Product created', 'success');
  };

  const handleDeactivate = async (id) => {
    if (!confirm('Deactivate this product?')) return;
    await api.deactivateProduct(id);
    setProducts(prev => prev.map(p => p.id === id ? { ...p, active: false } : p));
    toast('Product deactivated', 'info');
  };

  const filtered = products.filter(p =>
    p.name.toLowerCase().includes(search.toLowerCase()) ||
    p.category.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div>
      <div className="admin-page-title">Products</div>
      <div className="admin-page-subtitle">
        Manage catalogue · POST /api/products publishes to Kafka inventory topic
      </div>

      <div className="admin-table-wrap">
        <div className="admin-table-header">
          <h3>Product Catalogue ({products.length})</h3>
          <div className="admin-table-filters">
            <input
              className="admin-search-input"
              placeholder="Search products…"
              value={search}
              onChange={e => setSearch(e.target.value)}
            />
            <button className="btn-primary" style={{ padding: '8px 16px', fontSize: '.85rem' }} onClick={openAdd}>
              + Add Product
            </button>
          </div>
        </div>

        {loading ? (
          <div style={{ padding: 24, color: 'var(--text-s)', textAlign: 'center' }}>Loading…</div>
        ) : (
          <table>
            <thead>
              <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Category</th>
                <th>Price</th>
                <th>Stock</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map(p => (
                <tr key={p.id}>
                  <td style={{ color: 'var(--text-s)', fontSize: '.85rem' }}>#{p.id}</td>
                  <td><strong>{p.name}</strong></td>
                  <td style={{ textTransform: 'capitalize', color: 'var(--text-s)' }}>{p.category}</td>
                  <td><strong>${p.price?.toFixed(2)}</strong></td>
                  <td>
                    <span style={{ color: p.stockQuantity <= 5 ? 'var(--danger)' : p.stockQuantity <= 15 ? 'var(--accent)' : 'var(--success)', fontWeight: 600 }}>
                      {p.stockQuantity}
                    </span>
                  </td>
                  <td>
                    <span style={{ background: p.active !== false ? '#d1fae5' : '#fee2e2', color: p.active !== false ? '#065f46' : '#991b1b', padding: '2px 10px', borderRadius: 10, fontSize: '.78rem', fontWeight: 700 }}>
                      {p.active !== false ? 'Active' : 'Inactive'}
                    </span>
                  </td>
                  <td>
                    <div className="table-actions">
                      {p.active !== false && (
                        <button className="tbl-btn tbl-btn-danger" onClick={() => handleDeactivate(p.id)}>Deactivate</button>
                      )}
                    </div>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      {/* Add Product Modal */}
      {modal && (
        <div className="modal-overlay" onClick={e => e.target === e.currentTarget && closeModal()}>
          <div className="modal">
            <div className="modal-header">
              <h2>Add Product</h2>
              <button className="modal-close" onClick={closeModal}>✕</button>
            </div>
            <form className="modal-form" onSubmit={handleSave}>
              <div>
                <label>Product Name *</label>
                <input required value={form.name} onChange={e => setForm(f => ({ ...f, name: e.target.value }))} placeholder="e.g. Wireless Mouse" />
              </div>
              <div>
                <label>Description</label>
                <textarea rows={3} value={form.description} onChange={e => setForm(f => ({ ...f, description: e.target.value }))} placeholder="Product description…" />
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 12 }}>
                <div>
                  <label>Price (USD) *</label>
                  <input type="number" step="0.01" min="0" required value={form.price} onChange={e => setForm(f => ({ ...f, price: e.target.value }))} placeholder="29.99" />
                </div>
                <div>
                  <label>Stock Quantity</label>
                  <input type="number" min="0" value={form.stockQuantity} onChange={e => setForm(f => ({ ...f, stockQuantity: e.target.value }))} placeholder="100" />
                </div>
              </div>
              <div>
                <label>Category</label>
                <select value={form.category} onChange={e => setForm(f => ({ ...f, category: e.target.value }))}>
                  {CATEGORIES.map(c => <option key={c} value={c} style={{ textTransform: 'capitalize' }}>{c}</option>)}
                </select>
              </div>
              <div className="modal-actions">
                <button type="button" className="btn-ghost" onClick={closeModal}>Cancel</button>
                <button type="submit" className="btn-primary-lg" disabled={saving}>
                  {saving ? 'Saving…' : 'Create Product'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
