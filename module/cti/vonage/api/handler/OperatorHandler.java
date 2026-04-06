package dev.jms.app.module.cti.vonage.handler;

import com.vonage.client.users.UsersResponseException;
import dev.jms.app.module.cti.vonage.dao.OperatorDAO;
import dev.jms.app.module.cti.vonage.dto.OperatorDTO;
import dev.jms.app.module.cti.vonage.helper.VoiceHelper;
import dev.jms.util.Config;
import dev.jms.util.DB;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;
import dev.jms.util.Permission;
import dev.jms.util.Role;
import dev.jms.util.Session;
import dev.jms.util.ValidationException;
import dev.jms.util.Validator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Handler per la gestione degli operatori CTI (CRUD + sync con Vonage).
 */
public class OperatorHandler
{
  private static final Log log = Log.get(OperatorHandler.class);

  private final VoiceHelper voiceHelper;

  /**
   * @param config configurazione applicazione (credenziali Vonage)
   */
  public OperatorHandler(Config config)
  {
    this.voiceHelper = new VoiceHelper(config);
  }

  /**
   * POST /api/cti/vonage/admin/operator — crea un utente Vonage e lo registra come operatore.
   *
   * <p>Crea l'utente nell'applicazione Vonage tramite {@code UsersClient} e inserisce
   * la riga corrispondente in {@code cti_operatori}. Il campo {@code name} diventa
   * il {@code vonage_user_id} (claim {@code sub} del JWT SDK).</p>
   *
   * <p>Richiede autenticazione con ruolo ADMIN.</p>
   *
   * <p>Body JSON: {@code {"name": "operatore_01", "displayName": "Operatore 01"}}
   * — {@code displayName} è opzionale.</p>
   *
   * <p>Risposta: {@code {"vonageUserId": "...", "nome": "...", "attivo": true}}.</p>
   */
  public void create(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    HashMap<String, Object> body;
    String name;
    String displayName;
    String vonageUserId;
    OperatorDAO operatorDao;
    HashMap<String, Object> out;

    session.require(Role.ADMIN, Permission.WRITE);
    body = req.body();
    name = DB.toString(body.get("name"));
    displayName = DB.toString(body.get("displayName"));

    try {
      Validator.required(name, "name");
      vonageUserId = voiceHelper.createVonageUser(name, displayName);
      operatorDao = new OperatorDAO(db);
      operatorDao.insert(vonageUserId, displayName);
      out = new HashMap<>();
      out.put("vonageUserId", vonageUserId);
      out.put("nome", displayName);
      out.put("attivo", true);
      res.status(200)
         .contentType("application/json")
         .err(false)
         .log(null)
         .out(out)
         .send();
    } catch (ValidationException | UsersResponseException e) {
      log.warn("[CTI] createOperator: {}", e.getMessage());
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log(e.getMessage())
         .out(null)
         .send();
    }
  }

  /**
   * GET /api/cti/vonage/admin/operator — lista tutti gli operatori registrati localmente.
   *
   * <p>Richiede autenticazione con ruolo ADMIN.</p>
   */
  public void list(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    List<OperatorDTO> operators;
    List<HashMap<String, Object>> out;
    OperatorDAO dao;

    session.require(Role.ADMIN, Permission.READ);
    dao = new OperatorDAO(db);
    operators = dao.findAll();
    out = new ArrayList<>();
    for (OperatorDTO op : operators) {
      out.add(toMap(op));
    }
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(out)
       .send();
  }

  /**
   * GET /api/cti/vonage/admin/operator/{id} — restituisce un operatore per id.
   *
   * <p>Richiede autenticazione con ruolo ADMIN.</p>
   */
  public void get(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    long id;
    OperatorDTO op;
    OperatorDAO dao;

    session.require(Role.ADMIN, Permission.READ);
    id = Long.parseLong(req.urlArgs().get("id"));
    dao = new OperatorDAO(db);
    op = dao.findById(id);
    if (op == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Operatore non trovato")
         .out(null)
         .send();
    } else {
      res.status(200)
         .contentType("application/json")
         .err(false)
         .log(null)
         .out(toMap(op))
         .send();
    }
  }

  /**
   * PUT /api/cti/vonage/admin/operator/{id} — aggiorna nome e stato attivo di un operatore.
   *
   * <p>Richiede autenticazione con ruolo ADMIN.</p>
   * <p>Body JSON: {@code {"nome": "...", "attivo": true|false}}.</p>
   */
  public void update(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    long id;
    HashMap<String, Object> body;
    String nome;
    Boolean attivo;
    OperatorDAO dao;
    OperatorDTO op;

    session.require(Role.ADMIN, Permission.WRITE);
    id = Long.parseLong(req.urlArgs().get("id"));
    dao = new OperatorDAO(db);
    op = dao.findById(id);
    if (op == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Operatore non trovato")
         .out(null)
         .send();
    } else {
      body = req.body();
      nome = body.containsKey("nome") ? DB.toString(body.get("nome")) : op.nome();
      attivo = body.containsKey("attivo") ? DB.toBoolean(body.get("attivo")) : op.attivo();
      dao.update(id, nome, attivo);
      op = dao.findById(id);
      res.status(200)
         .contentType("application/json")
         .err(false)
         .log(null)
         .out(toMap(op))
         .send();
    }
  }

  /**
   * DELETE /api/cti/vonage/admin/operator/{id} — elimina un operatore locale e il corrispondente
   * utente su Vonage.
   *
   * <p>Richiede autenticazione con ruolo ADMIN.</p>
   */
  public void delete(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    long id;
    OperatorDAO dao;
    OperatorDTO op;

    session.require(Role.ADMIN, Permission.WRITE);
    id = Long.parseLong(req.urlArgs().get("id"));
    dao = new OperatorDAO(db);
    op = dao.findById(id);
    if (op == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Operatore non trovato")
         .out(null)
         .send();
    } else {
      voiceHelper.deleteVonageUser(op.vonageUserId());
      dao.delete(id);
      log.info("[CTI] deleteOperator: id={}, vonageUserId={}", id, op.vonageUserId());
      res.status(200)
         .contentType("application/json")
         .err(false)
         .log(null)
         .out(null)
         .send();
    }
  }

  /**
   * POST /api/cti/vonage/admin/operator/sync — allinea gli operatori locali agli utenti
   * registrati su Vonage.
   *
   * <p>Per ogni utente presente su Vonage ma assente in {@code jms_cti_operatori},
   * crea il record locale. Gli operatori locali senza corrispondente su Vonage
   * non vengono toccati. Restituisce la lista degli operatori creati.</p>
   *
   * <p>Richiede autenticazione con ruolo ADMIN.</p>
   */
  public void sync(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    List<HashMap<String, Object>> vonageUsers;
    OperatorDAO dao;
    List<HashMap<String, Object>> created;
    String name;
    String displayName;
    OperatorDTO existing;
    long newId;
    HashMap<String, Object> entry;

    session.require(Role.ADMIN, Permission.WRITE);
    vonageUsers = voiceHelper.listVonageUsers();
    dao = new OperatorDAO(db);
    created = new ArrayList<>();
    for (HashMap<String, Object> u : vonageUsers) {
      name = DB.toString(u.get("name"));
      displayName = DB.toString(u.get("displayName"));
      existing = dao.findByVonageUserId(name);
      if (existing == null) {
        newId = dao.insert(name, displayName);
        entry = new HashMap<>();
        entry.put("id", newId);
        entry.put("vonageUserId", name);
        entry.put("nome", displayName);
        entry.put("attivo", true);
        created.add(entry);
        log.info("[CTI] syncOperators: creato operatore locale vonageUserId={}", name);
      }
    }
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(created)
       .send();
  }

  /**
   * Converte un {@link OperatorDTO} in una mappa serializzabile JSON.
   *
   * @param op operatore da convertire
   * @return mappa con i campi dell'operatore
   */
  private HashMap<String, Object> toMap(OperatorDTO op)
  {
    HashMap<String, Object> m;

    m = new HashMap<>();
    m.put("id", op.id());
    m.put("vonageUserId", op.vonageUserId());
    m.put("accountId", op.accountId());
    m.put("nome", op.nome());
    m.put("attivo", op.attivo());
    return m;
  }
}
