# Piano di Implementazione: Architettura Ibrida Blocking/Non-Blocking

## Indice

1. [Panoramica](#1-panoramica)
2. [Analisi dello Stato Attuale](#2-analisi-dello-stato-attuale)
3. [Architettura Target Dettagliata](#3-architettura-target-dettagliata)
4. [Implementazione Fase per Fase](#4-implementazione-fase-per-fase)
5. [Esempi di Codice](#5-esempi-di-codice)
6. [Testing e Validazione](#6-testing-e-validazione)
7. [Monitoraggio e Tuning](#7-monitoraggio-e-tuning)
8. [Rollout e Migration Path](#8-rollout-e-migration-path)

---

## 1. Panoramica

### 1.1 Obiettivo

Trasformare l'attuale architettura completamente blocking in un sistema ibrido che:

- Mantiene compatibilità retroattiva al 100% con handler esistenti
- Introduce modalità async opzionale tramite annotation `@Async`
- Libera gli IO thread di Undertow per operazioni lente
- Mantiene lo stesso DB layer (JDBC + HikariCP)

### 1.2 Motivazioni

**Problemi attuali:**
- Ogni richiesta occupa un worker thread per tutta la durata (lettura body → query DB → risposta)
- Con query lente o body grandi, il thread pool si satura
- Scalabilità limitata dal numero di thread worker

**Vantaggi attesi:**
- IO thread sempre disponibili per nuove connessioni
- Migliore throughput su endpoint lenti
- Controllo granulare su quali handler necessitano async
- Nessun cambio di paradigma (no reactive streams, no async DB)

### 1.3 Non-Obiettivi

- Non implementare reactive DB (rimaniamo su JDBC blocking)
- Non riscrivere handler esistenti (backward compatibility)
- Non cambiare l'API pubblica di `Handler`, `HttpRequest`, `HttpResponse`

---

## 2. Analisi dello Stato Attuale

### 2.1 Flusso Attuale (Blocking)

```
Client → Undertow IO Thread
              ↓
        HandlerAdapter.handleRequest()
              ↓
        exchange.startBlocking()
              ↓
        dispatch(WORKER_THREAD)
              ↓
        [WORKER THREAD]
              ├─ req = new HttpRequest(exchange) → legge InputStream (BLOCKING)
              ├─ db = new DB(dataSource)
              ├─ db.open()               → prende connessione da pool (BLOCKING se pool saturo)
              ├─ handler.get/post/...()  → query JDBC (BLOCKING)
              ├─ db.close()              → restituisce connessione
              └─ res.send()              → scrive OutputStream (BLOCKING)
              ↓
        Fine exchange
```

**Problemi identificati:**
1. Worker thread bloccato durante lettura body (anche se 10MB)
2. Worker thread bloccato durante query lenta (anche se 5 secondi)
3. Worker thread bloccato durante scrittura risposta
4. Numero di richieste concorrenti = numero worker threads

### 2.2 Classi Coinvolte

| Classe | Percorso | Modifiche Necessarie |
|--------|----------|---------------------|
| `HandlerAdapter` | `dev.jms.util.HandlerAdapter` | ✅ Maggiori (detect @Async) |
| `HttpRequest` | `dev.jms.util.HttpRequest` | ✅ Medie (costruttore con byte[]) |
| `HttpResponse` | `dev.jms.util.HttpResponse` | ✅ Medie (send non-blocking) |
| `Handler` | `dev.jms.util.Handler` | ❌ Nessuna |
| `DB` | `dev.jms.util.DB` | ❌ Nessuna |

### 2.3 Configurazione Undertow Attuale

```java
// In App.java
Undertow server = Undertow.builder()
  .addHttpListener(port, "0.0.0.0")
  .setHandler(paths)
  .build();
```

**Default Undertow:**
- IO threads: `Math.max(Runtime.getRuntime().availableProcessors(), 2)`
- Worker threads: `ioThreads * 8` (default)

**Esempio su 4 CPU:**
- IO threads: 4
- Worker threads: 32

---

## 3. Architettura Target Dettagliata

### 3.1 Flusso Async Completo

```
Client → Undertow IO Thread
              ↓
        HandlerAdapter.handleRequest()
              ↓
        Rileva @Async sulla classe Handler
              ↓
        [ANCORA SU IO THREAD]
        exchange.getRequestReceiver().receiveFullBytes(callback)
              ↓
        [IO THREAD LIBERATO]
              ↓
        [Callback su IO thread con body completo]
              ↓
        exchange.dispatch(asyncExecutor, runnable)
              ↓
        [ASYNC EXECUTOR THREAD]
              ├─ req = new HttpRequest(exchange, bodyBytes)
              ├─ db = new DB(dataSource)
              ├─ db.open()              → BLOCKING ma su thread dedicato
              ├─ handler.get/post/...() → BLOCKING ma su thread dedicato
              ├─ db.close()
              └─ res.send()             → getResponseSender().send() NON-BLOCKING
              ↓
        Fine exchange
```

### 3.2 Componenti Nuovi

#### 3.2.1 Annotation `@Async`

```java
package dev.jms.util;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un Handler per essere eseguito in modalità non-blocking.
 * 
 * Gli handler annotati con @Async:
 * - Non bloccano l'IO thread durante la lettura del body
 * - Vengono eseguiti su un executor dedicato
 * - Scrivono la risposta in modo non-blocking
 * 
 * Usare solo per handler con operazioni lente (query pesanti, elaborazioni lunghe).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Async {
}
```

#### 3.2.2 AsyncExecutor

```java
package dev.jms.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Executor dedicato per handler @Async.
 * Dimensionato in base al pool HikariCP e alle operazioni lente attese.
 */
public class AsyncExecutor {
  private static ExecutorService executor;
  private static int poolSize;
  
  /**
   * Inizializza l'executor con la dimensione specificata.
   * 
   * Linee guida dimensionamento:
   * - Minimo: numero di CPU
   * - Raccomandato: max pool size HikariCP (es. 10-20)
   * - Massimo: 2x max pool size HikariCP
   * 
   * @param size Dimensione del pool
   */
  public static void init(int size) {
    if (executor != null) {
      throw new IllegalStateException("AsyncExecutor già inizializzato");
    }
    poolSize = size;
    executor = Executors.newFixedThreadPool(
      size,
      r -> {
        Thread t = new Thread(r);
        t.setName("async-handler-" + t.getId());
        t.setDaemon(false); // Non daemon per permettere graceful shutdown
        return t;
      }
    );
  }
  
  /**
   * Restituisce l'executor (per uso interno HandlerAdapter).
   */
  public static ExecutorService getExecutor() {
    if (executor == null) {
      throw new IllegalStateException("AsyncExecutor non inizializzato");
    }
    return executor;
  }
  
  /**
   * Shutdown graceful dell'executor.
   * Da chiamare in fase di shutdown applicazione.
   */
  public static void shutdown() {
    if (executor != null) {
      executor.shutdown();
      try {
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
          executor.shutdownNow();
        }
      } catch (InterruptedException e) {
        executor.shutdownNow();
        Thread.currentThread().interrupt();
      }
    }
  }
  
  /**
   * Statistiche per monitoraggio.
   */
  public static Stats getStats() {
    if (executor instanceof ThreadPoolExecutor) {
      ThreadPoolExecutor tpe = (ThreadPoolExecutor) executor;
      return new Stats(
        poolSize,
        tpe.getActiveCount(),
        tpe.getQueue().size(),
        tpe.getCompletedTaskCount()
      );
    }
    return new Stats(poolSize, 0, 0, 0);
  }
  
  public static class Stats {
    public final int poolSize;
    public final int activeThreads;
    public final int queuedTasks;
    public final long completedTasks;
    
    Stats(int poolSize, int activeThreads, int queuedTasks, long completedTasks) {
      this.poolSize = poolSize;
      this.activeThreads = activeThreads;
      this.queuedTasks = queuedTasks;
      this.completedTasks = completedTasks;
    }
  }
}
```

### 3.3 Configurazione in `App.java`

```java
public class App {
  public static void main(String[] args) {
    Config config;
    int port;
    int asyncPoolSize;
    
    config = new Config();
    port = config.getInt("server.port", 8080);
    
    // Inizializza DB
    DB.init(config);
    
    // Inizializza AsyncExecutor (dimensione basata su pool HikariCP)
    asyncPoolSize = config.getInt("async.pool.size", 20);
    AsyncExecutor.init(asyncPoolSize);
    
    // ... resto invariato
    
    // Shutdown hook
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      AsyncExecutor.shutdown();
      System.out.println("[info] AsyncExecutor terminato");
    }));
  }
}
```

**Nuove proprietà in `application.properties`:**
```properties
# Dimensione pool AsyncExecutor (default 20)
async.pool.size=20

# Massima dimensione body per handler async (default 10MB)
async.max.body.size=10485760
```

---

## 4. Implementazione Fase per Fase

### Fase 1: Annotation e AsyncExecutor

**Obiettivo:** Introdurre annotation e executor senza toccare logica esistente.

**Task:**
1. ✅ Creare `dev.jms.util.Async.java`
2. ✅ Creare `dev.jms.util.AsyncExecutor.java`
3. ✅ Aggiornare `App.java` con `AsyncExecutor.init()`
4. ✅ Aggiungere proprietà `async.pool.size` in `application.properties`
5. ✅ Test: verificare che applicazione parta senza errori

**Validazione:**
```bash
# Applicazione deve partire normalmente
cmd app run

# Log atteso:
# [info] AsyncExecutor inizializzato con pool size 20
```

---

### Fase 2: Refactor HandlerAdapter

**Obiettivo:** Rilevare `@Async` e instradare su executor dedicato.

**Modifiche a `HandlerAdapter.java`:**

```java
package dev.jms.util;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.BlockingHandler;
import javax.sql.DataSource;
import java.nio.ByteBuffer;

public class HandlerAdapter implements HttpHandler {
  private final Handler handler;
  private final DataSource dataSource;
  private final boolean isAsync;
  private final int maxBodySize;
  
  public HandlerAdapter(Handler handler, DataSource dataSource) {
    this.handler = handler;
    this.dataSource = dataSource;
    this.isAsync = handler.getClass().isAnnotationPresent(Async.class);
    this.maxBodySize = Config.get("async.max.body.size", 10 * 1024 * 1024); // 10MB default
  }
  
  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception {
    if (isAsync) {
      handleAsyncRequest(exchange);
    } else {
      handleBlockingRequest(exchange);
    }
  }
  
  /**
   * Modalità blocking (comportamento attuale, invariato).
   */
  private void handleBlockingRequest(HttpServerExchange exchange) throws Exception {
    if (exchange.isInIoThread()) {
      exchange.startBlocking();
      exchange.dispatch(this);
      return;
    }
    
    HttpRequest req = new HttpRequest(exchange);
    HttpResponse res = new HttpResponse(exchange);
    DB db = dataSource != null ? new DB(dataSource) : null;
    
    try {
      if (db != null) {
        db.open();
      }
      
      String method = exchange.getRequestMethod().toString();
      switch (method) {
        case "GET":    handler.get(req, res, db); break;
        case "POST":   handler.post(req, res, db); break;
        case "PUT":    handler.put(req, res, db); break;
        case "DELETE": handler.delete(req, res, db); break;
        default:        res.status(405).send();
      }
    } catch (Exception e) {
      if (!exchange.isResponseStarted()) {
        res.status(500).contentType("application/json")
           .err(true).log(e.getMessage()).out(null).send();
      }
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }
  
  /**
   * Modalità async: lettura non-blocking → dispatch executor → risposta non-blocking.
   */
  private void handleAsyncRequest(HttpServerExchange exchange) {
    // Lettura body non-blocking (su IO thread)
    exchange.getRequestReceiver().receiveFullBytes(
      (ex, bodyBytes) -> {
        // Body letto: ora dispatch su executor dedicato
        ex.dispatch(AsyncExecutor.getExecutor(), () -> {
          executeAsyncHandler(ex, bodyBytes);
        });
      },
      (ex, error) -> {
        // Errore lettura body
        sendErrorResponse(ex, 400, "Errore lettura body: " + error.getMessage());
      }
    );
  }
  
  /**
   * Esegue handler async su thread dedicato.
   * Questo metodo gira su AsyncExecutor thread (NON su IO thread).
   */
  private void executeAsyncHandler(HttpServerExchange exchange, byte[] bodyBytes) {
    HttpRequest req = new HttpRequest(exchange, bodyBytes);
    HttpResponse res = new HttpResponse(exchange);
    DB db = dataSource != null ? new DB(dataSource) : null;
    
    try {
      if (db != null) {
        db.open(); // BLOCKING ma su thread executor dedicato
      }
      
      String method = exchange.getRequestMethod().toString();
      switch (method) {
        case "GET":    handler.get(req, res, db); break;
        case "POST":   handler.post(req, res, db); break;
        case "PUT":    handler.put(req, res, db); break;
        case "DELETE": handler.delete(req, res, db); break;
        default:        res.status(405).send();
      }
    } catch (Exception e) {
      if (!exchange.isResponseStarted()) {
        sendErrorResponse(exchange, 500, "Errore interno: " + e.getMessage());
      }
    } finally {
      if (db != null) {
        db.close();
      }
    }
  }
  
  /**
   * Invia risposta errore in modo non-blocking.
   */
  private void sendErrorResponse(HttpServerExchange exchange, int status, String message) {
    exchange.setStatusCode(status);
    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
    String payload = String.format(
      "{\"err\":true,\"log\":\"%s\",\"out\":null}",
      message.replace("\"", "\\\"")
    );
    exchange.getResponseSender().send(payload);
  }
}
```

**Punti chiave:**
- `isAsync` rilevato a costruzione via reflection
- Handler async: `receiveFullBytes()` → `dispatch(executor)` → `executeAsyncHandler()`
- Handler blocking: comportamento invariato

**Validazione:**
```bash
# Test con handler blocking esistente (nessun @Async)
curl http://localhost:8080/api/home/hello
# Deve funzionare come prima

# Test con handler async (da creare in fase 5)
```

---

### Fase 3: Aggiornamento HttpRequest

**Obiettivo:** Supportare costruttore con `byte[]` per handler async.

**Modifiche a `HttpRequest.java`:**

```java
package dev.jms.util;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderMap;
import io.undertow.util.Headers;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class HttpRequest {
  private final HttpServerExchange exchange;
  private final byte[] bodyBytes; // Null se blocking mode
  private String bodyString; // Cache
  
  /**
   * Costruttore per modalità blocking (esistente, invariato).
   */
  public HttpRequest(HttpServerExchange exchange) {
    this.exchange = exchange;
    this.bodyBytes = null;
  }
  
  /**
   * Costruttore per modalità async.
   * @param exchange Exchange Undertow
   * @param bodyBytes Body già letto in modo non-blocking
   */
  public HttpRequest(HttpServerExchange exchange, byte[] bodyBytes) {
    this.exchange = exchange;
    this.bodyBytes = bodyBytes;
  }
  
  /**
   * Restituisce il body come stringa.
   * - Modalità blocking: legge da InputStream (BLOCKING)
   * - Modalità async: converte byte[] già letto (NON-BLOCKING)
   */
  public String getBody() {
    if (bodyString != null) {
      return bodyString; // Cache
    }
    
    if (bodyBytes != null) {
      // Async mode: body già disponibile
      bodyString = new String(bodyBytes, StandardCharsets.UTF_8);
      return bodyString;
    }
    
    // Blocking mode: legge da InputStream (comportamento attuale)
    try (InputStream is = exchange.getInputStream()) {
      byte[] bytes = is.readAllBytes();
      bodyString = new String(bytes, StandardCharsets.UTF_8);
      return bodyString;
    } catch (Exception e) {
      return "";
    }
  }
  
  /**
   * Restituisce il body come byte[].
   * Utile per upload binari.
   */
  public byte[] getBodyBytes() {
    if (bodyBytes != null) {
      return bodyBytes; // Async mode
    }
    
    // Blocking mode
    try (InputStream is = exchange.getInputStream()) {
      return is.readAllBytes();
    } catch (Exception e) {
      return new byte[0];
    }
  }
  
  // ... resto dei metodi invariato (getHeader, getParam, etc.)
}
```

**Validazione:**
```java
// Test blocking
HttpRequest req = new HttpRequest(exchange);
String body = req.getBody(); // Legge da InputStream

// Test async
byte[] bodyBytes = ...; // Letto da receiveFullBytes()
HttpRequest req = new HttpRequest(exchange, bodyBytes);
String body = req.getBody(); // Converte byte[] già disponibile
```

---

### Fase 4: Aggiornamento HttpResponse

**Obiettivo:** Supportare `send()` non-blocking per handler async.

**Modifiche a `HttpResponse.java`:**

```java
package dev.jms.util;

import dev.jms.util.json.Json;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import java.io.OutputStream;

public class HttpResponse {
  private final HttpServerExchange exchange;
  private boolean isAsync;
  private int statusCode;
  private String contentType;
  private Boolean err;
  private String log;
  private Object out;
  
  public HttpResponse(HttpServerExchange exchange) {
    this.exchange = exchange;
    this.isAsync = !exchange.isBlocking(); // Rileva se async
  }
  
  public HttpResponse status(int code) {
    this.statusCode = code;
    return this;
  }
  
  public HttpResponse contentType(String type) {
    this.contentType = type;
    return this;
  }
  
  public HttpResponse err(boolean value) {
    this.err = value;
    return this;
  }
  
  public HttpResponse log(String message) {
    this.log = message;
    return this;
  }
  
  public HttpResponse out(Object data) {
    this.out = data;
    return this;
  }
  
  /**
   * Invia la risposta.
   * - Modalità blocking: scrive su OutputStream (BLOCKING)
   * - Modalità async: usa ResponseSender (NON-BLOCKING)
   */
  public void send() throws Exception {
    if (exchange.isResponseStarted()) {
      throw new IllegalStateException("Risposta già inviata");
    }
    
    // Validazione
    if (statusCode == 0) {
      throw new IllegalStateException("status() non chiamato");
    }
    if (contentType == null) {
      throw new IllegalStateException("contentType() non chiamato");
    }
    
    // Costruisce payload JSON se err/log/out sono impostati
    String payload;
    if (err != null) {
      payload = Json.toJson(Map.of(
        "err", err,
        "log", log,
        "out", out
      ));
    } else {
      payload = "";
    }
    
    // Imposta header
    exchange.setStatusCode(statusCode);
    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType);
    
    if (isAsync) {
      // Async mode: ResponseSender (NON-BLOCKING)
      exchange.getResponseSender().send(payload);
    } else {
      // Blocking mode: OutputStream (BLOCKING)
      try (OutputStream os = exchange.getOutputStream()) {
        os.write(payload.getBytes(StandardCharsets.UTF_8));
        os.flush();
      }
    }
  }
  
  /**
   * Invia payload custom (string).
   */
  public void send(String payload) throws Exception {
    if (exchange.isResponseStarted()) {
      throw new IllegalStateException("Risposta già inviata");
    }
    
    if (statusCode == 0) statusCode = 200;
    if (contentType == null) contentType = "text/plain";
    
    exchange.setStatusCode(statusCode);
    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, contentType);
    
    if (isAsync) {
      exchange.getResponseSender().send(payload);
    } else {
      try (OutputStream os = exchange.getOutputStream()) {
        os.write(payload.getBytes(StandardCharsets.UTF_8));
        os.flush();
      }
    }
  }
}
```

**Punti chiave:**
- `isAsync` rilevato automaticamente (`!exchange.isBlocking()`)
- `send()` usa `ResponseSender` se async, `OutputStream` se blocking
- Nessun cambio API pubblica

**Validazione:**
```java
// Handler blocking
res.status(200).contentType("application/json")
   .err(false).log(null).out(data).send(); // Usa OutputStream

// Handler async
res.status(200).contentType("application/json")
   .err(false).log(null).out(data).send(); // Usa ResponseSender
```

---

### Fase 5: Test con Handler Async di Esempio

**Obiettivo:** Creare handler async di test per validare implementazione.

**Creare `SlowQueryHandler.java` (handler async test):**

```java
package com.example.test.handler;

import dev.jms.util.Async;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.DB;
import java.util.List;
import java.util.Map;

@Async
public class SlowQueryHandler implements Handler {
  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception {
    // Simula query lenta (1 secondo)
    List<Map<String, Object>> results = db.select(
      "SELECT pg_sleep(1), 'slow query' as message",
      List.of()
    );
    
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(results.get(0))
       .send();
  }
}
```

**Registrare in `App.java`:**

```java
paths = new PathTemplateHandler(staticHandler)
  // ... altre route
  .add("/api/test/slow", route(new SlowQueryHandler(), ds));
```

**Test di carico:**

```bash
# Test 100 richieste concorrenti su handler async
ab -n 100 -c 10 http://localhost:8080/api/test/slow

# Aspettative:
# - IO thread sempre liberi
# - Throughput limitato da AsyncExecutor pool size (es. 20)
# - Latenza ~1 secondo per richiesta
```

**Confronto con handler blocking equivalente:**

```java
// SENZA @Async
public class SlowQueryBlockingHandler implements Handler {
  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception {
    // Stesso codice
    List<Map<String, Object>> results = db.select(
      "SELECT pg_sleep(1), 'slow query' as message",
      List.of()
    );
    
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(results.get(0))
       .send();
  }
}
```

**Risultati attesi:**

| Modalità | Concorrenza | Throughput | Latenza Media | Note |
|----------|-------------|------------|---------------|------|
| Blocking | 10 | ~10 req/s | 1s | Limitato da worker pool |
| Async | 10 | ~10 req/s | 1s | Identico ma IO thread liberi |
| Blocking | 50 | ~32 req/s | 1.5s | Saturazione worker pool |
| Async | 50 | ~20 req/s | 2.5s | Limitato da AsyncExecutor size |

---

## 5. Esempi di Codice

### 5.1 Handler Blocking (Invariato)

```java
package com.example.home.handler;

import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.DB;

// NESSUNA annotation @Async → comportamento attuale
public class HelloHandler implements Handler {
  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception {
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out("Hello, World!")
       .send();
  }
}
```

### 5.2 Handler Async (Nuovo)

```java
package com.example.reports.handler;

import dev.jms.util.Async;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.DB;
import java.util.List;
import java.util.Map;

@Async // <-- Abilita modalità async
public class HeavyReportHandler implements Handler {
  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception {
    // Query pesante (5-10 secondi)
    List<Map<String, Object>> report = db.select("""
      SELECT 
        date_trunc('day', created_at) as day,
        count(*) as total,
        avg(amount) as avg_amount
      FROM transactions
      WHERE created_at > now() - interval '1 year'
      GROUP BY day
      ORDER BY day
    """, List.of());
    
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(report)
       .send();
  }
}
```

### 5.3 Handler Async con Upload File

```java
package com.example.upload.handler;

import dev.jms.util.Async;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.DB;

@Async
public class FileUploadHandler implements Handler {
  @Override
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception {
    // Body già letto in modo non-blocking (fino a 10MB)
    byte[] fileBytes = req.getBodyBytes();
    String fileName = req.getHeader("X-File-Name");
    
    // Salva su DB (BLOCKING ma su executor dedicato)
    db.query("""
      INSERT INTO files (name, data, size)
      VALUES (?, ?, ?)
    """, List.of(fileName, fileBytes, fileBytes.length));
    
    res.status(201)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(Map.of("uploaded", fileName, "size", fileBytes.length))
       .send();
  }
}
```

### 5.4 Mix Blocking/Async nello Stesso Modulo

```java
// Handler veloce → blocking
public class UserListHandler implements Handler {
  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception {
    List<Map<String, Object>> users = db.select(
      "SELECT id, username FROM users LIMIT 100",
      List.of()
    );
    res.status(200).contentType("application/json")
       .err(false).log(null).out(users).send();
  }
}

// Handler lento → async
@Async
public class UserExportHandler implements Handler {
  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception {
    // Export completo (può essere lento)
    List<Map<String, Object>> allUsers = db.select(
      "SELECT * FROM users",
      List.of()
    );
    res.status(200).contentType("application/json")
       .err(false).log(null).out(allUsers).send();
  }
}
```

---

## 6. Testing e Validazione

### 6.1 Unit Test

```java
package dev.jms.util;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AsyncAnnotationTest {
  
  @Async
  static class AsyncHandler implements Handler {
    public void get(HttpRequest req, HttpResponse res, DB db) {}
  }
  
  static class BlockingHandler implements Handler {
    public void get(HttpRequest req, HttpResponse res, DB db) {}
  }
  
  @Test
  void shouldDetectAsyncAnnotation() {
    assertTrue(AsyncHandler.class.isAnnotationPresent(Async.class));
    assertFalse(BlockingHandler.class.isAnnotationPresent(Async.class));
  }
}
```

### 6.2 Integration Test

```bash
#!/bin/bash
# test_async.sh

# Avvia applicazione
cmd app start

sleep 3

# Test handler blocking
echo "Test blocking handler..."
RESPONSE=
echo "Response: "

# Test handler async
echo "Test async handler..."
RESPONSE=
echo "Response: "

# Test concorrenza async (10 richieste parallele)
echo "Test concorrenza async..."
for i in {1..10}; do
  curl -s http://localhost:8080/api/test/slow &
done
wait

echo "✓ Tutti i test completati"
```

### 6.3 Load Test

```bash
# Apache Bench
ab -n 1000 -c 50 http://localhost:8080/api/test/slow

# wrk (più avanzato)
wrk -t 4 -c 50 -d 30s http://localhost:8080/api/test/slow

# Metriche da monitorare:
# - Requests/sec (throughput)
# - Latenza media
# - Latenza p95, p99
# - Errori
```

### 6.4 Checklist Validazione

- [ ] Handler blocking esistenti funzionano invariati
- [ ] Handler async ricevono body correttamente
- [ ] Query DB funzionano in modalità async
- [ ] Risposte async sono corrette
- [ ] Nessun deadlock su pool DB
- [ ] Nessun thread leak
- [ ] Errori gestiti correttamente (sia blocking che async)
- [ ] Load test: throughput adeguato
- [ ] Load test: nessun errore sotto carico

---

## 7. Monitoraggio e Tuning

### 7.1 Endpoint Monitoraggio

```java
package com.example.admin.handler;

import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.DB;
import dev.jms.util.AsyncExecutor;
import java.util.Map;

public class StatsHandler implements Handler {
  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception {
    AsyncExecutor.Stats stats = AsyncExecutor.getStats();
    
    Map<String, Object> metrics = Map.of(
      "asyncExecutor", Map.of(
        "poolSize", stats.poolSize,
        "activeThreads", stats.activeThreads,
        "queuedTasks", stats.queuedTasks,
        "completedTasks", stats.completedTasks
      ),
      "database", Map.of(
        "activeConnections", DB.getDataSource().getHikariPoolMXBean().getActiveConnections(),
        "idleConnections", DB.getDataSource().getHikariPoolMXBean().getIdleConnections(),
        "totalConnections", DB.getDataSource().getHikariPoolMXBean().getTotalConnections(),
        "threadsAwaitingConnection", DB.getDataSource().getHikariPoolMXBean().getThreadsAwaitingConnection()
      )
    );
    
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(metrics)
       .send();
  }
}
```

### 7.2 Logging

```java
// In AsyncExecutor.java, aggiungere logging

public static void init(int size) {
  // ...
  executor = Executors.newFixedThreadPool(size, r -> {
    Thread t = new Thread(r);
    t.setName("async-handler-" + t.getId());
    t.setUncaughtExceptionHandler((thread, throwable) -> {
      System.err.println("[error] Uncaught exception in " + thread.getName());
      throwable.printStackTrace();
    });
    return t;
  });
  
  System.out.println("[info] AsyncExecutor inizializzato: pool size = " + size);
}
```

### 7.3 Tuning Parametri

**Dimensionamento AsyncExecutor:**

```
Scenario 1: Query leggere, molte richieste concorrenti
- async.pool.size = HikariCP max pool size
- Esempio: 20 thread async, 20 connessioni DB

Scenario 2: Query pesanti, CPU-bound
- async.pool.size = numero CPU * 2
- Esempio: 8 CPU → 16 thread async

Scenario 3: Query pesanti, IO-bound (DB lento)
- async.pool.size = HikariCP max pool size * 1.5
- Esempio: 20 connessioni DB → 30 thread async
```

**Dimensionamento HikariCP:**

```properties
# Minimo
db.pool.min=5

# Massimo (allineato ad AsyncExecutor)
db.pool.max=20

# Timeout acquisizione connessione
db.pool.connection.timeout=30000
```

---

## 8. Rollout e Migration Path

### 8.1 Strategia di Rollout

**Step 1: Deploy passivo**
- Deployare codice con annotation `@Async` disponibile
- NESSUN handler usa ancora `@Async`
- Verificare che applicazione funzioni normalmente
- Durata: 1 giorno

**Step 2: Pilot endpoint**
- Scegliere 1-2 endpoint non critici con query lente
- Aggiungere `@Async`
- Monitorare metriche per 1 settimana
- Rollback immediato se problemi

**Step 3: Gradual adoption**
- Identificare handler lenti tramite profiling
- Migrare 20% handler lenti/settimana
- Monitorare throughput e latenza

**Step 4: Steady state**
- Mantenere mix blocking/async
- Usare `@Async` solo dove necessario
- Continuare monitoraggio

### 8.2 Criteri Selezione Handler per Async

**Candidati ideali:**
- ✅ Query con latenza > 500ms
- ✅ Endpoint con concorrenza alta (> 10 req/s)
- ✅ Operazioni CPU-intensive (export, report)
- ✅ Upload file > 1MB

**NON candidati:**
- ❌ CRUD semplici (< 100ms)
- ❌ Endpoint admin (bassa concorrenza)
- ❌ Health check / ping
- ❌ Operazioni transazionali complesse (rischio timeout)

### 8.3 Rollback Plan

Se si riscontrano problemi:

1. **Rollback immediato:** rimuovere `@Async` da handler problematici
2. **Rollback parziale:** disabilitare async tramite feature flag
3. **Rollback completo:** rimuovere `AsyncExecutor.init()` (tutti handler tornano blocking)

```java
// Feature flag per disabilitare async globalmente
public class AsyncExecutor {
  private static boolean enabled = true;
  
  public static void disable() {
    enabled = false;
  }
  
  public static boolean isEnabled() {
    return enabled;
  }
}

// In HandlerAdapter
private void handleRequest(HttpServerExchange exchange) {
  if (isAsync && AsyncExecutor.isEnabled()) {
    handleAsyncRequest(exchange);
  } else {
    handleBlockingRequest(exchange);
  }
}
```

---

## 9. Metriche di Successo

| Metrica | Before (Blocking) | After (Hybrid) | Target |
|---------|-------------------|----------------|--------|
| Throughput endpoint lenti | 32 req/s | 50+ req/s | +50% |
| Latenza p95 endpoint lenti | 2.5s | 1.5s | -40% |
| IO thread saturation | 80% | < 20% | < 30% |
| Worker thread saturation | 90% | < 50% | < 60% |
| DB connection saturation | 85% | 85% | < 90% |
| Errori 503 (pool saturo) | 5% | < 1% | < 1% |

---

## 10. Rischi e Mitigazioni

| Rischio | Probabilità | Impatto | Mitigazione |
|---------|-------------|---------|-------------|
| Saturazione AsyncExecutor | Media | Alto | Dimensionamento corretto + monitoraggio |
| Deadlock pool DB | Bassa | Critico | Timeout connessioni + pool size adeguato |
| Memory leak (body grandi) | Media | Medio | Limite max body size (10MB) |
| Doppia risposta | Bassa | Medio | Check `isResponseStarted()` |
| Regressione handler esistenti | Bassa | Critico | Test regressione completo |
| Performance degradation | Media | Alto | Load test prima/dopo + rollback plan |

---

## 11. Conclusioni

Questa architettura ibrida offre:

✅ **Backward compatibility:** nessun handler esistente deve cambiare
✅ **Opt-in async:** solo dove serve (`@Async`)
✅ **Stesso DB layer:** no cambio paradigma (JDBC + HikariCP)
✅ **Scalabilità:** IO thread sempre liberi
✅ **Controllo:** tuning granulare per endpoint

**Next Steps:**
1. Review documento con team
2. Setup ambiente test
3. Implementazione Fase 1-4
4. Pilot con 1 endpoint
5. Rollout graduale

**Timeline stimata:**
- Implementazione: 2-3 settimane
- Testing: 1 settimana
- Rollout graduale: 4 settimane
- **Totale: ~2 mesi**
