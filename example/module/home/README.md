# home

Modulo base con un endpoint di esempio. Utile come punto di partenza per nuove rotte o come verifica che il server sia operativo.

## Endpoint

| Metodo | Path | Descrizione |
|--------|------|-------------|
| GET | `/api/home/hello` | Restituisce un messaggio di benvenuto statico |

## Import

```bash
cmd module import home
```

## Benchmark

```bash
cmd bench -c 10 -r 50 -f modules/home/bench.txt
```
