# Java Coding Style DSL

```
version: 1.1
scope: dev.hello.**, dev.jms.**
```

---

## Formato delle regole

Ogni regola ha:
- `id` — identificatore univoco in forma `categoria.nome`
- `applies-to` — dove si applica la regola
- `ok` — esempio conforme
- `ko` — esempio non conforme

La conformità si verifica sui file `.java` nel `scope` dichiarato.

---

## FORMATTING

```
RULE fmt.indent
  applies-to: tutti i blocchi
  unit: 2 spazi per livello di indentazione (no tab)

  ok: |
    if (condition) {
      doSomething();
    }

  ko: |
    if (condition) {
        doSomething();
    }
```

```
RULE fmt.brace.class-method
  applies-to: dichiarazioni di classe, dichiarazioni di metodo
  note: la graffa di apertura sta su una riga propria

  ok: |
    public class Foo
    {
      ...
    }

    public void bar()
    {
      ...
    }

  ko: |
    public class Foo {
      ...
    }

    public void bar() {
      ...
    }
```

```
RULE fmt.brace.block
  applies-to: if, else, for, while, try, catch, finally, blocchi anonimi
  note: la graffa di apertura sta sulla stessa riga del costrutto

  ok: |
    if (condition) {
      ...
    } else {
      ...
    }

    for (int i = 0; i < n; i++) {
      ...
    }

  ko: |
    if (condition)
    {
      ...
    }
```

```
RULE fmt.brace.no-empty-line-after
  applies-to: graffa di apertura di classi e metodi
  note: la riga immediatamente successiva alla graffa non è mai vuota

  ok: |
    public class Foo
    {
      private static final Log log = Log.get(Foo.class);

    public void bar()
    {
      String x;

  ko: |
    public class Foo
    {

      private static final Log log = Log.get(Foo.class);

    public void bar()
    {

      String x;
```

```
RULE fmt.type-name-spacing
  applies-to: dichiarazioni di variabile, parametri di metodo
  note: esattamente uno spazio tra tipo e nome; nessuna spaziatura aggiuntiva per allineamento

  ok: |
    String username;
    int userId;
    ArrayList<HashMap<String, Object>> rows;

  ko: |
    String    username;
    int       userId;
    ArrayList<HashMap<String, Object>> rows;
```

```
RULE fmt.no-alignment-padding
  applies-to: ogni istruzione (assegnazioni, argomenti di metodo, inizializzazioni)
  note: è vietato aggiungere spazi extra per allineare verticalmente token sulla colonna;
        si usa esattamente uno spazio dove richiesto dalla sintassi

  ok: |
    username = (String) body.get("username");
    password = (String) body.get("password");
    rows = db.select(...);

    out.put("id", userId);
    out.put("username", uname);

  ko: |
    username = (String) body.get("username");
    password = (String) body.get("password");
    rows     = db.select(...);

    out.put("id",       userId);
    out.put("username", uname);
```

---

## VARIABILI

```
RULE var.declare-at-scope-top
  applies-to: corpo di metodi e blocchi
  note: tutte le variabili sono dichiarate in cima al proprio scope di visibilità,
        prima di qualsiasi istruzione eseguibile

  ok: |
    public void post(...)
    {
      String username;
      String password;
      ArrayList<HashMap<String, Object>> rows;

      username = (String) body.get("username");
      password = (String) body.get("password");
      rows = db.select(...);
    }

  ko: |
    public void post(...)
    {
      String username = (String) body.get("username");
      // ... altre istruzioni ...
      ArrayList<HashMap<String, Object>> rows = db.select(...);
    }
```

```
RULE var.separate-declaration-from-init
  applies-to: tutte le dichiarazioni di variabile
  note: la dichiarazione e l'inizializzazione sono su statement separati

  ok: |
    String username;
    username = (String) body.get("username");

  ko: |
    String username = (String) body.get("username");
```

```
RULE var.no-var
  applies-to: tutte le dichiarazioni di variabile
  note: il tipo deve essere sempre dichiarato esplicitamente; var è vietato

  ok: |
    ArrayList<HashMap<String, Object>> rows;
    rows = db.select(...);

  ko: |
    var rows = db.select(...);
```

---

## TIPI

```
RULE type.no-wildcard-generics
  applies-to: dichiarazioni di variabile, parametri, return type
  note: i tipi generici wildcard (?, ? extends, ? super) sono vietati;
        usare sempre il tipo parametrico concreto

  ok: |
    HashMap<String, Object> body;
    ArrayList<HashMap<String, Object>> rows;

  ko: |
    Map<?, ?> body;
    List<?> rows;
```

---

## FLUSSO DI CONTROLLO

```
RULE flow.single-exit
  applies-to: tutti i metodi
  note: ogni metodo ha un solo punto di uscita;
        nei metodi void si evitano return anticipati usando if-else;
        nei metodi tipizzati c'è un solo return statement, ottenuto con una variabile result

  ok: |
    // metodo void: if-else, nessun return anticipato
    public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
    {
      if (condition) {
        res.status(200)...err(true)...send();
      } else {
        res.status(200)...err(false)...send();
      }
    }

    // metodo tipizzato: variabile result, un solo return alla fine
    public UserDTO findById(long id) throws Exception
    {
      String sql;
      List<HashMap<String, Object>> rows;
      UserDTO result;

      sql = "SELECT * FROM users WHERE id = ?";
      rows = db.select(sql, id);
      if (rows.isEmpty()) {
        result = null;
      } else {
        result = toDTO(rows.get(0));
      }
      return result;
    }

  ko: |
    public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
    {
      if (condition) {
        res.status(200)...err(true)...send();
        return;
      }
      res.status(200)...err(false)...send();
    }

    public UserDTO findById(long id) throws Exception
    {
      ...
      if (rows.isEmpty()) {
        return null;
      }
      return toDTO(rows.get(0));
    }
```

---

## DATABASE

```
RULE db.sql-variable
  applies-to: ogni chiamata a db.select() e db.query()
  note: il testo SQL è sempre assegnato a una variabile locale chiamata sql
        prima di essere passato alla funzione; non si passa la stringa inline

  ok: |
    sql = "SELECT id, username FROM users WHERE username = ?";
    rows = db.select(sql, username);

    sql = "INSERT INTO refresh_tokens (token, user_id, expires_at) VALUES (?, ?, ?)";
    db.query(sql, token, userId, expiresAt);

  ko: |
    rows = db.select("SELECT id, username FROM users WHERE username = ?", username);

    db.query("INSERT INTO refresh_tokens (token, user_id, expires_at) VALUES (?, ?, ?)",
      token, userId, expiresAt);
```

```
RULE db.sql-multiline
  applies-to: stringhe SQL su più righe concatenate con +
  note: lo spazio separatore tra le clausole SQL appartiene alla fine del
        frammento corrente, non all'inizio del frammento successivo;
        ogni frammento deve terminare con uno spazio prima della virgoletta
        di chiusura

  ok: |
    sql = "SELECT id FROM cti_operatori "
        + "WHERE attivo = TRUE AND sessione_account_id IS NULL "
        + "ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED";

  ko: |
    sql = "SELECT id FROM cti_operatori"
        + " WHERE attivo = TRUE AND sessione_account_id IS NULL"
        + " ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED";
```

---

## LINGUA

```
RULE lang.exception-messages
  applies-to: messaggi di eccezione (Exception, IllegalStateException, ecc.)
  note: i messaggi di eccezione sono in inglese e brevi

  ok: |
    throw new IllegalStateException("cti.vonage.application_id is required");

  ko: |
    throw new IllegalStateException("cti.vonage.application_id non configurato");
```

```
RULE lang.comments
  applies-to: tutti i commenti inline (//)
  note: i commenti sono sempre in italiano

  ok: |
    // tipo di azione NCCO

  ko: |
    // NCCO action type
```

---

## GESTIONE ERRORI

```
RULE error.business-in-handler
  applies-to: implementazioni di Handler
  note: le eccezioni di business (validazione, autenticazione, risorsa assente)
        sono gestite nell'handler; restituiscono sempre HTTP 200 con err:true
        e un messaggio amichevole; si logga a livello WARN senza stack trace

  ok: |
    if (username == null || username.isBlank()) {
      log.warn("Login fallito: credenziali mancanti");
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Credenziali mancanti")
         .out(null)
         .send();
    }

  ko: |
    if (username == null || username.isBlank()) {
      throw new SomeException(400, "Credenziali mancanti");   // propaga fuori dall'handler
    }

  ko: |
    if (username == null || username.isBlank()) {
      res.status(400)...send();   // non 200
    }
```

```
RULE error.system-in-adapter
  applies-to: HandlerAdapter
  note: gli errori di sistema inattesi (DB irraggiungibile, NullPointerException, ecc.)
        non sono intercettati dall'handler; propagano a HandlerAdapter che logga
        a livello ERROR con stack trace completo e restituisce HTTP 500

  ok: |
    } catch (Exception e) {
      log.error("Errore di sistema in {}", handler.getClass().getSimpleName(), e);
      res.status(500)
         .contentType("application/json")
         .err(true)
         .log("Errore interno del server")
         .out(null)
         .send();
    }
```

```
RULE error.log-level
  applies-to: tutti i Log call

  | Situazione                          | Livello | Stack trace |
  |-------------------------------------|---------|-------------|
  | Eccezione di business (handler)     | WARN    | no          |
  | Errore di sistema (HandlerAdapter)  | ERROR   | sì          |
```

---

## DOCUMENTAZIONE

```
RULE doc.section-separator
  applies-to: sezioni logiche all'interno di una classe
  note: per separare sezioni logiche di una classe (es. campi statici, costruttori,
        metodi pubblici, metodi privati) si usa un blocco di tre righe commento:
        riga 1 e riga 3 sono "// =========================" (25 segni uguale),
        riga 2 è "// Titolo in italiano";
        il titolo è in italiano come tutti i commenti inline;
        questo costrutto è usato solo quando la classe è sufficientemente lunga
        da beneficiare di una struttura visiva esplicita

  ok: |
    // =========================
    // Metodi pubblici
    // =========================

  ko: |
    // ── Public methods ──────────────────────
```

```
RULE doc.inline-comment-position
  applies-to: commenti inline (//)
  note: i commenti inline vanno sulla riga immediatamente precedente all'istruzione commentata,
        mai sulla stessa riga (trailing comment)

  ok: |
    // assegna operatore e genera JWT SDK
    router.async(HttpMethod.POST, "/api/cti/vonage/sdk/auth", calls::sdkToken);

  ko: |
    router.async(HttpMethod.POST, "/api/cti/vonage/sdk/auth", calls::sdkToken); // assegna operatore e genera JWT SDK
```

```
RULE doc.javadoc
  applies-to: classi pubbliche, metodi pubblici, metodi package-private
  note: ogni classe pubblica e ogni metodo pubblico o package-private ha un commento Javadoc
        che descrive lo scopo; i metodi privati lo hanno se la logica non è auto-esplicativa;
        il commento descrive il "cosa" e il "perché", non il "come";
        @param è obbligatorio per ogni parametro;
        @return è obbligatorio per ogni metodo non-void;
        @throws è obbligatorio per ogni eccezione dichiarata nella firma;
        per i metodi che possono restituire null si indica esplicitamente la condizione
        nel testo descrittivo o nel @return

  ok: |
    /**
     * Cerca l'utente per username includendo passwordHash ed email.
     * Usato nel flusso di login per la verifica delle credenziali.
     *
     * @param username nome utente da cercare
     * @return DTO con dati di autenticazione, o {@code null} se non esiste o è disabilitato
     * @throws Exception se la query fallisce
     */
    public UserAuthDTO findForLogin(String username) throws Exception

    /**
     * Inserisce un nuovo refresh token.
     *
     * @param token     valore del token (hex 64 caratteri)
     * @param userId    id dell'account associato
     * @param expiresAt data di scadenza del token
     * @throws Exception se l'inserimento fallisce
     */
    public void insert(String token, int userId, LocalDateTime expiresAt) throws Exception

  ko: |
    public UserAuthDTO findForLogin(String username) throws Exception   // nessun Javadoc

    // commento inline invece di Javadoc
    public void insert(String token, int userId, LocalDateTime expiresAt) throws Exception

    // @param e @throws mancanti
    /**
     * Cerca l'utente per username.
     */
    public UserAuthDTO findForLogin(String username) throws Exception
```

---

## FORMATO RISPOSTA HTTP

```
RULE response.format
  applies-to: tutti gli endpoint /api/**
  note: il body di ogni risposta è sempre il seguente oggetto JSON

  format: |
    {
      "err": boolean,
      "log": string | null,
      "out": object | null
    }

  contract: |
    err:false → log è null, out contiene il payload
    err:true  → log contiene il messaggio, out è null
```

```
RULE response.status-on-exception
  applies-to: handler che gestiscono eccezioni di business
  note: le condizioni di errore gestite dall'handler restituiscono sempre HTTP 200;
        solo HandlerAdapter restituisce codici non-200 (500)

  ok: |
    res.status(200).err(true).log("Credenziali non valide").out(null).send();

  ko: |
    res.status(401).err(true).log("Credenziali non valide").out(null).send();
```

```
RULE response.builder-completeness
  applies-to: ogni chiamata a res.send()
  note: prima di send() devono essere invocati nell'ordine:
        status() → contentType() → [cookie()...] → err() → log() → out()
        tutti i metodi sono obbligatori; send() lancia IllegalStateException
        se uno di essi è mancante

  ok: |
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(out)
       .send();

  ko: |
    res.contentType("application/json").err(false).out(out).send();   // manca status()
```

```
RULE response.builder-chain
  applies-to: ogni chiamata a res.send()
  note: ogni metodo della catena è su una riga separata;
        le righe di continuazione sono indentate in modo che il punto
        sia allineato al punto di res. (ovvero: indentazione corrente + 3 spazi);
        il punto e virgola sta sulla riga di send()

  ok: |
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out("Hello, World!")
       .send();

  ko: |
    res.status(200).contentType("application/json").err(false).log(null).out(out).send();

  ko: |
    res.status(200)
      .contentType("application/json")
      .err(false)
      .log(null)
      .out(out)
      .send();
```
