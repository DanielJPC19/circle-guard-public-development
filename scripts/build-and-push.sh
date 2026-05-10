#!/bin/bash
# Builds and pushes 6 CircleGuard Docker images to Docker Hub.
# Must be run from the repo ROOT (where gradlew lives).
#
# Usage:  ./scripts/build-and-push.sh <DOCKERHUB_USER> <IMAGE_TAG>
# Example: ./scripts/build-and-push.sh jcmunoz dev

set -euo pipefail

DOCKERHUB_USER=${1:-""}
IMAGE_TAG=${2:-""}

if [ -z "$DOCKERHUB_USER" ] || [ -z "$IMAGE_TAG" ]; then
  echo "Usage: $0 <DOCKERHUB_USER> <IMAGE_TAG>"
  echo "Example: $0 jcmunoz dev"
  exit 1
fi

SERVICES=(
  auth-service
  identity-service
  promotion-service
  gateway-service
  notification-service
  form-service
)

echo "==> Building and pushing 6 CircleGuard images"
echo "    Docker Hub user : $DOCKERHUB_USER"
echo "    Image tag       : $IMAGE_TAG"
echo ""

for svc in "${SERVICES[@]}"; do
  FULL_TAG="${DOCKERHUB_USER}/circleguard-${svc}:${IMAGE_TAG}"
  echo "──────────────────────────────────────────────"
  echo "==> Building  ${FULL_TAG}"
  docker build \
    -t "${FULL_TAG}" \
    -f "services/circleguard-${svc}/Dockerfile" \
    .
  echo "==> Pushing   ${FULL_TAG}"
  docker push "${FULL_TAG}"
done

echo ""
echo "==> Done. All 6 images pushed to Docker Hub."
echo "    View at: https://hub.docker.com/u/${DOCKERHUB_USER}"
