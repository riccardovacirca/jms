# WF-006-STATUS-ENDPOINT

### Health check endpoint

### Obiettivo

Fornire un endpoint per verificare lo stato dell’applicazione e supportare monitoring o health check automatici.

### Attori

* Client HTTP (`Client HTTP`)
* Server (`Undertow Server`)
* Path dispatcher (`PathTemplateHandler`)
* Handler dello status (`StatusHandler`)

### Precondizioni

* Server in ascolto
* Endpoint `/api/status` registrato

---

### Flusso principale

1. `Client` invia richiesta GET a `/api/status`
2. `Server` passa la richiesta a `PathTemplateHandler`
3. `PathTemplateHandler` instrada la richiesta a `StatusHandler`
4. `StatusHandler` genera risposta JSON:
   `{"err":false,"log":null,"out":"App is running"}`
5. La risposta risale la catena fino al `Client`

---

### Postcondizioni

* Client riceve risposta JSON con lo stato dell’applicazione

---

### Diagramma di sequenza

```mermaid id="wf_status_seq"
sequenceDiagram
    participant Client as Client HTTP
    participant Server as Undertow Server
    participant Path as PathTemplateHandler
    participant Handler as StatusHandler

    Client->>Server: GET /api/status
    Server->>Path: route request
    Path->>Handler: handleRequest(exchange)
    Handler-->>Path: invia JSON {"err":false,"log":null,"out":"App is running"}
    Path-->>Server: risposta inviata
    Server-->>Client: JSON response
```
