# Module: header

Persistent header bar with authentication-aware UI.

> This module requires Bootstrap (already included in base template) and LitElement.

## Contents

- `gui/header/` — Frontend module (SPA, LitElement)
  - `index.js` — Entry point
  - `component.js` — Header web component

## Installation

### 1. Frontend sources

Copy `gui/header/` into the Vite modules directory:

```sh
cp -r gui/header/  vite/src/modules/header/
```

### 2. Register module in config.js

Add the module configuration to `vite/src/config.js`:

```javascript
export const MODULE_CONFIG = {
  header: {
    path: null,
    container: 'header',
    authorization: null,
    persistent: true,
    init: null
  },
  // ... other modules
};
```

**Important:** The header module must be declared **before** other modules in MODULE_CONFIG to ensure it mounts first.

### 3. Build

Inside the dev container, rebuild the frontend:

```sh
cmd gui build
```

**Done!** The header is now visible on all pages.

## Features

- Always visible (persistent module, never unmounted)
- Reactive to `authorized` and `user` store changes
- Shows login button when user is not authenticated
- Shows username and logout button when authenticated
- App title with link to home (`/#/home`)

## Module Structure

```
gui/header/
├── index.js          # Entry point (exports { mount })
└── component.js      # Header web component (LitElement)
```

## Dependencies

- Bootstrap CSS (imported in `index.js`)
- `authorized`, `user` from `../../store.js`
- `lit` (LitElement)

## Configuration

The header module requires these attributes in `config.js`:
- `path: null` — Not accessible via URL routing
- `container: 'header'` — Mounts in the header container
- `persistent: true` — Always mounted, never unmounted
