# ShopFlow — Session Context

> Last updated: 2026-06-21

## Project Summary

- **App:** ShopVerse (Spring Boot 3.2.5 / Java 21)
- **Region:** ap-south-1 (Mumbai)
- **Stack prefix:** `shopflow-`
- **Goal:** Learn AWS by deploying ShopVerse infrastructure module by module using CloudFormation

## Free-Tier Learning Sequence

Only covering modules that are free (always free or 12-month free tier).

| # | Module | Folder | Status | Notes |
|---|--------|--------|--------|-------|
| 1 | IAM | `01_IAM` | ✅ Done | |
| 2 | VPC | `02_VPC` | ✅ Done | No NAT Gateway (costs $32/mo) |
| 3 | SSM Parameter Store | `05_SSM` | ✅ Done | Free (Standard params) |
| 4 | S3 | `03_S3` | ✅ Done | 5GB free (12 months) |
| 5 | RDS MySQL | `06_RDS` | ✅ Done | db.t3.micro free (12 months) |
| 6 | EC2 | `08_EC2` | ✅ Done | t3.micro (free tier); public subnet; SSM Session Manager working |
| 7 | ALB | `09_ALB` | ✅ Done | 750 hrs free (12 months); stack shopflow-alb; EC2 registered as target |
| 8 | ECR | `10_ECR` | ✅ Done | 500MB free (12 months) |
| 9 | ECS Fargate | `11_ECS` | ✅ Done | |
| 10 | SQS | `12_SQS` | ✅ Done | 8 queues: notifications, payments (FIFO), order-events, inventory + DLQs |
| 11 | SNS | `13_SNS` | ✅ Done | 2 topics: ops-alerts (email) + order-fanout (→ 3 SQS queues) |
| 12 | Lambda | `14_Lambda` | ✅ Done | 2 functions: email-notifier + order-processor (Python inline, SQS triggers) |
| 13 | DynamoDB | `15_DynamoDB` | ✅ Done | 2 tables: sessions (JWT store + GSI) + idempotency (duplicate prevention) |
| 14 | CloudWatch | `16_Monitoring` | ✅ Done | 9 alarms + 6-chart dashboard covering SQS, Lambda, DynamoDB, SNS |
| 15 | CodeBuild + CodePipeline | `17_CICD` | ⬜ Pending | |

### Skipped (not free)
- Secrets Manager — $0.40/secret/month → use SSM Parameter Store instead
- ElastiCache — no free tier
- ECS Fargate compute — pay per vCPU (noted but may revisit)
- NAT Gateway — ~$32/month

## Current Module: EC2 (Module 6) — In Progress

**Stack name:** `shopflow-ec2-instance`
**Template:** `08_EC2/CloudFormation/01_ec2_instance.yaml`

**What it creates:**
- Launch Template: `shopflow-shopverse-lt-dev`
- EC2 Instance: t3.micro, Amazon Linux 2023, private subnet
- User Data: installs Java 21 + CloudWatch Agent, creates placeholder `shopverse` systemd service
- Access: SSM Session Manager only (no SSH, no port 22)

**Parameters needed at deploy time:**
```bash
# EC2 Instance Profile ARN
aws iam get-instance-profile --instance-profile-name shopflow-ec2-role \
  --query "InstanceProfile.Arn" --output text

# Security Group ID
aws ec2 describe-security-groups --filters "Name=group-name,Values=*ec2*shopflow*" \
  --query "SecurityGroups[0].GroupId" --output text --region ap-south-1

# Private Subnet ID
aws ec2 describe-subnets --filters "Name=tag:Name,Values=*shopflow*private*1a*" \
  --query "Subnets[0].SubnetId" --output text --region ap-south-1
```

**Issues fixed:**
- ✅ Replaced all `!ImportValue` with Parameters
- ✅ Fixed AMI parameter (moved from Resources to Parameters section)
- ✅ Replaced NetworkInterfaces block with SecurityGroupIds directly (subnet conflict fix)
- ✅ Changed InstanceType default: t2.micro → t3.micro (t3.micro is free tier eligible in ap-south-1)

**Deploy checklist:**
- [ ] Delete failed `shopflow-ec2-instance` stack (if exists)
- [ ] CloudFormation → Create stack → Upload `01_ec2_instance.yaml`
- [ ] Stack name: `shopflow-ec2-instance`
- [ ] Fill in 3 parameters: EC2InstanceProfileArn, EC2SecurityGroupId, PrivateSubnetId
- [ ] Select InstanceType: `t3.micro`
- [ ] Verify CREATE_COMPLETE (~2 min)
- [ ] EC2 Console → confirm instance State: Running
- [ ] Connect via SSM Session Manager → `systemctl status shopverse`

## Completed: CloudWatch (Module 14)

**Stack name:** `shopflow-monitoring` — ✅ CREATE_COMPLETE
- 9 Alarms: 4 SQS DLQ, 3 Lambda (2 errors + 1 duration), 2 DynamoDB throttle, 1 SNS failed delivery
- Dashboard: `shopflow-overview-dev` with 6 charts (SQS, Lambda, DynamoDB, SNS)

## Next Module: CI/CD — CodeBuild + CodePipeline (Module 15)

**Template:** `17_CICD/CloudFormation/`
**Status:** ⬜ Skipped for now — requires ECR + ECS to be deployed first.
Last module in the free-tier sequence. Return to after EC2, ALB, RDS, ECR, ECS are done.
