# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Full-stack Java + vanilla JavaScript application. Backend runs on Undertow (Java 21), frontend is built with Vite. PostgreSQL is managed via Flyway migrations. The app ships as a single executable JAR serving both the API and static frontend.

## Common Commands

### Via `cmd` (inside Docker container `ciao`)
```bash
cmd gui build            # Build frontend → src/main/resources/static/
cmd gui run              # Dev server Vite in foreground (port 5173)
cmd app build            # Compile Java (Maven) → target/service.jar
cmd app dev              # Watch src/, recompile and restart on changes
cmd app run              # Start JAR in foreground
cmd app start/stop       # Start/stop in background
cmd db                   # Interactive PostgreSQL CLI
cmd db setup             # Create user and database from .env file
cmd sync                 # Sync sources → jms/ (requires confirmation)
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

## Architecture

### Backend (`src/main/java/`)

Two packages:

- **`com.example/`** — App-specific code: `App.java` (entry point + server setup), `Config.java`, and `handler/` subpackage with one class per route.
- **`dev.jms.util/`** — Shared utility library: `Handler`, `HandlerAdapter`, `HttpRequest`, `HttpResponse`, `DB`, `Auth`, `Json`, `Log`, and an `excel/` subpackage.

**Handler pattern:** Each route is a class implementing `Handler`. Override `get()`, `post()`, `put()`, or `delete()` as needed — unimplemented methods return 405. `HandlerAdapter` wires the handler to Undertow, dispatches to a blocking thread, and auto-provides `HttpRequest`, `HttpResponse`, and `DB` per request. Uncaught exceptions return 500 JSON automatically. Routes are registered in `App.java` via `PathTemplateHandler`.

**Adding a new route:**
1. Create `src/main/java/com/example/handler/FooHandler.java` implementing `Handler`
2. Register in `App.java`: `router.add("/api/foo", new HandlerAdapter(FooHandler.class))`

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
├── index.html       → redirect to /home
├── store.js         → createStore() factory + auth store (checkAuth, logout) + currentModule store
├── util.js          → structured logger + fetchWithRefresh() (auto token refresh on 401) + mount()
├── header/          → <header-layout> web component
├── sidebar/         → <sidebar-layout> web component
├── auth/
│   ├── index.html   → served by Java at /auth  (has <base href="/auth/">)
│   ├── index.js     → <auth-layout> web component
│   └── index.css
└── home/
    ├── index.html   → served by Java at /home  (has <base href="/home/">)
    ├── index.js     → <home-layout> web component + checkAuth()
    └── index.css
```

**Web component pattern:** Each page is a custom element (`connectedCallback` + `_render()`), reactive to store changes via `subscribe()`. Bootstrap imported via npm in each `index.js`.

**Adding a new page:** Create `vite/src/newpage/index.{html,js,css}`, add `<base href="/newpage/">` to the HTML, add the entry to `rollupOptions.input` in `vite.config.js`, add a route-rewrite for the dev server, and serve it from Java.

**`fetchWithRefresh(url, options)`:** Drop-in replacement for `fetch`. On 401, automatically calls `/api/auth/refresh`, retries the original request once, and redirects to `/auth` if refresh fails. Deduplicates concurrent refresh requests.

**Structured logger** (`util.js`): `logger.debug/info/warn/error(module, message, data?)`, `logger.action(module, actionName, data?)`, `logger.api(module, method, endpoint, data?)`.

**Dev routing:** `route-rewrite` plugin in `vite.config.js` maps `/home` → `/home/index.html` and `/auth` → `/auth/index.html` (server-side, URL unchanged). The `<base>` tag in each HTML ensures relative imports resolve correctly.

### Database Migrations

Flyway migrations in `src/main/resources/db/migration/`. Naming: `V{timestamp}__{description}.sql` where timestamp format is `YYYYMMdd_HHmmss` (e.g., `V20260222_163602__auth.sql`). Migrations in `jms/template/sql/` are copied with the original name unchanged. Schema: `roles` → `users` → `refresh_tokens`.

### Docker / Deployment

- Dev container: `ciao`, network `ciao-net`
- PostgreSQL: shared `postgres` container (not project-specific), connected to `ciao-net`
- Dev ports: API 2310 → 8080, Vite 2350 → 5173
- Bind mounts: `./` → `/workspace`, `./logs/` → `/app/logs`, `./config/` → `/app/config`
- Production: non-root `appuser` (UID 1001), 512MB memory / 1.0 CPU limits, single JAR

### Template (`jms/`)

`jms/` contains the utility library (`lib/`), Vite template static files (`static/`), initial migrations (`template/sql/`), Java template files (`template/java/`), and scripts (`cmd`, `install.sh`, `release.sh`). Updated via `cmd sync`.
