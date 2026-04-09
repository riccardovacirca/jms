# WF-USER-006-LOGOUT

### Logout

### Obiettivo

Terminare la sessione dell'utente: revocare il refresh token dal database e aggiungere l'access token alla blacklist in memoria per impedirne il riutilizzo fino alla scadenza naturale.

### Attori

* Utente (`Browser`)
* Handler auth (`AuthHandler.logout`)
* DAO refresh token (`RefreshTokenDAO`)
* `Auth`, `JWTBlacklist`

### Precondizioni

* Cookie `refresh_token` e/o `access_token` presenti nel browser

---

### Flusso principale

1. Browser invia `POST /api/user/auth/logout`
2. Legge cookie `refresh_token`; se presente → `RefreshTokenDAO.delete(refreshToken)` elimina il record dal DB
3. Legge cookie `access_token`; se presente:
   * `Auth.get().extractJTI(accessToken)` estrae il JWT ID (`jti`)
   * `Auth.get().extractExpiration(accessToken)` estrae la scadenza Unix
   * Se entrambi validi → `JWTBlacklist.revoke(jti, expiresAt)` aggiunge il token alla blacklist in memoria (TTL = scadenza naturale)
4. `res.clearCookie(ACCESS_TOKEN)` e `res.clearCookie(REFRESH_TOKEN)` azzerano i cookie lato client
5. Risposta: `{err: false, log: "Logout effettuato"}`

---

### Postcondizioni

* Refresh token eliminato da `jms_refresh_tokens`
* Access token revocato in `JWTBlacklist` fino alla scadenza naturale
* Cookie `access_token` e `refresh_token` azzerati nel browser (`maxAge = 0`)

---

### Diagramma di sequenza

```mermaid
sequenceDiagram
    participant Browser
    participant Handler as AuthHandler
    participant TokDAO as RefreshTokenDAO
    participant Auth as Auth
    participant BL as JWTBlacklist

    Browser->>Handler: POST /api/user/auth/logout + cookie access_token + refresh_token
    Handler->>Handler: req.cookie(REFRESH_TOKEN)
    opt refresh_token presente
        Handler->>TokDAO: delete(refreshToken)
    end
    Handler->>Handler: req.cookie(ACCESS_TOKEN)
    opt access_token presente
        Handler->>Auth: extractJTI(accessToken)
        Auth-->>Handler: jti
        Handler->>Auth: extractExpiration(accessToken)
        Auth-->>Handler: expiresAt (Unix)
        opt jti e expiresAt validi
            Handler->>BL: revoke(jti, expiresAt)
        end
    end
    Handler-->>Browser: {err: false, log: "Logout effettuato"} + clearCookie(access_token) + clearCookie(refresh_token)
```
