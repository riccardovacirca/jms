# Modulo CTI

Integrazione telefonica (Computer Telephony Integration) basata su Vonage Voice API e Vonage Client SDK. Implementa il pattern **operator-first progressive dialer**: l'operatore è già in linea prima che il cliente risponda.

---

## Autenticazione

L'accesso al modulo non richiede un account nel sistema `user`. Il client si autentica presentando un'API key condivisa (`cti.api.key`) configurata lato server: se valida, riceve un JWT con cui operare su tutte le route CTI.

```
Frontend → POST /api/cti/auth  { "apiKey": "..." }
Backend  → verifica cti.api.key in config
         → emette JWT HS256 (cookie access_token)
```

Tutte le chiamate successive includono il cookie `access_token`.

---

## Connessione sessione WebRTC (operatore)

All'avvio del componente `<cti-call>`, il frontend stabilisce la sessione WebRTC con Vonage.

```
Frontend → GET /api/cti/sdk/auth?userId=...
Backend  → genera JWT RS256 per Vonage Client SDK (scadenza 1h)
Frontend → VonageClient.createSession(token)
         → sessione WebRTC attiva
         → pianifica refresh token ogni 13 minuti
```

---

## Avvio chiamata

Il frontend passa il numero del cliente direttamente a `serverCall()`. Vonage lo
inoltra come query param all'answer URL. Non è necessario alcun round-trip
preliminare di registrazione.

```
1. Frontend → VonageClient.serverCall({ customerNumber: "+39..." })
             Il Client SDK notifica Vonage che l'operatore vuole avviare una chiamata.
             Vonage chiama il backend sull'Answer URL configurato nel Dashboard:
   Vonage   → POST /api/cti/answer?customerNumber=+39...  (webhook, senza autenticazione applicativa)
             Il backend deve rispondere entro pochi secondi con un NCCO.

2. Backend (answer):
   - identifica l'operatore leggendo il campo from_user del body del webhook.
     Vonage lo valorizza con il sub del JWT SDK, ovvero l'userId passato a
     generateSdkJwt() al momento della connessione. Se from_user è assente,
     il backend registra un errore e non avvia la chiamata.
   - legge customerNumber dal query param (passato da Vonage dal serverCall)
   - genera conversationName univoco (call-<uuid>)
   - risponde SUBITO a Vonage con NCCO operatore:
       { action: conversation, name: <conv>, startOnEnter: false, musicOnHoldUrl: ... }
     → operatore entra nella conversazione e sente la musica di attesa

3. Backend (stesso thread, dopo 1s):
   - POST Vonage API /v1/calls con NCCO cliente:
       { action: conversation, name: <conv> }  ← stesso conversationName
     → Vonage chiama il numero del cliente
   - registra in memoria: operatorUuid → customerUuid
   - persiste il record nella tabella chiamate

4. Cliente risponde → entra nella conversazione → audio bidirezionale attivo
   Frontend: stato callState passa da waiting_customer a connected
```

---

## Fine chiamata

```
Frontend → PUT /api/cti/call/{uuid}/hangup
Backend  → PUT Vonage API /v1/calls/{operatorUuid}   { action: hangup }
         → PUT Vonage API /v1/calls/{customerUuid}   { action: hangup }
         → rimuove coppia dalla mappa in-memory

Vonage   → evento callHangup al Client SDK
Frontend → callState torna a idle
```

---

## Stato in-memory (VoiceHelper)

Una mappa `ConcurrentHashMap` thread-safe, condivisa tra richieste:

| Mappa | Chiave | Valore | Ciclo di vita |
|-------|--------|--------|---------------|
| `operatorToCustomerCalls` | `operatorUuid` | `customerUuid` | da `callCustomer` a `hangup` |

---

## Struttura modulo

```
api/
  handler/  AuthHandler.java       POST /api/cti/auth
            CallHandler.java       list, answer, hangup, sdkToken
  helper/   AuthHelper.java        verifica API key, emette JWT
            VoiceHelper.java       logica Vonage, stato in-memory
  dao/      CallDAO.java           insert, findAll, count, updateStatus
  dto/      CallDTO.java           record tabella chiamate
  Routes.java
gui/
  Call.js                          LitElement cti-call
  store.js                         callState atom (nanostores)
  index.js                         mount(container)
migration/  V20260319_100000__cti.sql   tabella chiamate
config/     application.properties     cti.api.key, cti.vonage.*
```
