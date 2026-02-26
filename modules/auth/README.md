# Module: auth

> Replace `com.example` with your Maven groupId (e.g. `io.mycompany`).
> Replace `{{APP_PACKAGE_PATH}}` with the corresponding filesystem path (e.g. `io/mycompany`).

## Contents

- `java/auth/` — Java package `com.example.auth`
- `gui/auth/` — Vite frontend sources
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

Copy `gui/auth/` into the Vite source tree:

```sh
cp -r gui/auth/  vite/src/auth/
```

### 3. Database migrations

Copy the SQL files into the Flyway migrations directory.
Rename each file with a fresh timestamp to avoid conflicts with existing history:

```sh
# Example — adjust the timestamp to current date/time
cp migration/V20260222_163602__auth.sql  src/main/resources/db/migration/V$(date +%Y%m%d_%H%M%S)__V20260222_163602__auth.sql
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
      .add("/api/auth/login",   route(new LoginHandler(),   DB.getDataSource()))
      .add("/api/auth/session", route(new SessionHandler(), DB.getDataSource()))
      .add("/api/auth/logout",  route(new LogoutHandler(),  DB.getDataSource()))
      .add("/api/auth/refresh",          route(new RefreshHandler(),         DB.getDataSource()))
      .add("/api/auth/change-password",  route(new ChangePasswordHandler(),  DB.getDataSource()))
      .add("/api/auth/forgot-password",  route(new ForgotPasswordHandler(),  DB.getDataSource()))
      .add("/api/auth/2fa",              route(new TwoFactorHandler(),        DB.getDataSource()));
```

### 7. vite.config.js

Add entry points to `rollupOptions.input`:

```js
auth_changepass: resolve(__dirname, 'src/auth/changepass.html'),
auth_login: resolve(__dirname, 'src/auth/login.html'),
```

Add URL rewrites in the `route-rewrite` plugin (dev server only).
Adjust the left-hand URL to match your routing conventions:

```js
else if (req.url === '/auth/changepass') req.url = '/auth/changepass.html'
else if (req.url === '/auth/login') req.url = '/auth/login.html'
```

