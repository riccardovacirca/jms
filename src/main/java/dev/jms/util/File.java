package dev.jms.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility per operazioni generiche su file e directory.
 * <p>
 * Fornisce metodi statici per lettura, scrittura, copia, spostamento, eliminazione,
 * hashing e listing file. Tutti i path sono sanitizzati per prevenire directory traversal.
 * </p>
 *
 * <h3>Esempio:</h3>
 * <pre>{@code
 * // Lettura
 * byte[] data = File.readBytes("/app/data/file.bin");
 * String text = File.readText("/app/data/file.txt");
 *
 * // Scrittura
 * File.writeBytes("/app/output/file.bin", data);
 * File.writeText("/app/output/file.txt", "contenuto");
 *
 * // Operazioni
 * File.copy("/app/source.pdf", "/app/backup/source.pdf");
 * File.delete("/app/temp/old.dat");
 *
 * // Utilità
 * String sha256 = File.hash("/app/data/file.pdf", "SHA-256");
 * List<Path> files = File.list("/app/uploads", "*.pdf");
 * }</pre>
 */
public final class File
{
  private static final Log log = Log.get(File.class);

  private File()
  {
  }

  /**
   * Legge il contenuto di un file come array di byte.
   *
   * @param path percorso assoluto del file
   * @return contenuto del file
   * @throws Exception se il file non esiste o non è leggibile
   */
  public static byte[] readBytes(String path) throws Exception
  {
    Path sanitized;

    sanitized = sanitizePath(path);
    log.debug("Reading file: {}", sanitized);
    return Files.readAllBytes(sanitized);
  }

  /**
   * Legge il contenuto di un file come stringa UTF-8.
   *
   * @param path percorso assoluto del file
   * @return contenuto del file come stringa
   * @throws Exception se il file non esiste o non è leggibile
   */
  public static String readText(String path) throws Exception
  {
    byte[] bytes;

    bytes = readBytes(path);
    return new String(bytes, StandardCharsets.UTF_8);
  }

  /**
   * Legge il contenuto di un file come lista di righe.
   *
   * @param path percorso assoluto del file
   * @return lista di righe (senza terminatori di riga)
   * @throws Exception se il file non esiste o non è leggibile
   */
  public static List<String> readLines(String path) throws Exception
  {
    Path sanitized;

    sanitized = sanitizePath(path);
    log.debug("Reading lines from file: {}", sanitized);
    return Files.readAllLines(sanitized, StandardCharsets.UTF_8);
  }

  /**
   * Scrive array di byte in un file (sovrascrive se esiste).
   *
   * @param path    percorso assoluto del file
   * @param content contenuto da scrivere
   * @throws Exception se la scrittura fallisce
   */
  public static void writeBytes(String path, byte[] content) throws Exception
  {
    Path sanitized;
    Path parent;

    sanitized = sanitizePath(path);
    parent = sanitized.getParent();
    if (parent != null && !Files.exists(parent)) {
      Files.createDirectories(parent);
    }
    log.debug("Writing {} bytes to file: {}", content.length, sanitized);
    Files.write(sanitized, content);
  }

  /**
   * Scrive stringa UTF-8 in un file (sovrascrive se esiste).
   *
   * @param path    percorso assoluto del file
   * @param content contenuto da scrivere
   * @throws Exception se la scrittura fallisce
   */
  public static void writeText(String path, String content) throws Exception
  {
    byte[] bytes;

    bytes = content.getBytes(StandardCharsets.UTF_8);
    writeBytes(path, bytes);
  }

  /**
   * Appende stringa UTF-8 alla fine di un file esistente (crea il file se non esiste).
   *
   * @param path    percorso assoluto del file
   * @param content contenuto da appendere
   * @throws Exception se la scrittura fallisce
   */
  public static void appendText(String path, String content) throws Exception
  {
    Path sanitized;
    Path parent;
    byte[] bytes;

    sanitized = sanitizePath(path);
    parent = sanitized.getParent();
    if (parent != null && !Files.exists(parent)) {
      Files.createDirectories(parent);
    }
    bytes = content.getBytes(StandardCharsets.UTF_8);
    log.debug("Appending {} bytes to file: {}", bytes.length, sanitized);
    Files.write(sanitized, bytes, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
  }

  /**
   * Copia un file (sovrascrive destinazione se esiste).
   *
   * @param source      percorso assoluto file sorgente
   * @param destination percorso assoluto file destinazione
   * @throws Exception se la copia fallisce
   */
  public static void copy(String source, String destination) throws Exception
  {
    Path sanitizedSource;
    Path sanitizedDest;
    Path parent;

    sanitizedSource = sanitizePath(source);
    sanitizedDest = sanitizePath(destination);
    parent = sanitizedDest.getParent();
    if (parent != null && !Files.exists(parent)) {
      Files.createDirectories(parent);
    }
    log.debug("Copying file: {} -> {}", sanitizedSource, sanitizedDest);
    Files.copy(sanitizedSource, sanitizedDest, StandardCopyOption.REPLACE_EXISTING);
  }

  /**
   * Sposta un file (sovrascrive destinazione se esiste).
   *
   * @param source      percorso assoluto file sorgente
   * @param destination percorso assoluto file destinazione
   * @throws Exception se lo spostamento fallisce
   */
  public static void move(String source, String destination) throws Exception
  {
    Path sanitizedSource;
    Path sanitizedDest;
    Path parent;

    sanitizedSource = sanitizePath(source);
    sanitizedDest = sanitizePath(destination);
    parent = sanitizedDest.getParent();
    if (parent != null && !Files.exists(parent)) {
      Files.createDirectories(parent);
    }
    log.debug("Moving file: {} -> {}", sanitizedSource, sanitizedDest);
    Files.move(sanitizedSource, sanitizedDest, StandardCopyOption.REPLACE_EXISTING);
  }

  /**
   * Elimina un file o directory vuota.
   *
   * @param path percorso assoluto del file o directory
   * @throws Exception se l'eliminazione fallisce
   */
  public static void delete(String path) throws Exception
  {
    Path sanitized;

    sanitized = sanitizePath(path);
    if (Files.exists(sanitized)) {
      log.debug("Deleting: {}", sanitized);
      Files.delete(sanitized);
    } else {
      log.debug("Delete skipped (not exists): {}", sanitized);
    }
  }

  /**
   * Crea una directory (ricorsivamente se necessario).
   *
   * @param path percorso assoluto della directory
   * @throws Exception se la creazione fallisce
   */
  public static void createDirectory(String path) throws Exception
  {
    Path sanitized;

    sanitized = sanitizePath(path);
    if (!Files.exists(sanitized)) {
      log.debug("Creating directory: {}", sanitized);
      Files.createDirectories(sanitized);
    } else {
      log.debug("Directory already exists: {}", sanitized);
    }
  }

  /**
   * Lista tutti i file in una directory che corrispondono al pattern glob.
   * <p>
   * Esempi pattern: {@code "*.pdf"}, {@code "report-*.txt"}, {@code "**}{@code /*.java"}
   * </p>
   *
   * @param directory percorso assoluto della directory
   * @param pattern   pattern glob (es. {@code "*.pdf"}), oppure {@code "*"} per tutti
   * @return lista di percorsi assoluti dei file trovati
   * @throws Exception se la directory non esiste o non è leggibile
   */
  public static List<Path> list(String directory, String pattern) throws Exception
  {
    Path sanitized;
    String globPattern;
    Stream<Path> stream;
    List<Path> result;

    sanitized = sanitizePath(directory);
    if (!Files.exists(sanitized) || !Files.isDirectory(sanitized)) {
      throw new Exception("Directory not found or not a directory: " + directory);
    }

    globPattern = "glob:" + sanitized.toString() + "/" + pattern;
    log.debug("Listing files in {} with pattern: {}", sanitized, pattern);

    stream = Files.list(sanitized);
    result = stream
      .filter(Files::isRegularFile)
      .filter(p -> p.getFileName().toString().matches(convertGlobToRegex(pattern)))
      .collect(Collectors.toList());
    stream.close();

    log.debug("Found {} files", result.size());
    return result;
  }

  /**
   * Calcola l'hash di un file.
   *
   * @param path      percorso assoluto del file
   * @param algorithm algoritmo di hash ({@code "MD5"}, {@code "SHA-1"}, {@code "SHA-256"})
   * @return hash in formato esadecimale minuscolo
   * @throws Exception se il file non esiste o l'algoritmo non è supportato
   */
  public static String hash(String path, String algorithm) throws Exception
  {
    Path sanitized;
    byte[] fileBytes;
    MessageDigest digest;
    byte[] hashBytes;
    StringBuilder hex;

    sanitized = sanitizePath(path);
    fileBytes = Files.readAllBytes(sanitized);
    digest = MessageDigest.getInstance(algorithm);
    hashBytes = digest.digest(fileBytes);

    hex = new StringBuilder();
    for (byte b : hashBytes) {
      hex.append(String.format("%02x", b));
    }

    log.debug("Hash ({}) of file {}: {}", algorithm, sanitized, hex);
    return hex.toString();
  }

  /**
   * Restituisce la dimensione di un file in byte.
   *
   * @param path percorso assoluto del file
   * @return dimensione in byte
   * @throws Exception se il file non esiste
   */
  public static long size(String path) throws Exception
  {
    Path sanitized;
    long bytes;

    sanitized = sanitizePath(path);
    bytes = Files.size(sanitized);
    log.debug("Size of file {}: {} bytes", sanitized, bytes);
    return bytes;
  }

  /**
   * Verifica se un file o directory esiste.
   *
   * @param path percorso assoluto del file o directory
   * @return {@code true} se esiste, {@code false} altrimenti
   */
  public static boolean exists(String path)
  {
    Path sanitized;

    try {
      sanitized = sanitizePath(path);
      return Files.exists(sanitized);
    } catch (Exception e) {
      log.warn("Error checking existence of {}: {}", path, e.getMessage());
      return false;
    }
  }

  /**
   * Sanitizza un path per prevenire directory traversal.
   * <p>
   * Converte in path assoluto e normalizza. Lancia eccezione se il path risultante
   * non è valido o tenta di uscire da directory consentite.
   * </p>
   *
   * @param path path da sanitizzare
   * @return path sanitizzato
   * @throws Exception se il path non è valido
   */
  private static Path sanitizePath(String path) throws Exception
  {
    Path p;
    Path normalized;

    if (path == null || path.isBlank()) {
      throw new Exception("Path cannot be null or empty");
    }

    p = Paths.get(path);
    normalized = p.normalize().toAbsolutePath();

    if (normalized.toString().contains("..")) {
      throw new Exception("Invalid path (directory traversal detected): " + path);
    }

    return normalized;
  }

  /**
   * Converte pattern glob in regex per matching file names.
   *
   * @param glob pattern glob (es. {@code "*.pdf"})
   * @return pattern regex equivalente
   */
  private static String convertGlobToRegex(String glob)
  {
    StringBuilder regex;
    int i;
    char c;

    regex = new StringBuilder("^");
    for (i = 0; i < glob.length(); i++) {
      c = glob.charAt(i);
      switch (c) {
        case '*':
          regex.append(".*");
          break;
        case '?':
          regex.append(".");
          break;
        case '.':
          regex.append("\\.");
          break;
        default:
          regex.append(c);
      }
    }
    regex.append("$");
    return regex.toString();
  }
}
