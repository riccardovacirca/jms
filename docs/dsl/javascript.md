# JavaScript Coding Style DSL

```
version: 1.0
scope:   vite/src/**/*.js
```

---

## Formato delle regole

Ogni regola ha:
- `id` — identificatore univoco in forma `categoria.nome`
- `applies-to` — dove si applica la regola
- `ok` — esempio conforme
- `ko` — esempio non conforme

La conformità si verifica sui file `.js` nel `scope` dichiarato.

---

## FORMATTING

```
RULE fmt.semicolon
  applies-to: ogni istruzione
  note: ogni istruzione termina con punto e virgola

  ok: |
    const x = 1;
    doSomething();
    export { foo, bar };

  ko: |
    const x = 1
    doSomething()
    export { foo, bar }
```

```
RULE fmt.one-statement-per-line
  applies-to: ogni istruzione
  note: una sola istruzione per riga; più istruzioni sulla stessa riga sono vietate

  ok: |
    this._loading = true;
    this._error   = null;
    this._render();

  ko: |
    this._loading = true; this._error = null; this._render()
```

```
RULE fmt.long-statement
  applies-to: istruzioni che superano la leggibilità su riga singola
  note: le istruzioni lunghe vengono spezzate estraendo variabili intermedie
        o espandendo oggetti/array su più righe con una proprietà per riga;
        si preferisce l'estrazione di variabili intermedie rispetto al wrapping della stessa espressione

  ok: |
    // variabili intermedie per template literal complessa
    const ts    = new Date().toISOString();
    const extra = data ? ' ' + JSON.stringify(data) : '';
    return `[${ts}] [${level}] [${module}] ${message}${extra}`;

    // oggetto espanso su più righe
    const CONFIG = {
      consoleEnabled:  true,
      backendEnabled:  false,
      debugToBackend:  false
    };

    // proprietà di oggetto letterale su righe separate
    body: JSON.stringify({
      module,
      level,
      message,
      data,
      timestamp: new Date().toISOString()
    })

  ko: |
    return `[${new Date().toISOString()}] [${level}] [${module}] ${message}${data ? ' ' + JSON.stringify(data) : ''}`;

    const CONFIG = { consoleEnabled: true, backendEnabled: false, debugToBackend: false };

    body: JSON.stringify({ module, level, message, data, timestamp: new Date().toISOString() })
```

```
RULE fmt.no-inline-function-body
  applies-to: function declaration, shorthand method, getter/setter in object literal
  note: il corpo sta sempre su righe separate rispetto alla firma,
        indipendentemente dal numero di istruzioni contenute

  ok: |
    function debug(m, msg, d) {
      log(m, 'DEBUG', msg, d);
    }

    return {
      get state() {
        return state;
      },
      subscribe(fn) {
        listeners.push(fn);
      }
    };

  ko: |
    function debug(m, msg, d) { log(m, 'DEBUG', msg, d); }

    return {
      get state() { return state; },
      subscribe(fn) { listeners.push(fn); }
    };
```

---

## EXPORT

```
RULE export.at-end
  applies-to: tutti i moduli con export
  note: gli export named sono raggruppati in un unico blocco export { } alla fine del file;
        non si usa export inline sulle dichiarazioni

  ok: |
    function foo() { ... }
    function bar() { ... }

    export { foo, bar };

  ko: |
    export function foo() { ... }
    export function bar() { ... }
```

```
RULE export.separate-from-declaration
  applies-to: tutti i moduli con export
  note: le dichiarazioni di funzioni e costanti sono separate dall'export;
        una funzione non viene dichiarata ed esportata nella stessa riga

  ok: |
    function mount(tag, component) {
      customElements.define(tag, component);
    }

    export { mount };

  ko: |
    export function mount(tag, component) {
      customElements.define(tag, component);
    }
```

```
RULE export.side-effect-only
  applies-to: moduli che registrano custom elements
  note: i file che si limitano a registrare un custom element
        non esportano nulla; l'effetto collaterale è il loro unico scopo;
        la registrazione avviene sempre tramite customElements.define direttamente

  ok: |
    class HeaderLayout extends LitElement { ... }
    customElements.define('header-layout', HeaderLayout);
    // nessun export

  ko: |
    export class HeaderLayout extends LitElement { ... }
```

---

## VARIABILI

```
RULE var.no-global-singles
  applies-to: variabili di stato a livello di modulo
  note: variabili singole correlate non vengono dichiarate separatamente nel contesto
        globale del modulo; vengono raggruppate in un oggetto con nome semantico

  ok: |
    const refreshState = { active: false, promise: null };

  ko: |
    let refreshing = false;
    let refreshPromise = null;
```

---

## COMMENTI

```
RULE comment.jsdoc-for-functions
  applies-to: tutte le funzioni esportate o pubblicamente rilevanti
  note: i commenti che descrivono una funzione usano il formato JSDoc con /** */;
        si includono @param per ogni parametro e @returns se la funzione restituisce un valore;
        i commenti inline // restano validi solo per note di contesto non legate a una funzione

  ok: |
    /**
     * Verifica la sessione sul server e aggiorna auth di conseguenza.
     * @param {string} url - URL dell'endpoint
     * @param {RequestInit} options - Opzioni fetch
     * @returns {Promise<Response>}
     */
    async function fetchWithRefresh(url, options = {}) {
      ...
    }

  ko: |
    // Verifica la sessione sul server e aggiorna auth di conseguenza.
    async function fetchWithRefresh(url, options = {}) {
      ...
    }
```

---

## WEB COMPONENTS

```
RULE wc.class-per-component
  applies-to: custom elements
  note: ogni custom element è una classe che estende LitElement;
        lo stato reattivo è dichiarato tramite static properties con { state: true }
        e inizializzato nel constructor;
        Shadow DOM è disabilitato con createRenderRoot() per consentire a Bootstrap
        di applicare gli stili normalmente;
        la logica di rendering è in render(), che restituisce un template html`...`;
        la registrazione avviene con customElements.define dopo la definizione della classe

  ok: |
    class AuthLayout extends LitElement {
      static properties = {
        _loading: { state: true },
        _error:   { state: true }
      };

      createRenderRoot() { return this; }

      constructor() {
        super();
        this._loading = false;
        this._error   = null;
      }

      render() {
        return html`...`;
      }
    }

    customElements.define('auth-layout', AuthLayout);

  ko: |
    class AuthLayout extends HTMLElement {
      constructor() {
        super();
        this._loading = false;
        this._error   = null;
      }

      _render() {
        this.innerHTML = `...`;
      }
    }

    customElements.define('auth-layout', AuthLayout);
```

```
RULE wc.reactive-render
  applies-to: componenti che dipendono da uno store
  note: il componente si sottoscrive allo store in connectedCallback e si disiscrive
        in disconnectedCallback, salvando la funzione di unsub restituita da subscribe();
        ogni aggiornamento dello store assegna il nuovo valore alla proprietà reattiva
        corrispondente (this._x = v); Lit rileva la modifica e chiama render()
        automaticamente — non si chiama render() esplicitamente;
        il subscriber non passa lo stato come argomento a un metodo handler separato

  ok: |
    connectedCallback() {
      super.connectedCallback();
      this._unsubAuth = auth.subscribe(v => { this._authorized = v; });
    }

    disconnectedCallback() {
      super.disconnectedCallback();
      this._unsubAuth();
    }

  ko: |
    // render() non va chiamato esplicitamente: Lit lo gestisce automaticamente
    connectedCallback() {
      super.connectedCallback();
      auth.subscribe(v => { this._authorized = v; this.render(); });
    }

    // lo stato non va passato come parametro a un handler: bypassa il pattern reattivo
    connectedCallback() {
      super.connectedCallback();
      auth.subscribe(state => this._updateView(state));
    }
```
