package {{APP_PACKAGE}}.contatti.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/** GET /api/import/campi — elenco dei campi importabili del sistema. */
public class ImportCampiHandler implements Handler
{
  private static final Log log = Log.get(ImportCampiHandler.class);

  @Override
  public void get(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String token;
    List<HashMap<String, String>> campi;

    token = req.getCookie("access_token");
    if (token == null) {
      res.status(200).contentType("application/json").err(true).log("Non autenticato").out(null).send();
      return;
    }
    try {
      Auth.get().verifyAccessToken(token);
    } catch (JWTVerificationException e) {
      res.status(200).contentType("application/json").err(true).log("Token non valido o scaduto").out(null).send();
      return;
    }

    campi = new ArrayList<>();
    campi.add(campo("nome",            "Nome"));
    campi.add(campo("cognome",         "Cognome"));
    campi.add(campo("ragione_sociale", "Ragione Sociale"));
    campi.add(campo("telefono",        "Telefono"));
    campi.add(campo("email",           "Email"));
    campi.add(campo("indirizzo",       "Indirizzo"));
    campi.add(campo("citta",           "Città"));
    campi.add(campo("cap",             "CAP"));
    campi.add(campo("provincia",       "Provincia"));
    campi.add(campo("note",            "Note"));

    res.status(200).contentType("application/json").err(false).log(null).out(campi).send();
  }

  private static HashMap<String, String> campo(String key, String label)
  {
    HashMap<String, String> m = new HashMap<>();
    m.put("key",   key);
    m.put("label", label);
    return m;
  }
}
