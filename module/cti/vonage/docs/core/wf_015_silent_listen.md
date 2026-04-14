# wf_015 — Ascolto Silenzioso (Silent Listen)

Un account con ruolo `ADMIN` o `ROOT` può entrare silenziosamente in una conversazione
attiva operatore-cliente per valutare l'operatore in tempo reale, senza che quest'ultimo
ne sia informato.

## Requisiti

- Solo ruolo `ADMIN` o `ROOT`.
- L'operatore non viene notificato.
- L'admin può terminare l'ascolto in autonomia senza chiudere la chiamata.
- Nessuna persistenza richiesta per l'attività di ascolto.

## Prerequisito tecnico: `conversation_name`

Il nome simbolico della conversazione Vonage (es. `"call-<uuid>"`) deve essere persistito
in `jms_cti_chiamate.conversation_name` per consentire all'admin di ri-entrare nella
conversazione già attiva tramite Client SDK.

Migration: `V20260415_100000__cti_conversation_name.sql`

```sql
ALTER TABLE jms_cti_chiamate
  ADD COLUMN IF NOT EXISTS conversation_name VARCHAR(100);
```

Viene popolato da `VoiceHelper.callCustomer()` al momento dell'inserimento del record
cliente nel DB.

## Flusso

```
Admin browser                    Backend                         Vonage
     │                               │                              │
     │ POST /api/cti/vonage          │                              │
     │   /sdk/auth/listen            │                              │
     │──────────────────────────────>│                              │
     │                               │ session.require(ADMIN, READ) │
     │                               │ vonageUserId = "admin-<sub>" │
     │                               │ generateSdkJwt(vonageUserId) │
     │<──────────────────────────────│                              │
     │  {"token": "<JWT RS256>"}     │                              │
     │                               │                              │
     │ [connessione WebRTC]          │                              │
     │ client.connect(token)         │                              │
     │──────────────────────────────────────────────────────────────>
     │                               │                              │
     │ [seleziona operatore target]  │                              │
     │ client.serverCall(            │                              │
     │   {listenTarget: opUuid})     │                              │
     │──────────────────────────────────────────────────────────────>
     │                               │                              │
     │                               │ POST /api/cti/vonage/answer  │
     │                               │<─────────────────────────────│
     │                               │ custom_data.listenTarget      │
     │                               │  presente → branch listen    │
     │                               │ findByUuid(listenTarget)     │
     │                               │ buildListenerNccoJson(       │
     │                               │   targetCall.conversationName│
     │                               │ )                            │
     │                               │──────────────────────────────>
     │                               │  NCCO: conversation          │
     │                               │  mute:true, endOnExit:false  │
     │                               │                              │
     │ [ascolto attivo]              │                              │
     │                               │                              │
     │ client.hangup()               │                              │
     │──────────────────────────────────────────────────────────────>
     │ [admin esce dalla conversaz.] │                              │
     │ [operatore e cliente continuano]                             │
```

## Endpoint

### `POST /api/cti/vonage/sdk/auth/listen`

Richiede ruolo `ADMIN` o `ROOT`. Genera un JWT SDK Vonage con `sub = "admin-<accountId>"`.

Il `vonageUserId` non esiste in `jms_cti_operatori`: il prefisso `"admin-"` impedisce
collisioni con gli operatori normali e consente all'answer webhook di discriminare
il tipo di chiamata senza consultare il DB.

Risposta:
```json
{ "err": false, "log": null, "out": { "token": "<JWT RS256>" } }
```

### `POST /api/cti/vonage/answer` — branch listen

Quando `custom_data.listenTarget` è presente, l'answer webhook entra nel branch di ascolto:

1. `callDao.findByUuid(listenTarget)` — recupera la chiamata target
2. `voiceHelper.buildListenerNccoJson(targetCall.conversationName())` — costruisce l'NCCO
3. Risponde immediatamente; non chiama `callCustomer`

Se la chiamata target non esiste o non ha `conversation_name`, risponde con `[]` (NCCO vuoto).

**Sicurezza**: il webhook non ha JWT, ma poiché solo un `ADMIN` poteva ottenere un token SDK
da `/sdk/auth/listen`, la presenza di `listenTarget` in `custom_data` è un'indicazione
sufficiente per fidarsi del branch.

## NCCO ascoltatore silenzioso

```json
[
  {
    "action": "conversation",
    "name": "call-<uuid>",
    "startOnEnter": false,
    "endOnExit": false,
    "mute": true
  }
]
```

- `mute: true` — l'admin non può essere sentito da nessuno nella conversazione
- `endOnExit: false` — l'uscita dell'admin non termina la conversazione
- `startOnEnter: false` — l'admin non avvia una nuova conversazione (quella è già attiva)

## Frontend (admin)

L'UI admin deve:

1. Ottenere il token: `POST /api/cti/vonage/sdk/auth/listen`
2. Connettersi al Client SDK: `client.connect(token)`
3. Selezionare l'operatore target dalla lista operatori attivi
4. Avviare l'ascolto: `client.serverCall({ listenTarget: operatoreUuid })`
   — dove `operatoreUuid` è lo `uuid` della chiamata attiva dell'operatore
5. Per terminare: `client.hangup()`

`operatoreUuid` si ottiene da `GET /api/cti/vonage/call/active` (se consultato
con privilegi admin) o da una query ad hoc sulle chiamate attive.

## Metodi coinvolti

| Classe | Metodo | Responsabilità |
|--------|--------|----------------|
| `CallHandler` | `sdkAuthListen` | Genera JWT SDK per admin, senza claim operatore |
| `CallHandler` | `answer` | Branch `listenTarget`: recupera `conversation_name` e risponde con NCCO silenzioso |
| `VoiceHelper` | `buildListenerNccoJson` | Costruisce NCCO `mute+endOnExit=false` |
| `CallDAO` | `findByUuid` | Recupera il record della chiamata target |
| `Routes` | `register` | `POST /api/cti/vonage/sdk/auth/listen` → `sdkAuthListen` (async) |

## Schema DB

Nessuna nuova tabella. Unica aggiunta:

```
jms_cti_chiamate
  conversation_name VARCHAR(100)   -- nome simbolico Vonage, es. "call-<uuid>"
```

Usato in sola lettura dall'answer webhook nel branch listen.
