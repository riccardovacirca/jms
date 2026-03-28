package dev.jms.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Blacklist in-memory per JWT revocati (logout, cambio password).
 * Previene session replay attack dopo logout tracciando JWT ID (jti) fino alla scadenza naturale.
 *
 * <p>Pattern: singleton lazy-init con cleanup automatico. Thread-safe (ConcurrentHashMap).
 * Cleanup eseguito ogni minuto da daemon thread per rimuovere token scaduti.
 */
public class JWTBlacklist
{
  private static final ConcurrentHashMap<String, Long> revokedTokens = new ConcurrentHashMap<>();
  private static ScheduledExecutorService cleanupExecutor;

  /**
   * Revoca un JWT aggiungendolo alla blacklist.
   * Il token rimane in blacklist fino alla sua scadenza naturale.
   *
   * @param jti           JWT ID (claim jti)
   * @param expiresAtMillis timestamp di scadenza in millisecondi
   */
  public static void revoke(String jti, long expiresAtMillis)
  {
    ensureCleanupStarted();
    revokedTokens.put(jti, expiresAtMillis);
  }

  /**
   * Verifica se un JWT e' revocato.
   * Se il token e' scaduto naturalmente, lo rimuove dalla blacklist e restituisce false.
   *
   * @param jti JWT ID (claim jti)
   * @return true se revocato, false altrimenti
   */
  public static boolean isRevoked(String jti)
  {
    Long expiresAt;
    long now;
    boolean result;

    expiresAt = revokedTokens.get(jti);
    result = false;
    if (expiresAt != null) {
      now = System.currentTimeMillis();
      if (now > expiresAt) {
        revokedTokens.remove(jti);
        result = false;
      } else {
        result = true;
      }
    }
    return result;
  }

  /** Avvia il cleanup automatico se non gia' attivo. */
  private static synchronized void ensureCleanupStarted()
  {
    if (cleanupExecutor == null) {
      cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t;
        t = new Thread(r, "jwt-blacklist-cleanup");
        t.setDaemon(true);
        return t;
      });
      cleanupExecutor.scheduleAtFixedRate(JWTBlacklist::cleanup, 60, 60, TimeUnit.SECONDS);
    }
  }

  /** Rimuove token scaduti dalla blacklist. */
  private static void cleanup()
  {
    long now;

    now = System.currentTimeMillis();
    revokedTokens.entrySet().removeIf(entry -> now > entry.getValue());
  }

  /** Chiude il cleanup executor (chiamato dallo shutdown hook di App.java). */
  public static synchronized void shutdown()
  {
    if (cleanupExecutor != null) {
      cleanupExecutor.shutdown();
      cleanupExecutor = null;
    }
  }
}
