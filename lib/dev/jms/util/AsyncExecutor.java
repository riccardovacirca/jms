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
      System.out.println("[info] AsyncExecutor terminato");
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
