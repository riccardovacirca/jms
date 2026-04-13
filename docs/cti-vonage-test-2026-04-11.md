# CTI Vonage — Test session report
**Date:** 2026-04-11  
**Environment:** local dev + ngrok (answer/event webhooks), Vonage sandbox application  
**Operator number (from):** configured in `CTI_VONAGE_FROM_NUMBER`  
**Customer number (to):** `393200584988` (Italian mobile, E.164 without `+`)

---

## Summary

Two successful outbound calls completed end-to-end using the operator-first progressive dialer:

| # | Contatto ID | Duration | Result |
|---|-------------|----------|--------|
| 1 | 6 | ~38 seconds | Operator hangup, both legs completed |
| 2 | 7 | ~2m 50s | Operator hangup, both legs completed |

---

## Issues encountered before first successful call

### 1. ngrok not active
**Symptom:** Call appeared to start on the frontend (`callId` received), but `callHangup` fired ~1 second later. No answer webhook arrived at the backend.  
**Root cause:** ngrok was not running, so Vonage could not reach the local answer/event URLs.  
**Fix:** Start ngrok before initiating any call. Configure answer and event URLs in Vonage dashboard to point to the active ngrok tunnel:
- Answer URL: `https://<tunnel>/api/cti/vonage/answer`
- Event URL: `https://<tunnel>/api/cti/vonage/event`

**Side effect:** The contact (contatto 5) was removed from `jms_cti_operatore_contatti` by the frontend after `serverCall()` succeeded (the SDK call is made client-side and does not depend on the backend webhook). Contact had to be re-inserted manually before retrying.

---

### 2. Wrong number format
**Symptom:** Customer leg was created (`callCustomer` completed without error), but a `status=failed` event arrived for the customer UUID immediately after `status=started`.  
**Root cause:** Customer number was passed as `3713989250` — no country code prefix.  
**Fix:** Use E.164 format without the leading `+`: `393200584988` for an Italian mobile number (`+39 320 0584988`). The Vonage API requires full international format; the `+` prefix must be omitted.

---

### 3. Vonage default audio ("Rebecca" message)
**Symptom:** After the operator leg connected and was placed in the conversation, an automated voice played: *"Hi, this is Rebecca… voice… ario file…"*  
**Root cause:** `cti.vonage.music_on_hold_url` was set to an empty string in `application.properties`. The NCCO builder was unconditionally adding `musicOnHoldUrl: [""]` to the conversation action. Vonage treated the empty URL as a missing asset and played its own default audio.  
**Fix:** `buildOperatorNccoJson()` in `VoiceHelper` now only includes `musicOnHoldUrl` in the NCCO if the value is non-blank:
```java
if (musicOnHoldUrl != null && !musicOnHoldUrl.isBlank()) {
    musicList = new ArrayList<>();
    musicList.add(musicOnHoldUrl);
    conversation.put("musicOnHoldUrl", musicList);
}
```

---

## Successful call flow

### Call 1 — contatto 6 (18:34:05 → 18:34:43, ~38 s)

| Time | Event | Detail |
|------|-------|--------|
| 18:34:05 | `answer` webhook received | `from_user` = operator Vonage user ID, `uuid` = `dfd5c244` |
| 18:34:05 | NCCO sent to Vonage | Operator placed in conversation `call-<UUID>`, no music |
| 18:34:06 | `callCustomer` invoked | 1 s delay elapsed, REST call to Vonage API |
| 18:34:07 | `status=started` | Operator leg in conversation |
| 18:34:08 | `status=ringing` | Customer leg `81d74375` ringing |
| 18:34:15 | `status=answered` | Customer answered, both parties in conversation |
| 18:34:43 | Operator hangup (frontend) | `PUT /api/cti/vonage/call/{uuid}/hangup` |
| 18:34:43 | `status=completed` | Operator leg `dfd5c244` — duration recorded |
| 18:34:44 | `status=completed` | Customer leg `81d74375` — call fully terminated |

### Call 2 — contatto 7 (18:35:17 → 18:38:07, ~2m 50s)

| Time | Event | Detail |
|------|-------|--------|
| 18:35:17 | `answer` webhook received | `uuid` = `520924ee` |
| 18:35:17 | NCCO sent | Operator in conversation |
| 18:35:18 | `callCustomer` invoked | |
| 18:35:20 | `status=ringing` | Customer leg `3b3dd36f` ringing |
| 18:35:27 | `status=answered` | Customer answered |
| 18:38:07 | Operator hangup | ~2m 50s conversation |
| 18:38:08 | `status=completed` | Both legs terminated |

---

## Log observations

### High-frequency null events
Throughout both calls, the event webhook received a large number of events where both `uuid` and `status` were `null`. These are **RTC/WebRTC session events** emitted by the Vonage client SDK and do not carry voice call state. The backend correctly ignores them via the early-exit path in `processEvent()`. No action needed.

### Frontend ↔ backend synchronization
The frontend `callHangup` event (fired by the SDK when the operator leg terminates) arrived in sync with the `status=completed` event on the operator leg. The customer leg completion followed within 1 second in both tests.

### DB recording
Both calls are recorded in `jms_chiamate` with:
- `operatore_id`, `chiamante_account_id`, `contatto_id` populated from the answer webhook
- `stato` set to `completed` after the final event
- `durata_secondi` computed from answered → completed timestamps

---

## Known issues (open)

### Flyway checksum mismatch
`V20260319_100000__cti.sql` was modified in-place after the first DB migration. Flyway logs a warning at startup but continues. **Action required:** run `cmd db reset` to rebuild the schema from scratch.

### Contact not preserved in history
After a confirmed call, the contact is deleted from `jms_cti_operatore_contatti`. There is no historical record in CTI tables linking a contact back to the call. Call outcome (answered, not answered, etc.) is only available in `jms_chiamate` via `contatto_id` if the CRM contact ID was passed in `custom_data.contactId`.

### Orphaned contacts (no cleanup job yet)
If an operator disconnects without calling a contact, the contact remains assigned in `jms_cti_operatore_contatti` indefinitely. A cleanup job scoped to expired operator sessions is tracked in `TODO.md`.

### CRM → CTI queue integration
The CRM module does not yet write directly to `jms_cti_coda_contatti`. Queue population currently requires a manual API call or bulk import.
