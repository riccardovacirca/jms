# ADR: Migrazione da pendingCalls a voice2 (serverCall params)

## Contesto

Il modulo CTI implementa il pattern **operator-first progressive dialer**: l'operatore
entra in una Vonage Conversation con musica di attesa (`startOnEnter: false`) prima
che il backend chiami il cliente via PSTN.

Per avviare la chiamata, il backend ha bisogno di due informazioni al momento del
webhook `POST /api/cti/answer`:

1. **Chi è l'operatore** — per associare il webhook alla sessione corretta.
2. **Quale numero chiamare** — il numero del cliente da contattare.

Il campo `from_user` del webhook body è valorizzato da Vonage con il claim `sub`
del JWT SDK usato dall'operatore, ovvero l'`userId` passato a `generateSdkJwt()`.
Non esiste ambiguità: per una chiamata originata da `client.serverCall()` nel
Vonage Client SDK, `from_user` è il campo definitivo e unico per identificare
l'operatore.

Il numero del cliente non viene inviato da Vonage nel webhook — deve arrivare
tramite un canale separato.

---

## Architettura precedente (pendingCalls / voice3)

### Flusso

```
1. Frontend → POST /api/cti/call/prepare  { userId: ..., customerNumber: ... }
              Backend salva in ConcurrentHashMap: userId → customerNumber

2. Frontend → client.serverCall({})
              Vonage → POST /api/cti/answer

3. answer webhook:
   - legge from_user dal body (con fallback su to, user_id, from)
   - chiave userId → recupera customerNumber dalla mappa (rimozione atomica)
   - risponde con NCCO operatore (hold music)
   - dopo 1s chiama il cliente
```

### Perché era stata scelta

Il `customerNumber` non transitava mai attraverso l'infrastruttura Vonage: veniva
inviato al nostro backend autenticato (POST /prepare) e recuperato server-side nel
webhook. Il numero del cliente non appariva mai in query string né nei parametri
del Client SDK, rimanendo confinato all'interno del nostro sistema.

Questo è un vantaggio di privacy/compliance: in contesti dove i numeri di telefono
dei clienti sono dati sensibili (GDPR, logging di terze parti), questo approccio
evita che il numero transiti attraverso Vonage.

### Limiti

- **Single-instance**: la mappa in-memory funziona solo con una JVM. In un
  deployment multi-istanza (load balancer su più pod), prepare e answer potrebbero
  colpire istanze diverse e il numero non verrebbe trovato.
- **Round-trip extra**: richiede un HTTP call aggiuntivo (prepare) prima di
  avviare la sessione WebRTC.
- **Fallback chain fragile**: l'identificazione dell'operatore usava un fallback
  `from_user → to → user_id → from` di origine difensiva (reference voice3),
  non documentato da Vonage, che mascherava potenziali errori.

---

## Architettura attuale (voice2 / serverCall params)

### Flusso

```
1. Frontend → client.serverCall({ customerNumber: "+39..." })
              Vonage → POST /api/cti/answer?customerNumber=+39...

2. answer webhook:
   - legge from_user dal body (obbligatorio, errore se assente)
   - legge customerNumber dal query param (passato da Vonage dal serverCall)
   - risponde con NCCO operatore (hold music)
   - dopo 1s chiama il cliente
```

### Cosa cambia rispetto alla precedente

| Aspetto | Prima (pendingCalls) | Ora (voice2) |
|---------|---------------------|-------------|
| Trasmissione customerNumber | POST /prepare → mappa in-memory | Parametro serverCall → query param Vonage |
| Stato in-memory | `pendingCalls` ConcurrentHashMap | Nessuno |
| Round-trip aggiuntivo | Sì (prepare) | No |
| Identificazione operatore | from_user con fallback chain | from_user obbligatorio (errore se assente) |
| Multi-instance | No (mappa locale) | Sì (stateless) |
| Privacy customerNumber | Rimane nel nostro sistema | Transita attraverso Vonage |

### Trade-off accettato

Il numero del cliente appare nei parametri del Vonage Client SDK (codice JS del
browser) e nella query string del webhook answer (che può apparire nei log HTTP
del reverse proxy). Questo è accettabile se:

- Il deployment non ha requisiti di privacy stringenti sul numero del cliente.
- I log del reverse proxy non sono accessibili a terze parti non autorizzate.

---

## Come ripristinare l'architettura precedente

Se fosse necessario tornare alla versione con `pendingCalls`, le modifiche da
effettuare sono le seguenti.

### 1. `VoiceHelper.java` — aggiungere mappa e metodi

```java
// Aggiungere il campo
private final Map<String, String> pendingCalls = new ConcurrentHashMap<>();

// Aggiungere il metodo
public void preparePendingCall(String userId, String customerNumber)
{
  log.info("[CTI] preparePendingCall: userId={}, customerNumber={}", userId, customerNumber);
  pendingCalls.put(userId, customerNumber);
}

// Aggiungere il metodo
public String consumePendingCall(String userId)
{
  String customerNumber;
  customerNumber = pendingCalls.remove(userId);
  log.info("[CTI] consumePendingCall: userId={}, customerNumber={}", userId, customerNumber);
  return customerNumber;
}
```

### 2. `CallHandler.java` — ripristinare prepare() e il fallback chain in answer()

Ripristinare il metodo `prepare()` che legge `userId` e `customerNumber` dal body
e chiama `voiceHelper.preparePendingCall(userId, customerNumber)`.

In `answer()`, ripristinare il fallback chain per l'identificazione dell'operatore:

```java
fromUser    = DB.toString(body.get("from_user"));
toParam     = DB.toString(body.get("to"));
userIdParam = DB.toString(body.get("user_id"));
fromParam   = DB.toString(body.get("from"));

if (fromUser != null && !fromUser.isEmpty()) {
  userId = fromUser;
} else if (toParam != null && !toParam.isEmpty()) {
  userId = toParam;
} else if (userIdParam != null && !userIdParam.isEmpty()) {
  userId = userIdParam;
} else {
  userId = fromParam;
}

customerNumber = userId != null ? voiceHelper.consumePendingCall(userId) : null;
```

Rimuovere la lettura di `customerNumber` dal query param.

### 3. `Routes.java` — aggiungere la rotta prepare

```java
router.route(HttpMethod.POST, "/api/cti/call/prepare", calls::prepare);
```

### 4. `Call.js` — ripristinare il fetch prepare prima di serverCall

```javascript
prepareResponse = await fetch('/api/cti/call/prepare', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    userId: <operatorUserId>,
    customerNumber: this._customerNumber.trim()
  })
});
prepareData = await prepareResponse.json();
if (prepareData.err) {
  throw new Error(prepareData.log || 'Errore prepare-call');
}
callId = await this._client.serverCall({});
```

**Nota**: `userId` deve essere l'identificatore Vonage dell'operatore, lo stesso
usato per generare il JWT SDK (claim `sub`). Deve corrispondere esattamente al
valore che Vonage invia come `from_user` nel webhook answer.

---

## Riferimenti

- `docs/vonage/voice2_operator_first_pattern.md` — pattern operator-first con serverCall params
- `docs/vonage/voice_webhooks.md` — comportamento webhook answer URL
- `docs/vonage/voice/client_sdk/en/doc8.md` — serverCall params forwarded to answer_url
- `api/helper/VoiceHelper.java` — implementazione attuale
- `api/handler/CallHandler.java` — handler webhook answer
