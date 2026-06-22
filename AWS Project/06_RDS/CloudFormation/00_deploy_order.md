# ShopFlow RDS — Module 6: Deployment Guide

## What Is RDS?

Amazon RDS (Relational Database Service) manages a MySQL server for you. AWS handles OS patching, DB engine upgrades, automated backups, and failover. You get a MySQL endpoint that your Spring Boot app connects to via JDBC — exactly the same as connecting to localhost, just with a different hostname.

---

## What Gets Created

Two stacks, deployed in order:

| File | Stack Name | What It Creates |
|------|-----------|-----------------|
| `01_rds_subnet_group.yaml` | `shopflow-rds-subnet-group` | DB Subnet Group (required before RDS instance) |
| `02_rds_instance.yaml` | `shopflow-rds-instance` | MySQL 8.0 instance + Parameter Group |

---

## Why Two Stacks?

The subnet group is a prerequisite for the RDS instance. Splitting them means if you ever need to recreate just the instance (different size, different config), you can do it without touching the subnet group.

---

## RDS Instance Details

| Setting | Value | Why |
|---------|-------|-----|
| Engine | MySQL 8.0 | Matches ShopVerse local dev |
| Instance | db.t3.micro | Free tier (750 hours/month) |
| Storage | 20 GB gp3 SSD | Minimum — auto-grows to 100 GB |
| AZ | Single-AZ | Dev only — one AZ is cheaper |
| Backups | 7 days | Point-in-time restore within a week |
| Encryption | AES-256 (at rest) | Always on, free |
| Public access | No | Only reachable from within VPC |
| Password | Auto-generated | From Secrets Manager `/shopflow/db/master` |

---

## Deployment Steps

### Prerequisites

In order:
1. `shopflow-vpc` (Module 2) — private subnets
2. `shopflow-security-groups` (Module 2) — RDS security group
3. `shopflow-secrets` (Module 4) — DB master credentials

### Step 1: Deploy the Subnet Group

1. Open **CloudFormation → Create stack → With new resources**
2. Upload: `01_rds_subnet_group.yaml`
3. Stack name: `shopflow-rds-subnet-group`
4. Parameters: ProjectName = `shopflow`, Environment = `dev`
5. Submit → wait for `CREATE_COMPLETE` (~15 seconds)

### Step 2: Deploy the RDS Instance

1. **CloudFormation → Create stack → With new resources**
2. Upload: `02_rds_instance.yaml`
3. Stack name: `shopflow-rds-instance`
4. Parameters:
   - **ProjectName**: `shopflow`
   - **Environment**: `dev`
   - **DBInstanceClass**: `db.t3.micro`
   - **AllocatedStorageGB**: `20`
5. Submit → wait for `CREATE_COMPLETE`

> **This takes 5–10 minutes.** RDS creates the instance, configures storage, and applies the parameter group. This is normal.

### Step 3: Update SSM Parameter

After the RDS stack reaches `CREATE_COMPLETE`, get the DB endpoint from CloudFormation Outputs and update SSM:

```bash
# Get endpoint from CloudFormation
DB_ENDPOINT=$(aws cloudformation describe-stacks \
  --stack-name shopflow-rds-instance \
  --query "Stacks[0].Outputs[?OutputKey=='DBEndpoint'].OutputValue" \
  --output text)

echo "DB Endpoint: $DB_ENDPOINT"

# Update SSM parameter
aws ssm put-parameter \
  --name "/shopflow/dev/db/endpoint" \
  --value "$DB_ENDPOINT" \
  --type String \
  --overwrite
```

---

## Verify After Deployment

1. Go to **RDS → Databases** in the AWS Console
2. You should see `shopflow-db-dev` with Status: **Available**
3. Click the instance → **Connectivity & security** tab:
   - Endpoint should be a long hostname ending in `rds.amazonaws.com`
   - Publicly accessible: **No**
4. Click **Configuration** tab:
   - DB name: `shopverse`
   - Parameter group: `shopflow-mysql8-params`
5. Click **Maintenance & backups**:
   - Automated backups: **Enabled**, 7-day retention

---

## How Spring Boot Connects to RDS

The ShopVerse `application.yml` uses these values:
```yaml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASS}
```

In ECS (Module 11), the task definition injects these from SSM and Secrets Manager:
- `DB_HOST` → SSM `/shopflow/dev/db/endpoint`
- `DB_PORT` → SSM `/shopflow/dev/db/port` → `3306`
- `DB_NAME` → SSM `/shopflow/dev/db/name` → `shopverse`
- `DB_USER` → Secrets Manager `/shopflow/db/app` JSON key `username`
- `DB_PASS` → Secrets Manager `/shopflow/db/app` JSON key `password`

No code changes needed — only config changes.

---

## Understanding Automated Backups

RDS takes automated snapshots daily during the backup window (2–3 AM UTC):

```
Today        → snapshot kept
Yesterday    → snapshot kept
...
7 days ago   → snapshot kept
8 days ago   → DELETED (beyond 7-day retention)
```

You can restore to any point-in-time within the 7-day window — not just to snapshot times, but to any specific second. RDS uses transaction logs to replay changes from the nearest snapshot.

**To restore (example):**
1. RDS Console → `shopflow-db-dev` → **Actions → Restore to point in time**
2. Pick a time → RDS creates a NEW instance with data from that moment
3. Update SSM parameter to point to the new instance

---

## MySQL Parameter Tuning (Explanation)

The `DBParameterGroup` sets these MySQL server variables:

| Parameter | Value | What It Does |
|-----------|-------|--------------|
| `slow_query_log` | 1 | Log slow queries to CloudWatch |
| `long_query_time` | 1 | "Slow" = longer than 1 second |
| `log_queries_not_using_indexes` | 1 | Find queries missing indexes |
| `innodb_buffer_pool_size` | 734 MB | RAM cache for DB pages (~70% of instance RAM) |
| `max_connections` | 100 | Concurrent connection limit |
| `transaction_isolation` | READ-COMMITTED | Isolation level for CQRS reads |

Slow query logs appear in: **CloudWatch Logs → /aws/rds/instance/shopflow-db-dev/slowquery**

---

## Cost Estimate (Monthly)

| Item | Cost |
|------|------|
| db.t3.micro instance (Single-AZ) | $12.41 |
| 20 GB gp3 storage | $2.30 |
| 7-day backup storage | ~$0 (100% free tier of allocated) |
| **Total** | **~$14.71/month** |

Free tier: 750 hours of db.t3.micro per month for 12 months → **$0 if your account is new**.

---

## Post-Deployment Checklist

- [ ] Stack `shopflow-rds-subnet-group` shows `CREATE_COMPLETE`
- [ ] Stack `shopflow-rds-instance` shows `CREATE_COMPLETE`
- [ ] RDS Console shows `shopflow-db-dev` Status: Available
- [ ] Endpoint visible in RDS Console and CloudFormation Outputs
- [ ] SSM parameter `/shopflow/dev/db/endpoint` updated with real endpoint
- [ ] Performance Insights enabled (visible in RDS Console)

---

## Next Steps

- **Module 7 — ElastiCache (Redis)**: Create the Redis cluster. After it deploys, update SSM `/shopflow/dev/redis/endpoint` with the real endpoint.
