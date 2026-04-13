package dev.jms.app.module.cti.vonage.dao;

import dev.jms.util.DB;
import java.util.HashMap;
import java.util.List;

/**
 * DAO per la tabella {@code jms_cti_prefissi_internazionali}.
 */
public class PrefissoDAO
{
  private final DB db;

  /**
   * @param db connessione DB iniettata dall'handler
   */
  public PrefissoDAO(DB db)
  {
    this.db = db;
  }

  /**
   * Restituisce tutti i prefissi internazionali attivi, ordinati per paese.
   *
   * @return lista di righe con campi {@code id}, {@code paese}, {@code iso}, {@code prefisso}
   */
  public List<HashMap<String, Object>> findAllAttivi() throws Exception
  {
    String sql;

    sql = "SELECT id, paese, iso, prefisso FROM jms_cti_prefissi_internazionali "
        + "WHERE attivo = TRUE ORDER BY paese";
    return db.select(sql);
  }
}
