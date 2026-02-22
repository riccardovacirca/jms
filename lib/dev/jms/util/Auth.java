package dev.jms.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

/**
 * Classe di utilità per autenticazione.
 * Wrappa com.auth0:java-jwt per la gestione dei token JWT (HS256)
 * e usa PBKDF2WithHmacSHA256 (stdlib Java) per l'hashing delle password.
 *
 * Inizializzare una volta all'avvio con Auth.init(), poi usare Auth.get()
 * ovunque nel codice.
 */
public class Auth
{
  /** Durata del refresh token in secondi (7 giorni). */
  public static final int REFRESH_EXPIRY = 7 * 24 * 60 * 60;

  private static Auth instance;

  private final Algorithm algorithm;
  private final JWTVerifier verifier;
  private final int accessExpiry;

  private Auth(String secret, int accessExpirySeconds)
  {
    this.algorithm = Algorithm.HMAC256(secret);
    this.verifier = JWT.require(algorithm).build();
    this.accessExpiry = accessExpirySeconds;
  }

  /** Da chiamare una volta in App.main() prima di avviare il server. */
  public static void init(String secret, int accessExpirySeconds)
  {
    instance = new Auth(secret, accessExpirySeconds);
  }

  public static Auth get()
  {
    if (instance == null) {
      throw new IllegalStateException("Auth non inizializzato — chiamare Auth.init()");
    }
    return instance;
  }

  // -------------------------
  // ACCESS TOKEN (JWT HS256)
  // -------------------------

  /** Crea un JWT firmato con userId, username, ruolo, flag di autorizzazione e must_change_password. */
  public String createAccessToken(int userId, String username, String ruolo,
                                  boolean canAdmin, boolean canWrite, boolean canDelete,
                                  boolean mustChangePassword)
  {
    String result;
    result = JWT.create()
      .withSubject(String.valueOf(userId))
      .withClaim("username", username)
      .withClaim("ruolo", ruolo)
      .withClaim("can_admin", canAdmin)
      .withClaim("can_write", canWrite)
      .withClaim("can_delete", canDelete)
      .withClaim("must_change_password", mustChangePassword)
      .withExpiresAt(Date.from(Instant.now().plusSeconds(accessExpiry)))
      .sign(algorithm);
    return result;
  }

  /**
   * Verifica e decodifica il JWT.
   * Lancia JWTVerificationException se il token è invalido o scaduto.
   */
  public DecodedJWT verifyAccessToken(String token) throws JWTVerificationException
  {
    return verifier.verify(token);
  }

  // -------------------------
  // REFRESH TOKEN
  // -------------------------

  /** Genera un token opaco casuale (64 hex char) da conservare nel DB. */
  public static String generateRefreshToken()
  {
    String result;
    result = UUID.randomUUID().toString().replace("-", "")
           + UUID.randomUUID().toString().replace("-", "");
    return result;
  }

  // -------------------------
  // PASSWORD HASHING (PBKDF2)
  // -------------------------

  /** Restituisce "salt:hash" in Base64. Da conservare nel DB al posto della password in chiaro. */
  public static String hashPassword(String password) throws Exception
  {
    byte[] salt;
    byte[] hash;
    String result;

    salt = new byte[16];
    new SecureRandom().nextBytes(salt);
    hash = pbkdf2(password, salt);
    result = Base64.getEncoder().encodeToString(salt) + ":" + Base64.getEncoder().encodeToString(hash);
    return result;
  }

  /** Confronta la password in chiaro con il valore "salt:hash" salvato nel DB. */
  public static boolean verifyPassword(String password, String stored) throws Exception
  {
    String[] parts;
    byte[] salt;
    byte[] expected;
    boolean result;

    parts = stored.split(":");
    salt = Base64.getDecoder().decode(parts[0]);
    expected = Base64.getDecoder().decode(parts[1]);
    result = MessageDigest.isEqual(expected, pbkdf2(password, salt));
    return result;
  }

  private static byte[] pbkdf2(String password, byte[] salt) throws Exception
  {
    PBEKeySpec spec;
    byte[] result;

    spec = new PBEKeySpec(password.toCharArray(), salt, 310_000, 256);
    result = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
    return result;
  }
}
