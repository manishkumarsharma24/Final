# ShopFlow ALB — Module 9: Deployment Guide

## What Is ALB?

An Application Load Balancer (ALB) is the public entry point for your application. It sits in the public subnets, accepts HTTP requests from the internet, and forwards them to EC2 instances or ECS tasks in the private subnets.

```
Internet
   │
   ↓ Port 80 (HTTP)
┌─────────────────────────┐
│   ALB (public subnet)   │  ← This module
└─────────────────────────┘
   │
   ↓ Port 8080
┌─────────────────────────┐
│ EC2 (private subnet)    │  ← Module 8
│ Spring Boot :8080       │
└─────────────────────────┘
```

---

## What Gets Created

| Resource | Purpose |
|----------|---------|
| `TargetGroup` | Defines EC2 port 8080 as the destination + health check config |
| `ALB` | The load balancer in public subnets (accepts port 80 from internet) |
| `HTTPListener` | Rule: listen on port 80, forward to TargetGroup |
| `EC2TargetAttachment` | Registers the EC2 instance (Module 8) into the TargetGroup |

---

## Prerequisites

1. `shopflow-vpc` + `shopflow-security-groups` (Module 2)
2. `shopflow-ec2-instance` (Module 8) — must be running and healthy

---

## Deployment Steps

### Step 1: Deploy the Stack

1. Open **CloudFormation → Create stack → With new resources**
2. Upload: `01_alb.yaml`
3. Stack name: `shopflow-alb`
4. Parameters:
   - **ProjectName**: `shopflow`
   - **Environment**: `dev`
5. Submit → wait for `CREATE_COMPLETE` (~2 minutes)

### Step 2: Test the Application

After deployment, get the ALB DNS name from CloudFormation Outputs:

```bash
ALB_DNS=$(aws cloudformation describe-stacks \
  --stack-name shopflow-alb \
  --query "Stacks[0].Outputs[?OutputKey=='ALBDNSName'].OutputValue" \
  --output text)

echo "Application URL: http://$ALB_DNS"

# Test the health endpoint
curl http://$ALB_DNS/actuator/health

# Test the products API (no auth needed — public endpoint)
curl http://$ALB_DNS/api/products

# Test login
curl -X POST http://$ALB_DNS/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@shopverse.com", "password": "your-password"}'
```

---

## Verify After Deployment

### In the Console

1. **EC2 → Load Balancers** — look for `shopflow-alb-dev` with State: **Active**
2. Click the ALB → **Listeners** tab → confirm port 80 listener exists
3. Click the listener → **Default rule** → confirm it forwards to `shopflow-tg-dev`
4. **EC2 → Target Groups** → click `shopflow-tg-dev` → **Targets** tab:
   - You should see your EC2 instance with Health status: **Healthy** (takes ~1 minute after app starts)

### Health Check States

| Status | Meaning |
|--------|---------|
| `initial` | Target was just registered, waiting for first check |
| `healthy` | Health check returned 200 — ALB is routing traffic here |
| `unhealthy` | Health check failed 3 times — ALB stopped routing traffic |
| `draining` | Target is being removed — ALB is finishing in-flight requests |

If the target stays `unhealthy`:
1. SSH/SSM into the EC2 instance
2. Run `curl localhost:8080/actuator/health`
3. If it returns 200, check the Security Group — EC2-SG must allow port 8080 from ALB-SG
4. If it returns an error, check Spring Boot logs: `journalctl -u shopverse -f`

---

## Understanding the Health Check

The ALB calls `GET /actuator/health` on the EC2 instance every 30 seconds:

```
ALB health check → EC2:8080/actuator/health
→ Spring Boot Actuator returns: {"status":"UP"}  (HTTP 200)
→ ALB marks target: Healthy
→ ALB sends user requests to this target

OR:

→ Spring Boot not running / returns non-200
→ ALB marks: Unhealthy (after 3 failures)
→ ALB stops routing user requests to this target
→ Users see a 503 (no healthy targets)
```

Spring Boot Actuator is already in the ShopVerse dependencies. No code changes needed — the endpoint is enabled by default.

---

## Path-Based Routing (For Future Reference)

The current listener forwards ALL traffic to the ShopVerse Target Group. You can add routing rules later. For example, if you add a separate microservice:

```
/api/search/* → SearchService Target Group (Elasticsearch)
/api/products/* → ShopVerse Target Group
/*              → ShopVerse Target Group (default)
```

This is added as "Listener Rules" on the same ALB — no new ALB needed.

---

## Why HTTP for Learning?

This template uses HTTP (port 80) for simplicity. For production:

1. Register a domain (Route 53)
2. Request a free SSL certificate (AWS Certificate Manager)
3. Add an HTTPS listener (port 443) using the certificate
4. Add a redirect rule: HTTP:80 → HTTPS:443 (301 redirect)

Cost: SSL certificates from ACM are **free** when used with ALB.

---

## Post-Deployment Checklist

- [ ] Stack `shopflow-alb` shows `CREATE_COMPLETE`
- [ ] ALB Console shows State: Active
- [ ] Target Group shows EC2 instance as Healthy
- [ ] `curl http://<ALB-DNS>/actuator/health` returns `{"status":"UP"}`
- [ ] `curl http://<ALB-DNS>/api/products` returns product data (or empty array)

---

## Next Steps

- **Module 10 — ECR**: Create a Docker image registry. You'll build a ShopVerse Docker image and push it here, ready for ECS Fargate in Module 11.
