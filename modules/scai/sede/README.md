# Modulo SEDE

Modulo per la gestione dell'anagrafica delle sedi di lavoro nel sistema SCAI.

## Descrizione

Questo modulo fornisce le funzionalità CRUD (Create, Read, Update, Delete) per la gestione delle sedi di lavoro. È un modulo di tipo **LOOKUP** che fornisce dati di riferimento utilizzati da altri moduli del sistema.

## Tipo Modulo

**Lookup** - Fornisce dati di riferimento per altri moduli

## Dipendenze

- **Nessuna dipendenza** - Questo modulo può essere installato autonomamente
- **Installation Order**: 2

## Moduli Dipendenti

Questo modulo è utilizzato come riferimento da:
- `rapporti` - Gestione rapporti di lavoro (cod_sede_primaria, cod_sede_secondaria)
- `badge` - Gestione badge dipendenti (opzionale)
- `policy` - Gestione policy rapporti (opzionale)

## Struttura Dati

### Tabella: `scai_sede`

| Campo | Tipo | Descrizione |
|-------|------|-------------|
| id | BIGSERIAL | Chiave primaria |
| cod_sede | VARCHAR(10) | Codice univoco sede (NOT NULL) |
| nome | VARCHAR(255) | Nome sede (NOT NULL) |
| indirizzo | VARCHAR(512) | Indirizzo completo (NOT NULL) |
| cap | VARCHAR(5) | Codice Avviamento Postale |
| city_code | VARCHAR(4) | Codice ISTAT comune |
| province_code | VARCHAR(4) | Codice provincia |
| istat_region | VARCHAR(2) | Codice ISTAT regione |
| nazione | VARCHAR(2) | Codice ISO nazione (es: IT) |
| descrizione_breve | VARCHAR(255) | Descrizione breve sede (NOT NULL) |
| descrizione_lunga | VARCHAR(512) | Descrizione completa sede (NOT NULL) |
| created_at | TIMESTAMP | Data creazione |
| updated_at | TIMESTAMP | Data ultimo aggiornamento |
| is_not_deleted | SMALLINT | Soft delete (1=attivo, NULL=cancellato) |

### Vincoli

- **Unique Key**: `(cod_sede, is_not_deleted)` - Garantisce unicità del codice sede tra i record attivi

### Indici

- `idx_sede_cod_sede` - Indice su cod_sede per ricerche rapide
- `idx_sede_nome` - Indice su nome per ricerche
- `idx_sede_city_code` - Indice su codice comune
- `idx_sede_province_code` - Indice su provincia
- `idx_sede_is_not_deleted` - Indice su soft delete

## API Endpoints

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | `/api/scai/sede` | Lista sedi con paginazione e filtri |
| GET | `/api/scai/sede/{id}` | Dettaglio singola sede |
| POST | `/api/scai/sede` | Creazione nuova sede |
| PUT | `/api/scai/sede/{id}` | Aggiornamento sede esistente |
| DELETE | `/api/scai/sede/{id}` | Eliminazione logica sede |

### Parametri Lista (GET /api/scai/sede)

- `draw` - Numero richiesta (per DataTable)
- `start` - Offset paginazione
- `length` - Numero record per pagina
- `orderby` - Campo ordinamento
- `ordertype` - Tipo ordinamento (asc/desc)
- `cod_sede` - Filtro per codice sede (opzionale)
- `nome` - Filtro per nome (opzionale)
- `descrizione_breve` - Filtro per descrizione (opzionale)

### Risposta Lista

```json
{
  "draw": 1,
  "recordsTotal": 250,
  "recordsFiltered": 250,
  "data": [
    {
      "id": 1,
      "cod_sede": "S001",
      "nome": "Sede Centrale",
      "indirizzo": "Via Roma 1",
      "cap": "00100",
      "city_code": "H501",
      "province_code": "RM",
      "istat_region": "12",
      "nazione": "IT",
      "descrizione_breve": "Sede centrale Roma",
      "descrizione_lunga": "Sede centrale operativa - Via Roma 1, Roma",
      "created_at": "2024-01-01T10:00:00Z",
      "updated_at": "2024-01-01T10:00:00Z"
    }
  ]
}
```

## Frontend Components

### `sede-module`
Componente principale del modulo che gestisce il routing e la visualizzazione.

### `sede-component`
Componente per la gestione CRUD con tre viste:
- **Lista** - Tabella con filtri e paginazione
- **Dettaglio** - Visualizzazione dati singola sede (dati identificativi + dati geografici)
- **Form** - Form per creazione/modifica sede con validazione

### Funzionalità Frontend

- Ricerca con debouncing (400ms)
- Paginazione server-side
- Filtri per codice, nome e descrizione
- Validazione form (campi obbligatori + pattern CAP)
- Gestione stati di caricamento
- Conferma eliminazione
- Organizzazione dati in sezioni (identificativi/geografici)

## Installazione

### 1. Database Migration

Eseguire la migration SQL:

```bash
psql -U username -d database -f modules/sede/migration/V20260304_000002__sede.sql
```

### 2. Frontend

Importare il modulo nell'applicazione:

```javascript
import { SedeModuleComponent } from './modules/sede/gui/sede/index.js';
```

### 3. Routing

Configurare il routing per `/sede`:

```javascript
{
  path: '/sede',
  component: 'sede-module'
}
```

## Sviluppo Backend (Java - Futuro)

La cartella `java/` contiene stub per una futura implementazione backend Java/Spring Boot:

- `dto/SedeDTO.java` - Data Transfer Object
- `adapter/` - Adapter layer (da implementare)
- `dao/` - Data Access Object (da implementare)
- `handler/` - Request handlers (da implementare)

## Dati Geografici

Il modulo gestisce dati geografici completi:
- **Indirizzo**: via, numero civico
- **CAP**: codice avviamento postale (5 cifre)
- **Comune**: codice ISTAT a 4 caratteri
- **Provincia**: sigla provincia (es: RM, MI, NA)
- **Regione**: codice ISTAT a 2 cifre
- **Nazione**: codice ISO a 2 lettere (es: IT, FR, DE)

## Note Architetturali

- **Modularità**: Modulo completamente autonomo e riutilizzabile
- **Zero Accoppiamento**: Nessuna dipendenza da altri moduli
- **Soft Delete**: Implementato tramite campo `is_not_deleted`
- **Natural Key**: Il campo `cod_sede` funge da chiave naturale per relazioni cross-modulo
- **Normalizzazione Geografica**: Supporto codici ISTAT per integrazione con sistemi esterni

## Licenza

Proprietary - SCAI Team
