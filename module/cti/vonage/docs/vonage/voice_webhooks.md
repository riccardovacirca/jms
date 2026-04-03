# Voice Webhooks — Answer URL, Event URL, Fallback URL

Quando si abilita la capability `voice` su una Vonage Application, vanno configurati
tre endpoint webhook. Questo documento spiega cosa fanno e come sono implementati
nella nostra app.

---

## Configurazione Vonage

```bash
vonage apps capabilities update <APP_ID> voice \
  --voice-answer-url='https://example.com/api/voice/webhook/answer' \
  --voice-event-url='https://example.com/api/voice/webhook/event' \
  --voice-fallback-url='https://example.com/api/voice/webhook/fallback'
```

---

## 1. Answer URL

### Scopo

Quando Vonage riceve o avvia una chiamata, fa una richiesta GET/POST a questo URL
per ottenere l'**NCCO** (array JSON che controlla il flusso della chiamata).

### Quando viene chiamato

Una volta per chiamata, all'inizio.

### Cosa deve restituire

```json
[
  {
    "action": "talk",
    "text": "Welcome to the system",
    "language": "en-US"
  }
]
```

### Implementazione corrente: NON USATO

La nostra app **non implementa** l'Answer URL perché usa **NCCO inline**.

Quando il backend chiama `POST /v1/calls` (Vonage API), il payload include già
l'NCCO completo:

```java
// VoiceService.submitCall()
payload.put("ncco", ncco);  // NCCO inline, nessun answer_url
payload.put("event_url", List.of(config.getEventUrl()));

postJson(config.getBaseUrl(), payload, token);
```

**Vantaggio NCCO inline:**
- No round-trip HTTP verso answer_url
- NCCO costruito direttamente nel backend Java (più controllo, no webhook pubblico)
- Meno latenza

**Quando servirebbe Answer URL:**
- Chiamate **in ingresso** verso un numero Vonage acquistato
- NCCO dinamico basato su chi chiama (caller ID lookup)
- Routing complesso che richiede logica esterna

Nel nostro caso tutte le chiamate sono **outbound** (generate dal CRM verso clienti),
quindi l'NCCO inline è sufficiente.

---

## 2. Event URL

### Scopo

Vonage invia eventi di stato della chiamata a questo URL: `started`, `ringing`,
`answered`, `completed`, `failed`, ecc.

### Quando viene chiamato

Più volte durante il ciclo di vita della chiamata, in modo asincrono.

### Payload ricevuto

```json
{
  "uuid": "abc-def-ghi",
  "conversation_uuid": "conv-123",
  "status": "answered",
  "direction": "outbound",
  "from": "12345678901",
  "to": "+39YYYYYYYYYY",
  "timestamp": "2026-02-06T12:34:56.000Z"
}
```

### Implementazione corrente: IMPLEMENTATO

**Endpoint:** `POST /api/voice/webhook/event`
**Controller:** `VoiceController.handleWebhookEvent()`
**Service:** `VoiceService.handleWebhookEvent()`

```java
@PostMapping("/webhook/event")
public ResponseEntity<Map<String, Object>> handleWebhookEvent(
    @RequestBody Map<String, Object> payload) throws Exception
{
  voiceService.handleWebhookEvent(payload);
  // ...
}
```

**Cosa fa:**

1. Riceve il payload da Vonage
2. Cerca la chiamata per `uuid` nella tabella `voice_calls`
3. Se trovata: aggiorna lo `status`
4. Se non trovata: inserisce una nuova riga (chiamata in ingresso non prevista)
5. Salva l'evento completo nella tabella `voice_events`

**Riferimento:** `docs/voice_flow.md` → sezione "Flusso 3 — Eventi di stato"

**Stati gestiti:**

```
started → ringing → answered → completed
            │           │
            ▼           ▼
          busy        failed
          timeout     cancelled
          unanswered  rejected
                      machine
```

Tutti gli eventi vengono salvati nel database per audit e reportistica.

---

## 3. Fallback URL

### Scopo

URL alternativo chiamato quando l'Answer URL non è raggiungibile o fallisce.

### Quando viene chiamato

- Se l'Answer URL ritorna errore HTTP (5xx, timeout)
- Se l'Answer URL è irraggiungibile

### Cosa deve restituire

Un NCCO, esattamente come l'Answer URL.

### Implementazione corrente: NON IMPLEMENTATO

Non abbiamo configurato un Fallback URL perché:

1. Non usiamo Answer URL (NCCO inline)
2. Il nostro backend è single-source: se è down, non c'è un "fallback backend"

**Quando servirebbe:**

- Setup multi-region con failover geografico
- Chiamate in ingresso con Answer URL esterno che potrebbe fallire
- NCCO di emergenza (es. "Il sistema è temporaneamente non disponibile")

Per la produzione, si potrebbe implementare un Fallback URL che restituisce
un NCCO minimale:

```json
[
  {
    "action": "talk",
    "text": "Il servizio è momentaneamente non disponibile. Riprova più tardi.",
    "language": "it-IT"
  }
]
```

---

## Riepilogo implementazione corrente

| URL           | Stato            | Endpoint                       | Motivo                           |
|---------------|------------------|--------------------------------|----------------------------------|
| Answer URL    | Non implementato | —                              | Usiamo NCCO inline               |
| Event URL     | Implementato     | `/api/voice/webhook/event`     | Salva eventi nel DB              |
| Fallback URL  | Non implementato | —                              | Answer URL non usato             |

---

## Configurazione production

Per la produzione, la configurazione minima richiesta è:

```bash
vonage apps capabilities update <APP_ID> voice \
  --voice-event-url='https://crm.example.com/api/voice/webhook/event'
```

L'Answer URL può essere omesso se si usano solo chiamate outbound con NCCO inline.

Il Fallback URL è opzionale ma raccomandato per robustezza.

---

## Riferimenti

- NCCO format: `docs/vonage/ncco.md`
- Flusso chiamate: `docs/voice_flow.md`
- Backend implementation: `src/main/java/dev/crm/module/voice/`
- Vonage Voice API: `docs/vonage/api.json`
