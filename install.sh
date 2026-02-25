#!/bin/sh
# =============================================================================
# Install Script - Development Environment Setup
# =============================================================================
#
# Usage: ./install.sh [OPTIONS]
#
# Options:
#   (nessuna)               Crea o riavvia l'ambiente di sviluppo completo
#   --groupid <id>          Maven GroupId (default: com.example)
#   --postgres              Installa il container PostgreSQL
#   -n, --name <name>       Nome del container PostgreSQL (default: postgres)
#   -p, --port <port>       Porta host per PostgreSQL (default: 5432)
#   --network <network>     Network Docker per PostgreSQL (default: bridge)
#   --help, -h              Mostra questo messaggio
#
# Examples:
#   ./install.sh                                # Primo install o riavvio
#   ./install.sh --groupid io.mycompany         # GroupId personalizzato
#   ./install.sh --postgres                     # Installa PostgreSQL
#   ./install.sh --postgres -n mydb             # Installa PostgreSQL con nome custom
#   ./install.sh --postgres -p 5433             # Installa PostgreSQL su porta custom
#   ./install.sh --postgres --network hola-net  # Installa su network custom
#   ./install.sh --postgres -n mydb -p 5433 --network hola-net  # Tutti i parametri custom
#   ./install.sh --mailpit                      # Installa Mailpit (SMTP fake + web UI)
#   ./install.sh --mailpit -n mymail            # Installa Mailpit con nome custom
#   ./install.sh --mailpit -p 2025              # Installa Mailpit con porta SMTP custom
#   ./install.sh --mailpit --ui-port 9025       # Installa Mailpit con porta web UI custom
#   ./install.sh --mailpit --network hola-net   # Installa su network custom
#
# -----------------------------------------------------------------------------
# PROCEDURA DI INSTALL (primo avvio)
# -----------------------------------------------------------------------------
#
# Al primo avvio (nessun container esistente) lo script esegue:
#
#   1. .env                 — genera il file di configurazione nella root
#                             del progetto se non esiste già
#   2. Docker network       — crea la rete <project>-net per la comunicazione
#                             tra i container (dev, postgres, produzione)
#   3. Dockerfile.dev       — genera un Dockerfile temporaneo basato su
#                             ubuntu:24.04 con: git, curl, nano, openjdk-21-jdk,
#                             maven, inotify-tools, nodejs
#   4. Docker image         — costruisce l'immagine <project>-dev:latest
#   5. Docker container     — avvia il container di sviluppo con:
#                               - bind mount PWD → /workspace (sorgenti)
#                               - bind mount ./logs/ → /app/logs (log applicazione)
#                               - bind mount ./config/ → /app/config (configurazione)
#                               - porte API e Vite esposte sull'host
#   6. Scaffolding          — genera la struttura del progetto solo se pom.xml
#                             non esiste: pom.xml, App.java, auth, migration SQL,
#                             logback.xml, application.properties, struttura Vite
#   7. npm install          — installa le dipendenze vite nel container
#   8. cmd tool             — copia bin/cmd nel container e lo registra come
#                             comando globale /usr/local/bin/cmd
#
# Al riavvio (container già esistente ma fermo):
#   - Se il container esiste ma è fermo: docker start
#   - Se il container è già running: nessuna azione
#
# -----------------------------------------------------------------------------
# PROCEDURA DI INSTALL (--postgres)
# -----------------------------------------------------------------------------
#
# Installa un container PostgreSQL standalone, indipendente da qualsiasi
# configurazione di progetto. Non richiede .env.
#
# Dopo l'installazione usare 'cmd db setup' per creare il database e l'utente
# applicativo specifici del progetto.
#
# -----------------------------------------------------------------------------
# PROCEDURA DI INSTALL (--mailpit)
# -----------------------------------------------------------------------------
#
# Installa un container Mailpit standalone per il test locale dell'invio email.
# Mailpit è un server SMTP "finto": cattura le email senza spedirle davvero
# e le espone in una web UI.
#
# Porte esposte sull'host:
#   - SMTP:   1025  (usare come mail.host=mailpit, mail.port=1025 in application.properties)
#   - Web UI: 8025  (aprire http://localhost:8025 per visualizzare le email)
#
# Non richiede .env. Non invia email reali.
#
# -----------------------------------------------------------------------------
# POLICY LOG
# -----------------------------------------------------------------------------
#
# I log dell'applicazione sono scritti da logback in /app/logs/ dentro
# il container (file: service.log, rotazione giornaliera con compressione gzip).
#
# In sviluppo /app/logs/ è montato via bind mount su ./logs/ nella cartella
# del progetto host, rendendo i log direttamente accessibili sul filesystem.
# La cartella ./logs/ viene creata automaticamente prima del docker run
# con i permessi dell'utente corrente (compatibile con macOS e Linux).
# La cartella è esclusa da git tramite .gitignore.
#
# -----------------------------------------------------------------------------
# POLICY NETWORK
# -----------------------------------------------------------------------------
#
# Tutti i container del progetto (sviluppo, postgres, produzione) condividono
# la stessa rete Docker: <project>-net.
# Questo consente la comunicazione tra i container tramite hostname
# (es. il container di sviluppo raggiunge postgres come "postgres").
# La rete viene creata automaticamente se non esiste.
# Se il container postgres è già in esecuzione, viene connesso alla rete
# del progetto automaticamente durante l'install dev.
#
# -----------------------------------------------------------------------------
# STRUTTURA DELL'AMBIENTE DI SVILUPPO
# -----------------------------------------------------------------------------
#
#   Host (macOS/Linux)                Container: <project>
#   ──────────────────                ──────────────────────────────
#   ./              ──bind mount──►   /workspace      (sorgenti)
#   ./logs/         ──bind mount──►   /app/logs/      (log applicazione)
#   ./config/       ──bind mount──►   /app/config/    (configurazione runtime)
#   localhost:2310  ◄─port mapping─   :8080           (Undertow API)
#   localhost:2350  ◄─port mapping─   :5173           (Vite dev server)
#   localhost:5005  ◄─port mapping─   :5005           (JDWP debugger)
#
# -----------------------------------------------------------------------------
# COMANDI DISPONIBILI DOPO L'INSTALL
# -----------------------------------------------------------------------------
#
# Entrare nel container:
#   docker exec -it <project> bash
#
# Comandi disponibili dentro il container (tramite cmd):
#   cmd app build       — compila il backend (Maven)
#   cmd app dev         — avvia in modalità sviluppo con hot reload (inotifywait)
#   cmd app run         — avvia il backend compilato in foreground
#   cmd app start       — avvia il backend compilato in background
#   cmd vite run        — avvia il dev server Vite con proxy API
#   cmd vite build      — compila il frontend per la produzione
#   cmd db              — apre la CLI PostgreSQL interattiva
#   cmd db setup        — crea utente e database dal file .env
#   cmd git push/pull   — operazioni git con credenziali da .env
#
# =============================================================================
set -e

INSTALLER_DIR=$(dirname "$0")

show_help() {
    cat << EOF
Usage: ./install.sh [OPTION]

Options:
  (none)                  Create complete development environment
  --groupid <id>          Maven GroupId (default: com.example)
  --postgres              Install PostgreSQL container
  -n, --name <name>       PostgreSQL container name (default: postgres)
  -p, --port <port>       PostgreSQL host port (default: 5432)
  --network <network>     Docker network for PostgreSQL (default: bridge)
  --mailpit               Install Mailpit container (fake SMTP + web UI)
  -n, --name <name>       Mailpit container name (default: mailpit)
  -p, --port <port>       Mailpit SMTP host port (default: 1025)
  --ui-port <port>        Mailpit web UI host port (default: 8025)
  --network <network>     Docker network for Mailpit (default: bridge)
  --help, -h              Show this message

Examples:
  ./install.sh                               # First install
  ./install.sh --groupid io.mycompany        # Custom GroupId
  ./install.sh --postgres                    # Install PostgreSQL
  ./install.sh --postgres -n mydb            # Install PostgreSQL with custom name
  ./install.sh --postgres -p 5433            # Install PostgreSQL on custom port
  ./install.sh --postgres --network hola-net # Install on custom network
  ./install.sh --postgres -n mydb -p 5433 --network hola-net  # All custom parameters
  ./install.sh --mailpit                     # Install Mailpit
  ./install.sh --mailpit -p 2025             # Install Mailpit with custom SMTP port
  ./install.sh --mailpit --ui-port 9025      # Install Mailpit with custom UI port
  ./install.sh --mailpit --network hola-net  # Install Mailpit on custom network
EOF
    exit 0
}

generate_env_file() {
    project_dir=$(basename "$PWD")
    cat > .env << EOF
# Java/Undertow project configuration
# Generated by install.sh

# ========================================
# Common
# ========================================
PROJECT_NAME=PROJECT_DIR_PLACEHOLDER
JAVA_VERSION=21
ARTIFACT_VERSION=1.0.0

# ========================================
# Development
# ========================================
DEV_NETWORK_SUFFIX=-net
DEV_IMAGE=ubuntu:24.04

# Undertow (API)
API_PORT=8080
API_PORT_HOST=2310

# Vite (dev server with proxy)
VITE_PORT=5173
VITE_PORT_HOST=2350

# Java Debug (JDWP)
DEBUG_PORT=5005
DEBUG_PORT_HOST=5005

# ========================================
# Release
# ========================================
RELEASE_IMAGE=ubuntu:24.04
RELEASE_MEMORY_LIMIT=512m
RELEASE_MEMORY_RESERVATION=256m
RELEASE_CPU_LIMIT=1.0
RELEASE_CPU_RESERVATION=0.5
RELEASE_PORT=8080
RELEASE_APP_USER=appuser
RELEASE_APP_USER_UID=1001
RELEASE_APP_USER_GID=1001

# ========================================
# Database
# ========================================
PGSQL_ENABLED=y
PGSQL_IMAGE=postgres:16
PGSQL_HOST=postgres
PGSQL_PORT=5432
PGSQL_PORT_HOST=2340
PGSQL_ROOT_USER=postgres
PGSQL_ROOT_PASSWORD=postgres
PGSQL_NAME=PROJECT_DIR_PLACEHOLDER
PGSQL_USER=PROJECT_DIR_PLACEHOLDER
PGSQL_PASSWORD=secret

# ========================================
# Git
# ========================================
GIT_USER=
GIT_EMAIL=
GIT_TOKEN=
EOF

    sed "s|PROJECT_DIR_PLACEHOLDER|$project_dir|g" .env > .env.tmp && mv -f .env.tmp .env
}

# =============================================================================
# install_postgres — container PostgreSQL standalone, senza .env
# =============================================================================

install_postgres() {
    local CONTAINER_NAME="postgres"
    local PORT_HOST=""
    local NETWORK=""

    # Parse arguments
    while [ $# -gt 0 ]; do
        case "$1" in
            -n|--name)
                [ -n "$2" ] || { echo "ERRORE: -n|--name richiede un valore"; exit 1; }
                CONTAINER_NAME="$2"
                shift 2
                ;;
            -p|--port)
                [ -n "$2" ] || { echo "ERRORE: -p|--port richiede un valore"; exit 1; }
                PORT_HOST="$2"
                shift 2
                ;;
            --network)
                [ -n "$2" ] || { echo "ERRORE: --network richiede un valore"; exit 1; }
                NETWORK="$2"
                shift 2
                ;;
            *) shift ;;
        esac
    done

    local IMAGE="${PGSQL_IMAGE:-postgres:16}"
    local ROOT_USER="${PGSQL_ROOT_USER:-postgres}"
    local ROOT_PASSWORD="${PGSQL_ROOT_PASSWORD:-postgres}"
    PORT_HOST="${PORT_HOST:-${PGSQL_PORT_HOST:-5432}}"

    if docker ps -a --format "{{.Names}}" | grep -q "^${CONTAINER_NAME}$"; then
        if docker ps --format "{{.Names}}" | grep -q "^${CONTAINER_NAME}$"; then
            echo "PostgreSQL container '$CONTAINER_NAME' is already running"
        else
            echo "Starting PostgreSQL container '$CONTAINER_NAME'..."
            docker start "$CONTAINER_NAME"
        fi
        echo "Done"
        exit 0
    fi

    if ! docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "^${IMAGE}$"; then
        echo "Pulling PostgreSQL image..."
        docker pull "$IMAGE"
    fi

    echo "Creating PostgreSQL container '$CONTAINER_NAME'..."

    # Build docker run command
    local DOCKER_CMD="docker run -d --name \"$CONTAINER_NAME\""
    DOCKER_CMD="$DOCKER_CMD -e POSTGRES_USER=\"$ROOT_USER\""
    DOCKER_CMD="$DOCKER_CMD -e POSTGRES_PASSWORD=\"$ROOT_PASSWORD\""
    DOCKER_CMD="$DOCKER_CMD -p \"$PORT_HOST:5432\""
    DOCKER_CMD="$DOCKER_CMD -v \"${CONTAINER_NAME}-data:/var/lib/postgresql/data\""

    # Add network if specified
    if [ -n "$NETWORK" ]; then
        # Check if network exists
        if ! docker network ls --format '{{.Name}}' | grep -q "^${NETWORK}$"; then
            echo "ERRORE: Network '$NETWORK' non esistente. Crearla prima con: docker network create $NETWORK"
            exit 1
        fi
        DOCKER_CMD="$DOCKER_CMD --network \"$NETWORK\""
    fi

    DOCKER_CMD="$DOCKER_CMD \"$IMAGE\""

    eval "$DOCKER_CMD" >/dev/null

    echo "Done"
}

# =============================================================================
# install_mailpit — container Mailpit standalone (SMTP fake + web UI)
# =============================================================================

install_mailpit() {
    local CONTAINER_NAME="mailpit"
    local SMTP_PORT_HOST=""
    local UI_PORT_HOST=""
    local NETWORK=""

    # Parse arguments
    while [ $# -gt 0 ]; do
        case "$1" in
            -n|--name)
                [ -n "$2" ] || { echo "ERRORE: -n|--name richiede un valore"; exit 1; }
                CONTAINER_NAME="$2"
                shift 2
                ;;
            -p|--port)
                [ -n "$2" ] || { echo "ERRORE: -p|--port richiede un valore"; exit 1; }
                SMTP_PORT_HOST="$2"
                shift 2
                ;;
            --ui-port)
                [ -n "$2" ] || { echo "ERRORE: --ui-port richiede un valore"; exit 1; }
                UI_PORT_HOST="$2"
                shift 2
                ;;
            --network)
                [ -n "$2" ] || { echo "ERRORE: --network richiede un valore"; exit 1; }
                NETWORK="$2"
                shift 2
                ;;
            *) shift ;;
        esac
    done

    local IMAGE="axllent/mailpit"
    SMTP_PORT_HOST="${SMTP_PORT_HOST:-1025}"
    UI_PORT_HOST="${UI_PORT_HOST:-8025}"

    if docker ps -a --format "{{.Names}}" | grep -q "^${CONTAINER_NAME}$"; then
        if docker ps --format "{{.Names}}" | grep -q "^${CONTAINER_NAME}$"; then
            echo "Mailpit container '$CONTAINER_NAME' is already running"
        else
            echo "Starting Mailpit container '$CONTAINER_NAME'..."
            docker start "$CONTAINER_NAME"
        fi
        echo "Done"
        exit 0
    fi

    if ! docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "^${IMAGE}$"; then
        echo "Pulling Mailpit image..."
        docker pull "$IMAGE"
    fi

    echo "Creating Mailpit container '$CONTAINER_NAME'..."

    # Build docker run command
    local DOCKER_CMD="docker run -d --name \"$CONTAINER_NAME\""
    DOCKER_CMD="$DOCKER_CMD -p \"$SMTP_PORT_HOST:1025\""
    DOCKER_CMD="$DOCKER_CMD -p \"$UI_PORT_HOST:8025\""

    # Add network if specified
    if [ -n "$NETWORK" ]; then
        # Check if network exists
        if ! docker network ls --format '{{.Name}}' | grep -q "^${NETWORK}$"; then
            echo "ERRORE: Network '$NETWORK' non esistente. Crearla prima con: docker network create $NETWORK"
            exit 1
        fi
        DOCKER_CMD="$DOCKER_CMD --network \"$NETWORK\""
    fi

    DOCKER_CMD="$DOCKER_CMD \"$IMAGE\""

    eval "$DOCKER_CMD" >/dev/null

    echo "Done"
}

# =============================================================================
# install_dev — ambiente di sviluppo completo
# =============================================================================

install_dev() {
    local GROUP_ID="com.example"
    while [ $# -gt 0 ]; do
        case "$1" in
            --groupid)
                [ -n "$2" ] || { echo "ERRORE: --groupid richiede un valore (es. io.mycompany)"; exit 1; }
                GROUP_ID="$2"
                shift 2
                ;;
            *) shift ;;
        esac
    done
    local GROUP_DIR
    GROUP_DIR=$(echo "$GROUP_ID" | tr '.' '/')

    if [ ! -f .env ]; then
        echo "Generating .env..."
        generate_env_file
    fi
    . ./.env

    local DEV_NETWORK="$PROJECT_NAME$DEV_NETWORK_SUFFIX"
    local DEV_CONTAINER="$PROJECT_NAME"

    mkdir -p config

    if docker ps -a --format '{{.Names}}' | grep -q "^$DEV_CONTAINER$"; then
        if ! docker ps --format '{{.Names}}' | grep -q "^$DEV_CONTAINER$"; then
            echo "Starting development container..."
            docker start "$DEV_CONTAINER" >/dev/null
        fi
    else
        if ! docker network ls --format '{{.Name}}' | grep -q "^$DEV_NETWORK$"; then
            echo "Creating Docker network..."
            docker network create "$DEV_NETWORK" >/dev/null
        fi

        if docker ps --format '{{.Names}}' | grep -q "^postgres$"; then
            docker network connect "$DEV_NETWORK" "postgres" 2>/dev/null || true
        fi

        if docker ps --format '{{.Names}}' | grep -q "^mailpit$"; then
            docker network connect "$DEV_NETWORK" "mailpit" 2>/dev/null || true
        fi

        mkdir -p docker
        cat > docker/Dockerfile.dev << 'DOCKERFILE'
FROM ubuntu:24.04

ENV DEBIAN_FRONTEND=noninteractive

RUN apt-get update && apt-get install -y \
    git \
    curl \
    nano \
    postgresql-client \
    ca-certificates \
    openjdk-21-jdk \
    maven \
    inotify-tools \
    rsync \
    && curl -fsSL https://deb.nodesource.com/setup_20.x | bash - \
    && apt-get install -y nodejs \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace

RUN mkdir -p /app/logs
DOCKERFILE

        echo "Building development image..."
        docker build -t "$PROJECT_NAME-dev:latest" -f docker/Dockerfile.dev .

        mkdir -p logs
        echo "Starting development container..."
        docker run -it -d \
            --name "$DEV_CONTAINER" \
            -v "$PWD":/workspace \
            -v "$PWD/logs":/app/logs \
            -v "$PWD/config":/app/config \
            -w /workspace \
            -p "$API_PORT_HOST:$API_PORT" \
            -p "$VITE_PORT_HOST:$VITE_PORT" \
            -p "$DEBUG_PORT_HOST:$DEBUG_PORT" \
            --network "$DEV_NETWORK" \
            "$PROJECT_NAME-dev:latest" \
            tail -f /dev/null >/dev/null

        rm -f docker/Dockerfile.dev
        rmdir docker 2>/dev/null || true
    fi

    rm -f .gitignore
    cat > .gitignore << 'GITIGNORE'
# Build artifacts
target/
vite/node_modules/

# Vite build output (generato da 'npm run build', non tracciato)
src/main/resources/static/

# Docker temporaneo
docker/

# Environment (contiene credenziali)
.env

# Log (directory di log del container, non tracciata)
logs/

# Config runtime (contiene credenziali, generato da install.sh)
config/
GITIGNORE

    if [ ! -f pom.xml ]; then
        echo "Scaffolding project..."

        mkdir -p "src/main/java/$GROUP_DIR"
        mkdir -p src/main/resources/static
        mkdir -p src/main/resources/db/migration

        cp -r "$INSTALLER_DIR/lib/." src/main/java/
        rm -rf "$INSTALLER_DIR/lib"

        # config/application.properties da template
        cp "$INSTALLER_DIR/template/application.properties" config/application.properties
        sed "s|{{PROJECT_NAME}}|$PROJECT_NAME|g" config/application.properties > config/application.properties.tmp && mv -f config/application.properties.tmp config/application.properties
        sed "s|{{PGSQL_PASSWORD}}|$PGSQL_PASSWORD|g" config/application.properties > config/application.properties.tmp && mv -f config/application.properties.tmp config/application.properties
        sed "s|db.host=postgres|db.host=$PGSQL_HOST|g" config/application.properties > config/application.properties.tmp && mv -f config/application.properties.tmp config/application.properties

        # pom.xml da template
        cp "$INSTALLER_DIR/template/pom.xml" pom.xml
        sed "s|{{APP_PACKAGE}}|$GROUP_ID|g" pom.xml > pom.xml.tmp && mv -f pom.xml.tmp pom.xml

        # Java files da template
        cp "$INSTALLER_DIR/template/java/App.java" "src/main/java/$GROUP_DIR/App.java"
        mkdir -p "src/main/java/$GROUP_DIR/auth"
        cp -r "$INSTALLER_DIR/template/java/auth/." "src/main/java/$GROUP_DIR/auth/"
        find "src/main/java/$GROUP_DIR" -name "*.java" -type f | while read -r file; do
            sed "s|{{APP_PACKAGE}}|$GROUP_ID|g" "$file" > "$file.tmp" && mv -f "$file.tmp" "$file"
        done

        cat > src/main/resources/logback.xml << 'LOGBACKXML'
<configuration>

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss} [%-5level] %logger{25} - %msg%n</pattern>
        </encoder>
    </appender>

    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>/app/logs/service.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <!-- Rotazione giornaliera, compressione gzip, 30 giorni di storico -->
            <fileNamePattern>/app/logs/service.%d{yyyy-MM-dd}.log.gz</fileNamePattern>
            <maxHistory>30</maxHistory>
        </rollingPolicy>
        <encoder>
            <pattern>%d{yyyy-MM-dd HH:mm:ss} [%-5level] %logger{25} - %msg%n</pattern>
        </encoder>
    </appender>

    <!-- Silenzia i logger interni di POI e xmlbeans -->
    <logger name="org.apache.poi"      level="OFF"/>
    <logger name="org.apache.xmlbeans" level="OFF"/>

    <root level="INFO">
        <appender-ref ref="CONSOLE"/>
        <appender-ref ref="FILE"/>
    </root>

</configuration>
LOGBACKXML

        # .vscode/launch.json da template
        mkdir -p .vscode
        cp "$INSTALLER_DIR/template/.vscode/launch.json" .vscode/launch.json

        # Migrazione iniziale Flyway da template
        for f in "$INSTALLER_DIR/template/sql/"*.sql; do
            cp "$f" "src/main/resources/db/migration/$(basename "$f")"
        done

        mkdir -p vite

        cat > vite/package.json << 'PACKAGEJSON'
{
  "name": "vite",
  "private": true,
  "version": "0.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite --host 0.0.0.0",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "bootstrap": "^5.3.3"
  },
  "devDependencies": {
    "vite": "^6.0.0"
  }
}
PACKAGEJSON

        cat > vite/vite.config.js << 'VITECONFIG'
import { defineConfig } from 'vite'
import { resolve } from 'path'

export default defineConfig({
  root: 'src',
  build: {
    outDir: '../../src/main/resources/static',
    emptyOutDir: true,
    rollupOptions: {
      input: {
        index:      resolve(__dirname, 'src/index.html'),
        login:      resolve(__dirname, 'src/auth/login.html'),
        changepass: resolve(__dirname, 'src/auth/changepass.html'),
        home:       resolve(__dirname, 'src/home/main.html'),
      }
    }
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
    proxy: {
      '/api': 'http://localhost:8080'
    }
  },
  plugins: [
    {
      name: 'route-rewrite',
      configureServer(server) {
        server.middlewares.use((req, _res, next) => {
          if (req.url === '/home') req.url = '/home/main.html'
          else if (req.url === '/auth') req.url = '/auth/login.html'
          next()
        })
      }
    }
  ]
})
VITECONFIG

        rm -rf "$INSTALLER_DIR/template"
    fi

    if [ -d "static" ]; then
        echo "Copying static/ → vite/src/..."
        mkdir -p vite/src
        cp -r static/. vite/src/
        rm -rf static
        echo "✓ static/ copiato in vite/src/ e rimosso"
    fi

    echo "Installing npm dependencies..."
    docker exec "$DEV_CONTAINER" sh -c "cd /workspace/vite && npm install"

    echo "Setting up cmd tool..."
    mkdir -p bin
    cp "$INSTALLER_DIR/cmd" bin/cmd
    sed "s|com\.example|$GROUP_ID|g" bin/cmd > bin/cmd.tmp && mv -f bin/cmd.tmp bin/cmd
    chmod +x bin/cmd
    docker exec "$DEV_CONTAINER" sh -c "
        ln -sf /workspace/bin/cmd /usr/local/bin/cmd
        chmod +x /workspace/bin/cmd
        grep -qxF \"alias cls='clear'\" /root/.bashrc || echo \"alias cls='clear'\" >> /root/.bashrc
    "

    echo "# $PROJECT_NAME" > README.md

    if [ -d .git ]; then
        rm -rf .git
    fi
    rm -f cmd

    echo "Done"
}

# =============================================================================
# Dispatcher
# =============================================================================

case "$1" in
    --postgres)
        shift
        install_postgres "$@"
        ;;
    --mailpit)
        shift
        install_mailpit "$@"
        ;;
    --help|-h)
        show_help
        ;;
    *)
        install_dev "$@"
        ;;
esac
