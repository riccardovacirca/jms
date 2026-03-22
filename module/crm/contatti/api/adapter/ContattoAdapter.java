package dev.jms.app.contatti.adapter;

import dev.jms.app.contatti.dto.ContattoDTO;
import dev.jms.util.HttpRequest;
import dev.jms.util.Json;
import dev.jms.util.ValidationException;
import dev.jms.util.Validator;

import java.util.HashMap;

/** Deserializza i dati di un contatto dal body JSON della richiesta. */
public class ContattoAdapter
{
  /** Legge e valida i campi del contatto dal body JSON. Lancia ValidationException se obbligatori mancanti. */
  @SuppressWarnings("unchecked")
  public static ContattoDTO from(HttpRequest req) throws Exception
  {
    HashMap<String, Object> body;
    String nome;
    String cognome;
    String ragioneSociale;
    String telefono;
    int stato;
    boolean consenso;
    boolean blacklist;

    body           = Json.decode(req.getBody(), HashMap.class);
    nome           = (String) body.get("nome");
    cognome        = (String) body.get("cognome");
    ragioneSociale = (String) body.get("ragione_sociale");
    telefono       = Validator.required((String) body.get("telefono"), "telefono");
    stato          = body.get("stato") != null ? ((Number) body.get("stato")).intValue() : 1;
    consenso       = Boolean.TRUE.equals(body.get("consenso"));
    blacklist      = Boolean.TRUE.equals(body.get("blacklist"));

    if (isBlank(nome) && isBlank(cognome) && isBlank(ragioneSociale)) {
      throw new ValidationException("Almeno uno tra nome, cognome e ragione sociale è obbligatorio");
    }

    return new ContattoDTO(
      null,
      nome,
      cognome,
      ragioneSociale,
      telefono,
      (String) body.get("email"),
      (String) body.get("indirizzo"),
      (String) body.get("citta"),
      (String) body.get("cap"),
      (String) body.get("provincia"),
      (String) body.get("note"),
      stato,
      consenso,
      blacklist,
      null, null, 0L
    );
  }

  private static boolean isBlank(String s)
  {
    return s == null || s.isBlank();
  }
}
