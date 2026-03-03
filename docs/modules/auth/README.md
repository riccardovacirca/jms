# Module: auth

> Replace `com.example` with your Maven groupId (e.g. `io.mycompany`).
> Replace `{{APP_PACKAGE_PATH}}` with the corresponding filesystem path (e.g. `io/mycompany`).

## Contents

- `java/auth/` — Java package `com.example.auth`
- `gui/auth/` — Frontend module (SPA, LitElement)
- `migration/` — Flyway SQL migrations

## Installation

### 1. Java sources

Copy `java/auth/` into your project's Java source tree:

```sh
cp -r java/auth/  src/main/java/{{APP_PACKAGE_PATH}}/auth/
```

Replace `com.example` in all copied Java files:

```sh
find src/main/java/{{APP_PACKAGE_PATH}}/auth -name '*.java' \
     -exec sed -i 's|com.example|your.package|g' {} +
```

### 2. Frontend sources

Copy `gui/auth/` into the Vite modules directory:

```sh
cp -r gui/auth/  vite/src/modules/auth/
```

### 3. Database migrations

Copy the SQL files into the Flyway migrations directory:

```sh
cp migration/*.sql  src/main/resources/db/migration/
```

### 4. pom.xml — dependencies

Add inside `<dependencies>` in `pom.xml`:

```xml
<dependency>
    <groupId>com.auth0</groupId>
    <artifactId>java-jwt</artifactId>
    <version>4.4.0</version>
</dependency>
```

### 5. application.properties — configuration keys

Add missing keys to `config/application.properties`:

```properties
jwt.secret=dev-secret-change-in-production
jwt.access.expiry.seconds=900
mail.enabled=false
mail.host=mailpit
mail.port=1025
mail.auth=false
mail.user=
mail.password=
mail.from=noreply@example.com
```

### 6. App.java — route registrations

Add imports at the top of `App.java`:

```java
import com.example.auth.handler.ChangePasswordHandler;
import com.example.auth.handler.ForgotPasswordHandler;
import com.example.auth.handler.LoginHandler;
import com.example.auth.handler.LogoutHandler;
import com.example.auth.handler.RefreshHandler;
import com.example.auth.handler.SessionHandler;
import com.example.auth.handler.TwoFactorHandler;
```

Add routes in the `PathTemplateHandler` chain:

```java
paths.add("/api/auth/login",           route(new LoginHandler(),          ds));
paths.add("/api/auth/session",         route(new SessionHandler(),        ds));
paths.add("/api/auth/logout",          route(new LogoutHandler(),         ds));
paths.add("/api/auth/refresh",         route(new RefreshHandler(),        ds));
paths.add("/api/auth/change-password", route(new ChangePasswordHandler(), ds));
paths.add("/api/auth/forgot-password", route(new ForgotPasswordHandler(), ds));
paths.add("/api/auth/2fa",             route(new TwoFactorHandler(),      ds));
```

### 7. Register module in config.js

Add the complete module configuration to `vite/src/config.js`:

```javascript
export const MODULE_CONFIG = {
  auth: {
    path: '/auth',
    container: 'main',
    authorization: null,
    persistent: false,
    init: () => import('./modules/auth/init.js').then(m => m.default())
  },
  // ... other modules
};
```

**Configuration attributes:**
- `path: '/auth'` — URL hash for navigation
- `container: 'main'` — Mounts in the main content area
- `authorization: null` — Publicly accessible (login page)
- `persistent: false` — Mounted/unmounted during navigation
- `init: function` — Session restore executed at app startup

The `init` thunk is called by the router at startup (before the first routing) to
restore the user session. Only the small `init.js` file is loaded at boot — the
full module is still lazy-loaded when the user navigates to `/#/auth`.

For routes that require authorization, set their redirect to `/auth`:

```javascript
home: {
  path: '/home',
  container: 'main',
  authorization: { redirectTo: '/auth' },
  persistent: false,
  init: null
}
```

### 8. Build

Inside the dev container, rebuild the frontend and the backend:

```sh
cmd gui build
cmd app build
```

**Done!** The auth module handles login, session restore on page load, and token refresh.

## Behavior

- On router startup: `init.js` checks `/api/auth/session` — if valid, sets `authorized`/`user` in the store before the first route loads
- On mount (if already authorized): redirects to `DEFAULT_ROUTE` without a network call
- On login: sets `authorized.set(true)` and `user.set(data)`, navigates to `DEFAULT_ROUTE`
- On `must_change_password`: shows the change password form
- After password change: logs out server-side, clears store, returns to login form

## Dependencies

- Bootstrap CSS (imported in `index.js`)
- `authorized`, `user` from `../../store.js`
- `DEFAULT_ROUTE` from `../../config.js`
- `lit` (LitElement)
