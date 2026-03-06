package {{APP_PACKAGE}}.contatti.handler;

import com.auth0.jwt.exceptions.JWTVerificationException;
import {{APP_PACKAGE}}.contatti.dao.ImportSessionDAO;
import dev.jms.util.Auth;
import dev.jms.util.DB;
import dev.jms.util.Handler;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Json;
import dev.jms.util.Log;
import dev.jms.util.excel.ExcelAnalyzer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.UUID;

/** POST /api/import/analyze — upload file Excel, analizza e crea sessione di importazione. */
public class ImportAnalyzeHandler implements Handler
{
  private static final Log log = Log.get(ImportAnalyzeHandler.class);

  @Override
  public void post(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String           token;
    byte[]           fileBytes;
    String           filename;
    ExcelAnalyzer    analyzer;
    ExcelAnalyzer.AnalysisResult analysis;
    String           sessionId;
    Path             tmpFile;
    ImportSessionDAO dao;
    HashMap<String, Object> out;

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

    fileBytes = req.getMultipartFileBytes("file");
    if (fileBytes == null || fileBytes.length == 0) {
      res.status(200).contentType("application/json").err(true).log("Nessun file ricevuto").out(null).send();
      return;
    }

    filename = req.getMultipartFilename("file");
    if (filename == null) filename = "import.xlsx";

    try {
      analyzer = new ExcelAnalyzer(new ByteArrayInputStream(fileBytes));
      analysis = analyzer.analyze(5);
    } catch (Exception e) {
      log.warn("Errore analisi file: " + e.getMessage());
      res.status(200).contentType("application/json").err(true).log("Errore nel file: " + e.getMessage()).out(null).send();
      return;
    }

    // Salva file in cartella temporanea
    tmpFile = createTempFile(filename, fileBytes);

    sessionId = UUID.randomUUID().toString();
    dao       = new ImportSessionDAO(db);
    dao.create(
      sessionId,
      filename,
      tmpFile.toString(),
      analysis.totalRows,
      Json.encode(analysis.headers),
      Json.encode(analysis.previewRows)
    );

    out = new HashMap<>();
    out.put("sessionId",  sessionId);
    out.put("filename",   filename);
    out.put("rowCount",   analysis.totalRows);
    out.put("headers",    analysis.headers);
    out.put("preview",    analysis.previewRows);
    out.put("warnings",   analysis.warnings);

    res.status(200).contentType("application/json").err(false).log(null).out(out).send();
  }

  private Path createTempFile(String filename, byte[] bytes) throws IOException
  {
    String ext;
    Path   dir;
    Path   file;

    ext  = filename.contains(".") ? filename.substring(filename.lastIndexOf('.')) : ".xlsx";
    dir  = Path.of(System.getProperty("java.io.tmpdir"), "hola_import");
    Files.createDirectories(dir);
    file = Files.createTempFile(dir, "import_", ext);
    Files.write(file, bytes);
    return file;
  }
}
