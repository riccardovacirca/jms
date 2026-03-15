# Module: rapporti

Gestione rapporti di lavoro del sistema SCAI (Sicurezza Controllo Accessi Integrato) con visualizzazione dettagli, ricerca avanzata, paginazione e integrazione con backend Laravel.

> **NOTA IMPORTANTE**: Questo modulo si integra con il backend Laravel esistente.
> La parte Java (handler, dao, adapter) è fornita come stub per future implementazioni di backend Java/Spring Boot.

## Contents

- `java/rapporti/` — Java package stub (handler, dao, dto, adapter) - **NON IMPLEMENTATO**
- `gui/rapporti/` — Frontend module (SPA, LitElement) - **IMPLEMENTATO**
  - `index.js` — Entry point
  - `module-component.js` — `<rapporti-module>` wrapper con tab nav
  - `component.js` — `<rapporti-layout>` web component (lista + dettaglio)
  - `rapporti.css` — Stili dedicati
- `migration/` — SQL schema documentazione (PostgreSQL)
- `config/` — Configurazione modulo

## Note sulle dipendenze

Il modulo rapporti è il modulo centrale del sistema SCAI. Ha relazioni con:
- **Badge** (scai_badge) - un rapporto può avere più badge
- **Policy** (scai_policy_rapporto) - policy di accesso associate al rapporto
- **Veicoli** (scai_veicolo_convenzione) - convenzioni veicoli associate al rapporto
- **Foto** (scai_rapporto_foto) - fotografia del dipendente

## Installation

### 1. Frontend sources

Copy `gui/rapporti/` into your frontend modules directory:

```sh
cp -r modules/rapporti/gui/rapporti/  /path/to/frontend/gui/rapporti/
```

### 2. Register route in application

Add the module configuration to your routing system:

```javascript
// esempio: gui/src/config.js
rapporti: {
  path: '/rapporti',
  authorization: { redirectTo: '/auth' }
}
```

### 3. Import in main application

```javascript
import rapportiModule from './gui/rapporti/index.js';

// Nel router:
if (route === '/rapporti') {
  rapportiModule.mount(containerElement);
}
```

### 4. Build frontend

```sh
npm run build
# oppure per development
npm run dev
```

**Done!** The module is accessible via `/#/rapporti`.

## API Endpoints (Backend Laravel)

### Rapporti

| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/scai/rapporto` | Lista paginata rapporti con filtri DataTable |
| GET | `/api/scai/rapporto/create` | Schema e opzioni per form/filtri |
| GET | `/api/scai/rapporto/{id}` | Dettaglio singolo rapporto |
| GET | `/api/scai/rapporto/foto/{rapporto_id}` | Download foto rapporto |
| POST | `/api/scai/rapporto/default_policy` | Assegna policy default (bulk) |
| GET | `/api/scai/rapporto/default_policy/edit` | Form validation policy default |

### Parametri Query DataTable

```
GET /api/scai/rapporto?draw=1&start=0&length=20&orderby=id&ordertype=desc
  &search[matricola]=123
  &search[cognome]=Rossi
  &search[nome]=Mario
  &search[cod_fis]=RSSMRA80A01H501U
  &search[cod_ente]=CRG
  &search[stato]=attivi
```

### Filtro stato

- `tutti` - tutti i rapporti
- `attivi` - rapporti con data_cessazione NULL o futura
- `disattivi` - rapporti con data_cessazione nel passato

### Response envelope

```json
{
  "data": {
    "draw": 1,
    "recordsTotal": 1500,
    "recordsFiltered": 45,
    "data": [
      {
        "id": 123,
        "cod_ente": "CRG",
        "matricola": "00123",
        "cognome": "Rossi",
        "nome": "Mario",
        "cod_fis": "RSSMRA80A01H501U",
        "data_assunzione": "2020-01-15T00:00:00.000000Z",
        "data_cessazione": null,
        "email": "mario.rossi@example.com",
        ...
      }
    ]
  }
}
```

## RapportoDTO (campi)

| Campo | Tipo | Max | Required | Descrizione |
|-------|------|-----|----------|-------------|
| `id` | Integer | - | PK | ID univoco |
| `cod_ente` | String | 15 | ✓ | Codice ente |
| `matricola` | String | 15 | ✓ | Matricola dipendente |
| `cod_rapporto` | String | 15 | - | Codice rapporto |
| `cognome` | String | 128 | ✓ | Cognome |
| `nome` | String | 128 | ✓ | Nome |
| `sesso` | String | 4 | ✓ | Sesso (M/F) |
| `cod_fis` | String | 16 | ✓ | Codice fiscale |
| `data_assunzione` | Timestamp | - | - | Data assunzione |
| `data_cessazione` | Timestamp | - | - | Data cessazione |
| `cod_ente_hr` | String | 16 | - | Codice ente HR numerico |
| `codice_azienda` | String | 15 | - | Codice azienda |
| `descrizione_azienda` | String | 255 | - | Descrizione azienda |
| `codice_mansione` | String | 16 | - | Codice mansione |
| `descrizione_mansione` | String | 512 | - | Descrizione mansione |
| `cod_sede_primaria` | String | 15 | - | Codice sede primaria |
| `descrizione_sede_primaria` | String | 512 | - | Descrizione sede primaria |
| `data_inizio_sede_primaria` | Timestamp | - | - | Data inizio sede primaria |
| `cod_sede_secondaria` | String | 15 | - | Codice sede secondaria |
| `descrizione_sede_secondaria` | String | 512 | - | Descrizione sede secondaria |
| `data_inizio_sede_secondaria` | Timestamp | - | - | Data inizio sede secondaria |
| `codice_struttura` | String | 16 | - | Codice struttura |
| `descrizione_struttura` | String | 255 | - | Descrizione struttura |
| `settore` | String | 30 | - | Settore |
| `ufficio_piano` | String | 4 | - | Piano ufficio |
| `ufficio_numero_stanza` | String | 10 | - | Numero stanza |
| `ufficio_telefono` | String | 16 | - | Telefono ufficio |
| `email` | String | 128 | - | Email istituzionale |
| `email_personale` | String | 128 | - | Email personale |
| `p_badge` | String | 16 | - | Badge virtuale HR |
| `url_image` | String | 255 | - | URL foto rapporto |
| `servizio_fuori_sede` | SmallInt | - | - | Servizio fuori sede (0/1) |
| `status` | String | 50 | - | nuovo, aggiornato, chiuso_nel_passato |
| `created_at` | Timestamp | - | ✓ | Data creazione |
| `updated_at` | Timestamp | - | ✓ | Data aggiornamento |
| `is_not_deleted` | SmallInt | - | - | Soft delete: 1=attivo, NULL=eliminato |

## Module Structure

```
modules/rapporti/
├── config/
│   └── application.properties        # Configurazione modulo
├── java/rapporti/                    # Backend Java (STUB - non implementato)
│   ├── dto/
│   │   ├── RapportoDTO.java
│   │   ├── RapportoBadgeDTO.java
│   │   └── RapportoPolicyDTO.java
│   ├── adapter/                      # .gitkeep (da implementare)
│   ├── dao/                          # .gitkeep (da implementare)
│   └── handler/                      # .gitkeep (da implementare)
├── migration/
│   └── V20260304_000001__rapporti.sql  # Schema PostgreSQL documentazione
├── gui/rapporti/                     # Frontend (IMPLEMENTATO)
│   ├── index.js                      # Entry point
│   ├── module-component.js           # <rapporti-module> wrapper
│   ├── component.js                  # <rapporti-layout> lista + dettaglio
│   ├── rapporti.css                  # Stili dedicati
│   └── README.md                     # Documentazione GUI (vecchia)
└── README.md                         # Questo file
```

## Funzionalità Frontend

### 1. Lista Rapporti

- ✅ Tabella paginata server-side (DataTable pattern)
- ✅ Filtri avanzati:
  - Codice Ente (select dinamica se disponibile)
  - Matricola
  - Cognome
  - Nome
  - Codice Fiscale
  - Stato (tutti/attivi/cessati)
- ✅ Debouncing ricerca (400ms)
- ✅ Badge stato visivo (verde=attivo, grigio=cessato)
- ✅ Ordinamento per campo
- ✅ Paginazione con navigazione intelligente

### 2. Vista Dettaglio Rapporto

Visualizzazione completa READ-ONLY organizzata in card:

- **Dati Anagrafici**: matricola, codice rapporto, cognome, nome, sesso, CF, email
- **Dati Contrattuali**: codice ente, date assunzione/cessazione, azienda, mansione
- **Sedi e Ufficio**: sede primaria/secondaria, piano, stanza, telefono, settore, struttura
- **Badge Virtuale**: P-Badge (se presente)
- **Fotografia**: visualizzazione inline o pulsante download

### 3. Tecnologie Utilizzate

- **Lit** - Web Components framework
- **Bootstrap 5** - UI framework (importato in index.js)
- **Vanilla JavaScript** - Nessuna dipendenza aggiuntiva
- **Fetch API** - Chiamate HTTP con `credentials: 'include'`
- **Shadow DOM disabilitato** - Per compatibilità Bootstrap

## Database Schema

Il modulo si basa sulle seguenti tabelle PostgreSQL:

### scai_rapporto (principale)

Gestita da Laravel Migrations, definizione completa in `migration/V20260304_000001__rapporti.sql`.

**Indici:**
- Primary Key: `id`
- Unique: `(matricola, cod_ente, is_not_deleted)`
- Index: `cod_ente`, `matricola`, `cod_fis`, `cognome`, `nome`, `data_assunzione`, `data_cessazione`, `status`

### scai_rapporto_foto

Fotografie dipendenti (campo `foto` BYTEA).

### scai_badge

Badge fisici associati ai rapporti (FK: cod_ente, matricola).

### scai_policy_rapporto

Policy di accesso associate ai rapporti (FK: cod_ente, matricola).

### scai_veicolo_convenzione

Convenzioni veicoli associate ai rapporti (FK: cod_ente, matricola, targa).

## Autenticazione e Sicurezza

- Tutte le chiamate usano `credentials: 'include'` per inviare cookie di sessione
- Backend Laravel gestisce autenticazione via middleware `ariaAuth`
- Redirect al login gestito a livello applicativo in caso di 401/403
- CSRF token via `/sanctum/csrf-cookie` (se necessario)

## Estensioni Future

### Backend Java/Spring Boot

I DTO Java sono già definiti in `java/rapporti/dto/`. Per implementare il backend:

1. Implementare `RapportoDAO.java` per accesso DB (JPA/JDBC)
2. Implementare `RapportoAdapter.java` per parsing body → DTO
3. Implementare handler HTTP per gli endpoint REST
4. Configurare datasource PostgreSQL
5. Eseguire migration SQL da `migration/V20260304_000001__rapporti.sql`

### Tab aggiuntive nel modulo

Estendere `module-component.js` con tab:
- Badge associati al rapporto
- Policy associate al rapporto
- Veicoli/Convenzioni associate al rapporto
- Storico modifiche

```javascript
// In module-component.js
<li class="nav-item">
  <button class="nav-link ${this._tab === 'badge' ? 'active' : ''}"
          @click=${() => { this._tab = 'badge'; }}>Badge</button>
</li>
```

Creare `badge-component.js` e importarlo in `index.js`.

### Export dati

Aggiungere funzionalità export CSV/Excel:

```javascript
_exportCSV() {
  const params = new URLSearchParams(this._search);
  params.append('export', 'csv');
  window.location.href = `/api/scai/rapporto/export?${params}`;
}
```

Backend Laravel dovrà implementare endpoint `/api/scai/rapporto/export`.

### State Management globale

Utilizzare **Nanostores** per condivisione stato tra moduli:

```javascript
// stores/rapportiStore.js
import { atom, map } from 'nanostores';

export const rapportiAtom = atom([]);
export const selectedRapportoAtom = atom(null);
export const loadingAtom = atom(false);
export const errorAtom = atom(null);
```

## Browser supportati

- Chrome/Edge 90+
- Firefox 88+
- Safari 14+
- Opera 76+

## Dependencies

Frontend:
- `lit` - Web Components framework
- `bootstrap` - UI framework (CSS only)

Backend (Laravel - esistente):
- PHP 8.1+
- PostgreSQL 12+
- Laravel 9+

## Licenza

Modulo proprietario - Sportello SCAI

## Changelog

### v1.0.0 (2026-03-04)
- ✅ Implementazione GUI completa (lista + dettaglio)
- ✅ Filtri avanzati con debouncing
- ✅ Paginazione server-side DataTable
- ✅ Integrazione backend Laravel SCAI
- ✅ Vista dettaglio organizzata in card
- ✅ Download foto rapporto
- ✅ Responsive design
- ⏳ Backend Java stub (da implementare)
- ✅ Migration SQL documentazione
- ✅ DTO Java definiti

## Contact

Per supporto e segnalazioni: [SCAI Support Team]
