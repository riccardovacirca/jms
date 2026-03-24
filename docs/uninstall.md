# Disinstallazione ambiente di sviluppo

Procedura di rimozione completa dell'ambiente Docker del progetto.

Non esiste uno script automatico di uninstall — i comandi Docker vanno eseguiti manualmente dalla macchina host.

## Rimozione completa

```bash
# 1. Ferma e rimuovi il container di sviluppo
docker stop <project> && docker rm <project>

# 2. Ferma e rimuovi il container PostgreSQL
docker stop <project>-db && docker rm <project>-db

# 3. Ferma e rimuovi il container Mailpit
docker stop <project>-mail && docker rm <project>-mail

# 4. Rimuovi le immagini Docker
docker rmi <project>-image
docker rmi <project>-db-image
docker rmi <project>-mail-image
docker rmi <project>-app:latest <project>-app:<version>   # immagini di produzione, se presenti

# 5. Rimuovi i volumi Docker
docker volume rm <project>-db-volume   # dati PostgreSQL
docker volume rm <project>-logs        # log produzione (se presente)

# 6. Rimuovi la rete Docker
docker network rm <project>-net
```

Sostituire `<project>` con il valore di `PROJECT_NAME` nel file `.env`.

## Rimozione selettiva

### Solo il container di sviluppo (mantieni DB e dati)

```bash
docker stop <project> && docker rm <project>
docker rmi <project>-image
```

### Solo il database (attenzione: elimina tutti i dati)

```bash
docker stop <project>-db && docker rm <project>-db
docker rmi <project>-db-image
docker volume rm <project>-db-volume
```

### Solo Mailpit

```bash
docker stop <project>-mail && docker rm <project>-mail
docker rmi <project>-mail-image
```

## File locali

I file locali **non** vengono rimossi dai comandi Docker. Vanno eliminati manualmente se necessario:

| Path | Contenuto |
|---|---|
| `./logs/` | Log dell'applicazione (bind mount da `/app/logs/`) |
| `./config/application.properties` | Configurazione runtime |
| `./.env` | Variabili d'ambiente del progetto |
| `./target/` | Output compilazione Maven |
| `./dist/` | Package di release |
| `./gui/node_modules/` | Dipendenze npm |
| `./src/main/resources/static/` | Bundle frontend compilato |

## Reinstallazione

Dopo la rimozione completa, il progetto può essere reinstallato da zero:

```bash
./install.sh             # ricrea container di sviluppo e .env
./install.sh --postgres  # ricrea PostgreSQL
./install.sh --mailpit   # ricrea Mailpit (opzionale)
```
