# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Full-stack Java + vanilla JavaScript application. Backend runs on Undertow (Java 21), frontend is built with Vite. PostgreSQL is managed via Flyway migrations. The app ships as a single executable JAR serving both the API and static frontend.

## Common Commands

### Via `cmd` (inside the dev Docker container or directly via `bin/cmd`)
```bash
cmd gui build            # Build frontend → src/main/resources/static/
cmd gui run              # Dev server Vite in foreground (port 5173)
cmd app build            # Compile Java (Maven) → target/service.jar
cmd app run              # Watch src/, recompile and restart on changes (recommended for development)
cmd app debug            # Same as run + JDWP debugger on port 5005
cmd app start            # Start JAR in background
cmd app stop             # Stop background process
cmd app stop --force     # Force kill: termina per nome e per porta API (cleanup zombie/porta occupata)
cmd app restart          # Stop + start
cmd app status           # Show running process info + zombie detection + port scan
cmd app clean            # Rimuove i file compilati (mvn clean → target/)
cmd gui clean            # Svuota src/main/resources/static/ (output build frontend)
cmd db                   # Interactive PostgreSQL CLI
cmd db -f <file>         # Execute SQL file or load CSV
cmd db status            # Show app DB config, connection health and migrations
cmd db setup             # Create user and database from .env file
cmd db reset             # Drop + recreate database (destructive!)
cmd module export --name <nome> [--vers 1.2.3]  # Esporta modulo in cartella module/<nome>[-version]/
cmd module import <path>                         # Installa modulo da path relativo a module/ (cartella o .tar.gz)
cmd module dist <path>                           # Pacchettizza modulo (path relativo a module/) → dist/<nome>-<vers>.tar.gz
cmd module remove --name <nome>                  # Disinstalla modulo usando il tracker installato
cmd module cli <module> <script> [OPTIONS]       # Esegue module/<module>/cli/<script>.sh (solo in sviluppo)
cmd module cli <module> list                     # Lista tutti i comandi CLI disponibili per il modulo
cmd module test <module> <script> [OPTIONS]      # Esegue module/<module>/test/<script>.sh (solo in sviluppo)
cmd module test <module> list                    # Lista tutti gli script di test disponibili per il modulo
cmd bench [options]      # Run siege benchmark (options passed to siege, log to bench/siege-YYYYMMDD-HHMMSS.log)
```

**Note:** The `cmd` script is located in `bin/cmd` and is available in PATH inside the Docker container.

### Directly (from `gui/` folder)
```bash
npm run dev              # Dev server on port 5173 (proxy /api → :8080)
npm run build            # Build → src/main/resources/static/
```

### Setup and deploy
```bash
./install.sh                        # Setup dev Docker environment (or restart existing)
./install.sh --postgres             # Install standalone PostgreSQL container
./install.sh --mailpit              # Install Mailpit (fake SMTP + web UI on port 8025)
./release.sh                        # Build production Docker image + tar.gz package
./release.sh -v 1.2.0               # Force specific version
```

### Container lifecycle (from host, outside Docker)
```bash
./app.sh --start    # Start all project containers in dependency order
./app.sh --stop     # Stop all project containers in reverse order
./app.sh --restart  # Stop then start
./app.sh --status   # Show running state of each container
```

Reads `PROJECT_NAME`, `PGSQL_HOST`, `PGSQL_ENABLED`, `MAILPIT_CONTAINER` from `.env` to determine which containers to manage. Only acts on containers that actually exist in Docker.

There are no test or lint commands configured.

## Typical Development Workflow

```bash
# Start the development environment
./install.sh                 # First time or restart existing container
docker exec -it <PROJECT_NAME> bash   # Enter dev container (container name matches PROJECT_NAME in .env)

# Terminal 1: Backend (watch mode with hot reload)
cmd app run
# Watches src/, recompiles on change, restarts JVM automatically
# Logs to stdout

# Terminal 2: Frontend (Vite dev server)
cmd gui run
# Dev server on http://localhost:5173 (or VITE_PORT_HOST from .env)
# Auto-refresh on file save
# Proxies /api/* to http://localhost:8080

# Terminal 3: Database (as needed)
cmd db status          # Check connections + migrations
cmd db                 # Interactive psql

# Access points:
# - Frontend: http://localhost:<VITE_PORT_HOST> (VITE_PORT_HOST from .env)
# - API: http://localhost:<API_PORT_HOST> (API_PORT_HOST from .env)
```

### Debugging (VSCode)

**Prerequisites:**
- [Java Extension Pack](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack) — backend debugging
- [Debugger for Firefox](https://marketplace.visualstudio.com/items?itemName=firefox-devtools.vscode-firefox-debug) — frontend debugging

Both are listed in `.vscode/extensions.json` (VSCode prompts to install on first open).

**Three debug configurations** in `.vscode/launch.json`:
- **`Attach to Docker`** — attaches Java debugger to JDWP on port 5005
- **`Debug Frontend (Vite)`** — launches Firefox pointed at Vite dev server (port 5173)
- **`Fullstack`** (compound) — activates both simultaneously

#### Backend only

```bash
cmd app debug   # starts JVM with JDWP on port 5005
```

Then in VSCode Run & Debug panel: select **"Attach to Docker"** → F5.

#### Full-stack (backend + frontend)

```bash
# Terminal 1
cmd app debug

# Terminal 2
cmd gui run
```

Then in VSCode Run & Debug panel: select **"Fullstack"** → F5.

VSCode attaches to Java on port 5005 and opens Firefox under debugger on `http://localhost:5173`. Set breakpoints in both `.java` handler files and `gui/src/` JS files — both will trigger in the same VSCode session.

**How it works:**
- `cmd app debug` starts the JVM with JDWP agent: `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005`
- The container exposes port 5005 → host's 5005
- Vite dev server (`cmd gui run`) generates source maps for `gui/src/` — breakpoints in JS files resolve correctly with `webRoot: ${workspaceFolder}/gui`
- Source map breakpoints work in JS logic (event handlers, lifecycle methods); breakpoints inside Lit `html\`...\`` template literals are not reliable (Vite limitation with tagged templates)

**Hot reload behavior:**
After every `.java` file save, `cmd app debug` recompiles and **restarts the JVM** — the JDWP connection drops. Re-attach by pressing F5 in the Run & Debug panel. The Firefox session remains open and does not need to be restarted.

## Architecture

### Backend (`src/main/java/`)

Two packages:

- **`<groupId>/`** — App-specific code: `App.java` (entry point + server setup), plus any installed module subpackages (e.g. `auth/` with `handler/`, `dao/`, `dto/`, `adapter/`).
- **`dev.jms.util/`** — Shared utility library: `Handler`, `HandlerAdapter`, `HttpRequest`, `HttpResponse`, `DB`, `Auth`, `Config`, `Json`, `Log`, `Mail`, `Validator`, `ValidationException`, `UnauthorizedException`, `Async`, `AsyncExecutor`, `Scheduler`, `Excel`, `PDF`, `HTML2PDF`, `File`, `RouteHandler`, `HttpMethod`, `Router`, `AuditLog`, `JWTBlacklist`, `RateLimiter`, `Session`, `Role`, `Permission`, `Cookie`.

**Handler pattern:** Routes use the `RouteHandler` functional interface `(HttpRequest, HttpResponse, Session, DB) throws Exception`. Handlers are plain classes with methods matching this signature, registered as method references. `HandlerAdapter` wires the handler to Undertow, dispatches to a worker thread, and auto-provides `HttpRequest`, `HttpResponse`, `Session`, and `DB` per request. Uncaught exceptions return 500 JSON automatically. Routes are registered in `App.java` (or a module's `Routes.java`) via a `Router` instance.

**Async routes:** Use `router.async()` instead of `router.route()` to dispatch to the `AsyncExecutor` dedicated thread pool (not Undertow worker threads). Use for slow DB queries, external API calls, or CPU-intensive operations. The thread name will be `async-handler-N`.

**Adding a new route:**
1. Create `src/main/java/<groupId>/handler/FooHandler.java` with methods `(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception`
2. Register in `App.java` or a module's `Routes.java`:
   - Blocking (Undertow worker): `router.route(HttpMethod.GET, "/api/foo", h::fooMethod)`
   - Async (dedicated pool): `router.async(HttpMethod.GET, "/api/foo", h::fooMethod)`

**Path parameters:** Use `{param}` in route path (e.g., `/api/users/{id}`) and access via `req.urlArgs().get("id")` in the handler.

**Response format:** All API responses use the envelope `{"err": boolean, "log": string|null, "out": object|null}`. Build via chained calls: `res.err(false).log(null).out(payload).send()`. Business errors return HTTP 200 with `err: true`; system errors return 500.

**DB layer:** `DB.java` is a thin JDBC wrapper — no ORM. Key methods:
- `db.select(sql, params)` → `List<HashMap<String, Object>>`
- `db.query(sql, params)` → `int` (rows affected)
- `db.cursor(sql, params)` → streaming `Cursor` for large result sets
- `db.begin()` / `db.commit()` / `db.rollback()` — manual transaction control
- Type helpers: `DB.toLong(obj)`, `DB.toLocalDate(obj)`, `DB.toBoolean(obj)`, `DB.toBigDecimal(obj)`, etc.

SQL parameters use JDBC `?` placeholders (not PostgreSQL `$1` syntax): `db.select("SELECT * FROM t WHERE id = ?", id)`.

**Config:** `Config.java` reads `/app/config/application.properties` (bind-mounted from `./config/` — not bundled in JAR). Environment variables override properties using uppercase+underscore notation: `DB_HOST` → `db.host`, `JWT_SECRET` → `jwt.secret`. Use `config.get(key, default)` and `config.getInt(key, default)`.

Key configuration parameters:
- `server.port` (default: 8080) — HTTP server port
- `db.*` — Database connection settings
- `jwt.secret`, `jwt.access.expiry.seconds` — JWT authentication
- `async.pool.size` (default: 20) — Thread pool size for @Async handlers
- `async.max.body.size` (default: 10485760 = 10MB) — Max body size for async handlers
- `mail.*` — SMTP configuration (disabled by default, set `mail.enabled=true` to enable)

**Auth:** PBKDF2 password hashing (16-byte salt, 310k iterations, SHA-256) in `Auth.java`. Two-token flow: access token (JWT HS256, 15 min) + refresh token (64-char hex, stored in `refresh_tokens` table, 7 days). JWT claims: `sub` (accountId), `username`, `ruolo`, `ruolo_level` (int), `must_change_password`.

**Session** (`Session.java`): Dual-role per-request object — JWT validation + server-side in-memory storage. Instantiated by `HandlerAdapter` alongside `HttpRequest`/`HttpResponse` and passed as the third handler argument. JWT is validated lazily on first access; result is cached for the request lifetime. Server-side storage (`ConcurrentHashMap`, sliding TTL, 30 min default) is keyed by `session_id` cookie (64-char hex, `SecureRandom`); the session is flushed to the store automatically via a pre-send hook before HTTP headers are committed. JWT methods: `session.require(Role, Permission)` (throws `UnauthorizedException` if not satisfied), `session.isAuthenticated()`, `session.sub()`, `session.username()`, `session.ruolo()`, `session.ruoloLevel()`, `session.mustChangePassword()`, `session.claims()` (empty map if not authenticated). Storage methods: `session.getAttr(key)`, `session.setAttr(key, value)`, `session.removeAttr(key)`, `session.clearAttrs()`, `session.sessionId()`. Static config: `Session.configure(int ttlSeconds)`, `Session.shutdown()` (called in JVM shutdown hook). Roles: `GUEST(0)`, `USER(1)`, `ADMIN(2)`, `ROOT(3)`. `GUEST + READ` is always permitted; all other combinations require a valid JWT with sufficient `ruolo_level`.

**JWT blacklist** (`JWTBlacklist.java`): In-memory blacklist for revoked JWTs (logout, password change). Tracks JWT ID (`jti`) until natural expiry to prevent session replay attacks. Singleton, lazy-init, thread-safe. Auto-cleanup every minute.

**Rate limiter** (`RateLimiter.java`): In-memory brute-force protection. Tracks failed attempts per key (e.g. `"user.login:IP"`) with a sliding time window. Singleton, configurable via `RateLimiter.configure(maxAttempts, windowMs)`. Auto-cleanup every minute.

**Audit log** (`AuditLog.java`): Structured event logging to `audit_log` table. Static methods, no initialization required. Errors on write are logged but not propagated (same pattern as `Log`).

**Scheduler:** JobRunr-based cron scheduler backed by PostgreSQL (`Scheduler.java`). Initialized in `App.main()` after `DB.init()`. Jobs are registered with `Scheduler.register("job-id", "0 2 * * *", Handler::staticMethod)` — the target method must be static and parameterless. Jobs persist across restarts (stored in `jobrunr_*` tables, auto-created). Config: `scheduler.enabled` (default: `true`), `scheduler.poll.interval.seconds` (default: `15`). Graceful shutdown via `Scheduler.shutdown()` in the JVM shutdown hook.

**Excel utilities** (`Excel.java`): Static methods `Excel.read(InputStream)` (returns all rows as `List<Map<String,Object>>`) and `Excel.analyze(InputStream, int)` (preview/validation without loading full file). Inner class `Excel.Importer` handles full import with pluggable `MappingStrategy` (column mapping) and `NormalizationStrategy` (data normalization).

**PDF utilities** (`PDF.java`): Instantiated via `PDF.load(InputStream)`; supports text extraction, annotation/signature field inspection, and page manipulation. Persist changes with `pdf.save(OutputStream)`, which also closes the document.

**HTML2PDF utilities** (`HTML2PDF.java`): Static methods for converting HTML to PDF using Flying Saucer + OpenPDF. `HTML2PDF.convert(String html)` converts HTML/XHTML to PDF bytes with CSS 2.1 support. `HTML2PDF.convert(String html, boolean preprocessInput)` optionally preprocesses HTML5 tags to XHTML strict (converts `<input>` → `<span>`, adds self-closing slashes). Automatically validates well-formed XHTML, adds DOCTYPE if missing, and wraps content in html/body structure. Supports tables, lists, images, and font embedding.

**File utilities** (`File.java`): Static methods for generic file and directory operations with built-in security (path sanitization to prevent directory traversal). Reading: `File.readBytes(path)`, `File.readText(path)`, `File.readLines(path)`. Writing: `File.writeBytes(path, content)`, `File.writeText(path, content)`, `File.appendText(path, content)`. Operations: `File.copy(source, dest)`, `File.move(source, dest)`, `File.delete(path)`, `File.createDirectory(path)`. Utilities: `File.list(directory, pattern)` for glob pattern matching, `File.hash(path, algorithm)` for MD5/SHA-256 hashing, `File.size(path)`, `File.exists(path)`. All write operations create parent directories automatically. All paths are normalized and sanitized.

### Frontend (`gui/`)

Vite 6 SPA project with modular multi-container architecture. Sources in `gui/src/`, build output in `src/main/resources/static/` (bundled in JAR by Maven).

```
gui/src/
├── index.html           → Entry point with multi-area layout (header, main, footer)
├── router.js            → Multi-container SPA router with persistent modules support
├── config.js            → Module configuration template (no project-specific entries)
├── init.js              → Global app initialization (fetch interceptor)
├── store.js             → Nanostores-based state management (authorized, user stores)
└── module/              → Empty placeholder — modules added via cmd module import
    └── .gitkeep
```

**Multi-container layout** (`index.html`): Defines three container areas:
- `#header` — For persistent modules like navigation bars
- `#main` — For dynamic page content
- `#footer` — For persistent modules like footers (optional)

**Module pattern:** Each module is a directory in `gui/src/module/` (top-level modules) or `gui/src/module/<namespace>/` (namespaced modules) with an `index.js` that exports a `mount(container)` function. The router mounts each module in its designated container based on configuration. Modules may contain semantic subfolders to organize components by domain (e.g., `user/auth/`, `user/account/`).

**Module configuration** (`config.js`): All modules declare these attributes explicitly:
```javascript
export const MODULE_CONFIG = {
  moduleName: {
    route: '/path' | null,             // URL hash route or null (not navigable, e.g. header)
    path: 'dirname' | 'ns/name' | null, // Folder under gui/src/module/ (top-level) or gui/src/module/<ns>/ (namespaced), or null
    container: 'main' | 'header' | 'footer',  // DOM container ID
    authorization: null | { redirectTo: '/route' },  // Access control
    persistent: true | false,          // persistent requires path !== null
    priority: 999,                     // Load order (lower = first, only for persistent)
    init: null | true | async function  // Initialization function; true = auto-import ./module/<path>/init.js
  }
};

// Selects which MODULE_CONFIG key to load when no hash is present
export const DEFAULT_MODULE = 'status';
```

`path: null` is reserved for routes handled entirely by the backend with no frontend module to mount. `persistent: true` with `path: null` is a configuration error — the router throws explicitly.

A new installation includes `status` as the only pre-installed frontend module (`gui/src/module/status/`), which calls `/api/status` and displays the result. It is the `DEFAULT_MODULE`.

**Adding a new module:**
1. Create `gui/src/module/newmodule/index.js` exporting `{ default: { mount(container) {...} } }`
2. Add complete entry to `config.js` with `route`, `path`, `container`, `authorization`, `persistent`, `priority`, `init`
3. Access via `http://localhost:5173/#/newmodule`

**Router:** Multi-container hash-based SPA router supports:
- **Persistent modules**: Mounted once at startup (e.g., `header`), never unmounted; require `path !== null`
- **Dynamic modules**: Mounted/unmounted during navigation (e.g., `home`, `auth`)
- **Default module**: When no hash is present, loads `DEFAULT_MODULE` by key directly (not by route lookup)
- **Container isolation**: Each module renders in its configured container
- **Init procedures**: Executed before first routing to prepare shared state
- **Authorization**: Redirects or shows 403 for protected routes
- **Navigation ID tracking**: Discards stale module loads during rapid navigation
- **`/#/` redirect**: Hash exactly equal to `/` redirects to the root URL `/` (not treated as a route)

**Important — Vite dynamic import limitation:** Vite cannot resolve template literal dynamic imports where the variable contains `/` (multi-level paths, e.g. `cti/vonage`). Module loading uses `import.meta.glob('./module/**/index.js')` and `import.meta.glob('./module/**/init.js')` at the top of `router.js` to pre-register all modules, then looks up the resolved module by path string. Never use `import(\`./module/${path}/index.js\`)` directly — it will fail for namespaced modules.

**Authorization model:**
- `authorization: null` — Publicly accessible
- `authorization: { redirectTo: '/path' }` — Protected; redirects unauthorized users

**Global fetch interceptor** (`init.js`): Replaces `window.fetch` to intercept all API responses. On `err: true` with a known auth error (`'Non autenticato'`, `'Token non valido o scaduto'`), it first attempts to refresh the token via `/api/user/auth/refresh`. If refresh succeeds, the original request is automatically retried. If refresh fails, `authorized` is set to `false`, triggering the router redirect to login. Requests to `/auth/refresh` and `/auth/login` skip the retry to prevent infinite loops.

**State management** (`store.js`): Uses [nanostores](https://github.com/nanostores/nanostores) (v0.11.3) for reactive state. Exports:
- `authorized` store (boolean) — Authentication state
- `user` store (object|null) — Current user data
Router and modules subscribe to these stores for reactive updates.

### Database Migrations

Flyway migrations in `src/main/resources/db/migration/`. Naming: `V{timestamp}__{description}.sql` where timestamp format is `YYYYMMdd_HHmmss` (e.g., `V20260222_163602__auth.sql`). The base scaffold has no migrations — they are introduced by modules.

### Docker / Deployment

- Dev container name matches `PROJECT_NAME` in `.env`
- Network: `<project>-net` (e.g., `hello-net`)
- PostgreSQL: shared `postgres` container (not project-specific), connected to the project network
- Dev ports (configurable in `.env`):
  - API: `API_PORT_HOST` → 8080
  - Vite: `VITE_PORT_HOST` → 5173
  - JDWP: `DEBUG_PORT_HOST` → 5005
  - PostgreSQL: `PGSQL_PORT_HOST` → 5432
- Bind mounts: `./` → `/workspace`, `./logs/` → `/app/logs`, `./config/` → `/app/config`
- Production: non-root `appuser` (UID 1001), 512MB memory / 1.0 CPU limits, single JAR

### Template (`jms/`)

`jms/` is an **optional** clone of the original jms repository in its non-contextualized state — not customized for any specific application. It is not present by default; it is only cloned here when you want to keep this project aligned with the upstream template and propagate improvements back. When present inside a project root, it means the original repo has been cloned here to keep it aligned with the project. The purpose is to propagate back to this repo any functionality that is general or generalizable (not project-specific), so that all current and future applications built from jms can benefit from those improvements.

**Key distinction — project files vs. template files:**
- Files in the project root are project-specific and fully instantiated (no placeholders).
- Files in `jms/` may contain placeholders (e.g. `{{PROJECT_NAME}}`, `{{DB_HOST}}`) that `install.sh` substitutes when creating a new project. Never copy `jms/` files into the project directly without first removing or resolving those placeholders.

**What can be propagated to `jms/`:**
- Changes that are already fully generic — e.g. additions or fixes to `dev.jms.util` library classes, improvements to `bin/cmd`, `install.sh`, or `release.sh`.
- Changes that are project-specific must be **generalized first** (re-expressed with placeholders or made fully project-agnostic) before being written into `jms/`.

**What must NOT be propagated as-is:**
- Any code that references project-specific names, credentials, routes, or domain logic — these belong only in the project root and must be abstracted before touching `jms/`.

Clone it with the project name to start a new project — the Java source structure is already in its final position. It contains:
- `src/main/java/dev/jms/util/` — Complete utility library source (26+ files: `Handler`, `HandlerAdapter`, `HttpRequest`, `HttpResponse`, `DB`, `Auth`, `Config`, `Json`, `Log`, `Mail`, `Validator`, `ValidationException`, `UnauthorizedException`, `Async`, `AsyncExecutor`, `Scheduler`, `Excel`, `PDF`, `HTML2PDF`, `File`, `RouteHandler`, `HttpMethod`, `Router`, `AuditLog`, `JWTBlacklist`, `RateLimiter`, `Session`, `Role`, `Permission`, `Cookie`)
- `src/main/java/dev/jms/app/App.java` — Entry point with AsyncExecutor initialization
- `src/main/resources/` — `logback.xml`, empty `static/` and `db/migration/` directories
- `pom.xml` — Maven dependencies (groupId: `dev.jms.app`)
- `config/application.properties` — Config template with placeholders substituted by `install.sh`
- `gui/` — Frontend base template (router, stores, init, empty module/), copied to `gui/` by `install.sh` when creating a new project
- `module/` — Module sources in expanded folder format (each module is a top-level subdirectory)
- `bin/cmd`, `install.sh`, `release.sh` — Scripts with bench support
- `docs/` — Documentation including architecture details

### Java package

The Java base package is always `dev.jms.app` — it never changes across projects and is not a substitutable placeholder. All module sources use `dev.jms.app` directly in `package` and `import` statements.

### Modules (`module/`)

Self-contained optional features. In `jms/module/` they are stored as expanded folders (not compressed); they can be packaged as `.tar.gz` for distribution via `cmd module dist`. Each module folder contains:
- `api/` — Java sources copied into `src/main/java/.../<module>/`. Internal layout: `handler/` (route handlers), `dao/`, `dto/`, `helper/` (shared logic, at the same level as `handler/`, not inside it), `Routes.java`
- `gui/` — Frontend module sources (copied into `gui/src/module/<name>/` for top-level, or `gui/src/module/<ns>/<name>/` for namespaced)
- `migration/` — Flyway SQL migrations
- `config/` — Application properties
- `module.json` — Module metadata (auto-generated by `cmd module export`)
- `profile.xml` — Maven profile with external dependencies (optional — only needed if the module requires a library not already in the base `pom.xml`)
- `cli/` — CLI scripts for the module (optional). Two reserved names: `setup.sh` (post-install) and `remove.sh` (pre-remove). All scripts in `cli/` are invocable via `cmd module cli <module> <script>`

**Util library is atomic:** All classes in `dev.jms.util` are always compiled as a unit. Their dependencies (Undertow, HikariCP, PostgreSQL, Flyway, Jackson, Logback, angus-mail, java-jwt, jobrunr, poi-ooxml, pdfbox) are all declared in the base `pom.xml`. A module's `profile.xml` is only needed for external libraries that are not backing any util class — e.g. a third-party SDK specific to that module.

**`module.json` schema:**
Top-level (`module/mymodule/`):
```json
{
  "name": "mymodule",
  "version": "1.0.0",
  "dependencies": {},
  "api": {
    "routes": "dev.jms.app.mymodule.Routes.register(router);",
    "config": {}
  },
  "gui": {
    "config": {
      "route": "/mymodule", "path": "mymodule", "container": "main",
      "authorization": null, "persistent": false, "priority": 999, "init": null
    }
  },
  "install_script": "cli/setup.sh",
  "uninstall_script": "cli/remove.sh",
  "install_notice": null
}
```

**Setup scripts** (optional):
- `install_script`: Path relativo allo script bash eseguito dopo l'import del modulo (es. `"cli/setup.sh"`). Lo script riceve due argomenti: `$1` = `WORKSPACE` path (es. `/workspace`), `$2` = `MODULE_KEY` (es. `aes` o `cti/vonage`). Usato per setup iniziale come creazione directory, download asset, ecc. Se lo script fallisce (exit code != 0), viene mostrato un warning ma l'installazione continua.
- `uninstall_script`: Path relativo allo script bash eseguito prima della rimozione del modulo (es. `"cli/remove.sh"`). Riceve gli stessi argomenti di `install_script`. Usato per cleanup, backup dati, o avvisi all'utente. Se lo script fallisce, viene mostrato un warning ma la rimozione continua.

Gli script in `cli/` sono invocabili anche manualmente: `cmd module cli <module> setup` / `cmd module cli <module> remove`. `module_export` rileva automaticamente `cli/setup.sh` e `cli/remove.sh` e li include nel `module.json` generato.

Con namespace (`module/ns/mymodule/`): `api.routes` usa il package completo `dev.jms.app.module.ns.mymodule.Routes.register(router);` e `gui.config.path` vale `"ns/mymodule"`. Il router usa automaticamente `gui/src/module/ns/mymodule/` per i path contenenti `/`.

**Export** un modulo dal progetto corrente in cartella non compressa. `--name` accetta sia nomi semplici che key con namespace:
```bash
cmd module export --name auth --vers 1.0.0      # → module/auth-1.0.0/
cmd module export --name auth                   # → module/auth/
cmd module export --name cti/vonage --vers 1.0.0 # → module/cti/vonage-1.0.0/
```
Export legge automaticamente `*Routes.java` (per i metadati API), `gui/src/config.js` (per l'entry GUI), e estrae il Maven profile dal `pom.xml` (se presente).

**Dist** pacchettizza una cartella di modulo in archivio distribuibile. Il path è relativo a `module/`; nome e versione vengono letti da `module.json`; l'archivio viene scritto in `dist/`:
```bash
cmd module dist auth   # → dist/auth-1.0.0.tar.gz
```

**Import** installa un modulo da cartella o archivio `.tar.gz`. Il path è sempre relativo a `module/` e può includere namespace:
```bash
cmd module import auth                   # da cartella top-level
cmd module import auth-1.0.0.tar.gz     # da archivio
cmd module import cti/vonage             # da cartella con namespace
cmd module import cti/vonage --force     # bypassa verifica dipendenze
```
Import automaticamente:
1. Verifica dipendenze **transitive e bloccanti** — se una dipendenza (diretta o transitiva) non è installata, l'import viene bloccato con errore; `--force` bypassa il blocco
2. Copia sorgenti Java in `app/module/<name>/` (top-level) o `app/module/<ns>/<name>/` (namespace), aggiornando i package declaration
3. Copia sorgenti GUI in `gui/src/module/<name>/` (top-level) o `gui/src/module/<ns>/<name>/` (namespace)
4. Copia migration SQL
5. Inserisce la chiamata `api.routes` in `App.java` dopo `// [MODULE_ROUTES]`
6. Inserisce l'entry `gui.config` in `gui/src/config.js` dopo `// [MODULE_ENTRIES]` (con `path` corretto per namespace)
7. Inserisce il Maven profile da `profile.xml` in `pom.xml` dopo `<!-- [MODULE_PROFILES] -->` (se presente)
8. Scrive il tracker in `src/main/resources/module/<key>/module.json`
9. Rigenera `src/main/resources/module/installed.json` con la mappa di tutti i moduli installati

**Maven auto-activation:** Il profile si attiva automaticamente quando il tracker `module.json` esiste, scaricando le dipendenze al prossimo build.

**Remove** disinstalla un modulo leggendo il tracker:
```bash
cmd module remove --name auth
cmd module remove --name cti/vonage
cmd module remove --name auth --force   # bypassa controllo dipendenze inverse
```
Prima di rimuovere verifica che nessun modulo installato dipenda dal modulo target — **bloccante** se trovati dipendenti; `--force` bypassa il blocco.
Rimuove sorgenti Java, GUI, route da `App.java`, entry da `config.js`, Maven profile da `pom.xml`, tracker, e rigenera `installed.json`. Le migration Flyway non vengono rimosse automaticamente (avviso esplicito).

**Tracker installati:** dopo ogni import riuscito viene scritto `src/main/resources/module/<key>/module.json` (es. `module/user/module.json` o `module/cti/vonage/module.json`). Le dipendenze vengono verificate cercando per `name` in tutti i tracker. `module remove` richiede il tracker per operare.

**Manifest:** `src/main/resources/module/installed.json` — auto-generato da ogni `import` e `remove`, contiene la mappa `key → {name, version, dependencies}` di tutti i moduli installati. Letto da `App.java` all'avvio per verificare le dipendenze e loggare `[warn]` se non soddisfatte.

**Dipendenze transitive:** la verifica all'import è ricorsiva — se A dipende da B e B dipende da C, installare A richiede che B e C siano entrambi installati. La verifica alla rimozione è inversa — se A dipende da B, rimuovere B è bloccato finché A è installato.

**Moduli migration-only:** moduli senza Java né GUI (es. `audit`). Struttura: solo `migration/` e `module.json` con `api: null` e `gui.config: null`. Supportati nativamente da `module_install`.

**Markers required in host project:**
- `App.java`: `// [MODULE_ROUTES]` prima delle registrazioni di route dei moduli
- `gui/src/config.js`: `// [MODULE_ENTRIES]` dopo l'entry `status` dentro `MODULE_CONFIG`

**Each Java module must have a `*Routes.java` class** at the module root (e.g. `mymodule/Routes.java`) with a static `register(Router router)` method. Modules that also need `Config` add it as a second parameter: `register(Router router, Config config)`.

**Install paths:** All modules are installed under `app/module/` and `gui/src/module/` regardless of whether they have a namespace:
- Top-level (`module/home/`): Java → `app/module/home/`, pkg `dev.jms.app.module.home`, GUI → `gui/src/module/home/`
- Namespace (`module/cti/vonage/`): Java → `app/module/cti/vonage/`, pkg `dev.jms.app.module.cti.vonage`, GUI → `gui/src/module/cti/vonage/`
- API routes convention for namespaced modules: `/api/<ns>/<name>/...` (e.g. `/api/cti/vonage/...`)
- `gui.config.path`: `"<ns>/<name>"` for namespaced modules — the router always resolves from `gui/src/module/`

The install script (`_rewrite_java_packages`) automatically updates package declarations and imports when copying to the target location.

**Available modules** (in `module/`):
- `audit/` — Creates `audit_log` table; migration-only (no Java, no GUI); required by `user`
- `user/` — Authentication + account management (login, logout, 2FA, reset/change password, CRUD admin, self-edit); route `/user`; requires: `audit`
- `header/` — Persistent navigation header (no dependencies)
- `home/` — Simple home page with API hello endpoint (no dependencies)
- `crm/` — Contact and list management module; CRUD for contacts and lists, advanced search with pagination, blacklist management, contact/list association, Excel file importer with column mapping and pre-import validation; route `/crm`; requires: `user`, `vonage`
- `aes/` — Advanced Electronic Signature module; provides PDF signature field insertion (`/api/aes/pdf/signature/field`, `/api/aes/html/signature/field`), file storage management, integration with Namirial/Savino remote signature platforms; no frontend (`path: null`); no dependencies
- `cti/vonage/` — Vonage Voice API integration; route `/cti`, namespace `cti`; protected; requires Vonage config + private key; `@vonage/client-sdk` and `@vonage/cli` are installed automatically via `cli/setup.sh` post-install script; routes under `/api/cti/vonage/`; requires: `user`. Webhook URLs to configure in Vonage dashboard: Voice Answer URL → `/api/cti/vonage/answer`, Voice Event URL → `/api/cti/vonage/event`, RTC Event URL → `/api/cti/vonage/event`. `CTI_VONAGE_FROM_NUMBER` must be a real Vonage virtual number (11 digits, no `+`) linked to the application. Custom data passed via `client.serverCall(data)` arrives in the answer webhook body as `custom_data` (JSON-serialized string), not as a query parameter. Operator-session TTL: `sessione_ttl TIMESTAMP` in `cti_operatori` is set to `NOW() + 30 minutes` on every `claimOrRenew()` call; the frontend refreshes every 13 minutes keeping it alive. Scheduler job `cti-session-cleanup` (every minute) releases all rows where `sessione_ttl < NOW()`, recovering operators from crashed browsers within 31 minutes max.
- `asynctest/` — Test module for async vs blocking behavior comparison; no frontend, not for production
- `schedulertest/` — Test module for Scheduler functionality; no frontend, not for production
- `iacs/` — Integrated Access Control System (stub/placeholder — no implementation yet)

All modules follow the complete configuration schema with all 7 attributes (`route`, `path`, `container`, `authorization`, `persistent`, `priority`, `init`).

## Coding Style

Full rules in `docs/dsl/java.md` and `docs/dsl/javascript.md`. Key non-obvious conventions:

### Java

**Formatting:**
- 2-space indentation (no tabs)
- Class/method opening brace on its **own line**; control flow (`if`, `for`, `try`, etc.) brace on **same line**
- No empty line immediately after the opening brace of a class or method
- No extra spaces for vertical alignment of assignments or arguments

**Variables:**
- Declare all variables at the top of their scope, before any executable statement
- Declaration and initialization are always separate statements
- Never use `var`; always declare the explicit type
- No wildcard generics (`?`, `? extends`, `? super`)

**Control flow:** Single exit point per method — no early `return` in `void` methods (use `if-else`); one `return` in typed methods.

**Database:** Always assign SQL to a local variable named `sql` before passing it to `db.select()` or `db.query()`.

**Error handling:**
- Business errors (validation, auth, not-found): handle in handler, return HTTP 200 with `err: true`, log at WARN level (no stack trace)
- System errors: let them propagate to `HandlerAdapter`, which returns HTTP 500 and logs at ERROR with stack trace

**`HttpResponse` extras:** `res.clearCookie(name)` sets a cookie with `maxAge=0` to delete it client-side. Goes between `contentType()` and `err()` in the chain.

**`Validator`:** Static utility class — `final` with private constructor, cannot be instantiated. All validation methods are static: `Validator.required(value, "fieldName")`, etc. Throws `ValidationException` directly.

**Response builder:** Always chain in this exact order before `send()`: `status() → contentType() → [cookie()/clearCookie()...] → err() → log() → out()`. Each method goes on its own line, continuation lines indented so the `.` aligns 3 spaces past the `res` start:
```java
res.status(200)
   .contentType("application/json")
   .err(false)
   .log(null)
   .out(payload)
   .send();
```

**Javadoc:** Required on all public and package-private classes and methods.

### JavaScript

**Formatting:**
- Semicolons on every statement; one statement per line
- Long expressions: extract intermediate variables rather than wrapping/chaining
- Function body always on separate lines from the signature (never inline)

**Exports:**
- All named exports go in a single `export { }` block at the **end** of the file — never `export function` or `export const` inline
- Files that only register a custom element (`customElements.define(...)`) export nothing

**JSDoc:** Required on all exported or publicly relevant functions (`/** ... @param ... @returns ... */`); inline `//` comments are for context notes only.

**Module-level state:** Group related variables in a named object (`const refreshState = { active: false, promise: null }`) instead of separate top-level `let` declarations.

**Web components (Lit):**
- Extend `LitElement`; disable Shadow DOM with `createRenderRoot() { return this; }` so Bootstrap styles apply
- Reactive state via `static properties = { _x: { state: true } }`, initialized in `constructor()`
- Subscribe to stores in `connectedCallback`, unsubscribe in `disconnectedCallback`; **never call `render()` explicitly** — Lit handles it automatically

**Frontend dependencies:** Bootstrap 5 (CSS/components), Lit 3 (web components), nanostores (reactive state).

**Naming conventions:**
- **Generality principle**: Absence of suffix indicates broader context or higher generality. `Login` is a full page (broad context); `LoginForm` or `LoginButton` would be specialized components (narrow context).
- **File = Class**: File name exactly matches class name (Java-like). Component files use PascalCase (`Login.js` → `class Login`); context files use lowercase (`index.js`, `init.js`).
- **Namespace via path**: Namespace expressed through folder structure, not class name prefixes. Use semantic subfolders like `user/auth/Login.js` (not `user/UserLogin.js`).
- **Custom elements**: Tag names use kebab-case with module prefix to avoid global conflicts (`customElements.define('user-login', Login)`).
- **Semantic folders**: Organize by domain semantics (`auth/`, `account/`), not technical categories (`components/`, `pages/`).

Full naming rules documented in `docs/dsl/javascript.md`.

## Environment Configuration

The `.env` file contains project configuration used by installation and release scripts:

**Key variables:**
- `PROJECT_NAME` — Container and database name (default: directory name)
- `API_PORT_HOST`, `VITE_PORT_HOST` — Host ports for development access
- `PGSQL_*` — PostgreSQL connection parameters
- `JAVA_VERSION` — Java version for Docker images (default: 21)
- `ARTIFACT_VERSION` — Application version for releases (default: 1.0.0)

Environment variables can override application properties. For example, `DB_HOST=localhost` in environment will override `db.host` in `config/application.properties`.
