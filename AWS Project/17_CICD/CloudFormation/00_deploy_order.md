# ShopFlow CI/CD — Modules 17 & 18: Deployment Guide

## What Is CodeBuild + CodePipeline?

**CodeBuild** is a managed build server. It compiles your code, runs tests, builds a Docker image, and pushes it to ECR. No Jenkins to maintain.

**CodePipeline** orchestrates the full flow: every git push to the main branch automatically triggers Source → Build → Deploy.

```
git push main
     │
     ↓ (CodePipeline detects change)
 ┌────────────────────────────────────────────────────────────┐
 │  Stage 1: SOURCE                                           │
 │  Download source code from GitHub → S3 artifact store     │
 └────────────────────────────────────────────────────────────┘
     │
     ↓
 ┌────────────────────────────────────────────────────────────┐
 │  Stage 2: BUILD (CodeBuild)                                │
 │  mvn clean package → docker build → docker push to ECR    │
 │  Output: imagedefinitions.json                             │
 └────────────────────────────────────────────────────────────┘
     │
     ↓
 ┌────────────────────────────────────────────────────────────┐
 │  Stage 3: DEPLOY (ECS)                                     │
 │  Rolling update: new task with new image → old task stops  │
 └────────────────────────────────────────────────────────────┘
```

---

## Files in This Module

| File | What It Creates |
|------|-----------------|
| `01_codebuild.yaml` | CodeBuild project (Maven + Docker build logic) |
| `02_codepipeline.yaml` | CodePipeline (Source → Build → Deploy) |

---

## Prerequisites

All Modules 1–16, plus:
- ShopVerse code in a GitHub repository
- A GitHub account

---

## Deployment Steps

### Step 1: Create the GitHub → AWS Connection (One-Time, Manual)

CodePipeline connects to GitHub via a "CodeStar Connection" (OAuth). You must create this manually first:

1. **AWS Console → CodePipeline → Settings → Connections**
2. Click **Create connection**
3. Select **GitHub** → name it `shopflow-github`
4. Click **Connect to GitHub** → authorize the AWS app in GitHub
5. Click **Connect** → status changes to **Available**
6. Copy the Connection ARN (you'll need it below)

### Step 2: Deploy CodeBuild

1. **CloudFormation → Create stack → With new resources**
2. Upload: `01_codebuild.yaml`
3. Stack name: `shopflow-codebuild`
4. Parameters:
   - **GitHubOwner**: your GitHub username
   - **GitHubRepo**: `ShopVerse`
5. Submit → `CREATE_COMPLETE`

### Step 3: Deploy CodePipeline

1. **CloudFormation → Create stack → With new resources**
2. Upload: `02_codepipeline.yaml`
3. Stack name: `shopflow-codepipeline`
4. Parameters:
   - **GitHubOwner**: your GitHub username
   - **GitHubRepo**: `ShopVerse`
   - **GitHubBranch**: `main`
   - **GitHubConnectionArn**: the ARN from Step 1
5. Submit → `CREATE_COMPLETE`

### Step 4: Watch the First Pipeline Run

1. **CodePipeline → Pipelines → shopflow-pipeline-dev**
2. The pipeline starts automatically on creation
3. Watch each stage turn green (or red if something fails)
4. Click **CodeBuild** in the Build stage to see build logs in real time

---

## First Run: What to Expect

| Stage | Duration | What Happens |
|-------|----------|--------------|
| Source | ~10 seconds | GitHub zip downloaded to S3 |
| Build | ~5–8 minutes | Maven compiles, Docker builds, ECR push |
| Deploy | ~3–5 minutes | ECS rolling update |

Total: **~10–15 minutes** from `git push` to deployed.

---

## Triggering a Deployment

After setup, every `git push main` triggers the full pipeline. You can also start it manually:

```bash
aws codepipeline start-pipeline-execution \
  --name shopflow-pipeline-dev
```

Or in the console: **CodePipeline → shopflow-pipeline-dev → Release change**

---

## Checking Build Logs

```bash
# Tail CodeBuild logs (requires CloudWatch Logs access)
aws logs tail /aws/codebuild/shopflow-build-dev --follow
```

Or: **CodePipeline → Build stage → Details → View logs in CodeBuild**

---

## Rolling Back a Bad Deploy

```bash
# Find the last successful image tag from ECR
aws ecr describe-images \
  --repository-name shopflow-shopverse \
  --query 'sort_by(imageDetails,&imagePushedAt)[-5:].imageTags'

# Force ECS to redeploy with a previous task definition revision
# (easier: update the pipeline to push the old image tag and re-run)
```

---

## Post-Deployment Checklist

- [ ] GitHub → AWS CodeStar Connection status: **Available**
- [ ] Stack `shopflow-codebuild` shows `CREATE_COMPLETE`
- [ ] Stack `shopflow-codepipeline` shows `CREATE_COMPLETE`
- [ ] Pipeline first run completes all 3 stages green
- [ ] New ECS task running with latest image
- [ ] `curl http://<ALB-DNS>/actuator/health` → `{"status":"UP"}`

---

## Congratulations — You Have a Complete AWS Stack!

You have now built and deployed every major AWS service for ShopVerse:

| Module | Service | Status |
|--------|---------|--------|
| 1 | IAM | ✅ Done |
| 2 | VPC | ✅ Done |
| 3 | S3 | ✅ Done |
| 4 | Secrets Manager | ✅ Done |
| 5 | SSM Parameter Store | ✅ Done |
| 6 | RDS MySQL | ✅ Done |
| 7 | ElastiCache Redis | ✅ Done |
| 8 | EC2 | ✅ Done |
| 9 | ALB | ✅ Done |
| 10 | ECR | ✅ Done |
| 11 | ECS Fargate | ✅ Done |
| 12 | SQS | ✅ Done |
| 13 | SNS | ✅ Done |
| 14 | Lambda | ✅ Done |
| 15 | DynamoDB | ✅ Done |
| 16 | CloudWatch | ✅ Done |
| 17 | CodeBuild | ✅ Done |
| 18 | CodePipeline | ✅ Done |
