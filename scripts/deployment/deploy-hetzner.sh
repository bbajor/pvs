#!/bin/bash
# Hetzner Cloud Deployment Script (Podman-based)
# Usage: ./deploy-hetzner.sh [environment] [image-tag]

set -e

ENVIRONMENT=${1:-prod}
IMAGE_TAG=${2:-latest}
IMAGE_NAME="ghcr.io/bbajor/pvs:${ENVIRONMENT}-${IMAGE_TAG}"

echo "🚀 PVS Hetzner Cloud Deployment"
echo "================================"
echo "Environment: $ENVIRONMENT"
echo "Image: $IMAGE_NAME"
echo ""

# Check if podman is available
if ! command -v podman &> /dev/null; then
    echo "❌ Error: Podman is not installed"
    exit 1
fi

# Check if podman-compose is available
if command -v podman-compose &> /dev/null; then
    COMPOSE_CMD="podman-compose"
elif podman compose version &> /dev/null; then
    COMPOSE_CMD="podman compose"
else
    echo "❌ Error: Neither podman-compose nor podman compose available"
    exit 1
fi

# Login to GHCR if not already logged in
if ! podman image exists "$IMAGE_NAME" 2>/dev/null; then
    echo "🔐 Logging in to GitHub Container Registry..."
    echo "Please enter your GitHub Personal Access Token:"
    read -s GITHUB_TOKEN
    echo "$GITHUB_TOKEN" | podman login ghcr.io -u "$(git config user.name)" --password-stdin
fi

# Navigate to deployment directory
cd /opt/pvs || {
    echo "❌ Error: /opt/pvs directory not found"
    exit 1
}

# Backup current deployment
BACKUP_TAG="${ENVIRONMENT}-backup-$(date +%Y%m%d-%H%M%S)"
if podman image exists "ghcr.io/bbajor/pvs:${ENVIRONMENT}-latest" 2>/dev/null; then
    echo "💾 Creating backup: $BACKUP_TAG"
    podman tag "ghcr.io/bbajor/pvs:${ENVIRONMENT}-latest" \
               "ghcr.io/bbajor/pvs:$BACKUP_TAG" || true
fi

# Pull new image
echo "📥 Pulling new image..."
podman pull "$IMAGE_NAME"
podman tag "$IMAGE_NAME" "ghcr.io/bbajor/pvs:${ENVIRONMENT}-latest"

# Deploy
echo "🚀 Deploying application..."
$COMPOSE_CMD -f podman-compose.production.yml --profile "$ENVIRONMENT" pull
$COMPOSE_CMD -f podman-compose.production.yml --profile "$ENVIRONMENT" up -d

# Wait for health check
echo "⏳ Waiting for application to start..."
sleep 60

# Health check
MAX_RETRIES=10
RETRY_COUNT=0
while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if curl -f http://localhost:8080/actuator/health 2>/dev/null; then
        echo "✅ Health check passed!"
        break
    else
        RETRY_COUNT=$((RETRY_COUNT + 1))
        echo "⏳ Health check failed, retrying ($RETRY_COUNT/$MAX_RETRIES)..."
        sleep 10
    fi
done

if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
    echo "❌ Health check failed after $MAX_RETRIES retries - rolling back..."
    $COMPOSE_CMD -f podman-compose.production.yml --profile "$ENVIRONMENT" restart
    exit 1
fi

# Clean up old images
echo "🧹 Cleaning up old images..."
podman image prune -f --filter "until=168h" || true

echo ""
echo "✅ Deployment successful!"
echo "📊 Image: $IMAGE_NAME"
echo "🏷️  Tag: ${ENVIRONMENT}-latest"


