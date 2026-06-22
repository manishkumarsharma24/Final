# ShopFlow ElastiCache — Module 7: Deployment Guide

## What Is ElastiCache?

Amazon ElastiCache is managed Redis. AWS handles OS patching, Redis upgrades, backups, and failure recovery. Your Spring Boot app connects exactly as it does locally — only the hostname changes.

---

## Why ShopVerse Needs Redis

ShopVerse uses Redis for 8 distinct purposes across the codebase:

| # | Use | Key Pattern | TTL | Class |
|---|-----|-------------|-----|-------|
| 1 | JWT Session validation | `session:{token}` | 24h | `RedisSessionStore.java` |
| 2 | Product cache | `products::{id}` | 30m | `CachedProductService.java` |
| 3 | Order cache | `orders::{id}` | 5m | `OrderCacheService.java` |
| 4 | Rate limiting | `rate:{ip}` | 1m | `RedisSessionStore.java` (Lua) |
| 5 | Shopping cart | `cart:{userId}` Hash | Session | Hash operations |
| 6 | Distributed locks | `lock:{resource}` | 5s | `DistributedLockService.java` |
| 7 | Pub/sub alerts | `low-stock-alerts` channel | — | `RedisPubSubPublisher.java` |
| 8 | Recently viewed | `recent:{userId}` List | 7d | List operations |

All of this works with a managed ElastiCache cluster — just change the hostname in the config.

---

## What Gets Created

| Resource | Purpose |
|----------|---------|
| `RedisSubnetGroup` | Puts Redis in the private subnets (same AZs as RDS) |
| `RedisParameterGroup` | Custom Redis config: eviction policy, slow log |
| `RedisReplicationGroup` | The actual Redis node (single-node for dev) |

---

## Deployment Steps

### Prerequisites

1. `shopflow-vpc` (Module 2) — private subnets + EC2 security group
2. `shopflow-secrets` (Module 4) — Redis AUTH token

### Step 1: Deploy the Stack

1. Open **CloudFormation → Create stack → With new resources**
2. Upload: `01_elasticache_redis.yaml`
3. Stack name: `shopflow-elasticache-redis`
4. Parameters:
   - **ProjectName**: `shopflow`
   - **Environment**: `dev`
   - **NodeType**: `cache.t3.micro`
5. Submit → wait for `CREATE_COMPLETE`

> **This takes 5–8 minutes.** ElastiCache provisions the Redis node, applies configuration, and performs an initial snapshot.

### Step 2: Update SSM Parameter

```bash
# Get the Redis endpoint from CloudFormation
REDIS_ENDPOINT=$(aws cloudformation describe-stacks \
  --stack-name shopflow-elasticache-redis \
  --query "Stacks[0].Outputs[?OutputKey=='RedisPrimaryEndpoint'].OutputValue" \
  --output text)

echo "Redis Endpoint: $REDIS_ENDPOINT"

# Update SSM
aws ssm put-parameter \
  --name "/shopflow/dev/redis/endpoint" \
  --value "$REDIS_ENDPOINT" \
  --type String \
  --overwrite
```

---

## Verify After Deployment

1. Go to **ElastiCache → Redis clusters** in the AWS Console
2. You should see `shopflow-redis-dev` with Status: **Available**
3. Click the cluster → **Configuration** tab:
   - Encryption at rest: **Enabled**
   - Encryption in transit: **Enabled**
   - Auth token: **Yes**
4. Note the **Primary endpoint** hostname

---

## How Spring Boot Connects to ElastiCache

ShopVerse `application.yml`:
```yaml
spring:
  data:
    redis:
      host: ${REDIS_HOST}
      port: ${REDIS_PORT:6379}
      password: ${REDIS_AUTH_TOKEN}
      ssl:
        enabled: true    # Required because transit encryption is on
      lettuce:
        pool:
          max-active: 16  # Already in application.yml
```

In ECS (Module 11), inject from SSM + Secrets Manager:
- `REDIS_HOST` → SSM `/shopflow/dev/redis/endpoint`
- `REDIS_PORT` → SSM `/shopflow/dev/redis/port` → `6379`
- `REDIS_AUTH_TOKEN` → Secrets Manager `/shopflow/app/redis-auth` key `auth_token`

---

## Test Redis Connection From EC2

After deploying EC2 (Module 8), SSH into it and test:

```bash
# Install redis-cli
sudo yum install -y redis6

# Get the AUTH token from Secrets Manager
AUTH_TOKEN=$(aws secretsmanager get-secret-value \
  --secret-id /shopflow/app/redis-auth \
  --query SecretString \
  --output text | jq -r '.auth_token')

# Connect with TLS + AUTH
redis-cli -h <REDIS_ENDPOINT> -p 6379 \
  --tls \
  -a "$AUTH_TOKEN" \
  PING

# Expected response: PONG

# Test set and get
redis-cli -h <REDIS_ENDPOINT> -p 6379 --tls -a "$AUTH_TOKEN" \
  SET "test:key" "hello-shopverse" EX 60
redis-cli -h <REDIS_ENDPOINT> -p 6379 --tls -a "$AUTH_TOKEN" \
  GET "test:key"
```

---

## Redis Eviction Policy Explained

The parameter group sets `maxmemory-policy: allkeys-lru`.

When Redis runs out of memory, it needs to delete some keys. The policy determines which:

| Policy | Behaviour | Good For |
|--------|-----------|----------|
| `allkeys-lru` | Evict the least-recently-used key (any key) | **Caches** ✓ |
| `volatile-lru` | Evict LRU keys that have a TTL set | Mixed cache + persistent |
| `noeviction` | Reject writes when full | Session stores where you can't lose data |

ShopVerse uses `allkeys-lru` because most keys have TTLs and old/stale cache data is the right thing to evict first.

---

## Cost Estimate (Monthly)

| Item | Cost |
|------|------|
| cache.t3.micro node | $11.52 |
| Backup storage (3 snapshots) | ~$0.10 |
| **Total** | **~$11.62/month** |

Free tier: 750 hours of cache.t3.micro per month for 12 months → **$0 if new account**.

---

## Post-Deployment Checklist

- [ ] Stack `shopflow-elasticache-redis` shows `CREATE_COMPLETE`
- [ ] ElastiCache Console shows `shopflow-redis-dev` Status: Available
- [ ] Transit + at-rest encryption both enabled
- [ ] SSM parameter `/shopflow/dev/redis/endpoint` updated with real endpoint
- [ ] Stack outputs visible in CloudFormation → Outputs tab

---

## Next Steps

- **Module 8 — EC2**: Create a virtual machine in the private subnet to run the Spring Boot app (or use as a jumpbox to test RDS + Redis connectivity).
