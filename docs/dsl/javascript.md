# JavaScript Coding Style DSL

```
version: 1.3
scope: gui/src/**/*.js
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
    this._error = null;
    this._render();

  ko: |
    this._loading = true; this._error = null; this._render();
```

```
RULE fmt.no-alignment-padding
  applies-to: assegnazioni, proprietà di oggetto, argomenti
  note: non si aggiungono spazi extra per allineare verticalmente il carattere = o :
        su righe adiacenti; ogni assegnazione usa un solo spazio attorno all'operatore;
        l'allineamento verticale è sempre vietato, anche in blocchi omogenei
        (assegnazioni consecutive, config, destructuring)

  ok: |
    const a = 1;
    const longName = 2;
    const x = 3;

    const obj = {
      foo: 1,
      longerKey: 2
    };

    const ts = new Date().toISOString();
    const extra = data ? ' ' + JSON.stringify(data) : '';

    const CONFIG = {
      consoleEnabled: true,
      backendEnabled: false,
      debugToBackend: false
    };

  ko: |
    const a        = 1;
    const longName = 2;
    const x        = 3;

    const obj = {
      foo:       1,
      longerKey: 2
    };

    const ts    = new Date().toISOString();
    const extra = data ? ' ' + JSON.stringify(data) : '';

    const CONFIG = {
      consoleEnabled:  true,
      backendEnabled:  false,
      debugToBackend:  false
    };
```

```
RULE fmt.long-statement
  applies-to: istruzioni che superano indicativamente 100 caratteri o che contengono
              più di un'operazione composta nella stessa espressione
  note: le istruzioni lunghe vengono spezzate estraendo variabili intermedie
        o espandendo oggetti/array su più righe con una proprietà per riga;
        si preferisce l'estrazione di variabili intermedie rispetto al wrapping della stessa espressione

  ok: |
    // variabili intermedie per template literal complessa
    const ts = new Date().toISOString();
    const extra = data ? ' ' + JSON.stringify(data) : '';
    return `[${ts}] [${level}] [${module}] ${message}${extra}`;

    // oggetto espanso su più righe
    const CONFIG = {
      consoleEnabled: true,
      backendEnabled: false,
      debugToBackend: false
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
  applies-to: function declaration, class method, shorthand method, getter/setter in object literal
  note: il corpo sta sempre su righe separate rispetto alla firma,
        indipendentemente dal numero di istruzioni contenute;
        nessuna eccezione, incluso createRenderRoot() nei LitElement;
        le arrow function usate come callback inline (es. v => { this._x = v; }) sono esentate

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

    createRenderRoot() {
      return this;
    }

  ko: |
    function debug(m, msg, d) { log(m, 'DEBUG', msg, d); }

    return {
      get state() { return state; },
      subscribe(fn) { listeners.push(fn); }
    };

    createRenderRoot() { return this; }
```

---

## EXPORT

```
RULE export.at-end
  applies-to: tutti i moduli con export
  note: dichiarazione ed export sono sempre su statement separati e l'export sta alla fine del file;
        gli export named usano un unico blocco export { } come ultima istruzione;
        i default export usano export default <nome> su una riga separata, dopo la dichiarazione;
        non si usa mai export inline su una dichiarazione (né export function, né export default function,
        né export const)

  ok: |
    // named exports
    function foo() { ... }
    function bar() { ... }

    export { foo, bar };

    // default export
    function init() { ... }

    export default init;

  ko: |
    export function foo() { ... }
    export function bar() { ... }

    export default function init() { ... }
```

```
RULE export.side-effect-only
  applies-to: moduli che registrano custom elements
  note: i file che si limitano a registrare un custom element
        non esportano nulla; l'effetto collaterale è il loro unico scopo;
        la registrazione avviene sempre tramite customElements.define direttamente;
        caso tipico: custom elements — il modulo viene importato solo per eseguire
        customElements.define; non si esporta mai la classe del componente

  ok: |
    // HeaderLayout.js
    class HeaderLayout extends LitElement { ... }
    customElements.define('header-layout', HeaderLayout);
    // nessun export

    // index.js
    import './HeaderLayout.js'; // side-effect import

  ko: |
    // export inutile: il componente non deve essere usato direttamente
    export class HeaderLayout extends LitElement { ... }

    // import inutile di simboli
    import { HeaderLayout } from './HeaderLayout.js';
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

## NAMING CONVENTIONS

```
RULE naming.generality-principle
  applies-to: classi di componenti
  note: l'assenza di suffisso indica il contesto più ampio o il grado più ampio di generalità;
        i suffissi indicano specializzazione o riduzione del contesto;
        una classe Login rappresenta una pagina completa (contesto ampio);
        LoginForm o LoginButton rappresenterebbero componenti più specifici (contesto ridotto);
        il nome senza suffisso rappresenta il livello più alto (pagina/intero contesto);
        i suffissi indicano sempre una specializzazione o una parte del tutto

  ok: |
    // Pagina completa (contesto più ampio)
    class Login extends LitElement { ... }

    // Componente specializzato (contesto ridotto)
    class LoginForm extends LitElement { ... }
    class LoginButton extends LitElement { ... }

    // Contesto completo
    class Dashboard extends LitElement { ... }

    // Parti del contesto
    class DashboardHeader extends LitElement { ... }
    class DashboardSidebar extends LitElement { ... }

  ko: |
    // Ridondante: il suffisso Page non aggiunge valore se tutti i componenti
    // in quel contesto sono pagine
    class LoginPage extends LitElement { ... }

    // Ridondante: "Page" non aggiunge informazione
    class DashboardPage extends LitElement { ... }

    // Ambiguo: manca il contesto principale
    class Header extends LitElement { ... }
```

```
RULE naming.file-equals-class
  applies-to: file che definiscono componenti (classi)
  note: il nome del file corrisponde esattamente al nome della classe (pattern Java-like);
        i file componente usano PascalCase;
        i file di contesto (orchestrazione, routing, inizializzazione) usano lowercase

  ok: |
    // Componenti (classi)
    user/auth/Login.js → class Login
    user/auth/Register.js → class Register
    user/account/Profile.js → class Profile
    user/account/Settings.js → class Settings

    // Contesti (orchestrazione)
    user/index.js → mount/unmount + routing
    user/init.js → initialization function

  ko: |
    // Nome file non corrisponde al nome classe
    user/auth/login.js → class Login
    user/auth/LoginPage.js → class Login

    // Contesto in PascalCase
    user/Index.js → mount/unmount function
```

```
RULE naming.namespace-via-path
  applies-to: organizzazione dei moduli
  note: il namespace è espresso tramite la struttura delle cartelle, non tramite prefissi
        nel nome della classe; le sottocartelle seguono la semantica del dominio;
        questo elimina ridondanza e migliora la leggibilità

  ok: |
    user/
      auth/
        Login.js → class Login (non UserLogin)
        Register.js → class Register
      account/
        Profile.js → class Profile
        Settings.js → class Settings

  ko: |
    user/
      UserLogin.js → class UserLogin (prefisso ridondante)
      UserRegister.js → class UserRegister
      UserProfile.js → class UserProfile
```

```
RULE naming.custom-elements
  applies-to: registrazione custom elements
  note: il tag name usa kebab-case e include il prefisso del modulo per evitare conflitti
        globali nel registry dei custom elements;
        per moduli top-level il prefisso è il nome del modulo (es. user-);
        per moduli con namespace (ns/name) il prefisso è ns-name- (es. cti-vonage-);
        il prefisso rimane anche se il nome della classe non lo include;
        il prefisso è obbligatorio per evitare collisioni globali nel registry;
        il tag è sempre kebab-case e univoco a livello applicativo

  ok: |
    // stesso nome di classe in moduli diversi: il prefisso evita la collisione
    // modulo user
    customElements.define('user-profile', Profile);

    // modulo admin
    customElements.define('admin-profile', Profile);

    // Modulo top-level — File: user/auth/Login.js
    class Login extends LitElement { ... }
    customElements.define('user-login', Login);

    // Modulo top-level — File: user/account/Profile.js
    class Profile extends LitElement { ... }
    customElements.define('user-profile', Profile);

    // Modulo con namespace — File: cti/vonage/call/Call.js
    class Call extends LitElement { ... }
    customElements.define('cti-vonage-call', Call);

    // Modulo con namespace — File: cti/vonage/session/Panel.js
    class Panel extends LitElement { ... }
    customElements.define('cti-vonage-panel', Panel);

  ko: |
    // Collisione globale possibile: manca prefisso modulo
    customElements.define('profile', Profile);

    // Formato non valido: non è kebab-case
    customElements.define('userProfile', Profile);

    // Manca prefisso modulo (rischio conflitti)
    class Login extends LitElement { ... }
    customElements.define('login', Login);

    // Namespace non incluso nel prefisso
    class Call extends LitElement { ... }
    customElements.define('vonage-call', Call);

    // Solo il namespace, manca il nome del modulo
    class Call extends LitElement { ... }
    customElements.define('cti-call', Call);
```

```
RULE naming.semantic-folders
  applies-to: organizzazione interna ai moduli
  note: le sottocartelle seguono criteri semantici legati al dominio, non tecnici;
        auth/ contiene componenti di autenticazione, account/ contiene gestione account;
        evita cartelle come components/, pages/, views/ che non esprimono semantica di dominio

  ok: |
    user/
      auth/ → autenticazione (login, register, password reset)
      account/ → gestione account (profile, settings)

    crm/
      contatti/ → gestione contatti
      aziende/ → gestione aziende

  ko: |
    user/
      components/ → generico, non esprime dominio
      pages/ → tecnico, non semantico
      views/ → tecnico, non semantico
```

```
RULE naming.module-entry
  applies-to: file index.js di ogni modulo
  note: il file index.js è il punto di ingresso del modulo ed è esentato dalla regola
        naming.file-equals-class; il suo scopo è orchestrare il montaggio del modulo
        nel DOM e non è considerato un componente;
        esporta un oggetto default con il metodo mount(container);
        per moduli semplici (un solo componente) il componente può essere definito
        nello stesso file index.js; per moduli complessi i componenti stanno in file separati
        (PascalCase, un file per classe) e index.js si limita all'orchestrazione;
        distinzione esplicita tra moduli semplici e complessi

  ok: |
    // modulo semplice (1 componente): componente e orchestrazione nello stesso file
    class StatusView extends LitElement { ... }
    customElements.define('status-view', StatusView);

    export default {
      mount(container) {
        container.innerHTML = '<status-view></status-view>';
      }
    };

    // modulo complesso (più componenti): index.js solo orchestrazione
    import './auth/Login.js';
    import './auth/Register.js';

    export default {
      mount(container) {
        container.innerHTML = '<user-login></user-login>';
      }
    };

  ko: |
    // index.js non esporta nulla (il router non riesce a montare il modulo)
    class StatusView extends LitElement { ... }
    customElements.define('status-view', StatusView);

    // logica e componenti mescolati senza criterio: import + definizione inline
    import './auth/Login.js';

    class Something extends LitElement { ... }

    // manca export default con mount
```

---

## COMMENTI

```
RULE comment.jsdoc-for-functions
  applies-to: tutte le funzioni esportate o pubblicamente rilevanti
  note: i commenti che descrivono una funzione usano il formato JSDoc con /** */;
        ogni @param include la type annotation in formato {type} e una descrizione;
        @returns è obbligatorio se la funzione restituisce un valore, con type annotation e descrizione;
        i commenti inline // restano validi solo per note di contesto non legate a una funzione

  ok: |
    /**
     * Verifica la sessione sul server e aggiorna auth di conseguenza.
     * @param {string} url - URL dell'endpoint
     * @param {RequestInit} options - Opzioni fetch
     * @returns {Promise<Response>} risposta del server dopo eventuale refresh del token
     */
    async function fetchWithRefresh(url, options = {}) {
      ...
    }

  ko: |
    // Verifica la sessione sul server e aggiorna auth di conseguenza.
    async function fetchWithRefresh(url, options = {}) {
      ...
    }

    // @param senza type annotation
    /**
     * @param url - URL dell'endpoint
     * @param options - Opzioni fetch
     * @returns risposta del server
     */
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
        di applicare gli stili normalmente — il corpo di createRenderRoot() sta su
        righe separate come qualsiasi altro metodo (regola fmt.no-inline-function-body);
        la logica di rendering è in render(), che restituisce un template html`...`;
        la registrazione avviene con customElements.define dopo la definizione della classe

  ok: |
    class AuthLayout extends LitElement {
      static properties = {
        _loading: { state: true },
        _error: { state: true }
      };

      createRenderRoot() {
        return this;
      }

      constructor() {
        super();
        this._loading = false;
        this._error = null;
      }

      render() {
        return html`...`;
      }
    }

    customElements.define('auth-layout', AuthLayout);

  ko: |
    // Estende HTMLElement invece di LitElement; usa innerHTML invece di html``
    class AuthLayout extends HTMLElement {
      constructor() {
        super();
        this._loading = false;
        this._error = null;
      }

      _render() {
        this.innerHTML = `...`;
      }
    }

    customElements.define('auth-layout', AuthLayout);

    // createRenderRoot con corpo inline: viola fmt.no-inline-function-body
    class AuthLayout extends LitElement {
      createRenderRoot() { return this; }
    }
```

```
RULE wc.reactive-render
  applies-to: componenti che dipendono da uno store
  note: il componente si sottoscrive allo store in connectedCallback e si disiscrive
        in disconnectedCallback, salvando la funzione di unsub restituita da subscribe();
        ogni aggiornamento dello store assegna il nuovo valore alla proprietà reattiva
        corrispondente (this._x = v); Lit rileva la modifica e chiama render()
        automaticamente — non si chiama render() esplicitamente;
        il subscriber non passa lo stato come argomento a un metodo handler separato;
        render() non viene mai chiamato manualmente;
        qualsiasi chiamata esplicita a render() è sempre un errore

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
    // chiamata manuale vietata nel subscriber
    connectedCallback() {
      super.connectedCallback();
      this._unsubAuth = store.subscribe(v => {
        this._value = v;
        this.render();
      });
    }

    // chiamata diretta altrove nel componente
    someMethod() {
      this.render();
    }

    // lo stato non va passato come parametro a un handler: bypassa il pattern reattivo
    connectedCallback() {
      super.connectedCallback();
      auth.subscribe(state => this._updateView(state));
    }
```
