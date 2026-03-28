# Session

La classe `Session` gestisce due aspetti distinti della sessione HTTP:

1. **JWT** — validazione del token di accesso e accesso ai claims
2. **Storage server-side** — stato condiviso tra richieste, identificato da un cookie `session_id`

Viene istanziata automaticamente da `HandlerAdapter` per ogni richiesta e passata agli handler come terzo argomento:

```java
public void myHandler(HttpRequest req, HttpResponse res, Session session, DB db)
    throws Exception
{
  // ...
}
```

---

## JWT

La validazione del token JWT è **lazy**: avviene alla prima chiamata che richiede autenticazione e il risultato è in cache per tutta la durata della richiesta.

Il token viene letto dal cookie `access_token`. Se assente, non valido o revocato (blacklist), la sessione è considerata non autenticata.

### Verifica accesso

```java
session.require(Role.USER, Permission.READ);
```

Lancia `UnauthorizedException` intercettata da `HandlerAdapter` → HTTP 401. La matrice di accesso:

| Ruolo    | Livello | READ | WRITE |
|----------|---------|------|-------|
| `GUEST`  | 0       | pubblico | non valido |
| `USER`   | 1       | autenticato | autenticato |
| `ADMIN`  | 2       | admin o root | admin o root |
| `ROOT`   | 3       | root | root |

`GUEST + READ` è sempre permesso senza JWT. Tutti gli altri richiedono un JWT valido con `ruolo_level` sufficiente.

### Claims disponibili

```java
boolean auth  = session.isAuthenticated();   // true se JWT valido
long    id    = session.sub();               // account ID (claim sub)
String  user  = session.username();          // username
String  ruolo = session.ruolo();             // nome del ruolo (es. "admin")
int     level = session.ruoloLevel();        // livello numerico (0-3)
boolean mcp   = session.mustChangePassword(); // flag cambio password obbligatorio
Map<String, Object> all = session.claims();  // tutti i claims, mappa vuota se non autenticato
```

I metodi che accedono ai claims (tranne `isAuthenticated()` e `claims()`) lanciano `UnauthorizedException` se non autenticato.

---

## Storage server-side

Lo storage mantiene una `HashMap<String, Object>` per ogni sessione, identificata da un cookie `session_id` (64 caratteri hex, generato con `SecureRandom`).

La mappa è conservata in memoria nella JVM (un `ConcurrentHashMap` statico in `Session`). Non è persistita su database: al riavvio dell'applicazione le sessioni vengono perse.

### Ciclo di vita

- **Creazione** — la sessione viene creata automaticamente alla prima chiamata di `setAttr()`. `getAttr()` non crea la sessione.
- **Caricamento** — se il cookie `session_id` è presente nella request, la sessione viene caricata lazy al primo accesso allo storage.
- **Rinnovo TTL** — ad ogni response in cui la sessione è stata acceduta, il cookie viene riscritto con TTL aggiornato (sliding window). Questo avviene nel pre-send hook di `HttpResponse`, prima che gli header vengano committati.
- **Scadenza** — un thread daemon esegue il cleanup ogni 60 secondi, rimuovendo le sessioni inattive da più di `ttlSeconds` (default: 1800 secondi = 30 minuti).
- **Shutdown** — `Session.shutdown()` nel shutdown hook dell'applicazione ferma il thread di cleanup.

### API

```java
// Scrittura — crea la sessione se non esiste
session.setAttr("cart", myCart);

// Lettura — null se la sessione non esiste o la chiave è assente
Object cart = session.getAttr("cart");

// Rimozione chiave
session.removeAttr("cart");

// Svuota la mappa (non elimina la sessione)
session.clearAttrs();

// Restituisce l'ID della sessione corrente, null se non esiste
String sid = session.sessionId();
```

### Configurazione TTL

Da chiamare in `App.main()` prima dell'avvio del server:

```java
Session.configure(3600); // TTL 60 minuti
```

Il default è 1800 secondi (30 minuti).

---

## Flusso per request

```
Request entra in HandlerAdapter
  │
  ├─ Session istanziata (stato non caricato)
  ├─ Pre-send hook registrato su HttpResponse: () -> session.flush(res)
  │
  ├─ handler.handle(req, res, session, db)
  │     │
  │     ├─ [opzionale] session.require(...) → JWT validato lazy
  │     ├─ [opzionale] session.getAttr(...) → storage caricato lazy da cookie
  │     ├─ [opzionale] session.setAttr(...) → sessione creata se non esiste
  │     └─ res.send() / res.raw() / res.download()
  │           │
  │           └─ pre-send hook eseguito PRIMA degli header HTTP
  │                 └─ session.flush(res)
  │                       ├─ store.put(sessionId, attrs)
  │                       ├─ touched.put(sessionId, now)
  │                       └─ res.cookie("session_id", sessionId, ttl)
  │
  └─ Response inviata al client (Set-Cookie: session_id=... incluso)
```

Il hook garantisce che il cookie `session_id` sia scritto prima che Undertow committi gli header, indipendentemente da quando l'handler accede allo storage.

---

## Uso combinato JWT + storage

JWT e storage sono indipendenti. È possibile usarli insieme, ad esempio per associare dati di sessione a un utente autenticato:

```java
public void login(HttpRequest req, HttpResponse res, Session session, DB db)
    throws Exception
{
  // ... verifica credenziali ...

  // Imposta cookie JWT
  res.cookie(Cookie.ACCESS_TOKEN, accessToken, 900);

  // Imposta dati di sessione server-side
  session.setAttr("last_login", Instant.now().toString());

  res.status(200)
     .contentType("application/json")
     .err(false)
     .log(null)
     .out(null)
     .send();
}
```

---

## Limiti

- **In-memory**: le sessioni non sopravvivono al riavvio dell'applicazione. Non adatto per deployment multi-istanza senza sticky sessions.
- **Concorrenza**: due richieste concorrenti con lo stesso `session_id` accedono alla stessa `HashMap`, che non è thread-safe. In pratica i browser non fanno richieste concorrenti sulla stessa sessione, ma è un limite da tenere presente.
- **Dimensione**: non c'è limite alla dimensione degli oggetti memorizzati. Usare con cautela per oggetti grandi o liste lunghe.
