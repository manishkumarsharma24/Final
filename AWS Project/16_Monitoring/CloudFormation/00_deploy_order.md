# ShopFlow CloudWatch — Module 16: Deployment Guide

## What Is CloudWatch?

AWS CloudWatch is the built-in monitoring service. It collects logs from every AWS service, lets you set alarms on metrics, and displays everything in dashboards.

---

## What Gets Created

| Resource | Purpose |
|----------|---------|
| 3 Log Groups | EC2 app, ECS containers, RDS slow queries |
| 5 Alarms | ALB 5xx errors, ALB latency, RDS CPU, RDS storage, SQS DLQ |
| 1 Dashboard | `shopflow-overview-dev` — charts for all key metrics |

---

## Deployment

### Prerequisites
- Modules 9 (ALB), 12 (SQS), and 13 (SNS) deployed

### Step 1: Deploy

1. **CloudFormation → Create stack → With new resources**
2. Upload: `01_cloudwatch.yaml`
3. Stack name: `shopflow-monitoring`
4. Parameters: ProjectName = `shopflow`, Environment = `dev`
5. Submit → `CREATE_COMPLETE`

### Step 2: View the Dashboard

1. **CloudWatch → Dashboards → shopflow-overview-dev**
2. You should see 4 graph widgets: ALB traffic, ALB latency, RDS metrics, SQS queue depths

### Step 3: View Alarms

1. **CloudWatch → Alarms → All alarms**
2. Filter for `shopflow-`
3. All alarms should be in **OK** state initially

---

## Alarm States

| State | Meaning |
|-------|---------|
| `OK` | Metric is below threshold — everything normal |
| `ALARM` | Metric crossed threshold — ops team notified |
| `INSUFFICIENT_DATA` | Not enough data yet to evaluate |

When an alarm transitions from OK → ALARM, SNS sends an email to manishkumarsharma24@gmail.com (from Module 13 subscription).

---

## Searching Logs with CloudWatch Logs Insights

```sql
-- Find all ERROR logs in the last hour
fields @timestamp, @message
| filter @message like /ERROR/
| sort @timestamp desc
| limit 50

-- Find slow requests (> 1 second)
fields @timestamp, @message
| filter @message like /execution time/
| sort @timestamp desc

-- Count requests by status code
fields @timestamp, @message
| filter @message like /HTTP/
| stats count() by statusCode
```

Run these in: **CloudWatch → Logs Insights → Select log group → Run query**

---

## Post-Deployment Checklist

- [ ] Stack `shopflow-monitoring` shows `CREATE_COMPLETE`
- [ ] Dashboard visible in CloudWatch
- [ ] 5 alarms visible, all in OK state
- [ ] Log groups created with 30-day retention

---

## Next Steps

- **Module 17 — CodeBuild**: Automate building, testing, and packaging ShopVerse.
- **Module 18 — CodePipeline**: Wire Source → Build → Deploy into a fully automated pipeline.
