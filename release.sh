#!/bin/bash

# =============================================================================
# Release Script - Production Image Export
# =============================================================================
#
# IMPORTANTE: eseguire dalla macchina HOST, NON dentro il container di sviluppo.
# I comandi Docker non sono disponibili all'interno del container.
#
# Usage: ./release.sh [-v|--vers <version>]
#
# Options:
#   -v, --vers <version>   Versione del release (default: ARTIFACT_VERSION da .env)
#
# Examples:
#   ./release.sh              # Usa ARTIFACT_VERSION da .env (es. 1.0.0)
#   ./release.sh -v 1.2.0     # Forza una versione specifica
#
# -----------------------------------------------------------------------------
# PROCEDURA DI RELEASE
# -----------------------------------------------------------------------------
#
# Questo script esegue automaticamente i seguenti passi:
#
#   1. Build Java (Maven)      — compila il backend dentro il container di sviluppo
#                                e produce target/service.jar (fat JAR via Shade plugin)
#   2. Build Svelte (Vite)     — compila il frontend e lo deposita in
#                                src/main/resources/static/ (bundled nel JAR al passo 1)
#   3. Dockerfile produzione   — genera un Dockerfile temporaneo basato su
#                                ubuntu:24.04 + openjdk-21-jre-headless
#   4. Docker image            — costruisce l'immagine di produzione con tag
#                                <project>-app:latest e <project>-app:<version>
#   5. Export tar              — esporta l'immagine in <project>-image.tar
#   6. install.sh              — genera lo script di installazione per il server
#                                di destinazione (vedi sezione INSTALLAZIONE)
#      Package finale          — impacchetta tar + install.sh in
#                                release/<project>-v<version>.tar.gz
#
# Output: release/<project>-v<version>.tar.gz contenente:
#   - <project>-image.tar   (immagine Docker completa, standalone)
#   - install.sh            (script di installazione per il server di destinazione)
#
# -----------------------------------------------------------------------------
# INSTALLAZIONE SUL SERVER DI DESTINAZIONE
# -----------------------------------------------------------------------------
#
# Trasferire il package e installare:
#
#   scp release/<project>-v<version>.tar.gz user@server:/tmp/
#   ssh user@server
#   cd /tmp && tar -xzf <project>-v<version>.tar.gz
#   ./install.sh
#
# install.sh esegue sul server:
#   1. docker load          — carica l'immagine dal tar nel Docker locale
#   2. docker network       — crea la rete <project>-net se non esiste
#   3. docker run           — avvia il container di produzione
#
# Requisiti sul server di destinazione:
#   - Docker installato e running
#   - Utente nel gruppo docker (non richiede sudo)
#   - Porta RELEASE_PORT (default 8080) disponibile
#   - Almeno 512 MB RAM disponibili
#
# -----------------------------------------------------------------------------
# POLICY LOG
# -----------------------------------------------------------------------------
#
# I log dell'applicazione sono scritti da logback in /app/logs/ dentro
# il container (file: service.log, rotazione giornaliera con compressione gzip).
#
# Sviluppo:
#   - /app/logs/ è montato via bind mount su ./logs/ nella cartella progetto
#   - I log sono direttamente accessibili sul filesystem dell'host
#   - La cartella ./logs/ è esclusa da git (.gitignore)
#
# Produzione:
#   - /app/logs/ è montato su un Docker named volume: <project>-logs
#   - Il volume è gestito da Docker, non richiede directory di sistema
#   - Sopravvive ai restart e agli aggiornamenti del container
#   - Accesso ai log: docker logs <project>-production
#                     oppure: docker exec <project>-production ls /app/logs/
#
# -----------------------------------------------------------------------------
# POLICY NETWORK
# -----------------------------------------------------------------------------
#
# Il container di produzione utilizza la stessa rete Docker del container
# di sviluppo (<project>-net), in modo da poter raggiungere gli altri
# servizi sulla rete (es. PostgreSQL) tramite hostname del container.
#
# La rete viene creata automaticamente da install.sh se non esiste.
#
# -----------------------------------------------------------------------------
# REQUISITI MACCHINA DI SVILUPPO (per eseguire questo script)
# -----------------------------------------------------------------------------
#
#   - Docker installato e running
#   - Container di sviluppo attivo (./install.sh deve essere stato eseguito)
#   - File .env presente nella root del progetto
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
            echo "Se -v non specificata, usa ARTIFACT_VERSION da .env"
            exit 1
            ;;
    esac
done

# =============================================================================
# Configuration
# =============================================================================

WORKSPACE="$(cd "$(dirname "$0")" && pwd)"
RELEASE_DIR="$WORKSPACE/release"
IMAGE_TAG="latest"
JAR_FILE="$WORKSPACE/target/service.jar"

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
    PROJECT_NAME="app"
fi

# Use VERSION from argument or default
if [ -z "$VERSION" ]; then
    VERSION="${ARTIFACT_VERSION:-1.0.0}"
    info "Using version from .env: $VERSION"
else
    info "Using version from argument: $VERSION"
fi

# Set release configuration with defaults if not in .env
: ${RELEASE_IMAGE:=ubuntu:24.04}
: ${RELEASE_MEMORY_LIMIT:=512m}
: ${RELEASE_MEMORY_RESERVATION:=256m}
: ${RELEASE_CPU_LIMIT:=1.0}
: ${RELEASE_CPU_RESERVATION:=0.5}
: ${RELEASE_PORT:=8080}
: ${RELEASE_APP_USER:=appuser}
: ${RELEASE_APP_USER_UID:=1001}
: ${RELEASE_APP_USER_GID:=1001}

# Derived variables
IMAGE_NAME="${PROJECT_NAME}-app"
TAR_NAME="${PROJECT_NAME}-image.tar"
RELEASE_PACKAGE="${PROJECT_NAME}-v${VERSION}.tar.gz"
DEV_NETWORK="${PROJECT_NAME}${DEV_NETWORK_SUFFIX:--net}"

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
# Step 1: Build Java Application
# =============================================================================

info "Step 1/6: Building Java application (Undertow/Maven)..."

# DEBUG: If this fails, check:
# - Maven is installed: mvn --version
# - pom.xml is valid: mvn validate
# - All dependencies are resolvable: mvn dependency:resolve
# - Java version matches pom.xml requirements

cd "$WORKSPACE"
docker exec "$PROJECT_NAME" bash -c "cd /workspace && bin/cmd app build" || {
    error "Failed to build Java application. Check Maven logs above."
}

if [ ! -f "$JAR_FILE" ]; then
    error "JAR file not found: $JAR_FILE. Build may have failed silently."
fi

success "Java application built: $JAR_FILE"

# =============================================================================
# Step 2: Build Svelte GUI
# =============================================================================

info "Step 2/6: Building Vite frontend..."

# DEBUG: If this fails, check:
# - Node.js and npm are installed in container
# - vite/package.json exists and is valid
# - All npm dependencies are installed: cd vite && npm install
# - Vite config is correct: vite/vite.config.js

docker exec "$PROJECT_NAME" bash -c "cd /workspace && bin/cmd vite build" || {
    error "Failed to build Vite frontend. Check npm/vite logs above."
}

STATIC_DIR="$WORKSPACE/src/main/resources/static"

if [ ! -d "$STATIC_DIR" ] || [ -z "$(ls -A $STATIC_DIR)" ]; then
    error "Static files not found in $STATIC_DIR. Frontend build may have failed."
fi

success "Vite frontend built: $STATIC_DIR"

# =============================================================================
# Step 3: Create Production Dockerfile
# =============================================================================

info "Step 3/6: Creating production Dockerfile..."

# DEBUG: This Dockerfile uses:
# - ubuntu:24.04 with openjdk-21-jre-headless for consistency with dev environment
# - Non-root user for security
# - Health check endpoint
# If image build fails:
# - Check if base image is available: docker pull ubuntu:24.04
# - Verify JAR file path is correct
# - Check EXPOSE port matches application.properties

cat > "$RELEASE_DIR/Dockerfile" << DOCKERFILE_EOF
# Production Dockerfile
# Uses Ubuntu 24.04 with OpenJDK 21 JRE

FROM ${RELEASE_IMAGE}

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y \
    openjdk-21-jre-headless \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Set working directory
WORKDIR /app

# Create non-root user for security
RUN groupadd -g ${RELEASE_APP_USER_GID} ${RELEASE_APP_USER} && \\
    useradd -u ${RELEASE_APP_USER_UID} -g ${RELEASE_APP_USER} -s /bin/sh -M ${RELEASE_APP_USER}

# Copy application JAR
COPY app.jar /app/app.jar

# Create log and config directories
RUN mkdir -p /app/logs /app/config && \\
    chown -R ${RELEASE_APP_USER}:${RELEASE_APP_USER} /app

# Switch to non-root user
USER ${RELEASE_APP_USER}

# Expose application port
EXPOSE ${RELEASE_PORT}

# Health check
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \\
    CMD curl -f http://localhost:${RELEASE_PORT}/api/status/health || exit 1

# Run application
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
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

info "Step 6/6: Generating installation script and configuration file..."

# Copy application.properties into release directory
PROPS_SRC="$WORKSPACE/src/main/resources/application.properties"
if [ ! -f "$PROPS_SRC" ]; then
    error "application.properties not found: $PROPS_SRC"
fi
cp "$PROPS_SRC" "$RELEASE_DIR/application.properties"
success "Configuration file copied: application.properties"



# DEBUG: This script will be run on the production server
# If installation fails on production:
# - Check Docker is installed: docker --version
# - Check Docker service is running: systemctl status docker
# - Verify port 8080 is available: netstat -tuln | grep 8080
# - Check disk space on production: df -h
# - Verify tar file was transferred correctly: md5sum ${TAR_NAME}

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
# Usage: ./install.sh
#
# =============================================================================

set -e

# Color output
error() { printf '\\033[0;31mERROR: %s\\033[0m\\n' "\$1" >&2; exit 1; }
info() { printf '\\033[0;34mINFO: %s\\033[0m\\n' "\$1"; }
success() { printf '\\033[0;32m✓ SUCCESS: %s\\033[0m\\n' "\$1"; }

# Configuration
CONTAINER_NAME="${PROJECT_NAME}-production"
IMAGE_TAR="${TAR_NAME}"
LOG_VOLUME="${PROJECT_NAME}-logs"
NETWORK="${DEV_NETWORK}"
APP_PORT="${RELEASE_PORT}"
APP_USER_UID="${RELEASE_APP_USER_UID}"
APP_USER_GID="${RELEASE_APP_USER_GID}"

# Resource limits (staging/beta mode - works on underprovisioned servers)
MEMORY_LIMIT="${RELEASE_MEMORY_LIMIT}"
MEMORY_RESERVATION="${RELEASE_MEMORY_RESERVATION}"
CPU_LIMIT="${RELEASE_CPU_LIMIT}"
CPU_RESERVATION="${RELEASE_CPU_RESERVATION}"

info "${PROJECT_NAME} Application - Production Installation"
info "=========================================="

# Check Docker
if ! command -v docker >/dev/null 2>&1; then
    error "Docker is not installed. Please install Docker first."
fi

if ! docker info >/dev/null 2>&1; then
    error "Docker daemon is not running. Please start Docker service."
fi

# Check if image tar exists
if [ ! -f "\$IMAGE_TAR" ]; then
    error "Image file not found: \$IMAGE_TAR"
fi

info "Loading Docker image..."
docker load -i "\$IMAGE_TAR" || error "Failed to load Docker image"
success "Docker image loaded"

# Create network if not exists
if ! docker network ls --format '{{.Name}}' | grep -q "^\${NETWORK}\$"; then
    info "Creating Docker network \${NETWORK}..."
    docker network create "\$NETWORK" >/dev/null
    success "Network created: \$NETWORK"
fi

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

if [ ! -f "application.properties" ]; then
    error "application.properties non trovato. Il file deve essere nella stessa cartella di install.sh."
fi

info "  Config: application.properties"

docker run -d \\
    --name "\$CONTAINER_NAME" \\
    --restart unless-stopped \\
    --network="\$NETWORK" \\
    --memory="\$MEMORY_LIMIT" \\
    --memory-reservation="\$MEMORY_RESERVATION" \\
    --cpus="\$CPU_LIMIT" \\
    --cpu-shares=512 \\
    -p "\${APP_PORT}:${RELEASE_PORT}" \\
    -v "\${LOG_VOLUME}:/app/logs" \\
    --mount type=bind,source=\$(pwd)/application.properties,target=/app/config/application.properties,readonly \\
    ${IMAGE_NAME}:latest || error "Failed to start container"

success "Container started: \$CONTAINER_NAME"

# Wait for application to be ready
info "Waiting for application to start (max 60 seconds)..."
COUNTER=0
while [ \$COUNTER -lt 30 ]; do
    STATUS=\$(docker inspect -f '{{.State.Status}}' "\$CONTAINER_NAME" 2>/dev/null)
    if [ "\$STATUS" = "running" ]; then
        success "Application is ready!"
        break
    fi
    sleep 2
    COUNTER=\$((COUNTER + 2))
done

if [ "\$STATUS" != "running" ]; then
    error "Application failed to start. Check logs: docker logs \$CONTAINER_NAME"
fi

# Display status
echo ""
info "Installation completed successfully!"
echo ""
info "Application Details:"
info "  Container: \$CONTAINER_NAME"
info "  Status: \$(docker inspect -f '{{.State.Status}}' \$CONTAINER_NAME)"
info "  URL: http://localhost:\${APP_PORT}"
info "  Log volume: \$LOG_VOLUME"
echo ""
info "Useful commands:"
info "  View logs:    docker logs -f \$CONTAINER_NAME"
info "  Stop:         docker stop \$CONTAINER_NAME"
info "  Start:        docker start \$CONTAINER_NAME"
info "  Restart:      docker restart \$CONTAINER_NAME"
info "  Shell access: docker exec -it \$CONTAINER_NAME bash"
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
tar -czf "$RELEASE_PACKAGE" "$TAR_NAME" install.sh application.properties || {
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
rm -f "$RELEASE_DIR/application.properties"

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
info "   ./install.sh"
echo ""
info "Production container configuration:"
info "  Container name: ${PROJECT_NAME}-production"
info "  Memory: ${RELEASE_MEMORY_LIMIT} (reserved: ${RELEASE_MEMORY_RESERVATION})"
info "  CPU: ${RELEASE_CPU_LIMIT} cores (reserved: ${RELEASE_CPU_RESERVATION})"
info "  Port: ${RELEASE_PORT}"
info "  Network: ${DEV_NETWORK}"
info "  User: ${RELEASE_APP_USER} (UID: ${RELEASE_APP_USER_UID}, GID: ${RELEASE_APP_USER_GID})"
info "  Mode: Staging/Beta-test (optimized for limited resources)"
echo ""
info "Docker images created:"
info "  - ${IMAGE_NAME}:${VERSION}"
info "  - ${IMAGE_NAME}:${IMAGE_TAG}"
echo ""
