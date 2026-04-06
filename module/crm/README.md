# crm/contatti

Gestione contatti e liste per applicazioni CRM. Fornisce CRUD completo per contatti e liste, gestione dello stato e della blacklist, associazione contatti/liste e un importer da file Excel con analisi, mapping colonne e validazione prima dell'esecuzione.

## Endpoint

### Contatti

| Metodo | Path | Descrizione |
|--------|------|-------------|
| GET | `/api/contatti` | Lista contatti con paginazione |
| POST | `/api/contatti` | Creazione contatto |
| GET | `/api/contatti/search` | Ricerca avanzata contatti |
| GET | `/api/contatti/{id}` | Contatto per ID |
| PUT | `/api/contatti/{id}` | Aggiornamento contatto |
| DELETE | `/api/contatti/{id}` | Eliminazione contatto |
| POST | `/api/contatti/{id}/stato` | Aggiornamento stato contatto |
| POST | `/api/contatti/{id}/blacklist` | Aggiunta/rimozione dalla blacklist |

### Liste

| Metodo | Path | Descrizione |
|--------|------|-------------|
| GET | `/api/liste` | Lista delle liste |
| POST | `/api/liste` | Creazione lista |
| GET | `/api/liste/{id}` | Lista per ID |
| PUT | `/api/liste/{id}` | Aggiornamento lista |
| DELETE | `/api/liste/{id}` | Eliminazione lista |
| POST | `/api/liste/{id}/stato` | Aggiornamento stato lista |
| PUT | `/api/liste/{id}/scadenza` | Impostazione scadenza lista |
| GET | `/api/liste/{id}/contatti` | Contatti associati alla lista |
| POST | `/api/liste/{id}/contatti/{cid}` | Associazione contatto a lista |
| DELETE | `/api/liste/{id}/contatti/{cid}` | Rimozione contatto da lista |

### Import

| Metodo | Path | Descrizione |
|--------|------|-------------|
| GET | `/api/import/campi` | Campi disponibili per il mapping |
| POST | `/api/import/analyze` | Analisi file Excel (anteprima colonne) |
| POST | `/api/import/{id}/mapping` | Definizione mapping colonne |
| POST | `/api/import/{id}/validate` | Validazione dati prima dell'import |
| POST | `/api/import/{id}/execute` | Esecuzione import |

## Import

```bash
cmd module import crm/contatti
```

## Benchmark

```bash
cmd bench -c 10 -r 50 -f modules/crm/contatti/bench.txt
```
