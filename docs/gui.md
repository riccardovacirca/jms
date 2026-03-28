# GUI

Frontend SPA in vanilla JavaScript con Vite 6. Le sorgenti stanno in `gui/src/`; il build output va in `src/main/resources/static/` e viene incluso nel JAR.

---

## Struttura base

```
gui/src/
├── index.html      → shell HTML con tre container
├── router.js       → Router SPA (entry point JS)
├── store.js        → stato globale reattivo (nanostores)
├── init.js         → interceptor fetch globale
├── config.js       → dichiarazione di tutti i moduli
└── module/         → cartella moduli
```

### Shell HTML (`index.html`)

Definisce tre container DOM vuoti e carica il router:

```html
<div id="app-layout">
  <div id="header"></div>
  <div id="main"></div>
  <div id="footer"></div>
</div>
<script type="module" src="/router.js"></script>
```

I moduli scrivono dentro uno di questi container. Il router è l'unico script caricato direttamente.

### Store (`store.js`)

Due atom nanostores condivisi tra router e moduli:

```js
authorized  // atom(false) — true quando l'utente ha una sessione valida
user        // atom(null)  — dati dell'utente corrente
```

Il modulo di autenticazione li imposta al login/logout. Il router li legge per controllare l'accesso alle rotte protette.

### Interceptor fetch (`init.js`)

Sostituisce `window.fetch` con una versione che intercetta ogni risposta API. Se la risposta contiene `err: true` con un messaggio di autenticazione (`"Non autenticato"`, `"Token non valido o scaduto"`), tenta automaticamente il refresh del token (`/api/user/auth/refresh`). Se il refresh ha successo, la richiesta originale viene ripetuta. Se fallisce, `authorized` viene impostato a `false`, causando il redirect del router alla pagina di login. Le richieste verso `/auth/refresh` e `/auth/login` non innescano il retry per evitare loop infiniti.

---

## Router

Il router è una classe `Router` istanziata una sola volta all'avvio. Gestisce sia i moduli persistenti (header, footer) sia quelli dinamici (pagine).

### Sequenza di avvio

```
new Router()
  │
  ├─ appInit()                        — installa l'interceptor fetch
  ├─ Promise.all([init di tutti i moduli che dichiarano init])
  ├─ _mountPersistentModules()        — monta header/footer in ordine di priority
  ├─ addEventListener('hashchange')   — ascolta navigazioni successive
  └─ authorized.subscribe(route())    — routing iniziale (nanostores chiama subito il subscriber)
```

Le procedure `init` vengono eseguite prima di qualsiasi montaggio per garantire che lo stato condiviso (es. sessione utente) sia già pronto quando i moduli vengono montati.

### Routing hash-based

Ogni navigazione legge `window.location.hash`. Il formato atteso è `#/route`.

- **Hash assente** → carica `DEFAULT_MODULE` (chiave in `MODULE_CONFIG`, non via route lookup)
- **`#/`** → redirect a `/` (non trattato come rotta)
- **Hash sconosciuto** → mostra "404 — Pagina non trovata"

La rotta viene cercata per corrispondenza esatta o come prefisso (`hash === route || hash.startsWith(route + '/')`), consentendo sub-path.

### Controllo accessi

```js
authorization: null               // sempre accessibile
authorization: { redirectTo: '/login' }  // protetta
```

Se la rotta è protetta e `authorized` è `false`, il router reindirizza a `redirectTo` (se quella rotta esiste in `MODULE_CONFIG`) oppure mostra "403 — Accesso negato".

### Navigazioni rapide

Ogni chiamata a `loadModule` incrementa un contatore `_navId`. Alla risoluzione dell'`import()`, se il contatore è cambiato (l'utente ha navigato altrove nel frattempo), il risultato viene scartato silenziosamente.

---

## Moduli

Ogni modulo è una cartella sotto `gui/src/module/` con un file `index.js` che esporta un oggetto con il metodo `mount(container)`:

```js
const MyModule = {
  mount(container) {
    container.innerHTML = '<my-component></my-component>';
  },
  unmount() {       // opzionale
    // cleanup
  }
};

export default MyModule;
```

`mount` riceve l'elemento DOM del container e vi scrive il contenuto. `unmount` è opzionale e viene chiamato dal router prima di montare un nuovo modulo nello stesso container.

### Esempio — modulo `status`

```js
class StatusView extends LitElement {
  createRenderRoot() { return this; }   // disabilita Shadow DOM → Bootstrap funziona

  async connectedCallback() {
    super.connectedCallback();
    const res  = await fetch('/api/status');
    const data = await res.json();
    this._message = data.err ? data.log : data.out;
  }

  render() {
    return html`<p>${this._message}</p>`;
  }
}

customElements.define('app-status', StatusView);

export default { mount(container) { container.innerHTML = '<app-status></app-status>'; } };
```

### Tipi di modulo

| Tipo | `persistent` | `path` | Comportamento |
|------|-------------|--------|---------------|
| Pagina dinamica | `false` | stringa | montato/smontato a ogni navigazione |
| Persistente (header/footer) | `true` | stringa | montato una volta all'avvio, mai smontato |
| Backend-only | `false` | `null` | nessun montaggio; la rotta esiste ma è gestita dal backend |

`persistent: true` con `path: null` è un errore di configurazione — il router lo rileva esplicitamente e lancia un'eccezione.

### Moduli con `init`

Un modulo può dichiarare una procedura di inizializzazione eseguita all'avvio dell'app, prima del primo routing:

```js
init: true                  // auto-import di ./module/<path>/init.js → chiama default()
init: async () => { ... }   // funzione inline
init: null                  // nessuna init
```

Tutte le init vengono eseguite in parallelo con `Promise.all`.

---

## Configurazione moduli (`config.js`)

Ogni modulo è dichiarato in `MODULE_CONFIG` con sette attributi obbligatori:

```js
export const MODULE_CONFIG = {
  home: {
    route:         '/home',          // hash di navigazione, null se non navigabile
    path:          'home',           // cartella sotto gui/src/module/
    container:     'main',           // ID del container DOM
    authorization: null,             // null=pubblico, {redirectTo}=protetto
    persistent:    false,            // true=sempre montato
    priority:      999,              // ordine di montaggio (solo per persistent)
    init:          null              // procedura di init o null
  }
};

export const DEFAULT_MODULE = 'home';  // modulo caricato senza hash
```

`path` supporta namespace: `'cti/vonage'` risolve in `gui/src/module/cti/vonage/index.js`.

---

## Integrazione moduli installati

Quando si installa un modulo con `cmd module import`, lo script aggiorna automaticamente `config.js`:

1. Inserisce l'entry del modulo dopo il marker `// [MODULE_ENTRIES]`
2. Il `path` viene impostato correttamente (con namespace se presente)
3. Alla prossima navigazione verso la rotta del modulo, il router carica `gui/src/module/<path>/index.js` via `import()` dinamico

La rimozione con `cmd module remove` fa l'operazione inversa: elimina l'entry da `MODULE_CONFIG` tramite regex Python3, preservando il resto del file.

---

## Build e sviluppo

**Sviluppo:**
```bash
cmd gui run   # Vite dev server su :5173, proxy /api → :8080, hot reload
```

**Build:**
```bash
cmd gui build  # output in src/main/resources/static/ (incluso nel JAR)
```

Vite usa `gui/src/` come root e `src/main/resources/static/` come `outDir`. In sviluppo il proxy di Vite inoltra le chiamate `/api/*` al backend sulla porta 8080, permettendo di lavorare con frontend e backend separati senza CORS.
