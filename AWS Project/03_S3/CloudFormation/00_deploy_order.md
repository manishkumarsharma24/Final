# ShopFlow S3 — Module 3: Deployment Guide

## What Is S3?

Amazon S3 (Simple Storage Service) is object storage — you store any file (called an **object**) inside a container (called a **bucket**). Think of it as an unlimited, always-on hard drive in the cloud.

**Key things to know before you deploy:**

1. **Bucket names are globally unique.** No two buckets anywhere in AWS can have the same name. This template uses `${ProjectName}-${Purpose}-${Environment}` (e.g., `shopflow-artifacts-dev`) to reduce collision risk.
2. **S3 has no VPC dependency.** Unlike RDS or EC2, S3 is a global service — you don't put it inside a VPC. IAM policies and bucket policies control who can access it.
3. **All buckets in this template are private.** Nothing is publicly accessible by default. Product images are served through CloudFront (set up later).
4. **Encryption is always on.** AES-256 server-side encryption is free and has zero performance impact. There's no reason not to enable it.

---

## Buckets Created

| Bucket | Name Pattern | Purpose |
|--------|-------------|---------|
| Artifacts | `shopflow-artifacts-dev` | Spring Boot JARs, build outputs |
| CI/CD | `shopflow-cicd-artifacts-dev` | CodePipeline stage artifacts |
| RDS Exports | `shopflow-rds-exports-dev` | Aurora snapshot exports, SQL dumps |
| Product Images | `shopflow-product-images-dev` | Product photos (served via CloudFront) |
| React SPA | `shopflow-spa-dev` | Compiled React frontend (`dist/`) |

---

## Deployment Steps

### Prerequisites
- Stack `shopflow-iam-policies` must be deployed (EC2/ECS roles need S3 access)
- That's it — S3 has no VPC or network dependency

### Step 1: Deploy the Stack

1. Open **AWS Console → CloudFormation → Create stack → With new resources**
2. Upload: `01_s3_buckets.yaml`
3. Stack name: `shopflow-s3-buckets`
4. Parameters:
   - **ProjectName**: `shopflow`
   - **Environment**: `dev`
5. Tags: `Project = shopflow`
6. No IAM capability checkbox needed (S3 buckets don't create IAM resources)
7. Click **Submit** → wait for `CREATE_COMPLETE`

---

## Verify After Deployment

1. Go to **AWS Console → S3**
2. You should see 5 new buckets:
   - `shopflow-artifacts-dev`
   - `shopflow-cicd-artifacts-dev`
   - `shopflow-rds-exports-dev`
   - `shopflow-product-images-dev`
   - `shopflow-spa-dev`
3. Click any bucket → **Properties** → confirm:
   - **Versioning**: Enabled (for artifacts and CI/CD buckets)
   - **Default encryption**: Server-side encryption with Amazon S3 managed keys (SSE-S3)
4. Click **Permissions** → confirm:
   - **Block all public access**: ON

---

## How to Upload and Download (Manual Test)

After deployment, test that the bucket works:

```bash
# Upload a file (AWS CLI)
aws s3 cp myfile.txt s3://shopflow-artifacts-dev/

# List bucket contents
aws s3 ls s3://shopflow-artifacts-dev/

# Download a file
aws s3 cp s3://shopflow-artifacts-dev/myfile.txt ./myfile-downloaded.txt

# Delete a file
aws s3 rm s3://shopflow-artifacts-dev/myfile.txt
```

---

## How S3 Storage Classes Work (Lifecycle Rules)

The `rds-exports` bucket uses lifecycle transitions to save money:

```
Day 0   → Object uploaded → stored in S3 Standard
           Cost: $0.023/GB/month

Day 30  → Automatically moved to S3 Standard-IA (Infrequent Access)
           Cost: $0.0125/GB/month (46% cheaper)
           Retrieval: same speed, but $0.01/GB retrieval fee

Day 90  → Automatically moved to S3 Glacier Instant Retrieval
           Cost: $0.004/GB/month (83% cheaper than Standard)
           Retrieval: milliseconds (same as Standard, $0.03/GB fee)

Day 365 → Automatically deleted
```

This is "set and forget" cost optimization — no manual intervention needed.

---

## S3 Versioning Explained

When versioning is enabled:
- Every PUT creates a new version; the old version is preserved
- DELETEs add a "delete marker" — the object isn't gone, just hidden
- You can restore any previous version instantly
- Lifecycle rules can delete old non-current versions after N days

**Example with the artifacts bucket:**
```
Upload shopverse-web-1.0.jar  → version ID: "abc123"
Upload shopverse-web-1.1.jar  → version ID: "def456"  (current)
Upload shopverse-web-1.2.jar  → version ID: "ghi789"  (current)

→ Bad deploy with 1.2. Roll back:
aws s3api get-object --bucket shopflow-artifacts-dev \
  --key shopverse-web.jar --version-id def456 shopverse-web.jar
→ EC2 restarts with 1.1. Problem solved.
```

---

## Post-Deployment Checklist

- [ ] Stack `shopflow-s3-buckets` shows `CREATE_COMPLETE`
- [ ] 5 buckets visible in S3 console
- [ ] All buckets show "Block all public access: On"
- [ ] Artifacts and CI/CD buckets show versioning: Enabled
- [ ] Manual upload test successful (`aws s3 cp`)
- [ ] Stack outputs visible in CloudFormation → Outputs tab

---

## Next Steps

Once S3 is deployed, continue to:
- **Module 05 — SSM Parameter Store**: Store app config (DB URL, JWT secret, Redis host) as free SSM parameters — no paid Secrets Manager needed

> **Skipping:** Modules 04 (Secrets Manager), 06 (RDS), 07 (ElastiCache), 08 (EC2 t3.small), 09 (ALB), 11 (ECS Fargate) are paid services. We deploy only the 12 always-free modules.
