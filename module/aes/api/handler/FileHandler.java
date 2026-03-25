package dev.jms.app.module.aes.handler;

import dev.jms.util.DB;
import dev.jms.util.File;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;

/**
 * Handler per gestione file temporanei: upload, download, delete.
 * <p>
 * Directory strutturata: {@code /tmp/aes/YYYY/MM/}<br>
 * Naming: {@code hash(filename)-timestamp.ext}
 * </p>
 */
public class FileHandler
{
  private static final String BASE_DIR = "/tmp/aes";

  /**
   * POST /api/aes/file/upload — carica file multipart in storage temporaneo.
   * <p>
   * Parametri multipart: {@code file}<br>
   * Risposta: {@code {"path": "/tmp/aes/YYYY/MM/..."}}
   * </p>
   */
  public void upload(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    byte[] fileBytes;
    String originalFilename;
    String tempPath;
    HashMap<String, Object> out;

    req.requireAuth();

    fileBytes = req.getMultipartFileBytes("file");
    originalFilename = req.getMultipartFilename("file");

    if (fileBytes == null || fileBytes.length == 0) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Nessun file ricevuto")
         .out(null)
         .send();
      return;
    }

    tempPath = saveTempFile(fileBytes, originalFilename);

    out = new HashMap<>();
    out.put("path", tempPath);
    out.put("size", fileBytes.length);
    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(out)
       .send();
  }

  /**
   * GET /api/aes/file/download?path=... — scarica file da storage temporaneo.
   * <p>
   * Parametri querystring: {@code path} (path assoluto del file)
   * </p>
   */
  public void download(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String path;
    String filename;
    byte[] fileBytes;

    req.requireAuth();

    path = req.getQueryParam("path");

    if (path == null || path.isBlank()) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Parametro 'path' obbligatorio")
         .out(null)
         .send();
      return;
    }

    if (!File.exists(path)) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("File non trovato")
         .out(null)
         .send();
      return;
    }

    fileBytes = File.readBytes(path);
    filename = path.substring(path.lastIndexOf('/') + 1);

    res.status(200)
       .download(fileBytes, filename, "application/octet-stream");
  }

  /**
   * DELETE /api/aes/file/delete?path=... — elimina file da storage temporaneo.
   * <p>
   * Parametri querystring: {@code path} (path assoluto del file)
   * </p>
   */
  public void delete(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String path;

    req.requireAuth();

    path = req.getQueryParam("path");

    if (path == null || path.isBlank()) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("Parametro 'path' obbligatorio")
         .out(null)
         .send();
      return;
    }

    if (!File.exists(path)) {
      res.status(200)
         .contentType("application/json")
         .err(true)
         .log("File non trovato")
         .out(null)
         .send();
      return;
    }

    File.delete(path);

    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(null)
       .send();
  }

  /**
   * Salva file in storage temporaneo con naming strutturato.
   *
   * @param content          contenuto del file
   * @param originalFilename nome originale del file
   * @return path assoluto del file salvato
   * @throws Exception se il salvataggio fallisce
   */
  private String saveTempFile(byte[] content, String originalFilename) throws Exception
  {
    LocalDateTime now;
    String yearMonth;
    String dir;
    String hash;
    String timestamp;
    String extension;
    String filename;
    String path;

    now = LocalDateTime.now();
    yearMonth = now.format(DateTimeFormatter.ofPattern("yyyy/MM"));
    dir = BASE_DIR + "/" + yearMonth;

    File.createDirectory(dir);

    hash = computeHash(originalFilename != null ? originalFilename : "file");
    timestamp = String.valueOf(System.currentTimeMillis());
    extension = extractExtension(originalFilename);
    filename = hash + "-" + timestamp + extension;
    path = dir + "/" + filename;

    File.writeBytes(path, content);

    return path;
  }

  /**
   * Estrae l'estensione da un nome file (include il punto).
   */
  private String extractExtension(String filename)
  {
    int idx;

    if (filename == null || filename.isBlank()) {
      return "";
    }

    idx = filename.lastIndexOf('.');
    if (idx > 0 && idx < filename.length() - 1) {
      return filename.substring(idx);
    }

    return "";
  }

  /**
   * Calcola hash MD5 di una stringa (primi 8 caratteri hex).
   */
  private String computeHash(String input) throws Exception
  {
    MessageDigest md;
    byte[] hashBytes;
    StringBuilder hex;

    md = MessageDigest.getInstance("MD5");
    hashBytes = md.digest(input.getBytes());

    hex = new StringBuilder();
    for (int i = 0; i < Math.min(4, hashBytes.length); i++) {
      hex.append(String.format("%02x", hashBytes[i]));
    }

    return hex.toString();
  }
}
