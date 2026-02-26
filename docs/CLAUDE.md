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
cmd app dev              # Watch src/, recompile and restart on changes
cmd app run              # Start JAR in foreground
cmd app start/stop       # Start/stop in background
cmd db                   # Interactive PostgreSQL CLI
cmd db status            # Show app DB config, connection health and migrations
cmd db setup             # Create user and database from .env file
cmd sync                 # Sync sources → jms/ (requires confirmation)
cmd module export <name> [-v 1.2.3]  # Export module to modules/<name>[-version].tar.gz
cmd module import <file.tar.gz>      # Extract module into modules/<name>/ with placeholders replaced
```

### Directly (from `vite/` folder)
```bash
npm run dev              # Dev server on port 5173 (proxy /api → :8080)
npm run build            # Build → src/main/resources/static/
```

### Setup and deploy
```bash
./install.sh                        # Setup dev Docker environment (or restart)
./install.sh --groupid io.mycompany # First install with custom GroupId
./install.sh --postgres             # Install standalone PostgreSQL container
./release.sh                        # Build and deploy production Docker image
```

There are no test or lint commands configured.

### Debugging (VSCode + Java Extension Pack)

```bash
docker exec <container> cmd app debug   # Start backend with JDWP on port 5005
```

Then in VSCode: **Run and Debug** (`Cmd+Shift+D`) → select **"Attach to Docker"** → press `F5`.

The `.vscode/launch.json` is already configured to attach to `localhost:5005`. The app starts immediately (`suspend=n`) and waits for VSCode to connect.

**After every file save**, `cmd app debug` recompiles and restarts the JVM — VSCode loses the connection and must be re-attached with `F5`.

## Architecture

### Backend (`src/main/java/`)

Two packages:

- **`<groupId>/`** — App-specific code: `App.java` (entry point + server setup), plus any installed module subpackages (e.g. `auth/` with `handler/`, `dao/`, `dto/`, `adapter/`).
- **`dev.jms.util/`** — Shared utility library: `Handler`, `HandlerAdapter`, `HttpRequest`, `HttpResponse`, `DB`, `Auth`, `Config`, `Json`, `Log`, `Mail`, `Validator`, and an `excel/` subpackage.

**Handler pattern:** Each route is a class implementing `Handler`. Override `get()`, `post()`, `put()`, or `delete()` as needed — unimplemented methods return 405. `HandlerAdapter` wires the handler to Undertow, dispatches to a blocking thread, and auto-provides `HttpRequest`, `HttpResponse`, and `DB` per request. Uncaught exceptions return 500 JSON automatically. Routes are registered in `App.java` via `PathTemplateHandler`.

**Adding a new route:**
1. Create `src/main/java/<groupId>/handler/FooHandler.java` implementing `Handler`
2. Register in `App.java`: `paths.add("/api/foo", new HandlerAdapter(FooHandler.class, DB.getDataSource()))`

**Response format:** All API responses use the envelope `{"err": boolean, "log": string|null, "out": object|null}`. Build via chained calls: `res.err(false).log(null).out(payload).send()`. Business errors return HTTP 200 with `err: true`; system errors return 500.

**DB layer:** `DB.java` is a thin JDBC wrapper — no ORM. Key methods:
- `db.select(sql, params)` → `List<HashMap<String, Object>>`
- `db.query(sql, params)` → `int` (rows affected)
- `db.cursor(sql, params)` → streaming `Cursor` for large result sets
- `db.begin()` / `db.commit()` / `db.rollback()` — manual transaction control
- Type helpers: `DB.toLong(obj)`, `DB.toLocalDate(obj)`, `DB.toBoolean(obj)`, `DB.toBigDecimal(obj)`, etc.

**Config:** `Config.java` reads `/app/config/application.properties` (bind-mounted from `./config/` — not bundled in JAR). Environment variables override properties using uppercase+underscore notation: `DB_HOST` → `db.host`, `JWT_SECRET` → `jwt.secret`. Use `config.get(key, default)` and `config.getInt(key, default)`.

**Auth:** PBKDF2 password hashing (16-byte salt, 310k iterations, SHA-256) in `Auth.java`. Two-token flow: access token (JWT HS256, 15 min) + refresh token (64-char hex, stored in `refresh_tokens` table, 7 days). JWT claims: `sub` (userId), `username`, `ruolo`, `can_admin`, `can_write`, `can_delete`, `must_change_password`.

**Excel utilities** (`dev.jms.util.excel`): `ExcelReader` parses `.xlsx` files; `ExcelImporter` handles full import with pluggable `MappingStrategy` (column mapping) and `NormalizationStrategy` (data normalization). `ExcelAnalyzer` provides preview/validation without loading the full file.

### Frontend (`vite/`)

Vite 6 MPA project. Sources in `vite/src/`, build output in `src/main/resources/static/` (bundled in JAR by Maven).

```
vite/src/
├── index.html         → redirect to /home
├── store.js           → createStore() factory + auth store (checkAuth, logout) + currentModule store
├── util.js            → structured logger + fetchWithRefresh() (auto token refresh on 401) + mount()
├── common/
│   ├── header.js      → <header-layout> web component
│   └── sidebar.js     → <sidebar-layout> web component
├── home/
│   ├── main.html      → served at /home  (has <base href="/home/">)
│   ├── main.js        → <home-layout> web component
│   └── home.css
└── <module>/          → added per module (e.g. auth/, invoices/)
    ├── main.html      → entry point, has <base href="/<module>/">
    ├── main.js        → web component
    └── *.css
```

**Web component pattern:** Each page is a custom element (`connectedCallback` + `_render()`), reactive to store changes via `subscribe()`. Bootstrap imported via npm in each `index.js`.

**Adding a new page:** Create `vite/src/newpage/main.{html,js,css}`, add `<base href="/newpage/">` to the HTML, add the entry to `rollupOptions.input` in `vite.config.js`, add a route-rewrite for the dev server, and serve it from Java.

**`fetchWithRefresh(url, options)`:** Drop-in replacement for `fetch`. On 401, automatically calls the token refresh endpoint, retries the original request once, and redirects to the login page if refresh fails. Deduplicates concurrent refresh requests.

**Structured logger** (`util.js`): `logger.debug/info/warn/error(module, message, data?)`, `logger.action(module, actionName, data?)`, `logger.api(module, method, endpoint, data?)`.

**Dev routing:** `route-rewrite` plugin in `vite.config.js` maps clean URLs to HTML files (e.g. `/home` → `/home/main.html`) server-side, leaving the URL unchanged. The `<base>` tag in each HTML ensures relative imports resolve correctly. Add a new rewrite rule when adding a new page.

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
- `lib/` — utility library source (`dev.jms.util.*`), synced from the project
- `template/` — scaffolding files: `App.java` (HelloWorld skeleton), `pom.xml`, `application.properties`, `.vscode/launch.json`
- `modules/` — distributable module archives (`.tar.gz`)
- `cmd`, `install.sh`, `release.sh` — scripts, synced from the project via `cmd sync`

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
