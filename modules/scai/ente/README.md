# Modulo ENTE

Modulo per la gestione dell'anagrafica degli enti regionali nel sistema SCAI.

## Descrizione

Questo modulo fornisce le funzionalità CRUD (Create, Read, Update, Delete) per la gestione degli enti regionali. È un modulo di tipo **LOOKUP** che fornisce dati di riferimento utilizzati da altri moduli del sistema.

## Tipo Modulo

**Lookup** - Fornisce dati di riferimento per altri moduli

## Dipendenze

- **Nessuna dipendenza** - Questo modulo può essere installato autonomamente
- **Installation Order**: 1 (primo modulo da installare)

## Moduli Dipendenti

Questo modulo è utilizzato come riferimento da:
- `rapporti` - Gestione rapporti di lavoro
- `badge` - Gestione badge dipendenti
- `policy` - Gestione policy rapporti
- `veicoli` - Gestione veicoli

## Struttura Dati

### Tabella: `scai_ente`

| Campo | Tipo | Descrizione |
|-------|------|-------------|
| id | BIGSERIAL | Chiave primaria |
| cod_ente | VARCHAR(15) | Codice univoco ente (NOT NULL) |
| descrizione_ente | VARCHAR(255) | Descrizione completa ente (NOT NULL) |
| flag_areas | SMALLINT | Flag integrazione AREAS (0=no, 1=sì) |
| id_azienda_areas | BIGINT | ID azienda nel sistema AREAS |
| created_at | TIMESTAMP | Data creazione |
| created_by | BIGINT | Utente creazione |
| updated_at | TIMESTAMP | Data ultimo aggiornamento |
| updated_by | BIGINT | Utente ultimo aggiornamento |
| is_not_deleted | SMALLINT | Soft delete (1=attivo, NULL=cancellato) |

### Vincoli

- **Unique Key**: `(cod_ente, is_not_deleted)` - Garantisce unicità del codice ente tra i record attivi

### Indici

- `idx_ente_cod_ente` - Indice su cod_ente per ricerche rapide
- `idx_ente_is_not_deleted` - Indice su soft delete

## API Endpoints

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | `/api/scai/ente` | Lista enti con paginazione e filtri |
| GET | `/api/scai/ente/{id}` | Dettaglio singolo ente |
| POST | `/api/scai/ente` | Creazione nuovo ente |
| PUT | `/api/scai/ente/{id}` | Aggiornamento ente esistente |
| DELETE | `/api/scai/ente/{id}` | Eliminazione logica ente |

### Parametri Lista (GET /api/scai/ente)

- `draw` - Numero richiesta (per DataTable)
- `start` - Offset paginazione
- `length` - Numero record per pagina
- `orderby` - Campo ordinamento
- `ordertype` - Tipo ordinamento (asc/desc)
- `cod_ente` - Filtro per codice ente (opzionale)
- `descrizione_ente` - Filtro per descrizione (opzionale)

### Risposta Lista

```json
{
  "draw": 1,
  "recordsTotal": 150,
  "recordsFiltered": 150,
  "data": [
    {
      "id": 1,
      "cod_ente": "REG001",
      "descrizione_ente": "Regione Esempio",
      "flag_areas": 1,
      "id_azienda_areas": 12345,
      "created_at": "2024-01-01T10:00:00Z",
      "updated_at": "2024-01-01T10:00:00Z"
    }
  ]
}
```

## Frontend Components

### `ente-module`
Componente principale del modulo che gestisce il routing e la visualizzazione.

### `ente-component`
Componente per la gestione CRUD con tre viste:
- **Lista** - Tabella con filtri e paginazione
- **Dettaglio** - Visualizzazione dati singolo ente
- **Form** - Form per creazione/modifica ente

### Funzionalità Frontend

- Ricerca con debouncing (400ms)
- Paginazione server-side
- Filtri per codice e descrizione
- Validazione form
- Gestione stati di caricamento
- Conferma eliminazione

## Installazione

### 1. Database Migration

Eseguire la migration SQL:

```bash
psql -U username -d database -f modules/ente/migration/V20260304_000001__ente.sql
```

### 2. Frontend

Importare il modulo nell'applicazione:

```javascript
import { EnteModuleComponent } from './modules/ente/gui/ente/index.js';
```

### 3. Routing

Configurare il routing per `/ente`:

```javascript
{
  path: '/ente',
  component: 'ente-module'
}
```

## Sviluppo Backend (Java - Futuro)

La cartella `java/` contiene stub per una futura implementazione backend Java/Spring Boot:

- `dto/EnteDTO.java` - Data Transfer Object
- `adapter/` - Adapter layer (da implementare)
- `dao/` - Data Access Object (da implementare)
- `handler/` - Request handlers (da implementare)

## Note Architetturali

- **Modularità**: Modulo completamente autonomo e riutilizzabile
- **Zero Accoppiamento**: Nessuna dipendenza da altri moduli
- **Soft Delete**: Implementato tramite campo `is_not_deleted`
- **Audit Trail**: Campi `created_by`, `updated_by` per tracciabilità
- **Natural Key**: Il campo `cod_ente` funge da chiave naturale per relazioni cross-modulo

## Licenza

Proprietary - SCAI Team
