# Installazione ambiente di sviluppo

Procedura di setup dell'ambiente di sviluppo locale basato su Docker.

## Prerequisiti

- Docker installato e running sulla macchina host
- Nessun altro requisito (Java, Maven, Node.js sono dentro il container)

## Primo avvio

```bash
./install.sh
```

Al primo avvio (container non esistente) esegue in sequenza:

1. **`.env`** — genera il file di configurazione nella root del progetto con tutti i valori di default (nome progetto, porte, credenziali DB, ecc.)
2. **Docker network** — crea la rete `<project>-net` per la comunicazione tra container
3. **Docker image** — costruisce l'immagine di sviluppo `<project>-image` da `docker/Dockerfile.dev` (Ubuntu 24.04, git, curl, nano, OpenJDK 21, Maven, inotify-tools, Node.js)
4. **Docker container** — avvia il container con i bind mount e le porte configurate
5. **Scaffolding** — genera la struttura del progetto se `pom.xml` non esiste (`App.java`, `logback.xml`, `application.properties`, struttura Vite, cartella `module/`)
6. **npm install** — installa le dipendenze frontend dentro il container
7. **cmd tool** — registra `bin/cmd` come comando globale `/usr/local/bin/cmd` nel container

Al **riavvio** (container già esistente ma fermo):
```bash
./install.sh   # docker start senza rebuild
```

Se il container è già running, lo script non esegue nessuna azione.

## Struttura dell'ambiente

```
Host (macOS/Linux)              Container: <project>
──────────────────              ──────────────────────────────
./              ─bind mount──►  /workspace      (sorgenti)
./logs/         ─bind mount──►  /app/logs/      (log applicazione)
./config/       ─bind mount──►  /app/config/    (configurazione runtime)
localhost:<API_PORT_HOST>  ◄─── :8080           (Undertow API)
localhost:<VITE_PORT_HOST> ◄─── :5173           (Vite dev server)
localhost:<DEBUG_PORT_HOST>◄─── :5005           (JDWP debugger)
```

Porte di default (configurabili in `.env`):

| Servizio | Porta host | Porta container |
|---|---|---|
| API (Undertow) | 2310 | 8080 |
| Vite dev server | 2350 | 5173 |
| JDWP debugger | 5005 | 5005 |

## PostgreSQL

```bash
./install.sh --postgres
```

Installa un container PostgreSQL dedicato al progetto (`<project>-db`), connesso alla rete `<project>-net`. Legge tutti i parametri dal file `.env`. Se il container dev è in esecuzione, esegue automaticamente `cmd db setup` per creare utente e database.

Se il container esiste già, chiede conferma prima di ricrearlo.

Porta host di default: `2340` (configurabile via `PGSQL_PORT_HOST` in `.env`).

**Nota:** lo script supporta anche un container PostgreSQL condiviso tra progetti (container `postgres` già in esecuzione). In questo caso lo connette automaticamente alla rete del progetto.

## Mailpit (SMTP di sviluppo)

```bash
./install.sh --mailpit
```

Installa Mailpit (`<project>-mail`): un server SMTP che cattura le email senza spedirle, esponendole in una web UI. Utile per testare funzionalità di invio email in sviluppo.

| Servizio | Porta host default |
|---|---|
| SMTP | 2325 (`MAILPIT_SMTP_PORT_HOST`) |
| Web UI | 2335 (`MAILPIT_UI_PORT_HOST`) |

Configurazione in `application.properties`:
```properties
mail.host=<project>-mail
mail.port=1025
mail.enabled=true
```

## Configurazione `.env`

Generato automaticamente al primo `./install.sh`. Contiene:

| Sezione | Variabili principali |
|---|---|
| Common | `PROJECT_NAME`, `JAVA_VERSION`, `ARTIFACT_VERSION` |
| Development | `API_PORT_HOST`, `VITE_PORT_HOST`, `DEBUG_PORT_HOST` |
| Release | `RELEASE_MEMORY_LIMIT`, `RELEASE_CPU_LIMIT`, `RELEASE_PORT` |
| Database | `PGSQL_HOST`, `PGSQL_NAME`, `PGSQL_USER`, `PGSQL_PASSWORD` |
| Mailpit | `MAILPIT_SMTP_PORT_HOST`, `MAILPIT_UI_PORT_HOST` |
| Git | `GIT_USER`, `GIT_EMAIL`, `GIT_TOKEN` |

Il file `.env` è escluso da git (`.gitignore`). Va ricreato manualmente su ogni macchina di sviluppo.

## Dopo l'installazione

Entrare nel container:
```bash
docker exec -it <PROJECT_NAME> bash
```

Avviare lo sviluppo (due terminali):
```bash
# Terminale 1 — backend con hot reload
cmd app run

# Terminale 2 — frontend dev server
cmd gui run
```

Per l'elenco completo dei comandi disponibili:
```bash
cmd --help
```

## Rete Docker

Tutti i container del progetto (sviluppo, PostgreSQL, produzione) condividono la rete `<project>-net`. I container si raggiungono tramite hostname (es. il backend raggiunge il DB come `<project>-db`). La rete viene creata automaticamente se non esiste.
