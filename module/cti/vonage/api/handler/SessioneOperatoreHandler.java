package dev.jms.app.module.cti.vonage.handler;

import dev.jms.app.module.cti.vonage.dao.OperatorDAO;
import dev.jms.app.module.cti.vonage.dao.SessioneOperatoreDAO;
import dev.jms.app.module.cti.vonage.dto.OperatorDTO;
import dev.jms.app.module.cti.vonage.dto.SessioneOperatoreDTO;
import dev.jms.util.DB;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;
import dev.jms.util.Permission;
import dev.jms.util.Role;
import dev.jms.util.Session;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Handler per la lettura delle sessioni tecniche CTI ({@code jms_cti_sessione_operatore}).
 *
 * <p>Le sessioni vengono aperte automaticamente all'accesso CTI dell'operatore.
 * La pianificazione dei turni è gestita dal modulo CRM.</p>
 */
public class SessioneOperatoreHandler
{
  private static final Log log = Log.get(SessioneOperatoreHandler.class);

  /**
   * GET /api/cti/vonage/admin/sessioni — lista paginata di tutte le sessioni tecniche.
   *
   * <p>Query params: {@code page} (default 1), {@code size} (default 20).</p>
   * <p>Richiede ruolo ADMIN.</p>
   */
  public void list(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    int page;
    int size;
    int total;
    String pageParam;
    String sizeParam;
    List<SessioneOperatoreDTO> items;
    List<HashMap<String, Object>> out;
    SessioneOperatoreDAO dao;
    HashMap<String, Object> envelope;

    session.require(Role.ADMIN, Permission.READ);
    pageParam = req.queryParam("page");
    sizeParam = req.queryParam("size");
    page  = (pageParam != null && !pageParam.isBlank()) ? Integer.parseInt(pageParam) : 1;
    size  = (sizeParam != null && !sizeParam.isBlank()) ? Integer.parseInt(sizeParam) : 20;
    dao   = new SessioneOperatoreDAO(db);
    total = dao.count();
    items = dao.findAll(page, size);
    out   = new ArrayList<>();
    for (SessioneOperatoreDTO s : items) {
      out.add(toMap(s));
    }
    envelope = new HashMap<>();
    envelope.put("total", total);
    envelope.put("page",  page);
    envelope.put("size",  size);
    envelope.put("items", out);
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(envelope)
       .send();
  }

  /**
   * GET /api/cti/vonage/sessione/corrente — sessione tecnica attiva dell'operatore autenticato.
   *
   * <p>Cerca la sessione con stato 1 (connesso), 2 (in pausa) o 3 (in chiamata).
   * Risponde con la sessione o {@code null} se nessuna sessione è attiva.</p>
   * <p>Richiede ruolo USER.</p>
   */
  public void corrente(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    long accountId;
    OperatorDAO opDao;
    OperatorDTO op;
    SessioneOperatoreDAO dao;
    SessioneOperatoreDTO sessione;

    session.require(Role.USER, Permission.READ);
    accountId = session.sub();
    opDao     = new OperatorDAO(db);
    op        = opDao.findByClaimAccountId((int) accountId);

    if (op == null) {
      res.status(200)
         .contentType("application/json")
         .err(false)
         .log(null)
         .out(null)
         .send();
      return;
    }

    dao     = new SessioneOperatoreDAO(db);
    sessione = dao.findActive(op.id());
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(sessione != null ? toMap(sessione) : null)
       .send();
  }

  /** Converte un DTO in mappa serializzabile JSON. */
  private HashMap<String, Object> toMap(SessioneOperatoreDTO s)
  {
    HashMap<String, Object> m;

    m = new HashMap<>();
    m.put("id",                  s.id());
    m.put("operatoreId",         s.operatoreId());
    m.put("connessioneInizio",   s.connessioneInizio());
    m.put("connessioneFine",     s.connessioneFine());
    m.put("durataTotale",        s.durataTotale());
    m.put("numeroPause",         s.numeroPause());
    m.put("durataPause",         s.durataPause());
    m.put("ultimaConnessione",   s.ultimaConnessione());
    m.put("numeroChiamate",      s.numeroChiamate());
    m.put("durataConversazione", s.durataConversazione());
    m.put("stato",               s.stato());
    m.put("creatoDA",            s.creatoDA());
    m.put("dataCreazione",       s.dataCreazione());
    m.put("modificatoDA",        s.modificatoDA());
    m.put("dataModifica",        s.dataModifica());
    return m;
  }
}
