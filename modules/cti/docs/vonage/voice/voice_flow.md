# Voice Module — Flusso Applicativo

## Cosa fa il modulo

Il modulo `voice` permette al CRM di fare chiamate telefoniche verso clienti
usando l'API Vonage. Ci sono due tipi di chiamata:

- **Test** — chiama un numero speciale e riproduce un messaggio vocale. Serve per
  verificare che tutto funzioni.
- **Reale** — chiama un cliente e lo collega direttamente a un operatore.

Ogni chiamata genera eventi di stato (es. "in ascolto", "risposta", "completata")
che vengono salvati nel database.

---

## Ambienti: Dev e Produzione

In sviluppo il modulo parla a un **mock server** (Prism). In produzione parla a
**Vonage**. Il codice è identico in entrambi i casi — cambia solo la configurazione.

```
  DEV                                    PRODUZIONE
  ───                                    ──────────
  baseUrl  → http://crm-prism:4010       baseUrl  → https://api.nexmo.com/v1/calls
  token    → mock-jwt-token              token    → (vuoto)
                                         privateKey → chiave RSA reale
                                                      └─► genera JWT automaticamente
```

Come funziona l'autenticazione:

```
  voice.token configurato?
       │
       ├── SÌ  ──► usa quel token (dev: "mock-jwt-token")
       │
       └── NO  ──► genera JWT RS256 dalla privateKey (produzione)
```

---

## Configurazione

```
voice.baseUrl       # URL dell'API (Prism o Vonage)
voice.token         # token Bearer diretto (solo dev)
voice.privateKey    # chiave RSA PEM per generare JWT (solo prod)
voice.applicationId # ID app Vonage (serve nel JWT)
voice.fromNumber    # numero Vonage mittente delle chiamate
voice.testNumber    # numero speciale per le chiamate di test
voice.eventUrl      # URL del nostro webhook per ricevere gli eventi
```

---

## Struttura del codice

```
Backend — src/main/java/dev/crm/module/voice/
  config/     VoiceConfig.java           # legge le proprietà voice.* dal properties
  controller/ VoiceController.java       # riceve le richieste HTTP
  service/    VoiceService.java          # logica: costruisce NCCO, chiama Vonage
  dao/        CallDao.java               # salva/legge chiamate nel DB
              CallEventDao.java         # salva/legge eventi nel DB
  dto/        CallDto.java               # oggetto "chiamata"
              CallEventDto.java         # oggetto "evento"
              CreateCallRequestDto.java  # richiesta dal frontend { toNumber, operatorType, operatorId }
              CreateCallResponseDto.java # risposta al frontend { uuid, status, ... }

Frontend — gui/src/module/voice/
  Layout.svelte              # entry point del modulo
  store.js                   # operatorUserId: userId Vonage dell'operatore corrente
  SdkSessionComponent.svelte # sessione Client SDK + gestione chiamate in ingresso
```

---

## Flusso 1 — Chiamata di Test

Scopo: verificare che la pipeline vocale funzioni.

```
  Frontend                     Backend                        Vonage / Prism
    │                            │                                  │
    │  POST /api/voice/test      │                                  │
    │───────────────────────────►│                                  │
    │                            │                                  │
    │                            │  costruisce NCCO:                │
    │                            │  [{ action: "talk",              │
    │                            │     text: "Benvenuto...",        │
    │                            │     language: "it-IT" }]         │
    │                            │                                  │
    │                            │  POST /v1/calls                  │
    │                            │  { to: testNumber,               │
    │                            │    from: fromNumber,             │
    │                            │    ncco: [...],                  │
    │                            │    event_url: [...] }            │
    │                            │─────────────────────────────────►│
    │                            │                                  │
    │                            │  { uuid, status: "started" }     │
    │                            │◄─────────────────────────────────│
    │                            │                                  │
    │                            │  salva in voice_calls            │
    │                            │                                  │
    │  { uuid, status, ... }     │                                  │
    │◄───────────────────────────│                                  │
```

Vonage chiama `testNumber` e riproduce il messaggio TTS.

---

## Flusso 2 — Chiamata Reale

Scopo: collegare un cliente a un operatore.

Il corpo della richiesta specifica il tipo di endpoint dell'operatore:

```
  operatorType = "phone"  →  endpoint: { type: "phone", number: operatorId }
  operatorType = "app"    →  endpoint: { type: "app",   user:   operatorId }
```

```
  Frontend                     Backend                        Vonage / Prism
    │                            │                                  │
    │  POST /api/voice/calls     │                                  │
    │  { toNumber: "cliente",    │                                  │
    │    operatorType: "phone",  │                                  │
    │    operatorId: "+39..." }  │                                  │
    │───────────────────────────►│                                  │
    │                            │                                  │
    │                            │  costruisce NCCO:                │
    │                            │  [{ action: "connect",           │
    │                            │     from: fromNumber,            │
    │                            │     endpoint: [{                 │
    │                            │       type: operatorType,        │
    │                            │       number|user: operatorId    │
    │                            │     }] }]                        │
    │                            │                                  │
    │                            │  POST /v1/calls                  │
    │                            │  { to: toNumber,                 │
    │                            │    from: fromNumber,             │
    │                            │    ncco: [...],                  │
    │                            │    event_url: [...] }            │
    │                            │─────────────────────────────────►│
    │                            │                                  │
    │                            │  { uuid, status: "started" }     │
    │                            │◄─────────────────────────────────│
    │                            │                                  │
    │                            │  salva in voice_calls            │
    │                            │                                  │
    │  { uuid, status, ... }     │                                  │
    │◄───────────────────────────│                                  │
```

Dopo la risposta del cliente, Vonage lo collega direttamente all'operatore:

```
  type = "phone":   Cliente ◄──── Vonage bridge ────► Operatore (telefono PSTN)
                              (connect)

  type = "app":     Cliente ◄──── Vonage bridge ────► Operatore (browser WebRTC)
                              (connect)                  └── Client SDK con SDK JWT
```

Se l'operatore non risponde entro 60 secondi (timeout di default) la chiamata termina.
Per gestire quel caso serve un fallback NCCO: vedi `docs/vonage/ncco.md` → *Fallback NCCO*.

---

## Flusso comune: submitCall

Entrambi i flussi (test e reale) convergono su un metodo unico:

```
  testCall()  ──► ncco: [talk]               ──┐
                                               ├──► submitCall(toNumber, ncco)
  createCall() ──► ncco: [connect → operator] ─┘          │
                                                          │  assembla payload
                                                          │  POST verso Vonage
                                                          │  salva in DB
                                                          ▼
                                                    risposta al frontend
```

---

## Flusso 3 — Eventi di stato (webhook)

Vonage chiama il nostro webhook ogni volta che lo stato della chiamata cambia.

```
  Vonage                       Backend                        Database
    │                            │                                │
    │  POST /api/voice/          │                                │
    │        webhook/event       │                                │
    │  { uuid, status, ... }     │                                │
    │───────────────────────────►│                                │
    │                            │                                │
    │                            │  cerca chiamata per uuid       │
    │                            │──────────────────────────────► │
    │                            │                                │
    │                            │  trovata?                      │
    │                            │  ├── SÌ  → aggiorna status     │
    │                            │  └── NO  → inserisce nuova     │
    │                            │            riga                │
    │                            │                                │
    │                            │  salva evento in voice_events  │
    │                            │──────────────────────────────► │
    │                            │                                │
    │  200 OK                    │                                │
    │◄───────────────────────────│                                │
```

Gli stati seguono questa sequenza:

```
  started ──► ringing ──► answered ──► completed
                │              │
                ▼              ▼
              busy           failed
              timeout        cancelled
              unanswered     rejected
                             machine
```

---

## Database

Due tabelle, relazione 1:N.

```
  voice_calls                          voice_events
  ───────────                          ────────────
  id (PK)  ◄────────────────────────── call_id (FK)
  uuid                                 id (PK)
  status                               uuid
  direction                            status
  from_number                          direction
  to_number                            timestamp
  event_url                            from_number
  created_at                           to_number
  ...                                  payload (JSON completo)
                                       created_at

  Una chiamata può avere più eventi:

  voice_calls: { uuid: "abc", status: "completed" }
       │
       └──► voice_events: { status: "started",   timestamp: T1 }
            voice_events: { status: "ringing",   timestamp: T2 }
            voice_events: { status: "answered",  timestamp: T3 }
            voice_events: { status: "completed", timestamp: T4 }
```

Migration: `V20260203_120002__module_voice.sql`

---

## Endpoint REST

```
  POST /api/voice/test              → chiama testNumber con TTS
  POST /api/voice/calls             → chiama cliente, collega operatore (phone o app)
  GET  /api/voice/calls             → lista tutte le chiamate (dal DB)
  GET  /api/voice/calls/{uuid}      → dettaglio singola chiamata
  GET  /api/voice/calls/{id}/events → eventi di una chiamata
  GET  /api/voice/sdk-token         → genera SDK JWT per il Client SDK nel browser
                                      ?userId=<user>
  POST /api/voice/webhook/event     → webhook ricevuto da Vonage
```

---

## NCCO — cosa sono

NCCO = **Nexmo Call Control Object**. È un array JSON che dice a Vonage
cosa fare durante una chiamata. Il modulo usa due azioni:

```
  talk ──► riproduce un messaggio vocale sintetico (TTS)
           usato in: chiamata di test

  connect ──► collega la chiamata a un endpoint
              usato in: chiamata reale
              endpoint types supportati:
                phone  → numero PSTN
                app    → utente Client SDK (WebRTC nel browser)
```

Riferimento completo: `docs/vonage/ncco.md`

---

## WebRTC nel browser (endpoint type "app")

Quando `operatorType` è `"app"`, Vonage instrada la chiamata verso un utente
che ha una sessione attiva tramite il Vonage Client SDK nel browser.

Cosa serve affinché funzioni:

```
  1. L'app Vonage (applicationId) deve avere due capability abilitate:
     → voice  (con answer_url, event_url, fallback_url)
     → rtc    (con event_url)
     Configurazione: vonage apps capabilities update <appId> voice ...
                     vonage apps capabilities update <appId> rtc ...
     Riferimento: docs/vonage/sdk/doc1.md

  2. Ogni operatore deve essere registrato come user:
     → vonage users create --name='operatore_01'
     Il campo name è quello che va nel claim "sub" del JWT SDK.
     Riferimento: docs/vonage/sdk/doc1.md, docs/vonage/sdk/doc6.md

  3. Il browser dell'operatore deve ottenere un SDK JWT e connettersi:

       Browser dell'operatore          Backend                Vonage
            │                            │                      │
            │  GET /api/voice/sdk-token  │                      │
            │  ?userId=operatore_01      │                      │
            │───────────────────────────►│                      │
            │                            │                      │
            │                            │  genera JWT RS256    │
            │                            │  sub = userId        │
            │                            │  acl = { paths }     │
            │                            │  exp = 1 ora         │
            │                            │                      │
            │  { token: "<jwt>" }        │                      │
            │◄───────────────────────────│                      │
            │                            │                      │
            │  client.createSession(jwt) │                      │
            │─────────────────────────────────────────────────► │
            │                            │                      │
            │  sessione WebRTC attiva    │                      │
            │◄──────────────────────────────────────────────────│
```

  4. Quando il CRM avvia una chiamata con operatorType="app" e operatorId="operatore_01",
     Vonage push la chiamata verso la sessione WebRTC di quell'utente.
     Il browser riceve l'evento "callInvite" e deve chiamare client.answer(callId).
     Riferimento: docs/vonage/client_sdk.txt
```

JWT API vs JWT SDK:

```
  JWT API  →  usato dal backend per chiamare Vonage
              claims: application_id, iat, exp (5 min), jti
              nessun sub, nessun acl

  JWT SDK  →  usato dal browser per il Client SDK
              claims: application_id, iat, exp (1 ora), jti, sub, acl
              sub  = userId (nome dell'utente registrato)
              acl  = { paths: { "/*/users/**":{}, "/*/conversations/**":{}, ... } }

  Entrambi firmati con la stessa chiave privata RSA dell'app.
  ACL paths di riferimento: docs/vonage/sdk/doc6.md
```

---

## Flusso webapp — operatore con Client SDK

Questa sezione descrive cosa succede nel **browser dell'operatore** quando
`operatorType` è `"app"`. Il browser deve mantenere una sessione Client SDK
attiva per ricevere le chiamate via WebRTC.

Dipendenza frontend: `@vonage/client-sdk` (package.json).

### Inizializzazione

```javascript
import { VonageClient } from '@vonage/client-sdk'

const client = new VonageClient()
```

### Sessione: ottenere JWT e connettersi

```
  Browser operatore            Backend                  Vonage
       │                         │                        │
       │  GET /api/voice/        │                        │
       │    sdk-token            │                        │
       │    ?userId=op_01       │                        │
       │────────────────────────►│                        │
       │                         │  genera JWT SDK        │
       │                         │  sub=op_01, acl, exp   │
       │  { token: "<jwt>" }     │                        │
       │◄────────────────────────│                        │
       │                         │                        │
       │  client.createSession(jwt)                       │
       │─────────────────────────────────────────────────►│
       │                                                  │
       │  sessione WebRTC attiva                          │
       │◄─────────────────────────────────────────────────│
```

### Chiamata in ingresso: callInvite

Quando il CRM avvia una chiamata con `operatorType: "app"` verso quell'utente,
Vonage invia l'evento nel browser:

```
  CRM (chi chiama)            Vonage              Browser operatore
       │                        │                       │
       │  POST /api/voice/calls │                       │
       │  { operatorType:"app", │                       │
       │    operatorId:"op_01" }│                       │
       │──────► Backend ───────►│                       │
       │                        │                       │
       │                        │  evento: callInvite   │
       │                        │  { callId, from }     │
       │                        │──────────────────────►│
       │                        │                       │
       │                        │  client.answer(callId)│  ← accetta
       │                        │  client.reject(callId)│  ← rifiuta
       │                        │◄──────────────────────│
       │                        │                       │
       │                        │  (se accettata)       │
       │  Cliente ◄──── Vonage bridge ─────────────────►│  ponte audio attivo
```

### Annullamento prima della risposta: callInviteCancel

Se la chiamata viene ritirata prima che l'operatore risponda, arriva
`callInviteCancel` invece di `callHangup`. Motivi possibili:

- timeout Vonage (60s, default del NCCO connect)
- il chiamatore ha annullato la chiamata
- la chiamata è stata accettata da otra sessione dello stesso utente

Il pannello della chiamata in ingresso viene nascosto.

### Fine della chiamata: callHangup

```
  Vonage                   Browser operatore
    │                            │
    │  evento: callHangup        │
    │  { callId }                │
    │───────────────────────────►│
    │                            │
    │  ponte audio chiuso        │
```

### Refresh della sessione

La sessione ha un TTL di **15 minuti** (ref: `docs/vonage/sdk/doc4.md`).
Prima della scadenza il browser deve rinnovarla:

```
  Browser operatore            Backend
       │                         │
       │  GET /api/voice/        │
       │    sdk-token?userId=X  │  ← richiede nuovo JWT
       │────────────────────────►│
       │  { token: "<jwt>" }     │
       │◄────────────────────────│
       │                         │
       │  client.refreshSession(newJwt)
       │──────────────────────────────► Vonage
       │                                  │
       │  sessione rinnovata              │
       │◄─────────────────────────────────│
```

### Riepilogo ciclo di vita

```
  createSession(jwt)
       │
       ▼
  sessione attiva (max 15 min)
       │
       ├──► callInvite ──► answer(callId) ──► ponte audio ──► callHangup
       │                   reject(callId)
       │                   callInviteCancel (timeout / cancel)
       │
       ├──► refreshSession(newJwt) ──► sessione rinnovata (altri 15 min)
       │
       └──► deleteSession ← componente rimosso dal DOM
```

---

## Dev: Prism mock server

In sviluppo Prism simula Vonage:

- Accetta le stesse richieste di Vonage.
- Risponde con dati di esempio conformi alla specifica.
- **Non chiama i webhook**: per testare gli eventi usare curl manuale verso
  `POST /api/voice/webhook/event`.
- Ignora il token Bearer (qualsiasi valore va bene).
- Per forzare risposte di errore: aggiungere `?__example=throttled` alla URL.
