package dev.jms.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Executor dedicato per handler @Async.
 * Dimensionato in base al pool HikariCP e alle operazioni lente attese.
 */
public class AsyncExecutor
{
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
  public static void init(int size)
  {
    if (executor != null) {
      throw new IllegalStateException("AsyncExecutor already initialized");
    }
    poolSize = size;
    executor = Executors.newFixedThreadPool(
      size,
      r -> {
        Thread t;
        t = new Thread(r);
        t.setName("async-handler-" + t.threadId());
        // Non daemon per permettere graceful shutdown
        t.setDaemon(false);
        t.setUncaughtExceptionHandler((thread, throwable) -> {
          System.err.println("[error] Uncaught exception in " + thread.getName());
          throwable.printStackTrace();
        });
        return t;
      }
    );
    System.out.println("[info] AsyncExecutor inizializzato: pool size = " + size);
  }

  /**
   * Restituisce l'executor (per uso interno HandlerAdapter).
   */
  public static ExecutorService getExecutor()
  {
    if (executor == null) {
      throw new IllegalStateException("AsyncExecutor not initialized");
    }
    return executor;
  }

  /**
   * Shutdown graceful dell'executor.
   * Da chiamare in fase di shutdown applicazione.
   */
  public static void shutdown()
  {
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
      System.out.println("[info] AsyncExecutor terminato");
    }
  }

  /**
   * Statistiche per monitoraggio.
   */
  public static Stats getStats()
  {
    ThreadPoolExecutor tpe;
    Stats result;

    if (executor instanceof ThreadPoolExecutor) {
      tpe = (ThreadPoolExecutor) executor;
      result = new Stats(
        poolSize,
        tpe.getActiveCount(),
        tpe.getQueue().size(),
        tpe.getCompletedTaskCount()
      );
    } else {
      result = new Stats(poolSize, 0, 0, 0);
    }
    return result;
  }

  /** Snapshot delle statistiche del pool di thread. */
  public static class Stats
  {
    /** Dimensione configurata del pool. */
    public final int poolSize;
    /** Numero di thread attivi. */
    public final int activeThreads;
    /** Numero di task in coda. */
    public final int queuedTasks;
    /** Numero totale di task completati. */
    public final long completedTasks;

    /** Crea uno snapshot con i valori forniti. */
    Stats(int poolSize, int activeThreads, int queuedTasks, long completedTasks)
    {
      this.poolSize = poolSize;
      this.activeThreads = activeThreads;
      this.queuedTasks = queuedTasks;
      this.completedTasks = completedTasks;
    }
  }
}
