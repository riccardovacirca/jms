# Modulo HOME

Dashboard principale del Sistema Controllo Accessi Integrato (SCAI).

## Descrizione

Questo modulo fornisce la pagina home dell'applicazione con una dashboard di navigazione organizzata in card cliccabili. La home presenta:
- **Header verde** con titolo sistema e sottotitolo "Sicurezza Sedi"
- **Griglia di card** (responsive: 1 colonna mobile, 2 tablet, 3 desktop) per accesso rapido alle funzionalità principali
- **Icone Bootstrap Icons** per rappresentazione visiva dei moduli
- **Navigazione** tramite eventi custom verso i moduli dell'applicazione

È un modulo di tipo **ENTRY POINT** senza dipendenze da altri moduli.

## Tipo Modulo

**Entry Point** - Dashboard principale dell'applicazione

## Dipendenze

Nessuna dipendenza - modulo standalone

## Contents

- `java/home/` — Java package con handler di esempio
- `gui/home/` — Frontend module (Lit Web Components)
  - `index.js` — Entry point
  - `component.js` — Home dashboard component
  - `home.css` — Stili custom per dashboard card

## Installation

### 1. Java sources

Copy `java/home/` into your project's Java source tree:

```sh
cp -r java/home/  src/main/java/dev/jms/app/home/
```

### 2. App.java — route registration

Add the import at the top of `App.java`:

```java
import com.example.home.handler.HelloHandler;
```

Add the route in the `PathTemplateHandler` chain:

```java
paths.add("/api/home/hello", route(new HelloHandler(), ds));
```

### 3. Frontend sources

Copy `gui/home/` into the Vite modules directory:

```sh
cp -r gui/home/  gui/src/modules/home/
```

### 4. Register module in config.js

Add the complete module configuration to `gui/src/config.js`:

```javascript
export const MODULE_CONFIG = {
  home: {
    path: '/home',
    container: 'main',
    authorization: null,
    persistent: false,
    init: null
  },
  // ... other modules
};
```

**Configuration attributes:**
- `path: '/home'` — URL hash for navigation
- `container: 'main'` — Mounts in the main content area
- `authorization: null` — Publicly accessible (or set `{ redirectTo: '/auth' }` for protected)
- `persistent: false` — Mounted/unmounted during navigation
- `init: null` — No initialization required

### 5. Set as default route (optional)

In `gui/src/config.js`, set home as the default route:

```javascript
export const DEFAULT_ROUTE = '/home';
```

### 6. Build

Inside the dev container, rebuild the frontend and the backend:

```sh
cmd gui build
cmd app build
```

**Done!** The home module is now accessible via `/#/home`.

## Features

- Welcome message from `/api/home/hello` endpoint (shown when available)
- Simple, clean layout for main content area
- Can be used as template for other content modules

## Module Structure

```
java/home/
└── handler/
    └── HelloHandler.java   # GET /api/home/hello → {"err":false,"log":null,"out":"Hello, World!"}

gui/home/
├── index.js          # Entry point (exports { mount })
├── component.js      # Home web component (LitElement)
└── home.css          # Styles
```

## Dependencies

- Bootstrap CSS (imported in `index.js`)
- `lit` (LitElement)

## Notes

This module does not include header/navigation UI. For authentication-aware navigation, 
install the `header` module separately.
