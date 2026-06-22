# ShopFlow SQS — Module 12: Deployment Guide

## What Is SQS?

Amazon SQS is a managed message queue. A producer puts a message in the queue; a consumer reads and processes it. If processing fails, the message is retried. Failed messages eventually move to a Dead-Letter Queue (DLQ) where they wait for a human to investigate.

SQS replaces RabbitMQ for AWS-hosted ShopVerse deployments.

---

## Queues Created

| Queue | Type | Purpose | Retry → DLQ after |
|-------|------|---------|-------------------|
| `shopflow-notifications-dev` | Standard | Email/SMS order notifications | 3 attempts |
| `shopflow-notification-dlq-dev` | Standard (DLQ) | Failed notification messages | — |
| `shopflow-payments-dev.fifo` | FIFO | Payment callbacks (exactly-once) | 3 attempts |
| `shopflow-payment-dlq-dev.fifo` | FIFO (DLQ) | Failed payment messages | — |
| `shopflow-order-events-dev` | Standard | Order placed/shipped/cancelled events | 5 attempts |
| `shopflow-order-events-dlq-dev` | Standard (DLQ) | Failed order events | — |
| `shopflow-inventory-update-dev` | Standard | Stock decrement on order placement | 5 attempts |
| `shopflow-inventory-update-dlq-dev` | Standard (DLQ) | Failed inventory updates | — |

---

## Deployment

### Step 1: Deploy

1. **CloudFormation → Create stack → With new resources**
2. Upload: `01_sqs_queues.yaml`
3. Stack name: `shopflow-sqs`
4. Parameters: ProjectName = `shopflow`, Environment = `dev`
5. Submit → `CREATE_COMPLETE` in ~30 seconds

### Step 2: Verify

1. **SQS Console** — you should see 8 queues
2. Click `shopflow-order-events-dev` → **Dead-letter queue** tab → shows linked DLQ

### Step 3: Test Manually (CLI)

```bash
# Send a test notification message
aws sqs send-message \
  --queue-url "https://sqs.ap-south-1.amazonaws.com/<account>/shopflow-notifications-dev" \
  --message-body '{"type": "ORDER_CONFIRMED", "orderId": "123", "email": "test@example.com"}'

# Receive the message (simulates a consumer)
aws sqs receive-message \
  --queue-url "https://sqs.ap-south-1.amazonaws.com/<account>/shopflow-notifications-dev" \
  --max-number-of-messages 1

# Delete the message after processing (required — prevents redelivery)
aws sqs delete-message \
  --queue-url "..." \
  --receipt-handle "<handle-from-receive-response>"
```

---

## Standard vs FIFO Queue

| | Standard | FIFO |
|--|--|--|
| **Ordering** | Best-effort | Strict (first-in, first-out) |
| **Delivery** | At-least-once | Exactly-once |
| **Throughput** | Unlimited | 3,000 msg/sec |
| **Use case** | Notifications, events | Payments, financial |
| **Queue name** | Any name | Must end in `.fifo` |

Payment queues are FIFO because you must not process the same payment twice (double charge) or process refund before charge.

---

## Dead-Letter Queue Explained

```
Message → Main Queue → Consumer tries to process
                             │
                       SUCCESS → Delete message → Done ✓
                             │
                       FAILURE (3x) → Message moves to DLQ

DLQ = messages that failed 3 times
     → CloudWatch alarm fires (Module 16)
     → Dev team investigates
```

Watch the DLQ depth metric in CloudWatch. A non-zero DLQ depth means processing failures need investigation.

---

## Post-Deployment Checklist

- [ ] Stack `shopflow-sqs` shows `CREATE_COMPLETE`
- [ ] 8 queues visible in SQS Console (4 main + 4 DLQ)
- [ ] Manual send + receive test successful
- [ ] DLQ linked to each main queue (verified in SQS Console → Dead-letter queue tab)
- [ ] Queue policies visible (Permissions tab → shows SNS allowed to send)

---

## Next Steps

**Module 13 — SNS**: Create topics that fan out to these SQS queues. When an order is placed, SNS broadcasts one message and both the notification queue and inventory queue receive it simultaneously.
