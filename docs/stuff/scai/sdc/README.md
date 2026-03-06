# Modulo SDC (Sistemi di Campo)

Modulo per la gestione dell'anagrafica dei sistemi di campo nel sistema SCAI.

## Descrizione

Questo modulo fornisce le funzionalità CRUD (Create, Read, Update, Delete) per la gestione dei sistemi di campo. È un modulo di tipo **LOOKUP** che fornisce dati di riferimento utilizzati da altri moduli del sistema.

## Tipo Modulo

**Lookup** - Fornisce dati di riferimento per altri moduli

## Dipendenze

- **Nessuna dipendenza** - Questo modulo può essere installato autonomamente
- **Installation Order**: 3

## Moduli Dipendenti

Questo modulo è utilizzato come riferimento da:
- `policy` - Gestione policy rapporti

## Struttura Dati

### Tabella: `scai_sistemi_campo`

| Campo | Tipo | Descrizione |
|-------|------|-------------|
| id | BIGSERIAL | Chiave primaria |
| code | VARCHAR(8) | Codice legacy SCAI (NOT NULL) |
| slug | VARCHAR(64) | Identificatore univoco slug (NOT NULL) |
| descrizione_breve | VARCHAR(255) | Descrizione breve (NOT NULL) |
| descrizione_lunga | VARCHAR(512) | Descrizione completa (NOT NULL) |
| created_at | TIMESTAMP | Data creazione |
| updated_at | TIMESTAMP | Data ultimo aggiornamento |
| is_not_deleted | SMALLINT | Soft delete (1=attivo, NULL=cancellato) |

### Vincoli

- **Unique Key**: `(slug, is_not_deleted)` - Garantisce unicità dello slug tra i record attivi

### Indici

- `idx_sdc_slug` - Indice su slug per ricerche rapide
- `idx_sdc_code` - Indice su code per compatibilità legacy
- `idx_sdc_is_not_deleted` - Indice su soft delete

## API Endpoints

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | `/api/scai/sdc` | Lista sistemi con paginazione e filtri |
| GET | `/api/scai/sdc/{id}` | Dettaglio singolo sistema |
| POST | `/api/scai/sdc` | Creazione nuovo sistema |
| PUT | `/api/scai/sdc/{id}` | Aggiornamento sistema esistente |
| DELETE | `/api/scai/sdc/{id}` | Eliminazione logica sistema |

### Parametri Lista (GET /api/scai/sdc)

- `draw` - Numero richiesta (per DataTable)
- `start` - Offset paginazione
- `length` - Numero record per pagina
- `orderby` - Campo ordinamento
- `ordertype` - Tipo ordinamento (asc/desc)
- `slug` - Filtro per slug (opzionale)
- `code` - Filtro per codice (opzionale)
- `descrizione_breve` - Filtro per descrizione (opzionale)

### Risposta Lista

```json
{
  "draw": 1,
  "recordsTotal": 50,
  "recordsFiltered": 50,
  "data": [
    {
      "id": 1,
      "code": "CHRE",
      "slug": "chronorium",
      "descrizione_breve": "Sistema Chronorium",
      "descrizione_lunga": "Sistema di rilevazione presenze Chronorium",
      "created_at": "2024-01-01T10:00:00Z",
      "updated_at": "2024-01-01T10:00:00Z"
    }
  ]
}
```

## Frontend Components

### `sdc-module`
Componente principale del modulo che gestisce il routing e la visualizzazione.

### `sdc-component`
Componente per la gestione CRUD con tre viste:
- **Lista** - Tabella con filtri e paginazione
- **Dettaglio** - Visualizzazione dati singolo sistema
- **Form** - Form per creazione/modifica sistema

### Funzionalità Frontend

- Ricerca con debouncing (400ms)
- Paginazione server-side
- Filtri per slug, codice e descrizione
- Validazione form (slug pattern: solo lowercase, numeri e trattini)
- Gestione stati di caricamento
- Conferma eliminazione

## Installazione

### 1. Database Migration

Eseguire la migration SQL:

```bash
psql -U username -d database -f modules/sdc/migration/V20260304_000003__sdc.sql
```

### 2. Frontend

Importare il modulo nell'applicazione:

```javascript
import { SdcModuleComponent } from './modules/sdc/gui/sdc/index.js';
```

### 3. Routing

Configurare il routing per `/sdc`:

```javascript
{
  path: '/sdc',
  component: 'sdc-module'
}
```

## Sviluppo Backend (Java - Futuro)

La cartella `java/` contiene stub per una futura implementazione backend Java/Spring Boot:

- `dto/SdcDTO.java` - Data Transfer Object
- `adapter/` - Adapter layer (da implementare)
- `dao/` - Data Access Object (da implementare)
- `handler/` - Request handlers (da implementare)

## Slug Format

Lo slug deve seguire il formato:
- Solo lettere minuscole (a-z)
- Numeri (0-9)
- Trattini (-)
- Massimo 64 caratteri

Esempi validi:
- `chronorium`
- `presenze-2024`
- `sistema-campo-1`

## Note Architetturali

- **Modularità**: Modulo completamente autonomo e riutilizzabile
- **Zero Accoppiamento**: Nessuna dipendenza da altri moduli
- **Soft Delete**: Implementato tramite campo `is_not_deleted`
- **Slug Pattern**: Lo slug funge da identificatore URL-friendly per il sistema
- **Legacy Code**: Il campo `code` mantiene compatibilità con il sistema SCAI legacy

## Licenza

Proprietary - SCAI Team
