# Manutenzione degli archivi dei moduli

Questo documento descrive come aggiornare i file sorgente dei moduli, ricostruire
gli archivi `.tar.gz` e mantenerli sincronizzati con `jms/`, senza alterare i file
base dell'applicazione.

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

| File live (`vite/src/`) | File archivio (`modules/`) |
|-------------------------|----------------------------|
| `modules/auth/init.js` | `auth/gui/auth/init.js` |
| `modules/auth/index.js` | `auth/gui/auth/index.js` |
| `modules/auth/login.js` | `auth/gui/auth/login.js` |
| `modules/auth/changepass.js` | `auth/gui/auth/changepass.js` |
| `modules/home/component.js` | `home/gui/home/component.js` |
| `modules/home/index.js` | `home/gui/home/index.js` |

---

## Workflow di aggiornamento

1. Modifica il file live in `vite/src/modules/<name>/`
2. Applica la stessa modifica al file corrispondente in `modules/<name>/gui/<name>/`
3. Ricostruisci l'archivio (vedi sezione successiva)
4. Verifica i checksums

---

## Ricostruzione degli archivi

I comandi vanno eseguiti dalla root del progetto. `COPYFILE_DISABLE=1` è obbligatorio
su macOS per evitare che `tar` includa i file metadata `._*` di AppleDouble.

```bash
# Rimuovere eventuali file metadata macOS residui
find modules/<name> -name '._*' -delete

# Ricostruire l'archivio
TMP=$(mktemp -d)
cp -r modules/<name>/. "$TMP/"
COPYFILE_DISABLE=1 tar -czf modules/<name>-x.y.z.tar.gz -C "$TMP" .
rm -rf "$TMP"

# Sincronizzare con jms
cp modules/<name>-x.y.z.tar.gz jms/modules/<name>-x.y.z.tar.gz

# Verificare identità dei due archivi
md5 modules/<name>-x.y.z.tar.gz jms/modules/<name>-x.y.z.tar.gz
```

I due MD5 devono essere identici. Se non lo sono, il `cp` non è andato a buon fine.

### Esempio per entrambi i moduli in una sola sessione

```bash
# auth
find modules/auth -name '._*' -delete
rm -f modules/auth-1.0.0.tar.gz
TMP=$(mktemp -d) && cp -r modules/auth/. "$TMP/"
COPYFILE_DISABLE=1 tar -czf modules/auth-1.0.0.tar.gz -C "$TMP" .
rm -rf "$TMP"
cp modules/auth-1.0.0.tar.gz jms/modules/auth-1.0.0.tar.gz

# home
find modules/home -name '._*' -delete
rm -f modules/home-1.0.0.tar.gz
TMP=$(mktemp -d) && cp -r modules/home/. "$TMP/"
COPYFILE_DISABLE=1 tar -czf modules/home-1.0.0.tar.gz -C "$TMP" .
rm -rf "$TMP"
cp modules/home-1.0.0.tar.gz jms/modules/home-1.0.0.tar.gz

# verifica
md5 modules/auth-1.0.0.tar.gz jms/modules/auth-1.0.0.tar.gz
md5 modules/home-1.0.0.tar.gz jms/modules/home-1.0.0.tar.gz
```

---

## Invarianti da rispettare

- Modificare sempre **entrambe** le copie di ogni file di modulo (live + archivio)
- `modules/*.tar.gz` e `jms/modules/*.tar.gz` devono avere **MD5 identici**
- Usare sempre `COPYFILE_DISABLE=1` nella creazione degli archivi su macOS
- `jms/template/vite/src/config.js` non deve contenere entry di moduli specifici
- Non usare `cmd sync` per propagare file app-specifici in `jms/template/`: il sync
  sovrascrive i file base ma non deve portare in jms le entry di moduli presenti
  nella versione app di `config.js`
