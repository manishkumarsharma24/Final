# ShopFlow DynamoDB — Module 15: Deployment Guide

## What Is DynamoDB?

Amazon DynamoDB is a fully managed NoSQL key-value database. No servers, no clusters — you define a table, put items in, get items out. It scales from zero to millions of requests per second automatically.

---

## Tables Created

| Table | Key | TTL | Purpose |
|-------|-----|-----|---------|
| `shopflow-sessions-dev` | `tokenHash` (PK) + userId-index (GSI) | `expiresAt` | JWT session store |
| `shopflow-idempotency-dev` | `idempotencyKey` (PK) | `expiresAt` | Duplicate order prevention |

---

## Deployment

### Step 1: Deploy

1. **CloudFormation → Create stack → With new resources**
2. Upload: `01_dynamodb.yaml`
3. Stack name: `shopflow-dynamodb`
4. Parameters: ProjectName = `shopflow`, Environment = `dev`
5. Submit → `CREATE_COMPLETE` (~15 seconds)

### Step 2: Test with CLI

```bash
# Write a test session item
aws dynamodb put-item \
  --table-name shopflow-sessions-dev \
  --item '{
    "tokenHash": {"S": "abc123hash"},
    "userId": {"S": "user-uuid-001"},
    "email": {"S": "manish@example.com"},
    "expiresAt": {"N": "9999999999"}
  }'

# Read it back
aws dynamodb get-item \
  --table-name shopflow-sessions-dev \
  --key '{"tokenHash": {"S": "abc123hash"}}'

# Query by userId (using the GSI)
aws dynamodb query \
  --table-name shopflow-sessions-dev \
  --index-name userId-index \
  --key-condition-expression "userId = :uid" \
  --expression-attribute-values '{":uid": {"S": "user-uuid-001"}}'
```

---

## How TTL Works

DynamoDB automatically deletes items when the `expiresAt` timestamp has passed:

```
Item created with expiresAt = now + 86400 (24 hours from now)

→ 24 hours later: DynamoDB marks item as expired
→ Within 48 hours: item is deleted from table
→ Item no longer appears in queries immediately after expiry
```

Cost: free. No Lambda or cron job needed for cleanup.

---

## On-Demand vs Provisioned Capacity

This template uses `PAY_PER_REQUEST` (on-demand):
- No configuration needed
- Pay per read/write: $0.00025 per read, $0.00125 per write
- For dev with low traffic: essentially free

For production with predictable high load, switch to Provisioned + Auto Scaling for ~40% cost savings.

---

## Post-Deployment Checklist

- [ ] Stack `shopflow-dynamodb` shows `CREATE_COMPLETE`
- [ ] Both tables visible in DynamoDB Console
- [ ] TTL enabled on both tables (visible in table details)
- [ ] CLI read/write test successful

---

## Next Steps

- **Module 16 — CloudWatch**: Set up log groups, alarms, and a dashboard to monitor all the services you've built.
