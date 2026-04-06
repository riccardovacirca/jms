package dev.jms.app.module.cti.vonage.handler;

import dev.jms.app.module.cti.vonage.dao.PrefissoDAO;
import dev.jms.util.DB;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Permission;
import dev.jms.util.Role;
import dev.jms.util.Session;
import java.util.HashMap;
import java.util.List;

/**
 * Handler per la lettura dei prefissi telefonici internazionali.
 */
public class PrefissoHandler
{
  /**
   * GET /api/cti/vonage/prefissi — restituisce tutti i prefissi internazionali attivi.
   *
   * <p>Risposta: array di oggetti {@code {"id": n, "paese": "...", "iso": "IT", "prefisso": "+39"}}
   * ordinati per paese.</p>
   */
  public void list(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    List<HashMap<String, Object>> items;
    PrefissoDAO dao;

    session.require(Role.USER, Permission.READ);
    dao = new PrefissoDAO(db);
    items = dao.findAllAttivi();
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(items)
       .send();
  }
}
