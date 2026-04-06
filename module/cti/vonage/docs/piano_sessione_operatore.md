# Piano — Refactoring sessione operatore CTI

## Obiettivo

Separare due concetti attualmente confusi sotto il termine "sessione":

1. **Lock** — occupazione temporanea di un operatore Vonage da parte di un account applicativo durante una connessione WebRTC attiva. Dura 30 minuti, rinnovabile. È un meccanismo tecnico di mutua esclusione, non un'entità di business.

2. **Sessione operatore** — record di business che rappresenta un turno di lavoro assegnato a un operatore CTI. Creato dall'admin prima che l'operatore si connetta. Aggiornato dall'operatore al momento della connessione e disconnessione. Contiene statistiche aggregate del turno.

---

## 1. Rinomina lock in `jms_cti_operatori`

Le colonne `sessione_account_id` e `sessione_ttl` rappresentano un claim/lock tecnico, non una sessione di business. Si rinominano:

| Colonna attuale | Nuovo nome | Motivazione |
|---|---|---|
| `sessione_account_id` | `claim_account_id` | account che ha acquisito il lock sull'operatore |
| `sessione_ttl` | `claim_scadenza` | scadenza del lock, non TTL di sessione |

L'indice `jms_idx_cti_operatori_sessione` diventa `jms_idx_cti_operatori_claim`.

Impatto: tutte le query in `OperatorDAO` che referenziano queste colonne vanno aggiornate, così come i riferimenti nei metodi `claimOrRenew`, `releaseSession`, `releaseExpired`.

---

## 2. Nuova tabella `jms_sessione_operatore`

### Ciclo di vita del record

```
Admin crea turno → record inserito con stato 'pianificato'
                         ↓
Operatore si connette → connessione_inizio, stato 'attivo'
                         ↓
Operatore si disconnette (dentro il turno) → pausa: numero_pause++,
                                              durata_pause += elapsed,
                                              pausa_attiva = TRUE,
                                              pausa_inizio = NOW()
                         ↓
Operatore si riconnette → pausa_attiva = FALSE, pausa_inizio = NULL
                         ↓
Turno termina o operatore si disconnette fuori turno →
    connessione_fine, durata_totale, stato 'completato'
```

### Definizione proposta

```sql
CREATE TABLE jms_sessione_operatore (
    id                   BIGSERIAL    PRIMARY KEY,
    operatore_id         INTEGER      NOT NULL,  -- FK logica verso jms_cti_operatori(id)

    -- Turno pianificato (definito dall'admin)
    turno_inizio         TIMESTAMP    NOT NULL,
    turno_fine           TIMESTAMP    NOT NULL,

    -- Connessione effettiva
    connessione_inizio   TIMESTAMP,              -- prima connessione dell'operatore nel turno
    connessione_fine     TIMESTAMP,              -- ultima disconnessione (chiusura turno)
    durata_totale        INTEGER,                -- secondi: connessione_fine - connessione_inizio

    -- Ritardo/overtime rispetto al turno pianificato
    ritardo_inizio       INTEGER,                -- secondi positivi = in ritardo, negativi = in anticipo
    overtime_fine        INTEGER,                -- secondi oltre turno_fine (negativo = uscito prima)

    -- Pause (disconnessioni dentro l'orario del turno)
    numero_pause         INTEGER      NOT NULL DEFAULT 0,
    durata_pause         INTEGER      NOT NULL DEFAULT 0,  -- secondi totali di pausa
    pausa_attiva         BOOLEAN      NOT NULL DEFAULT FALSE,
    pausa_inizio         TIMESTAMP,              -- inizio della pausa corrente (NULL se non in pausa)

    -- Statistiche chiamate
    numero_chiamate      INTEGER      NOT NULL DEFAULT 0,
    durata_conversazione INTEGER      NOT NULL DEFAULT 0,  -- secondi totali in chiamata attiva

    -- Stato del record
    stato                VARCHAR(20)  NOT NULL DEFAULT 'pianificato',
                                                -- pianificato | attivo | in_pausa | completato | annullato

    -- Note admin
    note                 TEXT,

    -- Audit
    creato_da            INTEGER      NOT NULL,  -- account_id admin che ha creato il turno
    data_creazione       TIMESTAMP    NOT NULL DEFAULT NOW(),
    modificato_da        INTEGER,                -- account_id dell'ultimo modificatore (admin o sistema)
    data_modifica        TIMESTAMP
);
```

### Indici proposti

```sql
CREATE INDEX jms_idx_sessione_operatore_operatore ON jms_sessione_operatore(operatore_id);
CREATE INDEX jms_idx_sessione_operatore_turno     ON jms_sessione_operatore(turno_inizio, turno_fine);
CREATE INDEX jms_idx_sessione_operatore_stato     ON jms_sessione_operatore(stato);
```

---

## 3. Campi aggiuntivi suggeriti non presenti nella richiesta

### `ritardo_inizio` e `overtime_fine`

Calcolati alla chiusura del record:
- `ritardo_inizio = EXTRACT(EPOCH FROM (connessione_inizio - turno_inizio))` — positivo = in ritardo, negativo = in anticipo
- `overtime_fine = EXTRACT(EPOCH FROM (connessione_fine - turno_fine))` — positivo = ha lavorato oltre il turno, negativo = uscito prima

Utili per reportistica presenze e conformità turni.

### `pausa_attiva` + `pausa_inizio`

Permettono di sapere in tempo reale se l'operatore è in pausa e da quanto. Senza questi campi non sarebbe possibile calcolare la durata della pausa corrente senza una tabella separata degli eventi.

### `stato`

Indispensabile per distinguere:
- `pianificato` — turno creato dall'admin, operatore non ancora connesso
- `attivo` — operatore connesso
- `in_pausa` — operatore disconnesso dentro il turno
- `completato` — turno chiuso regolarmente
- `annullato` — turno cancellato dall'admin prima o durante

Senza `stato` l'unico modo per inferire lo stato è combinare più colonne nullable con logica complessa.

### `note`

Campo libero per annotazioni dell'admin sul turno (es. "sostituzione per assenza", "turno straordinario").

### Campi non inclusi e motivazione

| Campo | Motivo dell'esclusione |
|---|---|
| `numero_disconnessioni` | Ridondante: `numero_pause + 1` (la disconnessione finale non è una pausa) |
| `durata_media_chiamata` | Derivabile: `durata_conversazione / numero_chiamate`, meglio calcolato a query time |
| `pausa_dettaglio` (array) | Fuori scope: se serve il dettaglio delle singole pause va in una tabella separata `jms_pausa_operatore` |

---

## 4. Impatto sul codice Java

### `OperatorDAO`

- `claimOrRenew()`: sostituire `sessione_account_id` con `claim_account_id`, `sessione_ttl` con `claim_scadenza`
- `releaseSession()`: stessa sostituzione
- `releaseExpired()`: stessa sostituzione + aggiornare il nome dell'indice nei log

### Nuovi DAO/handler da creare

- `SessioneOperatoreDAO` — CRUD della tabella `jms_sessione_operatore`
- `SessioneOperatoreHandler` — endpoint API:
  - `POST /api/cti/vonage/admin/sessione` — crea turno (ADMIN)
  - `GET /api/cti/vonage/admin/sessione` — lista turni paginata (ADMIN)
  - `PUT /api/cti/vonage/admin/sessione/{id}` — aggiorna turno (ADMIN)
  - `DELETE /api/cti/vonage/admin/sessione/{id}` — annulla turno (ADMIN)
  - `GET /api/cti/vonage/sessione/corrente` — turno attivo dell'operatore corrente (USER)

### `CallHandler`

Il metodo `sdkToken` (connessione operatore) dovrà aggiornare il record di sessione attivo trovando il turno pianificato per l'operatore con `turno_inizio <= NOW() <= turno_fine` e stato `pianificato` o `in_pausa`:
- Se `pianificato`: impostare `connessione_inizio = NOW()`, `stato = attivo`
- Se `in_pausa`: calcolare durata pausa, aggiornare `durata_pause`, `pausa_attiva = FALSE`

Il metodo `releaseSession` (disconnessione) dovrà aggiornare il turno attivo:
- Se dentro il turno (`NOW() < turno_fine`): `stato = in_pausa`, `pausa_attiva = TRUE`, `pausa_inizio = NOW()`, `numero_pause++`
- Se fuori dal turno (`NOW() >= turno_fine`): `connessione_fine = NOW()`, calcolare `durata_totale`, `ritardo_inizio`, `overtime_fine`, `stato = completato`

### `VoiceHelper` / evento `completed`

Al completamento di ogni chiamata aggiornare il record di sessione attivo:
- `numero_chiamate++`
- `durata_conversazione += durata_chiamata`

---

## 5. Ordine di implementazione suggerito

1. Migrazioni SQL: rinomina colonne in `jms_cti_operatori`, crea `jms_sessione_operatore`
2. Aggiornamento `OperatorDAO` per le colonne rinominate
3. Implementazione `SessioneOperatoreDAO`
4. Implementazione `SessioneOperatoreHandler` + route
5. Integrazione in `CallHandler.sdkToken()` e `releaseSession()`
6. Integrazione in `VoiceHelper.processEvent()` per aggiornamento statistiche chiamate
7. Frontend: pannello admin gestione turni (fuori scope di questo piano)
