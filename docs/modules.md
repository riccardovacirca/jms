# Modules

Module management and maintenance procedures.

## Installation

### Command

```bash
cmd module import modules/auth-1.1.0.tar.gz   # da archivio .tar.gz
cmd module import modules/auth-1.1.0           # da cartella estratta
```

**Automatic operations:**
- Copia sorgenti Java in `src/main/java/dev/jms/app/<name>/`
- Copia sorgenti GUI in `gui/src/modules/<name>/`
- Copia migration SQL in `src/main/resources/db/migration/`
- Registra la route in `App.java` (dopo `// [MODULE_ROUTES]`)
- Aggiunge l'entry in `gui/src/config.js` (dopo `// [MODULE_ENTRIES]`)
- **Aggiunge Maven profile al `pom.xml` (dopo `<!-- [MODULE_PROFILES] -->`)**
- Verifica dipendenze dal tracker (`src/main/resources/modules/`)
- Scrive il tracker: `src/main/resources/modules/<nome>/module.json`

**Nessun passo manuale richiesto:** tutte le dipendenze Maven vengono gestite automaticamente tramite `profile.xml`.

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
- **Maven profile da `pom.xml`**
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
  api/<nome>/                      ← Sorgenti Java (package dev.jms.app)
  gui/<nome>/                      ← Sorgenti GUI
  migration/                       ← Migration SQL Flyway
  module.json                      ← Metadati (auto-generati da export)
  profile.xml                      ← Maven profile con dipendenze (opzionale)
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

## Maven Dependencies (profile.xml)

I moduli possono includere un file `profile.xml` per dichiarare dipendenze Maven. Questo file viene gestito automaticamente durante import/export/remove.

### Formato profile.xml

```xml
<!-- [MODULE:auth] -->
<profile>
    <id>module-auth</id>
    <activation>
        <file>
            <exists>src/main/resources/modules/auth/module.json</exists>
        </file>
    </activation>
    <dependencies>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.12.3</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.12.3</version>
            <scope>runtime</scope>
        </dependency>
    </dependencies>
</profile>
<!-- [/MODULE:auth] -->
```

### Funzionamento automatico

1. **Import**: `cmd module import auth`
   - Copia `profile.xml` nel `pom.xml` dopo il marker `<!-- [MODULE_PROFILES] -->`
   - Maven rileva il tracker `module.json` e **attiva automaticamente** il profile
   - Le dipendenze vengono scaricate al prossimo build

2. **Remove**: `cmd module remove --name auth`
   - Rimuove il blocco tra `<!-- [MODULE:auth] -->` e `<!-- [/MODULE:auth] -->`
   - Maven non trova più il tracker e **disattiva automaticamente** il profile
   - Le dipendenze non vengono più incluse

3. **Export**: `cmd module export --name auth`
   - Estrae il profile dal `pom.xml` del progetto corrente
   - Lo salva come `profile.xml` nella cartella modulo

### Marker nel pom.xml

Il file `pom.xml` deve contenere il marker:

```xml
<profiles>
    <!-- [MODULE_PROFILES] -->
    <!-- I profile dei moduli vengono inseriti qui automaticamente -->
</profiles>
```

### Note importanti

- I marker `<!-- [MODULE:nome] -->` e `<!-- [/MODULE:nome] -->` sono **obbligatori** per la rimozione automatica
- L'attivazione tramite `<file><exists>` permette a Maven di attivare/disattivare automaticamente il profile
- Se un modulo non ha dipendenze esterne, `profile.xml` può essere omesso
- `cmd module export` estrae automaticamente il profile esistente (se presente)

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
