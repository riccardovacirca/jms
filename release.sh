#!/bin/bash

# =============================================================================
# Release Script - Production Image Export
# =============================================================================
#
# IMPORTANT: This script must be run from the HOST machine, NOT inside the container
# Reason: Docker commands are not available inside the development container
#
# Usage: ./bin/release.sh [-v|--vers <version>]
#
# Options:
#   -v, --vers <version>   Specify release version (default: ARTIFACT_VERSION from .env)
#
# Examples:
#   ./bin/release.sh              # Uses version from .env (e.g., 0.0.1-SNAPSHOT)
#   ./bin/release.sh -v 1.0.0     # Creates crm-v1.0.0.tar.gz
#
# What this script does:
# 1. Build Spring Boot application (Maven)
# 2. Build Svelte GUI (Vite)
# 3. Create optimized Docker image for production
# 4. Export image to tar file
# 5. Generate install.sh script for target server
# 6. Package everything into release.tar.gz
#
# Output: release.tar.gz containing:
#   - crm-image.tar (Docker image)
#   - install.sh (installation script for production server)
#
# Production Server Requirements:
# - Docker installed
# - Minimal resources: 512MB RAM, 1 CPU core (staging/beta-test mode)
# - Port 8080 available
#
# =============================================================================

set -e

# Color output functions
error() { printf '\033[0;31mERROR: %s\033[0m\n' "$1" >&2; exit 1; }
info() { printf '\033[0;34mINFO: %s\033[0m\n' "$1"; }
warn() { printf '\033[0;33mWARNING: %s\033[0m\n' "$1" >&2; }
success() { printf '\033[0;32m✓ SUCCESS: %s\033[0m\n' "$1"; }

# =============================================================================
# Parse Arguments
# =============================================================================

VERSION=""

while [[ $# -gt 0 ]]; do
    case $1 in
        -v|--vers)
            VERSION="$2"
            shift 2
            ;;
        *)
            echo "Opzione sconosciuta: $1"
            echo "Uso: $0 [-v|--vers <versione>]"
            echo ""
            echo "Se -v non specificata, usa ARTIFACT_VERSION da .env (default: 1.0.0)"
            exit 1
            ;;
    esac
done

# =============================================================================
# Configuration
# =============================================================================

WORKSPACE="$(cd "$(dirname "$0")/.." && pwd)"
RELEASE_DIR="$WORKSPACE/release"
IMAGE_TAG="latest"
TAR_NAME="crm-image.tar"

# =============================================================================
# Pre-flight checks
# =============================================================================

info "Starting release build process..."

# Check if we're inside the container (we should NOT be)
if [ -f "/.dockerenv" ]; then
    error "This script must be run from the HOST machine, not inside the container. Exit the container first."
fi

# Check if docker is available
if ! command -v docker >/dev/null 2>&1; then
    error "Docker is not installed or not in PATH. Please install Docker first."
fi

# Check if we're in the project root
if [ ! -f "$WORKSPACE/pom.xml" ]; then
    error "pom.xml not found. Please run this script from the project root."
fi

# Load environment variables
if [ -f "$WORKSPACE/.env" ]; then
    info "Loading environment variables from .env..."
    set -a
    . "$WORKSPACE/.env"
    set +a
else
    warn ".env file not found. Using default values."
    PROJECT_NAME="crm"
    ARTIFACT_VERSION="1.0-SNAPSHOT"
fi

# Use VERSION from argument or ARTIFACT_VERSION from .env
if [ -z "$VERSION" ]; then
    VERSION="${ARTIFACT_VERSION:-1.0.0}"
    info "Using version from .env: $VERSION"
else
    info "Using version from argument: $VERSION"
fi

# Set release configuration with defaults if not in .env
: ${RELEASE_JRE_IMAGE:=eclipse-temurin:21-jre-alpine}
: ${RELEASE_MEMORY_LIMIT:=512m}
: ${RELEASE_MEMORY_RESERVATION:=256m}
: ${RELEASE_CPU_LIMIT:=1.0}
: ${RELEASE_CPU_RESERVATION:=0.5}
: ${RELEASE_PORT:=8080}
: ${RELEASE_JVM_XMS:=256m}
: ${RELEASE_JVM_XMX:=512m}
: ${RELEASE_JVM_METASPACE:=128m}
: ${RELEASE_APP_USER:=appuser}
: ${RELEASE_APP_USER_UID:=1001}
: ${RELEASE_APP_USER_GID:=1001}

# Derived variables
IMAGE_NAME="${PROJECT_NAME}-app"
RELEASE_PACKAGE="${PROJECT_NAME}-v${VERSION}.tar.gz"

info "Project: $PROJECT_NAME"
info "Version: $VERSION"
info "Release package: $RELEASE_PACKAGE"

# =============================================================================
# Clean and prepare release directory
# =============================================================================

info "Preparing release directory..."

if [ -d "$RELEASE_DIR" ]; then
    warn "Release directory exists. Cleaning..."
    rm -rf "$RELEASE_DIR"
fi

mkdir -p "$RELEASE_DIR"
success "Release directory prepared: $RELEASE_DIR"

# =============================================================================
# Step 1: Build Spring Boot Application
# =============================================================================

info "Step 1/6: Building Spring Boot application..."

# DEBUG: If this fails, check:
# - Maven is installed: mvn --version
# - pom.xml is valid: mvn validate
# - All dependencies are resolvable: mvn dependency:resolve
# - Java version matches pom.xml requirements

cd "$WORKSPACE"
docker exec crm bash -c "cd /usr/src/app && bin/cmd app build" || {
    error "Failed to build Spring Boot application. Check Maven logs above."
}

JAR_FILE="$WORKSPACE/target/${PROJECT_NAME}-${ARTIFACT_VERSION}.jar"

if [ ! -f "$JAR_FILE" ]; then
    error "JAR file not found: $JAR_FILE. Build may have failed silently."
fi

success "Spring Boot application built: $JAR_FILE"

# =============================================================================
# Step 2: Build Svelte GUI
# =============================================================================

info "Step 2/6: Building Svelte GUI..."

# DEBUG: If this fails, check:
# - Node.js and npm are installed in container
# - gui/package.json exists and is valid
# - All npm dependencies are installed: cd gui && npm install
# - Vite config is correct: gui/vite.config.js

docker exec crm bash -c "cd /usr/src/app && bin/cmd gui build" || {
    error "Failed to build Svelte GUI. Check npm/vite logs above."
}

STATIC_DIR="$WORKSPACE/src/main/resources/static"

if [ ! -d "$STATIC_DIR" ] || [ -z "$(ls -A $STATIC_DIR)" ]; then
    error "Static files not found in $STATIC_DIR. GUI build may have failed."
fi

success "Svelte GUI built: $STATIC_DIR"

# =============================================================================
# Step 3: Create Production Dockerfile
# =============================================================================

info "Step 3/6: Creating production Dockerfile..."

# DEBUG: This Dockerfile uses:
# - eclipse-temurin:21-jre-alpine for minimal image size
# - Non-root user for security
# - Health check endpoint
# If image build fails:
# - Check if base image is available: docker pull eclipse-temurin:21-jre-alpine
# - Verify JAR file path is correct
# - Check EXPOSE port matches application.properties

cat > "$RELEASE_DIR/Dockerfile" << DOCKERFILE_EOF
# Production Dockerfile for CRM Application
# Uses minimal JRE image for smaller size and better security

FROM ${RELEASE_JRE_IMAGE}

# Set working directory
WORKDIR /app

# Create non-root user for security
RUN addgroup -g ${RELEASE_APP_USER_GID} -S ${RELEASE_APP_USER} && \\
    adduser -u ${RELEASE_APP_USER_UID} -S ${RELEASE_APP_USER} -G ${RELEASE_APP_USER}

# Copy application JAR
COPY app.jar /app/app.jar

# Create directories for data and logs
RUN mkdir -p /app/data /app/logs && \\
    chown -R ${RELEASE_APP_USER}:${RELEASE_APP_USER} /app

# Switch to non-root user
USER ${RELEASE_APP_USER}

# Expose application port
EXPOSE ${RELEASE_PORT}

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \\
    CMD wget --no-verbose --tries=1 --spider http://localhost:${RELEASE_PORT}/api/status/health || exit 1

# Set JVM options for limited resources (staging/beta environment)
# -Xms${RELEASE_JVM_XMS}: Initial heap size
# -Xmx${RELEASE_JVM_XMX}: Maximum heap size (matches container memory limit)
# -XX:MaxMetaspaceSize=${RELEASE_JVM_METASPACE}: Metaspace limit
# -XX:+UseSerialGC: Serial GC for low memory footprint
# These settings prioritize stability over performance for underprovisioned servers
ENV JAVA_OPTS="-Xms${RELEASE_JVM_XMS} -Xmx${RELEASE_JVM_XMX} -XX:MaxMetaspaceSize=${RELEASE_JVM_METASPACE} -XX:+UseSerialGC -Djava.security.egd=file:/dev/./urandom"

# Run application
ENTRYPOINT ["sh", "-c", "java \$JAVA_OPTS -jar /app/app.jar"]
DOCKERFILE_EOF

success "Production Dockerfile created"

# =============================================================================
# Step 4: Build Docker Image
# =============================================================================

info "Step 4/6: Building Docker image..."

# Copy JAR to release directory with standard name
cp "$JAR_FILE" "$RELEASE_DIR/app.jar"

# DEBUG: If docker build fails:
# - Check Dockerfile syntax: docker build --check $RELEASE_DIR
# - Verify base image is accessible
# - Check disk space: df -h
# - Try building with --no-cache flag
# - Check Docker daemon is running: docker info

cd "$RELEASE_DIR"
docker build -t "${IMAGE_NAME}:${IMAGE_TAG}" -t "${IMAGE_NAME}:${VERSION}" . || {
    error "Failed to build Docker image. Check Dockerfile and build logs above."
}

# Verify image was created
if ! docker images "${IMAGE_NAME}:${IMAGE_TAG}" | grep -q "${IMAGE_NAME}"; then
    error "Docker image not found after build. Build may have failed silently."
fi

success "Docker image built: ${IMAGE_NAME}:${IMAGE_TAG}, ${IMAGE_NAME}:${VERSION}"

# =============================================================================
# Step 5: Export Docker Image to TAR
# =============================================================================

info "Step 5/6: Exporting Docker image to tar..."

# DEBUG: If export fails:
# - Check disk space: df -h
# - Verify image exists: docker images
# - Check write permissions in release directory

docker save "${IMAGE_NAME}:${IMAGE_TAG}" -o "$RELEASE_DIR/$TAR_NAME" || {
    error "Failed to export Docker image. Check disk space and permissions."
}

if [ ! -f "$RELEASE_DIR/$TAR_NAME" ]; then
    error "TAR file not created: $RELEASE_DIR/$TAR_NAME"
fi

TAR_SIZE=$(du -h "$RELEASE_DIR/$TAR_NAME" | cut -f1)
success "Docker image exported: $TAR_NAME (Size: $TAR_SIZE)"

# =============================================================================
# Step 6: Generate Installation Script
# =============================================================================

info "Step 6/6: Generating installation script..."

# DEBUG: This script will be run on the production server
# If installation fails on production:
# - Check Docker is installed: docker --version
# - Check Docker service is running: systemctl status docker
# - Verify port 8080 is available: netstat -tuln | grep 8080
# - Check disk space on production: df -h
# - Verify tar file was transferred correctly: md5sum crm-image.tar

cat > "$RELEASE_DIR/install.sh" << INSTALL_EOF
#!/bin/bash

# =============================================================================
# ${PROJECT_NAME} Application - Production Installation Script
# =============================================================================
#
# This script installs the ${PROJECT_NAME} application on the production server
#
# Requirements:
# - Docker installed and running
# - Port ${RELEASE_PORT} available
# - At least 1GB disk space
# - At least ${RELEASE_MEMORY_LIMIT} RAM available
#
# Usage: sudo ./install.sh (or ./install.sh if running as root)
#
# =============================================================================

set -e

# Color output
error() { printf '\\033[0;31mERROR: %s\\033[0m\\n' "\$1" >&2; exit 1; }
info() { printf '\\033[0;34mINFO: %s\\033[0m\\n' "\$1"; }
success() { printf '\\033[0;32m✓ SUCCESS: %s\\033[0m\\n' "\$1"; }

# Configuration
CONTAINER_NAME="${PROJECT_NAME}-production"
IMAGE_TAR="crm-image.tar"
DATA_DIR="/var/lib/${PROJECT_NAME}/data"
LOGS_DIR="/var/lib/${PROJECT_NAME}/logs"
APP_PORT="${RELEASE_PORT}"
APP_USER_UID="${RELEASE_APP_USER_UID}"
APP_USER_GID="${RELEASE_APP_USER_GID}"

# Resource limits (staging/beta mode - works on underprovisioned servers)
MEMORY_LIMIT="${RELEASE_MEMORY_LIMIT}"
MEMORY_RESERVATION="${RELEASE_MEMORY_RESERVATION}"
CPU_LIMIT="${RELEASE_CPU_LIMIT}"
CPU_RESERVATION="${RELEASE_CPU_RESERVATION}"

info "CRM Application - Production Installation"
info "=========================================="

# Check if running as root
if [ "$EUID" -ne 0 ]; then
    error "Please run as root (use sudo)"
fi

# Check Docker
if ! command -v docker >/dev/null 2>&1; then
    error "Docker is not installed. Please install Docker first."
fi

if ! docker info >/dev/null 2>&1; then
    error "Docker daemon is not running. Please start Docker service."
fi

# Check if image tar exists
if [ ! -f "$IMAGE_TAR" ]; then
    error "Image file not found: $IMAGE_TAR"
fi

info "Loading Docker image..."
docker load -i "$IMAGE_TAR" || error "Failed to load Docker image"
success "Docker image loaded"

# Create data directories
info "Creating data directories..."
mkdir -p "\$DATA_DIR" "\$LOGS_DIR"
chown -R "\$APP_USER_UID:\$APP_USER_GID" "\$DATA_DIR" "\$LOGS_DIR"
chmod 755 "\$DATA_DIR" "\$LOGS_DIR"
success "Data directories created"

# Stop and remove existing container if exists
if docker ps -a --format '{{.Names}}' | grep -q "^\${CONTAINER_NAME}\$"; then
    info "Stopping existing container..."
    docker stop "\$CONTAINER_NAME" 2>/dev/null || true
    docker rm "\$CONTAINER_NAME" 2>/dev/null || true
    success "Existing container removed"
fi

# Start new container with resource limits
info "Starting ${PROJECT_NAME} application container..."
info "  Memory limit: \$MEMORY_LIMIT"
info "  CPU limit: \$CPU_LIMIT"
info "  Port: \$APP_PORT"

docker run -d \\
    --name "\$CONTAINER_NAME" \\
    --restart unless-stopped \\
    --memory="\$MEMORY_LIMIT" \\
    --memory-reservation="\$MEMORY_RESERVATION" \\
    --cpus="\$CPU_LIMIT" \\
    --cpu-shares=512 \\
    -p "\${APP_PORT}:${RELEASE_PORT}" \\
    -v "\${DATA_DIR}:/app/data" \\
    -v "\${LOGS_DIR}:/app/logs" \\
    -e SPRING_PROFILES_ACTIVE=production \\
    ${IMAGE_NAME}:latest || error "Failed to start container"

success "Container started: \$CONTAINER_NAME"

# Wait for application to be ready
info "Waiting for application to start (max 60 seconds)..."
COUNTER=0
while [ \$COUNTER -lt 60 ]; do
    if docker exec "\$CONTAINER_NAME" wget --no-verbose --tries=1 --spider http://localhost:${RELEASE_PORT}/api/status/health 2>/dev/null; then
        success "Application is ready!"
        break
    fi
    sleep 2
    COUNTER=\$((COUNTER + 2))
done

if [ \$COUNTER -ge 60 ]; then
    error "Application failed to start within 60 seconds. Check logs: docker logs \$CONTAINER_NAME"
fi

# Display status
echo ""
info "Installation completed successfully!"
echo ""
info "Application Details:"
info "  Container: \$CONTAINER_NAME"
info "  Status: \$(docker inspect -f '{{.State.Status}}' \$CONTAINER_NAME)"
info "  URL: http://localhost:\${APP_PORT}"
info "  Data: \$DATA_DIR"
info "  Logs: \$LOGS_DIR"
echo ""
info "Useful commands:"
info "  View logs:    docker logs -f \$CONTAINER_NAME"
info "  Stop:         docker stop \$CONTAINER_NAME"
info "  Start:        docker start \$CONTAINER_NAME"
info "  Restart:      docker restart \$CONTAINER_NAME"
info "  Shell access: docker exec -it \$CONTAINER_NAME sh"
info "  Remove:       docker stop \$CONTAINER_NAME && docker rm \$CONTAINER_NAME"
echo ""
INSTALL_EOF

chmod +x "$RELEASE_DIR/install.sh"
success "Installation script generated: install.sh"

# =============================================================================
# Package Release
# =============================================================================

info "Packaging release..."

cd "$RELEASE_DIR"
tar -czf "$RELEASE_PACKAGE" "$TAR_NAME" install.sh || {
    error "Failed to create release package"
}

if [ ! -f "$RELEASE_DIR/$RELEASE_PACKAGE" ]; then
    error "Release package not created"
fi

PACKAGE_SIZE=$(du -h "$RELEASE_DIR/$RELEASE_PACKAGE" | cut -f1)
success "Release package created: $RELEASE_PACKAGE (Size: $PACKAGE_SIZE)"

# =============================================================================
# Cleanup temporary files
# =============================================================================

info "Cleaning up temporary files..."
rm -f "$RELEASE_DIR/app.jar"
rm -f "$RELEASE_DIR/Dockerfile"
rm -f "$RELEASE_DIR/$TAR_NAME"
rm -f "$RELEASE_DIR/install.sh"

# =============================================================================
# Summary
# =============================================================================

echo ""
success "=========================================="
success "Release build completed successfully!"
success "=========================================="
echo ""
info "Release package: $RELEASE_DIR/$RELEASE_PACKAGE"
info "Package size: $PACKAGE_SIZE"
echo ""
info "Next steps:"
info "1. Transfer release package to production server:"
info "   scp $RELEASE_DIR/$RELEASE_PACKAGE root@ip_server:/tmp/"
echo ""
info "2. On production server, extract and install:"
info "   cd /tmp"
info "   tar -xzf $RELEASE_PACKAGE"
info "   ./install.sh  # (no sudo needed if running as root)"
echo ""
info "Production container configuration:"
info "  Container name: ${PROJECT_NAME}-production"
info "  Memory: ${RELEASE_MEMORY_LIMIT} (reserved: ${RELEASE_MEMORY_RESERVATION})"
info "  CPU: ${RELEASE_CPU_LIMIT} cores (reserved: ${RELEASE_CPU_RESERVATION})"
info "  Port: ${RELEASE_PORT}"
info "  User: ${RELEASE_APP_USER} (UID: ${RELEASE_APP_USER_UID}, GID: ${RELEASE_APP_USER_GID})"
info "  JVM: -Xms${RELEASE_JVM_XMS} -Xmx${RELEASE_JVM_XMX}"
info "  Mode: Staging/Beta-test (optimized for limited resources)"
echo ""
info "Docker images created:"
info "  - ${IMAGE_NAME}:${VERSION}"
info "  - ${IMAGE_NAME}:${IMAGE_TAG}"
echo ""
