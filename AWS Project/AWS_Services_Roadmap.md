# ShopFlow — AWS Services Roadmap (Scalable Architecture)
> Region: ap-south-1 (Mumbai) · Stack prefix: `shopflow-` · App: ShopVerse Spring Boot 3.2.5 / Java 21
> Design principle: **every layer auto-scales independently** so no single tier becomes a bottleneck.

---

## Scalability Architecture Overview

```
Internet
    │
    ▼
[CloudFront CDN]  ──── S3 (React SPA + product images, edge-cached globally)
    │
    ▼
[WAF]  ──── rate limiting, DDoS protection, OWASP rules
    │
    ▼
[Application Load Balancer]  ─── public subnets, Multi-AZ
    │                              distributes across all healthy ECS tasks
    ▼
[ECS Fargate]  ──────────────── private subnets, Multi-AZ
  Task 1 | Task 2 | Task N        AUTO SCALES: CPU, memory, ALB request count
    │
    ├──→ [Aurora Serverless v2]   private subnets, AUTO SCALES: 0.5–64 ACU
    │      Writer + Reader endpoints (ShopVerse RoutingDataSource maps to both)
    │
    ├──→ [ElastiCache Redis]      private subnets, cluster mode
    │      2 shards × 1 replica   AUTO SCALES: add shards on memory pressure
    │
    ├──→ [MSK (Kafka)]            private subnets, 3 brokers (one per AZ)
    │      MSK Serverless option   AUTO SCALES: throughput on demand
    │
    ├──→ [OpenSearch]             private subnets, 2 data nodes
    │      UltraWarm for cold data  SCALES: add data nodes, use index rollover
    │
    └──→ [SQS → Lambda]          fully serverless, scales to zero / to thousands
           email | payment | webhook  Lambda concurrency scales with queue depth
```

**Why each layer scales independently:**
ShopVerse is stateless at the compute layer (JWT auth stored in Redis, no HttpSession). This means any ECS task can serve any request — the ALB can route to N tasks without session affinity. The database layer uses Aurora Serverless v2 which scales ACUs (Aurora Capacity Units) in seconds, not minutes. Redis cluster mode adds shards without downtime. Each layer has its own scaling clock so a spike in search traffic doesn't starve order processing.

---

## What's Done

| Module | CloudFormation Stacks | Status |
|--------|----------------------|--------|
| IAM | shopflow-iam-password/policies/groups/roles/users | ✅ Complete |
| VPC | shopflow-vpc, shopflow-security-groups | ✅ Complete |

---

## Phase 1 — NAT Gateway (Required Before ECS)

### Stack 3: NAT Gateways
**File:** `VPC/CloudFormation/03_nat_gateways.yaml`

ECS Fargate tasks run in **private subnets** but need outbound internet access to pull ECR images, call Secrets Manager, send to SQS, and call external payment APIs. NAT Gateways provide this without exposing tasks to the internet.

**Why one per AZ:** If you use a single NAT Gateway in AZ-a and AZ-a goes down, ECS tasks in AZ-b lose internet access too. Two NAT Gateways (one per AZ) keep each AZ self-sufficient.

Creates:
- Elastic IP 1 (for NAT GW in AZ-a)
- Elastic IP 2 (for NAT GW in AZ-b)
- `shopflow-nat-1a` — placed in `shopflow-pub-1a`
- `shopflow-nat-1b` — placed in `shopflow-pub-1b`
- Update `shopflow-private-rt-1a` → add `0.0.0.0/0 → shopflow-nat-1a`
- Update `shopflow-private-rt-1b` → add `0.0.0.0/0 → shopflow-nat-1b`

> **Cost note:** Each NAT Gateway costs ~$32/month + $0.045/GB data processed. For a learning project, use a single NAT GW in one AZ to halve the cost; add the second AZ NAT GW before going production.

```
Imports: shopflow-pub-subnet-1a, shopflow-pub-subnet-1b, shopflow-prv-subnet-1a, shopflow-prv-subnet-1b
Exports: shopflow-nat-1a-id, shopflow-nat-1b-id
```

---

## Phase 2 — Secrets Manager

### Stack 4: Secrets
**File:** `SecretsManager/CloudFormation/01_secrets.yaml`

Creates:
- `shopflow/db/master` — Aurora master username + password (auto-rotated every 30 days by Secrets Manager → RDS integration)
- `shopflow/app/jwt-secret` — JWT signing key (32+ chars)
- `shopflow/app/redis-auth` — ElastiCache auth token
- `shopflow/app/kafka-credentials` — MSK SASL credentials (if TLS auth enabled)

Auto-rotation: Secrets Manager + Aurora have native rotation integration. On rotation, Secrets Manager updates the DB password and keeps the old one valid briefly so running tasks don't break — **zero-downtime credential rotation**.

```
Exports: shopflow-db-secret-arn, shopflow-jwt-secret-arn, shopflow-redis-auth-arn
```

---

## Phase 3 — Database (Aurora Serverless v2)

### Stack 5: Aurora Serverless v2 (MySQL-compatible)
**File:** `RDS/CloudFormation/01_rds_subnet_group.yaml` + `02_aurora_cluster.yaml`

**Why Aurora Serverless v2 over standard RDS MySQL:**

| Feature | RDS MySQL t3.small | Aurora Serverless v2 |
|---------|-------------------|---------------------|
| Scaling | Manual instance resize (requires reboot) | Automatic, in seconds, no downtime |
| Scale range | Fixed instance size | 0.5 ACU → 64 ACU (~1 GB → 128 GB RAM) |
| Read scaling | Manual read replica | Built-in reader endpoint, add readers instantly |
| Multi-AZ | Optional, doubles cost | Built-in, failover < 30 seconds |
| Cost at low traffic | Always-on cost | Scales to minimum (0.5 ACU) when idle |
| Failover | ~60-120 seconds | ~30 seconds |
| Flyway/JDBC | Same driver | Same driver (MySQL 8.0 compatible) |

ShopVerse's `RoutingDataSource` (write → primary, read → replica) maps directly to Aurora's Writer and Reader endpoints.

**01_rds_subnet_group.yaml** — Creates:
- `shopflow-rds-subnet-group` across both private subnets

**02_aurora_cluster.yaml** — Creates:
- Cluster: `shopflow-aurora-cluster` (Aurora MySQL 8.0)
- Writer instance: `shopflow-aurora-writer` — min 0.5 ACU, max 16 ACU (start here; increase to 64 for production)
- Reader instance: `shopflow-aurora-reader` — same ACU range, in AZ-b
- Cluster endpoints:
  - Writer: `shopflow-aurora-cluster.cluster-xxx.ap-south-1.rds.amazonaws.com:3306`
  - Reader: `shopflow-aurora-cluster.cluster-ro-xxx.ap-south-1.rds.amazonaws.com:3306`
- Security group: `shopflow-rds-sg` (already created, port 3306 from EC2/ECS/Lambda)
- Encryption: enabled (KMS)
- Backup retention: 7 days (automated backups, point-in-time recovery)
- Enhanced monitoring: 60-second granularity
- Performance Insights: enabled (slow query analysis)
- Credentials from Secrets Manager (auto-rotation enabled)

**Aurora Auto Scaling for reader instances:**
```yaml
AuroraReadReplicaScaling:
  Type: AWS::ApplicationAutoScaling::ScalableTarget
  Properties:
    ServiceNamespace: rds
    ResourceId: !Sub "cluster:${AuroraCluster}"
    ScalableDimension: rds:cluster:ReadReplicaCount
    MinCapacity: 1
    MaxCapacity: 5

AuroraReadScalingPolicy:
  Type: AWS::ApplicationAutoScaling::ScalingPolicy
  Properties:
    PolicyType: TargetTrackingScaling
    TargetTrackingScalingPolicyConfiguration:
      TargetValue: 70.0          # scale out when CPU > 70%
      PredefinedMetricSpecification:
        PredefinedMetricType: RDSReaderAverageCPUUtilization
      ScaleInCooldown: 300       # 5 min before scaling in (avoid flapping)
      ScaleOutCooldown: 60       # 1 min to scale out fast
```

**Spring Boot config for dual endpoints:**
```yaml
# application-prod.yml
spring:
  datasource:
    write:
      url: jdbc:mysql://${AURORA_WRITER_ENDPOINT}:3306/shopverse
    read:
      url: jdbc:mysql://${AURORA_READER_ENDPOINT}:3306/shopverse
```
ShopVerse's `ReadWriteRoutingAspect` already handles the routing — no code change needed, only the JDBC URL env vars change.

```
Imports: shopflow-rds-sg-id, shopflow-prv-subnet-1a, shopflow-prv-subnet-1b, shopflow-db-secret-arn
Exports: shopflow-aurora-writer-endpoint, shopflow-aurora-reader-endpoint, shopflow-aurora-port
```

---

## Phase 4 — Caching (ElastiCache Redis Cluster Mode)

### Stack 6: ElastiCache Redis (Cluster Mode Enabled)
**File:** `ElastiCache/CloudFormation/01_elasticache_subnet_group.yaml` + `02_elasticache_cluster.yaml`

ShopVerse uses Redis for 8 data structures (String, Hash, List, Set, Sorted Set, Pub/Sub, Streams, distributed locks). Cluster mode shards keys across multiple nodes so memory and throughput scale horizontally.

**Without cluster mode (single node):** all keys on one node — Redis is single-threaded, so one CPU core limits throughput. If it fills up, you must resize the instance (requires replacement).

**With cluster mode (2 shards × 1 replica):** keys are distributed by hash slot. Adding a shard online takes ~minutes with zero downtime. Each shard handles a subset of key space concurrently.

ShopVerse's Redisson client and `RedisTemplate` both support cluster mode without code changes — just update the config to use cluster mode endpoint.

**01_elasticache_subnet_group.yaml** — Creates:
- `shopflow-cache-subnet-group` across both private subnets

**02_elasticache_cluster.yaml** — Creates:
- Replication group: `shopflow-redis`
- Engine: Redis 7.x, cluster mode **enabled**
- Node type: `cache.t3.medium` (2 vCPU, 3.22 GB; upgrade to `cache.r7g.large` for production)
- Num node groups (shards): 2
- Replicas per shard: 1 (promotes to primary if shard leader fails — automatic failover)
- Total nodes: 4 (2 primaries + 2 replicas)
- Multi-AZ: enabled (replicas in different AZs from primaries)
- Encryption in transit: enabled (TLS)
- Auth token: from Secrets Manager
- New SG: `shopflow-redis-sg` — inbound 6379 from ECS task SG and Lambda SG

**ElastiCache Auto Scaling (Serverless mode — alternative):**
For maximum simplicity, ElastiCache Serverless automatically scales memory and connections:
```yaml
# ElastiCache Serverless (simpler, more expensive per GB)
ElastiCacheServerless:
  Type: AWS::ElastiCache::ServerlessCache
  Properties:
    ServerlessCacheName: shopflow-redis-serverless
    Engine: redis
    CacheUsageLimits:
      DataStorage:
        Minimum: 1
        Maximum: 100    # GB — scales between min and max automatically
        Unit: GB
      ECPUPerSecond:
        Minimum: 1000
        Maximum: 10000000
```

**Spring Boot cluster mode config:**
```yaml
spring:
  data:
    redis:
      cluster:
        nodes:
          - ${REDIS_CLUSTER_NODE_1}:6379
          - ${REDIS_CLUSTER_NODE_2}:6379
        max-redirects: 3
      lettuce:
        cluster:
          refresh:
            adaptive: true          # auto-discover new nodes
            period: 30s
```

Redisson config also supports cluster mode — set `clusterServersConfig` instead of `singleServerConfig`.

```
Exports: shopflow-redis-config-endpoint, shopflow-redis-port, shopflow-redis-sg-id
```

---

## Phase 5 — Compute (ECS Fargate with Auto Scaling)

### Stack 7: ECS Cluster + ECR
**File:** `ECS/CloudFormation/01_ecr.yaml` + `02_ecs_cluster.yaml`

**01_ecr.yaml** — Creates:
- Repository: `shopflow/shopverse-web`
- Image scan on push: enabled
- Lifecycle policy: keep last 10 tagged, expire untagged after 7 days

**02_ecs_cluster.yaml** — Creates:
- Cluster: `shopflow-cluster`
- Container Insights: enabled
- Capacity providers: `FARGATE` + `FARGATE_SPOT` (SPOT is 70% cheaper — use for non-critical tasks)

```
Exports: shopflow-ecr-uri, shopflow-ecs-cluster-arn, shopflow-ecs-cluster-name
```

---

### Stack 8: ECS Task Definition
**File:** `ECS/CloudFormation/03_ecs_task_definition.yaml`

Creates task definition `shopflow-shopverse`:
- Launch type: FARGATE
- CPU: 1024 (1 vCPU), Memory: 2048 MB
  - JVM: `-Xms512m -Xmx1g` (leaves ~1 GB for OS + Metaspace + NIO buffers)
  - More headroom avoids OOMKilled during GC spikes
- Container port: 8080
- Log driver: `awslogs` → CloudWatch log group `/ecs/shopflow-shopverse`
- Secrets (injected from Secrets Manager at task start, not baked into image):
  - `DB_PASS` ← `shopflow/db/master`
  - `JWT_SECRET` ← `shopflow/app/jwt-secret`
  - `REDIS_AUTH` ← `shopflow/app/redis-auth`
- Environment variables (from SSM Parameter Store):
  - `DB_WRITE_URL`, `DB_READ_URL` ← Aurora endpoints
  - `REDIS_NODES` ← ElastiCache cluster endpoints
  - `KAFKA_BROKERS` ← MSK bootstrap brokers
  - `ES_URI` ← OpenSearch endpoint
- IAM roles:
  - Task role: `shopflow-ecs-task-role` (app permissions)
  - Execution role: `shopflow-ecs-execution-role` (pull ECR, write logs, read secrets)

```
Exports: shopflow-task-definition-arn
```

---

### Stack 9: ECS Service + ALB + Auto Scaling
**File:** `ECS/CloudFormation/04_alb.yaml` + `05_ecs_service.yaml`

**04_alb.yaml** — Creates:
- ALB: `shopflow-alb` (internet-facing) in both public subnets
- Target group: `shopflow-tg` — protocol HTTP, port 8080
  - Health check: `GET /actuator/health`, 2xx response
  - Healthy threshold: 2 checks, interval 30s
  - Deregistration delay: 30s (tasks drain connections before stopping — important for zero-downtime deploys)
- Listener: port 80 → forward to `shopflow-tg`
- Listener: port 443 → SSL termination (ACM cert) → forward to `shopflow-tg`
- New SG: `shopflow-ecs-sg` — inbound 8080 from ALB-SG only

```
Exports: shopflow-alb-dns, shopflow-alb-arn, shopflow-target-group-arn
```

**05_ecs_service.yaml** — Creates:
- Service: `shopflow-web-service`
- Cluster: `shopflow-cluster`
- Task definition: `shopflow-shopverse`
- Launch type: FARGATE
- Desired count: **2** (minimum — one per AZ for HA; never run just 1)
- Subnets: both private subnets
- Security group: `shopflow-ecs-sg`
- Load balancer: attached to `shopflow-tg`
- Deployment: rolling update — `minimumHealthyPercent: 100`, `maximumPercent: 200`
  - Means: always keep 2 tasks running, add 2 new ones, health check, then remove old 2 (zero downtime)

**ECS Auto Scaling — 3 policies:**

```yaml
# 1. Target Tracking on CPU (primary scaling signal)
CPUScalingPolicy:
  PolicyType: TargetTrackingScaling
  TargetValue: 65.0              # scale out when avg CPU > 65%
  PredefinedMetricType: ECSServiceAverageCPUUtilization
  ScaleInCooldown: 300           # 5 min — don't scale in during a burst that just ended
  ScaleOutCooldown: 60           # 1 min — react fast to traffic spikes

# 2. Target Tracking on Memory
MemoryScalingPolicy:
  PolicyType: TargetTrackingScaling
  TargetValue: 70.0              # scale out when avg memory > 70%
  PredefinedMetricType: ECSServiceAverageMemoryUtilization
  ScaleInCooldown: 300
  ScaleOutCooldown: 60

# 3. Target Tracking on ALB Request Count (most responsive to traffic)
RequestScalingPolicy:
  PolicyType: TargetTrackingScaling
  TargetValue: 1000              # scale out when > 1000 requests/min per task
  PredefinedMetricType: ALBRequestCountPerTarget
  ResourceLabel: !Sub "${ALBArn}/targetgroup/${TargetGroupArn}"
  ScaleInCooldown: 300
  ScaleOutCooldown: 30           # Fastest — react in 30s to traffic burst

# Capacity bounds
ScalableTarget:
  MinCapacity: 2                 # always at least 2 (one per AZ)
  MaxCapacity: 10                # up to 10 tasks = 10 vCPU / ~10 GB heap pool
```

ECS uses the **most aggressive** policy — if CPU says scale to 3 tasks but request count says scale to 5, it uses 5. This ensures you never under-scale.

```
Exports: shopflow-ecs-service-name, shopflow-ecs-sg-id
```

---

## Phase 6 — Object Storage + CDN

### Stack 10: S3 + CloudFront
**File:** `S3/CloudFormation/01_s3_buckets.yaml` + `02_cloudfront.yaml`

**01_s3_buckets.yaml** — Creates:
| Bucket | Purpose | Public? |
|--------|---------|---------|
| `shopflow-artifacts` | App JARs for EC2 user data and CI/CD | No |
| `shopflow-cicd-artifacts` | CodePipeline artifacts | No |
| `shopflow-rds-exports` | Aurora snapshot exports | No |
| `shopflow-product-images` | Static product images | Via CloudFront only |
| `shopflow-spa` | React SPA build (`dist/`) | Via CloudFront only |

**02_cloudfront.yaml** — Creates:
- Distribution with 2 origins:
  - Origin 1: `shopflow-product-images` S3 bucket (product images)
  - Origin 2: `shopflow-spa` S3 bucket (React app)
- Cache behaviors:
  - `/images/*` → S3 images bucket, TTL 86400s (1 day), gzip + brotli compression
  - `/*` → S3 SPA bucket, TTL 0 for `index.html`, TTL 31536000 for hashed JS/CSS assets
- OAC (Origin Access Control): CloudFront only — direct S3 access blocked
- WAF WebACL attached: rate limit 2000 req/5min per IP

**Why CloudFront matters for scaling:**
Without CloudFront, every product image request hits your ALB → ECS task → S3 (or serves from EC2 disk). With CloudFront, images are cached at AWS edge locations (200+ globally). A spike in traffic from 10,000 users viewing product pages hits CloudFront's edge — **zero load on your ECS tasks** for image serving.

```
Exports: shopflow-cdn-domain, shopflow-cdn-distribution-id, shopflow-images-bucket, shopflow-spa-bucket
```

---

## Phase 7 — Messaging (MSK + SQS)

### Stack 11: MSK (Kafka) — Multi-Broker
**File:** `MSK/CloudFormation/01_msk_cluster.yaml`

ShopVerse Kafka topics: `shopverse.orders` (3 partitions), `shopverse.inventory` (3), `shopverse.analytics` (6 — high volume), `shopverse.products` (3), `shopverse.notifications` (2).

**Why 3 brokers (one per AZ):**
- With RF=3, each message is on all 3 brokers — one broker failure = no data loss, no downtime
- With RF=1 (as in local Docker), one broker failure = total outage

Creates:
- MSK cluster: `shopflow-kafka`
- Kafka version: 3.6.0
- Broker nodes: 3 (one per AZ using `shopflow-prv-1a`, `shopflow-prv-1b`, and a third AZ subnet if available, else 2 brokers)
- Broker type: `kafka.t3.small` (learning) → `kafka.m5.large` (production)
- Storage: 100 GB per broker, EBS encryption enabled
- Replication factor for all topics: **3** (change from local RF=1)
- New SG: `shopflow-msk-sg` — inbound 9092 (plaintext), 9094 (TLS) from ECS task SG and Lambda SG
- CloudWatch metrics: PER_BROKER (detailed metrics)
- Encryption in transit: TLS

**MSK Serverless (alternative — simpler, scales automatically):**
```yaml
MSKServerlessCluster:
  Type: AWS::MSK::ServerlessCluster
  Properties:
    ClusterName: shopflow-kafka-serverless
    ClientAuthentication:
      Sasl:
        Iam:
          Enabled: true
    VpcConfigs:
      - SubnetIds: [!ImportValue shopflow-prv-subnet-1a, !ImportValue shopflow-prv-subnet-1b]
        SecurityGroups: [!Ref MSKSecurityGroup]
```
MSK Serverless scales throughput automatically and you pay per-GB processed. No broker sizing decisions. IAM authentication replaces SASL/SCRAM.

```
Exports: shopflow-msk-brokers, shopflow-msk-arn, shopflow-msk-sg-id
```

### Stack 12: SQS Queues
**File:** `SQS/CloudFormation/01_sqs_queues.yaml`

SQS scales to unlimited throughput automatically — no capacity planning needed.

| Queue | Visibility Timeout | DLQ | Max Receive Count | Purpose |
|-------|-------------------|-----|-------------------|---------|
| `shopflow-email-notifications` | 60s | `shopflow-email-dlq` | 3 | Order emails |
| `shopflow-payment-callbacks` | 30s | `shopflow-payment-dlq` | 5 | Payment gateway callbacks |
| `shopflow-webhook-delivery` | 120s | `shopflow-webhook-dlq` | 3 | Merchant webhooks |

```
Exports: shopflow-email-queue-url, shopflow-payment-queue-url, shopflow-webhook-queue-url
```

---

## Phase 8 — Serverless (Lambda with Concurrency Scaling)

### Stack 13: Lambda Functions
**File:** `Lambda/CloudFormation/01_lambda_functions.yaml`

Lambda scales to thousands of concurrent executions automatically. SQS integration scales concurrently with queue depth (1 Lambda invocation per 5 SQS messages by default, up to `ReservedConcurrentExecutions`).

| Function | Trigger | Runtime | Reserved Concurrency | Purpose |
|----------|---------|---------|---------------------|---------|
| `shopflow-email-sender` | SQS: shopflow-email-notifications | Python 3.12 | 50 | SES email delivery |
| `shopflow-payment-processor` | SQS: shopflow-payment-callbacks | Java 21 | 20 | Update Aurora on payment result |
| `shopflow-search-sync` | SQS / EventBridge | Python 3.12 | 10 | Sync products to OpenSearch |
| `shopflow-analytics-processor` | Kinesis Data Stream | Python 3.12 | 100 | Aggregate analytics events |

All Lambda functions:
- VPC: private subnets, `shopflow-lambda-sg`
- IAM: `shopflow-lambda-role` (already created)
- Dead letter: SQS DLQ
- Timeout: 30s (email), 60s (payment), 300s (search sync)
- Memory: 512 MB (Python), 1024 MB (Java — accounts for JVM startup)
- Provisioned concurrency for payment processor: 5 (keeps 5 instances warm — avoids cold start on first payment callback after idle period)

**Lambda with SQS auto-scaling:**
```
Queue depth 0      → 0 Lambda invocations (scales to zero)
Queue depth 10     → 2 Lambda invocations (5 messages each)
Queue depth 100    → 20 Lambda invocations
Queue depth 1000   → 50 Lambda invocations (hits ReservedConcurrency cap)
→ Messages wait in queue (SQS is the buffer — no messages dropped)
```

```
Exports: shopflow-email-lambda-arn, shopflow-payment-lambda-arn
```

---

## Phase 9 — Search (OpenSearch Service)

### Stack 14: OpenSearch Service
**File:** `OpenSearch/CloudFormation/01_opensearch.yaml`

ShopVerse uses Elasticsearch for product full-text search, autocomplete (`nameSuggest` completion field), fuzzy search, and relevance scoring. Amazon OpenSearch Service is the AWS managed equivalent of Elasticsearch 8.x.

Creates:
- Domain: `shopflow-search`
- Engine: OpenSearch 2.11 (Elasticsearch-compatible API)
- Data nodes: 2 × `t3.small.search` (1 vCPU, 2 GB RAM each)
  - For production: 3 × `r6g.large.search` (2 vCPU, 16 GB)
- Master nodes: none (optional for small clusters; add 3 dedicated masters at scale)
- Storage: 20 GB gp3 per node (40 GB total)
- Multi-AZ: 2 AZs (one data node per AZ)
- VPC: private subnets, `shopflow-opensearch-sg` (inbound 443 from ECS-SG)
- Encryption at rest: enabled (KMS)
- Encryption in transit: enabled (TLS only)
- Fine-grained access control: IAM-based (no built-in user db for production)

**Index mapping migration from Elasticsearch to OpenSearch:**
The `ProductDocument` class uses Spring Data Elasticsearch annotations. OpenSearch Service is wire-compatible — change the endpoint URL in `application.yml` from `http://localhost:9200` to the OpenSearch VPC endpoint. No Java code changes.

```yaml
# application-prod.yml
shopverse:
  elasticsearch:
    uris: https://${OPENSEARCH_ENDPOINT}:443
```

**Scaling OpenSearch:**
- Add data nodes horizontally (online, no downtime) via UpdateDomainConfig API
- UltraWarm nodes for infrequently accessed indices (10× cheaper than hot storage)
- Cold storage for archival (S3-backed, pay only when querying)

```
Exports: shopflow-opensearch-endpoint, shopflow-opensearch-sg-id
```

---

## Phase 10 — CI/CD Pipeline

### Stack 15: CodePipeline + CodeBuild
**File:** `CICD/CloudFormation/01_codepipeline.yaml`

CI/CD role `shopflow-cicd-role` already created. This pipeline: builds → tests → packages → pushes to ECR → deploys to ECS with zero-downtime blue/green.

Creates:
- S3 bucket: `shopflow-cicd-artifacts` (pipeline artifact store)
- CodeBuild project: `shopflow-build`
  - Environment: `aws/codebuild/standard:7.0` (Java 21 + Docker)
  - Privileged mode: true (required for Docker builds)
  - Build spec stages:
    1. `mvn clean verify` (compile + unit tests)
    2. `mvn jacoco:report` (coverage must be ≥ 80% or build fails)
    3. `docker build -t $ECR_URI:$VERSION .`
    4. `docker push $ECR_URI:$VERSION`
    5. Write `imagedefinitions.json` (ECS blue/green input)
- CodePipeline: `shopflow-pipeline`
  - Stage 1 — Source: GitHub (or CodeCommit) webhook trigger
  - Stage 2 — Build: CodeBuild project above
  - Stage 3 — Deploy: ECS rolling deploy to `shopflow-web-service`
    - `minimumHealthyPercent: 100` → add new tasks, health check, then remove old
    - ALB deregistration delay 30s → old tasks finish in-flight requests before stopping

**Zero-downtime deploy sequence:**
```
Before: 2 tasks running (v1.0)
Deploy starts:
  Step 1: ECS adds 2 new tasks (v1.1) — total: 4 tasks
  Step 2: ALB health checks pass for new tasks — traffic flows to all 4
  Step 3: ALB deregisters old 2 tasks — 30s drain
  Step 4: Old 2 tasks stop
After: 2 tasks running (v1.1), zero dropped requests
```

---

## Phase 11 — Observability (CloudWatch + Alarms Wired to Scaling)

### Stack 16: CloudWatch Dashboards + Alarms
**File:** `Monitoring/CloudFormation/01_cloudwatch.yaml`

**Log Groups:**
- `/ecs/shopflow-shopverse` — ECS task stdout (JSON structured logs with traceId, customerId, orderId)
- `/aws/rds/cluster/shopflow-aurora-cluster/slowquery` — Aurora slow query log
- `/aws/lambda/shopflow-*` — Lambda function logs

**Alarms wired to actions:**

| Alarm | Metric | Threshold | Action |
|-------|--------|-----------|--------|
| High ECS CPU | ECS CPUUtilization | > 65% for 2 min | ECS auto scaling already handles; SNS alert |
| High ALB 5xx | HTTPCode_Target_5XX_Count | > 10/min | SNS → PagerDuty |
| ALB p99 latency | TargetResponseTime p99 | > 2s | SNS alert |
| Aurora CPU high | CPUUtilization | > 80% | SNS alert + read replica auto scale triggers |
| Aurora storage low | FreeLocalStorage | < 5 GB | SNS alert (Aurora auto-extends storage) |
| Aurora connections high | DatabaseConnections | > 80% of max | SNS alert (consider RDS Proxy) |
| Redis memory | DatabaseMemoryUsagePercentage | > 75% | SNS alert (add shard) |
| Redis hit rate | CacheHits / (CacheHits + CacheMisses) | < 80% | SNS alert |
| MSK consumer lag | kafka.consumer_lag | > 1000 | SNS alert |
| SQS queue depth | ApproximateNumberOfMessagesVisible | > 100 | Lambda auto-scales; alert at 500 |
| OpenSearch CPU | CPUUtilization | > 80% | SNS alert |

**Dashboard: `shopflow-overview`** — 4 rows:
1. Traffic: ALB request count, response time p50/p99, 5xx rate
2. Compute: ECS task count, ECS CPU/memory, active DB connections
3. Data: Aurora CPU, Redis hit rate, MSK consumer lag, OpenSearch query latency
4. Cost: Estimated daily spend by service (from Cost Explorer API)

**SNS topic:** `shopflow-alerts` — email subscription to `manishkumarsharma24@gmail.com`

### Stack 17: SSM Parameter Store
**File:** `Monitoring/CloudFormation/02_ssm_parameters.yaml`

Non-secret config readable by ECS tasks and Lambda:
- `/shopflow/app/aurora-writer` — Aurora writer endpoint
- `/shopflow/app/aurora-reader` — Aurora reader endpoint
- `/shopflow/app/redis-nodes` — ElastiCache cluster endpoints
- `/shopflow/app/kafka-brokers` — MSK bootstrap brokers
- `/shopflow/app/opensearch-url` — OpenSearch endpoint
- `/shopflow/app/environment` — `production`
- `/shopflow/app/feature/neo4j-enabled` — `false` (toggle for managed Neptune)

---

## Phase 12 — Managed NoSQL (Optional / Advanced)

Map ShopVerse's remaining local Docker NoSQL services to AWS managed equivalents.

### Stack 18: DynamoDB
**File:** `DynamoDB/CloudFormation/01_dynamodb.yaml`

| Table | Mode | Purpose |
|-------|------|---------|
| `shopflow-sessions` | On-demand (auto-scales) | JWT session store (alternative to Redis String) |
| `shopflow-idempotency` | On-demand | Idempotency key store for order placement |

On-demand mode: scales from 0 to 40,000 WCU/RCU per second instantly. Pay per request.

### Stack 19: DocumentDB (MongoDB-compatible)
**File:** `DocumentDB/CloudFormation/01_documentdb.yaml`

For ShopVerse's `ReviewDocument` (product reviews) and `ProductDocument`.
- 1 writer + 1 reader instance (add readers for scale)
- MongoDB 5.0-compatible API — change only the connection URI in `application.yml`
- Scales readers independently from writer

### Stack 20: Amazon Keyspaces (Cassandra-compatible)
**File:** `Keyspaces/CloudFormation/01_keyspaces.yaml`

For ShopVerse's `OrderActivityEntity` (order event log).
- Serverless — scales automatically with traffic, pay per request
- CQL-compatible — `OrderActivityRepository` works without code changes
- Change `application.yml` to point to `cassandra.ap-south-1.amazonaws.com:9142`

### Stack 21: Amazon Neptune (Graph — Neo4j-compatible)
**File:** `Neptune/CloudFormation/01_neptune.yaml`

For ShopVerse's `ProductNode`, `CustomerNode`, `PURCHASED` / `VIEWED_AFTER` relationships (recommendations).
- 1 writer + 1 reader instance
- Supports Cypher queries via openCypher protocol (Neo4j Cypher-compatible)
- Spring Data Neo4j driver works with Neptune openCypher endpoint

---

## Complete Deployment Order

| Order | Stack Name | Template | Phase | Depends On |
|-------|-----------|----------|-------|-----------|
| 1-7 | shopflow-iam-* + shopflow-vpc + shopflow-security-groups | IAM + VPC | ✅ Done | — |
| 8 | shopflow-nat-gateways | VPC/03 | Phase 1 | shopflow-vpc |
| 9 | shopflow-secrets | SecretsManager/01 | Phase 2 | — |
| 10 | shopflow-rds-subnet-group | RDS/01 | Phase 3 | shopflow-vpc |
| 11 | shopflow-aurora | RDS/02 | Phase 3 | shopflow-rds-subnet-group, shopflow-secrets |
| 12 | shopflow-cache-subnet-group | ElastiCache/01 | Phase 4 | shopflow-vpc |
| 13 | shopflow-elasticache | ElastiCache/02 | Phase 4 | shopflow-cache-subnet-group |
| 14 | shopflow-ecr | ECS/01 | Phase 5 | — |
| 15 | shopflow-ecs-cluster | ECS/02 | Phase 5 | — |
| 16 | shopflow-ecs-task | ECS/03 | Phase 5 | shopflow-ecr, shopflow-aurora, shopflow-elasticache |
| 17 | shopflow-alb | ECS/04 | Phase 5 | shopflow-vpc, shopflow-security-groups |
| 18 | shopflow-ecs-service | ECS/05 | Phase 5 | shopflow-ecs-task, shopflow-alb, shopflow-nat-gateways |
| 19 | shopflow-s3-buckets | S3/01 | Phase 6 | — |
| 20 | shopflow-cloudfront | S3/02 | Phase 6 | shopflow-s3-buckets |
| 21 | shopflow-msk | MSK/01 | Phase 7 | shopflow-vpc, shopflow-nat-gateways |
| 22 | shopflow-sqs-queues | SQS/01 | Phase 7 | — |
| 23 | shopflow-lambda | Lambda/01 | Phase 8 | shopflow-aurora, shopflow-sqs-queues |
| 24 | shopflow-opensearch | OpenSearch/01 | Phase 9 | shopflow-vpc |
| 25 | shopflow-cicd | CICD/01 | Phase 10 | shopflow-ecr, shopflow-ecs-cluster |
| 26 | shopflow-cloudwatch | Monitoring/01 | Phase 11 | shopflow-ecs-service, shopflow-aurora |
| 27 | shopflow-ssm-params | Monitoring/02 | Phase 11 | shopflow-aurora, shopflow-elasticache |
| 28 | shopflow-dynamodb | DynamoDB/01 | Phase 12 | — |
| 29 | shopflow-documentdb | DocumentDB/01 | Phase 12 | shopflow-vpc |
| 30 | shopflow-keyspaces | Keyspaces/01 | Phase 12 | — |
| 31 | shopflow-neptune | Neptune/01 | Phase 12 | shopflow-vpc |

---

## Auto-Scaling Summary — All Layers

| Layer | Service | Scales On | Min | Max | Scale-Out Time |
|-------|---------|-----------|-----|-----|----------------|
| CDN | CloudFront | Automatic (managed) | — | Unlimited | Instant |
| Compute | ECS Fargate | CPU 65%, Memory 70%, Requests/task > 1000 | 2 tasks | 10 tasks | ~60s |
| Database | Aurora Serverless v2 | Query load (ACU) + Read replica CPU 70% | 0.5 ACU | 16 ACU (64 max) | Seconds |
| Cache | ElastiCache (cluster) | Manual shard add (ElastiCache Serverless: automatic) | 2 nodes | Unlimited shards | Minutes (online) |
| Search | OpenSearch | Manual data node add | 2 nodes | Unlimited | Minutes |
| Messaging | MSK (or MSK Serverless) | Throughput-based (Serverless: automatic) | 3 brokers | Partition increase | Minutes |
| Async | SQS + Lambda | Queue depth | 0 | 1000 concurrent | Seconds |
| Storage | S3 | Automatic (managed) | — | Unlimited | Instant |

---

## Key Architectural Decisions for Scalability

**Stateless compute is the foundation.**
ShopVerse's JWT auth (stored in Redis, not HttpSession) means any ECS task can handle any request. The ALB routes to all healthy tasks equally — scaling out just means adding more identical tasks. If the app used sticky sessions or stored state in memory, horizontal scaling would break.

**Aurora Serverless v2 instead of standard RDS.**
Standard RDS requires you to pick an instance size upfront (`db.t3.small` caps at ~100 connections). Aurora Serverless v2 scales ACUs in seconds without restart. At low traffic (nights/weekends), it can scale down to 0.5 ACU saving cost. ShopVerse's `RoutingDataSource` maps writes to the writer endpoint and reads to the reader endpoint — Aurora handles the failover and scaling transparently.

**RDS Proxy (optional, recommended for production).**
ECS tasks open JDBC connections at startup. During a scale-out event (2 → 6 tasks), 4 new tasks each open ~10 connections = 40 new DB connections in seconds. Aurora's max connections are based on ACU count. RDS Proxy pools and multiplexes connections — 6 ECS tasks share a proxy pool of 20 connections rather than each holding their own 10. Add this as Stack 11a between ElastiCache and ECS.

**CloudFront absorbs read traffic spikes.**
The React SPA and product images are served from CloudFront edge. A marketing campaign that sends 50,000 users to the site simultaneously hits CloudFront — those requests never reach your ECS tasks. Only API calls (`/api/*`) reach the ALB and ECS.

**The private subnet / NAT Gateway pattern.**
ECS tasks in private subnets cannot be directly accessed from the internet — only the ALB (in public subnets) is publicly reachable. The NAT Gateway lets tasks make outbound calls (ECR image pull, Secrets Manager, external APIs) without being publicly addressable. This is the standard AWS production security pattern.
