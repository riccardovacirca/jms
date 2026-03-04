# Manutenzione degli archivi dei moduli

Questo documento descrive come aggiornare i file sorgente dei moduli, ricostruire
gli archivi `.tar.gz` e mantenerli sincronizzati con `jms/`, senza alterare i file
base dell'applicazione.

---

## Installazione di un modulo

### Comando unico

```bash
cmd module import modules/<name>-x.y.z.tar.gz
```

Il comando esegue automaticamente:
- Estrazione dell'archivio in `modules/<name>/` con sostituzione del placeholder `{{APP_PACKAGE}}`
- Copia dei sorgenti Java in `src/main/java/<package>/<name>/` con package corretto
- Copia dei sorgenti GUI in `vite/src/modules/<name>/`
- Copia delle migration SQL in `src/main/resources/db/migration/`
- Append di `config/application.properties` del modulo a `config/application.properties` (solo se contiene chiavi reali, e solo se non già presente)

Al termine mostra le **azioni manuali** rimaste, specifiche per il modulo.

### Azioni manuali

Dopo l'esecuzione del comando, il modulo richiede di completare a mano le modifiche ai file esistenti del progetto:

**1. `pom.xml`** *(solo se il modulo ha dipendenze Java esterne)*

Aggiungere le dipendenze nel blocco `<dependencies>`. Il comando stampa il blocco XML pronto per il copy-paste.

**2. `src/main/java/<package>/App.java`** — import

Aggiungere gli import delle classi handler del modulo nella sezione import. Il comando stampa le righe `import` già con il package corretto.

**3. `src/main/java/<package>/App.java`** — route

Aggiungere le route nel `PathTemplateHandler`. Il comando stampa le chiamate `paths.add(...)` pronte.

**4. `vite/src/config.js`** — MODULE_CONFIG

Aggiungere la configurazione del modulo nell'oggetto `MODULE_CONFIG`. Il comando stampa il blocco JavaScript pronto.

> Il modulo `header` deve essere dichiarato **primo** in `MODULE_CONFIG`.

### Build e avvio

```bash
cmd gui build && cmd app build
cmd app restart
```

---

## Struttura

Ogni modulo installato nell'applicazione ha i propri file in **due posti distinti**
che devono essere sempre tenuti in sincronia:

```
vite/src/modules/<name>/       ← file live dell'applicazione (modificati durante lo sviluppo)
modules/<name>/gui/<name>/     ← copia del sorgente GUI da archiviare
modules/<name>/java/<name>/    ← sorgente Java del modulo
modules/<name>/migration/      ← migrazioni Flyway del modulo
modules/<name>-x.y.z.tar.gz    ← archivio distribuibile
jms/modules/<name>-x.y.z.tar.gz ← copia identica in jms (sempre allineata)
```

---

## File base vs file dei moduli

### File base — NON modificare quando si lavora sui moduli

I seguenti file appartengono all'infrastruttura base dell'applicazione. Esistono
sia nella versione app (`vite/src/`) sia nella versione template (`jms/template/vite/src/`).
La versione template deve restare generica, senza riferimenti ai moduli installati:

| File | App (`vite/src/`) | Template (`jms/template/vite/src/`) |
|------|-------------------|--------------------------------------|
| `store.js` | atom `authorized`, `user` | identico |
| `router.js` | router hash-based generico | identico |
| `config.js` | contiene le entry dei moduli installati | solo entry generica `index` |

**Regola:** `jms/template/vite/src/config.js` non deve mai contenere entry di moduli
specifici (auth, home, ecc.). La versione app `vite/src/config.js` le contiene perché
i moduli sono installati.

### File dei moduli — modificare sempre in coppia

Ogni modifica a un file JS di modulo va applicata **sia nel file live che nel file
di archivio**:

| File live (`vite/src/modules/`) | File archivio (`modules/<name>/gui/<name>/`) |
|---------------------------------|----------------------------------------------|
| `auth/init.js` | `auth/gui/auth/init.js` |
| `auth/index.js` | `auth/gui/auth/index.js` |
| `auth/login.js` | `auth/gui/auth/login.js` |
| `auth/changepass.js` | `auth/gui/auth/changepass.js` |
| `header/index.js` | `header/gui/header/index.js` |
| `header/component.js` | `header/gui/header/component.js` |
| `home/index.js` | `home/gui/home/index.js` |
| `home/component.js` | `home/gui/home/component.js` |
| `contatti/index.js` | `contatti/gui/contatti/index.js` |
| `contatti/component.js` | `contatti/gui/contatti/component.js` |

---

## Workflow di aggiornamento

1. Modifica il file live in `vite/src/modules/<name>/`
2. Applica la stessa modifica al file corrispondente in `modules/<name>/gui/<name>/`
3. Ricostruisci l'archivio (vedi sezione successiva)
4. Verifica i checksums

---

## Ricostruzione degli archivi

Il comando `cmd module export` gestisce automaticamente la creazione dell'archivio
(incluse le configurazioni `config/` e il README personalizzato). Va eseguito
dall'interno del container.

```bash
# Ricostruire l'archivio (dentro il container)
cmd module export <name> -v x.y.z

# Sincronizzare con jms (dalla root del progetto, fuori dal container)
cp modules/<name>-x.y.z.tar.gz jms/modules/<name>-x.y.z.tar.gz

# Verificare identità dei due archivi
md5 modules/<name>-x.y.z.tar.gz jms/modules/<name>-x.y.z.tar.gz
```

I due MD5 devono essere identici. Se non lo sono, il `cp` non è andato a buon fine.

### Esempio per tutti i moduli

```bash
# Dentro il container
cmd module export auth     -v 1.0.0
cmd module export header   -v 1.0.0
cmd module export home     -v 1.0.0
cmd module export contatti -v 1.0.0

# Fuori dal container
cp modules/auth-1.0.0.tar.gz     jms/modules/auth-1.0.0.tar.gz
cp modules/header-1.0.0.tar.gz   jms/modules/header-1.0.0.tar.gz
cp modules/home-1.0.0.tar.gz     jms/modules/home-1.0.0.tar.gz
cp modules/contatti-1.0.0.tar.gz jms/modules/contatti-1.0.0.tar.gz

# Verifica
md5 modules/auth-1.0.0.tar.gz     jms/modules/auth-1.0.0.tar.gz
md5 modules/header-1.0.0.tar.gz   jms/modules/header-1.0.0.tar.gz
md5 modules/home-1.0.0.tar.gz     jms/modules/home-1.0.0.tar.gz
md5 modules/contatti-1.0.0.tar.gz jms/modules/contatti-1.0.0.tar.gz
```

---

## Invarianti da rispettare

- Modificare sempre **entrambe** le copie di ogni file di modulo (live + archivio)
- Usare sempre `cmd module export` per ricostruire gli archivi (gestisce `COPYFILE_DISABLE=1` e include `config/` automaticamente)
- `modules/*.tar.gz` e `jms/modules/*.tar.gz` devono avere **MD5 identici**
- `jms/template/vite/src/config.js` non deve contenere entry di moduli specifici
- Non usare `cmd sync` per propagare file app-specifici in `jms/template/`: il sync
  sovrascrive i file base ma non deve portare in jms le entry di moduli presenti
  nella versione app di `config.js`
