package dev.jms.app.module.cti.vonage.dao;

import dev.jms.app.module.cti.vonage.dto.OperatorDTO;
import dev.jms.util.DB;
import dev.jms.util.Log;
import java.util.HashMap;
import java.util.List;

/**
 * DAO per la tabella {@code cti_operatori}.
 *
 * <p>La colonna {@code sessione_account_id} non ha vincolo FK verso {@code accounts}:
 * il modulo user è opzionale. L'associazione è logica e garantita applicativamente.</p>
 */
public class OperatorDAO
{
  private static final Log log = Log.get(OperatorDAO.class);

  private final DB db;

  /**
   * @param db connessione al database
   */
  public OperatorDAO(DB db)
  {
    this.db = db;
  }

  /**
   * Assegna un operatore libero all'account o restituisce quello già assegnato,
   * aggiornando in entrambi i casi il TTL della sessione.
   *
   * <p>
   * Se l'account ha già un operatore assegnato nella sessione corrente
   * lo restituisce direttamente e ne rinnova il TTL (idempotente: usato anche
   * dal refresh token ogni 13 minuti).
   * <br>
   * Se non è assegnato, esegue un claim atomico tramite
   * {@code SELECT FOR UPDATE SKIP LOCKED} per evitare assegnazioni duplicate
   * in presenza di richieste concorrenti.
   * </p>
   *
   * @param accountId id account (claim {@code sub} del JWT applicativo)
   * @return operatore assegnato o {@code null} se operatore non disponibile
   */
  public OperatorDTO claimOrRenew(int accountId) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;
    long operatorId;
    OperatorDTO result;

    sql = "SELECT id, vonage_user_id, account_id, nome, attivo "
        + "FROM cti_operatori WHERE sessione_account_id = ? AND attivo = TRUE LIMIT 1";
    rows = db.select(sql, accountId);

    if (!rows.isEmpty()) {
      sql = "UPDATE cti_operatori SET sessione_ttl = NOW() + interval '30 minutes' "
          + "WHERE sessione_account_id = ?";
      db.query(sql, accountId);
      result = mapRow(rows.get(0));
    } else {
      result = null;
      db.begin();
      try {
        sql = "SELECT id FROM cti_operatori "
            + "WHERE attivo = TRUE AND sessione_account_id IS NULL "
            + "ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED";
        rows = db.select(sql);
        if (!rows.isEmpty()) {
          operatorId = DB.toLong(rows.get(0).get("id"));
          sql = "UPDATE cti_operatori "
              + "SET sessione_account_id = ?, sessione_ttl = NOW() + interval '30 minutes' "
              + "WHERE id = ?";
          db.query(sql, accountId, operatorId);
          sql = "SELECT id, vonage_user_id, account_id, nome, attivo "
              + "FROM cti_operatori WHERE id = ?";
          rows = db.select(sql, operatorId);
          result = mapRow(rows.get(0));
        }
        db.commit();
      } catch (Exception e) {
        db.rollback();
        throw e;
      }
    }
    return result;
  }

  /**
   * Rilascia l'operatore assegnato all'account.
   * Operazione idempotente: non fallisce se l'account non ha operatori assegnati.
   *
   * @param accountId id account da rilasciare
   */
  public void releaseSession(int accountId) throws Exception
  {
    String sql;
    sql = "UPDATE cti_operatori SET sessione_account_id = NULL, sessione_ttl = NULL "
        + "WHERE sessione_account_id = ?";
    db.query(sql, accountId);
  }

  /**
   * Inserisce un nuovo operatore nella tabella {@code cti_operatori}.
   * L'operatore è creato con {@code attivo = TRUE} e nessuna sessione assegnata.
   *
   * @param vonageUserId nome utente Vonage (claim {@code sub} del JWT SDK)
   * @param nome         nome visualizzato, o {@code null}
   * @return id generato dalla sequenza
   */
  public long insert(String vonageUserId, String nome) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql = "INSERT INTO cti_operatori (vonage_user_id, nome) VALUES (?, ?) RETURNING id";
    rows = db.select(sql, vonageUserId, nome);
    return DB.toLong(rows.get(0).get("id"));
  }

  /**
   * Rilascia tutte le sessioni il cui TTL è scaduto.
   * Metodo statico per l'esecuzione come job schedulato da {@link dev.jms.util.Scheduler}.
   * Acquisisce e rilascia autonomamente una connessione DB dal pool condiviso.
   *
   * <p>Progettato per essere eseguito ogni minuto. Se nessuna sessione è scaduta
   * non emette log.</p>
   *
   * @throws Exception se la connessione DB non è disponibile o la query fallisce
   */
  public static void releaseExpired() throws Exception
  {
    DB db;
    String sql;
    int released;

    db = new DB(DB.getDataSource());
    try {
      db.open();
      sql = "UPDATE cti_operatori SET sessione_account_id = NULL, sessione_ttl = NULL "
          + "WHERE sessione_ttl IS NOT NULL AND sessione_ttl < NOW()";
      released = db.query(sql);
      if (released > 0) {
        log.info("[CTI] releaseExpired: {} sessioni scadute rilasciate", released);
      }
    } finally {
      db.close();
    }
  }

  /**
   * Mappa una riga del result set in un {@link OperatorDTO}.
   *
   * @param row riga restituita da {@code db.select()}
   * @return DTO popolato
   */
  private OperatorDTO mapRow(HashMap<String, Object> row)
  {
    return new OperatorDTO(
        DB.toLong(row.get("id")),
        DB.toString(row.get("vonage_user_id")),
        DB.toLong(row.get("account_id")),
        DB.toString(row.get("nome")),
        DB.toBoolean(row.get("attivo"))
    );
  }
}
