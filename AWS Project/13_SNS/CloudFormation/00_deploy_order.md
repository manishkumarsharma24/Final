# ShopFlow SNS — Module 13: Deployment Guide

## What Is SNS?

Amazon SNS (Simple Notification Service) broadcasts one message to many subscribers at once. Publish once, deliver to N destinations simultaneously. This is called "fan-out."

---

## Topics Created

| Topic | Subscribers | Used For |
|-------|------------|---------|
| `shopflow-ops-alerts-dev` | Your email | CloudWatch alarm notifications |
| `shopflow-order-fanout-dev` | Notification SQS + Order Events SQS + Inventory SQS | Order placed/updated events → 3 independent consumers |

---

## Deployment

### Step 1: Deploy

1. **CloudFormation → Create stack → With new resources**
2. Upload: `01_sns_topics.yaml`
3. Stack name: `shopflow-sns`
4. Parameters:
   - **ProjectName**: `shopflow`
   - **Environment**: `dev`
   - **OpsEmailAddress**: `manishkumarsharma24@gmail.com`
5. Submit → `CREATE_COMPLETE`

### Step 2: Confirm Email Subscription

**IMPORTANT:** After deployment, check your email for a message from `no-reply@sns.amazonaws.com` with subject "AWS Notification - Subscription Confirmation". Click the link. Without this, you will NOT receive any alerts.

### Step 3: Test the Alert Topic

```bash
# Publish a test message to the ops alert topic
aws sns publish \
  --topic-arn "arn:aws:sns:ap-south-1:<account>:shopflow-ops-alerts-dev" \
  --subject "TEST: ShopFlow Alert" \
  --message "This is a test alert from the ShopFlow SNS topic."

# You should receive this in your email within ~30 seconds
```

### Step 4: Test the Fan-out

```bash
# Publish an order event to the fan-out topic
aws sns publish \
  --topic-arn "arn:aws:sns:ap-south-1:<account>:shopflow-order-fanout-dev" \
  --message '{"orderId": "456", "status": "PLACED", "email": "customer@example.com"}'

# Check both SQS queues received it:
aws sqs get-queue-attributes \
  --queue-url "https://sqs.ap-south-1.amazonaws.com/<account>/shopflow-notifications-dev" \
  --attribute-names ApproximateNumberOfMessages
```

---

## Fan-out Pattern Explained

```
Spring Boot publishes ONE message:
  → SNS Topic: shopflow-order-fanout-dev
       │
       ├── SQS: shopflow-notifications-dev     → Lambda reads → sends confirmation email/SMS
       │
       ├── SQS: shopflow-order-events-dev      → Consumer reads → updates order status, triggers fulfillment
       │
       └── SQS: shopflow-inventory-update-dev  → Consumer reads → decrements stock for each item
```

One publish, three independent consumers. None of them know about the others.
If the email service is down, inventory still updates. If inventory is slow, emails still go out.

---

## Post-Deployment Checklist

- [ ] Stack `shopflow-sns` shows `CREATE_COMPLETE`
- [ ] Confirmation email received from `no-reply@sns.amazonaws.com` → clicked the link
- [ ] Test alert email received in inbox
- [ ] Fan-out test shows messages in all 3 SQS queues (notification, order-events, inventory-update)
- [ ] SNS Console → Topics → both topics visible with correct subscription counts

---

## Next Steps

**Module 14 — Lambda**: Create a Lambda function triggered by the `shopflow-notifications-dev` SQS queue to send order confirmation emails. Lambda + SQS is the AWS-native way to process queue messages without running a server 24/7.
