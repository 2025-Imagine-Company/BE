#!/bin/bash

# Deployment script for Audion app on EC2

echo "🚀 Starting Audion deployment..."

# Update system
sudo apt update
sudo apt upgrade -y

# Install Docker
if ! command -v docker &> /dev/null; then
    echo "📦 Installing Docker..."
    sudo apt install -y docker.io
    sudo systemctl start docker
    sudo systemctl enable docker
    sudo usermod -aG docker $USER
fi

# Install Docker Compose
if ! command -v docker-compose &> /dev/null; then
    echo "📦 Installing Docker Compose..."
    sudo curl -L "https://github.com/docker/compose/releases/download/v2.20.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
fi

# Create app directory
mkdir -p /home/ubuntu/audion
cd /home/ubuntu/audion

echo "✅ Environment setup complete!"
echo ""
echo "Next steps:"
echo "1. Upload your project files to /home/ubuntu/audion/"
echo "2. Update .env.production with your actual values"
echo "3. Run: docker-compose -f docker-compose.prod.yml up -d"
echo ""
echo "🌐 Your app will be available at: http://13.124.255.83:8080"