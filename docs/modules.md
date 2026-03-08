# Modules

Module management and maintenance procedures.

## Installation

### Command

```bash
cmd module import modules/<name>-x.y.z.tar.gz
```

**Automatic operations:**
- Extract archive to `modules/<name>/`
- Replace `{{APP_PACKAGE}}` placeholder with actual package
- Copy Java sources to `src/main/java/<package>/<name>/`
- Copy GUI sources to `vite/src/modules/<name>/`
- Copy migration SQL to `src/main/resources/db/migration/`
- Append module config to `config/application.properties` (if not present)
- Verify dependencies from `module.json`

**Manual steps after import:**

1. `pom.xml` - Add dependencies (if module requires external Java libraries)
2. `App.java` - Add imports for module handlers
3. `App.java` - Add routes with `paths.add(...)`
4. `config.js` - Add module entry to `MODULE_CONFIG`

Complete instructions printed in README after import.

**Build after installation:**

```bash
cmd gui build && cmd app build
cmd app restart
```

## Structure

**Files locations:**

```
vite/src/modules/<name>/       ← Live application files (modified during development)
modules/<name>/gui/<name>/     ← GUI source copy for archiving
modules/<name>/java/<name>/    ← Java source
modules/<name>/migration/      ← Flyway migrations
modules/<name>/module.json     ← Metadata with dependencies
modules/<name>/README          ← Manual installation steps
modules/<name>-x.y.z.tar.gz    ← Distributable archive
jms/modules/<name>-x.y.z.tar.gz ← Template copy (always synchronized)
```

## Base Files vs Module Files

**Base files** (DO NOT modify when working on modules):

- `vite/src/store.js` - Nanostores (authorized, user)
- `vite/src/router.js` - Multi-container SPA router
- `vite/src/config.js` (app) - Contains installed module entries
- `jms/vite/src/config.js` (template) - Contains only the base `status` entry; must never contain project-specific module entries

**Module files** (modify in both locations):

| Live | Archive |
|------|---------|
| `vite/src/modules/<name>/*.js` | `modules/<name>/gui/<name>/*.js` |

**Examples:**
- `vite/src/modules/auth/init.js` → `modules/auth/gui/auth/init.js`
- `vite/src/modules/auth/login.js` → `modules/auth/gui/auth/login.js`
- `vite/src/modules/header/component.js` → `modules/header/gui/header/component.js`
- `vite/src/modules/home/component.js` → `modules/home/gui/home/component.js`

## Update Workflow

1. Modify live file in `vite/src/modules/<name>/`
2. Apply same change to `modules/<name>/gui/<name>/`
3. Rebuild archive
4. Verify checksums

## Rebuild Archives

**Inside container:**

```bash
cmd module export <name> -v x.y.z
```

**Outside container** (sync to template):

```bash
cp modules/<name>-x.y.z.tar.gz jms/modules/<name>-x.y.z.tar.gz
```

**Verify identity:**

```bash
md5 modules/<name>-x.y.z.tar.gz jms/modules/<name>-x.y.z.tar.gz
```

MD5 must be identical.

**Example for all modules:**

Inside container:
```bash
cmd module export auth -v 1.1.0
cmd module export header -v 1.0.0
cmd module export home -v 1.0.0
cmd module export contatti -v 1.0.0
```

Outside container:
```bash
cp modules/auth-1.1.0.tar.gz jms/modules/auth-1.1.0.tar.gz
cp modules/header-1.0.0.tar.gz jms/modules/header-1.0.0.tar.gz
cp modules/home-1.0.0.tar.gz jms/modules/home-1.0.0.tar.gz
cp modules/contatti-1.0.0.tar.gz jms/modules/contatti-1.0.0.tar.gz
```

Verify:
```bash
md5 modules/*.tar.gz jms/modules/*.tar.gz
```

## Dependencies

Module dependencies managed via `module.json`:

```json
{
  "name": "contatti",
  "version": "1.0.0",
  "dependencies": {
    "auth": "^1.0.0"
  }
}
```

Import verifies dependencies and warns if missing modules detected. User can confirm to proceed anyway or abort installation.

## Available Modules

**`auth-1.1.0.tar.gz`**
- Complete authentication system
- Login, session, 2FA, password management
- Dependencies: none

**`header-1.0.0.tar.gz`**
- Persistent navigation header
- Auth-aware, user display, login/logout
- Priority: 1 (loads first)
- Dependencies: none

**`home-1.0.0.tar.gz`**
- Home page with API hello endpoint
- Dependencies: none

**`contatti-1.0.0.tar.gz`**
- Contact management module
- Dependencies: auth ^1.0.0

## Invariants

- Always modify both copies of module files (live + archive)
- Use `cmd module export` to rebuild archives (handles `COPYFILE_DISABLE=1`, includes `config/`)
- `modules/*.tar.gz` and `jms/modules/*.tar.gz` must have identical MD5
- `jms/vite/src/config.js` must not contain project-specific module entries (only `status`)
- Do not use `cmd sync` to propagate app-specific files to `jms/`
