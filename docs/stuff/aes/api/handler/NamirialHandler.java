package dev.jms.app.module.aes.handler;

import dev.jms.app.module.aes.dto.NamirialDocument;
import dev.jms.app.module.aes.dto.NamirialFolder;
import dev.jms.app.module.aes.dto.NamirialUploadResult;
import dev.jms.app.module.aes.helper.NamirialClient;
import dev.jms.util.Config;
import dev.jms.util.DB;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Validator;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Handler per integrazione con piattaforma Namirial (firma remota DM7/Conserva).
 * <p>
 * Fornisce endpoint per l'invio di documenti alla firma remota, recupero
 * documenti firmati, spostamento file tra folder, gestione relazioni tra documenti,
 * e listing folder/documenti.
 * </p>
 */
public final class NamirialHandler
{
  private final Config config;

  public NamirialHandler(Config config)
  {
    this.config = config;
  }

  /**
   * POST /api/aes/namirial/require-signature
   * <p>
   * Invia un documento alla firma remota su tablet Namirial.
   * </p>
   * <p>
   * Body (JSON):
   * </p>
   * <pre>{@code
   * {
   *   "pdfBase64": "...",
   *   "filename": "contratto.pdf",
   *   "docTypeId": "CONTRATTO",
   *   "tabletId": "tablet-001",
   *   "metadata": { "id": "123", "customer": "ACME Corp" }
   * }
   * }</pre>
   *
   * @param req request
   * @param res response
   * @param db  database
   * @throws Exception se errore
   */
  public void requireSignature(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    Map<String, Object> body;
    String pdfBase64;
    String filename;
    String docTypeId;
    String tabletId;
    Map<String, Object> metadata;
    byte[] pdfBytes;
    NamirialClient client;
    NamirialUploadResult result;
    Map<String, Object> out;

    req.requireAuth();
    body = req.body();

    pdfBase64 = (String) body.get("pdfBase64");
    filename = (String) body.get("filename");
    docTypeId = (String) body.get("docTypeId");
    tabletId = (String) body.get("tabletId");
    metadata = (Map<String, Object>) body.get("metadata");

    Validator.required(pdfBase64, "pdfBase64");
    Validator.required(filename, "filename");
    Validator.required(docTypeId, "docTypeId");
    Validator.required(tabletId, "tabletId");

    pdfBytes = Base64.getDecoder().decode(pdfBase64);

    client = createNamirialClient();
    client.login(
      config.get("namirial.username", ""),
      config.get("namirial.password", "")
    );

    result = client.requireSignature(pdfBytes, filename, docTypeId, tabletId, metadata);

    out = Map.of(
      "success", result.success,
      "documentId", result.documentId != null ? result.documentId : "",
      "message", result.message != null ? result.message : ""
    );

    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(out)
       .send();
  }

  /**
   * GET /api/aes/namirial/document/{docId}?tabletId=...
   * <p>
   * Recupera un documento dal tablet Namirial.
   * </p>
   *
   * @param req request
   * @param res response
   * @param db  database
   * @throws Exception se errore
   */
  public void getDocument(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String docId;
    String tabletId;
    NamirialClient client;
    NamirialDocument doc;
    Map<String, Object> out;

    req.requireAuth();
    docId = req.urlArgs().get("docId");
    tabletId = req.queryParam("tabletId");

    Validator.required(docId, "docId");
    Validator.required(tabletId, "tabletId");

    client = createNamirialClient();
    client.login(
      config.get("namirial.username", ""),
      config.get("namirial.password", "")
    );

    doc = client.getDocument(tabletId, docId);

    out = Map.of(
      "id", doc.id,
      "fileName", doc.fileName,
      "mimeType", doc.mimeType,
      "contentBase64", doc.contentBase64,
      "isSigned", doc.isSigned,
      "documentTypeId", doc.documentTypeId,
      "userId", doc.userId
    );

    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(out)
       .send();
  }

  /**
   * POST /api/aes/namirial/move-signed
   * <p>
   * Sposta tutti i documenti firmati dal folder firma al folder firmati.
   * </p>
   * <p>
   * Body (JSON):
   * </p>
   * <pre>{@code
   * {
   *   "tabletId": "tablet-001"
   * }
   * }</pre>
   *
   * @param req request
   * @param res response
   * @param db  database
   * @throws Exception se errore
   */
  public void moveSigned(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    Map<String, Object> body;
    String tabletId;
    NamirialClient client;
    int movedCount;
    Map<String, Object> out;

    req.requireAuth();
    body = req.body();
    tabletId = (String) body.get("tabletId");

    Validator.required(tabletId, "tabletId");

    client = createNamirialClient();
    client.login(
      config.get("namirial.username", ""),
      config.get("namirial.password", "")
    );

    movedCount = client.moveSigned(tabletId);

    out = Map.of("movedCount", movedCount);

    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(out)
       .send();
  }

  /**
   * POST /api/aes/namirial/relate
   * <p>
   * Crea una relazione tra due documenti su tablet Namirial.
   * </p>
   * <p>
   * Body (JSON):
   * </p>
   * <pre>{@code
   * {
   *   "tabletId": "tablet-001",
   *   "docId": "doc-123",
   *   "relatedId": "doc-456"
   * }
   * }</pre>
   *
   * @param req request
   * @param res response
   * @param db  database
   * @throws Exception se errore
   */
  public void relateFiles(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    Map<String, Object> body;
    String tabletId;
    String docId;
    String relatedId;
    NamirialClient client;

    req.requireAuth();
    body = req.body();
    tabletId = (String) body.get("tabletId");
    docId = (String) body.get("docId");
    relatedId = (String) body.get("relatedId");

    Validator.required(tabletId, "tabletId");
    Validator.required(docId, "docId");
    Validator.required(relatedId, "relatedId");

    client = createNamirialClient();
    client.login(
      config.get("namirial.username", ""),
      config.get("namirial.password", "")
    );

    client.makeRelation(tabletId, docId, relatedId);

    res.status(200)
       .contentType("application/json")
       .err(false)
       .log("Relazione creata")
       .out(Map.of("success", true))
       .send();
  }

  /**
   * GET /api/aes/namirial/folders?tabletId=...
   * <p>
   * Elenca i folder disponibili su tablet Namirial.
   * </p>
   *
   * @param req request
   * @param res response
   * @param db  database
   * @throws Exception se errore
   */
  public void listFolders(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String tabletId;
    NamirialClient client;
    List<NamirialFolder> folders;
    List<Map<String, Object>> out;

    req.requireAuth();
    tabletId = req.queryParam("tabletId");

    Validator.required(tabletId, "tabletId");

    client = createNamirialClient();
    client.login(
      config.get("namirial.username", ""),
      config.get("namirial.password", "")
    );

    folders = client.listFolders(tabletId);

    out = folders.stream()
      .map(f -> Map.of(
        "id", (Object) f.id,
        "title", f.title,
        "parents", f.parents
      ))
      .toList();

    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(out)
       .send();
  }

  /**
   * GET /api/aes/namirial/documents?tabletId=...
   * <p>
   * Elenca i documenti disponibili su tablet Namirial.
   * </p>
   *
   * @param req request
   * @param res response
   * @param db  database
   * @throws Exception se errore
   */
  public void listDocuments(HttpRequest req, HttpResponse res, DB db) throws Exception
  {
    String tabletId;
    NamirialClient client;
    List<NamirialDocument> documents;
    List<Map<String, Object>> out;

    req.requireAuth();
    tabletId = req.queryParam("tabletId");

    Validator.required(tabletId, "tabletId");

    client = createNamirialClient();
    client.login(
      config.get("namirial.username", ""),
      config.get("namirial.password", "")
    );

    documents = client.getTabletDocuments(tabletId);

    out = documents.stream()
      .map(d -> Map.of(
        "id", (Object) d.id,
        "fileName", d.fileName,
        "mimeType", d.mimeType,
        "isSigned", d.isSigned,
        "documentTypeId", d.documentTypeId,
        "userId", d.userId
      ))
      .toList();

    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(out)
       .send();
  }

  /**
   * Crea un client Namirial configurato con endpoint e credenziali da application.properties.
   *
   * @return client Namirial
   */
  private NamirialClient createNamirialClient()
  {
    String endpoint;

    endpoint = config.get("namirial.endpoint", "");
    return new NamirialClient(endpoint);
  }
}
