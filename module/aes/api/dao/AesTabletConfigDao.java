package dev.jms.app.module.aes.dao;

import dev.jms.app.module.aes.dto.AesTabletConfig;
import dev.jms.util.DB;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * DAO per accesso alla tabella {@code aes_tablet_config}.
 * <p>
 * Gestisce le configurazioni tablet per firma remota Savino/Namirial.
 * Ogni tablet ha credenziali dedicate per autenticazione dm7auth.
 * </p>
 */
public final class AesTabletConfigDao
{
  private final DB db;

  public AesTabletConfigDao(DB db)
  {
    this.db = db;
  }

  /**
   * Recupera la configurazione tablet per ID tablet.
   *
   * @param tabletId ID tablet univoco
   * @return configurazione tablet, o {@code null} se non trovato
   * @throws Exception se errore database
   */
  public AesTabletConfig getByTabletId(String tabletId) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;

    sql = "SELECT * FROM aes_tablet_config WHERE tablet_id = ? AND enabled = true";
    rows = db.select(sql, tabletId);

    if (rows.isEmpty()) {
      return null;
    }

    return mapRow(rows.get(0));
  }

  /**
   * Recupera tutte le configurazioni tablet per account.
   *
   * @param accountId ID account
   * @return lista configurazioni (può essere vuota)
   * @throws Exception se errore database
   */
  public List<AesTabletConfig> getByAccountId(Long accountId) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;
    List<AesTabletConfig> result;

    sql = "SELECT * FROM aes_tablet_config WHERE account_id = ? AND enabled = true ORDER BY tablet_name";
    rows = db.select(sql, accountId);

    result = new ArrayList<>();
    for (HashMap<String, Object> row : rows) {
      result.add(mapRow(row));
    }

    return result;
  }

  /**
   * Recupera tutte le configurazioni tablet per provider.
   *
   * @param provider provider firma remota ('savino' o 'namirial')
   * @return lista configurazioni (può essere vuota)
   * @throws Exception se errore database
   */
  public List<AesTabletConfig> getByProvider(String provider) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;
    List<AesTabletConfig> result;

    sql = "SELECT * FROM aes_tablet_config WHERE provider = ? AND enabled = true ORDER BY tablet_name";
    rows = db.select(sql, provider);

    result = new ArrayList<>();
    for (HashMap<String, Object> row : rows) {
      result.add(mapRow(row));
    }

    return result;
  }

  /**
   * Recupera tutte le configurazioni tablet (abilitate).
   *
   * @return lista configurazioni (può essere vuota)
   * @throws Exception se errore database
   */
  public List<AesTabletConfig> getAll() throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;
    List<AesTabletConfig> result;

    sql = "SELECT * FROM aes_tablet_config WHERE enabled = true ORDER BY tablet_name";
    rows = db.select(sql);

    result = new ArrayList<>();
    for (HashMap<String, Object> row : rows) {
      result.add(mapRow(row));
    }

    return result;
  }

  /**
   * Inserisce una nuova configurazione tablet.
   *
   * @param accountId        ID account proprietario
   * @param tabletId         ID tablet univoco
   * @param tabletName       nome tablet
   * @param tabletApp        applicazione tablet
   * @param tabletDepartment dipartimento tablet
   * @param provider         provider ('savino' o 'namirial')
   * @param endpoint         URL base API provider
   * @param username         username autenticazione
   * @param password         password autenticazione
   * @return ID record inserito
   * @throws Exception se errore database o tablet_id duplicato
   */
  public Long insert(
    Long accountId,
    String tabletId,
    String tabletName,
    String tabletApp,
    String tabletDepartment,
    String provider,
    String endpoint,
    String username,
    String password
  ) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;
    Long id;

    sql = "INSERT INTO aes_tablet_config " +
          "(account_id, tablet_id, tablet_name, tablet_app, tablet_department, " +
          "provider, endpoint, username, password, enabled) " +
          "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, true) " +
          "RETURNING id";

    rows = db.select(
      sql,
      accountId,
      tabletId,
      tabletName,
      tabletApp,
      tabletDepartment,
      provider,
      endpoint,
      username,
      password
    );

    id = DB.toLong(rows.get(0).get("id"));
    return id;
  }

  /**
   * Aggiorna una configurazione tablet esistente.
   *
   * @param id               ID record
   * @param tabletName       nome tablet
   * @param tabletApp        applicazione tablet
   * @param tabletDepartment dipartimento tablet
   * @param endpoint         URL base API provider
   * @param username         username autenticazione
   * @param password         password autenticazione
   * @return numero righe modificate (1 se aggiornato, 0 se non trovato)
   * @throws Exception se errore database
   */
  public int update(
    Long id,
    String tabletName,
    String tabletApp,
    String tabletDepartment,
    String endpoint,
    String username,
    String password
  ) throws Exception
  {
    String sql;
    int rows;

    sql = "UPDATE aes_tablet_config SET " +
          "tablet_name = ?, tablet_app = ?, tablet_department = ?, " +
          "endpoint = ?, username = ?, password = ?, " +
          "updated_at = CURRENT_TIMESTAMP " +
          "WHERE id = ?";

    rows = db.query(
      sql,
      tabletName,
      tabletApp,
      tabletDepartment,
      endpoint,
      username,
      password,
      id
    );

    return rows;
  }

  /**
   * Disabilita una configurazione tablet (soft delete).
   *
   * @param id ID record
   * @return numero righe modificate (1 se disabilitato, 0 se non trovato)
   * @throws Exception se errore database
   */
  public int disable(Long id) throws Exception
  {
    String sql;
    int rows;

    sql = "UPDATE aes_tablet_config SET enabled = false, updated_at = CURRENT_TIMESTAMP WHERE id = ?";
    rows = db.query(sql, id);

    return rows;
  }

  /**
   * Elimina permanentemente una configurazione tablet (hard delete).
   * <p>
   * Usare {@link #disable(Long)} per soft delete (raccomandato per audit).
   * </p>
   *
   * @param id ID record
   * @return numero righe eliminate (1 se eliminato, 0 se non trovato)
   * @throws Exception se errore database
   */
  public int delete(Long id) throws Exception
  {
    String sql;
    int rows;

    sql = "DELETE FROM aes_tablet_config WHERE id = ?";
    rows = db.query(sql, id);

    return rows;
  }

  /**
   * Mappa una riga database a DTO.
   *
   * @param row riga database
   * @return DTO AesTabletConfig
   */
  private AesTabletConfig mapRow(HashMap<String, Object> row)
  {
    Long id;
    Long accountId;
    String tabletId;
    String tabletName;
    String tabletApp;
    String tabletDepartment;
    String provider;
    String endpoint;
    String username;
    String password;
    Boolean enabled;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    id = DB.toLong(row.get("id"));
    accountId = DB.toLong(row.get("account_id"));
    tabletId = (String) row.get("tablet_id");
    tabletName = (String) row.get("tablet_name");
    tabletApp = (String) row.get("tablet_app");
    tabletDepartment = (String) row.get("tablet_department");
    provider = (String) row.get("provider");
    endpoint = (String) row.get("endpoint");
    username = (String) row.get("username");
    password = (String) row.get("password");
    enabled = DB.toBoolean(row.get("enabled"));
    createdAt = DB.toLocalDateTime(row.get("created_at"));
    updatedAt = DB.toLocalDateTime(row.get("updated_at"));

    return new AesTabletConfig(
      id,
      accountId,
      tabletId,
      tabletName,
      tabletApp,
      tabletDepartment,
      provider,
      endpoint,
      username,
      password,
      enabled,
      createdAt,
      updatedAt
    );
  }
}
