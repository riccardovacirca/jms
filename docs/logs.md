# Logs

Il sistema di log dell'applicazione ha due livelli distinti:

- **Log applicativo** — messaggi di diagnostica scritti da `Log.java` via SLF4J + Logback
- **Audit log** — eventi di business scritti in tabella PostgreSQL da `AuditLog.java`

---

## Log applicativo

### Stack

SLF4J come API, Logback come implementazione. La configurazione è in `src/main/resources/logback.xml`, inclusa nel JAR.

### Destinazioni

I log vengono scritti simultaneamente su due appender:

| Appender | Destinazione | Formato timestamp |
|----------|-------------|-------------------|
| `CONSOLE` | stdout del container | `HH:mm:ss` |
| `FILE` | `/app/logs/service.log` | `yyyy-MM-dd HH:mm:ss` |

In sviluppo `/app/logs/` è bind-montato su `./logs/` nella cartella di progetto, quindi i log sono accessibili direttamente sul filesystem host.

### Rotazione

Il file viene ruotato giornalmente con compressione gzip. I file ruotati seguono il pattern `/app/logs/service.yyyy-MM-dd.log.gz`. Vengono conservati gli ultimi **30 giorni** di storico.

### Livello root

`INFO`. I livelli `DEBUG` vengono ignorati salvo modifica esplicita di `logback.xml`.

Logger silenziati esplicitamente (level `OFF`): `org.apache.poi`, `org.apache.xmlbeans` — producono output eccessivo durante la lettura di file Excel.

### Formato riga

```
# CONSOLE
HH:mm:ss [LEVEL] LoggerName - messaggio

# FILE
yyyy-MM-dd HH:mm:ss [LEVEL] LoggerName - messaggio
```

Il nome logger è troncato a 25 caratteri (`%logger{25}`).

### Utilizzo in codice

```java
private static final Log log = Log.get(MiaClasse.class);

log.info("Operazione completata");
log.info("Elaborati {} record in {}ms", count, elapsed);
log.warn("Configurazione assente, uso default");
log.warn("Tentativo fallito per utente {}", username, exception);
log.error("Errore imprevisto in {}", path, exception);
log.debug("Payload ricevuto: {}", body);
```

Il pattern `{}` è la sintassi SLF4J: gli argomenti vengono sostituiti nell'ordine. Se l'ultimo argomento è un `Throwable`, lo stack trace viene incluso automaticamente nel log (senza bisogno di un overload separato).

### Convenzioni di utilizzo

| Livello | Quando usarlo |
|---------|---------------|
| `INFO` | eventi normali rilevanti: avvio, migrazioni, connessioni |
| `WARN` | situazioni anomale ma recuperabili: config mancante, dipendenza non soddisfatta |
| `ERROR` | errori di sistema non attesi con stack trace — intercettati da `HandlerAdapter` per le eccezioni non gestite negli handler |
| `DEBUG` | diagnostica dettagliata, disabilitato in produzione |

Gli errori di **business** (validazione, autenticazione, not-found) non usano `Log` — vengono restituiti al client come HTTP 200 con `err: true` nel body JSON. Solo gli errori di sistema (eccezioni non gestite) producono un log ERROR con stack trace.

### Log di avvio

`App.java` usa direttamente `System.out.println` e `System.err.println` per i messaggi di bootstrap (prima che Logback sia attivo), con prefisso esplicito:

```
[info] Server in ascolto sulla porta 8080
[info] Flyway: 3 migrazione/i applicata/e
[warn] Modulo 'user': dipendenza non installata: audit
```

---

## Audit log

Registrazione strutturata di eventi di business in tabella PostgreSQL. Richiede il modulo `audit` installato (crea la tabella `audit_log`).

### Schema tabella

```sql
audit_log (
  id          BIGSERIAL PRIMARY KEY,
  event       TEXT NOT NULL,        -- identificatore evento (es. "user.login")
  user_id     INTEGER,              -- ID utente coinvolto (null se non autenticato)
  username    TEXT,                 -- username coinvolto
  ip_address  TEXT,                 -- IP client
  user_agent  TEXT,                 -- User-Agent header
  details     JSONB,                -- dati aggiuntivi evento-specifici
  created_at  TIMESTAMPTZ DEFAULT now()
)
```

### Utilizzo in codice

```java
// Con dettagli aggiuntivi
AuditLog.log(db, "user.login", userId, username,
             req.getClientIP(), req.getHeader("User-Agent"),
             Map.of("method", "password"));

// Senza dettagli
AuditLog.log(db, "user.logout", userId, username,
             req.getClientIP(), req.getHeader("User-Agent"));
```

Gli errori di scrittura vengono loggati con `Log.error` ma non propagati — un fallimento dell'audit log non interrompe il flusso della richiesta.

### Convenzione nomi evento

Gli eventi seguono la forma `<dominio>.<azione>` (es. `user.login`, `user.logout`, `account.password.change`). Il dominio corrisponde al nome del modulo.

---

## Accesso ai log in sviluppo

```bash
# Segui il log in tempo reale
tail -f logs/service.log

# Cerca errori
grep ERROR logs/service.log

# Log compressi dei giorni precedenti
zcat logs/service.2026-03-27.log.gz | grep WARN
```

In produzione i log sono dentro il container. Per accedervi:

```bash
docker logs <container>                        # stdout (CONSOLE appender)
docker exec <container> tail -f /app/logs/service.log  # file appender
```
