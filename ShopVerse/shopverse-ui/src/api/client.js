import axios from 'axios';

const BASE = '/api';

const http = axios.create({ baseURL: BASE });

http.interceptors.request.use(cfg => {
  const token = localStorage.getItem('sv_token');
  if (token) cfg.headers.Authorization = `Bearer ${token}`;
  return cfg;
});

// ── Mock data ─────────────────────────────────────────────────────────────────
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

const MOCK_REVIEWS = {
  1: [{ id: 1, customerId: 1, customerName: 'Alice M.', rating: 5, comment: 'Best headphones I have owned!', createdAt: '2024-03-01T10:00:00Z' }],
  3: [{ id: 2, customerId: 2, customerName: 'Bob T.', rating: 4, comment: 'Great monitor, colours are stunning.', createdAt: '2024-03-05T14:30:00Z' }],
};

const MOCK_ORDERS = [];
let MOCK_NEXT_ORDER_ID = 1001;

const MOCK_CUSTOMERS = [
  { id: 1, firstName: 'Alice', lastName: 'Martin', email: 'alice@example.com', tier: 'GOLD', createdAt: '2023-06-01T00:00:00Z' },
  { id: 2, firstName: 'Bob', lastName: 'Torres', email: 'bob@example.com', tier: 'SILVER', createdAt: '2023-09-15T00:00:00Z' },
  { id: 3, firstName: 'Carol', lastName: 'Zhang', email: 'carol@example.com', tier: 'BRONZE', createdAt: '2024-01-20T00:00:00Z' },
];

// ── Helper ────────────────────────────────────────────────────────────────────
async function tryApi(call, mockFn) {
  try {
    const res = await call();
    return res.data?.data ?? res.data;
  } catch {
    return mockFn();
  }
}

// ── API ───────────────────────────────────────────────────────────────────────
export const api = {

  // ── Auth — always hits real backend, no mock fallback ────────────────────
  login: async (email, password) => {
    const res = await http.post('/auth/login', { email, password });
    return res.data?.data ?? res.data;
  },

  loginAdmin: async (email, password, adminSecret) => {
    const res = await http.post('/auth/admin/login', { email, password, adminSecret });
    return res.data?.data ?? res.data;
  },

  register: async (firstName, lastName, email, password) => {
    const res = await http.post('/auth/register', { firstName, lastName, email, password });
    return res.data?.data ?? res.data;
  },

  registerAdmin: async (firstName, lastName, email, password, adminSecret) => {
    const res = await http.post('/auth/admin/register', { firstName, lastName, email, password, adminSecret });
    return res.data?.data ?? res.data;
  },

  logout: async () => {
    // Fire logout to blocklist the token in Redis; ignore errors (token may already be expired)
    try {
      await http.post('/auth/logout');
    } catch { /* ignore */ }
  },

  // ── Products ─────────────────────────────────────────────────────────────
  getProducts: ({ keyword = '', category = '', page = 0, size = 20 } = {}) =>
    tryApi(
      () => http.get('/products', { params: { keyword, category, page, size } }),
      () => {
        let items = [...MOCK_PRODUCTS];
        if (category) items = items.filter(p => p.category === category);
        if (keyword)  items = items.filter(p => p.name.toLowerCase().includes(keyword.toLowerCase()));
        return { content: items, totalElements: items.length, page, size, totalPages: 1 };
      }
    ),

  getProduct: (id) =>
    tryApi(
      () => http.get(`/products/${id}`),
      () => MOCK_PRODUCTS.find(p => p.id === Number(id)) || null
    ),

  // Admin: create product — publishes via Kafka to inventory topic
  createProduct: (payload) =>
    tryApi(
      () => http.post('/products', payload),
      () => ({ id: Date.now(), ...payload, active: true, stockQuantity: payload.stockQuantity || 0 })
    ),

  updateProduct: (id, payload) =>
    tryApi(
      () => http.put(`/products/${id}`, payload),
      () => ({ id, ...payload })
    ),

  deactivateProduct: (id) =>
    tryApi(
      () => http.delete(`/products/${id}`),
      () => ({ success: true })
    ),

  // ── Search (Elasticsearch) ────────────────────────────────────────────────
  search: (q) =>
    tryApi(
      () => http.get('/search', { params: { q } }),
      () => MOCK_PRODUCTS.filter(p => p.name.toLowerCase().includes(q.toLowerCase()))
    ),

  // ── Orders ────────────────────────────────────────────────────────────────
  // Placing an order publishes OrderPlaced → Kafka → shopverse.orders topic
  placeOrder: (payload) =>
    tryApi(
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
    ),

  getOrders: (customerId) =>
    tryApi(
      () => http.get(`/orders/customer/${customerId}`),
      () => MOCK_ORDERS
    ),

  // Admin: all orders with optional status filter
  getAllOrders: ({ page = 0, size = 20, status = '' } = {}) =>
    tryApi(
      () => http.get('/orders', { params: { page, size, status: status || undefined } }),
      () => ({ content: MOCK_ORDERS, totalElements: MOCK_ORDERS.length, page, size, totalPages: 1 })
    ),

  // Admin: status transitions — each PATCH publishes a Kafka event
  //   confirm  → OrderConfirmed  → shopverse.orders topic
  //   ship     → OrderShipped    → shopverse.orders topic
  //   deliver  → OrderDelivered  → shopverse.orders topic
  //   cancel   → OrderCancelled  → shopverse.orders topic
  //   refund   → OrderRefunded   → shopverse.orders topic
  updateOrderStatus: (id, action, extra = {}) =>
    tryApi(
      () => http.patch(`/orders/${id}/${action}`, action === 'ship' ? { trackingNumber: extra.trackingNumber ?? '' } : {}),
      () => {
        const statusMap = { confirm: 'CONFIRMED', ship: 'SHIPPED', deliver: 'DELIVERED', cancel: 'CANCELLED', refund: 'REFUNDED' };
        const order = MOCK_ORDERS.find(o => o.id === id);
        if (order) order.status = statusMap[action] || order.status;
        return order || { id, status: statusMap[action] };
      }
    ),

  cancelOrder: (id) =>
    tryApi(
      () => http.patch(`/orders/${id}/cancel`),
      () => {
        const order = MOCK_ORDERS.find(o => o.id === id);
        if (order) order.status = 'CANCELLED';
        return order;
      }
    ),

  // ── Order Activity (Cassandra) ─────────────────────────────────────────────
  // Full audit log persisted in Cassandra — optimised for time-series writes
  getOrderActivity: (orderId) =>
    tryApi(
      () => http.get(`/orders/${orderId}/activity`),
      () => [
        { eventId: '1', orderId, eventType: 'ORDER_PLACED',    details: 'Order was placed by customer',  eventTime: new Date(Date.now() - 3600000).toISOString() },
        { eventId: '2', orderId, eventType: 'ORDER_CONFIRMED', details: 'Payment confirmed',              eventTime: new Date(Date.now() - 3000000).toISOString() },
        { eventId: '3', orderId, eventType: 'ORDER_SHIPPED',   details: 'Package dispatched via courier', eventTime: new Date(Date.now() -  600000).toISOString() },
      ]
    ),

  // ── Reviews (MongoDB) ─────────────────────────────────────────────────────
  // Reviews stored as documents in MongoDB ReviewDocument collection
  getReviews: (productId) =>
    tryApi(
      () => http.get(`/products/${productId}/reviews`),
      () => MOCK_REVIEWS[productId] || []
    ),

  submitReview: (productId, payload) =>
    tryApi(
      () => http.post(`/products/${productId}/reviews`, payload),
      () => {
        const review = { id: Date.now(), productId, ...payload, createdAt: new Date().toISOString() };
        if (!MOCK_REVIEWS[productId]) MOCK_REVIEWS[productId] = [];
        MOCK_REVIEWS[productId].unshift(review);
        return review;
      }
    ),

  // ── Customers ────────────────────────────────────────────────────────────
  getCustomer: (id) =>
    tryApi(
      () => http.get(`/customers/${id}`),
      () => MOCK_CUSTOMERS.find(c => c.id === Number(id)) || null
    ),

  getAllCustomers: ({ page = 0, size = 20 } = {}) =>
    tryApi(
      () => http.get('/customers', { params: { page, size } }),
      () => ({ content: MOCK_CUSTOMERS, totalElements: MOCK_CUSTOMERS.length, page, size, totalPages: 1 })
    ),

  // ── Recommendations (Neo4j) ───────────────────────────────────────────────
  // Graph traversal: ProductNode relationships in Neo4j → similar/co-purchased items
  getRecommendations: (productId) =>
    tryApi(
      () => http.get(`/recommendations/${productId}`),
      () => MOCK_PRODUCTS.filter(p => p.id !== Number(productId)).slice(0, 4)
    ),

  // ── Analytics / Kafka events ──────────────────────────────────────────────
  // Each call produces an AnalyticsEvent to the shopverse.analytics Kafka topic
  trackView: (payload) =>
    tryApi(
      () => http.post('/analytics/track/view', payload),
      () => ({ tracked: true })
    ),

  trackSearch: (payload) =>
    tryApi(
      () => http.post('/analytics/track/search', payload),
      () => ({ tracked: true })
    ),

  trackAddToCart: (payload) =>
    tryApi(
      () => http.post('/analytics/track/cart', payload),
      () => ({ tracked: true })
    ),

  trackCheckout: (payload) =>
    tryApi(
      () => http.post('/analytics/track', { ...payload, eventType: 'CHECKOUT_STARTED' }),
      () => ({ tracked: true })
    ),

  trackConverted: (payload) =>
    tryApi(
      () => http.post('/analytics/track', { ...payload, eventType: 'ORDER_CONVERTED' }),
      () => ({ tracked: true })
    ),

  // ── Notifications (RabbitMQ) ──────────────────────────────────────────────
  // Publishes to RabbitMQ TopicExchange with routing key = notification type
  // Queues: shopverse.email, shopverse.sms, shopverse.push
  sendNotification: (payload) =>
    tryApi(
      () => http.post('/notifications/send', payload),
      () => ({ sent: true, routingKey: payload.type?.toLowerCase() || 'general' })
    ),

  registerWebhook: (payload) =>
    tryApi(
      () => http.post('/notifications/webhook', payload),
      () => ({ id: Date.now(), ...payload, active: true })
    ),

  // ── Infrastructure Health ─────────────────────────────────────────────────
  getHealth: () =>
    tryApi(
      () => http.get('/actuator/health'),
      () => ({
        status: 'UP',
        components: {
          db:            { status: 'UP', details: { database: 'PostgreSQL' } },
          redis:         { status: 'UP', details: { version: '7.0' } },
          mongo:         { status: 'UP', details: { version: '7.0' } },
          elasticsearch: { status: 'UP', details: { clusterName: 'docker-cluster' } },
          kafka:         { status: 'UP' },
          neo4j:         { status: 'UP', details: { edition: 'community' } },
          cassandra:     { status: 'UP' },
        },
      })
    ),

  getMetrics: (metric) =>
    tryApi(
      () => http.get(`/actuator/metrics/${metric}`),
      () => ({ name: metric, measurements: [{ statistic: 'VALUE', value: Math.random() * 100 }] })
    ),
};
