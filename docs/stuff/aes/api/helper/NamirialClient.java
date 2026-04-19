package dev.jms.app.module.aes.helper;

import dev.jms.app.module.aes.dto.NamirialDocument;
import dev.jms.app.module.aes.dto.NamirialFolder;
import dev.jms.app.module.aes.dto.NamirialUploadResult;
import dev.jms.util.Json;
import dev.jms.util.Log;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Client HTTP per integrazione con piattaforma Namirial (firma remota DM7/Conserva).
 * <p>
 * Fornisce metodi per autenticazione, upload documenti, recupero documenti firmati,
 * gestione folder, relazioni tra documenti. Usa Java 11+ HttpClient (built-in).
 * </p>
 * <h3>Protocollo DM7/Conserva:</h3>
 * <p>
 * L'autenticazione avviene tramite header HTTP {@code dm7auth} contenente
 * il token ottenuto dal login. Tutte le richieste successive all'autenticazione
 * devono includere questo header.
 * </p>
 */
public final class NamirialClient
{
  private static final Log log = Log.get(NamirialClient.class);
  private static final int TIMEOUT_SECONDS = 30;

  private final String endpoint;
  private final HttpClient httpClient;
  private String accessToken;

  /**
   * Crea un nuovo client Namirial.
   *
   * @param endpoint URL base della piattaforma Namirial (es. https://namirial.example.com)
   */
  public NamirialClient(String endpoint)
  {
    this.endpoint = endpoint;
    this.httpClient = HttpClient.newBuilder()
      .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
      .build();
    this.accessToken = null;
  }

  /**
   * Autentica il client con username e password.
   * <p>
   * Dopo login riuscito, tutte le richieste successive includeranno automaticamente
   * l'header {@code dm7auth} con il token ottenuto.
   * </p>
   *
   * @param username username
   * @param password password
   * @return {@code true} se login riuscito, {@code false} altrimenti
   * @throws Exception se errore di rete o risposta invalida
   */
  public boolean login(String username, String password) throws Exception
  {
    String url;
    Map<String, Object> bodyMap;
    String bodyJson;
    HttpRequest request;
    HttpResponse<String> response;
    Map<String, Object> responseMap;
    boolean success;
    String token;

    url = endpoint + "/api/auth/login";
    bodyMap = Map.of("username", username, "password", password);
    bodyJson = Json.encode(bodyMap);

    request = HttpRequest.newBuilder()
      .uri(URI.create(url))
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8))
      .build();

    response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      log.warn("Namirial login failed: HTTP {}", response.statusCode());
      return false;
    }

    responseMap = Json.decode(response.body(), Map.class);
    success = (Boolean) responseMap.getOrDefault("success", false);

    if (!success) {
      log.warn("Namirial login failed: {}", responseMap.get("message"));
      return false;
    }

    token = (String) responseMap.get("token");
    if (token == null || token.isBlank()) {
      log.warn("Namirial login succeeded but token is missing");
      return false;
    }

    this.accessToken = token;
    log.debug("Namirial login successful");
    return true;
  }

  /**
   * Invia un documento alla firma remota su tablet Namirial.
   *
   * @param pdfBytes   contenuto PDF
   * @param filename   nome file
   * @param docTypeId  tipo documento (es. "CONTRATTO")
   * @param tabletId   ID tablet destinazione
   * @param metadata   metadati opzionali (può essere {@code null})
   * @return risultato upload con documentId se riuscito
   * @throws Exception se errore
   */
  public NamirialUploadResult requireSignature(
    byte[] pdfBytes,
    String filename,
    String docTypeId,
    String tabletId,
    Map<String, Object> metadata
  ) throws Exception
  {
    String url;
    String pdfBase64;
    Map<String, Object> bodyMap;
    String bodyJson;
    HttpRequest request;
    HttpResponse<String> response;
    Map<String, Object> responseMap;
    boolean success;
    String documentId;
    String message;

    ensureAuthenticated();

    url = endpoint + "/api/documents/require-signature";
    pdfBase64 = Base64.getEncoder().encodeToString(pdfBytes);

    if (metadata == null) {
      bodyMap = Map.of(
        "pdfBase64", pdfBase64,
        "filename", filename,
        "docTypeId", docTypeId,
        "tabletId", tabletId
      );
    } else {
      bodyMap = Map.of(
        "pdfBase64", pdfBase64,
        "filename", filename,
        "docTypeId", docTypeId,
        "tabletId", tabletId,
        "metadata", metadata
      );
    }

    bodyJson = Json.encode(bodyMap);

    request = HttpRequest.newBuilder()
      .uri(URI.create(url))
      .header("Content-Type", "application/json")
      .header("dm7auth", accessToken)
      .POST(HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8))
      .build();

    response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      log.warn("Namirial requireSignature failed: HTTP {}", response.statusCode());
      return new NamirialUploadResult(false, null, "HTTP error " + response.statusCode());
    }

    responseMap = Json.decode(response.body(), Map.class);
    success = (Boolean) responseMap.getOrDefault("success", false);
    documentId = (String) responseMap.get("documentId");
    message = (String) responseMap.get("message");

    log.debug("Namirial requireSignature: success={}, documentId={}", success, documentId);

    return new NamirialUploadResult(success, documentId, message);
  }

  /**
   * Recupera un documento dal tablet Namirial.
   *
   * @param tabletId ID tablet
   * @param docId    ID documento
   * @return documento con contenuto Base64
   * @throws Exception se errore
   */
  public NamirialDocument getDocument(String tabletId, String docId) throws Exception
  {
    String url;
    HttpRequest request;
    HttpResponse<String> response;
    Map<String, Object> responseMap;
    Map<String, Object> doc;
    String id;
    String fileName;
    String mimeType;
    String contentBase64;
    boolean isSigned;
    String documentTypeId;
    String userId;

    ensureAuthenticated();

    url = endpoint + "/api/documents/" + docId + "?tabletId=" + tabletId;

    request = HttpRequest.newBuilder()
      .uri(URI.create(url))
      .header("dm7auth", accessToken)
      .GET()
      .build();

    response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      throw new Exception("Failed to get document: HTTP " + response.statusCode());
    }

    responseMap = Json.decode(response.body(), Map.class);
    doc = (Map<String, Object>) responseMap.get("document");

    if (doc == null) {
      throw new Exception("Document not found in response");
    }

    id = (String) doc.get("id");
    fileName = (String) doc.get("fileName");
    mimeType = (String) doc.get("mimeType");
    contentBase64 = (String) doc.get("contentBase64");
    isSigned = (Boolean) doc.getOrDefault("isSigned", false);
    documentTypeId = (String) doc.get("documentTypeId");
    userId = (String) doc.get("userId");

    return new NamirialDocument(
      id,
      fileName,
      mimeType,
      contentBase64,
      isSigned,
      documentTypeId,
      userId
    );
  }

  /**
   * Recupera l'elenco di tutti i documenti presenti su un tablet.
   *
   * @param tabletId ID tablet
   * @return lista documenti
   * @throws Exception se errore
   */
  public List<NamirialDocument> getTabletDocuments(String tabletId) throws Exception
  {
    String url;
    HttpRequest request;
    HttpResponse<String> response;
    Map<String, Object> responseMap;
    List<Map<String, Object>> docs;
    List<NamirialDocument> result;
    String id;
    String fileName;
    String mimeType;
    String contentBase64;
    boolean isSigned;
    String documentTypeId;
    String userId;

    ensureAuthenticated();

    url = endpoint + "/api/documents?tabletId=" + tabletId;

    request = HttpRequest.newBuilder()
      .uri(URI.create(url))
      .header("dm7auth", accessToken)
      .GET()
      .build();

    response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      throw new Exception("Failed to get tablet documents: HTTP " + response.statusCode());
    }

    responseMap = Json.decode(response.body(), Map.class);
    docs = (List<Map<String, Object>>) responseMap.get("documents");

    if (docs == null) {
      return new ArrayList<>();
    }

    result = new ArrayList<>();
    for (Map<String, Object> doc : docs) {
      id = (String) doc.get("id");
      fileName = (String) doc.get("fileName");
      mimeType = (String) doc.get("mimeType");
      contentBase64 = (String) doc.getOrDefault("contentBase64", "");
      isSigned = (Boolean) doc.getOrDefault("isSigned", false);
      documentTypeId = (String) doc.get("documentTypeId");
      userId = (String) doc.get("userId");

      result.add(new NamirialDocument(
        id,
        fileName,
        mimeType,
        contentBase64,
        isSigned,
        documentTypeId,
        userId
      ));
    }

    log.debug("Namirial getTabletDocuments: found {} documents", result.size());
    return result;
  }

  /**
   * Elenca i folder disponibili su un tablet.
   *
   * @param tabletId ID tablet
   * @return lista folder
   * @throws Exception se errore
   */
  public List<NamirialFolder> listFolders(String tabletId) throws Exception
  {
    String url;
    HttpRequest request;
    HttpResponse<String> response;
    Map<String, Object> responseMap;
    List<Map<String, Object>> folders;
    List<NamirialFolder> result;
    String id;
    String title;
    List<String> parents;

    ensureAuthenticated();

    url = endpoint + "/api/folders?tabletId=" + tabletId;

    request = HttpRequest.newBuilder()
      .uri(URI.create(url))
      .header("dm7auth", accessToken)
      .GET()
      .build();

    response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      throw new Exception("Failed to list folders: HTTP " + response.statusCode());
    }

    responseMap = Json.decode(response.body(), Map.class);
    folders = (List<Map<String, Object>>) responseMap.get("folders");

    if (folders == null) {
      return new ArrayList<>();
    }

    result = new ArrayList<>();
    for (Map<String, Object> folder : folders) {
      id = (String) folder.get("id");
      title = (String) folder.get("title");
      parents = (List<String>) folder.getOrDefault("parents", new ArrayList<>());

      result.add(new NamirialFolder(id, title, parents));
    }

    log.debug("Namirial listFolders: found {} folders", result.size());
    return result;
  }

  /**
   * Crea una relazione tra due documenti su tablet Namirial.
   *
   * @param tabletId  ID tablet
   * @param docId     ID documento principale
   * @param relatedId ID documento correlato
   * @throws Exception se errore
   */
  public void makeRelation(String tabletId, String docId, String relatedId) throws Exception
  {
    String url;
    Map<String, Object> bodyMap;
    String bodyJson;
    HttpRequest request;
    HttpResponse<String> response;

    ensureAuthenticated();

    url = endpoint + "/api/documents/relate";
    bodyMap = Map.of(
      "tabletId", tabletId,
      "docId", docId,
      "relatedId", relatedId
    );
    bodyJson = Json.encode(bodyMap);

    request = HttpRequest.newBuilder()
      .uri(URI.create(url))
      .header("Content-Type", "application/json")
      .header("dm7auth", accessToken)
      .POST(HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8))
      .build();

    response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      throw new Exception("Failed to make relation: HTTP " + response.statusCode());
    }

    log.debug("Namirial makeRelation: {} <-> {}", docId, relatedId);
  }

  /**
   * Sposta tutti i documenti firmati dal folder firma al folder firmati.
   *
   * @param tabletId ID tablet
   * @return numero documenti spostati
   * @throws Exception se errore
   */
  public int moveSigned(String tabletId) throws Exception
  {
    String url;
    Map<String, Object> bodyMap;
    String bodyJson;
    HttpRequest request;
    HttpResponse<String> response;
    Map<String, Object> responseMap;
    int movedCount;

    ensureAuthenticated();

    url = endpoint + "/api/documents/move-signed";
    bodyMap = Map.of("tabletId", tabletId);
    bodyJson = Json.encode(bodyMap);

    request = HttpRequest.newBuilder()
      .uri(URI.create(url))
      .header("Content-Type", "application/json")
      .header("dm7auth", accessToken)
      .POST(HttpRequest.BodyPublishers.ofString(bodyJson, StandardCharsets.UTF_8))
      .build();

    response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      throw new Exception("Failed to move signed documents: HTTP " + response.statusCode());
    }

    responseMap = Json.decode(response.body(), Map.class);
    movedCount = ((Number) responseMap.getOrDefault("movedCount", 0)).intValue();

    log.debug("Namirial moveSigned: moved {} documents", movedCount);
    return movedCount;
  }

  /**
   * Rimuove un documento da tablet Namirial.
   *
   * @param tabletId ID tablet
   * @param docId    ID documento
   * @throws Exception se errore
   */
  public void remove(String tabletId, String docId) throws Exception
  {
    String url;
    HttpRequest request;
    HttpResponse<String> response;

    ensureAuthenticated();

    url = endpoint + "/api/documents/" + docId + "?tabletId=" + tabletId;

    request = HttpRequest.newBuilder()
      .uri(URI.create(url))
      .header("dm7auth", accessToken)
      .DELETE()
      .build();

    response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

    if (response.statusCode() != 200) {
      throw new Exception("Failed to remove document: HTTP " + response.statusCode());
    }

    log.debug("Namirial remove: document {} removed from tablet {}", docId, tabletId);
  }

  /**
   * Verifica che il client sia autenticato.
   *
   * @throws IllegalStateException se non autenticato
   */
  private void ensureAuthenticated()
  {
    if (accessToken == null || accessToken.isBlank()) {
      throw new IllegalStateException("Client not authenticated. Call login() first.");
    }
  }
}
