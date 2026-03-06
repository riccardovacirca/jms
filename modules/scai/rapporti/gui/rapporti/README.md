# Module: rapporti

Modulo frontend per la gestione e visualizzazione dei **Rapporti di Lavoro** del sistema SCAI (Sicurezza Controllo Accessi Integrato).

## Descrizione

Questo modulo fornisce un'interfaccia completa per:
- Visualizzazione lista rapporti con ricerca avanzata e filtri
- Dettaglio completo di ogni rapporto
- Filtro per stato (attivi/cessati)
- Paginazione server-side DataTable
- Integrazione con backend Laravel SCAI

## Struttura del modulo

```
gui/rapporti/
├── index.js                # Entry point - esporta metodo mount()
├── module-component.js     # <rapporti-module> wrapper con navigazione tab
├── component.js            # <rapporti-layout> componente principale (lista + dettaglio)
├── rapporti.css            # Stili dedicati al modulo
└── README.md               # Documentazione
```

## Architettura

### Tecnologie utilizzate

- **Lit** - Web Components framework
- **Bootstrap 5** - UI framework
- **Vanilla JS** - Nessuna dipendenza aggiuntiva da framework

### Pattern architetturali

- **Web Components** personalizzati (`<rapporti-module>`, `<rapporti-layout>`)
- **State management** locale tramite Lit properties
- **Fetch API** per comunicazione con backend Laravel
- **Credentials: include** per autenticazione session-based

## Integrazione Backend

### Endpoint utilizzati

| Method | Endpoint | Descrizione |
|--------|----------|-------------|
| GET | `/api/scai/rapporto` | Lista paginata rapporti con filtri |
| GET | `/api/scai/rapporto/create` | Schema e opzioni per form/filtri |
| GET | `/api/scai/rapporto/{id}` | Dettaglio singolo rapporto |
| GET | `/api/scai/rapporto/foto/{id}` | Download foto rapporto |
| POST | `/api/scai/rapporto/default_policy` | Assegna policy default (bulk) |

### Formato richiesta DataTable

```
GET /api/scai/rapporto?draw=1&start=0&length=20&orderby=id&ordertype=desc&search[matricola]=123&search[stato]=attivi
```

### Formato risposta

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
        "cod_rapporto": "12345",
        "cognome": "Rossi",
        "nome": "Mario",
        "sesso": "M",
        "cod_fis": "RSSMRA80A01H501U",
        "data_assunzione": "2020-01-15",
        "data_cessazione": null,
        "cod_sede_primaria": "SEDE01",
        "descrizione_sede_primaria": "Palazzo Comunale",
        "cod_sede_secondaria": null,
        "descrizione_sede_secondaria": null,
        "email": "mario.rossi@example.com",
        "email_personale": "mario@gmail.com",
        "ufficio_piano": "3",
        "ufficio_numero_stanza": "301",
        "ufficio_telefono": "0101234567",
        "settore": "Amministrazione",
        "codice_struttura": "STR001",
        "descrizione_struttura": "Direzione Generale",
        "servizio_fuori_sede": 0,
        "status": "aggiornato",
        "p_badge": "ABC123456789",
        "codice_mansione": "MAN01",
        "descrizione_mansione": "Impiegato amministrativo",
        "codice_azienda": "AZ001",
        "descrizione_azienda": "Comune di...",
        "created_at": "2020-01-10T10:00:00.000000Z",
        "updated_at": "2025-03-01T15:30:00.000000Z"
      }
    ]
  }
}
```

## Entità Rapporto

### Campi principali

| Campo | Tipo | Descrizione | Validazione |
|-------|------|-------------|-------------|
| `id` | Integer | ID univoco | Primary key |
| `cod_ente` | String(4) | Codice ente | Required, max 4 |
| `matricola` | String(15) | Matricola dipendente | Required, max 15 |
| `cod_rapporto` | String(5) | Codice rapporto | Max 5 |
| `cognome` | String(30) | Cognome | Required, max 30 |
| `nome` | String(30) | Nome | Required, max 30 |
| `sesso` | String(1) | M/F | Required |
| `cod_fis` | String(16) | Codice fiscale | Required, max 16 |
| `data_assunzione` | Date | Data inizio contratto | Required |
| `data_cessazione` | Date | Data fine contratto | Nullable |
| `cod_sede_primaria` | String(15) | Codice sede principale | Max 15 |
| `cod_sede_secondaria` | String(15) | Codice sede secondaria | Nullable |
| `email` | String(128) | Email istituzionale | Max 128 |
| `email_personale` | String | Email personale | Nullable |
| `ufficio_piano` | String(4) | Piano ufficio | Max 4 |
| `ufficio_numero_stanza` | String(10) | Numero stanza | Max 10 |
| `ufficio_telefono` | String(10) | Telefono ufficio | Max 10 |
| `settore` | String(30) | Settore | Max 30 |
| `codice_struttura` | String(16) | Codice struttura | Max 16 |
| `descrizione_struttura` | String(255) | Descrizione struttura | Max 255 |
| `servizio_fuori_sede` | Boolean | Servizio fuori sede abilitato | 0/1 |
| `status` | Enum | Stato rapporto | 'nuovo', 'aggiornato', 'chiuso_nel_passato' |
| `p_badge` | String | Badge virtuale | - |
| `codice_mansione` | String(16) | Codice mansione | Max 16 |
| `descrizione_mansione` | String(512) | Descrizione mansione | Max 512 |
| `codice_azienda` | String | Codice azienda | - |
| `descrizione_azienda` | String | Descrizione azienda | - |

### Relazioni

- **Badge** → `scai_badge` (cod_ente, matricola)
- **Policy** → `scai_policy_rapporto` (cod_ente, matricola)
- **Veicoli** → `scai_veicolo_convenzione` (cod_ente, matricola)
- **Foto** → `scai_rapporto_foto` (id_rapporto)

## Funzionalità implementate

### 1. Lista Rapporti

- Tabella paginata con ordinamento
- Filtri di ricerca:
  - Codice Ente (select o input)
  - Matricola
  - Cognome
  - Nome
  - Codice Fiscale
  - Stato (tutti/attivi/cessati)
- Indicatore stato (badge verde/grigio)
- Pulsante "Dettaglio" per ogni riga

### 2. Dettaglio Rapporto

Vista completa con sezioni:
- **Dati Anagrafici**: matricola, codice rapporto, cognome, nome, sesso, codice fiscale, email
- **Dati Contrattuali**: codice ente, date assunzione/cessazione, azienda, mansione
- **Sedi e Ufficio**: sede primaria/secondaria, piano, stanza, telefono, settore, struttura
- **Badge Virtuale**: P-Badge (se presente)
- **Fotografia**: visualizzazione o download

### 3. Ricerca e Filtri

- Debouncing (400ms) su input di ricerca
- Reset paginazione al cambio filtri
- Combinazione multipla di filtri
- Persistenza stato durante navigazione

## Installazione

### 1. Copia file GUI

```bash
cp -r gui/rapporti/ /path/to/sportello-scai_fe/gui/rapporti/
```

### 2. Registra route in config

Aggiungi la configurazione del modulo in `vite/src/config.js` o nel file di routing dell'applicazione:

```javascript
rapporti: {
  path: '/rapporti',
  authorization: { redirectTo: '/auth' }
}
```

### 3. Importa il modulo

Nel file principale dell'applicazione (es. `main.js` o `App.js`):

```javascript
import rapportiModule from './gui/rapporti/index.js';

// ... nel router o nella logica di routing:
case '/rapporti':
  rapportiModule.mount(containerElement);
  break;
```

### 4. Build frontend

```bash
npm run build
# oppure
yarn build
```

## Utilizzo

### Mounting del modulo

```javascript
import rapportiModule from './gui/rapporti/index.js';

const container = document.getElementById('app');
rapportiModule.mount(container);
```

Il modulo verrà renderizzato come:

```html
<rapporti-module>
  <rapporti-layout></rapporti-layout>
</rapporti-module>
```

### Eventi personalizzati

Il componente non emette eventi custom al momento. Tutta la navigazione è gestita internamente tramite state management.

### Personalizzazione CSS

È possibile sovrascrivere gli stili in `rapporti.css` o aggiungere classi custom:

```css
/* Override esempio */
.rapporti-layout .table-hover tbody tr:hover {
  background-color: rgba(255, 193, 7, 0.1);
}
```

## Estensioni future

### Tab aggiuntive suggerite

```javascript
// In module-component.js, aggiungere tab:
<li class="nav-item">
  <button class="nav-link ${this._tab === 'badge' ? 'active' : ''}"
          @click=${() => { this._tab = 'badge'; }}>Badge</button>
</li>
<li class="nav-item">
  <button class="nav-link ${this._tab === 'policy' ? 'active' : ''}"
          @click=${() => { this._tab = 'policy'; }}>Policy</button>
</li>
<li class="nav-item">
  <button class="nav-link ${this._tab === 'veicoli' ? 'active' : ''}"
          @click=${() => { this._tab = 'veicoli'; }}>Veicoli</button>
</li>
```

### Store globale (Nanostores)

Per state management condiviso tra moduli:

```javascript
// stores/rapportiStore.js
import { atom, map } from 'nanostores';

export const rapportiAtom = atom([]);
export const selectedRapportoAtom = atom(null);
export const loadingAtom = atom(false);
```

### Export dati

Aggiungere funzionalità di export CSV/Excel:

```javascript
async _exportCSV() {
  const params = new URLSearchParams({ ...this._search, export: 'csv' });
  window.location.href = `/api/scai/rapporto/export?${params}`;
}
```

## Note tecniche

### Autenticazione

- Tutte le chiamate API usano `credentials: 'include'` per inviare cookie di sessione
- Se l'utente non è autenticato, il backend Laravel restituirà 401/403
- Gestire il redirect al login a livello di applicazione

### Paginazione

- Paginazione server-side tramite parametri `start` e `length`
- Il backend restituisce `recordsTotal` e `recordsFiltered` per calcolare pagine
- Navigazione ottimizzata: max 10 pagine visibili alla volta

### Performance

- Debouncing su input di ricerca (400ms)
- Caricamento lazy delle opzioni form (`/create` endpoint)
- Shadow DOM disabilitato (`createRenderRoot() { return this; }`) per integrazione Bootstrap

### Browser supportati

- Chrome/Edge 90+
- Firefox 88+
- Safari 14+
- Opera 76+

## Licenza

Modulo proprietario - Sportello SCAI

## Changelog

### v1.0.0 (2026-03-03)
- Implementazione iniziale
- Lista rapporti con filtri avanzati
- Vista dettaglio completo
- Integrazione backend Laravel SCAI
