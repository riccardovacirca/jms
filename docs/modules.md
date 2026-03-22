# Modules

Module management: installation, export, distribution, removal.

## Installation

```bash
cmd module import auth               # top-level module
cmd module import cti/vonage         # namespaced module
cmd module import auth-1.0.0.tar.gz  # from archive
```

**Automatic operations:**
- Verifica dipendenze dichiarate in `module.json`
- Copia sorgenti Java in `app/module/<name>/` (top-level) o `app/module/<ns>/<name>/` (namespace)
- Riscrive i package Java al path di installazione
- Copia sorgenti GUI in `gui/src/module/<name>/` (top-level) o `gui/src/module/<ns>/<name>/` (namespace)
- Copia migration SQL in `src/main/resources/db/migration/`
- Inserisce la chiamata route in `App.java` dopo `// [MODULE_ROUTES]`
- Aggiunge l'entry in `gui/src/config.js` dopo `// [MODULE_ENTRIES]`
- Scrive il tracker: `src/main/resources/module/<key>/module.json`

**Build dopo l'installazione:**

```bash
cmd gui build && cmd app build
cmd app restart
```

## Disinstallazione

```bash
cmd module remove --name auth
cmd module remove --name cti/vonage
```

Legge il tracker e rimuove: sorgenti Java e GUI, route da `App.java`, entry da `config.js`, tracker.

**Nota:** le migration Flyway non vengono rimosse automaticamente.

## Export e distribuzione

### Export (cartella espansa)

```bash
cmd module export --name auth --vers 1.0.0       # → module/auth-1.0.0/
cmd module export --name auth                    # → module/auth/
cmd module export --name cti/vonage --vers 1.0.0 # → module/cti/vonage-1.0.0/
```

Legge automaticamente `*Routes.java` e `gui/src/config.js`.

### Dist (archivio .tar.gz)

```bash
cmd module dist auth              # module/auth/ → dist/auth-1.0.0.tar.gz
cmd module dist cti/vonage-1.0.0  # module/cti/vonage-1.0.0/ → dist/vonage-1.0.0.tar.gz
```

Path relativo a `module/`; nome e versione letti da `module.json`.

### Sincronizzazione con jms

I moduli in `jms/module/` sono cartelle espanse (non archivi). Dopo un export:

```bash
cp -r module/auth/ jms/module/auth/
```

## Struttura file

**Top-level** (`module/auth/`):
```
module/auth/
  api/           ← Sorgenti Java
  gui/           ← Sorgenti GUI
  migration/     ← Migration SQL Flyway
  config/        ← Proprietà applicazione
  module.json    ← Metadati (auto-generato da export)
```

**Con namespace** (`module/cti/vonage/`):
```
module/cti/vonage/
  api/           ← Java installato in app/module/cti/vonage/
  gui/           ← GUI installata in gui/src/module/cti/vonage/
  migration/
  module.json
```

**Tracker installati:**
```
src/main/resources/module/auth/module.json
src/main/resources/module/cti/vonage/module.json
```

## Schema module.json

**Top-level:**
```json
{
  "name": "auth",
  "version": "1.0.0",
  "dependencies": {},
  "api": {
    "routes": "dev.jms.app.module.auth.Routes.register(router);",
    "config": {}
  },
  "gui": {
    "config": {
      "route": "/user",
      "path": "auth",
      "container": "main",
      "authorization": null,
      "persistent": false,
      "priority": 999,
      "init": null
    }
  },
  "install_notice": null
}
```

**Con namespace** (`cti/vonage`):
- `api.routes` → `dev.jms.app.module.cti.vonage.Routes.register(router, config);`
- `gui.config.path` → `"cti/vonage"` (il router risolve in `gui/src/module/cti/vonage/`)

## Namespace convention

Tutti i moduli installati usano `app/module/` e `gui/src/module/` come base:

| | Top-level (`auth`) | Namespace (`cti/vonage`) |
|---|---|---|
| Java package | `dev.jms.app.module.auth` | `dev.jms.app.module.cti.vonage` |
| Java path | `app/module/auth/` | `app/module/cti/vonage/` |
| GUI path | `gui/src/module/auth/` | `gui/src/module/cti/vonage/` |
| API routes | `/api/auth/...` | `/api/cti/vonage/...` |
| Tracker | `module/auth/module.json` | `module/cti/vonage/module.json` |

Il package Java nei sorgenti del modulo viene riscritto automaticamente all'installazione.

## Dipendenze tra moduli

Dichiarate in `module.json`:

```json
{
  "dependencies": {
    "user": "^1.0.0"
  }
}
```

`cmd module import` verifica la presenza del tracker per ciascuna dipendenza e avvisa se mancante.

## Marker obbligatori nel progetto host

- `App.java`: `// [MODULE_ROUTES]`
- `gui/src/config.js`: `// [MODULE_ENTRIES]`

## Moduli disponibili

| Modulo | Descrizione | Dipendenze |
|--------|-------------|------------|
| `user` | Autenticazione, account, 2FA, reset password | — |
| `header` | Header di navigazione persistente | — |
| `home` | Home page con endpoint `/api/home/hello` | — |
| `crm/contatti` | Gestione contatti con import Excel | `user` |
| `aes` | Cifratura/firma AES e PDF; nessun frontend | — |
| `cti/vonage` | Integrazione Vonage Voice API | `user` |
| `asynctest` | Test async vs blocking (non per produzione) | — |
| `schedulertest` | Test Scheduler (non per produzione) | — |

## Invarianti

- `jms/gui/src/config.js` non deve contenere entry specifiche del progetto
- Ogni modulo Java deve avere una classe `*Routes.java` con metodo statico `register(Router router[, Config config])`
- Il tracker `src/main/resources/module/<key>/module.json` è scritto da `import` e letto da `remove`
