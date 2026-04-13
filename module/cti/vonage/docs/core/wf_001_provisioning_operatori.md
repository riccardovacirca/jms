# WF-CTI-001-PROVISIONING-OPERATORI

### Provisioning operatori CTI (admin)

### Obiettivo

Creare, registrare e associare gli operatori CTI. Ogni operatore deve esistere sia su Vonage (come utente dell'applicazione) sia localmente (tabella `jms_cti_operatori`). L'associazione permanente tra un account utente (`jms_accounts.id`) e un operatore (`jms_cti_operatori.account_id`) è il requisito necessario perché quell'utente possa connettersi al CTI.

### Attori

* Amministratore (`Browser/Admin`)
* Handler operatori (`OperatorHandler`)
* Vonage Users API (`VoiceHelper.createVonageUser`)
* DAO locale (`OperatorDAO`)

### Precondizioni

* Credenziali Vonage configurate (`cti.vonage.application_id`, `cti.vonage.private_key`)
* Amministratore autenticato con ruolo ADMIN

---

### Flusso A — Creazione singola

1. Admin invia `POST /api/cti/vonage/admin/operator` con `{name}`
2. `OperatorHandler.create` valida che `name` sia presente
3. `VoiceHelper.createVonageUser(name, null)` chiama la Vonage Users API
4. Vonage restituisce il nome utente accettato (diventa `vonage_user_id`)
5. `OperatorDAO.insert(vonageUserId)` crea il record locale con `attivo = TRUE`, `account_id = NULL`
6. Risposta: `{vonageUserId, attivo: true}`

### Diagramma — Flusso A: Creazione singola

```mermaid
sequenceDiagram
    participant Admin as Browser/Admin
    participant Handler as OperatorHandler
    participant Helper as VoiceHelper
    participant Vonage as Vonage Users API
    participant DAO as OperatorDAO

    Admin->>Handler: POST /api/cti/vonage/admin/operator {name}
    Handler->>Handler: session.require(ADMIN, WRITE)
    Handler->>Handler: Validator.required(name)
    Handler->>Helper: createVonageUser(name, null)
    Helper->>Vonage: UsersClient.createUser(name)
    Vonage-->>Helper: vonageUserId accettato
    Helper-->>Handler: vonageUserId
    Handler->>DAO: insert(vonageUserId)
    DAO->>DAO: INSERT INTO jms_cti_operatori (vonage_user_id) RETURNING id
    DAO-->>Handler: id generato
    Handler-->>Admin: {vonageUserId, attivo: true}
```

### Flusso B — Sincronizzazione da Vonage

1. Admin invia `POST /api/cti/vonage/admin/operator/sync`
2. `VoiceHelper.listVonageUsers()` recupera tutti gli utenti dall'applicazione Vonage
3. Per ogni utente Vonage non presente in `jms_cti_operatori`, `OperatorDAO.insert()` crea il record locale
4. Utenti locali senza corrispondente su Vonage non vengono toccati
5. Risposta: lista degli operatori locali creati

### Diagramma — Flusso B: Sincronizzazione da Vonage

```mermaid
sequenceDiagram
    participant Admin as Browser/Admin
    participant Handler as OperatorHandler
    participant Helper as VoiceHelper
    participant Vonage as Vonage Users API
    participant DAO as OperatorDAO

    Admin->>Handler: POST /api/cti/vonage/admin/operator/sync
    Handler->>Handler: session.require(ADMIN, WRITE)
    Handler->>Helper: listVonageUsers()
    Helper->>Vonage: UsersClient.listUsers()
    Vonage-->>Helper: lista utenti Vonage
    Helper-->>Handler: vonageUsers

    loop per ogni utente Vonage
        Handler->>DAO: findByVonageUserId(name)
        DAO-->>Handler: existing (o null)
        alt utente non presente localmente
            Handler->>DAO: insert(name)
            DAO-->>Handler: id generato
            Note over Handler: aggiunge entry a created[]
        end
    end

    Handler-->>Admin: lista operatori creati
```

### Flusso C — Assegnazione account utente a operatore

**Obbligatorio** prima che un utente possa connettersi al CTI (WF-CTI-002).

1. Admin apre la dashboard CTI → sezione Operatori
2. `Operators.js` carica la lista operatori via `GET /api/cti/vonage/admin/operator`
   - La risposta include `account_id` e `account_username` (JOIN con `jms_accounts`)
3. Admin clicca il pulsante "Assegna" su un operatore
4. `Operators.js` carica la lista account via `GET /api/cti/vonage/admin/accounts?operatorId={id}` — restituisce solo gli account non ancora assegnati ad altri operatori; l'account già assegnato all'operatore corrente (se presente) è incluso
5. Admin seleziona un account dal dropdown e conferma
6. `PUT /api/cti/vonage/admin/operator/{id}/account` con body `{accountId}`
7. `OperatorHandler.assignAccount` verifica che l'operatore esista, poi chiama `OperatorDAO.assignAccount(operatoreId, accountId)`
8. `UPDATE jms_cti_operatori SET account_id = ? WHERE id = ?`
9. La lista viene ricaricata: la colonna "Utente associato" mostra lo username

### Diagramma — Flusso C: Assegnazione account utente

```mermaid
sequenceDiagram
    participant Admin as Browser/Admin
    participant Handler as OperatorHandler
    participant DAO as OperatorDAO
    participant DB as jms_accounts

    Admin->>Handler: GET /api/cti/vonage/admin/operator
    Handler->>Handler: session.require(ADMIN, READ)
    Handler->>DAO: findAllForAdmin()
    Note over DAO: SELECT ... LEFT JOIN jms_accounts ON account_id
    DAO-->>Handler: lista con account_username
    Handler-->>Admin: lista operatori

    Admin->>Handler: GET /api/cti/vonage/admin/accounts?operatorId={id}
    Handler->>Handler: session.require(ADMIN, READ)
    Handler->>DB: SELECT ... WHERE id NOT IN (SELECT account_id ... AND id != operatorId)
    DB-->>Handler: account non assegnati ad altri operatori
    Handler-->>Admin: lista account filtrata

    Admin->>Handler: PUT /api/cti/vonage/admin/operator/{id}/account {accountId}
    Handler->>Handler: session.require(ADMIN, WRITE)
    Handler->>DAO: findById(operatoreId)
    DAO-->>Handler: operatore (o null)
    alt operatore non trovato
        Handler-->>Admin: {err: true, log: "Operatore non trovato"}
    else operatore trovato
        Handler->>DAO: assignAccount(operatoreId, accountId)
        DAO->>DAO: UPDATE SET account_id = ? WHERE id = ?
        DAO-->>Handler: ok
        Handler-->>Admin: {err: false}
    end
```

### Flusso D — Rimozione associazione

1. Admin clicca "Rimuovi assegnazione" (pulsante visibile solo se `account_id` è impostato)
2. `DELETE /api/cti/vonage/admin/operator/{id}/account`
3. `OperatorDAO.assignAccount(operatoreId, null)` → `SET account_id = NULL`
4. L'utente precedentemente associato non potrà più connettersi al CTI

### Diagramma — Flusso D: Rimozione assegnazione

```mermaid
sequenceDiagram
    participant Admin as Browser/Admin
    participant Handler as OperatorHandler
    participant DAO as OperatorDAO

    Admin->>Handler: DELETE /api/cti/vonage/admin/operator/{id}/account
    Handler->>Handler: session.require(ADMIN, WRITE)
    Handler->>DAO: findById(operatoreId)
    DAO-->>Handler: operatore (o null)
    alt operatore non trovato
        Handler-->>Admin: {err: true, log: "Operatore non trovato"}
    else operatore trovato
        Handler->>DAO: assignAccount(operatoreId, null)
        DAO->>DAO: UPDATE SET account_id = NULL WHERE id = ?
        DAO-->>Handler: ok
        Handler-->>Admin: {err: false}
    end
```

---

### Postcondizioni

* Ogni operatore ha una riga in `jms_cti_operatori` con il suo `vonage_user_id`
* Il `vonage_user_id` sarà il claim `sub` del JWT SDK al momento della connessione
* `account_id` è `NULL` alla creazione; deve essere impostato esplicitamente tramite Flusso C prima che l'utente possa connettersi
* `claim_account_id` è sempre `NULL` alla creazione (nessun claim attivo)
