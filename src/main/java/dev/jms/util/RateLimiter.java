package dev.jms.util;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Rate limiter in-memory globale per protezione brute-force.
 * Traccia tentativi falliti per chiave (es. "user.login:IP") con finestra temporale scorrevole.
 *
 * <p>Pattern: singleton lazy-init con cleanup automatico. Configurabile tramite {@link #configure(int, long)}.
 * Thread-safe (ConcurrentHashMap). Cleanup eseguito ogni minuto da daemon thread.
 */
public class RateLimiter
{
  private static final ConcurrentHashMap<String, AttemptRecord> attempts = new ConcurrentHashMap<>();
  private static ScheduledExecutorService cleanupExecutor;
  private static int maxAttempts = 5;
  private static long windowSeconds = 300;

  /**
   * Configura rate limiting globale.
   * Avvia il cleanup automatico se non gia' attivo.
   *
   * @param max     massimo numero di tentativi falliti consentiti
   * @param window  finestra temporale in secondi
   */
  public static void configure(int max, long window)
  {
    maxAttempts = max;
    windowSeconds = window;
    ensureCleanupStarted();
  }

  /**
   * Verifica se la chiave e' bloccata (troppi tentativi falliti nella finestra).
   * Se la finestra e' scaduta, rimuove il record e restituisce false.
   *
   * @param key chiave univoca (es. "user.login:192.168.1.100")
   * @return true se bloccato, false altrimenti
   */
  public static boolean isBlocked(String key)
  {
    AttemptRecord record;
    long now;

    record = attempts.get(key);
    if (record == null) {
      return false;
    }
    now = Instant.now().getEpochSecond();
    if (now - record.firstAttempt > windowSeconds) {
      attempts.remove(key);
      return false;
    }
    return record.count >= maxAttempts;
  }

  /**
   * Registra un tentativo fallito.
   * Se e' il primo tentativo nella finestra, crea un nuovo record.
   * Se la finestra e' scaduta, resetta il contatore.
   *
   * @param key chiave univoca
   */
  public static void recordFailure(String key)
  {
    long now;
    AttemptRecord record;

    now = Instant.now().getEpochSecond();
    record = attempts.get(key);
    if (record == null) {
      attempts.put(key, new AttemptRecord(now, 1));
    } else {
      if (now - record.firstAttempt > windowSeconds) {
        attempts.put(key, new AttemptRecord(now, 1));
      } else {
        attempts.put(key, new AttemptRecord(record.firstAttempt, record.count + 1));
      }
    }
  }

  /**
   * Rimuove il record per la chiave specificata (es. dopo login riuscito).
   *
   * @param key chiave univoca
   */
  public static void reset(String key)
  {
    attempts.remove(key);
  }

  /** Avvia il cleanup automatico se non gia' attivo. */
  private static synchronized void ensureCleanupStarted()
  {
    if (cleanupExecutor == null) {
      cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t;
        t = new Thread(r, "ratelimiter-cleanup");
        t.setDaemon(true);
        return t;
      });
      cleanupExecutor.scheduleAtFixedRate(RateLimiter::cleanup, 60, 60, TimeUnit.SECONDS);
    }
  }

  /** Rimuove record scaduti dalla mappa. */
  private static void cleanup()
  {
    long now;

    now = Instant.now().getEpochSecond();
    attempts.entrySet().removeIf(entry -> now - entry.getValue().firstAttempt > windowSeconds);
  }

  /** Chiude il cleanup executor (chiamato dallo shutdown hook di App.java). */
  public static synchronized void shutdown()
  {
    if (cleanupExecutor != null) {
      cleanupExecutor.shutdown();
      cleanupExecutor = null;
    }
  }

  /** Record immutabile per tracciare tentativi. */
  private static class AttemptRecord
  {
    final long firstAttempt;
    final int count;

    AttemptRecord(long firstAttempt, int count)
    {
      this.firstAttempt = firstAttempt;
      this.count = count;
    }
  }
}
