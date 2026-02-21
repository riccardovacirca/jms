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
#   --postgres              Aggiunge il container PostgreSQL
#   --help, -h              Mostra questo messaggio
#
# Examples:
#   ./install.sh                                # Primo install o riavvio
#   ./install.sh --groupid io.mycompany         # GroupId personalizzato
#   ./install.sh --postgres                     # Aggiunge PostgreSQL
#   ./install.sh --groupid io.mycompany --postgres
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
#                               - porte API e Vite esposte sull'host
#   6. Scaffolding          — genera la struttura del progetto solo se pom.xml
#                             non esiste: pom.xml, App.java, handler di esempio,
#                             logback.xml, application.properties, struttura Svelte 5
#   7. npm install          — installa le dipendenze frontend nel container
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
# Aggiunge PostgreSQL all'ambiente di sviluppo esistente:
#
#   1. Verifica che PGSQL_ENABLED=y sia impostato in .env
#   2. Verifica che il container di sviluppo sia in esecuzione
#   3. Crea il container PostgreSQL (<project>-postgres) sulla stessa rete
#   4. Attende che PostgreSQL sia pronto (pg_isready)
#   5. Crea il database e l'utente applicativo con i permessi necessari
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
# (es. il container di sviluppo raggiunge postgres come "hello-postgres").
# La rete viene creata automaticamente se non esiste.
#
# -----------------------------------------------------------------------------
# STRUTTURA DELL'AMBIENTE DI SVILUPPO
# -----------------------------------------------------------------------------
#
#   Host (macOS/Linux)                Container: <project>
#   ──────────────────                ──────────────────────────────
#   ./              ──bind mount──►   /workspace      (sorgenti)
#   ./logs/         ──bind mount──►   /app/logs/      (log applicazione)
#   localhost:2310  ◄─port mapping─   :8080           (Undertow API)
#   localhost:2350  ◄─port mapping─   :5173           (Vite dev server)
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
#   cmd svelte run      — avvia il dev server Vite con proxy API
#   cmd svelte build    — compila il frontend per la produzione
#   cmd db              — apre la CLI PostgreSQL interattiva
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
  --help, -h              Show this message

Examples:
  ./install.sh                               # First install
  ./install.sh --groupid io.mycompany        # Custom GroupId
  ./install.sh --postgres                    # Add PostgreSQL
  ./install.sh --groupid io.mycompany --postgres
EOF
    exit 0
}

generate_env_file() {
    project_dir=$(basename "$PWD")
    cat > .env << EOF
# Java/Undertow + Svelte 5 project configuration
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

# Vite/Svelte (dev server with proxy)
VITE_PORT=5173
VITE_PORT_HOST=2350

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
# PostgreSQL
PGSQL_ENABLED=y
PGSQL_IMAGE=postgres:16
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

    sed "s|PROJECT_DIR_PLACEHOLDER|$project_dir|g" .env > .env.tmp && mv .env.tmp .env
}

if [ ! -f .env ]; then
    echo "Generating .env..."
    generate_env_file
    . ./.env
fi

if [ -z "$PROJECT_NAME" ]; then
    . ./.env
fi

# Variabili derivate
DEV_NETWORK="$PROJECT_NAME$DEV_NETWORK_SUFFIX"
DEV_CONTAINER="$PROJECT_NAME"
PGSQL_CONTAINER="$PROJECT_NAME-postgres"
PGSQL_VOLUME="$PROJECT_NAME-postgres-data"

# Parsing argomenti — estrae --groupid e ricostruisce $@
GROUP_ID="com.example"
_NEWARGS=""
while [ $# -gt 0 ]; do
    case "$1" in
        --groupid)
            [ -n "$2" ] || { echo "ERRORE: --groupid richiede un valore (es. io.mycompany)"; exit 1; }
            GROUP_ID="$2"
            shift 2
            ;;
        *)
            _NEWARGS="${_NEWARGS:+$_NEWARGS }$1"
            shift
            ;;
    esac
done
# shellcheck disable=SC2086
[ -n "$_NEWARGS" ] && set -- $_NEWARGS || set --

GROUP_DIR=$(echo "$GROUP_ID" | tr '.' '/')

# --help
if [ "$1" = "--help" ] || [ "$1" = "-h" ]; then
    show_help
fi

create_pgsql_container() {
    if ! docker network ls --format "{{.Name}}" | grep -q "^${DEV_NETWORK}$"; then
        echo "Creating Docker network..."
        docker network create "$DEV_NETWORK" >/dev/null 2>&1 || true
    fi

    if docker ps -a --format "{{.Names}}" | grep -q "^${PGSQL_CONTAINER}$"; then
        if docker ps --format "{{.Names}}" | grep -q "^${PGSQL_CONTAINER}$"; then
            return 0
        fi
        echo "Starting PostgreSQL container..."
        docker start "$PGSQL_CONTAINER" >/dev/null 2>&1 || {
            echo "ERROR: Failed to start PostgreSQL container '$PGSQL_CONTAINER'."
            return 1
        }
        return 0
    fi

    if ! docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "^${PGSQL_IMAGE}$"; then
        echo "Pulling PostgreSQL image..."
        docker pull "$PGSQL_IMAGE"
    fi

    echo "Creating PostgreSQL container..."
    docker run -d --name "$PGSQL_CONTAINER" --network "$DEV_NETWORK" \
        -e POSTGRES_USER="$PGSQL_ROOT_USER" \
        -e POSTGRES_PASSWORD="$PGSQL_ROOT_PASSWORD" \
        -e POSTGRES_DB="$PGSQL_NAME" \
        -p "$PGSQL_PORT_HOST:5432" \
        -v "$PGSQL_VOLUME:/var/lib/postgresql/data" \
        "$PGSQL_IMAGE" >/dev/null 2>&1 || {
            echo "ERROR: Failed to create PostgreSQL container '$PGSQL_CONTAINER'."
            return 1
        }
}

setup_pgsql_database() {
    sleep 3

    if ! docker exec "$DEV_CONTAINER" sh -c "command -v psql >/dev/null 2>&1"; then
        echo "Installing PostgreSQL client..."
        docker exec "$DEV_CONTAINER" sh -c "apt-get update && apt-get install -y postgresql-client"
    fi

    echo "Waiting for PostgreSQL to be ready..."
    for i in 1 2 3 4 5; do
        if docker exec "$DEV_CONTAINER" pg_isready -h"$PGSQL_CONTAINER" -U"$PGSQL_ROOT_USER" >/dev/null 2>&1; then
            break
        fi
        sleep 2
    done

    if ! docker exec "$DEV_CONTAINER" pg_isready -h"$PGSQL_CONTAINER" -U"$PGSQL_ROOT_USER" >/dev/null 2>&1; then
        echo "ERROR: PostgreSQL not reachable after 5 attempts."
        echo "Possible cause: container '$PGSQL_CONTAINER' not started or network '$DEV_NETWORK' not configured."
        echo "Check with: docker ps -a --filter name=$PGSQL_CONTAINER"
        return 1
    fi

    echo "Configuring database..."
    docker exec "$DEV_CONTAINER" sh -c "PGPASSWORD=\"$PGSQL_ROOT_PASSWORD\" psql -h\"$PGSQL_CONTAINER\" -U\"$PGSQL_ROOT_USER\" -d postgres \
        -c \"DO \\\$\\\$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '$PGSQL_USER') THEN CREATE ROLE \\\"$PGSQL_USER\\\" WITH LOGIN PASSWORD '$PGSQL_PASSWORD'; END IF; END \\\$\\\$;\"" 2>/dev/null || true

    docker exec "$DEV_CONTAINER" sh -c "PGPASSWORD=\"$PGSQL_ROOT_PASSWORD\" psql -h\"$PGSQL_CONTAINER\" -U\"$PGSQL_ROOT_USER\" -d postgres \
        -c \"SELECT 1 FROM pg_database WHERE datname = '$PGSQL_NAME'\" | grep -q 1 || \
         PGPASSWORD=\"$PGSQL_ROOT_PASSWORD\" psql -h\"$PGSQL_CONTAINER\" -U\"$PGSQL_ROOT_USER\" -d postgres \
        -c \"CREATE DATABASE \\\"$PGSQL_NAME\\\";\"" 2>/dev/null || true

    docker exec "$DEV_CONTAINER" sh -c "PGPASSWORD=\"$PGSQL_ROOT_PASSWORD\" psql -h\"$PGSQL_CONTAINER\" -U\"$PGSQL_ROOT_USER\" -d \"$PGSQL_NAME\" \
        -c \"GRANT ALL ON SCHEMA public TO \\\"$PGSQL_USER\\\";\"" 2>/dev/null || true
}

# --postgres
if [ "$1" = "--postgres" ]; then
    if [ "$PGSQL_ENABLED" != "y" ]; then
        echo "ERROR: PostgreSQL is not enabled in .env"
        echo "Set PGSQL_ENABLED=y in .env to continue"
        exit 1
    fi

    if ! docker ps --format '{{.Names}}' | grep -q "^$DEV_CONTAINER$"; then
        echo "ERROR: Development container '$DEV_CONTAINER' is not running."
        echo "Run './install.sh' first to start the development container."
        exit 1
    fi

    create_pgsql_container
    setup_pgsql_database

    echo "Done"
    exit 0
fi

# Creazione container di sviluppo
if [ "$1" = "--dev" ] || [ -z "$1" ]; then

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
            -w /workspace \
            -p "$API_PORT_HOST:$API_PORT" \
            -p "$VITE_PORT_HOST:$VITE_PORT" \
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
frontend/node_modules/

# Vite build output (generato da 'npm run build', non tracciato)
src/main/resources/static/

# Docker temporaneo
docker/

# Environment (contiene credenziali)
.env

# Log (directory di log del container, non tracciata)
logs/
GITIGNORE
    if [ ! -f pom.xml ]; then
        echo "Scaffolding project..."

        # --- Java / Undertow ---
        mkdir -p "src/main/java/$GROUP_DIR"
        mkdir -p src/main/resources/static
        mkdir -p src/main/resources/db/migration

        # Copia libreria util (dev.jms.util) nel progetto
        cp -r "$INSTALLER_DIR/lib/." src/main/java/
        rm -rf "$INSTALLER_DIR/lib"

        cat > pom.xml << 'POMXML_TEMPLATE'
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
             http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.example</groupId>
    <artifactId>service</artifactId>
    <version>1.0-SNAPSHOT</version>
    <packaging>jar</packaging>

    <properties>
        <maven.compiler.source>21</maven.compiler.source>
        <maven.compiler.target>21</maven.compiler.target>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <undertow.version>2.3.12.Final</undertow.version>
        <hikari.version>5.1.0</hikari.version>
        <postgresql.version>42.7.3</postgresql.version>
        <flyway.version>10.20.0</flyway.version>
        <jackson.version>2.17.2</jackson.version>
    </properties>

    <dependencies>
        <dependency>
            <groupId>io.undertow</groupId>
            <artifactId>undertow-core</artifactId>
            <version>${undertow.version}</version>
        </dependency>
        <dependency>
            <groupId>com.zaxxer</groupId>
            <artifactId>HikariCP</artifactId>
            <version>${hikari.version}</version>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>${postgresql.version}</version>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-core</artifactId>
            <version>${flyway.version}</version>
        </dependency>
        <dependency>
            <groupId>org.flywaydb</groupId>
            <artifactId>flyway-database-postgresql</artifactId>
            <version>${flyway.version}</version>
        </dependency>
        <dependency>
            <groupId>com.fasterxml.jackson.core</groupId>
            <artifactId>jackson-databind</artifactId>
            <version>${jackson.version}</version>
        </dependency>
        <dependency>
            <groupId>com.auth0</groupId>
            <artifactId>java-jwt</artifactId>
            <version>4.4.0</version>
        </dependency>
        <dependency>
            <groupId>org.apache.poi</groupId>
            <artifactId>poi-ooxml</artifactId>
            <version>5.2.5</version>
        </dependency>
        <!-- Logging: SLF4J + Logback (rotazione log) -->
        <dependency>
            <groupId>ch.qos.logback</groupId>
            <artifactId>logback-classic</artifactId>
            <version>1.2.13</version>
        </dependency>
        <!-- Bridge Log4j2 → SLF4J (silenzia i messaggi di POI) -->
        <dependency>
            <groupId>org.apache.logging.log4j</groupId>
            <artifactId>log4j-to-slf4j</artifactId>
            <version>2.20.0</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-shade-plugin</artifactId>
                <version>3.5.2</version>
                <executions>
                    <execution>
                        <phase>package</phase>
                        <goals>
                            <goal>shade</goal>
                        </goals>
                        <configuration>
                            <finalName>service</finalName>
                            <transformers>
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
                                    <mainClass>com.example.App</mainClass>
                                </transformer>
                                <!-- Merges META-INF/services files (required for Flyway 10 ServiceLoader) -->
                                <transformer implementation="org.apache.maven.plugins.shade.resource.ServicesResourceTransformer"/>
                            </transformers>
                            <filters>
                                <filter>
                                    <artifact>*:*</artifact>
                                    <excludes>
                                        <exclude>META-INF/*.SF</exclude>
                                        <exclude>META-INF/*.DSA</exclude>
                                        <exclude>META-INF/*.RSA</exclude>
                                    </excludes>
                                </filter>
                            </filters>
                        </configuration>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
POMXML_TEMPLATE
        sed -i '' "s|com\.example|$GROUP_ID|g" pom.xml

        cat > "src/main/java/$GROUP_DIR/Config.java" << 'CONFIGJAVA'
package com.example;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {

  private final Properties props = new Properties();

  public Config() {
    try (InputStream in = Config.class.getClassLoader()
        .getResourceAsStream("application.properties")) {
      if (in != null) {
        props.load(in);
      } else {
        System.err.println("[warn] application.properties non trovato nel classpath");
      }
    } catch (IOException e) {
      System.err.println("[warn] Errore lettura application.properties: " + e.getMessage());
    }
  }

  public String get(String key, String defaultValue) {
    String envKey = key.toUpperCase().replace('.', '_');
    String envVal = System.getenv(envKey);
    if (envVal != null && !envVal.isBlank()) return envVal;
    return props.getProperty(key, defaultValue);
  }

  public int getInt(String key, int defaultValue) {
    try {
      return Integer.parseInt(get(key, String.valueOf(defaultValue)));
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }
}
CONFIGJAVA
        sed -i '' "s|com\.example|$GROUP_ID|g" "src/main/java/$GROUP_DIR/Config.java"

        cat > "src/main/java/$GROUP_DIR/App.java" << 'APPJAVA'
package com.example;

import com.example.handler.LoginHandler;
import com.example.handler.LogoutHandler;
import com.example.handler.RefreshHandler;
import com.example.handler.SessionHandler;
import dev.jms.util.Auth;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import dev.jms.util.Handler;
import dev.jms.util.HandlerAdapter;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathTemplateHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.Headers;
import org.flywaydb.core.Flyway;
import java.io.InputStream;

public class App
{
  private static HikariDataSource dataSource;

  public static void main(String[] args)
  {
    Config config;
    int port;
    ResourceHandler staticHandler;
    PathTemplateHandler paths;
    Undertow server;

    config = new Config();
    port = config.getInt("server.port", 8080);

    initDataSource(config);
    runMigrations();

    Auth.init(
      config.get("jwt.secret", "dev-secret-change-in-production"),
      config.getInt("jwt.access.expiry.seconds", 900)
    );

    staticHandler = new ResourceHandler(
      new ClassPathResourceManager(App.class.getClassLoader(), "static")
    );

    paths = new PathTemplateHandler(staticHandler)
      .add("/",     redirect("/home"))
      .add("/home", page("module/home/main.html"))
      .add("/auth", page("module/auth/main.html"))
      .add("/api/auth/login",   route(new LoginHandler(),   dataSource))
      .add("/api/auth/session", route(new SessionHandler(), dataSource))
      .add("/api/auth/logout",  route(new LogoutHandler(),  dataSource))
      .add("/api/auth/refresh", route(new RefreshHandler(), dataSource));

    // Aggiungere qui i propri handler:
    // .add("/api/users",      route(new UserHandler(), dataSource))
    // .add("/api/users/{id}", route(new UserHandler(), dataSource))

    server = Undertow.builder()
      .addHttpListener(port, "0.0.0.0")
      .setHandler(paths)
      .build();

    server.start();
    System.out.println("[info] Server in ascolto sulla porta " + port);
  }

  private static HttpHandler page(String filename)
  {
    return exchange -> {
      InputStream in;
      byte[] bytes;
      in = App.class.getClassLoader().getResourceAsStream("static/" + filename);
      if (in == null) {
        exchange.setStatusCode(404);
        exchange.endExchange();
      } else {
        bytes = in.readAllBytes();
        in.close();
        exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/html; charset=UTF-8");
        exchange.getResponseSender().send(new String(bytes, java.nio.charset.StandardCharsets.UTF_8));
      }
    };
  }

  private static HttpHandler redirect(String location)
  {
    return exchange -> {
      exchange.setStatusCode(302);
      exchange.getResponseHeaders().put(Headers.LOCATION, location);
      exchange.endExchange();
    };
  }

  private static HandlerAdapter route(Handler handler)
  {
    return new HandlerAdapter(handler);
  }

  private static HandlerAdapter route(Handler handler, DataSource dataSource)
  {
    return new HandlerAdapter(handler, dataSource);
  }

  private static void initDataSource(Config config)
  {
    String host;
    String dbPort;
    String name;
    String user;
    String password;
    int poolSize;

    host = config.get("db.host", "");
    dbPort = config.get("db.port", "5432");
    name = config.get("db.name", "");
    user = config.get("db.user", "");
    password = config.get("db.password", "");
    poolSize = config.getInt("db.pool.size", 10);

    if (host.isBlank() || name.isBlank() || user.isBlank()) {
      System.out.println("[info] Database non configurato, pool non inizializzato");
    } else {
      try {
        HikariConfig hc;
        hc = new HikariConfig();
        hc.setJdbcUrl("jdbc:postgresql://" + host + ":" + dbPort + "/" + name);
        hc.setUsername(user);
        hc.setPassword(password);
        hc.setMaximumPoolSize(poolSize);
        hc.setInitializationFailTimeout(-1);
        dataSource = new HikariDataSource(hc);
        System.out.println("[info] Pool database inizializzato (" + host + ":" + dbPort + "/" + name + ")");
      } catch (Exception e) {
        System.err.println("[warn] Inizializzazione pool fallita: " + e.getMessage());
      }
    }
  }

  private static void runMigrations()
  {
    if (dataSource == null) {
      System.out.println("[info] Flyway: nessun DataSource, migrazione saltata");
    } else {
      try {
        Flyway flyway;
        int applied;
        flyway = Flyway.configure()
          .dataSource(dataSource)
          .locations("classpath:db/migration")
          .load();
        applied = flyway.migrate().migrationsExecuted;
        System.out.println("[info] Flyway: " + applied + " migrazione/i applicata/e");
      } catch (Exception e) {
        System.err.println("[warn] Flyway migration fallita: " + e.getMessage());
      }
    }
  }
}
APPJAVA
        sed -i '' "s|com\.example|$GROUP_ID|g" "src/main/java/$GROUP_DIR/App.java"

        mkdir -p "src/main/java/$GROUP_DIR/handler"

        cat > "src/main/java/$GROUP_DIR/handler/LoginHandler.java" << 'LOGINJAVA'
package com.example.handler;

import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Json;
import dev.jms.util.Log;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

public class LoginHandler implements Handler
{
  private static final Log log = Log.get(LoginHandler.class);

  @Override
  @SuppressWarnings("unchecked")
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    HashMap<String, Object> body;
    String username;
    String password;
    ArrayList<HashMap<String, Object>> rows;

    body = Json.decode(req.getBody(), HashMap.class);
    username = (String) body.get("username");
    password = (String) body.get("password");

    if (username == null || password == null || username.isBlank() || password.isBlank()) {
      log.warn("Login fallito: credenziali mancanti");
      res.status(200).contentType("application/json").err(true).log("Credenziali mancanti").out(null).send();
    } else {
      rows = db.select("SELECT id, username, password_hash FROM users WHERE username = ?", username);
      if (rows.isEmpty() || !Auth.verifyPassword(password, (String) rows.get(0).get("password_hash"))) {
        log.warn("Login fallito: credenziali non valide per utente '{}'", username);
        res.status(200).contentType("application/json").err(true).log("Credenziali non valide").out(null).send();
      } else {
        HashMap<String, Object> user;
        int userId;
        String uname;
        Auth auth;
        String accessToken;
        String refreshToken;
        LocalDateTime expiresAt;
        HashMap<String, Object> out;

        user = rows.get(0);
        userId = DB.toInteger(user.get("id"));
        uname = (String) user.get("username");
        auth = Auth.get();
        accessToken = auth.createAccessToken(userId, uname);
        refreshToken = Auth.generateRefreshToken();
        expiresAt = LocalDateTime.now().plusSeconds(Auth.REFRESH_EXPIRY);

        db.query("INSERT INTO refresh_tokens (token, user_id, expires_at) VALUES (?, ?, ?)",
          refreshToken, userId, DB.toSqlTimestamp(expiresAt));

        out = new HashMap<>();
        out.put("id", userId);
        out.put("username", uname);

        res.status(200)
           .contentType("application/json")
           .cookie("access_token", accessToken, 15 * 60)
           .cookie("refresh_token", refreshToken, Auth.REFRESH_EXPIRY)
           .err(false).log(null).out(out)
           .send();
      }
    }
  }
}
LOGINJAVA
        sed -i '' "s|com\.example|$GROUP_ID|g" "src/main/java/$GROUP_DIR/handler/LoginHandler.java"

        cat > "src/main/java/$GROUP_DIR/handler/SessionHandler.java" << 'SESSIONJAVA'
package com.example.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;

import java.util.HashMap;

public class SessionHandler implements Handler
{
  private static final Log log = Log.get(SessionHandler.class);

  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    DecodedJWT jwt;

    token = req.getCookie("access_token");

    if (token == null) {
      log.warn("Sessione rifiutata: cookie access_token assente");
      res.status(200).contentType("application/json").err(true).log("Non autenticato").out(null).send();
    } else {
      jwt = null;
      try {
        jwt = Auth.get().verifyAccessToken(token);
      } catch (JWTVerificationException e) {
        log.warn("Sessione rifiutata: token non valido o scaduto");
      }

      if (jwt != null) {
        HashMap<String, Object> out;
        out = new HashMap<>();
        out.put("id", jwt.getSubject());
        out.put("username", jwt.getClaim("username").asString());
        res.status(200).contentType("application/json").err(false).log(null).out(out).send();
      } else {
        res.status(200).contentType("application/json").err(true).log("Token non valido o scaduto").out(null).send();
      }
    }
  }
}
SESSIONJAVA
        sed -i '' "s|com\.example|$GROUP_ID|g" "src/main/java/$GROUP_DIR/handler/SessionHandler.java"

        cat > "src/main/java/$GROUP_DIR/handler/LogoutHandler.java" << 'LOGOUTJAVA'
package com.example.handler;

import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;

public class LogoutHandler implements Handler
{
  @Override
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String refreshToken;

    refreshToken = req.getCookie("refresh_token");

    if (refreshToken != null) {
      db.query("DELETE FROM refresh_tokens WHERE token = ?", refreshToken);
    }

    res.status(200)
       .contentType("application/json")
       .cookie("access_token", "", 0)
       .cookie("refresh_token", "", 0)
       .err(false).log(null).out(null)
       .send();
  }
}
LOGOUTJAVA
        sed -i '' "s|com\.example|$GROUP_ID|g" "src/main/java/$GROUP_DIR/handler/LogoutHandler.java"

        cat > "src/main/java/$GROUP_DIR/handler/RefreshHandler.java" << 'REFRESHJAVA'
package com.example.handler;

import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;

public class RefreshHandler implements Handler
{
  private static final Log log = Log.get(RefreshHandler.class);

  @Override
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String refreshToken;
    ArrayList<HashMap<String, Object>> rows;

    refreshToken = req.getCookie("refresh_token");

    if (refreshToken == null) {
      log.warn("Refresh rifiutato: cookie refresh_token assente");
      res.status(200).contentType("application/json").err(true).log("Non autenticato").out(null).send();
    } else {
      rows = db.select(
        "SELECT rt.token, u.id, u.username FROM refresh_tokens rt " +
        "JOIN users u ON u.id = rt.user_id " +
        "WHERE rt.token = ? AND rt.expires_at > NOW()",
        refreshToken
      );

      if (rows.isEmpty()) {
        log.warn("Refresh rifiutato: token non trovato o scaduto");
        res.status(200).contentType("application/json").err(true).log("Token non valido o scaduto").out(null).send();
      } else {
        HashMap<String, Object> row;
        int userId;
        String username;
        Auth auth;
        String newRefreshToken;
        LocalDateTime expiresAt;

        row = rows.get(0);
        userId = DB.toInteger(row.get("id"));
        username = (String) row.get("username");
        auth = Auth.get();
        newRefreshToken = Auth.generateRefreshToken();
        expiresAt = LocalDateTime.now().plusSeconds(Auth.REFRESH_EXPIRY);

        db.query("DELETE FROM refresh_tokens WHERE token = ?", refreshToken);
        db.query("INSERT INTO refresh_tokens (token, user_id, expires_at) VALUES (?, ?, ?)",
          newRefreshToken, userId, DB.toSqlTimestamp(expiresAt));

        res.status(200)
           .contentType("application/json")
           .cookie("access_token", auth.createAccessToken(userId, username), 15 * 60)
           .cookie("refresh_token", newRefreshToken, Auth.REFRESH_EXPIRY)
           .err(false).log(null).out(null)
           .send();
      }
    }
  }
}
REFRESHJAVA
        sed -i '' "s|com\.example|$GROUP_ID|g" "src/main/java/$GROUP_DIR/handler/RefreshHandler.java"

        # application.properties — generato con i valori del .env corrente
        # L'utente può modificarlo manualmente; le variabili d'ambiente hanno precedenza
        cat > src/main/resources/application.properties << EOF
# ============================================================
# Configurazione applicazione — modificare manualmente se necessario
# Le variabili d'ambiente hanno precedenza (es. DB_HOST sovrascrive db.host)
# ============================================================

# Server
server.port=${API_PORT:-8080}

# Database (PostgreSQL)
# In sviluppo: db.host è il nome del container sulla rete Docker
# In produzione: sostituire con hostname o IP del server PostgreSQL
db.host=${PGSQL_CONTAINER}
db.port=${PGSQL_PORT:-5432}
db.name=${PGSQL_NAME}
db.user=${PGSQL_USER}
db.password=${PGSQL_PASSWORD}
db.pool.size=10

# JWT
# jwt.secret deve essere una stringa lunga e casuale — cambiarla in produzione
jwt.secret=dev-secret-change-in-production
jwt.access.expiry.seconds=900
EOF

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

        # Migrazione iniziale Flyway
        MIGRATION_TS=$(date '+%Y%m%d_%H%M%S')
        cat > "src/main/resources/db/migration/V${MIGRATION_TS}__auth.sql" << 'AUTHSQL'
-- Tabella utenti. password_hash contiene "salt:hash" generato con PBKDF2WithHmacSHA256.
CREATE TABLE users (
  id            SERIAL PRIMARY KEY,
  username      VARCHAR(100) UNIQUE NOT NULL,
  password_hash VARCHAR(255)        NOT NULL,
  created_at    TIMESTAMP           NOT NULL DEFAULT NOW()
);

-- Refresh token opachi conservati nel DB per consentire la revoca.
-- Eliminati in cascata se l'utente viene rimosso.
CREATE TABLE refresh_tokens (
  token      VARCHAR(128) PRIMARY KEY,
  user_id    INTEGER      NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  expires_at TIMESTAMP    NOT NULL,
  created_at TIMESTAMP    NOT NULL DEFAULT NOW()
);
AUTHSQL

        # --- Frontend (Vite + Vanilla JS) ---
        mkdir -p frontend/src

        cat > frontend/package.json << 'PACKAGEJSON'
{
  "name": "frontend",
  "private": true,
  "version": "0.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite --host 0.0.0.0",
    "build": "vite build",
    "preview": "vite preview"
  },
  "devDependencies": {
    "vite": "^6.0.0"
  }
}
PACKAGEJSON

        cat > frontend/vite.config.js << 'VITECONFIG'
import { defineConfig } from 'vite'

export default defineConfig({
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
    proxy: {
      '/api': 'http://localhost:8080'
    }
  }
})
VITECONFIG

        cat > frontend/index.html << 'INDEXHTML'
<!doctype html>
<html lang="it">
  <head>
    <meta charset="UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>App</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.js"></script>
  </body>
</html>
INDEXHTML

        cat > frontend/src/main.js << 'MAINJS'
// Entry point
MAINJS

    fi

    echo "Installing npm dependencies..."
    docker exec "$DEV_CONTAINER" sh -c "cd /workspace/frontend && npm install"

    echo "Setting up cmd tool..."
    mkdir -p bin
    cp "$INSTALLER_DIR/cmd" bin/cmd
    sed -i '' "s|com\.example|$GROUP_ID|g" bin/cmd
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
    exit 0
fi
