# Modulo VEICOLI

Modulo per la gestione dell'anagrafica veicoli e delle convenzioni tra veicoli e dipendenti nel sistema SCAI.

## Descrizione

Questo modulo fornisce le funzionalità CRUD (Create, Read, Update, Delete) per:
- **Veicoli**: Anagrafica completa con targa, modello, marca, tipo e alimentazione
- **Tipi Veicolo**: Lookup interno per tipologie (Auto, Moto, Furgone, etc.)
- **Alimentazioni**: Lookup interno per tipi di alimentazione (Benzina, Diesel, Elettrico, etc.)
- **Convenzioni**: Associazioni tra veicoli e dipendenti con stato autorizzazione

È un modulo di tipo **ENTITY** che dipende dai moduli `ente` e `rapporti`.

## Tipo Modulo

**Entity** - Gestisce entità di business con dipendenze da altri moduli

## Dipendenze

- **ente** (^1.0.0) - Anagrafica enti (required)
- **rapporti** (^1.0.0) - Anagrafica dipendenti (required)
- **Installation Order**: 20

## Moduli Dipendenti

Questo modulo è utilizzato come riferimento da:
- Moduli futuri che necessitano di gestire veicoli (es: badge, policy, accessi)

## Struttura Dati

### Tabella: `scai_veicolo_tipo`

Lookup table per tipologie di veicoli.

| Campo | Tipo | Descrizione |
|-------|------|-------------|
| id | BIGSERIAL | Chiave primaria |
| codice_tipo | VARCHAR(10) | Codice univoco tipologia |
| descrizione | VARCHAR(128) | Descrizione tipologia veicolo |
| created_at | TIMESTAMP | Data creazione |
| created_by | BIGINT | User ID creazione |
| updated_at | TIMESTAMP | Data ultimo aggiornamento |
| updated_by | BIGINT | User ID aggiornamento |
| is_not_deleted | SMALLINT | Soft delete (1=attivo, NULL=cancellato) |

#### Vincoli
- **Unique Key**: `(codice_tipo, is_not_deleted)` - Garantisce unicità del codice

#### Indici
- `idx_veicolo_tipo_codice` - Indice su codice_tipo
- `idx_veicolo_tipo_is_not_deleted` - Indice su soft delete

---

### Tabella: `scai_veicolo_alimentazione`

Lookup table per tipi di alimentazione.

| Campo | Tipo | Descrizione |
|-------|------|-------------|
| id | BIGSERIAL | Chiave primaria |
| codice_alimentazione | VARCHAR(10) | Codice univoco alimentazione |
| descrizione | VARCHAR(128) | Descrizione tipo alimentazione |
| created_at | TIMESTAMP | Data creazione |
| created_by | BIGINT | User ID creazione |
| updated_at | TIMESTAMP | Data ultimo aggiornamento |
| updated_by | BIGINT | User ID aggiornamento |
| is_not_deleted | SMALLINT | Soft delete (1=attivo, NULL=cancellato) |

#### Vincoli
- **Unique Key**: `(codice_alimentazione, is_not_deleted)` - Garantisce unicità del codice

#### Indici
- `idx_veicolo_alimentazione_codice` - Indice su codice_alimentazione
- `idx_veicolo_alimentazione_is_not_deleted` - Indice su soft delete

---

### Tabella: `scai_veicolo`

Anagrafica principale dei veicoli.

| Campo | Tipo | Descrizione |
|-------|------|-------------|
| id | BIGSERIAL | Chiave primaria |
| targa | VARCHAR(20) | Targa veicolo (natural key) |
| modello | VARCHAR(128) | Modello veicolo |
| marca | VARCHAR(128) | Marca veicolo |
| colore | VARCHAR(64) | Colore veicolo |
| anno_immatricolazione | INTEGER | Anno di immatricolazione |
| veicolo_tipo_id | BIGINT | FK a scai_veicolo_tipo |
| veicolo_alimentazione_id | BIGINT | FK a scai_veicolo_alimentazione |
| note | TEXT | Note aggiuntive |
| created_at | TIMESTAMP | Data creazione |
| created_by | BIGINT | User ID creazione |
| updated_at | TIMESTAMP | Data ultimo aggiornamento |
| updated_by | BIGINT | User ID aggiornamento |
| is_not_deleted | SMALLINT | Soft delete (1=attivo, NULL=cancellato) |

#### Vincoli
- **Unique Key**: `(targa, is_not_deleted)` - Garantisce unicità della targa
- **Foreign Key** (opzionale): `veicolo_tipo_id` → `scai_veicolo_tipo(id)`
- **Foreign Key** (opzionale): `veicolo_alimentazione_id` → `scai_veicolo_alimentazione(id)`

#### Indici
- `idx_veicolo_targa` - Indice su targa per ricerche veloci
- `idx_veicolo_tipo_id` - Indice su veicolo_tipo_id per join
- `idx_veicolo_alimentazione_id` - Indice su veicolo_alimentazione_id per join
- `idx_veicolo_is_not_deleted` - Indice su soft delete

---

### Tabella: `scai_veicolo_convenzione`

Associazione tra veicoli e dipendenti con stato autorizzazione.

| Campo | Tipo | Descrizione |
|-------|------|-------------|
| id | BIGSERIAL | Chiave primaria |
| cod_ente | VARCHAR(15) | Codice ente (FK cross-module) |
| matricola | VARCHAR(16) | Matricola dipendente (FK cross-module) |
| targa | VARCHAR(20) | Targa veicolo (FK a scai_veicolo) |
| data_inizio | DATE | Data inizio convenzione |
| data_fine | DATE | Data fine convenzione |
| status | VARCHAR(20) | Stato: INSERITO, AUTORIZZATO |
| note | TEXT | Note aggiuntive |
| created_at | TIMESTAMP | Data creazione |
| created_by | BIGINT | User ID creazione |
| updated_at | TIMESTAMP | Data ultimo aggiornamento |
| updated_by | BIGINT | User ID aggiornamento |
| is_not_deleted | SMALLINT | Soft delete (1=attivo, NULL=cancellato) |

#### Vincoli
- **Unique Key**: `(cod_ente, matricola, targa, is_not_deleted)` - Garantisce unicità dell'associazione
- **Check Constraint**: `status IN ('INSERITO', 'AUTORIZZATO')`
- **Foreign Key** (opzionale): `(cod_ente, matricola)` → `scai_rapporto(cod_ente, matricola)`
- **Foreign Key** (opzionale): `targa` → `scai_veicolo(targa)`

#### Indici
- `idx_veicolo_convenzione_cod_ente` - Indice su cod_ente
- `idx_veicolo_convenzione_matricola` - Indice su matricola
- `idx_veicolo_convenzione_targa` - Indice su targa
- `idx_veicolo_convenzione_status` - Indice su status per filtri
- `idx_veicolo_convenzione_is_not_deleted` - Indice su soft delete

---

## API Endpoints

### Veicolo (Principale)

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | `/api/scai/veicolo` | Lista veicoli con paginazione e filtri |
| GET | `/api/scai/veicolo/{id}` | Dettaglio singolo veicolo |
| POST | `/api/scai/veicolo` | Creazione nuovo veicolo |
| PUT | `/api/scai/veicolo/{id}` | Aggiornamento veicolo esistente |
| DELETE | `/api/scai/veicolo/{id}` | Eliminazione logica veicolo |

#### Parametri Lista (GET /api/scai/veicolo)

- `draw` - Numero richiesta (per DataTable)
- `start` - Offset paginazione
- `length` - Numero record per pagina
- `orderby` - Campo ordinamento
- `ordertype` - Tipo ordinamento (asc/desc)
- `targa` - Filtro per targa (opzionale)
- `modello` - Filtro per modello (opzionale)
- `marca` - Filtro per marca (opzionale)
- `veicolo_tipo_id` - Filtro per tipo (opzionale)
- `veicolo_alimentazione_id` - Filtro per alimentazione (opzionale)

#### Risposta Lista

```json
{
  "draw": 1,
  "recordsTotal": 150,
  "recordsFiltered": 150,
  "data": [
    {
      "id": 1,
      "targa": "AB123CD",
      "modello": "Giulia",
      "marca": "Alfa Romeo",
      "colore": "Rosso",
      "anno_immatricolazione": 2020,
      "veicolo_tipo_id": 1,
      "veicolo_alimentazione_id": 2,
      "tipo_descrizione": "Auto",
      "alimentazione_descrizione": "Diesel",
      "note": null,
      "created_at": "2024-01-01T10:00:00Z",
      "created_by": 1,
      "updated_at": "2024-01-01T10:00:00Z",
      "updated_by": 1
    }
  ]
}
```

---

### Veicolo Tipo (Lookup)

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | `/api/scai/veicolo/tipo` | Lista tipi veicolo |
| GET | `/api/scai/veicolo/tipo/{id}` | Dettaglio singolo tipo |
| POST | `/api/scai/veicolo/tipo` | Creazione nuovo tipo |
| PUT | `/api/scai/veicolo/tipo/{id}` | Aggiornamento tipo |
| DELETE | `/api/scai/veicolo/tipo/{id}` | Eliminazione logica tipo |

---

### Veicolo Alimentazione (Lookup)

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | `/api/scai/veicolo/alimentazione` | Lista alimentazioni |
| GET | `/api/scai/veicolo/alimentazione/{id}` | Dettaglio singola alimentazione |
| POST | `/api/scai/veicolo/alimentazione` | Creazione nuova alimentazione |
| PUT | `/api/scai/veicolo/alimentazione/{id}` | Aggiornamento alimentazione |
| DELETE | `/api/scai/veicolo/alimentazione/{id}` | Eliminazione logica alimentazione |

---

### Convenzione

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | `/api/scai/veicolo/convenzione` | Lista convenzioni |
| GET | `/api/scai/veicolo/convenzione/{id}` | Dettaglio singola convenzione |
| POST | `/api/scai/veicolo/convenzione` | Creazione nuova convenzione |
| PUT | `/api/scai/veicolo/convenzione/{id}` | Aggiornamento convenzione |
| DELETE | `/api/scai/veicolo/convenzione/{id}` | Eliminazione logica convenzione |

---

## Frontend Components

### `veicoli-module`
Componente principale del modulo che gestisce il routing e la visualizzazione.

### `veicoli-component`
Componente per la gestione CRUD dei veicoli con tre viste:
- **Lista** - Tabella con 5 filtri (targa, modello, marca, tipo select, alimentazione select) e paginazione
- **Dettaglio** - Visualizzazione dati singolo veicolo con audit trail completo
- **Form** - Form per creazione/modifica con 2 select dinamiche (Tipo + Alimentazione)

### Funzionalità Frontend

- Ricerca con debouncing (400ms)
- Paginazione server-side
- **5 filtri**: targa, modello, marca, tipo (select dinamica), alimentazione (select dinamica)
- **2 select dinamiche** caricate da API:
  - `/api/scai/veicolo/tipo` per tipo veicolo
  - `/api/scai/veicolo/alimentazione` per alimentazione
- Validazione form (targa required, anno numerico 1900-2100)
- Gestione stati di caricamento
- Conferma eliminazione
- Badge colorati per tipo (bg-info) e alimentazione (bg-success)
- Audit trail con created_by/updated_by
- Targa non modificabile in edit (natural key)

---

## Installazione

### 1. Prerequisiti

Assicurarsi che i moduli **ente** e **rapporti** siano installati prima di procedere:

```bash
# Verificare che le tabelle esistano
psql -U username -d database -c "SELECT COUNT(*) FROM scai_ente;"
psql -U username -d database -c "SELECT COUNT(*) FROM scai_rapporto;"
```

### 2. Database Migration

Eseguire la migration SQL:

```bash
psql -U username -d database -f modules/veicoli/migration/V20260304_000020__veicoli.sql
```

### 3. Frontend

Importare il modulo nell'applicazione:

```javascript
import { VeicoliModuleComponent } from './modules/veicoli/gui/veicoli/index.js';
```

### 4. Routing

Configurare il routing per `/veicoli`:

```javascript
{
  path: '/veicoli',
  component: 'veicoli-module'
}
```

### 5. Dati Iniziali (Opzionale)

Popolare le tabelle lookup con dati iniziali:

```sql
-- Tipi Veicolo
INSERT INTO scai_veicolo_tipo (codice_tipo, descrizione) VALUES
('AUTO', 'Autovettura'),
('MOTO', 'Motocicletta'),
('FURG', 'Furgone'),
('CAMION', 'Camion'),
('BUS', 'Autobus');

-- Alimentazioni
INSERT INTO scai_veicolo_alimentazione (codice_alimentazione, descrizione) VALUES
('BENZ', 'Benzina'),
('DIESEL', 'Diesel'),
('GPL', 'GPL'),
('METN', 'Metano'),
('EL', 'Elettrico'),
('IBR', 'Ibrido');
```

---

## Sviluppo Backend (Java - Futuro)

La cartella `java/` contiene stub per una futura implementazione backend Java/Spring Boot:

- `dto/VeicoloDTO.java` - Data Transfer Object veicolo
- `dto/VeicoloTipoDTO.java` - Data Transfer Object tipo
- `dto/VeicoloAlimentazioneDTO.java` - Data Transfer Object alimentazione
- `dto/VeicoloConvenzioneDTO.java` - Data Transfer Object convenzione
- `adapter/` - Adapter layer (da implementare)
- `dao/` - Data Access Object (da implementare)
- `handler/` - Request handlers (da implementare)

---

## Relazioni con Altri Moduli

### Dipendenza da Ente

Le convenzioni fanno riferimento a enti:

```sql
SELECT vc.*, e.descrizione_ente
FROM scai_veicolo_convenzione vc
LEFT JOIN scai_ente e ON vc.cod_ente = e.cod_ente
WHERE vc.is_not_deleted = 1;
```

### Dipendenza da Rapporti

Le convenzioni associano veicoli a dipendenti (rapporti):

```sql
SELECT vc.*, r.nome, r.cognome, v.modello, v.marca
FROM scai_veicolo_convenzione vc
LEFT JOIN scai_rapporto r ON vc.cod_ente = r.cod_ente AND vc.matricola = r.matricola
LEFT JOIN scai_veicolo v ON vc.targa = v.targa
WHERE vc.is_not_deleted = 1;
```

### Lookup Interne al Modulo

Tipo e alimentazione sono lookup interne gestite completamente dal modulo:

```sql
SELECT v.*, vt.descrizione AS tipo_desc, va.descrizione AS alimentazione_desc
FROM scai_veicolo v
LEFT JOIN scai_veicolo_tipo vt ON v.veicolo_tipo_id = vt.id
LEFT JOIN scai_veicolo_alimentazione va ON v.veicolo_alimentazione_id = va.id
WHERE v.is_not_deleted = 1;
```

---

## Note Architetturali

- **Modularità**: Modulo autonomo con dipendenze verso ente e rapporti
- **Lookup Interne**: Tipo e alimentazione sono gestite internamente al modulo
- **Cross-Module Relations**: Convenzioni collegano veicoli a dipendenti via natural key `(cod_ente, matricola)`
- **Soft Delete**: Implementato su tutte le tabelle tramite campo `is_not_deleted`
- **Natural Key**: Targa come chiave naturale per veicolo, non modificabile in edit
- **Foreign Key Opzionali**: Le FK sono commentate, gestite a livello applicativo
- **Audit Trail Completo**: Tracciamento created_by/updated_by oltre ai timestamp
- **Stati Convenzione**: Workflow INSERITO → AUTORIZZATO con check constraint
- **Multiple Entities**: Modulo complesso con 4 tabelle correlate (1 principale + 2 lookup + 1 associazione)

---

## Scenari d'Uso

### Scenario 1: Registrazione Nuovo Veicolo
1. Creare record in `scai_veicolo` con targa, modello, marca
2. Associare tipo e alimentazione se necessario
3. Creare convenzione in `scai_veicolo_convenzione` collegando a dipendente

### Scenario 2: Autorizzazione Convenzione
1. Ricercare convenzione per dipendente
2. Verificare dati veicolo e periodo validità
3. Aggiornare `status` da 'INSERITO' a 'AUTORIZZATO'

### Scenario 3: Report Veicoli per Dipendente
1. Query join tra `scai_veicolo_convenzione` e `scai_rapporto`
2. Filtrare per `cod_ente` e `matricola`
3. Includere dettagli veicolo (modello, marca, targa)

---

## Licenza

Proprietary - SCAI Team
