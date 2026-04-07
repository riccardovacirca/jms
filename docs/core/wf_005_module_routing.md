# WF-005-MODULE-ROUTING

### Routing dei moduli

### Obiettivo

Registrare le route dei moduli e instradare le richieste HTTP alle rispettive logiche di business.

### Attori

* Applicazione (`App.main`)
* Router (`Router`)
* Moduli applicativi (`module.user.Routes`, `module.cti.vonage.Routes`, `module.home.Routes`)
* Handler richieste (`RequestHandler`)
* Adapter (`HandlerAdapter`)
* Database/DAO (`DAO/DB`)

### Precondizioni

* Moduli installati e disponibili
* Router inizializzato
* DAO pronto per query/aggiornamenti

---

### Flusso principale

1. `App.main` registra le route di ciascun modulo:

   * `module.user.Routes.register(router, config)`
   * `module.cti.vonage.Routes.register(router, config)`
   * `module.home.Routes.register(router)`
2. Al runtime, quando arriva una richiesta API verso un modulo:

   * `Router` invoca `HandlerAdapter`
   * `HandlerAdapter` chiama `RequestHandler.handleRequest(exchange)`
   * `RequestHandler` interagisce con il `DAO` se necessario
   * La risposta risale la catena fino al `Client`

---

### Postcondizioni

* Tutte le route dei moduli registrate correttamente
* Richieste instradate e gestite dai moduli appropriati
* Risposta inviata al client

---

### Diagramma di sequenza

```mermaid id="8t3qmv"
sequenceDiagram
    participant Main as App.main
    participant Router as Router
    participant ModuleUser as module.user.Routes
    participant ModuleCTI as module.cti.vonage.Routes
    participant ModuleHome as module.home.Routes
    participant Handler as RequestHandler
    participant Adapter as HandlerAdapter
    participant DAO as DAO/DB

    %% Registrazione route moduli
    Main->>ModuleUser: Routes.register(router, config)
    ModuleUser-->>Router: registra endpoint utente
    Main->>ModuleCTI: Routes.register(router, config)
    ModuleCTI-->>Router: registra endpoint CTI/Vonage
    Main->>ModuleHome: Routes.register(router)
    ModuleHome-->>Router: registra endpoint home

    %% Gestione richiesta modulare
    note over Router: Al runtime, quando arriva una richiesta API
    Client->>Router: HTTP Request a route modulo
    Router->>Adapter: invoke RequestHandler
    Adapter->>Handler: handleRequest(exchange)
    Handler->>DAO: eventualmente query/aggiornamento DB
    DAO-->>Handler: risultati DB
    Handler-->>Adapter: risposta
    Adapter-->>Router: invia response
    Router-->>Client: HTTP Response
```
