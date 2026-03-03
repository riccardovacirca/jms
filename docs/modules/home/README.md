# Module: home

Basic home module with welcome page and API integration example.

> Replace `com.example` with your Maven groupId (e.g. `io.mycompany`).
> Replace `{{APP_PACKAGE_PATH}}` with the corresponding filesystem path (e.g. `io/mycompany`).

## Contents

- `java/home/` — Java package `com.example.home`
- `gui/home/` — Frontend module (SPA, LitElement)
  - `index.js` — Entry point
  - `component.js` — Home web component
  - `home.css` — Styles

## Installation

### 1. Java sources

Copy `java/home/` into your project's Java source tree:

```sh
cp -r java/home/  src/main/java/{{APP_PACKAGE_PATH}}/home/
```

Replace `com.example` in all copied Java files:

```sh
find src/main/java/{{APP_PACKAGE_PATH}}/home -name '*.java' \
     -exec sed -i 's|com.example|your.package|g' {} +
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
cp -r gui/home/  vite/src/modules/home/
```

### 4. Register module in config.js

Add the complete module configuration to `vite/src/config.js`:

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

In `vite/src/config.js`, set home as the default route:

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
