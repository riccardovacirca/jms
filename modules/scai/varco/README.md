# Modulo VARCO (Varchi)

Modulo per la gestione dell'anagrafica dei varchi di accesso nel sistema SCAI.

## Descrizione

Questo modulo fornisce le funzionalità CRUD (Create, Read, Update, Delete) per la gestione dei varchi di accesso. È un modulo di tipo **LOOKUP** che fornisce dati di riferimento utilizzati da altri moduli del sistema.

## Tipo Modulo

**Lookup** - Fornisce dati di riferimento per altri moduli

## Dipendenze

- **sdc** (^1.0.0) - Sistema di campo (required)
- **repertorio** (^1.0.0) - Repertorio (required)
- **Installation Order**: 5

## Moduli Dipendenti

Questo modulo è utilizzato come riferimento da:
- `policy` - Gestione policy rapporti

## Struttura Dati

### Tabella: `scai_sistemi_campo_varchi`

| Campo | Tipo | Descrizione |
|-------|------|-------------|
| id | BIGSERIAL | Chiave primaria |
| codice_varco | VARCHAR(6) | Codice varco |
| desc_ridotta | VARCHAR(128) | Descrizione breve |
| desc_lunga | VARCHAR(256) | Descrizione completa |
| cod_repertorio | VARCHAR(6) | Riferimento a repertorio |
| slug_sdc | VARCHAR(64) | Riferimento a sistema di campo |
| created_at | TIMESTAMP | Data creazione |
| created_by | BIGINT | User ID creazione |
| updated_at | TIMESTAMP | Data ultimo aggiornamento |
| updated_by | BIGINT | User ID aggiornamento |
| is_not_deleted | SMALLINT | Soft delete (1=attivo, NULL=cancellato) |

### Vincoli

- **Unique Key**: `(cod_repertorio, codice_varco, is_not_deleted)` - Garantisce unicità del varco per repertorio
- **Foreign Key** (opzionale): `cod_repertorio` → `scai_repertorio(codice_repertorio)`
- **Foreign Key** (opzionale): `slug_sdc` → `scai_sistemi_campo(slug)`

### Indici

- `idx_varco_codice` - Indice su codice_varco
- `idx_varco_cod_repertorio` - Indice su cod_repertorio per join
- `idx_varco_slug_sdc` - Indice su slug_sdc per join
- `idx_varco_is_not_deleted` - Indice su soft delete

## API Endpoints

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | `/api/scai/varco` | Lista varchi con paginazione e filtri |
| GET | `/api/scai/varco/{id}` | Dettaglio singolo varco |
| POST | `/api/scai/varco` | Creazione nuovo varco |
| PUT | `/api/scai/varco/{id}` | Aggiornamento varco esistente |
| DELETE | `/api/scai/varco/{id}` | Eliminazione logica varco |

### Parametri Lista (GET /api/scai/varco)

- `draw` - Numero richiesta (per DataTable)
- `start` - Offset paginazione
- `length` - Numero record per pagina
- `orderby` - Campo ordinamento
- `ordertype` - Tipo ordinamento (asc/desc)
- `codice_varco` - Filtro per codice (opzionale)
- `desc_ridotta` - Filtro per descrizione (opzionale)
- `cod_repertorio` - Filtro per repertorio (opzionale)
- `slug_sdc` - Filtro per sistema di campo (opzionale)

### Risposta Lista

```json
{
  "draw": 1,
  "recordsTotal": 500,
  "recordsFiltered": 500,
  "data": [
    {
      "id": 1,
      "codice_varco": "V001",
      "desc_ridotta": "Varco principale",
      "desc_lunga": "Varco di accesso principale edificio A",
      "cod_repertorio": "REP001",
      "slug_sdc": "chronorium",
      "created_at": "2024-01-01T10:00:00Z",
      "created_by": 1,
      "updated_at": "2024-01-01T10:00:00Z",
      "updated_by": 1
    }
  ]
}
```

## Frontend Components

### `varco-module`
Componente principale del modulo che gestisce il routing e la visualizzazione.

### `varco-component`
Componente per la gestione CRUD con tre viste:
- **Lista** - Tabella con filtri multipli e paginazione
- **Dettaglio** - Visualizzazione dati singolo varco con audit trail completo
- **Form** - Form per creazione/modifica con 2 select dinamiche (SDC + Repertorio)

### Funzionalità Frontend

- Ricerca con debouncing (400ms)
- Paginazione server-side
- **4 filtri**: codice, descrizione, repertorio (select), sistema di campo (select)
- **2 select dinamiche** caricate da API:
  - `/api/scai/sdc` per sistema di campo
  - `/api/scai/repertorio` per repertorio
- Validazione form
- Gestione stati di caricamento
- Conferma eliminazione
- Badge colorati per repertorio (bg-secondary) e SDC (bg-info)
- Audit trail con created_by/updated_by

## Installazione

### 1. Prerequisiti

Assicurarsi che i moduli **sdc** e **repertorio** siano installati prima di procedere:

```bash
# Verificare che le tabelle esistano
psql -U username -d database -c "SELECT COUNT(*) FROM scai_sistemi_campo;"
psql -U username -d database -c "SELECT COUNT(*) FROM scai_repertorio;"
```

### 2. Database Migration

Eseguire la migration SQL:

```bash
psql -U username -d database -f modules/varco/migration/V20260304_000005__varco.sql
```

### 3. Frontend

Importare il modulo nell'applicazione:

```javascript
import { VarcoModuleComponent } from './modules/varco/gui/varco/index.js';
```

### 4. Routing

Configurare il routing per `/varco`:

```javascript
{
  path: '/varco',
  component: 'varco-module'
}
```

## Sviluppo Backend (Java - Futuro)

La cartella `java/` contiene stub per una futura implementazione backend Java/Spring Boot:

- `dto/VarcoDTO.java` - Data Transfer Object
- `adapter/` - Adapter layer (da implementare)
- `dao/` - Data Access Object (da implementare)
- `handler/` - Request handlers (da implementare)

## Relazioni con Altri Moduli

### Dipendenza da Repertorio

Ogni varco può essere associato a un repertorio:

```sql
SELECT v.*, r.descrizione AS repertorio_desc
FROM scai_sistemi_campo_varchi v
LEFT JOIN scai_repertorio r ON v.cod_repertorio = r.codice_repertorio
WHERE v.is_not_deleted = 1;
```

### Dipendenza da Sistema di Campo

Ogni varco può essere associato a un sistema di campo:

```sql
SELECT v.*, s.descrizione_breve AS sistema_campo
FROM scai_sistemi_campo_varchi v
LEFT JOIN scai_sistemi_campo s ON v.slug_sdc = s.slug
WHERE v.is_not_deleted = 1;
```

## Note Architetturali

- **Modularità**: Modulo autonomo con dipendenze verso sdc e repertorio
- **Dipendenze Multiple**: Richiede 2 moduli installati (sdc + repertorio)
- **Soft Delete**: Implementato tramite campo `is_not_deleted`
- **Composite Natural Key**: `(cod_repertorio, codice_varco)` garantisce unicità per repertorio
- **Foreign Key Opzionali**: Le FK sono commentate, gestite a livello applicativo
- **Audit Trail Completo**: Tracciamento created_by/updated_by oltre ai timestamp

## Licenza

Proprietary - SCAI Team
