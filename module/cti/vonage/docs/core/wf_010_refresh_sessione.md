# WF-CTI-010-REFRESH-SESSIONE

### Refresh automatico della sessione SDK

### Obiettivo

Mantenere attiva la sessione Vonage Client SDK oltre la scadenza del JWT (1 ora). Il frontend rinnova il token ogni 13 minuti chiamando di nuovo `POST /api/cti/vonage/sdk/auth`. Il backend rinnova il `claim_scadenza` dell'operatore (+30 minuti). Questo impedisce che il claim venga rilasciato dallo scheduler di cleanup (WF-CTI-012) durante una sessione di lavoro attiva.

### Attori

* Componente CTI (`Bar._scheduleRefresh`)
* Backend CTI (`CallHandler.sdkToken`)
* DAO operatori (`OperatorDAO.claimOrRenewAssigned`)

### Precondizioni

* Sessione WebRTC attiva (WF-CTI-002 completato)
* `_refreshTimer` schedulato a 13 minuti

---

### Flusso principale

1. Timer `REFRESH_DELAY_MS = 13 * 60 * 1000` scatta
2. `Bar._fetchToken()` invia `POST /api/cti/vonage/sdk/auth`
3. `OperatorDAO.findByAccountId(accountId)` trova l'operatore permanentemente assegnato
4. `OperatorDAO.claimOrRenewAssigned(accountId, operatoreId)`:
   * `claim_account_id` è già questo account → `UPDATE claim_scadenza = NOW() + 30 min` → `COMMIT`
5. Genera nuovo JWT RS256 con nuova scadenza (+1 ora)
6. `Bar._lastToken = token`
7. Il timer si riprogramma per il successivo refresh

```mermaid
sequenceDiagram
    participant Timer as setTimeout (13 min)
    participant Bar as Bar
    participant Handler as CallHandler
    participant OpDAO as OperatorDAO
    participant Helper as VoiceHelper

    Timer->>Bar: scatta _scheduleRefresh
    Bar->>Handler: POST /api/cti/vonage/sdk/auth
    Handler->>OpDAO: findByAccountId(accountId)
    Note over OpDAO: WHERE account_id = ? AND attivo = TRUE
    OpDAO-->>Handler: operatore assegnato
    Handler->>OpDAO: claimOrRenewAssigned(accountId, operatoreId)
    Note over OpDAO: claim già attivo → UPDATE claim_scadenza = NOW()+30m
    OpDAO-->>Handler: operatore
    Handler->>Helper: generateSdkJwt(vonageUserId)
    Helper-->>Handler: nuovo JWT RS256
    Handler-->>Bar: {token: "<nuovo JWT>"}
    Bar->>Bar: _lastToken = token
    Bar->>Bar: _scheduleRefresh() riprogramma per altri 13 min
```

### Flusso alternativo — Errore refresh

1. `_fetchToken()` fallisce (es. sessione scaduta, errore rete)
2. `Bar._connect()` cattura l'eccezione → `_sessionError` impostato
3. La sessione WebRTC rimane attiva finché Vonage non la scade

```mermaid
sequenceDiagram
    participant Timer as setTimeout (13 min)
    participant Bar as Bar
    participant Handler as CallHandler

    Timer->>Bar: scatta _scheduleRefresh
    Bar->>Handler: POST /api/cti/vonage/sdk/auth
    Handler-->>Bar: {err: true, log: "..."}
    Bar->>Bar: _sessionError = messaggio errore
    Note over Bar: sessione WebRTC rimane attiva fino a scadenza Vonage
```

---

### Postcondizioni

* `claim_scadenza` rinnovata: la sessione rimane viva per altri 30 minuti
* JWT fresco disponibile per eventuali reconnect SDK
