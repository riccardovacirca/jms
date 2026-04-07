# WF-001-INITIALIZATION

### Inizializzazione entità applicative

### Obiettivo

Inizializzare tutte le componenti dell’applicazione, configurare servizi core e avviare il server.

### Attori

* Applicazione (`App.main`)
* Configurazione (`Config`)
* Database (`DB`)
* Autenticazione (`Auth`)
* Servizio email (`Mail`)
* Esecutore asincrono (`AsyncExecutor`)
* Flyway (`Flyway`)
* Scheduler (`Scheduler`)
* Moduli applicativi (`Moduli`)
* Router e server (`Undertow`)

### Precondizioni

* File di configurazione `application.properties` presente e valido
* Database accessibile
* Moduli necessari disponibili

---

### Flusso principale

1. `App.main` legge la configurazione tramite `Config`
2. Inizializza servizi core:

   * `DB.init(config)`
   * `Auth.init(jwtSecret, expiry)`
   * `Mail.init(config)`
   * `AsyncExecutor.init(poolSize)`
3. Esegue migrazioni DB tramite `Flyway.runMigrations()`
4. Inizializza lo scheduler con `Scheduler.init(config, DB.getDataSource())`
5. Verifica dipendenze dei moduli con `Modules.checkModuleDependencies()`
6. Configura router e static handler con `Router(paths, DB.getDataSource())`
7. Avvia server `Undertow` e logga che il server è pronto

---

### Postcondizioni

* Tutti i servizi core inizializzati correttamente
* Database aggiornato se necessario
* Server in ascolto pronto a ricevere richieste
* Eventuali warning sui moduli mancanti loggati

---

### Diagramma di sequenza

```mermaid
sequenceDiagram
    participant Main as App.main
    participant Config as Config
    participant DB as DB
    participant Auth as Auth
    participant Mail as Mail
    participant Async as AsyncExecutor
    participant Flyway as Flyway
    participant Scheduler as Scheduler
    participant Modules as Moduli
    participant Router as Router
    participant Server as Undertow

    %% Caricamento configurazione
    Main->>Config: leggi application.properties
    Config-->>Main: valori config

    %% Init servizi core
    Main->>DB: DB.init(config)
    Main->>Auth: Auth.init(jwtSecret, expiry)
    Main->>Mail: Mail.init(config)
    Main->>Async: AsyncExecutor.init(poolSize)

    %% Migrazione DB
    Main->>Flyway: runMigrations()
    Flyway-->>Main: risultato migrazione

    %% Scheduler
    Main->>Scheduler: Scheduler.init(config, DB.getDataSource())

    %% Verifica moduli
    Main->>Modules: checkModuleDependencies()
    Modules-->>Main: warning/moduli mancanti

    %% Router e static handler
    Main->>Router: Router(paths, DB.getDataSource())

    %% Avvio server
    Main->>Server: Undertow.builder(...).setHandler(paths).build()
    Server-->>Main: server pronto
```
