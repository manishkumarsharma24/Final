# ShopFlow EC2 — Module 8: Deployment Guide

## What Is EC2?

Amazon EC2 (Elastic Compute Cloud) is a virtual machine in the cloud. You choose the OS, CPU, and RAM — AWS provides the physical hardware. Your Spring Boot app runs on it exactly as it would on your laptop, just with a different hostname for the DB and Redis.

---

## What Gets Created

| Resource | Purpose |
|----------|---------|
| `LaunchTemplate` | Config blueprint for the EC2 instance |
| `EC2Instance` | The running virtual machine (t3.small, Amazon Linux 2023) |

---

## What Happens on First Boot (User Data)

The User Data script runs automatically when the instance starts for the first time:

```
1. Update OS packages
2. Install Java 21 (Amazon Corretto) 
3. Install CloudWatch Agent (for log shipping)
4. Download ShopVerse JAR from S3
5. Fetch config from SSM Parameter Store (DB host, Redis host, etc.)
6. Fetch secrets from Secrets Manager (DB password, JWT secret, Redis auth)
7. Write /opt/shopverse/app.env (environment file — permissions 600)
8. Register Spring Boot as a systemd service
9. Start the service
```

After ~2 minutes, the Spring Boot app is running on port 8080.

---

## Prerequisites

In order:
1. `shopflow-iam-policies` + `shopflow-iam-roles` (Module 1) — EC2 instance profile
2. `shopflow-vpc` + `shopflow-security-groups` (Module 2) — private subnet + EC2 SG
3. `shopflow-s3-buckets` (Module 3) — artifacts bucket (JAR must already be uploaded)
4. `shopflow-secrets` (Module 4) — DB password, JWT secret, Redis auth
5. `shopflow-ssm-parameters` (Module 5) — with real DB/Redis endpoints filled in
6. `shopflow-rds-instance` (Module 6) — RDS must be running
7. `shopflow-elasticache-redis` (Module 7) — Redis must be running

### Upload the JAR to S3 First

Before launching EC2, build and upload the JAR:

```bash
# Build ShopVerse (from F:\Final\ShopVerse directory)
./mvnw clean package -DskipTests -pl shopverse-web

# Upload to S3
aws s3 cp shopverse-web/target/shopverse-web-*.jar \
  s3://shopflow-artifacts-dev/shopverse-web.jar
```

---

## Deployment Steps

### Step 1: Deploy the Stack

1. Open **CloudFormation → Create stack → With new resources**
2. Upload: `01_ec2_instance.yaml`
3. Stack name: `shopflow-ec2-instance`
4. Parameters:
   - **ProjectName**: `shopflow`
   - **Environment**: `dev`
   - **InstanceType**: `t3.small`
   - **ArtifactsBucketName**: `shopflow-artifacts-dev`
   - **JarFileName**: `shopverse-web.jar`
5. Submit → wait for `CREATE_COMPLETE` (~2 minutes for stack; ~3 more for User Data)

---

## Verify After Deployment

### Check Instance is Running

1. **EC2 Console → Instances** — look for `shopflow-shopverse-dev` with State: Running
2. Note the Private IP address (no public IP — it's in a private subnet)

### Connect via SSM Session Manager (No SSH Required)

```bash
# Connect to the instance using AWS CLI + SSM (no port 22, no key pair)
aws ssm start-session \
  --target <instance-id> \
  --region ap-south-1
```

Or in the console: **EC2 → Instances → Select instance → Connect → Session Manager → Connect**

### Check Application Status

Once connected via Session Manager:

```bash
# Check if Spring Boot started successfully
systemctl status shopverse

# View live logs
journalctl -u shopverse -f

# Check if port 8080 is listening
ss -tlnp | grep 8080

# Test the health endpoint
curl http://localhost:8080/actuator/health
```

Expected response from health endpoint:
```json
{"status": "UP"}
```

---

## Why SSM Session Manager Instead of SSH?

Traditional SSH requires:
- Opening port 22 on the security group (attack surface)
- Creating and managing key pairs (lost key = no access)
- Either a bastion host or VPN for private subnet access

SSM Session Manager:
- **Zero open inbound ports** — the agent initiates the connection outbound
- **IAM-controlled access** — who can connect is managed by IAM, not key files
- **Audit log** — every session recorded in CloudTrail
- **Works in private subnets** — the agent connects via the SSM endpoint in your VPC

---

## Reading Application Logs

Logs from the Spring Boot app go to the system journal:

```bash
# All logs
journalctl -u shopverse

# Tail logs in real time
journalctl -u shopverse -f

# Logs from the last hour
journalctl -u shopverse --since "1 hour ago"

# Filter for ERROR level
journalctl -u shopverse | grep "ERROR"
```

In Module 16 (CloudWatch), we will configure the CloudWatch Agent to ship these logs to CloudWatch Logs, so you can view them from the console without connecting to the instance.

---

## Restarting the App After a New JAR Deploy

```bash
# Upload new JAR
aws s3 cp shopverse-web-1.1.jar s3://shopflow-artifacts-dev/shopverse-web.jar

# On the EC2 instance (via SSM):
aws s3 cp s3://shopflow-artifacts-dev/shopverse-web.jar /opt/shopverse/shopverse.jar
systemctl restart shopverse
journalctl -u shopverse -f  # Watch startup
```

This is the manual deploy process. In Module 18 (CodePipeline), this becomes automated.

---

## Cost Estimate (Monthly)

| Item | Cost |
|------|------|
| t3.small instance (24/7) | $15.18 |
| 30 GB gp3 EBS volume | $2.40 |
| **Total** | **~$17.58/month** |

Free tier: 750 hours of t2.micro per month for 12 months. Note: t3.small is NOT free tier — use t2.micro if you want free tier, but Spring Boot may be slow.

---

## Post-Deployment Checklist

- [ ] JAR uploaded to `shopflow-artifacts-dev` S3 bucket
- [ ] Stack `shopflow-ec2-instance` shows `CREATE_COMPLETE`
- [ ] EC2 Console shows instance State: Running
- [ ] SSM Session Manager connection works
- [ ] `systemctl status shopverse` → Active: active (running)
- [ ] `curl localhost:8080/actuator/health` → `{"status":"UP"}`

---

## Next Steps

- **Module 9 — ALB**: Create an Application Load Balancer in the public subnets to route internet traffic to this EC2 instance on port 8080. After the ALB is deployed, ShopVerse will be accessible from the internet.
