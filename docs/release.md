# Release

Procedura di build e distribuzione dell'applicazione in produzione.

## Prerequisiti

**Macchina di sviluppo:**
- Docker installato e running
- Container di sviluppo attivo (`./install.sh` eseguito)
- File `.env` presente nella root del progetto

**Server di destinazione:**
- Docker installato e running
- Utente nel gruppo `docker` (non richiede sudo)
- Porta 8080 disponibile (configurabile via `RELEASE_PORT` in `.env`)
- Almeno 512 MB RAM disponibili

## Esecuzione

Lo script va eseguito dalla **macchina host**, non dentro il container di sviluppo.

```bash
./release.sh -v 1.0.0
```

La versione è obbligatoria. Il parametro `-v` (o `--vers`) deve essere sempre specificato.

## Cosa fa lo script

6 passi eseguiti in sequenza:

1. **Build Vite (frontend)** — compila il frontend (`npm run build`) e deposita il bundle in `src/main/resources/static/`
2. **Build Java (Maven)** — compila il backend e produce `target/service.jar` (fat JAR via Maven Shade), includendo il bundle frontend del passo 1
3. **Dockerfile produzione** — genera un `Dockerfile` temporaneo basato su `ubuntu:24.04` + `openjdk-21-jre-headless`, con utente non-root `appuser` (UID 1001) e healthcheck su `/api/status`
4. **Docker image** — costruisce l'immagine e la tagga come `<project>-app:latest` e `<project>-app:<version>`
5. **Export tar** — esporta l'immagine in `<project>-image.tar`
6. **Package finale** — genera `install.sh` per il server di destinazione, copia `application.properties`, comprime tutto in `dist/<project>-v<version>.tar.gz`

I file intermedi (JAR, Dockerfile, tar, install.sh) vengono rimossi al termine. Nella cartella `dist/` rimane solo il pacchetto finale.

## Output

```
dist/<project>-v<version>.tar.gz
  ├── <project>-image.tar      # immagine Docker completa, standalone
  ├── install.sh               # script di installazione per il server
  └── application.properties   # configurazione applicazione
```

## Deployment sul server

```bash
# Trasferire il pacchetto
scp dist/<project>-v1.0.0.tar.gz user@server:/tmp/

# Sul server
cd /tmp
tar -xzf <project>-v1.0.0.tar.gz
./install.sh
```

`install.sh` esegue sul server:
1. `docker load` — carica l'immagine dal tar nel Docker locale
2. `docker network create` — crea la rete `<project>-net` se non esiste
3. `docker run` — avvia il container di produzione con i limiti configurati

Il container viene avviato con:
- Nome: `<project>-<version>` (es. `hello-1.0.0`)
- Restart policy: `unless-stopped`
- Memory limit: 512m (reservation: 256m)
- CPU limit: 1.0 core
- Volume per i log: `<project>-logs` (named volume Docker)
- Config bind-mounted: `application.properties` → `/app/config/application.properties`

## Configurazione

I parametri di produzione sono configurabili nel `.env`:

| Variabile | Default | Descrizione |
|---|---|---|
| `RELEASE_IMAGE` | `ubuntu:24.04` | Immagine base Docker |
| `RELEASE_PORT` | `8080` | Porta esposta dal container |
| `RELEASE_MEMORY_LIMIT` | `512m` | Limite memoria |
| `RELEASE_MEMORY_RESERVATION` | `256m` | Reservation memoria |
| `RELEASE_CPU_LIMIT` | `1.0` | Limite CPU (core) |
| `RELEASE_CPU_RESERVATION` | `0.5` | Reservation CPU |
| `RELEASE_APP_USER` | `appuser` | Utente non-root nel container |
| `RELEASE_APP_USER_UID` | `1001` | UID utente |
| `RELEASE_APP_USER_GID` | `1001` | GID utente |

## Log in produzione

I log sono scritti da logback in `/app/logs/` dentro il container (file `service.log`, rotazione giornaliera con compressione gzip), montati su un Docker named volume `<project>-logs`.

```bash
# Visualizzare i log
docker logs -f <project>-<version>
docker exec <project>-<version> ls /app/logs/
```

## Rete

Il container di produzione si connette alla stessa rete Docker del container di sviluppo (`<project>-net`), raggiungendo gli altri servizi (es. PostgreSQL) tramite hostname del container. La rete viene creata automaticamente da `install.sh` se non esiste.
