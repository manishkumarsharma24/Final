# ShopFlow Lambda — Module 14: Deployment Guide

## What Is Lambda?

AWS Lambda runs a function in response to an event — no server to manage. The function wakes up, processes one event, and sleeps. You pay only for execution time (per millisecond). Zero cost when not running.

---

## Functions Created

| Function | Trigger | What It Does |
|----------|---------|-------------|
| `shopflow-email-notifier-dev` | SQS: `shopflow-notifications-dev` | Reads order event → sends email via SendGrid |
| `shopflow-order-processor-dev` | SQS: `shopflow-order-events-dev` | Reads order event → updates inventory |

---

## Prerequisites

1. Modules 1–13 deployed
2. Lambda JAR files uploaded to S3 (see below)

---

## Deployment Steps

### Step 1: Build and Upload Lambda JARs

Lambda function code is separate from this CloudFormation template. You need to build and upload the JARs before the functions can run.

```bash
# Build the Lambda modules (if they exist in ShopVerse)
# These would be separate Maven modules: shopverse-lambda-notification, shopverse-lambda-order

# Upload email notifier JAR
aws s3 cp target/email-notifier.jar \
  s3://shopflow-artifacts-dev/lambda/email-notifier.jar

# Upload order processor JAR
aws s3 cp target/order-processor.jar \
  s3://shopflow-artifacts-dev/lambda/order-processor.jar
```

> **Note:** If the Lambda Java code doesn't exist yet, deploy the CloudFormation stack anyway (it will succeed). Update the function code later with `aws lambda update-function-code`.

### Step 2: Deploy the Stack

1. **CloudFormation → Create stack → With new resources**
2. Upload: `01_lambda.yaml`
3. Stack name: `shopflow-lambda`
4. Parameters: ProjectName = `shopflow`, Environment = `dev`
5. Submit → `CREATE_COMPLETE` (~30 seconds)

### Step 3: Test the Email Lambda

```bash
# Send a test message to the notification SQS queue
aws sqs send-message \
  --queue-url "https://sqs.ap-south-1.amazonaws.com/<account>/shopflow-notifications-dev" \
  --message-body '{
    "type": "ORDER_CONFIRMED",
    "orderId": "ORD-001",
    "customerEmail": "test@example.com",
    "customerName": "Manish Kumar",
    "totalAmount": "999.99"
  }'

# Lambda triggers within seconds. Check the logs:
aws logs tail /aws/lambda/shopflow-email-notifier-dev --follow
```

---

## How SQS Triggers Lambda

```
1. Lambda continuously polls the SQS queue (no cost for polling)
2. Messages arrive in shopflow-notifications-dev
3. Lambda batches up to 10 messages (BatchSize: 10)
4. Lambda invokes shopflow-email-notifier with the batch
5. Function processes each message, calls SendGrid API
6. Function returns success → Lambda deletes messages from queue
7. If function throws exception → messages become visible again → retry
8. After 3 retries → messages move to DLQ
```

## Cold Start Explained

First invocation of a Java Lambda is slow (~2-3 seconds) because:
- AWS provisions a container
- JVM initializes
- Spring context loads (if using Spring Cloud Function)

Subsequent invocations reuse the same container (fast, ~50ms).

For async email sending, a 2-3 second cold start is acceptable. The user doesn't wait — the notification is processed in the background.

---

## Updating Function Code

After updating the Java code:

```bash
# Rebuild and upload
aws s3 cp target/email-notifier.jar s3://shopflow-artifacts-dev/lambda/email-notifier.jar

# Update the Lambda function
aws lambda update-function-code \
  --function-name shopflow-email-notifier-dev \
  --s3-bucket shopflow-artifacts-dev \
  --s3-key lambda/email-notifier.jar
```

---

## Post-Deployment Checklist

- [ ] Stack `shopflow-lambda` shows `CREATE_COMPLETE`
- [ ] Both functions visible in Lambda Console
- [ ] SQS trigger configured on each function (visible in Lambda → Configuration → Triggers)
- [ ] Test message sent and visible in CloudWatch Logs

---

## Next Steps

- **Module 15 — DynamoDB**: Create tables for JWT session store and idempotency keys.
