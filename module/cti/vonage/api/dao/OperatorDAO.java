package dev.jms.app.module.cti.vonage.dao;

import dev.jms.app.module.cti.vonage.dto.OperatorDTO;
import dev.jms.util.DB;
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
  private final DB db;

  /**
   * @param db connessione al database
   */
  public OperatorDAO(DB db)
  {
    this.db = db;
  }

  /**
   * Assegna un operatore libero all'account, o restituisce quello già assegnato.
   *
   * <p>Se l'account ha già un operatore assegnato nella sessione corrente lo restituisce
   * direttamente (idempotente: usato anche dal refresh token).<br>
   * Se non è assegnato, esegue un claim atomico tramite
   * {@code SELECT FOR UPDATE SKIP LOCKED} per evitare assegnazioni duplicate
   * in presenza di richieste concorrenti.</p>
   *
   * @param accountId id account (claim {@code sub} del JWT applicativo)
   * @return operatore assegnato, o {@code null} se nessun operatore è disponibile
   */
  public OperatorDTO claimOrRenew(int accountId) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;
    long operatorId;
    OperatorDTO result;

    sql = "SELECT id, vonage_user_id, account_id, nome, attivo"
        + " FROM cti_operatori WHERE sessione_account_id = ? AND attivo = TRUE LIMIT 1";
    rows = db.select(sql, accountId);

    if (!rows.isEmpty()) {
      result = mapRow(rows.get(0));
    } else {
      result = null;
      db.begin();
      try {
        sql = "SELECT id FROM cti_operatori"
            + " WHERE attivo = TRUE AND sessione_account_id IS NULL"
            + " ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED";
        rows = db.select(sql);
        if (!rows.isEmpty()) {
          operatorId = DB.toLong(rows.get(0).get("id"));
          sql = "UPDATE cti_operatori SET sessione_account_id = ? WHERE id = ?";
          db.query(sql, accountId, operatorId);
          sql = "SELECT id, vonage_user_id, account_id, nome, attivo"
              + " FROM cti_operatori WHERE id = ?";
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
    sql = "UPDATE cti_operatori SET sessione_account_id = NULL WHERE sessione_account_id = ?";
    db.query(sql, accountId);
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
