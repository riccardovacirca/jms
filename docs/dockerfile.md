# Dockerfile.dev

`docker/Dockerfile.dev` definisce l'immagine del container di sviluppo. Viene usato da `install.sh` per costruire l'immagine `<project>-image` al primo avvio.

## Contenuto

```dockerfile
FROM ubuntu:24.04
```

Base Ubuntu 24.04 LTS.

**Pacchetti installati via apt:**

| Pacchetto | Ruolo |
|-----------|-------|
| `git` | controllo versione |
| `curl` | download (usato anche per installare Node.js) |
| `nano` | editor testo nel container |
| `postgresql-client` | CLI `psql` per `cmd db` |
| `ca-certificates` | certificati TLS di sistema |
| `openjdk-21-jdk` | Java 21 — compilazione e runtime backend |
| `maven` | build Java (`cmd app build`) |
| `inotify-tools` | `inotifywait` — rilevamento modifiche file per `cmd app run` (watch mode) |
| `rsync` | sincronizzazione file (usato da script interni) |
| `siege` | benchmark HTTP (`cmd bench`) |
| `iproute2` | strumenti di rete (`ss`, `ip`) — usati da `cmd app status` per scansione porte |

**Node.js 20.x** installato tramite NodeSource (non il pacchetto apt predefinito) per avere una versione recente compatibile con Vite 6.

**Working directory:** `/workspace` (bind mount della cartella di progetto).

**Directory creata:** `/app/logs` — punto di mount per i log dell'applicazione (`./logs/` sul host).

## Come viene usato

`install.sh` esegue:

```sh
docker build -t "$PROJECT_NAME-image" -f docker/Dockerfile.dev .
```

Poi avvia il container con bind mount e porte esposte:

```sh
docker run -it -d \
    --name "$PROJECT_NAME" \
    -v "$PWD":/workspace \
    -v "$PWD/logs":/app/logs \
    -v "$PWD/config":/app/config \
    -p "$API_PORT_HOST:8080" \
    -p "$VITE_PORT_HOST:5173" \
    -p "$DEBUG_PORT_HOST:5005" \
    --network "$PROJECT_NAME-net" \
    "$PROJECT_NAME-image" \
    tail -f /dev/null
```

Il container rimane in esecuzione con `tail -f /dev/null` — i processi (backend, frontend) vengono avviati manualmente con `cmd app run` e `cmd gui run`.

## Bind mount

| Host | Container | Contenuto |
|------|-----------|-----------|
| `./` | `/workspace` | sorgenti del progetto (live, modifiche immediate) |
| `./logs/` | `/app/logs` | log applicazione (logback scrive qui) |
| `./config/` | `/app/config` | `application.properties` con configurazione runtime |

`/workspace` è il working directory: tutti i comandi `cmd` operano su questa cartella.

## Note

- Il `docker/` è escluso da `.gitignore` — l'immagine viene ricostruita da zero su ogni macchina tramite `install.sh`.
- Per modificare l'ambiente di sviluppo (aggiungere un pacchetto, cambiare versione Java) è sufficiente modificare questo file e rieseguire `./install.sh` da zero (dopo aver rimosso il container esistente).
