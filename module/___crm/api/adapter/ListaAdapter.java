package dev.jms.app.crm.adapter;

import dev.jms.app.crm.dto.ListaDTO;
import dev.jms.util.HttpRequest;
import dev.jms.util.Json;
import dev.jms.util.Validator;

import java.util.HashMap;

/** Deserializza i dati di una lista dal body JSON della richiesta. */
public class ListaAdapter
{
  private ListaAdapter() {}

  /** Legge e valida i campi della lista dal body JSON. Lancia ValidationException se obbligatori mancanti. */
  @SuppressWarnings("unchecked")
  public static ListaDTO from(HttpRequest req) throws Exception
  {
    HashMap<String, Object> body;
    String nome;
    String descrizione;
    boolean consenso;
    int stato;
    String scadenza;

    boolean isDefault;

    body        = Json.decode(req.getBody(), HashMap.class);
    nome        = (String) body.get("nome");
    descrizione = (String) body.get("descrizione");
    consenso    = Boolean.TRUE.equals(body.get("consenso"));
    stato       = body.get("stato") instanceof Number ? ((Number) body.get("stato")).intValue() : 1;
    scadenza    = (String) body.get("scadenza");
    isDefault   = Boolean.TRUE.equals(body.get("isDefault"));

    Validator.required(nome, "nome");
    Validator.maxLength(nome, 100, "nome");

    return new ListaDTO(null, nome, descrizione, consenso, stato, scadenza, null, null, null, isDefault, 0);
  }
}
