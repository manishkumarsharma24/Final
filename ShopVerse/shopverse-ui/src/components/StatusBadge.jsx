const CONFIG = {
  PENDING:    { bg: '#fef3c7', color: '#d97706', label: 'Pending' },
  CONFIRMED:  { bg: '#dbeafe', color: '#1d4ed8', label: 'Confirmed' },
  PROCESSING: { bg: '#ede9fe', color: '#6d28d9', label: 'Processing' },
  SHIPPED:    { bg: '#cffafe', color: '#0e7490', label: 'Shipped' },
  DELIVERED:  { bg: '#dcfce7', color: '#15803d', label: 'Delivered' },
  CANCELLED:  { bg: '#fee2e2', color: '#b91c1c', label: 'Cancelled' },
  REFUNDED:   { bg: '#f3f4f6', color: '#4b5563', label: 'Refunded' },
};

export default function StatusBadge({ status }) {
  const cfg = CONFIG[status] || { bg: '#f3f4f6', color: '#4b5563', label: status };
  return (
    <span className="status-badge" style={{ background: cfg.bg, color: cfg.color }}>
      {cfg.label}
    </span>
  );
}
