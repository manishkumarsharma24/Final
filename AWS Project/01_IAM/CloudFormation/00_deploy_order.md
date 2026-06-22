# ShopFlow IAM — CloudFormation Deployment Guide

## Files in This Folder

| File | Stack Name | What It Creates | Deploy Order |
|------|-----------|-----------------|--------------|
| 05_iam_password_policy.yaml | shopflow-iam-password | Account password policy | 1st |
| 01_iam_policies.yaml | shopflow-iam-policies | 5 custom IAM policies | 2nd |
| 02_iam_groups.yaml | shopflow-iam-groups | 4 IAM groups + policy attachments | 3rd |
| 03_iam_roles.yaml | shopflow-iam-roles | 5 service roles (EC2, Lambda, ECS, RDS, CI/CD) | 4th |
| 04_iam_users.yaml | shopflow-iam-users | 10 IAM users + group assignments | 5th |

---

## How to Deploy — AWS Console

### Step 1 — Open CloudFormation
```
AWS Console → Search "CloudFormation" → Click it
→ Click "Create stack"
→ Select "With new resources (standard)"
```

### Step 2 — Upload Template
```
→ Select "Upload a template file"
→ Click "Choose file"
→ Select the YAML file
→ Click Next
```

### Step 3 — Configure Stack
```
Stack name: shopflow-iam-password   (for file 05)
Stack name: shopflow-iam-policies   (for file 01)
Stack name: shopflow-iam-groups     (for file 02)
Stack name: shopflow-iam-roles      (for file 03)
Stack name: shopflow-iam-users      (for file 04)

Parameters:
  ProjectName: shopflow        (leave as default)
  PrimaryRegion: ap-south-1   (Mumbai — change if different)
  DefaultPassword: (for users stack only — set your temp password)

→ Click Next → Next → Create stack
```

### Step 4 — Wait for Completion
```
Status: CREATE_IN_PROGRESS → wait 1-2 minutes
Status: CREATE_COMPLETE ✅ → move to next file
```

---

## How to Deploy — AWS CLI (Faster)

```bash
# Set your AWS account region
export AWS_DEFAULT_REGION=ap-south-1

# 1. Password Policy
aws cloudformation deploy \
  --template-file 05_iam_password_policy.yaml \
  --stack-name shopflow-iam-password \
  --capabilities CAPABILITY_NAMED_IAM

# 2. Custom Policies
aws cloudformation deploy \
  --template-file 01_iam_policies.yaml \
  --stack-name shopflow-iam-policies \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides ProjectName=shopflow PrimaryRegion=ap-south-1

# 3. Groups
aws cloudformation deploy \
  --template-file 02_iam_groups.yaml \
  --stack-name shopflow-iam-groups \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides ProjectName=shopflow

# 4. Roles
aws cloudformation deploy \
  --template-file 03_iam_roles.yaml \
  --stack-name shopflow-iam-roles \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides ProjectName=shopflow PrimaryRegion=ap-south-1

# 5. Users
aws cloudformation deploy \
  --template-file 04_iam_users.yaml \
  --stack-name shopflow-iam-users \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameter-overrides ProjectName=shopflow DefaultPassword=YourTempPassword@123
```

---

## After Deployment — Checklist

- [ ] Share AWS sign-in URL with each user:
      https://YOUR_ACCOUNT_ID.signin.aws.amazon.com/console
- [ ] Share usernames and temporary passwords securely
- [ ] Each user must set up MFA on first login
- [ ] Each user must change password on first login
- [ ] Verify groups have correct users:
      IAM → Groups → Check member count
- [ ] Test one user from each group to verify permissions work

---

## To Delete All Stacks (Clean Up)

Delete in REVERSE order:
```bash
aws cloudformation delete-stack --stack-name shopflow-iam-users
aws cloudformation delete-stack --stack-name shopflow-iam-roles
aws cloudformation delete-stack --stack-name shopflow-iam-groups
aws cloudformation delete-stack --stack-name shopflow-iam-policies
aws cloudformation delete-stack --stack-name shopflow-iam-password
```
