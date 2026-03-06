#!/bin/sh
# =============================================================================
# Uninstall Script - Remove development environment based on .env
# =============================================================================
#
# Usage: ./uninstall.sh
#
# Removes:
#   - Development container (PROJECT_NAME)
#   - Development image (PROJECT_NAME-dev:latest)
#   - PostgreSQL container (PROJECT_NAME-db) and volume
#   - Docker network (DEV_NETWORK)
#
# Preserves:
#   - Source code, .env, logs/, config/
#   - Standalone PostgreSQL and Mailpit containers
#
# =============================================================================
set -e

if [ ! -f .env ]; then
    echo "ERROR: .env file not found"
    exit 1
fi

echo "Loading configuration from .env..."
. ./.env

DEV_CONTAINER="$PROJECT_NAME"
DEV_IMAGE="${PROJECT_NAME}-dev:latest"
PG_CONTAINER="${PROJECT_NAME}-db"
PG_VOLUME="${PG_CONTAINER}-data"

echo ""
echo "Configuration:"
echo "  Dev container:  $DEV_CONTAINER"
echo "  Dev image:      $DEV_IMAGE"
echo "  PG container:   $PG_CONTAINER"
echo "  PG volume:      $PG_VOLUME"
echo "  Network:        $DEV_NETWORK"
echo ""

# Check what exists
CONTAINER_EXISTS=$(docker ps -a --format "{{.Names}}" | grep -q "^${DEV_CONTAINER}$" && echo "true" || echo "false")
IMAGE_EXISTS=$(docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "^${DEV_IMAGE}$" && echo "true" || echo "false")
PG_CONTAINER_EXISTS=$(docker ps -a --format "{{.Names}}" | grep -q "^${PG_CONTAINER}$" && echo "true" || echo "false")
PG_VOLUME_EXISTS=$(docker volume ls --format "{{.Name}}" | grep -q "^${PG_VOLUME}$" && echo "true" || echo "false")
NETWORK_EXISTS=$(docker network ls --format "{{.Name}}" | grep -q "^${DEV_NETWORK}$" && echo "true" || echo "false")

echo "Found:"
[ "$CONTAINER_EXISTS" = "true" ] && echo "  ✓ Dev container" || echo "  ✗ Dev container"
[ "$IMAGE_EXISTS" = "true" ] && echo "  ✓ Dev image" || echo "  ✗ Dev image"
[ "$PG_CONTAINER_EXISTS" = "true" ] && echo "  ✓ PostgreSQL container" || echo "  ✗ PostgreSQL container"
[ "$PG_VOLUME_EXISTS" = "true" ] && echo "  ✓ PostgreSQL volume" || echo "  ✗ PostgreSQL volume"
[ "$NETWORK_EXISTS" = "true" ] && echo "  ✓ Network" || echo "  ✗ Network"
echo ""

if [ "$CONTAINER_EXISTS" = "false" ] && [ "$IMAGE_EXISTS" = "false" ] && [ "$PG_CONTAINER_EXISTS" = "false" ] && [ "$PG_VOLUME_EXISTS" = "false" ] && [ "$NETWORK_EXISTS" = "false" ]; then
    echo "Nothing to remove"
    exit 0
fi

printf "Proceed? [y/N]: "
read -r response
case "$response" in
    [yY]) ;;
    *) echo "Cancelled"; exit 0 ;;
esac

# Remove container
if [ "$CONTAINER_EXISTS" = "true" ]; then
    if docker ps --format "{{.Names}}" | grep -q "^${DEV_CONTAINER}$"; then
        echo "Stopping container..."
        docker stop "$DEV_CONTAINER" >/dev/null
    fi
    echo "Removing container..."
    docker rm "$DEV_CONTAINER" >/dev/null
fi

# Remove image
if [ "$IMAGE_EXISTS" = "true" ]; then
    echo "Removing image..."
    docker rmi "$DEV_IMAGE" >/dev/null 2>&1 || echo "WARNING: Could not remove image"
fi

# Remove PostgreSQL container
if [ "$PG_CONTAINER_EXISTS" = "true" ]; then
    if docker ps --format "{{.Names}}" | grep -q "^${PG_CONTAINER}$"; then
        echo "Stopping PostgreSQL container..."
        docker stop "$PG_CONTAINER" >/dev/null
    fi
    echo "Removing PostgreSQL container..."
    docker rm "$PG_CONTAINER" >/dev/null
fi

# Remove PostgreSQL volume
if [ "$PG_VOLUME_EXISTS" = "true" ]; then
    echo "Removing PostgreSQL volume..."
    docker volume rm "$PG_VOLUME" >/dev/null
fi

# Remove network (only if empty)
if [ "$NETWORK_EXISTS" = "true" ]; then
    CONNECTED=$(docker network inspect "$DEV_NETWORK" --format '{{len .Containers}}' 2>/dev/null || echo "0")
    if [ "$CONNECTED" -eq 0 ]; then
        echo "Removing network..."
        docker network rm "$DEV_NETWORK" >/dev/null
    else
        echo "WARNING: Network has $CONNECTED connected container(s), skipping"
    fi
fi

echo ""
echo "Done"
