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
   * Assegna un operatore libero all'account o restituisce quello già assegnato,
   * aggiornando in entrambi i casi la scadenza del claim.
   *
   * <p>
   * Se l'account ha già un claim attivo su un operatore
   * lo restituisce direttamente e ne rinnova la scadenza (idempotente: usato anche
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
        + "FROM jms_cti_operatori WHERE claim_account_id = ? AND attivo = TRUE LIMIT 1";
    rows = db.select(sql, accountId);

    if (!rows.isEmpty()) {
      sql = "UPDATE jms_cti_operatori SET claim_scadenza = NOW() + interval '30 minutes' "
          + "WHERE claim_account_id = ?";
      db.query(sql, accountId);
      result = mapRow(rows.get(0));
    } else {
      result = null;
      db.begin();
      try {
        sql = "SELECT id FROM jms_cti_operatori "
            + "WHERE attivo = TRUE AND claim_account_id IS NULL "
            + "ORDER BY id LIMIT 1 FOR UPDATE SKIP LOCKED";
        rows = db.select(sql);
        if (!rows.isEmpty()) {
          operatorId = DB.toLong(rows.get(0).get("id"));
          sql = "UPDATE jms_cti_operatori "
              + "SET claim_account_id = ?, claim_scadenza = NOW() + interval '30 minutes' "
              + "WHERE id = ?";
          db.query(sql, accountId, operatorId);
          sql = "SELECT id, vonage_user_id, account_id, nome, attivo "
              + "FROM jms_cti_operatori WHERE id = ?";
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

    sql = "SELECT id, vonage_user_id, account_id, nome, attivo "
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

    sql = "SELECT id, vonage_user_id, account_id, nome, attivo "
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

    sql = "SELECT id, vonage_user_id, account_id, nome, attivo "
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

    sql = "SELECT id, vonage_user_id, account_id, nome, attivo "
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
   * Aggiorna nome e stato attivo di un operatore.
   *
   * @param id     chiave primaria
   * @param nome   nuovo nome visualizzato
   * @param attivo nuovo stato abilitazione
   */
  public void update(long id, String nome, Boolean attivo) throws Exception
  {
    String sql;

    sql = "UPDATE jms_cti_operatori SET nome = ?, attivo = ? WHERE id = ?";
    db.query(sql, nome, attivo, id);
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
   * @param nome         nome visualizzato, o {@code null}
   * @return id generato dalla sequenza
   */
  public long insert(String vonageUserId, String nome) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql = "INSERT INTO jms_cti_operatori (vonage_user_id, nome) VALUES (?, ?) RETURNING id";
    rows = db.select(sql, vonageUserId, nome);
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
        DB.toString(row.get("nome")),
        DB.toBoolean(row.get("attivo"))
    );
  }

  /**
   * Assegna il contatto corrente all'operatore.
   *
   * @param operatoreId id operatore
   * @param contattoJson JSON serializzato del contatto
   */
  public void setContattoCorrente(long operatoreId, String contattoJson) throws Exception
  {
    String sql;

    sql = "UPDATE jms_cti_operatori SET contatto_corrente = ?::jsonb, contatto_data_assegnazione = NOW() WHERE id = ?";
    db.query(sql, contattoJson, operatoreId);
  }

  /**
   * Recupera il contatto corrente assegnato all'operatore.
   *
   * @param operatoreId id operatore
   * @return JSON del contatto o {@code null}
   */
  public String getContattoCorrente(long operatoreId) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql = "SELECT contatto_corrente::text FROM jms_cti_operatori WHERE id = ?";
    rows = db.select(sql, operatoreId);
    if (rows.isEmpty() || rows.get(0).get("contatto_corrente") == null) {
      return null;
    }
    return DB.toString(rows.get(0).get("contatto_corrente"));
  }

  /**
   * Cancella il contatto corrente dall'operatore.
   *
   * @param operatoreId id operatore
   */
  public void clearContattoCorrente(long operatoreId) throws Exception
  {
    String sql;

    sql = "UPDATE jms_cti_operatori SET contatto_corrente = NULL, contatto_data_assegnazione = NULL WHERE id = ?";
    db.query(sql, operatoreId);
  }
}
