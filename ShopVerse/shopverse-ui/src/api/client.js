import axios from 'axios';

const BASE = '/api';

const http = axios.create({ baseURL: BASE });

http.interceptors.request.use(cfg => {
  const token = localStorage.getItem('sv_token');
  if (token) cfg.headers.Authorization = `Bearer ${token}`;
  return cfg;
});

// ── Mock data (used when backend is unavailable) ──────────────────────────────
const MOCK_PRODUCTS = [
  { id: 1, name: 'Wireless Noise-Cancelling Headphones', description: 'Premium over-ear headphones with 30hr battery, active noise cancellation and Hi-Res audio.', price: 249.99, currency: 'USD', category: 'electronics', stockQuantity: 45, active: true },
  { id: 2, name: 'Mechanical Gaming Keyboard', description: 'Compact TKL layout with Cherry MX switches, per-key RGB backlight and USB-C cable.', price: 129.99, currency: 'USD', category: 'electronics', stockQuantity: 30, active: true },
  { id: 3, name: 'Ultra-Wide 34" Monitor', description: '3440×1440 IPS panel, 144 Hz, 1ms, HDR400 and USB-C 90W charging.', price: 699.99, currency: 'USD', category: 'electronics', stockQuantity: 12, active: true },
  { id: 4, name: 'Ergonomic Office Chair', description: 'Lumbar support, breathable mesh back, adjustable armrests and 5-year warranty.', price: 449.99, currency: 'USD', category: 'furniture', stockQuantity: 20, active: true },
  { id: 5, name: 'Stainless Steel Water Bottle', description: 'Double-walled vacuum insulation keeps drinks cold 24hr / hot 12hr. BPA free.', price: 34.99, currency: 'USD', category: 'lifestyle', stockQuantity: 150, active: true },
  { id: 6, name: 'Wireless Charging Pad', description: '15W Qi fast charger, compatible with all Qi devices, LED indicator, anti-slip pad.', price: 29.99, currency: 'USD', category: 'electronics', stockQuantity: 80, active: true },
  { id: 7, name: 'Leather Bifold Wallet', description: 'Slim genuine leather, RFID blocking, 8 card slots and coin pouch.', price: 49.99, currency: 'USD', category: 'accessories', stockQuantity: 60, active: true },
  { id: 8, name: 'Smart Fitness Tracker', description: 'Heart rate, SpO2, sleep tracking, GPS, 7-day battery and water-resistant to 50m.', price: 179.99, currency: 'USD', category: 'electronics', stockQuantity: 35, active: true },
  { id: 9, name: 'Pour-Over Coffee Set', description: 'Borosilicate glass dripper, gooseneck kettle, 40 paper filters and measuring scoop.', price: 59.99, currency: 'USD', category: 'kitchen', stockQuantity: 25, active: true },
  { id: 10, name: 'Portable Bluetooth Speaker', description: '20W stereo sound, IPX7 waterproof, 360° surround, 12hr playback.', price: 89.99, currency: 'USD', category: 'electronics', stockQuantity: 55, active: true },
  { id: 11, name: 'Merino Wool Crew Sweater', description: 'Superfine 17.5 micron merino, relaxed fit, available in 8 colours.', price: 119.99, currency: 'USD', category: 'clothing', stockQuantity: 40, active: true },
  { id: 12, name: 'Bamboo Cutting Board Set', description: 'Set of 3 eco bamboo boards, juice groove, non-slip feet, dishwasher safe.', price: 39.99, currency: 'USD', category: 'kitchen', stockQuantity: 70, active: true },
];

const MOCK_ORDERS = [];
let MOCK_NEXT_ORDER_ID = 1001;

// ── API wrappers with mock fallback ──────────────────────────────────────────

async function tryApi(call, mockFn) {
  try {
    const res = await call();
    return res.data?.data ?? res.data;
  } catch {
    return mockFn();
  }
}

export const api = {
  // Auth
  login: async (email, password) => {
    return tryApi(
      () => http.post('/auth/login', { email, password }),
      () => ({ token: 'mock-jwt-token', customerId: 1, email, name: 'Demo User', role: 'USER' })
    );
  },
  register: async (firstName, lastName, email, password) => {
    return tryApi(
      () => http.post('/auth/register', { firstName, lastName, email, password }),
      () => ({ token: 'mock-jwt-token', customerId: Date.now(), email, name: `${firstName} ${lastName}`, role: 'USER' })
    );
  },

  // Products
  getProducts: async ({ keyword = '', category = '', page = 0, size = 20 } = {}) => {
    return tryApi(
      () => http.get('/products', { params: { keyword, category, page, size } }),
      () => {
        let items = [...MOCK_PRODUCTS];
        if (category) items = items.filter(p => p.category === category);
        if (keyword)  items = items.filter(p => p.name.toLowerCase().includes(keyword.toLowerCase()));
        return { content: items, totalElements: items.length, page, size, totalPages: 1 };
      }
    );
  },
  getProduct: async (id) => {
    return tryApi(
      () => http.get(`/products/${id}`),
      () => MOCK_PRODUCTS.find(p => p.id === Number(id)) || null
    );
  },

  // Orders
  placeOrder: async (payload) => {
    return tryApi(
      () => http.post('/orders', payload),
      () => {
        const order = {
          id: MOCK_NEXT_ORDER_ID++,
          customerId: payload.customerId,
          status: 'CONFIRMED',
          total: payload.items.reduce((sum, i) => {
            const p = MOCK_PRODUCTS.find(p => p.id === i.productId);
            return sum + (p ? p.price * i.quantity : 0);
          }, 0),
          currency: 'USD',
          items: payload.items.map(i => {
            const p = MOCK_PRODUCTS.find(p => p.id === i.productId);
            return { productId: i.productId, productName: p?.name, quantity: i.quantity, unitPrice: p?.price };
          }),
          createdAt: new Date().toISOString(),
        };
        MOCK_ORDERS.unshift(order);
        return order;
      }
    );
  },
  getOrders: async (customerId) => {
    return tryApi(
      () => http.get(`/orders/customer/${customerId}`),
      () => MOCK_ORDERS
    );
  },

  // Search
  search: async (q) => {
    return tryApi(
      () => http.get('/search', { params: { q } }),
      () => MOCK_PRODUCTS.filter(p => p.name.toLowerCase().includes(q.toLowerCase()))
    );
  },
};
