# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Full-stack Java + vanilla JavaScript application. Backend runs on Undertow (Java 21), frontend is built with Vite. PostgreSQL is managed via Flyway migrations. The app ships as a single executable JAR serving both the API and static frontend.

## Common Commands

```bash
# Backend (inside dev Docker container)
cmd app run              # Watch src/, recompile and restart on changes
cmd app debug            # Same as run + JDWP debugger on port 5005
cmd app build            # Compile Java → target/service.jar
cmd app start/stop/restart/status/clean

# Frontend
cmd gui run              # Vite dev server (port 5173)
cmd gui build            # Build → src/main/resources/static/
cmd gui clean

# Database
cmd db                   # Interactive psql
cmd db status            # Config, connection health, migrations
cmd db setup/reset

# Modules
cmd module export --name <name> [--vers x.y.z]
cmd module install <path>          # path relative to module/
cmd module uninstall <key>
cmd module dist <path>             # → dist/<name>-<vers>.tar.gz
cmd module cli <module> <script>
cmd module test <module> <script>

# Other
cmd snap / snap restore            # Encrypted snapshot (config + DB)
cmd bench [options]                # Siege benchmark
```

```bash
# Setup / deploy (from host)
./install.sh                       # Setup dev Docker environment
./install.sh --postgres/--mailpit/--vscode
./release.sh [-v 1.2.0]           # Build production Docker image
./app.sh --start/--stop/--restart/--status
```

Dev workflow: `./install.sh` → `docker exec -it <PROJECT_NAME> bash` → `cmd app run` (T1) + `cmd gui run` (T2).

Debugging: `cmd app debug` + VSCode "Attach to Docker" (F5). "Fullstack" compound config attaches Java + Firefox simultaneously. After each Java save the JVM restarts — re-attach with F5.

## Architecture

### Backend (`src/main/java/`)

- **`dev.jms.app/`** — App code: `App.java` + installed module subpackages under `module/`
- **`dev.jms.util/`** — Shared library: `Handler`, `HandlerAdapter`, `HttpRequest`, `HttpResponse`, `DB`, `Auth`, `Config`, `Json`, `Log`, `Mail`, `Validator`, `ValidationException`, `UnauthorizedException`, `AsyncExecutor`, `Scheduler`, `Excel`, `PDF`, `HTML2PDF`, `File`, `Router`, `AuditLog`, `JWTBlacklist`, `RateLimiter`, `Session`, `Role`, `Permission`, `Cookie`

**Handler pattern:** `RouteHandler` = `(HttpRequest, HttpResponse, Session, DB) throws Exception`. Methods registered as references in `App.java` or module `Routes.java`. `HandlerAdapter` auto-provides all four arguments per request; uncaught exceptions → HTTP 500.

**Async routes:** `router.async()` dispatches to `AsyncExecutor` thread pool (not Undertow workers). Use for slow DB queries or external calls.

**Response format:** `{"err": boolean, "log": string|null, "out": object|null}`. Chain: `res.status(200).contentType("application/json").err(false).log(null).out(payload).send()` — each call on its own line, `.` aligned 3 spaces past `res`. Business errors → HTTP 200 `err: true`; system errors propagate → HTTP 500.

**DB:** Thin JDBC wrapper. `db.select(sql, params)` → `List<HashMap<String,Object>>`, `db.query(sql, params)` → `int`, `db.cursor(sql, params)` → streaming. `db.begin()/commit()/rollback()`. Always assign SQL to a local `sql` variable. Use `?` placeholders (not `$1`).

**Config:** Reads `/app/config/application.properties` (bind-mounted, not in JAR). Env vars override with uppercase+underscore: `DB_HOST` → `db.host`. `config.get(key, default)`, `config.getInt(key, default)`. Properties prefixed `public.*` are exposed via `GET /api/config`.

**Auth:** PBKDF2 hashing + two-token flow: JWT HS256 access token (15 min) + 64-char hex refresh token (7 days, stored in `refresh_tokens`). Claims: `sub`, `username`, `ruolo`, `ruolo_level`, `must_change_password`.

**Session:** Per-request object injected by `HandlerAdapter`. JWT validated lazily and cached. `session.require(Role, Permission)` throws `UnauthorizedException`. Server-side storage: `ConcurrentHashMap`, 30 min sliding TTL, keyed by `session_id` cookie. Roles: `GUEST(0)`, `USER(1)`, `ADMIN(2)`, `ROOT(3)`. `GUEST + READ` always permitted.

**Scheduler:** JobRunr backed by PostgreSQL. `Scheduler.register("id", "0 2 * * *", Handler::staticMethod)` — target must be static, parameterless.

**Utilities:** `Excel.read(InputStream)` / `Excel.analyze()` / `Excel.Importer`; `PDF.load(InputStream)` + `pdf.save(OutputStream)`; `HTML2PDF.convert(String html)`; `File.*` (path-sanitized read/write/copy/move/delete/list/hash); `AuditLog.*` → `audit_log` table; `RateLimiter.configure(maxAttempts, windowMs)`.

### Frontend (`gui/src/`)

Vite 6 SPA. Build output → `src/main/resources/static/` (bundled in JAR).

- `index.html` — Three container areas: `#header`, `#main`, `#footer`
- `router.js` — Hash-based multi-container router; `import.meta.glob` pre-registers all modules (required for namespaced paths like `cti/vonage` — never use template literal dynamic imports)
- `config.js` — Module registry (`MODULE_CONFIG` + `DEFAULT_MODULE`)
- `init.js` — Fetch interceptor: on auth error attempts token refresh, retries original request, else sets `authorized = false`
- `store.js` — Nanostores: `authorized` (boolean), `user` (object|null)

**Module config schema:**
```javascript
moduleName: {
  route: '/path' | null,       // null = not navigable
  path: 'name' | 'ns/name' | null,  // null = backend-only route
  container: 'main' | 'header' | 'footer',
  authorization: null | { redirectTo: '/route' },
  persistent: true | false,    // persistent requires path !== null
  priority: 999,               // only for persistent (lower = first)
  init: null | true | async fn // true = auto-import ./module/<path>/init.js
}
```

**Router:** Persistent modules mount once at startup. Dynamic modules mount/unmount on navigation. Default module loads when no hash. `/#/` redirects to `/`.

### Database Migrations

`src/main/resources/db/migration/`. Naming: `V{YYYYMMdd_HHmmss}__{description}.sql`. Base scaffold has no migrations — introduced by modules.

### Modules (`module/`)

Each module folder:
- `api/` — Java sources (`handler/`, `dao/`, `dto/`, `helper/`, `Routes.java`)
- `gui/` — Frontend sources
- `db/install/` — Flyway migrations; `db/uninstall/` — DROP scripts
- `module.json` — Metadata (name, version, dependencies, routes call, gui config)
- `profile.xml` — Maven profile for module-specific external dependencies (optional)
- `cli/install.sh` / `cli/uninstall.sh` — Pre/post hooks (receive `$1=WORKSPACE`, `$2=MODULE_KEY`)

**Markers required in host project:**
- `App.java`: `// [MODULE_ROUTES]`
- `gui/src/config.js`: `// [MODULE_ENTRIES]`

Each module needs a `Routes.java` with `static register(Router router)` or `register(Router router, Config config)`.

`cmd module install` automatically: verifies transitive deps, copies Java + GUI sources, applies migrations, injects route call (wrapped in try-catch) + config entry + Maven profile, writes tracker to `src/main/resources/module/<key>/module.json`, regenerates `installed.json`.

`cmd module uninstall` reverses all steps, runs DROP scripts, runs `mvn clean`.

Install paths: `app/module/<name>/` (Java, pkg `dev.jms.app.module.<name>`), `gui/src/module/<name>/` (GUI). Namespaced: `app/module/<ns>/<name>/`, pkg `dev.jms.app.module.<ns>.<name>`.

**Available modules:**
- `audit` — `audit_log` table; migration-only; required by `user`
- `user` — Auth + account management; route `/user`; requires `audit`. Default accounts: `root/root123.`, `admin/admin123.`, `operator/operator123.`
- `header` — Persistent nav header
- `home` — Simple home page + hello endpoint
- `dashboard` — Dashboard with stats/sidebar; route `/dashboard`
- `sales` — Contact/list/campaign management, Excel import; route `/sales`; requires `user`
- `cti/vonage` — Vonage Voice API CTI bar (persistent footer); requires `user`. Webhooks: answer → `/api/cti/vonage/answer`, event → `/api/cti/vonage/event`. Operator-session TTL: 30 min, refreshed every 13 min; cleanup job runs every minute.
- `aes` — PDF/HTML signature field insertion + Namirial/Savino integration; no frontend; no deps
- `asynctest`, `schedulertest` — Dev/test only

### Java Package

Always `dev.jms.app` — never changes, never a placeholder.

## Coding Style

Full rules in `docs/dsl/java.md` and `docs/dsl/javascript.md`.

### Java

- 2-space indent; class/method `{` on own line; control flow `{` on same line
- No empty line after opening brace
- Declare all variables at top of scope; declaration and initialization always separate; never `var`; no wildcard generics
- Single exit point per method (no early returns in void; one return in typed methods)
- Business errors → `err: true` + WARN log (no stack trace); system errors → propagate → HTTP 500
- `Validator` is static-only (`final`, private constructor): `Validator.required(value, "field")`
- Javadoc required on all public and package-private classes and methods

### JavaScript

- Semicolons everywhere; one statement per line; extract intermediates over chaining
- All named exports in a single `export { }` block at end of file
- JSDoc required on exported/public functions
- Group module-level state in named objects (not separate `let`s)
- Lit components: extend `LitElement`, `createRenderRoot() { return this; }`, reactive state via `static properties`, subscribe in `connectedCallback` / unsubscribe in `disconnectedCallback`, never call `render()` explicitly
- File name = class name (PascalCase for components, lowercase for `index.js`/`init.js`)
- Namespace via folder structure, not class name prefixes
- Custom element tag: `module-ComponentName` kebab-case

## Environment

`.env` drives Docker setup and can override `application.properties` via uppercase+underscore env vars. Key: `PROJECT_NAME`, `API_PORT_HOST`, `VITE_PORT_HOST`, `PGSQL_*`, `JAVA_VERSION`, `ARTIFACT_VERSION`.
