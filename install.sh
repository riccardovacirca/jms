#!/bin/sh
# =============================================================================
# Install Script - Development Environment Setup
# =============================================================================
#
# Usage: ./install.sh [OPTIONS]
#
# Options:
#   (nessuna)               Crea o riavvia l'ambiente di sviluppo completo
#   --postgres              Installa/ricrea il container PostgreSQL del progetto
#   --mailpit               Installa/ricrea il container Mailpit del progetto
#   --ngrok                 Installa ngrok nel container di sviluppo
#   --help, -h              Mostra questo messaggio
#
# Examples:
#   ./install.sh             # Primo install o riavvio
#   ./install.sh --postgres  # Installa/ricrea PostgreSQL
#   ./install.sh --mailpit   # Installa/ricrea Mailpit
#   ./install.sh --ngrok     # Installa ngrok nel container
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
#                             non esiste: pom.xml, App.java skeleton, logback.xml,
#                             application.properties, struttura Vite, modules/
#                             I moduli (es. auth) si installano manualmente via
#                             'cmd module import auth.tar.gz'
#   7. npm install          — installa le dipendenze vite nel container
#   8. cmd tool             — copia bin/cmd nel container e lo registra come
#                             comando globale /usr/local/bin/cmd
#
# Al riavvio (container già esistente ma fermo):
#   - Se il container esiste ma è fermo: docker start
#   - Se il container è già running: nessuna azione
#
# -----------------------------------------------------------------------------
# PROCEDURA DI INSTALL (--postgres / --with-postgres)
# -----------------------------------------------------------------------------
#
# Installa un container PostgreSQL vincolato al progetto corrente.
# Genera .env se non esiste, legge tutti i parametri da esso.
# Il container viene nominato <project>-db e connesso alla rete <project>-net.
# Se il container esiste già, chiede conferma prima di ricrearlo.
# Se il container dev è in esecuzione, esegue anche 'cmd db setup'.
#
# -----------------------------------------------------------------------------
# PROCEDURA DI INSTALL (--mailpit)
# -----------------------------------------------------------------------------
#
# Installa un container Mailpit vincolato al progetto corrente.
# Mailpit è un server SMTP "finto": cattura le email senza spedirle davvero
# e le espone in una web UI.
# Genera .env se non esiste, legge tutti i parametri da esso.
# Il container viene nominato <project>-mail e connesso alla rete <project>-net.
# Se il container esiste già, chiede conferma prima di ricrearlo.
#
# Porte esposte sull'host (da .env):
#   - SMTP:   MAILPIT_SMTP_PORT_HOST  (default 1025)
#   - Web UI: MAILPIT_UI_PORT_HOST    (default 8025, aprire http://localhost:8025)
#
# -----------------------------------------------------------------------------
# PROCEDURA DI INSTALL (--ngrok)
# -----------------------------------------------------------------------------
#
# Installa il pacchetto ngrok dentro il container di sviluppo tramite apt.
# ngrok crea un tunnel HTTPS pubblico verso la porta del backend (8080),
# necessario in sviluppo locale per ricevere i webhook Vonage.
#
# Configurazione in .env:
#   - NGROK_ENABLED=true/false  abilita l'avvio automatico con cmd app run/debug
#   - NGROK_AUTHTOKEN=...       token di autenticazione (dashboard.ngrok.com/authtokens)
#
# Avvio automatico:
#   Se NGROK_ENABLED=true, ngrok viene avviato in background all'avvio di
#   cmd app run / cmd app debug e terminato al Ctrl+C.
#   L'URL pubblico è mostrato a console e disponibile via cmd ngrok status.
#
# Note:
#   - Sul piano gratuito l'URL cambia a ogni riavvio del tunnel.
#   - Aggiornare manualmente cti.vonage.event_url in application.properties
#     e i webhook URL nel Vonage Dashboard quando l'URL cambia.
#   - Porta web UI ngrok: http://localhost:4040
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
#   cmd gui run         — avvia il dev server Vite con proxy API
#   cmd gui build       — compila il frontend per la produzione
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
  (none)                  Create or restart the development environment
  --postgres              Install/recreate project PostgreSQL container
  --mailpit               Install/recreate project Mailpit container
  --ngrok                 Install ngrok inside the dev container
  --help, -h              Show this message

Examples:
  ./install.sh             # First install or restart
  ./install.sh --postgres  # Install/recreate PostgreSQL (reads from .env)
  ./install.sh --mailpit   # Install/recreate Mailpit (reads from .env)
  ./install.sh --ngrok     # Install ngrok in dev container
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
DEV_NETWORK=PROJECT_DIR_PLACEHOLDER-net
DEV_IMAGE=ubuntu:24.04

# Undertow (API)
API_PORT=8080
API_PORT_HOST=2310
APP_BASE_URL=http://localhost:2310

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
PGSQL_HOST=PROJECT_DIR_PLACEHOLDER-db
PGSQL_PORT=5432
PGSQL_PORT_HOST=2340
PGSQL_ROOT_USER=postgres
PGSQL_ROOT_PASSWORD=postgres
PGSQL_NAME=PROJECT_DIR_PLACEHOLDER
PGSQL_USER=PROJECT_DIR_PLACEHOLDER
PGSQL_PASSWORD=secret
DB_POOL_SIZE=10

# ========================================
# JWT
# ========================================
JWT_SECRET=dev-secret-change-in-production
JWT_ACCESS_EXPIRY_SECONDS=900

# ========================================
# Async Handler
# ========================================
ASYNC_POOL_SIZE=20
ASYNC_MAX_BODY_SIZE=10485760

# ========================================
# Mailpit (fake SMTP for development)
# ========================================
MAILPIT_CONTAINER=PROJECT_DIR_PLACEHOLDER-mail
MAILPIT_IMAGE=axllent/mailpit
MAILPIT_HOST=PROJECT_DIR_PLACEHOLDER-mail
MAILPIT_SMTP_PORT=1025
MAILPIT_SMTP_PORT_HOST=2325
MAILPIT_UI_PORT=8025
MAILPIT_UI_PORT_HOST=2335
MAILPIT_USER=
MAILPIT_PASSWORD=
MAIL_ENABLED=false
MAIL_AUTH=false
MAIL_FROM=noreply@example.com

# ========================================
# Scheduler
# ========================================
SCHEDULER_ENABLED=true
SCHEDULER_POLL_INTERVAL_SECONDS=15

# ========================================
# ngrok (tunnel per webhook in sviluppo)
# ========================================
NGROK_ENABLED=false
NGROK_AUTHTOKEN=

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
# install_postgres — container PostgreSQL del progetto corrente
# =============================================================================

install_postgres() {
    if [ ! -f .env ]; then
        echo "Generating .env..."
        generate_env_file
    fi
    . ./.env

    local CONTAINER_NAME="$PGSQL_HOST"
    local IMAGE="${PGSQL_IMAGE:-postgres:16}"
    local LOCAL_TAG="${PROJECT_NAME}-db-image"
    local ROOT_USER="${PGSQL_ROOT_USER:-postgres}"
    local ROOT_PASSWORD="${PGSQL_ROOT_PASSWORD:-postgres}"
    local PORT_HOST="${PGSQL_PORT_HOST:-5432}"
    local VOLUME="${PROJECT_NAME}-db-volume"

    if docker ps -a --format "{{.Names}}" | grep -q "^${CONTAINER_NAME}$"; then
        echo "PostgreSQL container '$CONTAINER_NAME' already exists."
        printf "Remove and recreate it? [y/N] "
        read answer || true
        case "$answer" in
            y|Y) ;;
            *) echo "Aborted"; exit 0 ;;
        esac
        docker stop "$CONTAINER_NAME" 2>/dev/null || true
        docker rm "$CONTAINER_NAME"
    fi

    if ! docker network ls --format '{{.Name}}' | grep -q "^${DEV_NETWORK}$"; then
        echo "Creating Docker network '$DEV_NETWORK'..."
        docker network create "$DEV_NETWORK" >/dev/null
    fi

    if ! docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "^${LOCAL_TAG}:latest$"; then
        if ! docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "^${IMAGE}$"; then
            echo "Pulling PostgreSQL image..."
            docker pull "$IMAGE"
        fi
        echo "Tagging PostgreSQL image as '$LOCAL_TAG:latest'..."
        docker tag "$IMAGE" "$LOCAL_TAG:latest"
    fi

    echo "Creating PostgreSQL container '$CONTAINER_NAME'..."
    docker run -d \
        --name "$CONTAINER_NAME" \
        --network "$DEV_NETWORK" \
        -e POSTGRES_USER="$ROOT_USER" \
        -e POSTGRES_PASSWORD="$ROOT_PASSWORD" \
        -p "${PORT_HOST}:5432" \
        -v "${VOLUME}:/var/lib/postgresql/data" \
        "$LOCAL_TAG:latest" >/dev/null

    echo "Waiting for PostgreSQL to be ready..."
    local MAX_WAIT=30
    local WAITED=0
    while [ $WAITED -lt $MAX_WAIT ]; do
        if docker exec "$CONTAINER_NAME" pg_isready -U "$ROOT_USER" >/dev/null 2>&1; then
            echo "PostgreSQL is ready"
            break
        fi
        sleep 1
        WAITED=$((WAITED + 1))
        printf "."
    done
    echo ""

    if [ $WAITED -eq $MAX_WAIT ]; then
        echo "WARNING: PostgreSQL did not become ready within ${MAX_WAIT}s"
        echo "Run manually: docker exec $PROJECT_NAME cmd db setup"
    elif docker ps --format '{{.Names}}' | grep -q "^${PROJECT_NAME}$"; then
        echo "Setting up database..."
        docker exec "$PROJECT_NAME" cmd db setup
    else
        echo "Dev container not running. Run manually: docker exec $PROJECT_NAME cmd db setup"
    fi

    echo ""
    echo "Done"
    echo "PostgreSQL container: $CONTAINER_NAME (port $PORT_HOST)"
}

# =============================================================================
# install_mailpit — container Mailpit del progetto corrente
# =============================================================================

install_mailpit() {
    if [ ! -f .env ]; then
        echo "Generating .env..."
        generate_env_file
    fi
    . ./.env

    local CONTAINER_NAME="$MAILPIT_CONTAINER"
    local IMAGE="${MAILPIT_IMAGE:-axllent/mailpit}"
    local LOCAL_TAG="${PROJECT_NAME}-mail-image"
    local SMTP_PORT_HOST="${MAILPIT_SMTP_PORT_HOST:-2325}"
    local UI_PORT_HOST="${MAILPIT_UI_PORT_HOST:-2335}"

    if docker ps -a --format "{{.Names}}" | grep -q "^${CONTAINER_NAME}$"; then
        echo "Mailpit container '$CONTAINER_NAME' already exists."
        printf "Remove and recreate it? [y/N] "
        read answer || true
        case "$answer" in
            y|Y) ;;
            *) echo "Aborted"; exit 0 ;;
        esac
        docker stop "$CONTAINER_NAME" 2>/dev/null || true
        docker rm "$CONTAINER_NAME"
    fi

    if ! docker network ls --format '{{.Name}}' | grep -q "^${DEV_NETWORK}$"; then
        echo "Creating Docker network '$DEV_NETWORK'..."
        docker network create "$DEV_NETWORK" >/dev/null
    fi

    if ! docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "^${LOCAL_TAG}:latest$"; then
        if ! docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "^${IMAGE}$"; then
            echo "Pulling Mailpit image..."
            docker pull "$IMAGE"
        fi
        echo "Tagging Mailpit image as '$LOCAL_TAG:latest'..."
        docker tag "$IMAGE" "$LOCAL_TAG:latest"
    fi

    echo "Creating Mailpit container '$CONTAINER_NAME'..."
    docker run -d \
        --name "$CONTAINER_NAME" \
        --network "$DEV_NETWORK" \
        -p "${SMTP_PORT_HOST}:1025" \
        -p "${UI_PORT_HOST}:8025" \
        "$LOCAL_TAG:latest" >/dev/null

    echo ""
    echo "Done"
    echo "Mailpit container: $CONTAINER_NAME"
    echo "SMTP:   localhost:$SMTP_PORT_HOST  (mail.host=$CONTAINER_NAME, mail.port=1025)"
    echo "Web UI: http://localhost:$UI_PORT_HOST"
}

# =============================================================================
# install_ngrok — ngrok dentro il container di sviluppo
# =============================================================================

install_ngrok() {
    if [ ! -f .env ]; then
        echo "Generating .env..."
        generate_env_file
    fi
    . ./.env

    local DEV_CONTAINER="$PROJECT_NAME"

    if ! docker ps --format '{{.Names}}' | grep -q "^${DEV_CONTAINER}$"; then
        echo "Dev container '$DEV_CONTAINER' is not running. Start it first with: ./install.sh"
        exit 1
    fi

    echo "Installing ngrok in container '$DEV_CONTAINER'..."
    docker exec "$DEV_CONTAINER" bash -c "
        curl -sSL https://ngrok-agent.s3.amazonaws.com/ngrok.asc \
          | tee /etc/apt/trusted.gpg.d/ngrok.asc >/dev/null \
          && echo 'deb https://ngrok-agent.s3.amazonaws.com bookworm main' \
          | tee /etc/apt/sources.list.d/ngrok.list \
          && apt-get update -qq && apt-get install -y ngrok
    "

    echo ""
    echo "Done"
    echo "ngrok installed in container '$DEV_CONTAINER'"
    echo ""
    echo "Next steps:"
    echo "  1. Set NGROK_AUTHTOKEN=<token> in .env  (get it at https://dashboard.ngrok.com/authtokens)"
    echo "  2. Set NGROK_ENABLED=true in .env to auto-start ngrok with cmd app run/debug"
    echo "  3. After startup, update cti.vonage.event_url in config/application.properties"
    echo "     with the public URL shown by ngrok (changes on every restart)"
}

# =============================================================================
# install_dev — ambiente di sviluppo completo
# =============================================================================

install_dev() {
    if [ ! -f .env ]; then
        echo "Generating .env..."
        generate_env_file
    fi
    . ./.env

    local DEV_CONTAINER="$PROJECT_NAME"

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

        local MAILPIT_CONT="${MAILPIT_CONTAINER:-mailpit}"
        if docker ps --format '{{.Names}}' | grep -q "^${MAILPIT_CONT}$"; then
            docker network connect "$DEV_NETWORK" "$MAILPIT_CONT" 2>/dev/null || true
        fi

        echo "Building development image..."
        docker build -t "$PROJECT_NAME-image" -f docker/Dockerfile.dev .

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
            "$PROJECT_NAME-image" \
            tail -f /dev/null >/dev/null
    fi

    rm -f .gitignore
    cat > .gitignore << 'GITIGNORE'
target/
gui/node_modules/
src/main/resources/static/
docker/
.env
logs/
config/
GITIGNORE

    # config/application.properties — sostituisce i placeholder con i valori da .env
    sed "s|{{PROJECT_NAME}}|$PROJECT_NAME|g" config/application.properties > config/application.properties.tmp && mv -f config/application.properties.tmp config/application.properties
    sed "s|{{PGSQL_PASSWORD}}|$PGSQL_PASSWORD|g" config/application.properties > config/application.properties.tmp && mv -f config/application.properties.tmp config/application.properties
    sed "s|db.host=postgres|db.host=$PGSQL_HOST|g" config/application.properties > config/application.properties.tmp && mv -f config/application.properties.tmp config/application.properties
    sed "s|{{APP_BASE_URL}}|${APP_BASE_URL:-http://localhost:$API_PORT_HOST}|g" config/application.properties > config/application.properties.tmp && mv -f config/application.properties.tmp config/application.properties
    sed "s|{{DB_POOL_SIZE}}|${DB_POOL_SIZE:-10}|g" config/application.properties > config/application.properties.tmp && mv -f config/application.properties.tmp config/application.properties
    sed "s|{{JWT_SECRET}}|${JWT_SECRET:-dev-secret-change-in-production}|g" config/application.properties > config/application.properties.tmp && mv -f config/application.properties.tmp config/application.properties
    sed "s|{{JWT_ACCESS_EXPIRY_SECONDS}}|${JWT_ACCESS_EXPIRY_SECONDS:-900}|g" config/application.properties > config/application.properties.tmp && mv -f config/application.properties.tmp config/application.properties
    sed "s|{{ASYNC_POOL_SIZE}}|${ASYNC_POOL_SIZE:-20}|g" config/application.properties > config/application.properties.tmp && mv -f config/application.properties.tmp config/application.properties
    sed "s|{{ASYNC_MAX_BODY_SIZE}}|${ASYNC_MAX_BODY_SIZE:-10485760}|g" config/application.properties > config/application.properties.tmp && mv -f config/application.properties.tmp config/application.properties
    sed "s|{{MAILPIT_HOST}}|${MAILPIT_HOST}|g" config/application.properties > config/application.properties.tmp && mv -f config/application.properties.tmp config/application.properties
    sed "s|{{MAILPIT_SMTP_PORT}}|${MAILPIT_SMTP_PORT:-1025}|g" config/application.properties > config/application.properties.tmp && mv -f config/application.properties.tmp config/application.properties
    sed "s|{{MAILPIT_USER}}|${MAILPIT_USER:-}|g" config/application.properties > config/application.properties.tmp && mv -f config/application.properties.tmp config/application.properties
    sed "s|{{MAILPIT_PASSWORD}}|${MAILPIT_PASSWORD:-}|g" config/application.properties > config/application.properties.tmp && mv -f config/application.properties.tmp config/application.properties
    sed "s|{{MAIL_ENABLED}}|${MAIL_ENABLED:-false}|g" config/application.properties > config/application.properties.tmp && mv -f config/application.properties.tmp config/application.properties
    sed "s|{{MAIL_AUTH}}|${MAIL_AUTH:-false}|g" config/application.properties > config/application.properties.tmp && mv -f config/application.properties.tmp config/application.properties
    sed "s|{{MAIL_FROM}}|${MAIL_FROM:-noreply@example.com}|g" config/application.properties > config/application.properties.tmp && mv -f config/application.properties.tmp config/application.properties
    sed "s|{{SCHEDULER_ENABLED}}|${SCHEDULER_ENABLED:-true}|g" config/application.properties > config/application.properties.tmp && mv -f config/application.properties.tmp config/application.properties
    sed "s|{{SCHEDULER_POLL_INTERVAL_SECONDS}}|${SCHEDULER_POLL_INTERVAL_SECONDS:-15}|g" config/application.properties > config/application.properties.tmp && mv -f config/application.properties.tmp config/application.properties
    sed "s|{{NGROK_ENABLED}}|${NGROK_ENABLED:-false}|g" config/application.properties > config/application.properties.tmp && mv -f config/application.properties.tmp config/application.properties
    sed "s|{{NGROK_AUTHTOKEN}}|${NGROK_AUTHTOKEN:-}|g" config/application.properties > config/application.properties.tmp && mv -f config/application.properties.tmp config/application.properties

    echo "Installing npm dependencies..."
    docker exec "$DEV_CONTAINER" sh -c "cd /workspace/gui && npm install"

    echo "Setting up cmd tool..."
    chmod +x bin/cmd
    docker exec "$DEV_CONTAINER" sh -c "
        ln -sf /workspace/bin/cmd /usr/local/bin/cmd
        chmod +x /workspace/bin/cmd
        grep -qxF \"alias cls='clear'\" /root/.bashrc || echo \"alias cls='clear'\" >> /root/.bashrc
        grep -qxF \"export LC_ALL=C\" /root/.bashrc || echo \"export LC_ALL=C\" >> /root/.bashrc
    "

    echo "# $PROJECT_NAME" > README.md

    if [ -d .git ]; then
        rm -rf .git
    fi

    echo "Done"
}

# =============================================================================
# Dispatcher
# =============================================================================

case "$1" in
    --postgres)
        install_postgres
        ;;
    --mailpit)
        install_mailpit
        ;;
    --ngrok)
        install_ngrok
        ;;
    --help|-h)
        show_help
        ;;
    *)
        install_dev "$@"
        ;;
esac
