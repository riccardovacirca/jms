# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Full-stack Java + vanilla JavaScript application. Backend runs on Undertow (Java 21), frontend is built with Vite. PostgreSQL is managed via Flyway migrations. The app ships as a single executable JAR serving both the API and static frontend.

## Common Commands

### Via `cmd` (inside the dev Docker container)
```bash
cmd gui build            # Build frontend → src/main/resources/static/
cmd gui run              # Dev server Vite in foreground (port 5173)
cmd app build            # Compile Java (Maven) → target/service.jar
cmd app run              # Watch src/, recompile and restart on changes (recommended for development)
cmd app debug            # Same as run + JDWP debugger on port 5005
cmd app start            # Start JAR in background
cmd app stop             # Stop background process
cmd app restart          # Stop + start
cmd app status           # Show running process info
cmd db                   # Interactive PostgreSQL CLI
cmd db -f <file>         # Execute SQL file or load CSV
cmd db status            # Show app DB config, connection health and migrations
cmd db setup             # Create user and database from .env file
cmd db reset             # Drop + recreate database (destructive!)
cmd sync                 # Sync sources → jms/ (requires confirmation)
cmd module export <name> [-v 1.2.3]  # Export module to modules/<name>[-version].tar.gz
cmd module import <file.tar.gz>      # Extract module into modules/<name>/ with placeholders replaced
cmd bench [options]      # Run siege benchmark (options passed to siege, log to bench/siege-YYYYMMDD-HHMMSS.log)
```

### Directly (from `vite/` folder)
```bash
npm run dev              # Dev server on port 5173 (proxy /api → :8080)
npm run build            # Build → src/main/resources/static/
```

### Setup and deploy
```bash
./install.sh                        # Setup dev Docker environment (or restart existing)
./install.sh --groupid io.mycompany # First install with custom GroupId
./install.sh --postgres             # Install standalone PostgreSQL container
./install.sh --mailpit              # Install Mailpit (fake SMTP + web UI on port 8025)
./release.sh                        # Build production Docker image + tar.gz package
./release.sh -v 1.2.0               # Force specific version
```

There are no test or lint commands configured.

## Typical Development Workflow

```bash
# Start the development environment
./install.sh                 # First time or restart existing container
docker exec -it <project> bash    # Enter dev container

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
# - Frontend: http://localhost:2350 (or VITE_PORT_HOST)
# - API: http://localhost:2310 (or API_PORT_HOST)
```

### Debugging (VSCode + Java Extension Pack)

```bash
# Inside the dev container
cmd app debug   # Start backend with JDWP on port 5005
```

Then in VSCode: **Run and Debug** (`Cmd+Shift+D`) → select **"Attach to Docker"** → press `F5`.

The `.vscode/launch.json` is already configured to attach to `localhost:5005`. The app starts immediately (`suspend=n`) and waits for VSCode to connect.

**Important:** After every file save, `cmd app debug` recompiles and restarts the JVM — VSCode loses the connection and must be re-attached with `F5`. This is normal behavior in watch mode.

## Architecture

### Backend (`src/main/java/`)

Two packages:

- **`<groupId>/`** — App-specific code: `App.java` (entry point + server setup), plus any installed module subpackages (e.g. `auth/` with `handler/`, `dao/`, `dto/`, `adapter/`).
- **`dev.jms.util/`** — Shared utility library: `Handler`, `HandlerAdapter`, `HttpRequest`, `HttpResponse`, `DB`, `Auth`, `Config`, `Json`, `Log`, `Mail`, `Validator`, `Async`, `AsyncExecutor`, and an `excel/` subpackage.

**Handler pattern:** Each route is a class implementing `Handler`. Override `get()`, `post()`, `put()`, or `delete()` as needed — unimplemented methods return 405. `HandlerAdapter` wires the handler to Undertow, dispatches to a blocking thread, and auto-provides `HttpRequest`, `HttpResponse`, and `DB` per request. Uncaught exceptions return 500 JSON automatically. Routes are registered in `App.java` via `PathTemplateHandler`.

**Async handlers:** Add `@Async` annotation to enable non-blocking execution. Async handlers use dedicated thread pool (`AsyncExecutor`) and non-blocking body reading. Useful for slow queries, external API calls, or CPU-intensive operations. Example:

```java
@Async
public class SlowQueryHandler implements Handler {
  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception {
    List<HashMap<String, Object>> results = db.select(
      "SELECT pg_sleep(1), 'slow query' as message"
    );
    res.status(200).contentType("application/json")
       .err(false).log(null).out(results.get(0)).send();
  }
}
```

**Adding a new route:**
1. Create `src/main/java/<groupId>/handler/FooHandler.java` implementing `Handler`
2. Optionally add `@Async` annotation for non-blocking execution
3. Register in `App.java`: `paths.add("/api/foo", route(new FooHandler(), ds))`

**Path parameters:** Use `{param}` in route path (e.g., `/api/users/{id}`) and access via `req.urlArgs().get("id")` in the handler.

**Response format:** All API responses use the envelope `{"err": boolean, "log": string|null, "out": object|null}`. Build via chained calls: `res.err(false).log(null).out(payload).send()`. Business errors return HTTP 200 with `err: true`; system errors return 500.

**DB layer:** `DB.java` is a thin JDBC wrapper — no ORM. Key methods:
- `db.select(sql, params)` → `List<HashMap<String, Object>>`
- `db.query(sql, params)` → `int` (rows affected)
- `db.cursor(sql, params)` → streaming `Cursor` for large result sets
- `db.begin()` / `db.commit()` / `db.rollback()` — manual transaction control
- Type helpers: `DB.toLong(obj)`, `DB.toLocalDate(obj)`, `DB.toBoolean(obj)`, `DB.toBigDecimal(obj)`, etc.

**Config:** `Config.java` reads `/app/config/application.properties` (bind-mounted from `./config/` — not bundled in JAR). Environment variables override properties using uppercase+underscore notation: `DB_HOST` → `db.host`, `JWT_SECRET` → `jwt.secret`. Use `config.get(key, default)` and `config.getInt(key, default)`.

Key configuration parameters:
- `server.port` (default: 8080) — HTTP server port
- `db.*` — Database connection settings
- `jwt.secret`, `jwt.access.expiry.seconds` — JWT authentication
- `async.pool.size` (default: 20) — Thread pool size for @Async handlers
- `async.max.body.size` (default: 10485760 = 10MB) — Max body size for async handlers
- `mail.*` — SMTP configuration (disabled by default)

**Auth:** PBKDF2 password hashing (16-byte salt, 310k iterations, SHA-256) in `Auth.java`. Two-token flow: access token (JWT HS256, 15 min) + refresh token (64-char hex, stored in `refresh_tokens` table, 7 days). JWT claims: `sub` (userId), `username`, `ruolo`, `can_admin`, `can_write`, `can_delete`, `must_change_password`.

**Excel utilities** (`dev.jms.util.excel`): `ExcelReader` parses `.xlsx` files; `ExcelImporter` handles full import with pluggable `MappingStrategy` (column mapping) and `NormalizationStrategy` (data normalization). `ExcelAnalyzer` provides preview/validation without loading the full file.

### Frontend (`vite/`)

Vite 6 SPA project with modular architecture. Sources in `vite/src/`, build output in `src/main/resources/static/` (bundled in JAR by Maven).

```
vite/src/
├── index.html           → Entry point (loads router.js)
├── router.js            → SPA router with hash-based navigation
├── modules.config.js    → Module registry + context definitions
├── store.js             → createStore() factory + auth/currentModule stores
├── util.js              → Structured logger + fetchWithRefresh()
└── modules/             → Web components (one per module)
    ├── auth/
    │   ├── index.js     → Module entry point, exports mount()
    │   ├── login.js     → <auth-login> component
    │   └── *.js
    └── home/
        └── index.js     → <home-layout> component
```

**Module pattern:** Each module is a directory in `vite/src/modules/` with an `index.js` that exports a `mount(container)` function. Modules are loaded dynamically by the router based on URL hash.

**Adding a new module:**
1. Create `vite/src/modules/newmodule/index.js` exporting `{ default: { mount(container) {...} } }`
2. Add entry to `modules.config.js`:
   ```javascript
   export const MODULE_CONFIG = {
     newmodule: {
       context: 'public',  // or 'private', 'auth'
       path: '/newmodule',
       title: 'New Module'
     }
   };
   ```
3. Access via `http://localhost:5173/#newmodule`

**Router:** Hash-based SPA router (`router.js`) handles navigation, authentication checks, and module loading. Routes are configured in `modules.config.js`. Fallback pages shown when modules are not found.

**Contexts:** Three built-in contexts:
- `public` — No authentication required
- `private` — Requires authentication, redirects to `/auth` if not logged in
- `auth` — Authentication pages (login, register), redirects to home when already logged in

**`fetchWithRefresh(url, options)`:** Drop-in replacement for `fetch`. On 401, automatically calls the token refresh endpoint, retries the original request once, and redirects to login if refresh fails. Deduplicates concurrent refresh requests.

**Structured logger** (`util.js`): `debug/info/warn/error(module, message, data?)`, `action(module, actionName, data?)`, `api(module, method, endpoint, data?)`, `apiResponse(module, endpoint, ok, data?)`. Logs to console and optionally to backend (`/api/logs`).

### Database Migrations

Flyway migrations in `src/main/resources/db/migration/`. Naming: `V{timestamp}__{description}.sql` where timestamp format is `YYYYMMdd_HHmmss` (e.g., `V20260222_163602__create_users.sql`). The base scaffold has no migrations — they are introduced by modules. When installing a module manually, rename its migration files with a fresh timestamp to avoid Flyway checksum conflicts.

### Docker / Deployment

- Dev container name matches the project directory name, network `<project>-net`
- PostgreSQL: shared `postgres` container (not project-specific), connected to the project network
- Dev ports: API 2310 → 8080, Vite 2350 → 5173, JDWP 5005 → 5005
- Bind mounts: `./` → `/workspace`, `./logs/` → `/app/logs`, `./config/` → `/app/config`
- Production: non-root `appuser` (UID 1001), 512MB memory / 1.0 CPU limits, single JAR

### Template (`jms/`)

`jms/` is the upstream template repository. It contains:
- `lib/dev/jms/util/` — Complete utility library source with async support (25 files: `Handler`, `HandlerAdapter`, `HttpRequest`, `HttpResponse`, `DB`, `Auth`, `Config`, `Json`, `Log`, `Mail`, `Validator`, `Async`, `AsyncExecutor`, plus `excel/` subpackage)
- `template/` — Scaffolding files ready for new projects:
  - `java/App.java` — Entry point with AsyncExecutor initialization
  - `pom.xml` — Maven dependencies
  - `application.properties` — Config with async parameters
  - `vite/` — Frontend base (router, stores, utilities, empty modules/)
  - `.vscode/launch.json` — VSCode debug configuration
- `modules/` — Distributable module archives (`.tar.gz`): `auth-1.0.0.tar.gz`, `home-1.0.0.tar.gz`
- `cmd`, `install.sh`, `release.sh` — Scripts with bench support, synced from project via `cmd sync`
- `docs/` — Documentation including `ASYNC_IMPLEMENTATION_PLAN.md`

### Modules (`modules/`)

Self-contained optional features distributed as `.tar.gz` archives in `modules/`. Each archive contains `java/<module>/`, `gui/<module>/`, `migration/`, and `README.md`.

**Export** a module from the current project:
```bash
cmd module export auth          # → modules/auth.tar.gz
cmd module export auth -v 1.0.0 # → modules/auth-1.0.0.tar.gz
```

**Import** (extract and contextualize, no files installed automatically):
```bash
cmd module import auth-1.0.0.tar.gz   # → modules/auth/ with {{APP_PACKAGE}} replaced
```
Then follow `modules/auth/README.md` to manually copy files and configure `pom.xml`, `application.properties`, `App.java`, and `vite.config.js`.

`cmd sync` propagates `modules/` to `jms/modules/` so new archives are available to other projects.
