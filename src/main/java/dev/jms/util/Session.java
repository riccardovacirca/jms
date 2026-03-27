package dev.jms.util;

import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.HashMap;
import java.util.Map;

/**
 * Rappresenta la sessione JWT per una singola richiesta HTTP.
 *
 * <p>Istanziata da {@link HandlerAdapter} per ogni richiesta e passata agli handler
 * come terzo argomento, prima di {@link DB}.
 *
 * <p>La validazione del token è lazy: avviene alla prima chiamata di un metodo
 * che richiede autenticazione, e il risultato è memorizzato in cache per la
 * durata della richiesta.
 *
 * <pre>{@code
 * // Esempio d'uso in un handler:
 * session.require(Role.ADMIN, Permission.READ);
 * long id = session.sub();
 * }</pre>
 */
public class Session
{
  private final HttpRequest req;
  private Map<String, Object> _claims;
  private boolean _resolved;

  /**
   * Costruttore package-private — istanziato da {@link HandlerAdapter}.
   *
   * @param req la request HTTP corrente
   */
  Session(HttpRequest req)
  {
    this.req      = req;
    this._resolved = false;
  }

  /**
   * Verifica che la sessione soddisfi il livello minimo richiesto.
   * Lancia {@link UnauthorizedException} intercettata da {@link HandlerAdapter} → HTTP 401.
   *
   * <p>Matrice di accesso:
   * <ul>
   *   <li>{@code GUEST + READ}  — pubblico, nessun JWT richiesto.</li>
   *   <li>{@code USER  + READ}  — qualsiasi utente autenticato.</li>
   *   <li>{@code USER  + WRITE} — qualsiasi utente autenticato.</li>
   *   <li>{@code ADMIN + READ}  — solo admin o root.</li>
   *   <li>{@code ADMIN + WRITE} — solo admin o root.</li>
   *   <li>{@code ROOT  + WRITE} — solo root.</li>
   * </ul>
   *
   * <p>Nota: {@code GUEST + WRITE} non è un accesso valido — usare almeno {@code USER + WRITE}.
   *
   * @param minRole    livello minimo di ruolo richiesto
   * @param permission tipo di permesso richiesto
   * @throws UnauthorizedException se non autenticato o livello di ruolo insufficiente
   */
  public void require(Role minRole, Permission permission)
  {
    Map<String, Object> c;
    int                 userLevel;

    if (minRole != Role.GUEST || permission != Permission.READ) {
      c = resolve();
      if (c.isEmpty()) {
        throw new UnauthorizedException("Non autenticato");
      }
      userLevel = (int) c.get("ruolo_level");
      if (userLevel < minRole.level()) {
        throw new UnauthorizedException("Accesso non autorizzato");
      }
    }
  }

  /**
   * Restituisce {@code true} se il JWT presente è valido e non revocato.
   * Non lancia eccezioni.
   *
   * @return {@code true} se autenticato
   */
  public boolean isAuthenticated()
  {
    return !resolve().isEmpty();
  }

  /**
   * Restituisce l'ID account (claim {@code sub}) della sessione corrente.
   *
   * @return account id
   * @throws UnauthorizedException se non autenticato
   */
  public long sub()
  {
    return Long.parseLong(requireClaims().get("sub").toString());
  }

  /**
   * Restituisce lo username della sessione corrente.
   *
   * @return username
   * @throws UnauthorizedException se non autenticato
   */
  public String username()
  {
    return (String) requireClaims().get("username");
  }

  /**
   * Restituisce il ruolo della sessione corrente.
   *
   * @return nome del ruolo
   * @throws UnauthorizedException se non autenticato
   */
  public String ruolo()
  {
    return (String) requireClaims().get("ruolo");
  }

  /**
   * Restituisce il livello numerico del ruolo della sessione corrente.
   *
   * @return livello del ruolo
   * @throws UnauthorizedException se non autenticato
   */
  public int ruoloLevel()
  {
    return (int) requireClaims().get("ruolo_level");
  }

  /**
   * Restituisce il flag {@code must_change_password} della sessione corrente.
   *
   * @return {@code true} se l'utente deve cambiare la password
   * @throws UnauthorizedException se non autenticato
   */
  public boolean mustChangePassword()
  {
    return (boolean) requireClaims().get("must_change_password");
  }

  /**
   * Restituisce tutti i claims JWT della sessione corrente.
   * Restituisce mappa vuota se non autenticato. Non lancia eccezioni.
   *
   * @return claims JWT, o mappa vuota
   */
  public Map<String, Object> claims()
  {
    return resolve();
  }

  // ── private ─────────────────────────────────────────────────────────────────

  /** Restituisce i claims lanciando {@link UnauthorizedException} se non autenticato. */
  private Map<String, Object> requireClaims()
  {
    Map<String, Object> c;

    c = resolve();
    if (c.isEmpty()) {
      throw new UnauthorizedException("Non autenticato");
    }
    return c;
  }

  /**
   * Valida il token JWT lazy e mette in cache il risultato.
   * Restituisce mappa vuota se il token è assente, non valido o revocato.
   */
  private Map<String, Object> resolve()
  {
    String              token;
    DecodedJWT          jwt;
    String              jti;
    Map<String, Object> result;

    if (!_resolved) {
      _resolved = true;
      token     = req.getCookie(Cookie.ACCESS_TOKEN);
      if (token != null && !token.isBlank()) {
        try {
          jwt = Auth.get().verifyAccessToken(token);
          jti = jwt.getId();
          if (jti == null || !JWTBlacklist.isRevoked(jti)) {
            result = new HashMap<>();
            result.put("sub",                  jwt.getSubject());
            result.put("username",             jwt.getClaim("username").asString());
            result.put("ruolo",                jwt.getClaim("ruolo").asString());
            result.put("ruolo_level",          jwt.getClaim("ruolo_level").asInt());
            result.put("must_change_password", jwt.getClaim("must_change_password").asBoolean());
            _claims = result;
          } else {
            _claims = new HashMap<>();
          }
        } catch (com.auth0.jwt.exceptions.JWTVerificationException e) {
          _claims = new HashMap<>();
        }
      } else {
        _claims = new HashMap<>();
      }
    }
    return _claims;
  }
}
