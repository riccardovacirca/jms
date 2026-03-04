package dev.jms.util;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;

import javax.sql.DataSource;

/**
 * Adattatore tra l'interfaccia Handler della libreria e HttpHandler di Undertow.
 * Supporta due modalità di esecuzione:
 *
 * - BLOCKING (default): handler senza @Async, eseguiti su worker thread con startBlocking()
 * - ASYNC: handler con @Async, lettura body non-blocking + esecuzione su AsyncExecutor
 *
 * La modalità viene rilevata automaticamente tramite reflection sulla classe handler.
 *
 * Gestione eccezioni:
 * Gli handler gestiscono autonomamente le eccezioni di business (es. credenziali errate, token scaduto)
 * restituendo HTTP 200 con err:true e un messaggio amichevole, e loggando a livello WARN.
 * Qualsiasi eccezione non intercettata dall'handler (errore di sistema inaspettato) risale qui:
 * viene loggata a livello ERROR con stack trace e restituisce HTTP 500 al client.
 */
public class HandlerAdapter implements HttpHandler
{
  private static final Log log = Log.get(HandlerAdapter.class);

  private final Handler handler;
  private final DataSource dataSource;
  private final boolean isAsync;

  public HandlerAdapter(Handler handler, DataSource dataSource)
  {
    this.handler = handler;
    this.dataSource = dataSource;
    this.isAsync = handler.getClass().isAnnotationPresent(Async.class);
  }

  public HandlerAdapter(Handler handler)
  {
    this(handler, null);
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception
  {
    if (isAsync) {
      handleAsyncRequest(exchange);
    } else {
      handleBlockingRequest(exchange);
    }
  }

  /**
   * Modalità blocking (comportamento attuale, invariato).
   */
  private void handleBlockingRequest(HttpServerExchange exchange) throws Exception
  {
    if (exchange.isInIoThread()) {
      exchange.dispatch(this);
    } else {
      DB db;
      HttpResponse res;

      exchange.startBlocking();
      db = dataSource != null ? new DB(dataSource) : null;
      res = new HttpResponse(exchange);

      try {
        HttpRequest req;

        if (db != null) {
          db.open();
        }
        req = new HttpRequest(exchange);
        switch (req.getMethod()) {
          case "GET"    -> handler.get(req, res, db);
          case "POST"   -> handler.post(req, res, db);
          case "PUT"    -> handler.put(req, res, db);
          case "DELETE" -> handler.delete(req, res, db);
          default -> res.status(405).contentType("application/json").err(true).log("Method Not Allowed").out(null).send();
        }
      } catch (Exception e) {
        log.error("Errore di sistema in {}", handler.getClass().getSimpleName(), e);
        if (!exchange.isResponseStarted()) {
          res.status(500)
             .contentType("application/json")
             .err(true)
             .log("Errore interno del server")
             .out(null)
             .send();
        }
      } finally {
        if (db != null) {
          db.close();
        }
      }
    }
  }

  /**
   * Modalità async: lettura non-blocking → dispatch executor → risposta non-blocking.
   */
  private void handleAsyncRequest(HttpServerExchange exchange)
  {
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
  private void executeAsyncHandler(HttpServerExchange exchange, byte[] bodyBytes)
  {
    HttpRequest req = new HttpRequest(exchange, bodyBytes);
    HttpResponse res = new HttpResponse(exchange);
    DB db = dataSource != null ? new DB(dataSource) : null;

    try {
      if (db != null) {
        db.open(); // BLOCKING ma su thread executor dedicato
      }

      String method = exchange.getRequestMethod().toString();
      switch (method) {
        case "GET"    -> handler.get(req, res, db);
        case "POST"   -> handler.post(req, res, db);
        case "PUT"    -> handler.put(req, res, db);
        case "DELETE" -> handler.delete(req, res, db);
        default -> res.status(405).contentType("application/json").err(true).log("Method Not Allowed").out(null).send();
      }
    } catch (Exception e) {
      log.error("Errore di sistema in {} (async)", handler.getClass().getSimpleName(), e);
      if (!exchange.isResponseStarted()) {
        sendErrorResponse(exchange, 500, "Errore interno del server");
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
  private void sendErrorResponse(HttpServerExchange exchange, int status, String message)
  {
    exchange.setStatusCode(status);
    exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "application/json");
    String payload = String.format(
      "{\"err\":true,\"log\":\"%s\",\"out\":null}",
      message.replace("\"", "\\\"")
    );
    exchange.getResponseSender().send(payload);
  }
}
