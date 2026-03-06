# Modulo BADGE

Modulo per la gestione dell'anagrafica badge e richieste di assegnazione badge nel sistema SCAI.

## ⚠️ ATTENZIONE - Inconsistenza Database Rilevata

Questo modulo presenta un'**inconsistenza critica** nel design del database originale:

- **scai_badge** usa `id_ente` (BIGINT, FK a `scai_ente.id`)
- **scai_badge_request** usa `cod_ente` (VARCHAR, natural key)

Questa inconsistenza:
- Rompe il pattern architetturale standard usato in altri moduli (policy, veicoli, rapporti)
- Complica i join tra badge e badge_request
- Impedisce FK dirette tra le due tabelle

**Soluzioni proposte** nella migration SQL. Il team dovrebbe valutare la standardizzazione.

---

## Descrizione

Questo modulo fornisce le funzionalità CRUD (Create, Read, Update, Delete) per:
- **Badge**: Anagrafica completa badge dipendenti con validità, tecnologia e stato
- **Badge Tipo**: Lookup interno per tipologie di badge
- **Badge Causale Emissione**: Lookup interno per causali di emissione
- **Badge Request**: Richieste di assegnazione badge con workflow

È un modulo di tipo **ENTITY** che gestisce l'identificazione e l'accesso dei dipendenti.

## Tipo Modulo

**Entity** - Gestisce entità di business con dipendenze da moduli lookup

## Dipendenze

- **ente** (^1.0.0) - Anagrafica enti (required)
- **rapporti** (^1.0.0) - Anagrafica dipendenti (required)
- **Installation Order**: 30

## Moduli Dipendenti

Questo modulo è utilizzato da:
- `timbrature` - Gestione timbrature accessi
- Sistemi di campo - Per controllo accessi

## Struttura Dati

### Tabella: `scai_badge_tipo`

Lookup table per tipologie di badge.

| Campo | Tipo | Descrizione |
|-------|------|-------------|
| id | BIGSERIAL | Chiave primaria |
| cod_tipo_badge | VARCHAR(2) | Codice univoco tipologia badge |
| descrizione_tipo_badge | VARCHAR(255) | Descrizione tipologia |
| is_not_deleted | SMALLINT | Soft delete (1=attivo, NULL=cancellato) |
| created_at | TIMESTAMP | Data creazione |
| updated_at | TIMESTAMP | Data ultimo aggiornamento |

#### Vincoli
- **Unique Key**: `(cod_tipo_badge, is_not_deleted)`

#### Indici
- `idx_badge_tipo_cod` - Indice su cod_tipo_badge
- `idx_badge_tipo_is_not_deleted` - Indice su soft delete

---

### Tabella: `scai_badge_causale_emissione`

Lookup table per causali di emissione badge.

| Campo | Tipo | Descrizione |
|-------|------|-------------|
| id | BIGSERIAL | Chiave primaria |
| cod_causale_emissione | VARCHAR(8) | Codice univoco causale |
| desc_causale_emissione | VARCHAR(255) | Descrizione causale emissione |
| created_at | TIMESTAMP | Data creazione |
| updated_at | TIMESTAMP | Data ultimo aggiornamento |

#### Vincoli
- **Unique Key**: `(cod_causale_emissione)`

#### Indici
- `idx_badge_causale_cod` - Indice su cod_causale_emissione

---

### Tabella: `scai_badge`

Anagrafica principale badge dipendenti.

| Campo | Tipo | Descrizione |
|-------|------|-------------|
| id | BIGSERIAL | Chiave primaria |
| seq_id | INTEGER | ID sequenziale |
| id_ente | BIGINT | ⚠️ ID ente (FK a scai_ente.id) - INCONSISTENZA! |
| id_tipo_tessera | BIGINT | FK a scai_badge_tipo.id |
| numero | VARCHAR(20) | Numero badge (univoco) |
| matricola | VARCHAR(15) | Matricola dipendente |
| cod_fis | VARCHAR(16) | Codice fiscale |
| cognome | VARCHAR(128) | Cognome |
| nome | VARCHAR(128) | Nome |
| tecnologia | VARCHAR(8) | Tecnologia badge (RFID, NFC, etc.) |
| data_inizio_validita | TIMESTAMP | Data inizio validità |
| data_fine_validita | TIMESTAMP | Data fine validità |
| data_produzione | TIMESTAMP | Data produzione badge |
| data_ritiro | TIMESTAMP | Data ritiro badge |
| attivo | SMALLINT | Stato attivazione (1=attivo, 0=disattivo) |
| created_at | TIMESTAMP | Data creazione |
| updated_at | TIMESTAMP | Data ultimo aggiornamento |
| is_not_deleted | SMALLINT | Soft delete (1=attivo, NULL=cancellato) |

#### Vincoli
- **Unique Key**: `(matricola, id_ente, is_not_deleted)`
- **Foreign Key** (opzionale): `id_ente` → `scai_ente(id)` ⚠️ usa ID invece di cod_ente
- **Foreign Key** (opzionale): `id_tipo_tessera` → `scai_badge_tipo(id)`

#### Indici
- `idx_badge_id_ente` - Indice su id_ente
- `idx_badge_matricola` - Indice su matricola
- `idx_badge_numero` - Indice su numero per ricerche
- `idx_badge_cod_fis` - Indice su codice fiscale
- `idx_badge_id_tipo_tessera` - Indice su id_tipo_tessera per join
- `idx_badge_attivo` - Indice su attivo per filtri
- `idx_badge_is_not_deleted` - Indice su soft delete

---

### Tabella: `scai_badge_request`

Richieste di assegnazione badge con workflow.

| Campo | Tipo | Descrizione |
|-------|------|-------------|
| id | BIGSERIAL | Chiave primaria |
| cod_ente | VARCHAR(10) | ⚠️ Codice ente (natural key) - INCONSISTENZA! |
| matricola | VARCHAR(20) | Matricola dipendente |
| cod_fis | VARCHAR(16) | Codice fiscale |
| data_inizio_validita | TIMESTAMP | Data inizio validità richiesta |
| instance_id | BIGINT | ID istanza workflow (integrazione esterna) |
| is_not_deleted | SMALLINT | Soft delete (1=attivo, NULL=cancellato) |
| created_at | TIMESTAMP | Data creazione |
| updated_at | TIMESTAMP | Data ultimo aggiornamento |

#### Vincoli
- **Unique Key**: `(cod_ente, matricola, is_not_deleted)`
- **Foreign Key** (opzionale): `(cod_ente, matricola)` → `scai_rapporto` ⚠️ incompatibile con scai_badge

#### Indici
- `idx_badge_request_cod_ente` - Indice su cod_ente
- `idx_badge_request_matricola` - Indice su matricola
- `idx_badge_request_cod_fis` - Indice su codice fiscale
- `idx_badge_request_instance_id` - Indice su instance_id per workflow
- `idx_badge_request_is_not_deleted` - Indice su soft delete

---

## API Endpoints

### Badge (Principale)

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | `/api/scai/badge` | Lista badge con paginazione e filtri |
| GET | `/api/scai/badge/{id}` | Dettaglio singolo badge |
| POST | `/api/scai/badge` | Creazione nuovo badge |
| PUT | `/api/scai/badge/{id}` | Aggiornamento badge esistente |
| DELETE | `/api/scai/badge/{id}` | Eliminazione logica badge |

#### Parametri Lista (GET /api/scai/badge)

- `draw` - Numero richiesta (per DataTable)
- `start` - Offset paginazione
- `length` - Numero record per pagina
- `orderby` - Campo ordinamento
- `ordertype` - Tipo ordinamento (asc/desc)
- `numero` - Filtro per numero badge (opzionale)
- `matricola` - Filtro per matricola (opzionale)
- `cognome` - Filtro per cognome (opzionale)
- `id_ente` - Filtro per ente (opzionale) ⚠️ usa ID non cod_ente
- `id_tipo_tessera` - Filtro per tipo badge (opzionale)
- `attivo` - Filtro per stato (opzionale: 1=attivo, 0=disattivo)

#### Risposta Lista

```json
{
  "draw": 1,
  "recordsTotal": 1500,
  "recordsFiltered": 1500,
  "data": [
    {
      "id": 1,
      "seq_id": 100,
      "id_ente": 5,
      "descrizione_ente": "Ente Regionale",
      "id_tipo_tessera": 1,
      "descrizione_tipo_badge": "Badge Standard",
      "numero": "B001234",
      "matricola": "12345",
      "cod_fis": "RSSMRA80A01H501U",
      "cognome": "Rossi",
      "nome": "Mario",
      "tecnologia": "RFID",
      "data_inizio_validita": "2024-01-01T00:00:00Z",
      "data_fine_validita": "2025-12-31T23:59:59Z",
      "data_produzione": "2023-12-15T10:00:00Z",
      "data_ritiro": null,
      "attivo": 1,
      "created_at": "2024-01-01T10:00:00Z",
      "updated_at": "2024-01-01T10:00:00Z"
    }
  ]
}
```

---

### Badge Tipo (Lookup)

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | `/api/scai/badge/tipo` | Lista tipi badge |
| GET | `/api/scai/badge/tipo/{id}` | Dettaglio singolo tipo |
| POST | `/api/scai/badge/tipo` | Creazione nuovo tipo |
| PUT | `/api/scai/badge/tipo/{id}` | Aggiornamento tipo |
| DELETE | `/api/scai/badge/tipo/{id}` | Eliminazione logica tipo |

---

### Badge Causale (Lookup)

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | `/api/scai/badge/causale` | Lista causali emissione |
| GET | `/api/scai/badge/causale/{id}` | Dettaglio singola causale |
| POST | `/api/scai/badge/causale` | Creazione nuova causale |
| PUT | `/api/scai/badge/causale/{id}` | Aggiornamento causale |
| DELETE | `/api/scai/badge/causale/{id}` | Eliminazione causale |

---

### Badge Request

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | `/api/scai/badge/request` | Lista richieste badge |
| GET | `/api/scai/badge/request/{id}` | Dettaglio singola richiesta |
| POST | `/api/scai/badge/request` | Creazione nuova richiesta |
| PUT | `/api/scai/badge/request/{id}` | Aggiornamento richiesta |
| DELETE | `/api/scai/badge/request/{id}` | Eliminazione logica richiesta |

---

## Frontend Components

### `badge-module`
Componente principale del modulo che gestisce il routing e la visualizzazione.

### `badge-component`
Componente per la gestione CRUD dei badge con tre viste:
- **Lista** - Tabella con 6 filtri e paginazione
- **Dettaglio** - Visualizzazione dati completi badge con date validità
- **Form** - Form per creazione/modifica con validazione codice fiscale

### Funzionalità Frontend

- Ricerca con debouncing (400ms)
- Paginazione server-side
- **6 filtri**: numero, matricola, cognome, ente (select), tipo (select), stato attivo/disattivo
- **2 select dinamiche** caricate da API:
  - `/api/scai/ente` per enti ⚠️ attenzione: usa ID non cod_ente
  - `/api/scai/badge/tipo` per tipi badge
- Validazione codice fiscale (pattern italiano)
- Gestione 4 date: inizio/fine validità, produzione, ritiro
- Badge colorati per stato: verde (attivo), rosso (disattivo)
- Visualizzazione ente e tipo con badge
- Input datetime-local per date
- Toggle stato attivo/disattivo

---

## Installazione

### 1. Prerequisiti

Assicurarsi che i moduli **ente** e **rapporti** siano installati:

```bash
# Verificare che le tabelle esistano
psql -U username -d database -c "SELECT COUNT(*) FROM scai_ente;"
psql -U username -d database -c "SELECT COUNT(*) FROM scai_rapporto;"
```

### 2. Database Migration

Eseguire la migration SQL:

```bash
psql -U username -d database -f modules/badge/migration/V20260304_000030__badge.sql
```

⚠️ **IMPORTANTE**: Leggere attentamente i commenti nella migration riguardo l'inconsistenza id_ente/cod_ente.

### 3. Frontend

Importare il modulo nell'applicazione:

```javascript
import { BadgeModuleComponent } from './modules/badge/gui/badge/index.js';
```

### 4. Routing

Configurare il routing per `/badge`:

```javascript
{
  path: '/badge',
  component: 'badge-module'
}
```

### 5. Dati Iniziali (Opzionale)

Popolare le tabelle lookup:

```sql
-- Tipi Badge
INSERT INTO scai_badge_tipo (cod_tipo_badge, descrizione_tipo_badge) VALUES
('ST', 'Badge Standard'),
('TE', 'Badge Temporaneo'),
('VI', 'Badge Visitatore'),
('EM', 'Badge Emergenza');

-- Causali Emissione
INSERT INTO scai_badge_causale_emissione (cod_causale_emissione, desc_causale_emissione) VALUES
('NUOVA', 'Nuova assunzione'),
('SOST', 'Sostituzione badge danneggiato'),
('PERD', 'Smarrimento badge'),
('SCAD', 'Scadenza validità');
```

---

## Sviluppo Backend (Java - Futuro)

La cartella `java/` contiene stub per una futura implementazione backend Java/Spring Boot:

- `dto/BadgeDTO.java` - Data Transfer Object badge ⚠️ con nota inconsistenza
- `dto/BadgeTipoDTO.java` - Data Transfer Object tipo
- `dto/BadgeCausaleEmissioneDTO.java` - Data Transfer Object causale
- `dto/BadgeRequestDTO.java` - Data Transfer Object request ⚠️ con nota inconsistenza
- `adapter/` - Adapter layer (da implementare)
- `dao/` - Data Access Object (da implementare)
- `handler/` - Request handlers (da implementare)

---

## Relazioni con Altri Moduli

### Dipendenza da Ente

⚠️ **ATTENZIONE**: Badge usa id_ente (BIGINT) invece di cod_ente (VARCHAR):

```sql
-- Join badge con ente (usa ID)
SELECT b.*, e.descrizione_ente
FROM scai_badge b
LEFT JOIN scai_ente e ON b.id_ente = e.id
WHERE b.is_not_deleted = 1;
```

### Dipendenza da Rapporti (Indiretta)

Badge contiene matricola ma non FK diretta:

```sql
-- Join badge con rapporti (via matricola, senza FK formale)
SELECT b.*, r.nome, r.cognome
FROM scai_badge b
LEFT JOIN scai_rapporto r ON b.matricola = r.matricola
WHERE b.is_not_deleted = 1;
```

⚠️ Questo join può generare duplicati se la matricola non è univoca.

### Lookup Interne al Modulo

Tipo badge gestito internamente:

```sql
SELECT b.*, bt.descrizione_tipo_badge
FROM scai_badge b
LEFT JOIN scai_badge_tipo bt ON b.id_tipo_tessera = bt.id
WHERE b.is_not_deleted = 1;
```

### Relazione con Badge Request

⚠️ **PROBLEMA**: Impossibile join diretto a causa inconsistenza chiavi:

```sql
-- Join problematico: id_ente vs cod_ente
SELECT b.*, br.*
FROM scai_badge b
LEFT JOIN scai_badge_request br
  ON b.matricola = br.matricola
  AND ??? -- Come joinare id_ente con cod_ente?
WHERE b.is_not_deleted = 1;

-- Soluzione temporanea: join via scai_ente
SELECT b.*, br.*
FROM scai_badge b
LEFT JOIN scai_ente e ON b.id_ente = e.id
LEFT JOIN scai_badge_request br
  ON br.cod_ente = e.cod_ente
  AND br.matricola = b.matricola
WHERE b.is_not_deleted = 1;
```

---

## Note Architetturali

- **Modularità**: Modulo autonomo con dipendenze da ente e rapporti
- **Multiple Entities**: Gestisce 4 tabelle (1 principale + 2 lookup + 1 request)
- **⚠️ INCONSISTENZA CRITICA**: id_ente (BIGINT) in badge vs cod_ente (VARCHAR) in badge_request
- **Impatto Inconsistenza**: Complica join, prestazioni ridotte, impossibile FK diretta
- **Soft Delete**: Implementato su tutte le tabelle
- **Natural Keys Parziali**: numero badge univoco, ma matricola non ha FK formale
- **Foreign Key Opzionali**: Alcune FK commentate per flessibilità
- **Temporal Validity**: Date inizio/fine validità per controllo accessi temporizzati
- **Workflow Integration**: instance_id in badge_request per integrazione sistemi esterni
- **Technology Field**: Campo tecnologia per supporto multi-standard (RFID, NFC, QR, etc.)
- **Status Management**: Campo attivo per disattivazione badge senza eliminazione

---

## Scenari d'Uso

### Scenario 1: Emissione Nuovo Badge
1. Creare richiesta in badge_request con workflow
2. Approvare richiesta (instance_id workflow)
3. Creare badge in scai_badge
4. Associare tipo badge e tecnologia
5. Impostare date validità

### Scenario 2: Sostituzione Badge Danneggiato
1. Disattivare badge esistente (attivo = 0)
2. Creare nuovo badge con stesso dipendente
3. Impostare causale "SOST"
4. Aggiornare date validità

### Scenario 3: Scadenza Badge
1. Query badge con data_fine_validita < NOW()
2. Disattivare badge scaduti automaticamente
3. Generare notifiche rinnovo

### Scenario 4: Report Badge Attivi per Ente
```sql
SELECT e.descrizione_ente, COUNT(*) AS badge_attivi
FROM scai_badge b
LEFT JOIN scai_ente e ON b.id_ente = e.id
WHERE b.attivo = 1
  AND b.is_not_deleted = 1
  AND b.data_fine_validita > NOW()
GROUP BY e.descrizione_ente
ORDER BY badge_attivi DESC;
```

---

## Risoluzione Inconsistenza Consigliata

### Opzione 1: Standardizzare su cod_ente (VARCHAR) - **CONSIGLIATA**

**Pro**:
- Coerenza con policy, veicoli, rapporti
- Segue pattern architetturale generale
- FK dirette tra badge e badge_request

**Contro**:
- Richiede migration dati esistenti
- Modifica struttura tabella principale

**Steps**:
```sql
-- 1. Aggiungi colonna cod_ente a scai_badge
ALTER TABLE scai_badge ADD COLUMN cod_ente VARCHAR(15);

-- 2. Popola da scai_ente
UPDATE scai_badge b
SET cod_ente = e.cod_ente
FROM scai_ente e
WHERE b.id_ente = e.id;

-- 3. Rimuovi id_ente (dopo verifica)
ALTER TABLE scai_badge DROP COLUMN id_ente;

-- 4. Aggiungi constraint
ALTER TABLE scai_badge ADD CONSTRAINT uq_badge_cod_ente_matricola
  UNIQUE (cod_ente, matricola, is_not_deleted);
```

### Opzione 2: Standardizzare su id_ente (BIGINT)

**Pro**:
- Minimizza modifiche su tabella principale
- Performance join migliori (INTEGER vs VARCHAR)

**Contro**:
- Perde coerenza con pattern cross-module
- Richiede modifica badge_request

### Opzione 3: Mantenere Entrambe (Status Quo)

**Pro**:
- Nessuna migration immediata
- Zero downtime

**Contro**:
- Complessità query persistente
- Join con doppio passaggio via scai_ente
- Prestazioni ridotte

---

## Licenza

Proprietary - SCAI Team
