# jms

Java Microservice Framework

---

## Pubblicare un progetto su Git con snapshot

Eseguire dal container di sviluppo (`docker exec -it <PROJECT_NAME> bash`).

**1. Creare il repository su GitHub** (vuoto, senza README).

**2. Inizializzare e pubblicare il repo:**
```bash
cmd git init
```
Esegue `git init`, primo commit di tutti i file non esclusi da `.gitignore`, e push su `origin/main`.

**3. Creare lo snapshot cifrato:**
```bash
cmd snap
```
Produce `snapshots/snapshot.tar.gz.gpg` contenente `config/`, `.env`, `.vscode/settings.json` e un dump del database (schema + dati).

**4. Committare e pushare lo snapshot:**
```bash
cmd git push
```

---

## Ripristinare un progetto da Git e snapshot

**1. Clonare il repo e installare il container:**
```bash
./install.sh
```
Se il container esiste già, `install.sh` lo riavvia senza sovrascrivere i dati.

**2. Entrare nel container:**
```bash
docker exec -it <PROJECT_NAME> bash
```

**3. Scaricare il codice aggiornato dal remote:**
```bash
cmd git fetch
```
Esegue `git reset --hard origin/main` — sovrascrive il workspace con il contenuto del repo.

**4. Ripristinare database e configurazione dallo snapshot:**
```bash
cmd snap restore
```
Decifra `snapshots/snapshot.tar.gz.gpg`, ricrea il database da zero e ripristina `config/` e `.env`.

**5. Compilare e avviare:**
```bash
cmd app build
cmd app start
```

---

## Note

- `cmd git init` richiede `GIT_USER`, `GIT_TOKEN`, `GIT_EMAIL` nel file `.env`.
- `cmd snap` e `cmd snap restore` richiedono una passphrase simmetrica (AES256).
- Le tabelle di log e i token temporanei sono escluse dal dump per default; configurabile via `SNAP_EXCLUDE_DATA` in `.env`.
- I sorgenti Java in `src/main/java/` devono essere tracciati da git: verificare con `git ls-files src/main/java/` prima del primo push.