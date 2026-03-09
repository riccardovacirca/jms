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
cmd sync                 # Sync sources → jms/ (requires confirmation)
cmd module export --name <nome> [--vers 1.2.3]  # Esporta modulo in cartella modules/<nome>[-version]/
cmd module dist --name <nome-versione>           # Comprime cartella → modules/<nome-versione>.tar.gz
cmd module import <archivio.tar.gz | cartella>   # Installa modulo (accetta archivio o cartella estratta)
cmd module remove --name <nome>                  # Disinstalla modulo usando il tracker installato
cmd bench [options]      # Run siege benchmark (options passed to siege, log to bench/siege-YYYYMMDD-HHMMSS.log)
```

**Note:** The `cmd` script is located in `bin/cmd` and is available in PATH inside the Docker container.

### Directly (from `vite/` folder)
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

There are no test or lint commands configured.

## Typical Development Workflow

```bash
# Start the development environment
./install.sh                 # First time or restart existing container
docker exec -it hello bash   # Enter dev container (container name matches PROJECT_NAME in .env)

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
# - Frontend: http://localhost:2350 (VITE_PORT_HOST from .env)
# - API: http://localhost:2310 (API_PORT_HOST from .env)
```

### Debugging (VSCode + Java Extension Pack)

**Prerequisites:** Install the [Java Extension Pack](https://marketplace.visualstudio.com/items?itemName=vscjava.vscode-java-pack) in VSCode.

**Step-by-step workflow:**

1. **Start the backend in debug mode** (inside the container):
   ```bash
   cmd app debug   # Starts with JDWP on port 5005
   ```
   Output shows:
   ```
   [debug] Remote debug enabled on port 5005
   [debug] Attach your debugger to localhost:5005
   [dev] App started (PID: ...)
   ```

2. **Set breakpoints** in VSCode:
   - Open a handler (e.g., `src/main/java/dev/jms/app/home/handler/HelloHandler.java`)
   - Click left of the line number to add a red breakpoint dot

3. **Attach the debugger**:
   - Open **Run and Debug** panel (`Cmd+Shift+D` or `Ctrl+Shift+D`)
   - Select **"Attach to Docker"** from the dropdown
   - Press **F5** or click the green play button
   - The orange debug toolbar appears when connected

4. **Trigger the breakpoint from the browser**:
   - Navigate to `http://localhost:2350/#/home`
   - The frontend makes a fetch to `/api/home/hello`
   - **Execution stops at your breakpoint** in the Java handler
   - Inspect variables, call stack, step through code (F10, F11)
   - Press F5 to continue execution

**How it works:**
- `cmd app debug` starts the JVM with JDWP agent: `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005`
- The container exposes port 5005 → host's 5005
- VSCode connects to `localhost:5005` via configuration in `.vscode/launch.json`
- HTTP requests from the browser → Undertow → handlers → **breakpoints trigger**

**Hot reload behavior:**
After every `.java` file save, `cmd app debug` recompiles and **restarts the JVM**. This terminates the JDWP connection. You must **re-attach the debugger** by pressing `F5` in VSCode's Run & Debug panel. Breakpoints remain configured and will work immediately after reconnecting.

**VSCode configuration** (`.vscode/launch.json`):
```json
{
  "type": "java",
  "name": "Attach to Docker",
  "request": "attach",
  "hostName": "localhost",
  "port": 5005
}
```

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
- `mail.*` — SMTP configuration (disabled by default, set `mail.enabled=true` to enable)

**Auth:** PBKDF2 password hashing (16-byte salt, 310k iterations, SHA-256) in `Auth.java`. Two-token flow: access token (JWT HS256, 15 min) + refresh token (64-char hex, stored in `refresh_tokens` table, 7 days). JWT claims: `sub` (accountId), `username`, `ruolo`, `permissions` (List<String>, e.g. `["can_admin","can_write","can_delete"]`), `must_change_password`.

**Excel utilities** (`dev.jms.util.excel`): `ExcelReader` parses `.xlsx` files; `ExcelImporter` handles full import with pluggable `MappingStrategy` (column mapping) and `NormalizationStrategy` (data normalization). `ExcelAnalyzer` provides preview/validation without loading the full file.

### Frontend (`vite/`)

Vite 6 SPA project with modular multi-container architecture. Sources in `vite/src/`, build output in `src/main/resources/static/` (bundled in JAR by Maven).

```
vite/src/
├── index.html           → Entry point with multi-area layout (header, main, footer)
├── router.js            → Multi-container SPA router with persistent modules support
├── config.js            → Module configuration template (no project-specific entries)
├── init.js              → Global app initialization (fetch interceptor)
├── store.js             → Nanostores-based state management (authorized, user stores)
└── modules/             → Empty placeholder — modules added via cmd module import
    └── .gitkeep
```

**Multi-container layout** (`index.html`): Defines three container areas:
- `#header` — For persistent modules like navigation bars
- `#main` — For dynamic page content
- `#footer` — For persistent modules like footers (optional)

**Module pattern:** Each module is a directory in `vite/src/modules/` with an `index.js` that exports a `mount(container)` function. The router mounts each module in its designated container based on configuration.

**Module configuration** (`config.js`): All modules declare these attributes explicitly:
```javascript
export const MODULE_CONFIG = {
  moduleName: {
    route: '/path' | null,             // URL hash route or null (not navigable, e.g. header)
    path: 'dirname' | null,            // Folder under vite/src/modules/ or null (no frontend module)
    container: 'main' | 'header' | 'footer',  // DOM container ID
    authorization: null | { redirectTo: '/route' },  // Access control
    persistent: true | false,          // persistent requires path !== null
    priority: 999,                     // Load order (lower = first, only for persistent)
    init: null | async function        // Initialization function
  }
};

// Selects which MODULE_CONFIG key to load when no hash is present
export const DEFAULT_MODULE = 'status';
```

`path: null` is reserved for routes handled entirely by the backend with no frontend module to mount. `persistent: true` with `path: null` is a configuration error — the router throws explicitly.

A new installation includes `status` as the only pre-installed frontend module (`vite/src/modules/status/`), which calls `/api/status` and displays the result. It is the `DEFAULT_MODULE`.

**Adding a new module:**
1. Create `vite/src/modules/newmodule/index.js` exporting `{ default: { mount(container) {...} } }`
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

**Authorization model:**
- `authorization: null` — Publicly accessible
- `authorization: { redirectTo: '/path' }` — Protected; redirects unauthorized users

**Global fetch interceptor** (`init.js`): Intercepts all `fetch()` calls and checks for authentication errors in API responses. If `err: true` with `log` matching known auth errors (`'Non autenticato'`, `'Token non valido o scaduto'`), sets `authorized` store to `false`, triggering router redirect.

**State management** (`store.js`): Uses [nanostores](https://github.com/nanostores/nanostores) (v0.11.3) for reactive state. Exports:
- `authorized` store (boolean) — Authentication state
- `user` store (object|null) — Current user data
Router and modules subscribe to these stores for reactive updates.

### Database Migrations

Flyway migrations in `src/main/resources/db/migration/`. Naming: `V{timestamp}__{description}.sql` where timestamp format is `YYYYMMdd_HHmmss` (e.g., `V20260222_163602__create_users.sql`). The base scaffold has no migrations — they are introduced by modules.

### Docker / Deployment

- Dev container name matches `PROJECT_NAME` in `.env` (currently: `hello`)
- Network: `<project>-net` (e.g., `hello-net`)
- PostgreSQL: shared `postgres` container (not project-specific), connected to the project network
- Dev ports (configurable in `.env`):
  - API: `API_PORT_HOST` (2310) → 8080
  - Vite: `VITE_PORT_HOST` (2350) → 5173
  - JDWP: `DEBUG_PORT_HOST` (5005) → 5005
  - PostgreSQL: `PGSQL_PORT_HOST` (2340) → 5432
- Bind mounts: `./` → `/workspace`, `./logs/` → `/app/logs`, `./config/` → `/app/config`
- Production: non-root `appuser` (UID 1001), 512MB memory / 1.0 CPU limits, single JAR

### Template (`jms/`)

`jms/` is the upstream template repository from which projects are cloned. It may be present as a subdirectory of a project root to allow propagating improvements, fixes, or enhancements back to the template so that all future (and other existing) projects can benefit.

**Key distinction — project files vs. template files:**
- Files in the project root are project-specific and fully instantiated (no placeholders).
- Files in `jms/` may contain placeholders (e.g. `{{PROJECT_NAME}}`, `{{DB_HOST}}`) that `install.sh` substitutes when creating a new project. Never copy `jms/` files into the project directly without first removing or resolving those placeholders.

**What can be propagated to `jms/`:**
- Changes that are already fully generic — e.g. additions or fixes to `dev.jms.util` library classes, improvements to `bin/cmd`, `install.sh`, or `release.sh`.
- Changes that are project-specific must be **generalized first** (re-expressed with placeholders or made fully project-agnostic) before being written into `jms/`.

**What must NOT be propagated as-is:**
- Any code that references project-specific names, credentials, routes, or domain logic — these belong only in the project root and must be abstracted before touching `jms/`.

Clone it with the project name to start a new project — the Java source structure is already in its final position. It contains:
- `src/main/java/dev/jms/util/` — Complete utility library source (25 files: `Handler`, `HandlerAdapter`, `HttpRequest`, `HttpResponse`, `DB`, `Auth`, `Config`, `Json`, `Log`, `Mail`, `Validator`, `ValidationException`, `Async`, `AsyncExecutor`, plus `excel/` subpackage)
- `src/main/java/dev/jms/app/App.java` — Entry point with AsyncExecutor initialization
- `src/main/resources/` — `logback.xml`, empty `static/` and `db/migration/` directories
- `pom.xml` — Maven dependencies (groupId: `dev.jms.app`)
- `config/application.properties` — Config template with placeholders substituted by `install.sh`
- `vite/` — Frontend base template (router, stores, init, empty modules/), copied to `vite/` by `install.sh` when creating a new project
- `modules/` — Module sources in expanded folder format: `auth-1.1.0/`, `header-1.0.0/`, `home-1.0.0/`, `contatti-1.0.0/`, `users-1.0.0/`
- `bin/cmd`, `install.sh`, `release.sh` — Scripts with bench support, synced from project via `cmd sync`
- `docs/` — Documentation including architecture details

### Modules (`modules/`)

Self-contained optional features distributed as `.tar.gz` archives in `modules/`. Each archive contains:
- `java/<module>/` — Java handlers, DAOs, DTOs, and `*Routes.java`
- `gui/<module>/` — Frontend module sources
- `migration/` — Flyway SQL migrations
- `config/` — Application properties
- `module.json` — Module metadata (auto-generated by `cmd module export`)

**`module.json` schema:**
```json
{
  "name": "auth",
  "version": "1.1.0",
  "dependencies": {},
  "api": {
    "routes": "dev.jms.app.auth.Routes.register(paths, ds, config);",
    "config": {}
  },
  "gui": {
    "config": {
      "route": "/auth", "path": "auth", "container": "main",
      "authorization": null, "persistent": false, "priority": 999, "init": null
    }
  },
  "install_notice": null
}
```

**Export** un modulo dal progetto corrente in cartella non compressa:
```bash
cmd module export --name auth --vers 1.1.0   # → modules/auth-1.1.0/
cmd module export --name auth                # → modules/auth/
```
Export legge automaticamente `*Routes.java` (per i metadati API) e `vite/src/config.js` (per l'entry GUI).

**Dist** comprime una cartella di modulo in archivio distribuibile:
```bash
cmd module dist --name auth-1.1.0   # modules/auth-1.1.0/ → modules/auth-1.1.0.tar.gz
```

**Import** installa un modulo da archivio `.tar.gz` o da cartella estratta:
```bash
cmd module import modules/auth-1.1.0.tar.gz   # da archivio
cmd module import modules/auth-1.1.0           # da cartella
```
Import automaticamente:
1. Verifica dipendenze da `module.json` (avvisa se mancanti, controlla tracker installati)
2. Copia sorgenti Java, GUI, migration SQL
3. Inserisce la chiamata `api.routes` in `App.java` dopo `// [MODULE_ROUTES]`
4. Inserisce l'entry `gui.config` in `vite/src/config.js` dopo `// [MODULE_ENTRIES]`
5. Scrive il tracker in `src/main/resources/modules/<nome>/module.json`

Solo un **passo manuale** può restare: `pom.xml` — aggiungere dipendenze Java esterne se richieste dal modulo.

**Remove** disinstalla un modulo leggendo il tracker:
```bash
cmd module remove --name auth
```
Rimuove sorgenti Java, GUI, route da `App.java`, entry da `config.js`, tracker. Le migration Flyway non vengono rimosse automaticamente (avviso esplicito).

**Tracker installati:** dopo ogni import riuscito viene scritto `src/main/resources/modules/<nome>/module.json`. Le dipendenze vengono verificate contro questi tracker. `module remove` richiede il tracker per operare.

**Markers required in host project:**
- `App.java`: `// [MODULE_ROUTES]` prima delle registrazioni di route dei moduli
- `vite/src/config.js`: `// [MODULE_ENTRIES]` dopo l'entry `status` dentro `MODULE_CONFIG`

**Each Java module must have a `*Routes.java` class** at the module root (e.g. `auth/Routes.java`) with a static `register(PathTemplateHandler paths, DataSource ds)` method. Modules that need `Config` add it as a third parameter.

**Available modules** (in `modules/`):
- `auth-1.1.0.tar.gz` — Complete authentication system with token-based password reset (no dependencies)
- `header-1.0.0.tar.gz` — Persistent navigation header (no dependencies)
- `home-1.0.0.tar.gz` — Simple home page with API hello endpoint (no dependencies)
- `contatti-1.0.0.tar.gz` — Contact management module (requires: auth)

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

**Response builder:** Always chain in this exact order before `send()`: `status() → contentType() → [cookie()...] → err() → log() → out()`. Each method goes on its own line, continuation lines indented so the `.` aligns 3 spaces past the `res` start:
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

## Environment Configuration

The `.env` file contains project configuration used by installation and release scripts:

**Key variables:**
- `PROJECT_NAME` — Container and database name (default: directory name)
- `API_PORT_HOST`, `VITE_PORT_HOST` — Host ports for development access
- `PGSQL_*` — PostgreSQL connection parameters
- `JAVA_VERSION` — Java version for Docker images (default: 21)
- `ARTIFACT_VERSION` — Application version for releases (default: 1.0.0)

Environment variables can override application properties. For example, `DB_HOST=localhost` in environment will override `db.host` in `config/application.properties`.
