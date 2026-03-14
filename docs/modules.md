# Modules

Module management and maintenance procedures.

## Installation

### Command

```bash
cmd module import modules/auth-1.1.0.tar.gz   # da archivio .tar.gz
cmd module import modules/auth-1.1.0           # da cartella estratta
```

**Automatic operations:**
- Sostituisce il placeholder `{{APP_PACKAGE}}` con il package reale (solo per archivi)
- Copia sorgenti Java in `src/main/java/<package>/<name>/`
- Copia sorgenti GUI in `gui/src/modules/<name>/`
- Copia migration SQL in `src/main/resources/db/migration/`
- Registra la route in `App.java` (dopo `// [MODULE_ROUTES]`)
- Aggiunge l'entry in `gui/src/config.js` (dopo `// [MODULE_ENTRIES]`)
- Verifica dipendenze dal tracker (`src/main/resources/modules/`)
- Scrive il tracker: `src/main/resources/modules/<nome>/module.json`

**Unico passo manuale:**

- `pom.xml` — aggiungere dipendenze Java esterne (solo se il modulo le richiede)

**Build dopo l'installazione:**

```bash
cmd gui build && cmd app build
cmd app restart
```

## Disinstallazione

```bash
cmd module remove --name auth
```

Legge il tracker installato (`src/main/resources/modules/auth/module.json`) e rimuove:
- Sorgenti Java e GUI
- Route da `App.java`
- Entry da `gui/src/config.js`
- Il tracker stesso

**Nota:** le migration Flyway non vengono rimosse automaticamente. Usare `cmd db reset` per ripristinare il database da zero.

## Export e Distribuzione

### Export (cartella non compressa)

```bash
cmd module export --name auth --vers 1.1.0   # → modules/auth-1.1.0/
cmd module export --name auth                # → modules/auth/
```

Genera automaticamente `module.json` leggendo `*Routes.java` e `gui/src/config.js`.

### Dist (compressione archivio)

```bash
cmd module dist --name auth-1.1.0   # modules/auth-1.1.0/ → modules/auth-1.1.0.tar.gz
```

### Sincronizzazione con jms

```bash
cp modules/auth-1.1.0.tar.gz jms/modules/auth-1.1.0.tar.gz
md5sum modules/auth-1.1.0.tar.gz jms/modules/auth-1.1.0.tar.gz   # devono essere identici
```

**Invariante:** i MD5 di `modules/*.tar.gz` e `jms/modules/*.tar.gz` devono essere sempre identici.

## Struttura file

```
modules/<nome>-<vers>/             ← Cartella export (non compressa)
  java/<nome>/                     ← Sorgenti Java (con placeholder {{APP_PACKAGE}})
  gui/<nome>/                      ← Sorgenti GUI
  migration/                       ← Migration SQL Flyway
  module.json                      ← Metadati (auto-generati da export)
modules/<nome>-<vers>.tar.gz        ← Archivio distribuibile (generato da dist)
jms/modules/<nome>-<vers>.tar.gz   ← Copia template (MD5 identico)
src/main/resources/modules/<nome>/ ← Tracker moduli installati
  module.json                      ← Copia module.json post-installazione
```

## Schema module.json

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
      "route": "/accounts",
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

- `api.routes` — chiamata statica da inserire in `App.java`
- `api.config` — proprietà da aggiungere manualmente a `config/application.properties`
- `gui.config` — entry da inserire in `gui/src/config.js`
- `install_notice` — messaggio opzionale mostrato al termine dell'installazione

## File base vs file modulo

**File base** (NON modificare quando si lavora sui moduli):

- `gui/src/store.js`
- `gui/src/router.js`
- `jms/gui/src/config.js` — solo entry `status`; non deve contenere entry specifiche del progetto

**Markers obbligatori nel progetto host:**
- `App.java`: `// [MODULE_ROUTES]` prima delle registrazioni route dei moduli
- `gui/src/config.js`: `// [MODULE_ENTRIES]` dopo l'entry `status`

## Workflow aggiornamento modulo

1. Modifica il file live in `gui/src/modules/<nome>/` o `src/main/java/.../app/<nome>/`
2. Esporta: `cmd module export --name <nome> --vers x.y.z`
3. Crea archivio: `cmd module dist --name <nome>-x.y.z`
4. Sincronizza: `cp modules/<nome>-x.y.z.tar.gz jms/modules/<nome>-x.y.z.tar.gz`
5. Verifica MD5

## Dipendenze

Le dipendenze sono dichiarate in `module.json`:

```json
{
  "dependencies": {
    "auth": "^1.0.0"
  }
}
```

`cmd module import` verifica le dipendenze controllando la presenza del tracker in `src/main/resources/modules/<nome>/`. Avvisa se mancanti e chiede conferma prima di procedere.

## Moduli disponibili

**`auth-1.1.0.tar.gz`**
- Sistema di autenticazione completo
- Login, sessione, reset password con token
- Permesso `can_send_mail` disponibile per l'invio email da admin
- Dipendenze: nessuna

**`header-1.0.0.tar.gz`**
- Header navigazione persistente
- Link Utenti/Profilo in base al ruolo
- Dipendenze: nessuna

**`home-1.0.0.tar.gz`**
- Home page con endpoint `/api/home/hello`
- Dipendenze: nessuna

**`contatti-1.0.0.tar.gz`**
- Gestione contatti
- Dipendenze: auth ^1.0.0

## Invarianti

- MD5 di `modules/*.tar.gz` e `jms/modules/*.tar.gz` devono essere identici
- `jms/gui/src/config.js` non deve contenere entry specifiche del progetto
- Ogni modulo Java deve avere una classe `*Routes.java` con metodo statico `register(...)`
- Il tracker `src/main/resources/modules/<nome>/module.json` è scritto da `import` e letto da `remove`
