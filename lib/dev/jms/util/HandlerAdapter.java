package dev.jms.util;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import javax.sql.DataSource;

/**
 * Adattatore tra l'interfaccia Handler della libreria e HttpHandler di Undertow.
 * Istanzia req, res e db dal contesto della richiesta e li passa all'handler.
 * Esegue il dispatch su un thread bloccante prima di invocare l'handler,
 * rendendo disponibile la lettura del body tramite HttpRequest.getBody().
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

  public HandlerAdapter(Handler handler, DataSource dataSource)
  {
    this.handler = handler;
    this.dataSource = dataSource;
  }

  public HandlerAdapter(Handler handler)
  {
    this(handler, null);
  }

  @Override
  public void handleRequest(HttpServerExchange exchange) throws Exception
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
}
