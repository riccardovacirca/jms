# asynctest

Modulo di test per le feature async dell'applicazione. Fornisce endpoint che simulano operazioni lente (query DB e task CPU/IO) con varianti async e blocking per confrontare il comportamento sotto carico concorrente.

> **Attenzione:** modulo di test — non installare in produzione.

## Endpoint

| Metodo | Path | Descrizione |
|--------|------|-------------|
| GET | `/api/asynctest/slow-query?ms=500` | Query lenta simulata con `pg_sleep` (ASYNC) |
| GET | `/api/asynctest/slow-task?ms=500` | Sleep senza DB (ASYNC) |
| GET | `/api/asynctest/blocking-query?ms=500` | Stessa query lenta, esecuzione BLOCKING |
| GET | `/api/asynctest/status` | Stato del pool AsyncExecutor |

Il parametro `ms` indica la durata in millisecondi (default: 500, range: 1–30000).

## Import

```bash
cmd module import asynctest
```

## Benchmark

```bash
cmd bench -c 10 -r 50 -f modules/asynctest/bench.txt
```
