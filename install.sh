#!/bin/sh
set -e

# Mostra help
show_help() {
    cat << EOF
Uso: ./install.sh [OPZIONE]

Opzioni:
  (nessuna)      Crea ambiente di sviluppo completo
  --postgres     Installa container PostgreSQL
  --help, -h     Mostra questo messaggio

Esempi:
  ./install.sh              # Prima installazione
  ./install.sh --postgres   # Aggiungi PostgreSQL
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
        mkdir -p src/main/java/com/example
        mkdir -p src/main/resources/static

        cat > pom.xml << 'POMXML'
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
    </properties>

    <dependencies>
        <dependency>
            <groupId>io.undertow</groupId>
            <artifactId>undertow-core</artifactId>
            <version>${undertow.version}</version>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-assembly-plugin</artifactId>
                <version>3.6.0</version>
                <configuration>
                    <archive>
                        <manifest>
                            <mainClass>com.example.App</mainClass>
                        </manifest>
                    </archive>
                    <descriptorRefs>
                        <descriptorRef>jar-with-dependencies</descriptorRef>
                    </descriptorRefs>
                    <finalName>service</finalName>
                    <appendAssemblyId>false</appendAssemblyId>
                </configuration>
                <executions>
                    <execution>
                        <id>make-assembly</id>
                        <phase>package</phase>
                        <goals>
                            <goal>single</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
POMXML

        cat > src/main/java/com/example/App.java << 'APPJAVA'
package com.example;

import io.undertow.Undertow;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.resource.ClassPathResourceManager;
import io.undertow.server.handlers.resource.ResourceHandler;
import io.undertow.util.Headers;

public class App {

    public static void main(String[] args) {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));

        // Serve static files bundled in JAR from src/main/resources/static/
        // (populated by 'npm run build' in the svelte/ project)
        ResourceHandler staticHandler = new ResourceHandler(
            new ClassPathResourceManager(App.class.getClassLoader(), "static")
        ).setWelcomeFiles("index.html");

        PathHandler paths = new PathHandler(staticHandler)
            .addPrefixPath("/api/hello", exchange -> {
                exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
                exchange.getResponseSender().send(
                    "{\"message\":\"Hello from Undertow!\",\"status\":\"ok\"}"
                );
            });

        // Register your handlers here:
        // .addPrefixPath("/api/items", new ItemHandler())

        Undertow server = Undertow.builder()
            .addHttpListener(port, "0.0.0.0")
            .setHandler(paths)
            .build();

        server.start();
        System.out.println("[info] Server listening on port " + port);
    }
}
APPJAVA

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
    "@sveltejs/vite-plugin-svelte": "^4.0.0",
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
          <svelte:component this={privateComponent} />
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
    <button class="btn btn-sm btn-outline-secondary me-1" onclick={toggleDropdown}>
      <svelte:component this={$contextHeader} {...$contextHeaderProps} isDropdown={false} />
      ▾
    </button>
    {#if dropdownOpen}
      <div class="context-dropdown shadow-sm border rounded p-1">
        <svelte:component this={$contextHeader} {...$contextHeaderProps} isDropdown={true} onClose={closeDropdown} />
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
    <div class="mt-3 border-top pt-2">
      <svelte:component this={$contextSidebar} {...$contextSidebarProps} />
    </div>
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
    <label class="form-label">Username</label>
    <input class="form-control" bind:value={username} onkeydown={handleKeydown} disabled={$loading} />
  </div>
  <div class="mb-3">
    <label class="form-label">Password</label>
    <input type="password" class="form-control" bind:value={password} onkeydown={handleKeydown} disabled={$loading} />
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
    docker exec "$DEV_CONTAINER" sh -c "cd /workspace/svelte && npm install" 2>/dev/null || true

    # Configura jms nel PATH del container
    echo "Configurazione jms nel PATH..."
    mkdir -p bin
    cp jms bin/jms
    chmod +x bin/jms
    docker exec "$DEV_CONTAINER" sh -c "
        ln -sf /workspace/bin/jms /usr/local/bin/jms
        chmod +x /workspace/bin/jms
    "
    echo "✓ jms disponibile nel PATH del container"

    # README
    echo "# $PROJECT_NAME" > README.md
    echo "✓ README.md aggiornato"

    # Rimuove .git del repo originale
    if [ -d .git ]; then
        rm -rf .git
        echo "✓ .git rimosso"
    fi

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
    echo "  jms app build        # compila → target/service.jar"
    echo "  jms app start        # avvia backend in background"
    echo "  jms app stop         # ferma backend"
    echo "  jms app status       # stato backend"
    echo "  jms app run          # avvia backend in foreground"
    echo "  jms svelte build     # build → src/main/resources/static/"
    echo "  jms svelte run       # Vite dev server con proxy API"
    echo "  jms db               # CLI PostgreSQL (richiede --postgres)"
    echo "  jms git push         # commit + push"
    echo "  jms --help           # tutti i comandi"
    echo ""
    echo "Dev workflow consigliato:"
    echo "  terminale 1: jms app start"
    echo "  terminale 2: jms svelte run"
    echo "  browser:     http://localhost:$VITE_PORT_HOST"
    echo ""
    echo "Altri comandi:"
    echo "  ./install.sh --postgres   # Installa PostgreSQL"
    echo ""
    exit 0
fi
