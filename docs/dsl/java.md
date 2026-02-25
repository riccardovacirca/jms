# Java Coding Style DSL

```
version: 1.0
scope:   dev.hello.**, dev.jms.**
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
      rows     = db.select(...);
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
        nei metodi tipizzati c'è un solo return statement

  ok: |
    public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
    {
      if (condition) {
        res.status(200)...err(true)...send();
      } else {
        res.status(200)...err(false)...send();
      }
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
      res.status(200).contentType("application/json")
         .err(true).log("Credenziali mancanti").out(null).send();
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
      res.status(500).contentType("application/json")
         .err(true).log("Errore interno del server").out(null).send();
    }
```

```
RULE error.log-level
  applies-to: tutti i Log call

  | Situazione                          | Livello | Stack trace |
  |-------------------------------------|---------|-------------|
  | Eccezione di business (handler)     | WARN    | no          |
  | Errore di sistema (HandlerAdapter)  | ERROR   | si          |
```

---

## DOCUMENTAZIONE

```
RULE doc.javadoc
  applies-to: classi pubbliche, metodi pubblici, metodi package-private
  note: ogni classe pubblica e ogni metodo pubblico o package-private ha un commento Javadoc
        che descrive lo scopo; i metodi privati lo hanno se la logica non è auto-esplicativa;
        il commento descrive il "cosa" e il "perché", non il "come";
        per i metodi che restituiscono null si indica esplicitamente la condizione

  ok: |
    /**
     * Cerca l'utente per username includendo passwordHash ed email.
     * Usato nel flusso di login per la verifica delle credenziali.
     * Restituisce null se l'utente non esiste o è disabilitato.
     */
    public UserAuthDTO findForLogin(String username) throws Exception

    /** Inserisce un nuovo refresh token. */
    public void insert(String token, int userId, LocalDateTime expiresAt) throws Exception

  ko: |
    public UserAuthDTO findForLogin(String username) throws Exception   // nessun commento

    // commento in linea invece di Javadoc
    public void insert(String token, int userId, LocalDateTime expiresAt) throws Exception
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
    err:false → log è null,  out contiene il payload
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
       .err(false).log(null).out(out)
       .send();

  ko: |
    res.contentType("application/json").err(false).out(out).send();   // manca status()
```
