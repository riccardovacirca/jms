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
  // PASSWORD POLICY
  // -------------------------

  private static final String LOWER   = "abcdefghijklmnopqrstuvwxyz";
  private static final String UPPER   = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
  private static final String DIGITS  = "0123456789";
  private static final String SPECIAL = "!@#$%&*?+-_=";

  /**
   * Verifica la policy password.
   * Ritorna null se valida, altrimenti una stringa con la motivazione del rifiuto.
   * Policy: 8-32 caratteri, almeno una minuscola, una maiuscola, un cifra, un carattere speciale.
   */
  public static String validatePassword(String password)
  {
    boolean hasLower;
    boolean hasUpper;
    boolean hasDigit;
    boolean hasSpecial;

    if (password == null || password.length() < 8) {
      return "La password deve avere almeno 8 caratteri";
    }
    if (password.length() > 32) {
      return "La password non può superare i 32 caratteri";
    }
    hasLower   = false;
    hasUpper   = false;
    hasDigit   = false;
    hasSpecial = false;
    for (char c : password.toCharArray()) {
      if      (Character.isLowerCase(c)) hasLower   = true;
      else if (Character.isUpperCase(c)) hasUpper   = true;
      else if (Character.isDigit(c))     hasDigit   = true;
      else                               hasSpecial = true;
    }
    if (!hasLower)   return "La password deve contenere almeno una lettera minuscola";
    if (!hasUpper)   return "La password deve contenere almeno una lettera maiuscola";
    if (!hasDigit)   return "La password deve contenere almeno un numero";
    if (!hasSpecial) return "La password deve contenere almeno un carattere speciale";
    return null;
  }

  /** Genera una password casuale di 12 caratteri conforme alla policy. */
  public static String generatePassword()
  {
    SecureRandom rnd;
    char[] pwd;
    String all;
    int i;
    int j;
    char tmp;

    rnd = new SecureRandom();
    pwd = new char[12];
    pwd[0] = LOWER.charAt(rnd.nextInt(LOWER.length()));
    pwd[1] = UPPER.charAt(rnd.nextInt(UPPER.length()));
    pwd[2] = DIGITS.charAt(rnd.nextInt(DIGITS.length()));
    pwd[3] = SPECIAL.charAt(rnd.nextInt(SPECIAL.length()));
    all = LOWER + UPPER + DIGITS + SPECIAL;
    for (i = 4; i < 12; i++) {
      pwd[i] = all.charAt(rnd.nextInt(all.length()));
    }
    for (i = 11; i > 0; i--) {
      j = rnd.nextInt(i + 1);
      tmp = pwd[i]; pwd[i] = pwd[j]; pwd[j] = tmp;
    }
    return new String(pwd);
  }

  /** Genera un PIN numerico di 8 cifre per la 2FA. */
  public static String generatePin()
  {
    SecureRandom rnd;
    StringBuilder sb;

    rnd = new SecureRandom();
    sb  = new StringBuilder(8);
    for (int i = 0; i < 8; i++) {
      sb.append(rnd.nextInt(10));
    }
    return sb.toString();
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
