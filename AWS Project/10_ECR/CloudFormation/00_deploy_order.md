# ShopFlow ECR — Module 10: Deployment Guide

## What Is ECR?

Amazon ECR (Elastic Container Registry) is a private Docker image registry. Think of it as Docker Hub, but private, encrypted, and integrated with AWS IAM. You push your ShopVerse Docker image here, and ECS Fargate pulls it from here to run containers.

---

## Deployment Steps

### Step 1: Deploy the Stack

1. **CloudFormation → Create stack → With new resources**
2. Upload: `01_ecr.yaml`
3. Stack name: `shopflow-ecr`
4. Parameters: ProjectName = `shopflow`, Environment = `dev`
5. Submit → wait for `CREATE_COMPLETE` (~15 seconds)

### Step 2: Build and Push the Docker Image

Get the repository URI from CloudFormation Outputs, then:

```bash
# Set variables
ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
REGION=ap-south-1
REPO_URI="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com/shopflow-shopverse"

# Authenticate Docker to ECR (token valid 12 hours)
aws ecr get-login-password --region $REGION \
  | docker login --username AWS --password-stdin \
    "${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"

# Build the ShopVerse Docker image
# (from F:\Final\ShopVerse directory — where Dockerfile is)
docker build -t shopverse .

# Tag with ECR URI and version
docker tag shopverse:latest ${REPO_URI}:latest
docker tag shopverse:latest ${REPO_URI}:v1.0

# Push both tags
docker push ${REPO_URI}:latest
docker push ${REPO_URI}:v1.0
```

### Step 3: Verify in Console

1. **ECR → Repositories** → click `shopflow-shopverse`
2. You should see your image with tags `:latest` and `:v1.0`
3. Click the image → **Vulnerabilities** tab → scan results appear here

---

## Post-Deployment Checklist

- [ ] Stack `shopflow-ecr` shows `CREATE_COMPLETE`
- [ ] Docker image pushed — visible in ECR Console
- [ ] Image scan complete — vulnerability report visible
- [ ] Repository URI noted from CloudFormation Outputs

---

## Next Steps

- **Module 11 — ECS Fargate**: Use the ECR image to run ShopVerse as a containerised service. No EC2 to manage — AWS runs the containers for you.
