# Snapshot

Lo snapshot cattura lo stato completo dell'ambiente di esecuzione: configurazione, credenziali e dati del database. È cifrato e versionato in git insieme al codice sorgente.

## Contenuto

| File | Descrizione |
|------|-------------|
| `.env` | Variabili d'ambiente del progetto |
| `config/` | Cartella di configurazione completa (application.properties, chiavi private, ecc.) |
| `database.sql` | pg_dump completo del database (schema + dati) |

`pom.xml`, `package.json` e l'elenco dei moduli non sono inclusi: sono già nel repo git.

## Formato

```
snapshots/snapshot.tar.gz.gpg   ← unico file, tracciato da git
```

Il file è un archivio `.tar.gz` cifrato con GPG simmetrico AES256. Il plain `.tar.gz` esiste solo durante la creazione o il ripristino e viene rimosso automaticamente al termine.

## Versionamento

Il versionamento dello snapshot segue il normale flusso git: lo snapshot valido è l'ultimo committato. Uno snapshot nuovo sovrascrive sempre quello esistente. `cmd snap` non esegue comandi git.

## Comandi

### Creazione

```bash
cmd snap
```

Equivalente a `cmd snap create`. Chiede la passphrase due volte (conferma). Sovrascrive `snapshots/snapshot.tar.gz.gpg` se già presente.

### Ripristino

```bash
cmd snap restore             # ripristina tutto (.env, config/, database)
cmd snap restore --db-only   # solo database
cmd snap restore --config-only  # solo .env e config/
```

Chiede la passphrase una volta. Il ripristino del database richiede conferma esplicita prima di eseguire drop + recreate + import.

## Passphrase

In modalità interattiva la passphrase viene digitata da terminale e non appare nei log né nella history della shell.

Per usi automatici (CI, cron) impostare la variabile d'ambiente `SNAP_PASSPHRASE`:

```bash
SNAP_PASSPHRASE=mysecret cmd snap
SNAP_PASSPHRASE=mysecret cmd snap restore
```

La passphrase non va in `.env` (sarebbe committata in chiaro). Va comunicata out-of-band al team.

## .gitignore

I file temporanei sono già esclusi dal tracciamento:

```
snapshots/snapshot.tar.gz   ← plain temporaneo durante creazione/ripristino
snapshots/snapshot/         ← directory temporanea durante estrazione
```

Il file `snapshots/snapshot.tar.gz.gpg` non è ignorato e viene tracciato normalmente.

## Procedura di ripristino completa

1. Clona o aggiorna il repo (contiene già `snapshots/snapshot.tar.gz.gpg`)
2. Avvia l'ambiente:
   ```bash
   ./install.sh
   docker exec -it <PROJECT_NAME> bash
   ```
3. Ripristina configurazione e database:
   ```bash
   cmd snap restore
   ```
4. Compila e avvia:
   ```bash
   cmd app build
   cmd app start
   ```

Il database viene ricreato autonomamente dal dump (schema + dati): non è necessario eseguire le migration Flyway separatamente.
