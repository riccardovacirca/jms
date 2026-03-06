package dev.crm.module.voice3.controller;

import dev.crm.module.voice3.service.VoiceService3;
import dev.springtools.util.ApiResponse;
import dev.springtools.util.ApiPayload;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller REST del modulo voice3.
 * Espone gli endpoint su /api/voice3/*.
 *
 * Funzione di adapter layer: riceve le richieste HTTP, delega al service
 * la logica di business e restituisce la response tramite ApiResponse builder.
 *
 * ENDPOINT DISPONIBILI:
 *   POST /api/voice3/prepare-call         → fase 1: registra il numero cliente
 *   POST /api/voice3/answer               → webhook Vonage answer URL
 *   POST /api/voice3/trigger-customer-call → avvia manualmente la chiamata cliente
 *   PUT  /api/voice3/calls/{uuid}/hangup  → riagancia le chiamate
 *   GET  /api/voice3/sdk-token            → genera JWT per il Vonage Client SDK
 */
@RestController
@RequestMapping("/api/voice3")
public class VoiceController3
{
  private static final Logger log = LoggerFactory.getLogger(VoiceController3.class);

  private final VoiceService3 voiceService;

  public VoiceController3(VoiceService3 voiceService)
  {
    this.voiceService = voiceService;
  }

  /**
   * Fase 1 del flusso: il frontend registra il numero cliente da chiamare.
   * Deve essere invocato PRIMA di client.serverCall() per garantire
   * che il backend abbia il customerNumber disponibile al momento
   * della ricezione del webhook answer.
   *
   * Request body:
   *   { "userId": "operatore_01", "customerNumber": "+39XXXXXXXXXX" }
   */
  @PostMapping("/prepare-call")
  public ApiPayload prepareCall(
      @RequestBody Map<String, String> request)
  {
    ApiPayload response;
    String userId;
    String customerNumber;

    userId = request.get("userId");
    customerNumber = request.get("customerNumber");

    if (userId == null || customerNumber == null) {
      response = ApiResponse
          .create()
          .err(true)
          .log("userId e customerNumber sono obbligatori")
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    }

    voiceService.prepareCall(userId, customerNumber);

    response = ApiResponse
        .create()
        .out(Map.of("message", "Chiamata preparata"))
        .status(200)
        .contentType("application/json")
        .build();

    return response;
  }

  /**
   * Fase 2 del flusso — Answer URL webhook di Vonage.
   *
   * Vonage chiama questo endpoint quando l'operatore esegue serverCall()
   * tramite il Vonage Client SDK. Il backend deve rispondere IMMEDIATAMENTE
   * con un NCCO (Nexmo Call Control Object) che definisce il comportamento della chiamata.
   *
   * Questo endpoint:
   *   1. Estrae l'userId dell'operatore dal campo from_user (o fallback su altri campi)
   *   2. Recupera il customerNumber dalla mappa in-memory tramite userId
   *   3. Risponde a Vonage con NCCO: operatore entra nella conversazione con musica di attesa
   *   4. In modo ASINCRONO (thread separato, delay 1s) chiama il cliente tramite API Vonage
   *
   * NOTA: questo endpoint non richiede autenticazione perché deve essere
   * raggiungibile da Vonage. La sicurezza è delegata alla configurazione
   * dell'Answer URL nel Vonage Dashboard (solo Vonage conosce l'URL).
   *
   * Configurazione richiesta nel Vonage Dashboard:
   *   Answer URL: <VONAGE_ANSWER_URL_VOICE3> (vedi .env)
   */
  @PostMapping("/answer")
  public ResponseEntity<List<Map<String, Object>>> handleAnswerWebhook(
      @RequestBody Map<String, Object> allParams)
  {
    List<Map<String, Object>> ncco;
    String conversationName;
    String musicOnHoldUrl;
    String customerNumber;
    String userId;
    String operatorCallUuid;

    log.info("=== VOICE3 ANSWER WEBHOOK ===");
    log.info("Parametri ricevuti: {}", allParams);

    // Estrae userId dall'operatore dai parametri del webhook.
    // Vonage può passare l'userId in campi diversi a seconda del tipo di chiamata:
    //   from_user: usato da serverCall() del Client SDK
    //   to: usato in alcuni scenari
    //   user_id: parametro opzionale
    //   from: fallback finale
    Object fromUserObj = allParams.get("from_user");
    Object toObj = allParams.get("to");
    Object fromObj = allParams.get("from");
    Object userIdObj = allParams.get("user_id");

    if (fromUserObj != null && !fromUserObj.toString().isEmpty()) {
      userId = fromUserObj.toString();
    } else if (toObj != null && !toObj.toString().isEmpty()) {
      userId = toObj.toString();
    } else if (userIdObj != null && !userIdObj.toString().isEmpty()) {
      userId = userIdObj.toString();
    } else if (fromObj != null && !fromObj.toString().isEmpty()) {
      userId = fromObj.toString();
    } else {
      userId = null;
    }

    log.info("userId estratto: {}", userId);

    // UUID della chiamata dell'operatore: serve per associarla alla chiamata cliente
    Object uuidObj = allParams.get("uuid");
    operatorCallUuid = uuidObj != null ? uuidObj.toString() : null;
    log.info("UUID chiamata operatore: {}", operatorCallUuid);

    // Recupera il numero cliente registrato nella fase 1 (prepareCall)
    if (userId != null) {
      customerNumber = voiceService.getPendingCustomerNumber(userId);
      log.info("Numero cliente recuperato: {}", customerNumber);
    } else {
      customerNumber = null;
      log.warn("ATTENZIONE: impossibile estrarre userId dai parametri webhook!");
    }

    // Genera un nome univoco per la conversazione Vonage
    conversationName = "call-" + UUID.randomUUID().toString();
    musicOnHoldUrl = "https://nexmo-community.github.io/ncco-examples/assets/voice_api_audio_streaming.mp3";

    log.info("Nome conversazione: {}", conversationName);

    // Costruisce l'NCCO: l'operatore entra in attesa con musica di sottofondo
    ncco = voiceService.buildOperatorWaitingNcco(conversationName, musicOnHoldUrl);

    // Avvia la chiamata al cliente in modo asincrono.
    // Il ritardo di 1 secondo garantisce che l'operatore sia entrato nella
    // conversazione prima che il cliente risponda.
    if (customerNumber != null && !customerNumber.isEmpty() && operatorCallUuid != null) {
      log.info("Avvio chiamata cliente asincrona verso: {}", customerNumber);
      String finalConversationName = conversationName;
      String finalOperatorCallUuid = operatorCallUuid;
      new Thread(() -> {
        try {
          Thread.sleep(1000);
          log.info("Thread asincrono: chiamo cliente {}", customerNumber);
          voiceService.callCustomer(customerNumber, finalConversationName, finalOperatorCallUuid);
          log.info("Chiamata cliente avviata con successo");
        } catch (Exception e) {
          log.error("ERRORE: impossibile avviare la chiamata cliente: {}", e.getMessage(), e);
        }
      }).start();
    } else {
      log.warn("ATTENZIONE: customerNumber o operatorCallUuid mancante - chiamata cliente NON avviata!");
    }

    log.info("=== ANSWER WEBHOOK: RISPOSTA INVIATA ===");

    // Risponde IMMEDIATAMENTE a Vonage con l'NCCO (Vonage ha timeout breve)
    return ResponseEntity.ok(ncco);
  }

  /**
   * Endpoint opzionale per avviare manualmente la chiamata al cliente.
   * Utile per test o scenari in cui il flusso automatico dall'answer webhook
   * non è disponibile.
   *
   * Request body:
   *   {
   *     "customerNumber": "+39XXXXXXXXXX",
   *     "conversationName": "call-<uuid>",
   *     "operatorCallUuid": "<uuid>"  (opzionale)
   *   }
   */
  @PostMapping("/trigger-customer-call")
  public ApiPayload triggerCustomerCall(
      @RequestBody Map<String, String> request) throws Exception
  {
    ApiPayload response;
    String customerNumber;
    String conversationName;
    String operatorCallUuid;

    customerNumber = request.get("customerNumber");
    conversationName = request.get("conversationName");
    operatorCallUuid = request.get("operatorCallUuid");

    if (customerNumber == null || conversationName == null) {
      response = ApiResponse
          .create()
          .err(true)
          .log("customerNumber e conversationName sono obbligatori")
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    }

    voiceService.callCustomer(customerNumber, conversationName, operatorCallUuid);

    response = ApiResponse
        .create()
        .out(Map.of("message", "Chiamata cliente avviata"))
        .status(200)
        .contentType("application/json")
        .build();

    return response;
  }

  /**
   * Riagancia la chiamata dell'operatore e quella del cliente associata.
   * Il service recupera dalla mappa in-memory il customerCallUuid associato
   * all'operatorCallUuid e invia hangup a entrambe le chiamate Vonage.
   *
   * @param uuid UUID della chiamata dell'operatore (leg UUID restituito da serverCall())
   */
  @PutMapping("/calls/{uuid}/hangup")
  public ApiPayload hangupCall(
      @PathVariable String uuid) throws Exception
  {
    ApiPayload response;

    voiceService.hangupCall(uuid);

    response = ApiResponse
        .create()
        .out(Map.of("message", "Riagancio eseguito"))
        .status(200)
        .contentType("application/json")
        .build();

    return response;
  }

  /**
   * Genera il JWT SDK per autenticare l'operatore nel Vonage Client SDK lato browser.
   * Il frontend deve chiamare questo endpoint all'avvio della sessione e ogni ~13 minuti
   * per il refresh (la scadenza del token è 1 ora).
   *
   * @param userId identificatore dell'operatore nel sistema Vonage
   * @return token JWT da passare a VonageClient.createSession()
   */
  @GetMapping("/sdk-token")
  public ApiPayload getSdkToken(
      @RequestParam String userId)
  {
    ApiPayload response;

    try {
      String token;

      token = voiceService.generateSdkJwt(userId);

      response = ApiResponse
          .create()
          .out(Map.of("token", token))
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    } catch (Exception e) {
      response = ApiResponse
          .create()
          .err(true)
          .log("Impossibile generare SDK token: " + e.getMessage())
          .status(200)
          .contentType("application/json")
          .build();

      return response;
    }
  }
}
