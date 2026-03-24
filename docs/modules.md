# Modules

Module management: installation, export, distribution, removal.

## Installation

```bash
cmd module import auth               # top-level module
cmd module import cti/vonage         # namespaced module
cmd module import auth-1.0.0.tar.gz  # from archive
cmd module import auth --force       # bypassa verifica dipendenze
```

**Automatic operations:**
- Verifica dipendenze transitive (ricorsiva) dichiarate in `module.json` — **bloccante** se mancanti
- Copia sorgenti Java in `app/module/<name>/` (top-level) o `app/module/<ns>/<name>/` (namespace)
- Riscrive i package Java al path di installazione
- Copia sorgenti GUI in `gui/src/module/<name>/` (top-level) o `gui/src/module/<ns>/<name>/` (namespace)
- Copia migration SQL in `src/main/resources/db/migration/`
- Inserisce la chiamata route in `App.java` dopo `// [MODULE_ROUTES]`
- Aggiunge l'entry in `gui/src/config.js` dopo `// [MODULE_ENTRIES]`
- Scrive il tracker: `src/main/resources/module/<key>/module.json`
- Rigenera il manifest `src/main/resources/module/installed.json`

**Build dopo l'installazione:**

```bash
cmd gui build && cmd app build
cmd app restart
```

## Disinstallazione

```bash
cmd module remove --name auth
cmd module remove --name cti/vonage
cmd module remove --name auth --force   # bypassa controllo dipendenze inverse
```

Legge il tracker e rimuove: sorgenti Java e GUI, route da `App.java`, entry da `config.js`, tracker.
Prima di rimuovere verifica che nessun altro modulo installato dichiari questo come dipendenza — **bloccante** se trovati dipendenti.

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

**Migration-only** (`module/audit/`):
```
module/audit/
  migration/     ← Solo migration SQL, nessun Java né GUI
  module.json    ← api: null, gui.config: null
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

**Manifest installati** (auto-generato):
```
src/main/resources/module/installed.json
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

**Migration-only** (nessun Java, nessuna GUI):
```json
{
  "name": "audit",
  "version": "1.0.0",
  "dependencies": {},
  "api": null,
  "gui": { "config": null },
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

## Gestione dipendenze

### Dichiarazione

Dichiarate in `module.json` come mappa `name → version`:

```json
{
  "dependencies": {
    "user": "*",
    "audit": "^1.0.0"
  }
}
```

### Verifica all'import

`cmd module import` esegue una verifica **transitiva e bloccante**:
- Raccoglie le dipendenze del modulo da installare
- Per ogni dipendenza installata, raccoglie ricorsivamente le sue dipendenze (via tracker)
- Se una qualsiasi dipendenza è mancante → **errore**, installazione bloccata
- Bypass con `--force`

### Controllo inverso alla rimozione

`cmd module remove` verifica che nessun modulo installato dipenda da quello da rimuovere:
- Scansiona tutti i tracker installati
- Se un tracker dichiara il modulo target come dipendenza → **errore**, rimozione bloccata
- Bypass con `--force`

### Manifest e verifica all'avvio

Ogni `import` e `remove` rigenera `src/main/resources/module/installed.json` con la mappa `key → {name, version, dependencies}` di tutti i moduli installati.

All'avvio, `App.java` legge il manifest e logga `[warn]` per ogni dipendenza non soddisfatta, senza bloccare l'avvio.

## Marker obbligatori nel progetto host

- `App.java`: `// [MODULE_ROUTES]`
- `gui/src/config.js`: `// [MODULE_ENTRIES]`

## Moduli disponibili

| Modulo | Descrizione | Dipendenze |
|--------|-------------|------------|
| `audit` | Tabella `audit_log` (migration-only, nessun Java né GUI) | — |
| `user` | Autenticazione, account, 2FA, reset password | `audit` |
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
- Il manifest `src/main/resources/module/installed.json` è sempre rigenerato da `import` e `remove`
- Moduli migration-only: `api: null`, `gui.config: null`, nessuna cartella `api/` o `gui/`
