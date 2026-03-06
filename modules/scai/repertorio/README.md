# Modulo REPERTORIO

Modulo per la gestione dell'anagrafica dei repertori nel sistema SCAI.

## Descrizione

Questo modulo fornisce le funzionalità CRUD (Create, Read, Update, Delete) per la gestione dei repertori. È un modulo di tipo **LOOKUP** che fornisce dati di riferimento utilizzati da altri moduli del sistema.

## Tipo Modulo

**Lookup** - Fornisce dati di riferimento per altri moduli

## Dipendenze

- **sdc** (^1.0.0) - Sistema di campo (required)
- **Installation Order**: 4

## Moduli Dipendenti

Questo modulo è utilizzato come riferimento da:
- `policy` - Gestione policy rapporti

## Struttura Dati

### Tabella: `scai_repertorio`

| Campo | Tipo | Descrizione |
|-------|------|-------------|
| id | BIGSERIAL | Chiave primaria |
| codice_repertorio | VARCHAR(6) | Codice univoco repertorio (NOT NULL) |
| descrizione | VARCHAR(255) | Descrizione repertorio |
| slug_sdc | VARCHAR(32) | Riferimento a sistema di campo (NOT NULL) |
| livello | VARCHAR(8) | Tipologia/livello repertorio |
| flag_parcheggio | VARCHAR(4) | Flag parcheggio |
| flag_struttura | VARCHAR(4) | Flag struttura |
| created_at | TIMESTAMP | Data creazione |
| updated_at | TIMESTAMP | Data ultimo aggiornamento |
| is_not_deleted | SMALLINT | Soft delete (1=attivo, NULL=cancellato) |

### Vincoli

- **Unique Key**: `(codice_repertorio, is_not_deleted)` - Garantisce unicità del codice tra i record attivi
- **Foreign Key** (opzionale): `slug_sdc` → `scai_sistemi_campo(slug)`

### Indici

- `idx_repertorio_codice` - Indice su codice_repertorio
- `idx_repertorio_slug_sdc` - Indice su slug_sdc per join
- `idx_repertorio_is_not_deleted` - Indice su soft delete

## API Endpoints

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | `/api/scai/repertorio` | Lista repertori con paginazione e filtri |
| GET | `/api/scai/repertorio/{id}` | Dettaglio singolo repertorio |
| POST | `/api/scai/repertorio` | Creazione nuovo repertorio |
| PUT | `/api/scai/repertorio/{id}` | Aggiornamento repertorio esistente |
| DELETE | `/api/scai/repertorio/{id}` | Eliminazione logica repertorio |

### Parametri Lista (GET /api/scai/repertorio)

- `draw` - Numero richiesta (per DataTable)
- `start` - Offset paginazione
- `length` - Numero record per pagina
- `orderby` - Campo ordinamento
- `ordertype` - Tipo ordinamento (asc/desc)
- `codice_repertorio` - Filtro per codice (opzionale)
- `descrizione` - Filtro per descrizione (opzionale)
- `slug_sdc` - Filtro per sistema di campo (opzionale)

### Risposta Lista

```json
{
  "draw": 1,
  "recordsTotal": 100,
  "recordsFiltered": 100,
  "data": [
    {
      "id": 1,
      "codice_repertorio": "REP001",
      "descrizione": "Repertorio principale",
      "slug_sdc": "chronorium",
      "livello": "L1",
      "flag_parcheggio": "SI",
      "flag_struttura": "NO",
      "created_at": "2024-01-01T10:00:00Z",
      "updated_at": "2024-01-01T10:00:00Z"
    }
  ]
}
```

## Frontend Components

### `repertorio-module`
Componente principale del modulo che gestisce il routing e la visualizzazione.

### `repertorio-component`
Componente per la gestione CRUD con tre viste:
- **Lista** - Tabella con filtri e paginazione
- **Dettaglio** - Visualizzazione dati singolo repertorio
- **Form** - Form per creazione/modifica repertorio con select dinamica SDC

### Funzionalità Frontend

- Ricerca con debouncing (400ms)
- Paginazione server-side
- Filtri per codice, descrizione e sistema di campo (select)
- Select dinamica caricata da API `/api/scai/sdc`
- Validazione form
- Gestione stati di caricamento
- Conferma eliminazione
- Codice repertorio disabilitato in modifica (chiave naturale)

## Installazione

### 1. Prerequisiti

Assicurarsi che il modulo **sdc** sia installato prima di procedere:

```bash
# Verificare che scai_sistemi_campo esista
psql -U username -d database -c "SELECT COUNT(*) FROM scai_sistemi_campo;"
```

### 2. Database Migration

Eseguire la migration SQL:

```bash
psql -U username -d database -f modules/repertorio/migration/V20260304_000004__repertorio.sql
```

### 3. Frontend

Importare il modulo nell'applicazione:

```javascript
import { RepertorioModuleComponent } from './modules/repertorio/gui/repertorio/index.js';
```

### 4. Routing

Configurare il routing per `/repertorio`:

```javascript
{
  path: '/repertorio',
  component: 'repertorio-module'
}
```

## Sviluppo Backend (Java - Futuro)

La cartella `java/` contiene stub per una futura implementazione backend Java/Spring Boot:

- `dto/RepertorioDTO.java` - Data Transfer Object
- `adapter/` - Adapter layer (da implementare)
- `dao/` - Data Access Object (da implementare)
- `handler/` - Request handlers (da implementare)

## Relazione con Sistema di Campo

Ogni repertorio è associato a un sistema di campo tramite il campo `slug_sdc`:

```sql
-- Esempio query con join
SELECT r.*, s.descrizione_breve AS sistema_campo
FROM scai_repertorio r
LEFT JOIN scai_sistemi_campo s ON r.slug_sdc = s.slug
WHERE r.is_not_deleted = 1;
```

## Note Architetturali

- **Modularità**: Modulo autonomo con dipendenza verso sdc
- **Dipendenza**: Richiede modulo sdc installato
- **Soft Delete**: Implementato tramite campo `is_not_deleted`
- **Natural Key**: Il campo `codice_repertorio` funge da chiave naturale
- **Foreign Key Opzionale**: La FK verso sdc è commentata, gestita a livello applicativo

## Licenza

Proprietary - SCAI Team
