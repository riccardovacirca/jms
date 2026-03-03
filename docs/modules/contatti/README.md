# Module: contatti

Gestione contatti con CRUD, ricerca full-text, paginazione e raggruppamento in liste.

> Replace `com.example` with your Maven groupId (e.g. `io.mycompany`).
> Replace `{{APP_PACKAGE_PATH}}` with the corresponding filesystem path (e.g. `io/mycompany`).

## Contents

- `java/contatti/` — Java package `com.example.contatti` (handler, dao, dto, adapter)
- `gui/contatti/` — Frontend module (SPA, LitElement)
  - `index.js` — Entry point
  - `module-component.js` — `<contatti-module>` wrapper con tab nav (Contatti | Liste)
  - `component.js` — `<contatti-layout>` web component (lista + form)
  - `liste-component.js` — `<liste-layout>` web component (lista + form + contatti per lista)
- `migration/` — Flyway migration: crea le tabelle `contatti`, `liste`, `lista_contatti`

## Note sulla dipendenza

Le tabelle `liste` e `lista_contatti` fanno parte di questo modulo: appartengono al
dominio dei contatti (raggruppamento/campagne) e non hanno significato autonomo.
Non esistono dipendenze verso altri moduli.

## Installation

### 1. Java sources

Copy `java/contatti/` into your project's Java source tree:

```sh
cp -r java/contatti/  src/main/java/{{APP_PACKAGE_PATH}}/contatti/
```

Replace `com.example` in all copied Java files:

```sh
find src/main/java/{{APP_PACKAGE_PATH}}/contatti -name '*.java' \
     -exec sed -i 's|com.example|your.package|g' {} +
```

### 2. Migration

Copy the migration file:

```sh
cp migration/V20260301_000001__module_contatti.sql \
   src/main/resources/db/migration/
```

> Rename the file with a fresh timestamp if Flyway reports a checksum conflict:
> `V$(date +%Y%m%d_%H%M%S)__module_contatti.sql`

### 3. App.java — route registration

Add the imports at the top of `App.java`:

```java
import com.example.contatti.handler.ContattiHandler;
import com.example.contatti.handler.ContattiSearchHandler;
import com.example.contatti.handler.ContattoBlacklistHandler;
import com.example.contatti.handler.ContattoHandler;
import com.example.contatti.handler.ContattoStatoHandler;
import com.example.contatti.handler.ListaContattiHandler;
import com.example.contatti.handler.ListaContattoHandler;
import com.example.contatti.handler.ListaHandler;
import com.example.contatti.handler.ListaScadenzaHandler;
import com.example.contatti.handler.ListaStatoHandler;
import com.example.contatti.handler.ListeHandler;
```

Add the routes in the `PathTemplateHandler` chain
(`search` deve precedere `{id}` per evitare conflitti di routing):

```java
paths.add("/api/contatti",                    route(new ContattiHandler(),          ds));
paths.add("/api/contatti/search",             route(new ContattiSearchHandler(),    ds));
paths.add("/api/contatti/{id}",               route(new ContattoHandler(),          ds));
paths.add("/api/contatti/{id}/stato",         route(new ContattoStatoHandler(),     ds));
paths.add("/api/contatti/{id}/blacklist",     route(new ContattoBlacklistHandler(), ds));

paths.add("/api/liste",                       route(new ListeHandler(),             ds));
paths.add("/api/liste/{id}",                  route(new ListaHandler(),             ds));
paths.add("/api/liste/{id}/stato",            route(new ListaStatoHandler(),        ds));
paths.add("/api/liste/{id}/scadenza",         route(new ListaScadenzaHandler(),     ds));
paths.add("/api/liste/{id}/contatti",         route(new ListaContattiHandler(),     ds));
paths.add("/api/liste/{id}/contatti/{cid}",   route(new ListaContattoHandler(),     ds));
```

### 4. Frontend sources

Copy `gui/contatti/` into the Vite modules directory:

```sh
cp -r gui/contatti/  vite/src/modules/contatti/
```

### 5. Register route in config.js

Add the module configuration to `vite/src/config.js`:

```javascript
contatti: {
  path: '/contatti',
  authorization: { redirectTo: '/auth' }
}
```

### 6. Build

Inside the dev container, rebuild the frontend and the backend:

```sh
cmd gui build
cmd app build
```

**Done!** The module is accessible via `/#/contatti`.

## API Endpoints

### Contatti

| Method | Path                           | Auth | Description                          |
|--------|--------------------------------|------|--------------------------------------|
| GET    | `/api/contatti`                | ✓    | Lista paginata (`page`, `size`, `listaId`) |
| POST   | `/api/contatti`                | ✓    | Crea contatto                        |
| GET    | `/api/contatti/search`         | ✓    | Ricerca full-text (`q`, `page`, `size`) |
| GET    | `/api/contatti/{id}`           | ✓    | Recupera contatto                    |
| PUT    | `/api/contatti/{id}`           | ✓    | Aggiorna contatto                    |
| DELETE | `/api/contatti/{id}`           | ✓    | Elimina contatto                     |
| PUT    | `/api/contatti/{id}/stato`     | ✓    | Aggiorna stato. Body: `{"stato":1}`  |
| PUT    | `/api/contatti/{id}/blacklist` | ✓    | Aggiorna blacklist. Body: `{"blacklist":true}` |

### Liste

| Method | Path                                  | Auth | Description                                  |
|--------|---------------------------------------|------|----------------------------------------------|
| GET    | `/api/liste`                          | ✓    | Liste attive paginate (`page`, `size`)        |
| POST   | `/api/liste`                          | ✓    | Crea lista                                   |
| GET    | `/api/liste/{id}`                     | ✓    | Recupera lista                               |
| PUT    | `/api/liste/{id}`                     | ✓    | Aggiorna lista                               |
| DELETE | `/api/liste/{id}`                     | ✓    | Soft delete lista                            |
| PUT    | `/api/liste/{id}/stato`               | ✓    | Aggiorna stato. Body: `{"stato":1}`          |
| PUT    | `/api/liste/{id}/scadenza`            | ✓    | Aggiorna scadenza. Body: `{"scadenza":"2026-12-31"}` |
| GET    | `/api/liste/{id}/contatti`            | ✓    | Contatti della lista (`page`, `size`)        |
| POST   | `/api/liste/{id}/contatti`            | ✓    | Aggiunge contatto. Body: `{"contattoId":42}` |
| DELETE | `/api/liste/{id}/contatti/{cid}`      | ✓    | Rimuove contatto dalla lista                 |

### Response envelope

```json
{ "err": false, "log": null, "out": { ... } }
```

Lista/ricerca: `out = { total, page, size, items: [...] }`

### ContattoDTO (campi)

`id`, `nome`, `cognome`, `ragioneSociale`, `telefono`, `email`, `indirizzo`,
`citta`, `cap`, `provincia`, `note`, `stato` (int), `consenso` (bool),
`blacklist` (bool), `createdAt` (ISO-8601), `updatedAt` (ISO-8601), `listeCount` (long)

### ListaDTO (campi)

`id`, `nome`, `descrizione`, `consenso` (bool), `stato` (int), `scadenza` (ISO-8601 date),
`createdAt` (ISO-8601), `updatedAt` (ISO-8601), `deletedAt` (ISO-8601), `contattiCount` (long)

### ListaContattoDTO (campi)

`id`, `listaId`, `contattoId`, `createdAt` (ISO-8601), `nome`, `cognome`, `telefono`

## Module Structure

```
java/contatti/
├── dto/
│   ├── ContattoDTO.java
│   ├── ListaDTO.java
│   └── ListaContattoDTO.java
├── adapter/
│   ├── ContattoAdapter.java          # parsing body → ContattoDTO
│   └── ListaAdapter.java             # parsing body → ListaDTO
├── dao/
│   ├── ContattoDAO.java              # CRUD + search + paginazione
│   └── ListaDAO.java                 # CRUD soft-delete + associazioni contatti
└── handler/
    ├── ContattiHandler.java          # GET + POST /api/contatti
    ├── ContattiSearchHandler.java    # GET /api/contatti/search
    ├── ContattoHandler.java          # GET + PUT + DELETE /api/contatti/{id}
    ├── ContattoStatoHandler.java     # PUT /api/contatti/{id}/stato
    ├── ContattoBlacklistHandler.java # PUT /api/contatti/{id}/blacklist
    ├── ListeHandler.java             # GET + POST /api/liste
    ├── ListaHandler.java             # GET + PUT + DELETE /api/liste/{id}
    ├── ListaStatoHandler.java        # PUT /api/liste/{id}/stato
    ├── ListaScadenzaHandler.java     # PUT /api/liste/{id}/scadenza
    ├── ListaContattiHandler.java     # GET + POST /api/liste/{id}/contatti
    └── ListaContattoHandler.java     # DELETE /api/liste/{id}/contatti/{cid}

gui/contatti/
├── index.js              # Entry point (monta <contatti-module>)
├── module-component.js   # <contatti-module> tab nav (Contatti | Liste)
├── component.js          # <contatti-layout> (lista + form)
├── liste-component.js    # <liste-layout> (lista + form + contatti per lista)
└── contatti.css

migration/
└── V20260301_000001__module_contatti.sql  # contatti + liste + lista_contatti
```

## Dependencies

- Bootstrap CSS (importato in `index.js`)
- `lit` (LitElement)
