# Module 05 — SSM Parameter Store: Deploy Guide

**Prerequisites:** Module 01 (IAM) must be deployed.
**Stack name:** `shopflow-ssm`
**Template file:** `01_ssm_parameters.yaml`
**Region:** `ap-south-1` (Mumbai)
**Free Tier:** SSM Standard parameters are completely free — no storage or API call limits.

---

## What Is SSM Parameter Store?

AWS Systems Manager (SSM) Parameter Store is a centralized store for configuration strings. Think of it as a shared `.env` file in the cloud that all your services can read securely. Unlike Secrets Manager, it's **free for standard parameters** and designed for app configuration.

**The core idea:** instead of hardcoding `redis.host=localhost` in your `application.yml` or Docker image, you store it in SSM and your app fetches it at startup. To change a value, you update the parameter — no rebuild, no redeploy, no commit.

---

## Secrets Manager vs. SSM Parameter Store

| | Secrets Manager | SSM Parameter Store |
|--|--|--|
| **Use for** | Passwords, tokens, API keys | Hostnames, ports, flags, config |
| **Encryption** | Always encrypted (KMS) | Optional (SecureString) |
| **Auto-rotation** | Yes (with Lambda) | No |
| **Cost** | $0.40/secret/month | Free (Standard) |
| **Version history** | Yes | Yes (last 100 versions) |

ShopFlow rule: if it's a password or signing key → Secrets Manager. If it's a hostname, port, or feature flag → SSM.

---

## Parameters Created (27 total)

### PostgreSQL Database
| Path | Default Value | Notes |
|------|--------------|-------|
| `/shopflow/dev/database/host` | `localhost` | Update to RDS endpoint after Module 06 |
| `/shopflow/dev/database/port` | `5432` | PostgreSQL default |
| `/shopflow/dev/database/name` | `shopverse` | DB name |
| `/shopflow/dev/database/username` | `shopverse` | DB username |
| `/shopflow/dev/database/password` | `CHANGE_ME` | **Update in console immediately** |
| `/shopflow/dev/database/url` | `jdbc:postgresql://localhost:5432/shopverse` | Full JDBC URL |

### Redis
| Path | Default Value | Notes |
|------|--------------|-------|
| `/shopflow/dev/redis/host` | `localhost` | Docker: use container name |
| `/shopflow/dev/redis/port` | `6379` | Redis default |
| `/shopflow/dev/redis/password` | _(empty)_ | No auth for local dev |

### Kafka
| Path | Default Value | Notes |
|------|--------------|-------|
| `/shopflow/dev/kafka/brokers` | `localhost:9092` | Comma-separated broker list |
| `/shopflow/dev/kafka/topics/orders` | `order-events` | Topic for OrderKafkaProducer |
| `/shopflow/dev/kafka/group-id` | `shopverse-consumer-group` | Consumer group ID |

### JWT Security
| Path | Default Value | Notes |
|------|--------------|-------|
| `/shopflow/dev/jwt/secret` | `CHANGE_ME` | **Update in console — run `openssl rand -base64 32`** |
| `/shopflow/dev/jwt/expiration-ms` | `86400000` | 24-hour token lifetime |

### MongoDB
| Path | Default Value | Notes |
|------|--------------|-------|
| `/shopflow/dev/mongodb/uri` | `mongodb://localhost:27017/shopverse` | Atlas URI for cloud |
| `/shopflow/dev/mongodb/database` | `shopverse` | DB name for ReviewDocument |

### Elasticsearch
| Path | Default Value | Notes |
|------|--------------|-------|
| `/shopflow/dev/elasticsearch/uri` | `http://localhost:9200` | REST API endpoint |
| `/shopflow/dev/elasticsearch/index/products` | `shopverse-products` | ProductDocument index |

### Application
| Path | Default Value | Notes |
|------|--------------|-------|
| `/shopflow/dev/app/port` | `8080` | Spring Boot server port |
| `/shopflow/dev/app/profile` | `dev` | Spring active profile |
| `/shopflow/dev/app/base-url` | `http://localhost:8080` | Update to EC2 IP after deploy |

### Cassandra
| Path | Default Value | Notes |
|------|--------------|-------|
| `/shopflow/dev/cassandra/contact-points` | `localhost` | For OrderActivityEntity |
| `/shopflow/dev/cassandra/port` | `9042` | CQL native transport port |
| `/shopflow/dev/cassandra/keyspace` | `shopverse` | Keyspace name |

### Neo4j
| Path | Default Value | Notes |
|------|--------------|-------|
| `/shopflow/dev/neo4j/uri` | `bolt://localhost:7687` | Bolt protocol URI |
| `/shopflow/dev/neo4j/username` | `neo4j` | Default Neo4j username |
| `/shopflow/dev/neo4j/password` | `CHANGE_ME` | **Update in console** |

---

## Deployment Steps

### Prerequisites
- Stack `shopflow-iam-policies` deployed (EC2/ECS execution role needs `ssm:GetParameter`)
- No VPC or network dependency

### Step 1: Deploy the Stack

1. Open **AWS Console → CloudFormation → Create stack → With new resources**
2. Choose **Upload a template file** → select: `01_ssm_parameters.yaml`
3. Stack name: `shopflow-ssm`
4. Parameters:
   - **ProjectName**: `shopflow`
   - **Environment**: `dev`
5. Click **Next** → **Next** → **Submit** → wait for `CREATE_COMPLETE` (~30 seconds — parameters create very fast)

### Step 2: Update Sensitive Values Immediately

Go to **Systems Manager → Parameter Store** and update these three placeholders:

| Parameter | What to set |
|-----------|-------------|
| `/shopflow/dev/database/password` | Your PostgreSQL password |
| `/shopflow/dev/jwt/secret` | Output of `openssl rand -base64 32` |
| `/shopflow/dev/neo4j/password` | Your Neo4j password |

Click the parameter name → **Edit** → replace the value → **Save changes**.

### Step 3: Verify in Console

1. Go to **AWS Console → Systems Manager → Parameter Store**
2. Search for `/shopflow` — you should see 27 parameters
3. Click `/shopflow/dev/database/url` → Value should be `jdbc:postgresql://localhost:5432/shopverse`

---

## Updating Parameters After Later Modules Deploy

When you run services locally with Docker Compose, the `localhost` defaults work. If you later move to cloud services, update via CLI:

```bash
# Update database host after RDS deploys (Module 06 — paid, optional)
aws ssm put-parameter \
  --name "/shopflow/dev/database/host" \
  --value "shopflow-db.xxxxxxxxxx.ap-south-1.rds.amazonaws.com" \
  --type String \
  --overwrite \
  --region ap-south-1

# Update full JDBC URL too
aws ssm put-parameter \
  --name "/shopflow/dev/database/url" \
  --value "jdbc:postgresql://shopflow-db.xxx.ap-south-1.rds.amazonaws.com:5432/shopverse" \
  --type String \
  --overwrite \
  --region ap-south-1
```

---

## How to Toggle a Feature Flag Without Redeployment

```bash
# Turn on maintenance mode before a DB migration
aws ssm put-parameter \
  --name "/shopflow/dev/feature/maintenance-mode" \
  --value "true" \
  --type String \
  --overwrite

# ... run migration ...

# Turn it off when done
aws ssm put-parameter \
  --name "/shopflow/dev/feature/maintenance-mode" \
  --value "false" \
  --type String \
  --overwrite
```

Your Spring Boot app reads this parameter via the AWS SDK. If you implement it as a `@RefreshScope` bean (Spring Cloud AWS), it picks up the change without restart.

---

## How Spring Boot Reads SSM Parameters

Option A — ECS task definition injection (simplest, no code change):
```json
{
  "environment": [
    {
      "name": "REDIS_HOST",
      "valueFrom": "arn:aws:ssm:ap-south-1:123456789:parameter/shopflow/dev/redis/endpoint"
    }
  ]
}
```

Option B — AWS SDK in Spring Boot (real-time reads):
```java
// Using Spring Cloud AWS (automatically maps /shopflow/dev/* to properties)
@Value("${shopflow.dev.redis.endpoint}")
private String redisEndpoint;
```

Option C — Manual SDK call at startup:
```java
SsmClient ssm = SsmClient.builder().region(Region.AP_SOUTH_1).build();
String endpoint = ssm.getParameter(r -> r.name("/shopflow/dev/redis/endpoint"))
                     .parameter().value();
```

We will use Option A (task definition injection) in Module 11 (ECS) — no code changes required.

---

## Viewing Parameter Change History

SSM keeps the last 100 versions of every parameter:

```bash
# See version history
aws ssm get-parameter-history \
  --name "/shopflow/dev/feature/maintenance-mode"
```

This audit trail shows who changed what and when — useful for incident investigation.

---

## Post-Deployment Checklist

- [ ] Stack `shopflow-ssm` shows `CREATE_COMPLETE`
- [ ] 27 parameters visible in Systems Manager → Parameter Store under `/shopflow/dev`
- [ ] `/shopflow/dev/database/url` shows `jdbc:postgresql://localhost:5432/shopverse`
- [ ] `/shopflow/dev/jwt/expiration-ms` shows `86400000`
- [ ] Sensitive placeholders updated (database/password, jwt/secret, neo4j/password)

---

## Next Steps

**Module 12 — SQS**: Create the async order event queue. SQS gives you 1 million messages/month free — used by the `OrderKafkaProducer` / `OrderKafkaConsumer` pattern in ShopVerse.

> **Skipping Modules 06 (RDS), 07 (ElastiCache), 08 (EC2), 09 (ALB), 11 (ECS)** — all are paid services. All SSM parameters for those services default to `localhost`, which works perfectly with Docker Compose for local development.
