#!/bin/bash
set -e

echo "Waiting for LocalStack to be ready..."
sleep 3

BUCKET_NAME=${LOCALSTACK_S3_BUCKET_NAME:-microservices}

echo "Creating S3 bucket: $BUCKET_NAME"
if awslocal s3 mb s3://$BUCKET_NAME 2>&1; then
    echo "✓ Bucket '$BUCKET_NAME' created successfully!"
else
    echo "⚠ Bucket creation attempt completed"
fi

echo "Available buckets:"
awslocal s3 ls