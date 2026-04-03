package dev.jms.util;

import java.util.Map;

/**
 * Utility per il logging strutturato degli eventi in tabella jms_audit_log.
 * Disponibile a tutti i moduli per la registrazione di eventi significativi
 * (login, logout, modifiche dati, accessi non autorizzati, ecc.).
 *
 * <p>Pattern di utilizzo analogo a {@link Log}: metodi statici, nessuna inizializzazione richiesta.
 * Gli errori di scrittura vengono loggati ma non propagati per non interrompere il flusso applicativo.
 */
public class AuditLog
{
  private static final Log logger = Log.get(AuditLog.class);

  /**
   * Registra un evento di audit con tutti i dettagli disponibili.
   *
   * @param db        connessione database
   * @param event     tipo di evento (es. "user.login", "user.logout", "account.password.change")
   * @param userId    ID utente coinvolto (null se non autenticato)
   * @param username  username coinvolto (null se non autenticato)
   * @param ip        indirizzo IP client
   * @param userAgent User-Agent header del client
   * @param details   mappa chiave/valore con dettagli aggiuntivi (serializzata in JSONB)
   */
  public static void log(DB db, String event, Integer userId, String username,
                         String ip, String userAgent, Map<String, Object> details)
  {
    String detailsJson;
    String sql;

    detailsJson = details != null ? Json.encode(details) : null;
    sql = "INSERT INTO jms_audit_log (event, user_id, username, ip_address, user_agent, details) "
        + "VALUES (?, ?, ?, ?, ?, ?::jsonb)";

    try {
      db.query(sql, event, userId, username, ip, userAgent, detailsJson);
    } catch (Exception e) {
      logger.error("Audit log write failed for event '{}': {}", event, e.getMessage());
    }
  }

  /**
   * Registra un evento di audit senza dettagli aggiuntivi.
   *
   * @param db        connessione database
   * @param event     tipo di evento
   * @param userId    ID utente coinvolto (null se non autenticato)
   * @param username  username coinvolto (null se non autenticato)
   * @param ip        indirizzo IP client
   * @param userAgent User-Agent header del client
   */
  public static void log(DB db, String event, Integer userId, String username,
                         String ip, String userAgent)
  {
    log(db, event, userId, username, ip, userAgent, null);
  }
}
