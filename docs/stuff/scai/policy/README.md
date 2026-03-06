# Modulo POLICY

Modulo per la gestione delle policy di accesso associate ai rapporti dipendenti nel sistema SCAI.

## Descrizione

Questo modulo fornisce le funzionalità CRUD (Create, Read, Update, Delete) per:
- **Policy Rapporto**: Policy di accesso associate a singoli dipendenti
- **Policy Default Sede**: Policy di default configurate per sede
- **Policy Rapporto Validity**: Validità temporale delle policy

È un modulo di tipo **ENTITY** che gestisce le autorizzazioni di accesso ai sistemi di campo per i dipendenti.

## Tipo Modulo

**Entity** - Gestisce entità di business con dipendenze da moduli lookup

## Dipendenze

- **ente** (^1.0.0) - Anagrafica enti (required)
- **sede** (^1.0.0) - Anagrafica sedi (required)
- **rapporti** (^1.0.0) - Anagrafica dipendenti (required)
- **sdc** (^1.0.0) - Sistemi di campo (required)
- **repertorio** (^1.0.0) - Repertori (required)
- **Installation Order**: 25

## Moduli Dipendenti

Questo modulo può essere utilizzato da:
- `badge` - Gestione badge accessi
- `timbrature` - Gestione timbrature

## Struttura Dati

### Tabella: `scai_policy_rapporto`

Tabella principale per le policy associate ai rapporti dipendenti.

| Campo | Tipo | Descrizione |
|-------|------|-------------|
| id | BIGSERIAL | Chiave primaria |
| cod_ente | VARCHAR(15) | Codice ente (FK cross-module) |
| matricola | VARCHAR(15) | Matricola dipendente (FK cross-module) |
| slug_sdc | VARCHAR(64) | Slug sistema di campo (FK) |
| codice_repertorio | VARCHAR(6) | Codice repertorio (FK) |
| codice_policy | VARCHAR(16) | Codice policy di accesso |
| data_inizio_validita | TIMESTAMP | Data inizio validità policy |
| created_at | TIMESTAMP | Data creazione |
| created_by | VARCHAR(255) | User creazione |
| updated_at | TIMESTAMP | Data ultimo aggiornamento |
| updated_by | VARCHAR(255) | User aggiornamento |
| is_not_deleted | SMALLINT | Soft delete (1=attivo, NULL=cancellato) |

#### Vincoli
- **Unique Key**: `(cod_ente, matricola, codice_repertorio, codice_policy, is_not_deleted)`
- **Foreign Key** (opzionale): `(cod_ente, matricola)` → `scai_rapporto`
- **Foreign Key** (opzionale): `slug_sdc` → `scai_sistemi_campo(slug)`
- **Foreign Key** (opzionale): `codice_repertorio` → `scai_repertorio`

#### Indici
- `idx_policy_rapporto_cod_ente` - Indice su cod_ente
- `idx_policy_rapporto_matricola` - Indice su matricola
- `idx_policy_rapporto_slug_sdc` - Indice su slug_sdc
- `idx_policy_rapporto_codice_repertorio` - Indice su codice_repertorio
- `idx_policy_rapporto_codice_policy` - Indice su codice_policy
- `idx_policy_rapporto_is_not_deleted` - Indice su soft delete

---

### Tabella: `scai_policy_default_sede`

Policy di default configurate per sede.

| Campo | Tipo | Descrizione |
|-------|------|-------------|
| id | BIGSERIAL | Chiave primaria |
| codice_policy | VARCHAR(16) | Codice policy di default |
| codice_repertorio | VARCHAR(6) | Codice repertorio (FK) |
| cod_ente | VARCHAR(15) | Codice ente (FK) |
| cod_sede | VARCHAR(10) | Codice sede (FK) |
| created_at | TIMESTAMP | Data creazione |
| updated_at | TIMESTAMP | Data ultimo aggiornamento |
| is_not_deleted | SMALLINT | Soft delete (1=attivo, NULL=cancellato) |

#### Vincoli
- **Unique Key**: `(codice_policy, codice_repertorio, cod_ente, cod_sede, is_not_deleted)`
- **Foreign Key** (opzionale): `cod_ente` → `scai_ente(cod_ente)`
- **Foreign Key** (opzionale): `(cod_ente, cod_sede)` → `scai_sede`
- **Foreign Key** (opzionale): `codice_repertorio` → `scai_repertorio`

#### Indici
- `idx_policy_default_sede_cod_ente` - Indice su cod_ente
- `idx_policy_default_sede_cod_sede` - Indice su cod_sede
- `idx_policy_default_sede_codice_repertorio` - Indice su codice_repertorio
- `idx_policy_default_sede_is_not_deleted` - Indice su soft delete

---

### Tabella: `scai_policy_rapporto_validity`

Validità temporale delle policy associate ai rapporti.

| Campo | Tipo | Descrizione |
|-------|------|-------------|
| id | BIGSERIAL | Chiave primaria |
| cod_ente | VARCHAR(15) | Codice ente (FK cross-module) |
| matricola | VARCHAR(15) | Matricola dipendente (FK cross-module) |
| slug_sdc | VARCHAR(16) | Slug sistema di campo (FK) |
| data_inizio | TIMESTAMP | Data inizio validità |
| data_fine | TIMESTAMP | Data fine validità |
| data_cessazione | TIMESTAMP | Data cessazione validità |
| attivo | SMALLINT | Flag attivo (1=attivo, 0=non attivo) |
| created_at | TIMESTAMP | Data creazione |
| updated_at | TIMESTAMP | Data ultimo aggiornamento |
| is_not_deleted | SMALLINT | Soft delete (1=attivo, NULL=cancellato) |

#### Vincoli
- **Unique Key**: `(cod_ente, matricola, slug_sdc, is_not_deleted)`
- **Foreign Key** (opzionale): `(cod_ente, matricola)` → `scai_rapporto`
- **Foreign Key** (opzionale): `slug_sdc` → `scai_sistemi_campo(slug)`

#### Indici
- `idx_policy_validity_cod_ente` - Indice su cod_ente
- `idx_policy_validity_matricola` - Indice su matricola
- `idx_policy_validity_slug_sdc` - Indice su slug_sdc
- `idx_policy_validity_attivo` - Indice su attivo per filtri
- `idx_policy_validity_is_not_deleted` - Indice su soft delete

---

## API Endpoints

### Policy Rapporto (Principale)

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | `/api/scai/policy` | Lista policy con paginazione e filtri |
| GET | `/api/scai/policy/{id}` | Dettaglio singola policy |
| POST | `/api/scai/policy` | Creazione nuova policy |
| PUT | `/api/scai/policy/{id}` | Aggiornamento policy esistente |
| DELETE | `/api/scai/policy/{id}` | Eliminazione logica policy |

#### Parametri Lista (GET /api/scai/policy)

- `draw` - Numero richiesta (per DataTable)
- `start` - Offset paginazione
- `length` - Numero record per pagina
- `orderby` - Campo ordinamento
- `ordertype` - Tipo ordinamento (asc/desc)
- `cod_ente` - Filtro per ente (opzionale)
- `matricola` - Filtro per matricola (opzionale)
- `codice_repertorio` - Filtro per repertorio (opzionale)
- `slug_sdc` - Filtro per sistema di campo (opzionale)
- `codice_policy` - Filtro per codice policy (opzionale)

#### Risposta Lista

```json
{
  "draw": 1,
  "recordsTotal": 500,
  "recordsFiltered": 500,
  "data": [
    {
      "id": 1,
      "cod_ente": "001",
      "matricola": "12345",
      "nome_rapporto": "Mario",
      "cognome_rapporto": "Rossi",
      "slug_sdc": "chronorium",
      "descrizione_sdc": "Sistema Chronorium",
      "codice_repertorio": "REP001",
      "descrizione_repertorio": "Repertorio A",
      "codice_policy": "POL001",
      "data_inizio_validita": "2024-01-01T00:00:00Z",
      "created_at": "2024-01-01T10:00:00Z",
      "created_by": "admin",
      "updated_at": "2024-01-01T10:00:00Z",
      "updated_by": "admin"
    }
  ]
}
```

---

### Policy Default Sede

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | `/api/scai/policy/default` | Lista policy default |
| GET | `/api/scai/policy/default/{id}` | Dettaglio singola policy default |
| POST | `/api/scai/policy/default` | Creazione nuova policy default |
| PUT | `/api/scai/policy/default/{id}` | Aggiornamento policy default |
| DELETE | `/api/scai/policy/default/{id}` | Eliminazione logica policy default |

---

### Policy Validity

| Metodo | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | `/api/scai/policy/validity` | Lista validità policy |
| GET | `/api/scai/policy/validity/{id}` | Dettaglio singola validità |
| POST | `/api/scai/policy/validity` | Creazione nuova validità |
| PUT | `/api/scai/policy/validity/{id}` | Aggiornamento validità |
| DELETE | `/api/scai/policy/validity/{id}` | Eliminazione logica validità |

---

## Frontend Components

### `policy-module`
Componente principale del modulo che gestisce il routing e la visualizzazione.

### `policy-component`
Componente per la gestione CRUD delle policy rapporto con tre viste:
- **Lista** - Tabella con 5 filtri (ente select, matricola, repertorio select, sdc select, codice policy) e paginazione
- **Dettaglio** - Visualizzazione dati singola policy con audit trail completo
- **Form** - Form per creazione/modifica con 4 select dinamiche cascading

### Funzionalità Frontend

- Ricerca con debouncing (400ms)
- Paginazione server-side
- **5 filtri**: ente (select), matricola (text), repertorio (select), sdc (select), codice policy (text)
- **4 select dinamiche** con dipendenze cascading:
  1. `/api/scai/ente` per enti
  2. `/api/scai/rapporti?cod_ente={ente}` per rapporti (dipende da ente selezionato)
  3. `/api/scai/repertorio` per repertori
  4. `/api/scai/sdc` per sistemi di campo
- Validazione form
- Gestione stati di caricamento
- Conferma eliminazione
- Badge colorati per ente (bg-secondary) e repertorio (bg-info)
- Audit trail con created_by/updated_by
- Campi chiave non modificabili in edit (cod_ente, matricola, codice_repertorio, codice_policy)
- Input datetime-local per data inizio validità

---

## Installazione

### 1. Prerequisiti

Assicurarsi che i moduli **ente**, **sede**, **rapporti**, **sdc** e **repertorio** siano installati:

```bash
# Verificare che le tabelle esistano
psql -U username -d database -c "SELECT COUNT(*) FROM scai_ente;"
psql -U username -d database -c "SELECT COUNT(*) FROM scai_sede;"
psql -U username -d database -c "SELECT COUNT(*) FROM scai_rapporto;"
psql -U username -d database -c "SELECT COUNT(*) FROM scai_sistemi_campo;"
psql -U username -d database -c "SELECT COUNT(*) FROM scai_repertorio;"
```

### 2. Database Migration

Eseguire la migration SQL:

```bash
psql -U username -d database -f modules/policy/migration/V20260304_000025__policy.sql
```

### 3. Frontend

Importare il modulo nell'applicazione:

```javascript
import { PolicyModuleComponent } from './modules/policy/gui/policy/index.js';
```

### 4. Routing

Configurare il routing per `/policy`:

```javascript
{
  path: '/policy',
  component: 'policy-module'
}
```

---

## Sviluppo Backend (Java - Futuro)

La cartella `java/` contiene stub per una futura implementazione backend Java/Spring Boot:

- `dto/PolicyRapportoDTO.java` - Data Transfer Object policy rapporto
- `dto/PolicyDefaultSedeDTO.java` - Data Transfer Object policy default
- `dto/PolicyRapportoValidityDTO.java` - Data Transfer Object validity
- `adapter/` - Adapter layer (da implementare)
- `dao/` - Data Access Object (da implementare)
- `handler/` - Request handlers (da implementare)

---

## Relazioni con Altri Moduli

### Dipendenza da Ente e Rapporti

Ogni policy è associata a un dipendente tramite composite key (cod_ente, matricola):

```sql
SELECT p.*, r.nome, r.cognome, e.descrizione_ente
FROM scai_policy_rapporto p
LEFT JOIN scai_rapporto r ON p.cod_ente = r.cod_ente AND p.matricola = r.matricola
LEFT JOIN scai_ente e ON p.cod_ente = e.cod_ente
WHERE p.is_not_deleted = 1;
```

### Dipendenza da Sistema di Campo

Ogni policy può essere associata a un sistema di campo:

```sql
SELECT p.*, s.descrizione_breve AS sistema_campo
FROM scai_policy_rapporto p
LEFT JOIN scai_sistemi_campo s ON p.slug_sdc = s.slug
WHERE p.is_not_deleted = 1;
```

### Dipendenza da Repertorio

Ogni policy è associata a un repertorio:

```sql
SELECT p.*, rep.descrizione AS repertorio_desc
FROM scai_policy_rapporto p
LEFT JOIN scai_repertorio rep ON p.codice_repertorio = rep.codice_repertorio
WHERE p.is_not_deleted = 1;
```

### Dipendenza da Sede (Policy Default)

Le policy default sono associate alle sedi:

```sql
SELECT pd.*, s.descrizione AS sede_desc
FROM scai_policy_default_sede pd
LEFT JOIN scai_sede s ON pd.cod_ente = s.cod_ente AND pd.cod_sede = s.cod_sede
WHERE pd.is_not_deleted = 1;
```

---

## Note Architetturali

- **Modularità**: Modulo autonomo con dipendenze da 5 moduli lookup/entity
- **Multiple Entities**: Gestisce 3 tabelle correlate (policy rapporto, policy default, validity)
- **Cross-Module Relations**: Usa composite natural key (cod_ente, matricola) per relazioni con rapporti
- **Soft Delete**: Implementato su tutte le 3 tabelle
- **Composite Unique Keys**: Garantiscono unicità delle associazioni policy-dipendente-repertorio
- **Foreign Key Opzionali**: Le FK sono commentate, gestite a livello applicativo
- **Audit Trail Completo**: Tracciamento created_by/updated_by con tipo VARCHAR per username
- **Cascading Selects**: Form con select dipendenti (rapporti caricati in base a ente selezionato)
- **Temporal Validity**: Tabella separata per gestire validità temporale delle policy
- **Default Policies**: Sistema di policy di default configurabili per sede

---

## Scenari d'Uso

### Scenario 1: Assegnazione Policy a Dipendente
1. Selezionare ente dal form
2. Selezionare dipendente dall'elenco filtrato per ente
3. Selezionare repertorio e sistema di campo
4. Inserire codice policy e data inizio validità
5. Salvare policy

### Scenario 2: Configurazione Policy Default per Sede
1. Accedere alla gestione policy default
2. Selezionare ente e sede
3. Configurare policy di default per repertorio
4. Salvare configurazione

### Scenario 3: Gestione Validità Temporale
1. Creare policy rapporto
2. Configurare validità temporale con date inizio/fine
3. Gestire cessazione validità quando necessario
4. Attivare/disattivare validità tramite flag

### Scenario 4: Query Policy Attive per Dipendente
```sql
SELECT p.*, pv.attivo, pv.data_inizio, pv.data_fine
FROM scai_policy_rapporto p
LEFT JOIN scai_policy_rapporto_validity pv
  ON p.cod_ente = pv.cod_ente
  AND p.matricola = pv.matricola
WHERE p.cod_ente = '001'
  AND p.matricola = '12345'
  AND p.is_not_deleted = 1
  AND (pv.attivo = 1 OR pv.attivo IS NULL);
```

---

## Licenza

Proprietary - SCAI Team
