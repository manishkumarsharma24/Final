# ShopFlow ECS Fargate — Module 11: Deployment Guide

## What Is ECS Fargate?

ECS (Elastic Container Service) runs Docker containers. Fargate is the "serverless" mode — you tell AWS what container to run and how much CPU/RAM it needs, and AWS runs it without you managing any EC2 instances.

**EC2 (Module 8) vs ECS Fargate (Module 11):**

| | EC2 | ECS Fargate |
|--|--|--|
| What you manage | OS, JVM, systemd | Nothing — just the Docker image |
| Deployment | Copy JAR + restart service | Push new Docker image tag |
| Scaling | Manual (or ASG) | Change DesiredCount |
| Cost | Pay for instance running 24/7 | Pay per task per second |
| Best for | Long-running, predictable load | Containerised, variable load |

Both work. ECS Fargate is the modern approach for containerised apps.

---

## Prerequisites

1. All of Modules 1–9 deployed
2. Module 10 (ECR) with ShopVerse Docker image pushed
3. Have the image URI ready (from ECR Outputs)

---

## Deployment Steps

### Step 1: Get the Image URI

```bash
IMAGE_URI=$(aws cloudformation describe-stacks \
  --stack-name shopflow-ecr \
  --query "Stacks[0].Outputs[?OutputKey=='RepositoryUri'].OutputValue" \
  --output text)

echo "Image URI: $IMAGE_URI:v1.0"
```

### Step 2: Deploy the Stack

1. **CloudFormation → Create stack → With new resources**
2. Upload: `01_ecs.yaml`
3. Stack name: `shopflow-ecs`
4. Parameters:
   - **ProjectName**: `shopflow`
   - **Environment**: `dev`
   - **ImageUri**: paste the image URI + tag (e.g., `123456789.dkr.ecr.ap-south-1.amazonaws.com/shopflow-shopverse:v1.0`)
   - **DesiredCount**: `1`
5. Submit → wait for `CREATE_COMPLETE` (~3–5 minutes)

### Step 3: Watch Tasks Start

1. **ECS → Clusters → shopflow-cluster-dev → Tasks** tab
2. Watch the task transition: `PROVISIONING → PENDING → RUNNING`
3. Once RUNNING, click the task → **Logs** tab to see Spring Boot startup logs

---

## Verify After Deployment

```bash
# The ALB DNS is the same as Module 9 — now pointing to ECS tasks
ALB_DNS=$(aws cloudformation describe-stacks \
  --stack-name shopflow-alb \
  --query "Stacks[0].Outputs[?OutputKey=='ALBDNSName'].OutputValue" \
  --output text)

# Test health
curl http://$ALB_DNS/actuator/health

# Test API
curl http://$ALB_DNS/api/products
```

---

## Deploying a New Version (Rolling Update)

```bash
# 1. Build and push new Docker image
docker build -t shopverse .
docker tag shopverse:latest ${IMAGE_URI}:v1.1
docker push ${IMAGE_URI}:v1.1

# 2. Update the CloudFormation stack with new image URI
aws cloudformation update-stack \
  --stack-name shopflow-ecs \
  --use-previous-template \
  --parameters \
    ParameterKey=ProjectName,ParameterValue=shopflow \
    ParameterKey=Environment,ParameterValue=dev \
    ParameterKey=ImageUri,ParameterValue=${IMAGE_URI}:v1.1 \
    ParameterKey=DesiredCount,UsePreviousValue=true

# ECS performs a rolling update:
# 1. Launch new task with v1.1
# 2. Wait for health check to pass
# 3. Stop old task with v1.0
```

The `DeploymentCircuitBreaker` will automatically roll back if the new tasks fail health checks.

---

## Post-Deployment Checklist

- [ ] Stack `shopflow-ecs` shows `CREATE_COMPLETE`
- [ ] ECS task shows State: RUNNING
- [ ] CloudWatch Logs show Spring Boot startup (log group: `/ecs/shopflow-shopverse-dev`)
- [ ] ALB Target Group shows ECS task IP as Healthy
- [ ] `curl http://<ALB-DNS>/actuator/health` returns `{"status":"UP"}`

---

## Next Steps

- **Module 12 — SQS**: Create message queues for async processing (email notifications, payment callbacks).
