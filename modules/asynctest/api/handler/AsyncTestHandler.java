package {{APP_PACKAGE}}.asynctest.handler;

import dev.jms.util.DB;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;

import java.util.HashMap;
import java.util.List;

/**
 * Handler per il modulo asynctest.
 * Fornisce endpoint che simulano operazioni lente, con varianti async e blocking
 * per confrontare il comportamento sotto carico concorrente.
 */
public class AsyncTestHandler
{
  /**
   * GET /api/asynctest/slow-query?ms=500 — query lenta simulata con pg_sleep (ASYNC).
   * Usa il thread pool di AsyncExecutor: non satura i worker thread di Undertow.
   */
  public void slowQuery(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String msParam;
    int ms;
    String sql;
    List<HashMap<String, Object>> result;
    HashMap<String, Object> out;

    msParam = req.getQueryParam("ms");
    ms      = parseMsParam(msParam, 500);
    sql     = "SELECT pg_sleep(? / 1000.0), ? AS ms, 'async' AS mode";
    result  = db.select(sql, ms, ms);
    out     = result.isEmpty() ? new HashMap<>() : result.get(0);
    out.put("thread", Thread.currentThread().getName());
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(out)
       .send();
  }

  /**
   * GET /api/asynctest/blocking-query?ms=500 — stessa query lenta, esecuzione BLOCKING.
   * Usa i worker thread di Undertow: mostra la saturazione sotto carico elevato.
   */
  public void blockingQuery(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String msParam;
    int ms;
    String sql;
    List<HashMap<String, Object>> result;
    HashMap<String, Object> out;

    msParam = req.getQueryParam("ms");
    ms      = parseMsParam(msParam, 500);
    sql     = "SELECT pg_sleep(? / 1000.0), ? AS ms, 'blocking' AS mode";
    result  = db.select(sql, ms, ms);
    out     = result.isEmpty() ? new HashMap<>() : result.get(0);
    out.put("thread", Thread.currentThread().getName());
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(out)
       .send();
  }

  /**
   * GET /api/asynctest/slow-task?ms=500 — operazione lenta senza DB (ASYNC).
   * Simula elaborazione CPU/IO tramite Thread.sleep sul thread pool AsyncExecutor.
   */
  public void slowTask(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String msParam;
    int ms;
    long start;
    long elapsed;
    HashMap<String, Object> out;

    msParam = req.getQueryParam("ms");
    ms      = parseMsParam(msParam, 500);
    start   = System.currentTimeMillis();
    Thread.sleep(ms);
    elapsed = System.currentTimeMillis() - start;
    out     = new HashMap<>();
    out.put("requested_ms", ms);
    out.put("elapsed_ms",   elapsed);
    out.put("thread",       Thread.currentThread().getName());
    out.put("mode",         "async");
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(out)
       .send();
  }

  /**
   * GET /api/asynctest/status — stato del pool AsyncExecutor.
   * Mostra thread attivi, task in coda e task completati.
   */
  public void status(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    dev.jms.util.AsyncExecutor.Stats stats;
    HashMap<String, Object> out;

    stats = dev.jms.util.AsyncExecutor.getStats();
    out   = new HashMap<>();
    out.put("pool_size",        stats.poolSize);
    out.put("active_threads",   stats.activeThreads);
    out.put("queued_tasks",     stats.queuedTasks);
    out.put("completed_tasks",  stats.completedTasks);
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(out)
       .send();
  }

  /**
   * Parsa il parametro ms dalla querystring.
   * Restituisce il valore di default se assente, non numerico o fuori dall'intervallo [1, 30000].
   */
  private static int parseMsParam(String value, int defaultMs)
  {
    int ms;

    if (value == null || value.isBlank()) {
      return defaultMs;
    }
    try {
      ms = Integer.parseInt(value);
    } catch (NumberFormatException e) {
      return defaultMs;
    }
    if (ms < 1 || ms > 30000) {
      return defaultMs;
    }
    return ms;
  }
}
