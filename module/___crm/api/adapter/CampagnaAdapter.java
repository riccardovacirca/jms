package dev.jms.app.crm.adapter;

import dev.jms.app.crm.dto.CampagnaDTO;
import dev.jms.util.HttpRequest;
import dev.jms.util.Json;
import dev.jms.util.Validator;

import java.util.HashMap;

/** Deserializza i dati di una campagna dal body JSON della richiesta. */
public class CampagnaAdapter
{
  private CampagnaAdapter() {}

  /** Legge e valida i campi della campagna dal body JSON. Lancia ValidationException se obbligatori mancanti. */
  @SuppressWarnings("unchecked")
  public static CampagnaDTO from(HttpRequest req) throws Exception
  {
    HashMap<String, Object> body;
    String nome;
    String descrizione;
    int stato;

    body        = Json.decode(req.getBody(), HashMap.class);
    nome        = (String) body.get("nome");
    descrizione = (String) body.get("descrizione");
    stato       = body.get("stato") instanceof Number ? ((Number) body.get("stato")).intValue() : 1;

    Validator.required(nome, "nome");
    Validator.maxLength(nome, 100, "nome");

    return new CampagnaDTO(null, nome, descrizione, stato, null, null, null, 0);
  }
}
