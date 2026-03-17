package dev.jms.app.contatti.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import dev.jms.app.contatti.adapter.ContattoAdapter;
import dev.jms.app.contatti.dao.ContattoDAO;
import dev.jms.app.contatti.dto.ContattoDTO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;
import dev.jms.util.ValidationException;

/** GET /api/contatti/{id}    — recupera un contatto.
 *  PUT /api/contatti/{id}    — aggiorna un contatto.
 *  DELETE /api/contatti/{id} — elimina un contatto. */
public class ContattoHandler implements Handler
{
  private static final Log log = Log.get(ContattoHandler.class);

  /** Restituisce il contatto per id, 404 se non trovato. */
  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    int id;
    ContattoDAO dao;
    ContattoDTO contatto;

    token = req.getCookie("access_token");
    if (token == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Non autenticato")
         .out(null)
         .send();
    } else {
      try {
        Auth.get().verifyAccessToken(token);
        id = Integer.parseInt(req.urlArgs().get("id"));
        dao = new ContattoDAO(db);
        contatto = dao.findById(id);
        if (contatto == null) {
          res.status(200)
             .contentType("application/json")
             .err(true)
             .log("Contatto non trovato")
             .out(null)
             .send();
        } else {
          res.status(200)
             .contentType("application/json")
             .err(false)
             .log(null)
             .out(contatto)
             .send();
        }
      } catch (JWTVerificationException e) {
        res.status(200)
           .contentType("application/json")
           .err(true)
           .log("Token non valido o scaduto")
           .out(null)
           .send();
      }
    }
  }

  /** Aggiorna tutti i campi del contatto. */
  @Override
  public void put(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    int id;
    ContattoDAO dao;
    ContattoDTO existing;
    ContattoDTO updated;

    token = req.getCookie("access_token");
    if (token == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Non autenticato")
         .out(null)
         .send();
    } else {
      try {
        Auth.get().verifyAccessToken(token);
        id = Integer.parseInt(req.urlArgs().get("id"));
        dao = new ContattoDAO(db);
        existing = dao.findById(id);
        if (existing == null) {
          res.status(200)
             .contentType("application/json")
             .err(true)
             .log("Contatto non trovato")
             .out(null)
             .send();
        } else {
          try {
            updated = ContattoAdapter.from(req);
            if (dao.existsByTelefono(updated.telefono(), id)) {
              res.status(200)
                 .contentType("application/json")
                 .err(true)
                 .log("Telefono già esistente")
                 .out(null)
                 .send();
            } else {
              updated = new ContattoDTO(
                id,
                updated.nome(), updated.cognome(), updated.ragioneSociale(),
                updated.telefono(), updated.email(), updated.indirizzo(),
                updated.citta(), updated.cap(), updated.provincia(), updated.note(),
                updated.stato(), updated.consenso(), updated.blacklist(),
                existing.createdAt(), null, existing.listeCount()
              );
              dao.update(updated);
              res.status(200)
                 .contentType("application/json")
                 .err(false)
                 .log(null)
                 .out(null)
                 .send();
            }
          } catch (ValidationException e) {
            res.status(200)
               .contentType("application/json")
               .err(true)
               .log(e.getMessage())
               .out(null)
               .send();
          }
        }
      } catch (JWTVerificationException e) {
        res.status(200)
           .contentType("application/json")
           .err(true)
           .log("Token non valido o scaduto")
           .out(null)
           .send();
      }
    }
  }

  /** Elimina il contatto per id. */
  @Override
  public void delete(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    int id;
    ContattoDAO dao;
    ContattoDTO existing;

    token = req.getCookie("access_token");
    if (token == null) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Non autenticato")
         .out(null)
         .send();
    } else {
      try {
        Auth.get().verifyAccessToken(token);
        id = Integer.parseInt(req.urlArgs().get("id"));
        dao = new ContattoDAO(db);
        existing = dao.findById(id);
        if (existing == null) {
          res.status(200)
             .contentType("application/json")
             .err(true)
             .log("Contatto non trovato")
             .out(null)
             .send();
        } else {
          dao.delete(id);
          res.status(200)
             .contentType("application/json")
             .err(false)
             .log(null)
             .out(null)
             .send();
        }
      } catch (JWTVerificationException e) {
        res.status(200)
           .contentType("application/json")
           .err(true)
           .log("Token non valido o scaduto")
           .out(null)
           .send();
      }
    }
  }
}
