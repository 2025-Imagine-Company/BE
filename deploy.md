# AWS Deployment Guide for AudIon Application

## Prerequisites
- AWS CLI configured
- Docker installed
- AWS Account with appropriate permissions

## Option 1: AWS ECS with Fargate (Recommended)

### 1. Create ECR Repository
```bash
aws ecr create-repository --repository-name audion-app --region ap-northeast-2
```

### 2. Build and Push Docker Image
```bash
# Get login token
aws ecr get-login-password --region ap-northeast-2 | docker login --username AWS --password-stdin YOUR_ACCOUNT_ID.dkr.ecr.ap-northeast-2.amazonaws.com

# Build image
docker build -t audion-app .

# Tag image
docker tag audion-app:latest YOUR_ACCOUNT_ID.dkr.ecr.ap-northeast-2.amazonaws.com/audion-app:latest

# Push image
docker push YOUR_ACCOUNT_ID.dkr.ecr.ap-northeast-2.amazonaws.com/audion-app:latest
```

### 3. Set up RDS PostgreSQL Database
```bash
aws rds create-db-instance \
  --db-instance-identifier audion-db \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --master-username audion_user \
  --master-user-password YOUR_DB_PASSWORD \
  --allocated-storage 20 \
  --db-name audion_db \
  --vpc-security-group-ids sg-xxxxxxxxx \
  --publicly-accessible
```

### 4. Create AWS Secrets Manager Secrets
```bash
# Database credentials
aws secretsmanager create-secret --name "audion/database-password" --secret-string "YOUR_DB_PASSWORD"
aws secretsmanager create-secret --name "audion/jwt-secret" --secret-string "YOUR_JWT_SECRET"
aws secretsmanager create-secret --name "audion/aws-access-key" --secret-string "YOUR_AWS_ACCESS_KEY"
aws secretsmanager create-secret --name "audion/aws-secret-key" --secret-string "YOUR_AWS_SECRET_KEY"
aws secretsmanager create-secret --name "audion/s3-bucket" --secret-string "YOUR_S3_BUCKET"
```

### 5. Create ECS Cluster
```bash
aws ecs create-cluster --cluster-name audion-cluster --capacity-providers FARGATE
```

### 6. Register Task Definition
```bash
# Update aws-ecs-task-definition.json with your account ID
aws ecs register-task-definition --cli-input-json file://aws-ecs-task-definition.json
```

### 7. Create ECS Service
```bash
aws ecs create-service \
  --cluster audion-cluster \
  --service-name audion-service \
  --task-definition audion-app:1 \
  --desired-count 1 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxxxxxxxx],securityGroups=[sg-xxxxxxxxx],assignPublicIp=ENABLED}"
```

## Option 2: AWS EKS (Kubernetes)

### 1. Create EKS Cluster
```bash
eksctl create cluster --name audion-cluster --region ap-northeast-2 --nodes 2
```

### 2. Create Kubernetes Secrets
```bash
kubectl create secret generic audion-secrets \
  --from-literal=database-url="jdbc:postgresql://YOUR_RDS_ENDPOINT:5432/audion_db" \
  --from-literal=database-username="audion_user" \
  --from-literal=database-password="YOUR_DB_PASSWORD" \
  --from-literal=jwt-secret="YOUR_JWT_SECRET" \
  --from-literal=aws-access-key="YOUR_AWS_ACCESS_KEY" \
  --from-literal=aws-secret-key="YOUR_AWS_SECRET_KEY" \
  --from-literal=s3-bucket="YOUR_S3_BUCKET" \
  --from-literal=ai-server-url="http://ai-server:8081" \
  --from-literal=xauth-secret="YOUR_XAUTH_SECRET"
```

### 3. Deploy Application
```bash
# Update k8s-deployment.yaml with your ECR repository URL
kubectl apply -f k8s-deployment.yaml
```

## Option 3: Local Development with Docker Compose

### 1. Set up Environment Variables
```bash
cp .env.example .env
# Edit .env with your actual values
```

### 2. Run with Docker Compose
```bash
docker-compose -f docker-compose.aws.yml up -d
```

## Post-Deployment Steps

### 1. Database Migration
The application uses Flyway for database migrations. Migrations will run automatically on startup.

### 2. Health Check
```bash
curl http://YOUR_LOAD_BALANCER_URL/actuator/health
```

### 3. API Documentation
Access Swagger UI at: `http://YOUR_LOAD_BALANCER_URL/swagger-ui/index.html`

## Security Considerations

1. **Network Security**: Configure security groups to allow only necessary traffic
2. **Database Security**: Use RDS with encryption at rest and in transit
3. **Secrets Management**: Store sensitive data in AWS Secrets Manager
4. **IAM Roles**: Use least-privilege IAM roles for ECS tasks
5. **SSL/TLS**: Configure Application Load Balancer with SSL certificate

## Monitoring and Logging

1. **CloudWatch Logs**: Application logs are automatically sent to CloudWatch
2. **CloudWatch Metrics**: Set up custom metrics for application monitoring
3. **AWS X-Ray**: Enable for distributed tracing (optional)

## Cost Optimization

1. Use **t3.micro** or **t3.small** instances for development
2. Enable **Auto Scaling** for production workloads
3. Use **Spot Instances** for non-critical workloads
4. Set up **CloudWatch Alarms** for cost monitoring

## Troubleshooting

### Common Issues:
1. **502 Bad Gateway**: Check application logs and health check endpoint
2. **Database Connection**: Verify RDS security groups and connection strings
3. **ECR Access**: Ensure ECS execution role has ECR permissions
4. **Secrets Access**: Verify IAM roles have Secrets Manager permissions

Replace `YOUR_ACCOUNT_ID`, `YOUR_DB_PASSWORD`, etc., with your actual values before deployment.