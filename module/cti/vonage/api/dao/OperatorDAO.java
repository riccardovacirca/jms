package dev.jms.app.module.cti.vonage.dao;

import dev.jms.app.module.cti.vonage.dto.OperatorDTO;
import dev.jms.util.DB;
import dev.jms.util.Log;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * DAO per la tabella {@code jms_cti_operatori}.
 *
 * <p>La colonna {@code claim_account_id} non ha vincolo FK verso {@code accounts}:
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
   * Esegue il claim sull'operatore permanentemente assegnato all'account.
   *
   * <p>Se l'account ha già un claim attivo su un operatore, ne rinnova la scadenza.
   * Altrimenti esegue il claim atomico sull'operatore specificato tramite
   * {@code SELECT FOR UPDATE}. Restituisce {@code null} se l'operatore è temporaneamente
   * non disponibile (già claimato da un altro account in corso di commit).</p>
   *
   * @param accountId          id account applicativo
   * @param assignedOperatorId id dell'operatore permanentemente assegnato all'account
   * @return operatore claimato o {@code null} se non disponibile
   */
  public OperatorDTO claimOrRenewAssigned(int accountId, long assignedOperatorId) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;
    Integer existingClaim;
    OperatorDTO result;

    db.begin();
    try {
      sql = "SELECT id, claim_account_id FROM jms_cti_operatori "
          + "WHERE id = ? AND attivo = TRUE FOR UPDATE";
      rows = db.select(sql, assignedOperatorId);
      if (rows.isEmpty()) {
        db.rollback();
        result = null;
      } else {
        existingClaim = DB.toInteger(rows.get(0).get("claim_account_id"));
        if (existingClaim != null && existingClaim != accountId) {
          db.rollback();
          result = null;
        } else {
          sql = "UPDATE jms_cti_operatori "
              + "SET claim_account_id = ?, claim_scadenza = NOW() + interval '30 minutes' "
              + "WHERE id = ?";
          db.query(sql, accountId, assignedOperatorId);
          db.commit();
          sql = "SELECT id, vonage_user_id, account_id, attivo "
              + "FROM jms_cti_operatori WHERE id = ?";
          rows = db.select(sql, assignedOperatorId);
          result = mapRow(rows.get(0));
        }
      }
    } catch (Exception e) {
      db.rollback();
      throw e;
    }
    return result;
  }

  /**
   * Cerca l'operatore con assegnazione permanente per l'account indicato.
   *
   * @param accountId id account applicativo
   * @return operatore trovato o {@code null}
   */
  public OperatorDTO findByAccountId(int accountId) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql = "SELECT id, vonage_user_id, account_id, attivo "
        + "FROM jms_cti_operatori WHERE account_id = ? AND attivo = TRUE LIMIT 1";
    rows = db.select(sql, accountId);
    if (rows.isEmpty()) {
      return null;
    }
    return mapRow(rows.get(0));
  }

  /**
   * Assegna o rimuove l'associazione permanente tra un account applicativo e l'operatore.
   *
   * @param operatoreId id dell'operatore da aggiornare
   * @param accountId   id account da associare, o {@code null} per rimuovere l'associazione
   */
  public void assignAccount(long operatoreId, Integer accountId) throws Exception
  {
    String sql;

    sql = "UPDATE jms_cti_operatori SET account_id = ? WHERE id = ?";
    db.query(sql, accountId, operatoreId);
  }

  /**
   * Restituisce tutti gli operatori con il relativo username dell'account associato.
   * Esegue un {@code LEFT JOIN} su {@code jms_user_accounts} per includere il campo
   * {@code account_username}.
   *
   * @return lista di mappe con i campi dell'operatore e {@code account_username}
   */
  public List<HashMap<String, Object>> findAllForAdmin() throws Exception
  {
    String sql;

    sql = "SELECT o.id, o.vonage_user_id, o.account_id, o.attivo, "
        + "o.claim_account_id, o.claim_scadenza, "
        + "a.username AS account_username "
        + "FROM jms_cti_operatori o "
        + "LEFT JOIN jms_user_accounts a ON a.id = o.account_id "
        + "ORDER BY o.id";
    return db.select(sql);
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
    sql = "UPDATE jms_cti_operatori SET claim_account_id = NULL, claim_scadenza = NULL "
        + "WHERE claim_account_id = ?";
    db.query(sql, accountId);
  }

  /**
   * Restituisce tutti gli operatori ordinati per id.
   *
   * @return lista di tutti gli operatori registrati
   */
  public List<OperatorDTO> findAll() throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;
    List<OperatorDTO> result;

    sql = "SELECT id, vonage_user_id, account_id, attivo "
        + "FROM jms_cti_operatori ORDER BY id";
    rows = db.select(sql);
    result = new ArrayList<>();
    for (HashMap<String, Object> row : rows) {
      result.add(mapRow(row));
    }
    return result;
  }

  /**
   * Cerca un operatore per chiave primaria.
   *
   * @param id chiave primaria
   * @return operatore trovato o {@code null}
   */
  public OperatorDTO findById(long id) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql = "SELECT id, vonage_user_id, account_id, attivo "
        + "FROM jms_cti_operatori WHERE id = ?";
    rows = db.select(sql, id);
    if (rows.isEmpty()) {
      return null;
    }
    return mapRow(rows.get(0));
  }

  /**
   * Cerca l'operatore che ha il claim attivo per l'account indicato.
   *
   * @param accountId id account applicativo
   * @return operatore trovato o {@code null}
   */
  public OperatorDTO findByClaimAccountId(int accountId) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql = "SELECT id, vonage_user_id, account_id, attivo "
        + "FROM jms_cti_operatori WHERE claim_account_id = ? AND attivo = TRUE LIMIT 1";
    rows = db.select(sql, accountId);
    if (rows.isEmpty()) {
      return null;
    }
    return mapRow(rows.get(0));
  }

  /**
   * Cerca un operatore per {@code vonage_user_id}.
   *
   * @param vonageUserId nome utente Vonage
   * @return operatore trovato o {@code null}
   */
  public OperatorDTO findByVonageUserId(String vonageUserId) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql = "SELECT id, vonage_user_id, account_id, attivo "
        + "FROM jms_cti_operatori WHERE vonage_user_id = ?";
    rows = db.select(sql, vonageUserId);
    if (rows.isEmpty()) {
      return null;
    }
    return mapRow(rows.get(0));
  }

  /**
   * Restituisce il {@code claim_account_id} corrente dell'operatore,
   * ovvero l'account che detiene il claim su questo operatore al momento della chiamata.
   *
   * @param operatoreId chiave primaria dell'operatore
   * @return accountId dell'utente che ha reclamato l'operatore, o {@code null}
   */
  public Long findSessionAccountId(long operatoreId) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql = "SELECT claim_account_id FROM jms_cti_operatori WHERE id = ?";
    rows = db.select(sql, operatoreId);
    if (rows.isEmpty()) {
      return null;
    }
    return DB.toLong(rows.get(0).get("claim_account_id"));
  }

  /**
   * Aggiorna lo stato attivo di un operatore.
   *
   * @param id     chiave primaria
   * @param attivo nuovo stato abilitazione
   */
  public void update(long id, Boolean attivo) throws Exception
  {
    String sql;

    sql = "UPDATE jms_cti_operatori SET attivo = ? WHERE id = ?";
    db.query(sql, attivo, id);
  }

  /**
   * Elimina un operatore dalla tabella.
   *
   * @param id chiave primaria
   */
  public void delete(long id) throws Exception
  {
    String sql;

    sql = "DELETE FROM jms_cti_operatori WHERE id = ?";
    db.query(sql, id);
  }

  /**
   * Inserisce un nuovo operatore nella tabella {@code jms_cti_operatori}.
   * L'operatore è creato con {@code attivo = TRUE} e nessun claim attivo.
   *
   * @param vonageUserId nome utente Vonage (claim {@code sub} del JWT SDK)
   * @return id generato dalla sequenza
   */
  public long insert(String vonageUserId) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql = "INSERT INTO jms_cti_operatori (vonage_user_id) VALUES (?) RETURNING id";
    rows = db.select(sql, vonageUserId);
    return DB.toLong(rows.get(0).get("id"));
  }

  /**
   * Rilascia tutte le sessioni il cui TTL è scaduto.
   * Metodo statico per l'esecuzione come job schedulato da {@link dev.jms.util.Scheduler}.
   * Acquisisce e rilascia autonomamente una connessione DB dal pool condiviso.
   *
   * <p>Progettato per essere eseguito ogni minuto. Se nessun claim è scaduto
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
      sql = "UPDATE jms_cti_operatori SET claim_account_id = NULL, claim_scadenza = NULL "
          + "WHERE claim_scadenza IS NOT NULL AND claim_scadenza < NOW()";
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
        DB.toBoolean(row.get("attivo"))
    );
  }

}
