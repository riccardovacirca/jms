package dev.jms.util;

import com.auth0.jwt.interfaces.DecodedJWT;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Rappresenta la sessione per una singola richiesta HTTP.
 *
 * <p>Gestisce due aspetti distinti della sessione:
 * <ul>
 *   <li><b>JWT</b>: validazione lazy del token di accesso, con accesso ai claims e ai ruoli.</li>
 *   <li><b>Storage server-side</b>: {@link ConcurrentHashMap} in memoria condivisa tra richieste,
 *       identificata da un cookie {@code session_id} con TTL sliding (default 30 minuti).</li>
 * </ul>
 *
 * <p>Istanziata da {@link HandlerAdapter} per ogni richiesta e passata agli handler
 * come terzo argomento, prima di {@link DB}.
 *
 * <p>La validazione JWT è lazy: avviene alla prima chiamata di un metodo che richiede
 * autenticazione, e il risultato è memorizzato in cache per la durata della richiesta.
 *
 * <p>Lo storage server-side è lazy: la sessione non viene creata finché non viene
 * chiamato {@link #setAttr(String, Object)}. La lettura con {@link #getAttr(String)}
 * non crea la sessione. Il TTL viene rinnovato ad ogni risposta in cui la sessione è
 * accessibile, tramite il rinnovo automatico del cookie.
 *
 * <p>Il cleanup delle sessioni scadute avviene automaticamente ogni minuto
 * in background (thread daemon).
 *
 * <pre>{@code
 * // Uso JWT in un handler:
 * session.require(Role.ADMIN, Permission.READ);
 * long id = session.sub();
 *
 * // Uso storage in un handler:
 * session.setAttr("cart", cartItems);
 * List<?> cart = (List<?>) session.getAttr("cart");
 * }</pre>
 */
public class Session
{
  // ── Static session store ─────────────────────────────────────────────────────

  private static final ConcurrentHashMap<String, HashMap<String, Object>> store;
  private static final ConcurrentHashMap<String, Long> touched;
  private static final SecureRandom secureRandom;
  private static ScheduledExecutorService cleanupExecutor;
  private static int ttlSeconds;

  static {
    store = new ConcurrentHashMap<>();
    touched = new ConcurrentHashMap<>();
    secureRandom = new SecureRandom();
    ttlSeconds = 1800;
    cleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t;
      t = new Thread(r);
      t.setName("session-cleanup");
      t.setDaemon(true);
      return t;
    });
    cleanupExecutor.scheduleAtFixedRate(Session::cleanup, 60, 60, TimeUnit.SECONDS);
  }

  // ── Per-request JWT state ────────────────────────────────────────────────────

  private final HttpRequest req;
  private Map<String, Object> _claims;
  private boolean _resolved;

  // ── Per-request storage state ────────────────────────────────────────────────

  private String _sessionId;
  private HashMap<String, Object> _attrs;
  private boolean _attrsLoaded;
  private boolean _dirty;

  // ── Constructor ──────────────────────────────────────────────────────────────

  /**
   * Costruttore package-private — istanziato da {@link HandlerAdapter}.
   *
   * @param req la request HTTP corrente
   */
  Session(HttpRequest req)
  {
    this.req = req;
    this._resolved = false;
    this._attrsLoaded = false;
    this._dirty = false;
  }

  // ── Static configuration ─────────────────────────────────────────────────────

  /**
   * Configura il TTL dello store server-side.
   * Da chiamare una volta in {@code App.main()} prima del primo request.
   *
   * @param ttl durata in secondi di inattività prima della scadenza della sessione (default: 1800)
   */
  public static void configure(int ttl)
  {
    ttlSeconds = ttl;
  }

  /**
   * Arresta il thread di cleanup dello store server-side.
   * Da chiamare nel shutdown hook dell'applicazione.
   */
  public static void shutdown()
  {
    if (cleanupExecutor != null) {
      cleanupExecutor.shutdownNow();
      System.out.println("[info] Session store terminato");
    }
  }

  // ── JWT methods ──────────────────────────────────────────────────────────────

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
    int userLevel;

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

  // ── Storage methods ──────────────────────────────────────────────────────────

  /**
   * Restituisce il valore associato alla chiave nello storage server-side.
   * Restituisce {@code null} se la sessione non esiste o la chiave non è presente.
   * Non crea una nuova sessione.
   *
   * @param key chiave dell'attributo
   * @return valore o {@code null}
   */
  public Object getAttr(String key)
  {
    HashMap<String, Object> attrs;
    Object result;

    attrs = ensureAttrs();
    result = attrs != null ? attrs.get(key) : null;
    return result;
  }

  /**
   * Imposta un valore nello storage server-side.
   * Se la sessione non esiste, la crea automaticamente.
   *
   * @param key   chiave dell'attributo
   * @param value valore da memorizzare
   */
  public void setAttr(String key, Object value)
  {
    if (!_attrsLoaded) {
      ensureAttrs();
    }
    if (_attrs == null) {
      _sessionId = generateSessionId();
      _attrs = new HashMap<>();
      store.put(_sessionId, _attrs);
    }
    _attrs.put(key, value);
    _dirty = true;
  }

  /**
   * Rimuove la chiave dallo storage server-side.
   * Non effettua operazioni se la sessione non esiste.
   *
   * @param key chiave da rimuovere
   */
  public void removeAttr(String key)
  {
    HashMap<String, Object> attrs;

    attrs = ensureAttrs();
    if (attrs != null) {
      attrs.remove(key);
      _dirty = true;
    }
  }

  /**
   * Svuota tutto lo storage server-side della sessione corrente.
   * Non elimina la sessione: il cookie e la entry nello store rimangono.
   */
  public void clearAttrs()
  {
    HashMap<String, Object> attrs;

    attrs = ensureAttrs();
    if (attrs != null) {
      attrs.clear();
      _dirty = true;
    }
  }

  /**
   * Restituisce l'ID della sessione corrente, o {@code null} se non esiste.
   * Non crea una nuova sessione.
   *
   * @return session ID o {@code null}
   */
  public String sessionId()
  {
    String result;

    ensureAttrs();
    result = _sessionId;
    return result;
  }

  // ── Package-private ──────────────────────────────────────────────────────────

  /**
   * Persiste lo storage e rinnova il cookie di sessione nella risposta corrente.
   * Invocato da {@link HandlerAdapter} tramite il pre-send hook di {@link HttpResponse}
   * prima che gli header HTTP vengano committati al client.
   * Non effettua operazioni se la sessione non è stata acceduta o non esiste.
   *
   * @param res response HTTP corrente
   */
  void flush(HttpResponse res)
  {
    if (_attrsLoaded && _sessionId != null) {
      store.put(_sessionId, _attrs);
      touched.put(_sessionId, Instant.now().getEpochSecond());
      res.cookie(Cookie.SESSION_ID, _sessionId, ttlSeconds);
    }
  }

  // ── Private JWT helpers ──────────────────────────────────────────────────────

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
    String token;
    DecodedJWT jwt;
    String jti;
    Map<String, Object> result;

    if (!_resolved) {
      _resolved = true;
      token = req.getCookie(Cookie.ACCESS_TOKEN);
      if (token != null && !token.isBlank()) {
        try {
          jwt = Auth.get().verifyAccessToken(token);
          jti = jwt.getId();
          if (jti == null || !JWTBlacklist.isRevoked(jti)) {
            result = new HashMap<>();
            result.put("sub", jwt.getSubject());
            result.put("username", jwt.getClaim("username").asString());
            result.put("ruolo", jwt.getClaim("ruolo").asString());
            result.put("ruolo_level", jwt.getClaim("ruolo_level").asInt());
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

  // ── Private storage helpers ──────────────────────────────────────────────────

  /**
   * Carica lazy lo storage dal cookie di sessione.
   * Restituisce la mappa degli attributi, o {@code null} se la sessione non esiste.
   */
  private HashMap<String, Object> ensureAttrs()
  {
    String cookieId;
    HashMap<String, Object> found;

    if (!_attrsLoaded) {
      _attrsLoaded = true;
      cookieId = req.getCookie(Cookie.SESSION_ID);
      if (cookieId != null) {
        found = store.get(cookieId);
        if (found != null) {
          _sessionId = cookieId;
          _attrs = found;
        }
      }
    }
    return _attrs;
  }

  /** Genera un session ID casuale sicuro (64 caratteri hex). */
  private static String generateSessionId()
  {
    byte[] bytes;
    String result;

    bytes = new byte[32];
    secureRandom.nextBytes(bytes);
    result = HexFormat.of().formatHex(bytes);
    return result;
  }

  /** Rimuove dallo store le sessioni con TTL scaduto. Eseguito ogni minuto. */
  private static void cleanup()
  {
    long now;
    long cutoff;

    now = Instant.now().getEpochSecond();
    cutoff = now - ttlSeconds;
    for (Map.Entry<String, Long> entry : touched.entrySet()) {
      if (entry.getValue() < cutoff) {
        store.remove(entry.getKey());
        touched.remove(entry.getKey());
      }
    }
  }
}
