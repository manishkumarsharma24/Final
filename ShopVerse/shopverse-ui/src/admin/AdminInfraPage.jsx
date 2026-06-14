import { useState, useEffect } from 'react';
import { api } from '../api/client';

const SERVICE_META = {
  db:            { icon: '🐘', name: 'PostgreSQL',     desc: 'Primary relational DB — JPA/Hibernate + Flyway migrations',  label: 'postgres'   },
  redis:         { icon: '🔴', name: 'Redis 7',        desc: 'Cache (30/60/5 min TTLs) + JWT session store + rate limiter', label: 'redis'      },
  mongo:         { icon: '🍃', name: 'MongoDB',        desc: 'Product reviews stored as ReviewDocument documents',           label: 'mongo'      },
  elasticsearch: { icon: '🔍', name: 'Elasticsearch',  desc: 'Full-text product search via /api/search endpoint',           label: 'elastic'    },
  kafka:         { icon: '⚡', name: 'Kafka',          desc: 'Event streaming — order lifecycle + analytics topics',        label: 'kafka'      },
  neo4j:         { icon: '🕸️', name: 'Neo4j',          desc: 'Product graph — recommendations via ProductNode relationships', label: 'neo4j'    },
  cassandra:     { icon: '📋', name: 'Cassandra',      desc: 'Order activity log — optimised for time-series append writes', label: 'cassandra' },
};

export default function AdminInfraPage() {
  const [health, setHealth]   = useState(null);
  const [loading, setLoading] = useState(true);
  const [refreshed, setRefreshed] = useState(null);

  const load = () => {
    setLoading(true);
    api.getHealth().then(data => {
      setHealth(data);
      setLoading(false);
      setRefreshed(new Date());
    });
  };

  useEffect(() => { load(); }, []);

  const components = health?.components ?? {};
  const allUp = Object.values(components).every(c => c.status === 'UP');

  return (
    <div>
      <div className="admin-page-title">Infrastructure</div>
      <div className="admin-page-subtitle">
        Spring Boot Actuator /health · All services monitored via Prometheus + Grafana
      </div>

      {/* Overall status */}
      <div style={{
        background: allUp ? '#d1fae5' : '#fee2e2',
        border: `1px solid ${allUp ? '#a7f3d0' : '#fca5a5'}`,
        borderRadius: 12,
        padding: '14px 20px',
        marginBottom: 24,
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
      }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, fontWeight: 700, color: allUp ? '#065f46' : '#991b1b' }}>
          <span style={{ fontSize: '1.4rem' }}>{allUp ? '✅' : '⚠️'}</span>
          {loading ? 'Checking services…' : allUp ? 'All systems operational' : 'Some services degraded'}
        </div>
        <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
          {refreshed && <span style={{ fontSize: '.82rem', color: 'var(--text-s)' }}>Updated {refreshed.toLocaleTimeString()}</span>}
          <button className="btn-ghost" style={{ padding: '6px 14px', fontSize: '.85rem' }} onClick={load}>↻ Refresh</button>
        </div>
      </div>

      {/* Service cards */}
      <div className="health-grid" style={{ marginBottom: 28 }}>
        {Object.entries(SERVICE_META).map(([key, meta]) => {
          const comp   = components[key];
          const status = comp?.status ?? 'UNKNOWN';
          const details = comp?.details ?? {};
          return (
            <div key={key} className={`health-card health-${status}`}>
              <div className="health-card-header">
                <span className="health-icon">{meta.icon}</span>
                <div>
                  <div className="health-name">{meta.name}</div>
                  <span className={`tech-label tech-${meta.label}`} style={{ fontSize: '.68rem' }}>
                    {meta.label.toUpperCase()}
                  </span>
                </div>
              </div>
              <div className={`health-status`}>
                <div className="health-dot" />
                {loading ? 'Checking…' : status}
              </div>
              <div className="health-detail">{meta.desc}</div>
              {Object.entries(details).length > 0 && (
                <div style={{ fontSize: '.78rem', color: 'var(--text-s)', marginTop: 4, fontFamily: 'monospace' }}>
                  {Object.entries(details).map(([k, v]) => <div key={k}>{k}: {String(v)}</div>)}
                </div>
              )}
            </div>
          );
        })}
      </div>

      {/* Architecture overview */}
      <div style={{ background: 'var(--surface)', border: '1.5px solid var(--border)', borderRadius: 12, padding: 24 }}>
        <h3 style={{ fontWeight: 700, marginBottom: 16 }}>Architecture Overview</h3>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fill, minmax(280px, 1fr))', gap: 14 }}>
          {[
            { title: 'PostgreSQL', icon: '🐘', points: ['Primary data store', 'JPA + Hibernate ORM', 'Flyway schema migrations', 'Connection pool via HikariCP'] },
            { title: 'Redis 7',    icon: '🔴', points: ['@Cacheable products (30 min)', '@Cacheable customers (60 min)', 'JWT token store (session: prefix)', 'Distributed locks via Redisson'] },
            { title: 'Kafka',      icon: '⚡', points: ['shopverse.orders topic', 'shopverse.analytics topic', 'shopverse.inventory topic', 'DLT for failed messages'] },
            { title: 'RabbitMQ',   icon: '🐰', points: ['TopicExchange for routing', 'Queues: email, sms, push', 'DLX for dead-lettering', 'Publisher confirms enabled'] },
            { title: 'Elasticsearch', icon: '🔍', points: ['Full-text product search', 'ProductDocument index', 'Auto-synced on product save', 'Accessible via /api/search'] },
            { title: 'Neo4j',      icon: '🕸️', points: ['ProductNode graph', 'SIMILAR_TO relationships', 'Graph-based recommendations', 'Accessed via /api/recommendations'] },
          ].map(s => (
            <div key={s.title} style={{ background: 'var(--bg)', borderRadius: 10, padding: 16 }}>
              <div style={{ fontWeight: 700, marginBottom: 8 }}>{s.icon} {s.title}</div>
              <ul style={{ listStyle: 'none', display: 'flex', flexDirection: 'column', gap: 4 }}>
                {s.points.map(p => (
                  <li key={p} style={{ fontSize: '.82rem', color: 'var(--text-s)', display: 'flex', alignItems: 'flex-start', gap: 6 }}>
                    <span style={{ color: 'var(--success)', marginTop: 2 }}>✓</span> {p}
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
