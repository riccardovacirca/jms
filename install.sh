#!/bin/sh
set -e

# Mostra help
show_help() {
    cat << EOF
Uso: ./install.sh [OPZIONE]

Opzioni:
  (nessuna)               Crea ambiente di sviluppo completo
  --groupid <id>          GroupId Maven (default: com.example)
  --postgres              Installa container PostgreSQL
  --help, -h              Mostra questo messaggio

Esempi:
  ./install.sh                               # Prima installazione
  ./install.sh --groupid io.mycompany        # GroupId personalizzato
  ./install.sh --postgres                    # Aggiungi PostgreSQL
  ./install.sh --groupid io.mycompany --postgres
EOF
    exit 0
}

# Genera .env se non esiste
generate_env_file() {
    project_dir=$(basename "$PWD")
    cat > .env << EOF
# Configurazione Progetto Java/Undertow + Svelte 5
# Generato automaticamente da install.sh

# ========================================
# Configurazione Comune
# ========================================
PROJECT_NAME=PROJECT_DIR_PLACEHOLDER
JAVA_VERSION=21

# ========================================
# Configurazione Sviluppo
# ========================================
DEV_NETWORK_SUFFIX=-net
DEV_IMAGE=ubuntu:24.04

# Undertow (API)
API_PORT=8080
API_PORT_HOST=2310

# Vite/Svelte (dev server con proxy)
VITE_PORT=5173
VITE_PORT_HOST=2350

# ========================================
# Configurazione Release
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
# Database Containers
# ========================================
# PostgreSQL
PGSQL_ENABLED=n
PGSQL_IMAGE=postgres:16
PGSQL_PORT=5432
PGSQL_PORT_HOST=2340
PGSQL_ROOT_USER=postgres
PGSQL_ROOT_PASSWORD=postgres
PGSQL_NAME=PROJECT_DIR_PLACEHOLDERdb
PGSQL_USER=PROJECT_DIR_PLACEHOLDERuser
PGSQL_PASSWORD=PROJECT_DIR_PLACEHOLDERpass

# ========================================
# Git
# ========================================
GIT_USER=
GIT_EMAIL=
GIT_TOKEN=
EOF

    sed "s|PROJECT_DIR_PLACEHOLDER|$project_dir|g" .env > .env.tmp && mv .env.tmp .env
    echo "File .env generato con configurazione di default"
}

# Genera o carica .env
if [ ! -f .env ]; then
    echo "File .env non trovato, genero configurazione di default..."
    generate_env_file
    echo ""
    . ./.env
fi

if [ -z "$PROJECT_NAME" ]; then
    . ./.env
    echo "Configurazione caricata da .env"
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

# Crea container PostgreSQL
create_pgsql_container() {
    if ! docker network ls --format "{{.Name}}" | grep -q "^${DEV_NETWORK}$"; then
        docker network create "$DEV_NETWORK" >/dev/null 2>&1 || true
    fi

    if docker ps -a --format "{{.Names}}" | grep -q "^${PGSQL_CONTAINER}$"; then
        if docker ps --format "{{.Names}}" | grep -q "^${PGSQL_CONTAINER}$"; then
            echo "PostgreSQL container già in esecuzione."
            return 0
        fi
        docker start "$PGSQL_CONTAINER" >/dev/null 2>&1 || {
            echo "Errore nell'avvio del container PostgreSQL."
            return 1
        }
        echo "Container PostgreSQL avviato."
        return 0
    fi

    if ! docker images --format "{{.Repository}}:{{.Tag}}" | grep -q "^${PGSQL_IMAGE}$"; then
        echo "Download immagine PostgreSQL..."
        docker pull "$PGSQL_IMAGE" >/dev/null 2>&1
    fi

    echo "Creazione container PostgreSQL..."
    docker run -d --name "$PGSQL_CONTAINER" --network "$DEV_NETWORK" \
        -e POSTGRES_USER="$PGSQL_ROOT_USER" \
        -e POSTGRES_PASSWORD="$PGSQL_ROOT_PASSWORD" \
        -e POSTGRES_DB="$PGSQL_NAME" \
        -p "$PGSQL_PORT_HOST:5432" \
        -v "$PGSQL_VOLUME:/var/lib/postgresql/data" \
        "$PGSQL_IMAGE" >/dev/null 2>&1 || {
            echo "Errore nella creazione del container PostgreSQL."
            return 1
        }

    echo "Container PostgreSQL creato e avviato."
    echo "  Host: localhost:$PGSQL_PORT_HOST"
    echo "  Database: $PGSQL_NAME"
    echo "  User: $PGSQL_ROOT_USER / $PGSQL_ROOT_PASSWORD"
}

# Setup database PostgreSQL
setup_pgsql_database() {
    echo "  Attesa disponibilità PostgreSQL..."
    sleep 3

    if ! docker exec "$DEV_CONTAINER" sh -c "command -v psql >/dev/null 2>&1"; then
        echo "  Installazione client PostgreSQL nel container dev..."
        docker exec "$DEV_CONTAINER" sh -c "apt-get update -qq && apt-get install -y -qq postgresql-client >/dev/null 2>&1"
    fi

    echo "  Verifica connessione PostgreSQL..."
    for i in 1 2 3 4 5; do
        if docker exec "$DEV_CONTAINER" pg_isready -h"$PGSQL_CONTAINER" -U"$PGSQL_ROOT_USER" >/dev/null 2>&1; then
            echo "  PostgreSQL pronto"
            break
        fi
        echo "  Tentativo $i/5..."
        sleep 2
    done

    if ! docker exec "$DEV_CONTAINER" pg_isready -h"$PGSQL_CONTAINER" -U"$PGSQL_ROOT_USER" >/dev/null 2>&1; then
        echo "  [WARN] PostgreSQL non raggiungibile"
        return 1
    fi

    echo "  Creazione utente applicazione..."
    docker exec "$DEV_CONTAINER" sh -c "PGPASSWORD=\"$PGSQL_ROOT_PASSWORD\" psql -h\"$PGSQL_CONTAINER\" -U\"$PGSQL_ROOT_USER\" -d postgres \
        -c \"DO \\\$\\\$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '$PGSQL_USER') THEN CREATE ROLE \\\"$PGSQL_USER\\\" WITH LOGIN PASSWORD '$PGSQL_PASSWORD'; END IF; END \\\$\\\$;\"" 2>/dev/null || true

    echo "  Creazione database applicazione..."
    docker exec "$DEV_CONTAINER" sh -c "PGPASSWORD=\"$PGSQL_ROOT_PASSWORD\" psql -h\"$PGSQL_CONTAINER\" -U\"$PGSQL_ROOT_USER\" -d postgres \
        -c \"SELECT 1 FROM pg_database WHERE datname = '$PGSQL_NAME'\" | grep -q 1 || \
         PGPASSWORD=\"$PGSQL_ROOT_PASSWORD\" psql -h\"$PGSQL_CONTAINER\" -U\"$PGSQL_ROOT_USER\" -d postgres \
        -c \"CREATE DATABASE \\\"$PGSQL_NAME\\\";\"" 2>/dev/null || true

    echo "  Configurazione permessi schema..."
    docker exec "$DEV_CONTAINER" sh -c "PGPASSWORD=\"$PGSQL_ROOT_PASSWORD\" psql -h\"$PGSQL_CONTAINER\" -U\"$PGSQL_ROOT_USER\" -d \"$PGSQL_NAME\" \
        -c \"GRANT ALL ON SCHEMA public TO \\\"$PGSQL_USER\\\";\"" 2>/dev/null || true

    echo "  Setup PostgreSQL completato"
}

# --postgres
if [ "$1" = "--postgres" ]; then
    if [ "$PGSQL_ENABLED" != "y" ]; then
        echo "ERRORE: PostgreSQL non è abilitato nel file .env"
        echo "Imposta PGSQL_ENABLED=y nel file .env per continuare"
        exit 1
    fi

    echo "Configurazione PostgreSQL..."

    if ! docker ps --format '{{.Names}}' | grep -q "^$DEV_CONTAINER$"; then
        echo "ERRORE: Container dev '$DEV_CONTAINER' non in esecuzione."
        echo "Esegui prima './install.sh' per avviare il container dev."
        exit 1
    fi

    create_pgsql_container
    setup_pgsql_database

    echo ""
    echo "=========================================="
    echo "PostgreSQL configurato e pronto!"
    echo "=========================================="
    echo "  Host: localhost:$PGSQL_PORT_HOST"
    echo "  Database: $PGSQL_NAME"
    echo "  User: $PGSQL_USER / $PGSQL_PASSWORD"
    echo ""
    exit 0
fi

# Creazione container di sviluppo
if [ "$1" = "--dev" ] || [ -z "$1" ]; then

    if docker ps -a --format '{{.Names}}' | grep -q "^$DEV_CONTAINER$"; then
        echo "Container '$DEV_CONTAINER' già esistente."
        if ! docker ps --format '{{.Names}}' | grep -q "^$DEV_CONTAINER$"; then
            echo "Avvio container..."
            docker start "$DEV_CONTAINER"
        else
            echo "Container già in esecuzione."
        fi
    else
        # Crea la rete se non esiste
        if ! docker network ls --format '{{.Name}}' | grep -q "^$DEV_NETWORK$"; then
            docker network create "$DEV_NETWORK"
            echo "Docker network '$DEV_NETWORK' creata."
        fi

        # Dockerfile.dev temporaneo
        echo "Creazione Dockerfile.dev..."
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
DOCKERFILE

        # Build immagine dev
        echo "Build immagine di sviluppo (richiede qualche minuto)..."
        docker build -t "$PROJECT_NAME-dev:latest" -f docker/Dockerfile.dev .

        # Avvia container di sviluppo
        echo "Creazione container di sviluppo..."
        docker run -it -d \
            --name "$DEV_CONTAINER" \
            -v "$PWD":/workspace \
            -w /workspace \
            -p "$API_PORT_HOST:$API_PORT" \
            -p "$VITE_PORT_HOST:$VITE_PORT" \
            --network "$DEV_NETWORK" \
            "$PROJECT_NAME-dev:latest" \
            tail -f /dev/null

        echo "Container '$DEV_CONTAINER' creato e avviato."

        # Rimuove il Dockerfile dopo la build (effimero)
        rm -f docker/Dockerfile.dev
        rmdir docker 2>/dev/null || true
    fi

    # .gitignore
    echo "Creazione .gitignore..."
    rm -f .gitignore
    cat > .gitignore << 'GITIGNORE'
# Build artifacts
target/
svelte/node_modules/

# Vite build output (generato da 'npm run build', non tracciato)
src/main/resources/static/

# Docker temporaneo
docker/

# Environment (contiene credenziali)
.env
GITIGNORE
    echo ".gitignore creato"

    # Struttura progetto se non esiste ancora
    if [ ! -f pom.xml ]; then
        echo "Creazione struttura progetto..."

        # --- Java / Undertow ---
        mkdir -p "src/main/java/$GROUP_DIR"
        mkdir -p src/main/resources/static
        mkdir -p src/main/resources/db/migration

        # Copia libreria util (dev.jms.util) nel progetto
        cp -r "$INSTALLER_DIR/lib/." src/main/java/

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
        sed -i "s|com\.example|$GROUP_ID|g" pom.xml

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
        sed -i "s|com\.example|$GROUP_ID|g" "src/main/java/$GROUP_DIR/Config.java"

        cat > "src/main/java/$GROUP_DIR/App.java" << 'APPJAVA'
package com.example;

import com.example.handler.DbTestHandler;
import com.example.handler.HelloHandler;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import org.flywaydb.core.Flyway;

public class App {

  private static HikariDataSource dataSource;

  public static void main(String[] args) {
    Config config = new Config();
    int port = config.getInt("server.port", 8080);

    initDataSource(config);
    runMigrations();

    // File statici bundled nel JAR (generati da 'cmd svelte build')
    ResourceHandler staticHandler = new ResourceHandler(
      new ClassPathResourceManager(App.class.getClassLoader(), "static")
    ).setWelcomeFiles("index.html");

    PathHandler paths = new PathHandler(staticHandler)
      .addPrefixPath("/api/hello", new HelloHandler())
      .addPrefixPath("/api/db/test", new DbTestHandler(dataSource));

    // Aggiungere qui i propri handler:
    // .addPrefixPath("/api/items", new ItemHandler(dataSource))

    Undertow server = Undertow.builder()
      .addHttpListener(port, "0.0.0.0")
      .setHandler(paths)
      .build();

    server.start();
    System.out.println("[info] Server in ascolto sulla porta " + port);
  }

  private static void initDataSource(Config config) {
    String host     = config.get("db.host", "");
    String dbPort   = config.get("db.port", "5432");
    String name     = config.get("db.name", "");
    String user     = config.get("db.user", "");
    String password = config.get("db.password", "");
    int    poolSize = config.getInt("db.pool.size", 10);

    if (host.isBlank() || name.isBlank() || user.isBlank()) {
      System.out.println("[info] Database non configurato, pool non inizializzato");
      return;
    }

    try {
      HikariConfig hc = new HikariConfig();
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

  private static void runMigrations() {
    if (dataSource == null) {
      System.out.println("[info] Flyway: nessun DataSource, migrazione saltata");
      return;
    }
    try {
      Flyway flyway = Flyway.configure()
        .dataSource(dataSource)
        .locations("classpath:db/migration")
        .load();
      int applied = flyway.migrate().migrationsExecuted;
      System.out.println("[info] Flyway: " + applied + " migrazione/i applicata/e");
    } catch (Exception e) {
      System.err.println("[warn] Flyway migration fallita: " + e.getMessage());
    }
  }
}
APPJAVA
        sed -i "s|com\.example|$GROUP_ID|g" "src/main/java/$GROUP_DIR/App.java"

        mkdir -p "src/main/java/$GROUP_DIR/handler"

        cat > "src/main/java/$GROUP_DIR/handler/HelloHandler.java" << 'HELLOJAVA'
package com.example.handler;

import dev.jms.util.Json;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.util.Map;

public class HelloHandler implements HttpHandler {

  @Override
  public void handleRequest(HttpServerExchange exchange) {
    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
    exchange.getResponseSender().send(
      Json.encode(Map.of("status", "ok", "message", "Hello from Undertow!"))
    );
  }
}
HELLOJAVA
        sed -i "s|com\.example|$GROUP_ID|g" "src/main/java/$GROUP_DIR/handler/HelloHandler.java"

        cat > "src/main/java/$GROUP_DIR/handler/DbTestHandler.java" << 'DBTESTJAVA'
package com.example.handler;

import com.zaxxer.hikari.HikariDataSource;
import dev.jms.util.Json;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import java.sql.Connection;
import java.util.Map;

public class DbTestHandler implements HttpHandler {

  private final HikariDataSource dataSource;

  public DbTestHandler(HikariDataSource dataSource) {
    this.dataSource = dataSource;
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) {
    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
    if (dataSource == null) {
      exchange.setStatusCode(503);
      exchange.getResponseSender().send(
        Json.encode(Map.of("status", "error", "message", "DataSource non inizializzato"))
      );
      return;
    }
    try (Connection conn = dataSource.getConnection()) {
      String version = conn.getMetaData().getDatabaseProductVersion();
      exchange.getResponseSender().send(
        Json.encode(Map.of("status", "ok", "message", "Connessione riuscita", "db", version))
      );
    } catch (Exception e) {
      exchange.setStatusCode(500);
      exchange.getResponseSender().send(
        Json.encode(Map.of("status", "error", "message", e.getMessage()))
      );
    }
  }
}
DBTESTJAVA
        sed -i "s|com\.example|$GROUP_ID|g" "src/main/java/$GROUP_DIR/handler/DbTestHandler.java"

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
EOF

        # Migrazione iniziale Flyway
        MIGRATION_TS=$(date '+%Y%m%d_%H%M%S')
        cat > "src/main/resources/db/migration/V${MIGRATION_TS}__init.sql" << 'INITSQL'
-- Migrazione iniziale
-- Aggiungere qui la struttura del database (CREATE TABLE, ecc.)
INITSQL

        echo "Struttura Java creata"

        # --- Svelte 5 ---
        mkdir -p svelte/src

        cat > svelte/package.json << 'PACKAGEJSON'
{
  "name": "svelte",
  "private": true,
  "version": "0.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite --host 0.0.0.0",
    "build": "vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "bootstrap": "^5.3.0"
  },
  "devDependencies": {
    "@sveltejs/vite-plugin-svelte": "^5.0.0",
    "svelte": "^5.0.0",
    "vite": "^6.0.0"
  }
}
PACKAGEJSON

        cat > svelte/svelte.config.js << 'SVELTECONFIG'
import { vitePreprocess } from '@sveltejs/vite-plugin-svelte'

export default {
  preprocess: vitePreprocess()
}
SVELTECONFIG

        cat > svelte/vite.config.js << 'VITECONFIG'
import { defineConfig } from 'vite'
import { svelte } from '@sveltejs/vite-plugin-svelte'

export default defineConfig({
  plugins: [svelte()],
  build: {
    // Build output goes into the Java resources folder
    // so Undertow can serve it from the classpath in production
    outDir: '../src/main/resources/static',
    emptyOutDir: true
  },
  server: {
    port: 5173,
    host: '0.0.0.0',
    proxy: {
      // In dev: proxy /api/* to Undertow — no CORS needed
      '/api': 'http://localhost:8080'
    }
  }
})
VITECONFIG

        cat > svelte/index.html << 'INDEXHTML'
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

        # Crea directory moduli
        mkdir -p svelte/src/util
        mkdir -p svelte/src/module/header
        mkdir -p svelte/src/module/sidebar
        mkdir -p svelte/src/module/auth
        mkdir -p svelte/src/module/home

        cat > svelte/src/main.js << 'MAINJS'
import { mount } from 'svelte'
import Main from './Main.svelte'

const app = mount(Main, {
  target: document.getElementById('app')
})

export default app
MAINJS

        cat > svelte/src/store.js << 'STOREJS'
import { writable, derived, get } from 'svelte/store'

export const auth = writable({ isAuthenticated: false, user: null })

// Moduli privati — aggiungere il nome qui quando si crea un nuovo modulo privato
const privateModules = [
  // 'dashboard',
]

const intendedDestination = writable(null)

function createCurrentModule() {
  const { subscribe, set } = writable({ name: 'home' })
  return {
    subscribe,
    navigate(name) {
      const a = get(auth)
      if (privateModules.includes(name) && !a.isAuthenticated) {
        intendedDestination.set(name)
        set({ name: 'auth' })
      } else {
        set({ name })
        history.pushState({ module: name }, '', '/' + name)
      }
    },
    initFromURL() {
      const name = window.location.pathname.replace('/', '') || 'home'
      set({ name })
    }
  }
}

export const currentModule     = createCurrentModule()
export const currentModuleName = derived(currentModule, $m => $m.name)

export function navigateAfterLogin() {
  const dest = get(intendedDestination)
  intendedDestination.set(null)
  currentModule.navigate(dest || 'home')
}

export async function checkAuth() {
  try {
    const res = await fetch('/api/auth/session')
    if (res.ok) {
      const data = await res.json()
      auth.set({ isAuthenticated: true, user: data.user })
    }
  } catch (_) {}
}

export async function logout() {
  try { await fetch('/api/auth/logout', { method: 'POST' }) } finally {
    auth.set({ isAuthenticated: false, user: null })
    currentModule.navigate('home')
  }
}
STOREJS

        cat > svelte/src/Main.svelte << 'MAINSVELTE'
<script>
  import { onMount } from 'svelte'
  import { auth, currentModule, currentModuleName, checkAuth } from './store.js'
  import HomeLayout    from './module/home/Layout.svelte'
  import AuthLayout    from './module/auth/Layout.svelte'
  import SidebarLayout from './module/sidebar/Layout.svelte'
  import HeaderLayout  from './module/header/Layout.svelte'
  import 'bootstrap/dist/css/bootstrap.min.css'

  // Registro moduli privati — importare e registrare qui ogni nuovo modulo privato
  // import DashboardLayout from './module/dashboard/Layout.svelte'
  const privateRegistry = {
    // dashboard: DashboardLayout,
  }

  let loading = $state(true)
  onMount(async () => { await checkAuth(); loading = false })

  const area = $derived(
    $currentModuleName === 'home' ? 'home'
    : $currentModuleName === 'auth' ? 'auth'
    : $auth.isAuthenticated ? 'private'
    : 'auth'
  )

  const privateComponent = $derived(privateRegistry[$currentModuleName] ?? null)
</script>

{#if loading}
  <div class="d-flex justify-content-center align-items-center min-vh-100">
    <div class="spinner-border text-primary" role="status"></div>
  </div>

{:else if area === 'home'}
  <HomeLayout />

{:else if area === 'auth'}
  <AuthLayout />

{:else}
  <div class="d-flex" style="min-height:100vh">
    <SidebarLayout />
    <div class="flex-grow-1 d-flex flex-column">
      <HeaderLayout />
      <main class="p-4 flex-grow-1 bg-light">
        {#if privateComponent}
          {@const PrivateComponent = privateComponent}
          <PrivateComponent />
        {:else}
          <p class="text-muted">Modulo non trovato.</p>
        {/if}
      </main>
    </div>
  </div>
{/if}
MAINSVELTE

        cat > svelte/src/util/fetchWithRefresh.js << 'FETCHWITHREFRESH'
let refreshing = false
let refreshPromise = null

async function refreshToken() {
  const res = await fetch('/api/auth/refresh', { method: 'POST', credentials: 'include' })
  if (!res.ok) { window.location.href = '/'; throw new Error('Session expired') }
}

export async function fetchWithRefresh(url, options = {}) {
  options.credentials = 'include'
  let res = await fetch(url, options)
  if (res.status === 401 && !url.includes('/api/auth/')) {
    if (!refreshing) {
      refreshing = true
      refreshPromise = refreshToken().finally(() => { refreshing = false })
    }
    await refreshPromise
    res = await fetch(url, options)
  }
  return res
}
FETCHWITHREFRESH

        cat > svelte/src/util/logger.js << 'LOGGERJS'
const CONFIG = { consoleEnabled: true, backendEnabled: false, debugToBackend: false }

function fmt(module, level, message, data) {
  return `[${new Date().toISOString()}] [${level}] [${module}] ${message}${data ? ' ' + JSON.stringify(data) : ''}`
}

async function send(module, level, message, data) {
  try {
    await fetch('/api/logs', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ module, level, message, data, timestamp: new Date().toISOString() })
    })
  } catch (_) {}
}

function log(module, level, message, data) {
  if (CONFIG.consoleEnabled) {
    level === 'ERROR' ? console.error(fmt(module, level, message, data))
    : level === 'WARN'  ? console.warn(fmt(module, level, message, data))
    : console.log(fmt(module, level, message, data))
  }
  if (CONFIG.backendEnabled && (level !== 'DEBUG' || CONFIG.debugToBackend))
    send(module, level, message, data)
}

export const debug       = (m, msg, d) => log(m, 'DEBUG', msg, d)
export const info        = (m, msg, d) => log(m, 'INFO',  msg, d)
export const warn        = (m, msg, d) => log(m, 'WARN',  msg, d)
export const error       = (m, msg, d) => log(m, 'ERROR', msg, d)
export const action      = (m, act, d) => log(m, 'INFO',  'action:' + act, d)
export const api         = (m, method, endpoint, d) => log(m, 'DEBUG', method + ' ' + endpoint, d)
export const apiResponse = (m, endpoint, ok, d)     => log(m, ok ? 'INFO' : 'ERROR', 'response:' + endpoint, d)
export const configure   = (opts) => Object.assign(CONFIG, opts)
LOGGERJS

        cat > svelte/src/module/header/store.js << 'HEADERSTOREJS'
import { writable } from 'svelte/store'

export const contextHeader      = writable(null)
export const contextHeaderProps = writable({})
export const contextTitle       = writable('')

export function setContextHeader(component, props = {}, title = '') {
  contextHeader.set(component)
  contextHeaderProps.set(props)
  contextTitle.set(title)
}

export function clearContextHeader() {
  contextHeader.set(null)
  contextHeaderProps.set({})
  contextTitle.set('')
}
HEADERSTOREJS

        cat > svelte/src/module/header/Layout.svelte << 'HEADERLAYOUT'
<script>
  import { contextHeader, contextHeaderProps, contextTitle } from './store.js'
  import { auth, logout } from '../../store.js'

  let dropdownOpen = $state(false)
  const toggleDropdown = () => { dropdownOpen = !dropdownOpen }
  const closeDropdown  = () => { dropdownOpen = false }
</script>

<header class="app-header d-flex align-items-center px-3 border-bottom bg-white">
  {#if $contextHeader}
    {@const ContextHeader = $contextHeader}
    <button class="btn btn-sm btn-outline-secondary me-1" onclick={toggleDropdown}>
      <ContextHeader {...$contextHeaderProps} isDropdown={false} />
      ▾
    </button>
    {#if dropdownOpen}
      <div class="context-dropdown shadow-sm border rounded p-1">
        <ContextHeader {...$contextHeaderProps} isDropdown={true} onClose={closeDropdown} />
      </div>
    {/if}
  {:else}
    <span class="fw-medium">{$contextTitle}</span>
  {/if}

  <div class="ms-auto d-flex align-items-center gap-2">
    {#if $auth.user}
      <span class="text-muted small">{$auth.user.username}</span>
      <button class="btn btn-sm btn-outline-danger" onclick={logout}>Esci</button>
    {/if}
  </div>
</header>

<style>
  .app-header { height: 56px; position: relative; }
  .context-dropdown {
    position: absolute; top: 56px; left: 0;
    z-index: 100; background: #fff; min-width: 180px;
  }
</style>
HEADERLAYOUT

        cat > svelte/src/module/sidebar/store.js << 'SIDEBARSTOREJS'
import { writable } from 'svelte/store'

export const contextSidebar      = writable(null)
export const contextSidebarProps = writable({})

export function setContextSidebar(component, props = {}) {
  contextSidebar.set(component)
  contextSidebarProps.set(props)
}

export function clearContextSidebar() {
  contextSidebar.set(null)
  contextSidebarProps.set({})
}
SIDEBARSTOREJS

        cat > svelte/src/module/sidebar/Layout.svelte << 'SIDEBARLAYOUT'
<script>
  import { contextSidebar, contextSidebarProps } from './store.js'
  import { currentModule } from '../../store.js'

  // Menu — aggiungere una voce per ogni modulo privato
  const menuItems = [
    // { id: 'dashboard', label: 'Dashboard', icon: '📊' },
  ]
</script>

<nav class="app-sidebar d-flex flex-column p-2">
  <div class="brand px-2 py-3 fw-bold fs-5">App</div>

  <ul class="nav flex-column gap-1">
    {#each menuItems as item}
      <li>
        <button
          class="nav-link btn btn-link w-100 text-start text-light"
          class:active={$currentModule.name === item.id}
          onclick={() => currentModule.navigate(item.id)}
        >
          <span class="me-2">{item.icon}</span>{item.label}
        </button>
      </li>
    {/each}
  </ul>

  {#if $contextSidebar}
    {@const ContextSidebar = $contextSidebar}
    <div class="mt-3 border-top pt-2"><ContextSidebar {...$contextSidebarProps} /></div>
  {/if}
</nav>

<style>
  .app-sidebar { width: 220px; min-height: 100vh; background: #212529; color: #fff; }
  .nav-link { color: #adb5bd; border-radius: 4px; }
  .nav-link:hover, .nav-link.active { color: #fff; background: #343a40; }
</style>
SIDEBARLAYOUT

        cat > svelte/src/module/auth/store.js << 'AUTHSTOREJS'
import { writable } from 'svelte/store'

export const loading = writable(false)
export const error   = writable(null)

export async function login(username, password) {
  loading.set(true)
  error.set(null)
  try {
    const res  = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    })
    const data = await res.json()
    if (!res.ok || data.err) throw new Error(data.err || 'Credenziali non valide')
    return data
  } catch (e) {
    error.set(e.message)
    return null
  } finally {
    loading.set(false)
  }
}
AUTHSTOREJS

        cat > svelte/src/module/auth/LoginComponent.svelte << 'LOGINCOMPONENT'
<script>
  import { login, loading, error } from './store.js'
  import { checkAuth, navigateAfterLogin } from '../../store.js'

  let username = $state('')
  let password = $state('')

  async function handleSubmit() {
    const data = await login(username, password)
    if (data) {
      await checkAuth()
      navigateAfterLogin()
    }
  }

  function handleKeydown(e) { if (e.key === 'Enter') handleSubmit() }
</script>

<div class="login-box">
  <h4 class="mb-4">Accedi</h4>

  {#if $error}
    <div class="alert alert-danger py-2">{$error}</div>
  {/if}

  <div class="mb-3">
    <label class="form-label" for="login-username">Username</label>
    <input id="login-username" class="form-control" bind:value={username} onkeydown={handleKeydown} disabled={$loading} />
  </div>
  <div class="mb-3">
    <label class="form-label" for="login-password">Password</label>
    <input id="login-password" type="password" class="form-control" bind:value={password} onkeydown={handleKeydown} disabled={$loading} />
  </div>

  <button class="btn btn-primary w-100" onclick={handleSubmit}
    disabled={$loading || !username || !password}>
    {$loading ? 'Accesso in corso...' : 'Accedi'}
  </button>
</div>

<style>
  .login-box { width: 100%; max-width: 360px; }
</style>
LOGINCOMPONENT

        cat > svelte/src/module/auth/Layout.svelte << 'AUTHLAYOUT'
<script>
  import LoginComponent from './LoginComponent.svelte'
</script>

<div class="auth-page d-flex align-items-center justify-content-center min-vh-100 bg-light">
  <LoginComponent />
</div>
AUTHLAYOUT

        cat > svelte/src/module/home/store.js << 'HOMESTOREJS'
import { writable } from 'svelte/store'

// Store del modulo home — aggiungere stato applicativo qui
HOMESTOREJS

        cat > svelte/src/module/home/HomeComponent.svelte << 'HOMECOMPONENT'
<script>
  import { currentModule } from '../../store.js'
</script>

<div class="text-center py-5">
  <h1 class="display-5">Benvenuto</h1>
  <p class="text-muted">Seleziona un modulo per iniziare.</p>
</div>
HOMECOMPONENT

        cat > svelte/src/module/home/Layout.svelte << 'HOMELAYOUT'
<script>
  import HomeComponent from './HomeComponent.svelte'
  import { auth, currentModule } from '../../store.js'
</script>

<div class="min-vh-100 bg-light">
  <header class="d-flex align-items-center px-4 py-3 bg-white border-bottom">
    <span class="fw-bold fs-5">App</span>
    <div class="ms-auto">
      {#if $auth.isAuthenticated}
        <button class="btn btn-sm btn-outline-secondary"
          onclick={() => currentModule.navigate('home')}>
          {$auth.user?.username}
        </button>
      {:else}
        <button class="btn btn-sm btn-outline-primary"
          onclick={() => currentModule.navigate('auth')}>
          Accedi
        </button>
      {/if}
    </div>
  </header>
  <main class="container py-5">
    <HomeComponent />
  </main>
</div>
HOMELAYOUT

        echo "Struttura Svelte 5 creata"
    fi

    # Installa dipendenze npm nel container
    echo "Installazione dipendenze frontend..."
    docker exec "$DEV_CONTAINER" sh -c "cd /workspace/svelte && npm install"

    # Configura cmd nel PATH del container
    echo "Configurazione cmd nel PATH..."
    INSTALLER_DIR=$(dirname "$0")
    mkdir -p bin
    cp "$INSTALLER_DIR/cmd" bin/cmd
    sed -i "s|com\.example|$GROUP_ID|g" bin/cmd
    chmod +x bin/cmd
    docker exec "$DEV_CONTAINER" sh -c "
        ln -sf /workspace/bin/cmd /usr/local/bin/cmd
        chmod +x /workspace/bin/cmd
    "
    echo "✓ cmd disponibile nel PATH del container"

    # README
    echo "# $PROJECT_NAME" > README.md
    echo "✓ README.md aggiornato"

    # Rimuove .git del repo originale
    if [ -d .git ]; then
        rm -rf .git
        echo "✓ .git rimosso"
    fi

    # Rimuove cmd dalla root (è stato copiato in bin/cmd)
    rm -f cmd
    echo "✓ cmd spostato in bin/"

    echo ""
    echo "=========================================="
    echo "Ambiente di sviluppo pronto!"
    echo "=========================================="
    echo ""
    echo "Container: $DEV_CONTAINER"
    echo "Network:   $DEV_NETWORK"
    echo ""
    echo "Porte:"
    echo "  Undertow (API):  localhost:$API_PORT_HOST  -> container:$API_PORT"
    echo "  Vite (frontend): localhost:$VITE_PORT_HOST -> container:$VITE_PORT"
    echo ""
    echo "Entra nel container:"
    echo "  docker exec -it $DEV_CONTAINER bash"
    echo ""
    echo "Comandi disponibili (dentro il container):"
    echo "  cmd app build        # compila → target/service.jar"
    echo "  cmd app start        # avvia backend in background"
    echo "  cmd app stop         # ferma backend"
    echo "  cmd app status       # stato backend"
    echo "  cmd app run          # avvia backend in foreground"
    echo "  cmd svelte build     # build → src/main/resources/static/"
    echo "  cmd svelte run       # Vite dev server con proxy API"
    echo "  cmd db               # CLI PostgreSQL (richiede --postgres)"
    echo "  cmd git push         # commit + push"
    echo "  cmd --help           # tutti i comandi"
    echo ""
    echo "Dev workflow consigliato:"
    echo "  terminale 1: cmd app start"
    echo "  terminale 2: cmd svelte run"
    echo "  browser:     http://localhost:$VITE_PORT_HOST"
    echo ""
    echo "Altri comandi:"
    echo "  ./install.sh --postgres   # Installa PostgreSQL"
    echo ""
    exit 0
fi
