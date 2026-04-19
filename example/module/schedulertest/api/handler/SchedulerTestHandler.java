package dev.jms.app.schedulertest.handler;

import dev.jms.util.DB;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Scheduler;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

/**
 * Handler per il modulo schedulertest.
 * Dimostra la registrazione e l'esecuzione di job schedulati tramite {@link Scheduler}.
 * Il job {@link #tick()} è statico (requisito JobRunr) e scrive su un log in-memory
 * leggibile tramite l'endpoint {@code /api/schedulertest/log}.
 */
public class SchedulerTestHandler
{
  private static final int              MAX_LOG_ENTRIES = 20;
  private static final LinkedList<HashMap<String, Object>> executionLog = new LinkedList<>();

  /**
   * Job schedulato — eseguito ogni minuto da JobRunr.
   * Deve essere statico e senza parametri per garantire la corretta
   * serializzazione del method reference da parte di JobRunr.
   *
   * @throws Exception in caso di errore durante l'esecuzione
   */
  public static void tick() throws Exception
  {
    HashMap<String, Object> entry;

    entry = new HashMap<>();
    entry.put("timestamp",   Instant.now().toString());
    entry.put("thread",      Thread.currentThread().getName());
    entry.put("executed_at", System.currentTimeMillis());

    synchronized (executionLog) {
      executionLog.addFirst(entry);
      if (executionLog.size() > MAX_LOG_ENTRIES) {
        executionLog.removeLast();
      }
    }

    System.out.println("[info] schedulertest: tick eseguito @ " + entry.get("timestamp")
      + " su thread " + entry.get("thread"));
  }

  /**
   * GET /api/schedulertest/log — ultime esecuzioni del job {@code schedulertest-tick}.
   * Restituisce al massimo {@value MAX_LOG_ENTRIES} voci in ordine cronologico inverso.
   */
  public void log(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    List<HashMap<String, Object>> snapshot;

    synchronized (executionLog) {
      snapshot = new ArrayList<>(executionLog);
    }

    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(snapshot)
       .send();
  }

  /**
   * GET /api/schedulertest/status — stato dello scheduler.
   * Riporta se lo scheduler è inizializzato e quante esecuzioni sono state registrate.
   */
  public void status(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    HashMap<String, Object> out;
    int                     count;

    synchronized (executionLog) {
      count = executionLog.size();
    }

    out = new HashMap<>();
    out.put("scheduler_initialized", Scheduler.isInitialized());
    out.put("executions_recorded",   count);
    out.put("max_log_entries",       MAX_LOG_ENTRIES);

    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(out)
       .send();
  }
}
