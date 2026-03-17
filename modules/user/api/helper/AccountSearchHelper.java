package dev.jms.app.user.helper;

import dev.jms.app.user.dao.AccountDAO;
import dev.jms.util.DB;

import java.util.HashMap;
import java.util.List;

/**
 * Helper per le operazioni di ricerca e paginazione degli account.
 * Centralizza la logica di get usata da AccountHandler.getEntries().
 */
public class AccountSearchHelper
{
  private static final int DEFAULT_PAGE_SIZE = 50;

  private final DB db;

  /** Costruttore. */
  public AccountSearchHelper(DB db)
  {
    this.db = db;
  }

  /**
   * Restituisce una pagina di account con conteggio totale.
   *
   * @param search   stringa di ricerca su username/email (null o blank = nessun filtro)
   * @param page     numero di pagina (1-based)
   * @param pageSize dimensione pagina (0 = DEFAULT_PAGE_SIZE)
   * @return mappa con chiavi "items" (List), "total" (long), "page" (int), "pageSize" (int)
   */
  public HashMap<String, Object> getEntries(String search, int page, int pageSize) throws Exception
  {
    AccountDAO dao;
    int size;
    int offset;
    List<HashMap<String, Object>> items;
    long total;
    HashMap<String, Object> result;

    dao    = new AccountDAO(db);
    size   = pageSize > 0 ? pageSize : DEFAULT_PAGE_SIZE;
    offset = (Math.max(page, 1) - 1) * size;
    items  = dao.findAll(search, offset, size);
    total  = dao.count(search);

    result = new HashMap<>();
    result.put("items",    items);
    result.put("total",    total);
    result.put("page",     Math.max(page, 1));
    result.put("pageSize", size);
    return result;
  }

  /**
   * Restituisce un account per id (management). Null se non trovato.
   *
   * @param id identificativo account
   */
  public HashMap<String, Object> getEntryById(long id) throws Exception
  {
    return new AccountDAO(db).findByIdManagement(id);
  }
}
