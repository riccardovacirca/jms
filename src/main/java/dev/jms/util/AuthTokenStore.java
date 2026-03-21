package dev.jms.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestisce il ciclo di vita dei token di autorizzazione API.
 *
 * <p>I token vengono caricati da un file CSV all'avvio e persistiti ad ogni nuova emissione.
 * La struttura in memoria è una {@link ConcurrentHashMap} indicizzata per token value.
 *
 * <p>Formato file CSV (con header):
 * <pre>token,ip,createdAt,expiresAt,enabled</pre>
 * Il campo {@code expiresAt} può essere vuoto (nessuna scadenza).
 *
 * <p>Utilizzo tipico in {@code App.java}:
 * <pre>
 *   AuthTokenStore.init(config.get("authn.tokens.file", "/app/config/tokens.csv"));
 * </pre>
 */
public final class AuthTokenStore
{
  private static final Log log = Log.get(AuthTokenStore.class);
  private static final ConcurrentHashMap<String, AuthToken> tokens = new ConcurrentHashMap<>();

  private static String filePath;

  private AuthTokenStore()
  {
  }

  /**
   * Inizializza l'AuthTokenStore caricando il file CSV al percorso indicato.
   * Se il file non esiste viene creato con solo la riga di intestazione.
   * Chiamare una sola volta all'avvio dell'applicazione.
   *
   * @param path percorso assoluto del file CSV dei token
   */
  public static void init(String path)
  {
    filePath = path;
    load();
  }

  /**
   * Genera un nuovo token, lo registra in memoria e lo persiste nel file CSV.
   * Metodo sincronizzato per garantire consistenza tra struttura in memoria e file.
   *
   * @param ip        IP del customer associato (può essere stringa vuota)
   * @param expiresAt timestamp di scadenza in epoch millis; 0 = nessuna scadenza
   * @param enabled   se il token è immediatamente attivo
   * @return il {@link AuthToken} appena creato
   * @throws IOException se la scrittura su file non riesce
   */
  public static synchronized AuthToken generate(String ip, long expiresAt, boolean enabled)
    throws IOException
  {
    String    token;
    long      createdAt;
    AuthToken info;

    token     = generateToken();
    createdAt = System.currentTimeMillis();
    info      = new AuthToken(token, ip, createdAt, expiresAt, enabled);
    tokens.put(token, info);
    rewrite();
    return info;
  }

  /**
   * Verifica se il token fornito è valido: deve esistere, essere abilitato e non scaduto.
   * Se il token è stato generato con un IP associato, verifica che coincida con {@code requestIp}.
   * Se il token non ha IP associato (token di test), il controllo IP viene saltato.
   *
   * @param token     valore del token da validare
   * @param requestIp indirizzo IP della richiesta in arrivo
   * @return {@code true} se il token è valido
   */
  public static boolean isValid(String token, String requestIp)
  {
    AuthToken info;
    boolean   ipOk;
    boolean   valid;

    if (token == null || token.isBlank()) {
      valid = false;
    } else {
      info = tokens.get(token);
      if (info == null || !info.enabled) {
        valid = false;
      } else {
        ipOk  = info.ip.isEmpty() || info.ip.equals(requestIp);
        valid = ipOk && (info.expiresAt == 0 || System.currentTimeMillis() < info.expiresAt);
      }
    }
    return valid;
  }

  /**
   * Restituisce il token corrispondente all'hash fornito, o {@code null} se non esiste.
   *
   * @param token valore del token
   * @return {@link AuthToken} corrispondente, o {@code null}
   */
  public static AuthToken get(String token)
  {
    AuthToken result;

    result = token != null ? tokens.get(token) : null;
    return result;
  }

  /**
   * Aggiorna un token esistente con i nuovi valori forniti.
   * I campi assenti nel body (null) mantengono il valore corrente.
   * Aggiorna la struttura in memoria e riscrive l'intero file CSV.
   *
   * @param token     valore del token da aggiornare
   * @param ip        nuovo IP associato, o {@code null} per mantenere il valore corrente
   * @param expiresAt nuova scadenza in epoch millis, o {@code null} per mantenere il valore corrente
   * @param enabled   nuovo stato abilitazione, o {@code null} per mantenere il valore corrente
   * @return il {@link AuthToken} aggiornato, o {@code null} se il token non esiste
   * @throws IOException se la riscrittura del file non riesce
   */
  public static synchronized AuthToken update(String token, String ip, Long expiresAt, Boolean enabled)
    throws IOException
  {
    AuthToken existing;
    AuthToken updated;

    existing = tokens.get(token);
    if (existing == null) {
      updated = null;
    } else {
      updated = new AuthToken(
        existing.token,
        ip        != null ? ip        : existing.ip,
        existing.createdAt,
        expiresAt != null ? expiresAt : existing.expiresAt,
        enabled   != null ? enabled   : existing.enabled
      );
      tokens.put(token, updated);
      rewrite();
    }
    return updated;
  }

  /**
   * Rimuove un token dalla struttura in memoria e riscrive l'intero file CSV.
   *
   * @param token valore del token da rimuovere
   * @return {@code true} se il token esisteva ed è stato rimosso
   * @throws IOException se la riscrittura del file non riesce
   */
  public static synchronized boolean remove(String token) throws IOException
  {
    boolean removed;

    removed = tokens.remove(token) != null;
    if (removed) {
      rewrite();
    }
    return removed;
  }

  /**
   * Restituisce il numero di token caricati in memoria.
   *
   * @return numero di token
   */
  public static int size()
  {
    return tokens.size();
  }

  private static void load()
  {
    Path         path;
    List<String> lines;
    AuthToken    info;
    int          loaded;

    path = Paths.get(filePath);
    if (!Files.exists(path)) {
      log.info("File token non trovato, verrà creato al primo accesso: {}", filePath);
      createFile(path);
      return;
    }
    try {
      lines  = Files.readAllLines(path);
      loaded = 0;
      for (int i = 1; i < lines.size(); i++) {
        String line = lines.get(i).trim();
        if (!line.isEmpty()) {
          info = parseLine(line);
          if (info != null) {
            tokens.put(info.token, info);
            loaded++;
          }
        }
      }
      log.info("AuthTokenStore: {} token caricati da {}", loaded, filePath);
    } catch (IOException e) {
      log.warn("Errore lettura file token: {}", e.getMessage());
    }
  }

  private static void createFile(Path path)
  {
    try {
      if (path.getParent() != null) {
        Files.createDirectories(path.getParent());
      }
      Files.writeString(path, "token,ip,createdAt,expiresAt,enabled\n");
    } catch (IOException e) {
      log.warn("Impossibile creare il file token: {}", e.getMessage());
    }
  }

  private static AuthToken parseLine(String line)
  {
    String[]  parts;
    String    token;
    String    ip;
    long      createdAt;
    long      expiresAt;
    boolean   enabled;
    AuthToken result;

    parts  = line.split(",", -1);
    result = null;
    if (parts.length >= 5) {
      try {
        token     = parts[0].trim();
        ip        = parts[1].trim();
        createdAt = Long.parseLong(parts[2].trim());
        expiresAt = parts[3].trim().isEmpty() ? 0L : Long.parseLong(parts[3].trim());
        enabled   = Boolean.parseBoolean(parts[4].trim());
        result    = new AuthToken(token, ip, createdAt, expiresAt, enabled);
      } catch (NumberFormatException e) {
        log.warn("Riga token non parsabile (ignorata): {}", line);
      }
    } else {
      log.warn("Riga token malformata (ignorata): {}", line);
    }
    return result;
  }

  private static void rewrite() throws IOException
  {
    Path path;

    path = Paths.get(filePath);
    if (!Files.exists(path)) {
      createFile(path);
    }
    try (PrintWriter pw = new PrintWriter(new FileWriter(path.toFile(), false))) {
      pw.println("token,ip,createdAt,expiresAt,enabled");
      for (AuthToken info : tokens.values()) {
        pw.printf("%s,%s,%d,%s,%b%n",
          info.token,
          info.ip,
          info.createdAt,
          info.expiresAt == 0 ? "" : String.valueOf(info.expiresAt),
          info.enabled
        );
      }
    }
  }

  private static String generateToken()
  {
    SecureRandom  random;
    byte[]        bytes;
    StringBuilder sb;

    random = new SecureRandom();
    bytes  = new byte[32];
    random.nextBytes(bytes);
    sb     = new StringBuilder(64);
    for (byte b : bytes) {
      sb.append(String.format("%02x", b & 0xff));
    }
    return sb.toString();
  }
}
