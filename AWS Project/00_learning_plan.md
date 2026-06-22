# ShopFlow — AWS Learning Plan
> App: ShopVerse (Spring Boot 3.2.5 / Java 21 / MySQL / Redis / Kafka)
> Region: ap-south-1 (Mumbai) · Stack prefix: `shopflow-`
> Each module follows the same pattern: CloudFormation templates + step-by-step deploy guide

---

## Already Completed

| # | Module | Folder | What You Learned |
|---|--------|--------|-----------------|
| 1 | IAM | `IAM/CloudFormation/` | Policies, groups, roles, users, password policy |
| 2 | VPC | `VPC/CloudFormation/` | VPC, subnets, IGW, route tables, security groups |

---

## Learning Sequence (Simple — No Auto Scaling)

Each module is independent enough to learn one at a time. Deploy in this order because each module depends on the ones above it.

| # | Module | Folder | Key Concept | ShopVerse Use |
|---|--------|--------|-------------|---------------|
| 3 | **S3** | `S3/CloudFormation/` | Object storage — store any file, forever | App JARs, product images, CI/CD artifacts, RDS backups |
| 4 | **Secrets Manager** | `SecretsManager/CloudFormation/` | Store secrets encrypted; auto-rotate DB passwords | DB password, JWT secret, Redis auth token |
| 5 | **SSM Parameter Store** | `SSM/CloudFormation/` | Store non-secret config strings centrally | Redis endpoint, Kafka brokers, feature flags |
| 6 | **RDS (MySQL)** | `RDS/CloudFormation/` | Managed relational database | ShopVerse primary DB — orders, products, customers |
| 7 | **ElastiCache (Redis)** | `ElastiCache/CloudFormation/` | Managed in-memory cache | JWT sessions, product cache, rate limiter, cart |
| 8 | **EC2** | `EC2/CloudFormation/` | Virtual machine — run any software | Run Spring Boot app (port 8080) |
| 9 | **ALB** | `ALB/CloudFormation/` | Application Load Balancer — distribute traffic | Public entry point → EC2 on port 8080 |
| 10 | **ECR** | `ECR/CloudFormation/` | Docker image registry | Store ShopVerse Docker images |
| 11 | **ECS Fargate** | `ECS/CloudFormation/` | Run containers without managing servers | Alternative to EC2 — run ShopVerse as a container |
| 12 | **SQS** | `SQS/CloudFormation/` | Message queue — decouple services | Notifications, payment callbacks, webhook delivery |
| 13 | **SNS** | `SNS/CloudFormation/` | Pub/sub notifications — fan out to many | Alert ops team on failures; trigger Lambda from SQS |
| 14 | **Lambda** | `Lambda/CloudFormation/` | Serverless function — run code without a server | Send emails, process payments, sync search index |
| 15 | **DynamoDB** | `DynamoDB/CloudFormation/` | Managed NoSQL key-value store | JWT session store, idempotency keys |
| 16 | **CloudWatch** | `Monitoring/CloudFormation/` | Logs, metrics, alarms, dashboards | Monitor EC2/ECS health, alert on errors |
| 17 | **CodeBuild** | `CICD/CloudFormation/` | Build and test code in the cloud | Compile ShopVerse, run tests, build Docker image |
| 18 | **CodePipeline** | `CICD/CloudFormation/` | Automated deploy pipeline | Source → Build → Deploy to ECS on every push |

---

## What Each Service Does (Plain English)

### S3 — Simple Storage Service
Think of S3 as an unlimited hard drive in the cloud. You store files (called **objects**) inside containers (called **buckets**). Files can be private (only your app reads them) or public (product images served to users). Used by almost every other AWS service as a place to put files.

### Secrets Manager
Stores sensitive values like database passwords and API keys, encrypted at rest. The key feature: it can **automatically rotate** database passwords on a schedule — it updates the password in both the DB and the secret without any downtime. Your app fetches the secret at runtime using an IAM role — the password never appears in your config files or environment variables in plain text.

### SSM Parameter Store
Like Secrets Manager but for non-sensitive config — endpoints, feature flags, environment names. Free for Standard parameters. Your ECS tasks and Lambda functions read these at startup so you never hardcode config values in your code or Docker images.

### RDS (Relational Database Service)
AWS manages the MySQL server for you — patching, backups, failover. You pick the instance size and AWS handles the rest. The DB sits in your private subnets (no direct internet access). Your Spring Boot app connects via JDBC exactly as it does locally — only the hostname changes.

### ElastiCache (Redis)
AWS managed Redis. ShopVerse uses Redis for 8 things: JWT session validation on every request, product/order caching, rate limiting (Lua script), shopping cart (Hash), distributed locks (Redisson), recently viewed products (List), leaderboard (Sorted Set), and pub/sub for low-stock alerts. All of this works with a managed Redis cluster — just change the hostname.

### EC2 (Elastic Compute Cloud)
A virtual machine in the cloud. You choose the OS, CPU, and RAM. Your Spring Boot JAR runs on it exactly as it does on your laptop — but it's always on and accessible from the internet (via ALB). EC2 lives in your private subnet; users reach it through the ALB in the public subnet.

### ALB (Application Load Balancer)
Sits in the public subnet and receives HTTP/HTTPS traffic from users. Routes requests to EC2 instances (or ECS tasks) in the private subnet. Performs health checks — if an instance is unhealthy, the ALB stops sending traffic to it. Also handles SSL termination (you don't need HTTPS on EC2).

### ECR (Elastic Container Registry)
AWS's private Docker Hub. You `docker push` your ShopVerse image here, then ECS pulls it to run containers. Images are encrypted, scanned for vulnerabilities, and access is controlled via IAM.

### ECS Fargate (Elastic Container Service)
Run Docker containers without managing any servers. You define what container to run (from ECR) and how much CPU/RAM it needs — AWS handles everything else. Fargate is the serverless version (no EC2 to manage). ECS + Fargate is the modern replacement for running apps directly on EC2.

### SQS (Simple Queue Service)
A message queue that decouples two services. Producer puts a message in the queue; consumer picks it up when ready. If the consumer crashes, the message stays in the queue and is retried. ShopVerse uses SQS for email notifications, payment callbacks, and merchant webhooks — the same jobs that RabbitMQ handles locally.

### SNS (Simple Notification Service)
Broadcasts one message to many subscribers simultaneously (fan-out). Send one alert to SNS and it forwards to email, SMS, Lambda, and SQS all at once. Used in ShopVerse to alert the ops team when CloudWatch alarms fire.

### Lambda
Runs a function in response to an event — no server to manage, and you pay only when it runs. A Lambda function wakes up, processes one event, and sleeps again. ShopVerse uses Lambda to send transactional emails (triggered by SQS), process payment callbacks, and sync products to OpenSearch. Scales from 0 to thousands of parallel executions automatically.

### DynamoDB
AWS's fully managed NoSQL database. Stores data as key-value pairs or JSON documents. Scales to any throughput without configuration. ShopVerse uses it for the JWT session store (`UserSessionTable` — single table design) and idempotency keys for order placement.

### CloudWatch
AWS's built-in monitoring service. Collects logs from EC2, ECS, Lambda, and RDS. Lets you set **alarms** (e.g., "alert me if ALB 5xx rate exceeds 5%") and build **dashboards** showing all key metrics in one view. The central observability tool for everything running in your AWS account.

### CodeBuild
A CI build server in the cloud. Checks out your code, runs `mvn clean package`, builds the Docker image, and pushes it to ECR. You define the build steps in a `buildspec.yml`. Replaces Jenkins or local `mvn` commands in a CI pipeline.

### CodePipeline
Orchestrates the full CI/CD flow: Source (GitHub) → Build (CodeBuild) → Deploy (ECS). Triggered automatically on every git push. Chains the stages together and shows you the status of each deployment. Think of it as the conductor that coordinates CodeBuild and ECS rolling deploy.

---

## Folder Structure (Full)

```
F:\Final\AWS Project\
│
├── 00_learning_plan.md              ← This file
├── AWS_Services_Roadmap.md          ← Detailed technical roadmap
│
├── IAM\                             ✅ Done
├── VPC\                             ✅ Done
│
├── S3\CloudFormation\               ← Module 3
├── SecretsManager\CloudFormation\   ← Module 4
├── SSM\CloudFormation\              ← Module 5
├── RDS\CloudFormation\              ← Module 6
├── ElastiCache\CloudFormation\      ← Module 7
├── EC2\CloudFormation\              ← Module 8
├── ALB\CloudFormation\              ← Module 9
├── ECR\CloudFormation\              ← Module 10
├── ECS\CloudFormation\              ← Module 11
├── SQS\CloudFormation\              ← Module 12
├── SNS\CloudFormation\              ← Module 13
├── Lambda\CloudFormation\           ← Module 14
├── DynamoDB\CloudFormation\         ← Module 15
├── Monitoring\CloudFormation\       ← Module 16
└── CICD\CloudFormation\             ← Modules 17-18
```

---

## Progress Tracker

- [x] Module 1 — IAM
- [x] Module 2 — VPC
- [x] Module 3 — S3
- [x] Module 4 — Secrets Manager
- [x] Module 5 — SSM Parameter Store
- [x] Module 6 — RDS
- [x] Module 7 — ElastiCache
- [x] Module 8 — EC2
- [x] Module 9 — ALB
- [x] Module 10 — ECR
- [x] Module 11 — ECS Fargate
- [x] Module 12 — SQS
- [x] Module 13 — SNS
- [x] Module 14 — Lambda
- [x] Module 15 — DynamoDB
- [x] Module 16 — CloudWatch
- [x] Module 17-18 — CI/CD (CodeBuild + CodePipeline)
