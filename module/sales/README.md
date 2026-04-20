# sales

Gestione contatti, liste e campagne. Fornisce CRUD completo per contatti e liste, gestione dello stato e della blacklist, associazione contatti/liste, importer da file Excel con analisi, mapping colonne e validazione prima dell'esecuzione, e gestione campagne con associazione liste.

## Endpoint

### Contatti

| Metodo | Path | Descrizione |
|--------|------|-------------|
| GET | `/api/sales/contatti` | Lista contatti con paginazione |
| POST | `/api/sales/contatti` | Creazione contatto |
| GET | `/api/sales/contatti/search` | Ricerca avanzata contatti |
| GET | `/api/sales/contatti/{id}` | Contatto per ID |
| PUT | `/api/sales/contatti/{id}` | Aggiornamento contatto |
| DELETE | `/api/sales/contatti/{id}` | Eliminazione contatto |
| PUT | `/api/sales/contatti/{id}/stato` | Aggiornamento stato contatto |
| PUT | `/api/sales/contatti/{id}/blacklist` | Aggiunta/rimozione dalla blacklist |

### Liste

| Metodo | Path | Descrizione |
|--------|------|-------------|
| GET | `/api/sales/liste` | Lista delle liste |
| POST | `/api/sales/liste` | Creazione lista |
| GET | `/api/sales/liste/default` | Lista di default |
| GET | `/api/sales/liste/{id}` | Lista per ID |
| PUT | `/api/sales/liste/{id}` | Aggiornamento lista |
| DELETE | `/api/sales/liste/{id}` | Eliminazione lista |
| PUT | `/api/sales/liste/{id}/default` | Imposta lista di default |
| PUT | `/api/sales/liste/{id}/stato` | Aggiornamento stato lista |
| PUT | `/api/sales/liste/{id}/scadenza` | Impostazione scadenza lista |
| GET | `/api/sales/liste/{id}/contatti` | Contatti associati alla lista |
| POST | `/api/sales/liste/{id}/contatti` | Associazione contatto a lista |
| DELETE | `/api/sales/liste/{id}/contatti/{cid}` | Rimozione contatto da lista |

### Campagne

| Metodo | Path | Descrizione |
|--------|------|-------------|
| GET | `/api/sales/campagne` | Lista campagne con paginazione |
| POST | `/api/sales/campagne` | Creazione campagna |
| GET | `/api/sales/campagne/{id}` | Campagna per ID |
| PUT | `/api/sales/campagne/{id}` | Aggiornamento campagna |
| DELETE | `/api/sales/campagne/{id}` | Eliminazione campagna |
| GET | `/api/sales/campagne/{id}/liste` | Liste associate alla campagna |
| POST | `/api/sales/campagne/{id}/liste` | Aggiunta lista alla campagna |
| DELETE | `/api/sales/campagne/{id}/liste/{lid}` | Rimozione lista dalla campagna |

### Import

| Metodo | Path | Descrizione |
|--------|------|-------------|
| GET | `/api/sales/import/campi` | Campi disponibili per il mapping |
| POST | `/api/sales/import/analyze` | Analisi file Excel (anteprima colonne) |
| PUT | `/api/sales/import/{id}/mapping` | Definizione mapping colonne |
| GET | `/api/sales/import/{id}/validate` | Validazione dati prima dell'import |
| POST | `/api/sales/import/{id}/execute` | Esecuzione import |

## Installazione

```bash
cmd module install sales
```

## Benchmark

```bash
cmd bench -c 10 -r 50 -f module/sales/bench.txt
```
