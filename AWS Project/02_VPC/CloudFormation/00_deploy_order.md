# ShopFlow VPC — CloudFormation Deployment Guide

## Deployment Order

Deploy stacks in this exact order. Each stack depends on the previous.

| Order | Stack Name | Template File | Depends On |
|-------|-----------|---------------|------------|
| 1 | shopflow-vpc | 01_vpc.yaml | Nothing |
| 2 | shopflow-security-groups | 02_security_groups.yaml | shopflow-vpc |

---

## Stack 1 — shopflow-vpc

### Console Steps
1. AWS Console → CloudFormation → Create stack → With new resources
2. Upload: `01_vpc.yaml`
3. Stack name: `shopflow-vpc`
4. Parameters:
   - ProjectName: `shopflow`
   - VpcCidr: `10.0.0.0/16`
   - PublicSubnet1Cidr: `10.0.1.0/24`
   - PublicSubnet2Cidr: `10.0.2.0/24`
   - PrivateSubnet1Cidr: `10.0.3.0/24`
   - PrivateSubnet2Cidr: `10.0.4.0/24`
5. Tags: Project = shopflow
6. No IAM checkbox needed (no IAM resources)
7. Submit → wait for CREATE_COMPLETE

### What Gets Created
- shopflow-vpc (10.0.0.0/16)
- shopflow-pub-1a (10.0.1.0/24) — ap-south-1a
- shopflow-pub-1b (10.0.2.0/24) — ap-south-1b
- shopflow-prv-1a (10.0.3.0/24) — ap-south-1a
- shopflow-prv-1b (10.0.4.0/24) — ap-south-1b
- shopflow-igw (Internet Gateway)
- shopflow-public-rt (Public Route Table)
- shopflow-private-rt (Private Route Table)

---

## Stack 2 — shopflow-security-groups

### Console Steps
1. AWS Console → CloudFormation → Create stack → With new resources
2. Upload: `02_security_groups.yaml`
3. Stack name: `shopflow-security-groups`
4. Parameters:
   - ProjectName: `shopflow`
5. Tags: Project = shopflow
6. No IAM checkbox needed
7. Submit → wait for CREATE_COMPLETE

### What Gets Created
- shopflow-alb-sg (ports 80, 443 from internet)
- shopflow-ec2-sg (port 8080 from ALB only)
- shopflow-rds-sg (port 3306 from EC2/Lambda only)
- shopflow-lambda-sg (outbound to RDS + internet)

---

## Verify After Deployment

### Verify VPC
- AWS Console → VPC → Your VPCs → find shopflow-vpc
- Check: 4 subnets, 1 IGW, 2 route tables

### Verify Security Groups
- AWS Console → EC2 → Security Groups
- Filter by VPC: shopflow-vpc
- Should see 4 groups with correct inbound/outbound rules

---

## Post-Deployment Checklist

- [ ] Stack 1 CREATE_COMPLETE
- [ ] Stack 2 CREATE_COMPLETE
- [ ] shopflow-vpc visible in VPC console
- [ ] 4 subnets visible (2 public, 2 private)
- [ ] Internet Gateway attached to VPC
- [ ] 4 security groups visible in EC2 console
- [ ] Public route table has 0.0.0.0/0 → IGW route
- [ ] Private route table has no internet route

---

## Next Steps After VPC

Once VPC is deployed, the next stacks will be:
- Stack 3: RDS Subnet Group + RDS Instance
- Stack 4: EC2 Launch Template + Auto Scaling Group
- Stack 5: Application Load Balancer + Target Group
