# app.sh

Script per la gestione orchestrata dei container Docker del progetto dalla macchina host.

Gestisce l'avvio e lo stop nell'ordine corretto: dipendenze prima (PostgreSQL, Mailpit), container di sviluppo per ultimo. Lo stop avviene in ordine inverso.

## Uso

```bash
./app.sh --start    # Avvia tutti i container del progetto
./app.sh --stop     # Ferma tutti i container del progetto
./app.sh --restart  # Stop + start
./app.sh --status   # Mostra lo stato di tutti i container
```

Da eseguire dalla **macchina host**, non dentro il container di sviluppo.

## Ordine di avvio

I container vengono avviati nella sequenza:

1. `<project>-db` (PostgreSQL) — se `PGSQL_ENABLED=true` in `.env` e il container esiste
2. `<project>-mail` (Mailpit) — se il container esiste
3. `<project>` (container di sviluppo)

Lo stop avviene in ordine inverso. Vengono inclusi solo i container che esistono in Docker — quelli non installati vengono ignorati silenziosamente.

## Prerequisiti

- Docker installato e running sulla macchina host
- File `.env` presente nella root del progetto
- Container già creati via `./install.sh`

## Differenza con install.sh

| | `app.sh` | `install.sh` |
|---|---|---|
| Scopo | Start/stop container esistenti | Crea l'ambiente da zero |
| Rebuild immagine | No | Sì (al primo avvio) |
| Uso tipico | Avvio quotidiano | Prima configurazione o ricreare container |

## Esempio di output

```
Starting project 'hello'...
  hello-db    — starting
  hello-mail  — already running
  hello       — starting
Done.
```
