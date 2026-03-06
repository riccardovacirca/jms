package dev.crm.module.voice3.dto;

import java.time.LocalDateTime;

/**
 * DTO che rappresenta una chiamata telefonica gestita tramite Vonage.
 * Viene popolato sia al momento della creazione della chiamata (callCustomer)
 * che durante la ricezione degli eventi webhook da Vonage.
 *
 * Corrisponde alla tabella voice_calls nel database.
 */
public class CallDto3
{
  // Identificatore interno del record nel DB
  public Long id;

  // UUID univoco assegnato da Vonage alla chiamata (leg)
  public String uuid;

  // UUID della conversazione Vonage (condiviso tra tutti i partecipanti)
  public String conversationUuid;

  // Direzione della chiamata: "outbound" (generata dal sistema) o "inbound"
  public String direction;

  // Stato corrente della chiamata: started, ringing, answered, completed, etc.
  public String status;

  // Tipo e numero del chiamante (es. "phone", "+39...")
  public String fromType;
  public String fromNumber;

  // Tipo e numero del destinatario
  public String toType;
  public String toNumber;

  // Informazioni di costo (valorizzate da Vonage al completamento)
  public String rate;
  public String price;

  // Durata in secondi (valorizzata al completamento)
  public Integer duration;

  // Timestamp di inizio e fine chiamata
  public LocalDateTime startTime;
  public LocalDateTime endTime;

  // Rete telefonica utilizzata
  public String network;

  // URL webhook usati per questa chiamata
  public String answerUrl;
  public String eventUrl;

  // Dettagli errore in caso di fallimento
  public String errorTitle;
  public String errorDetail;

  // Riferimenti CRM: operatore, campagna, contatto associati alla chiamata
  public Long operatorId;
  public Long campagnaId;
  public Long contattoId;

  // Timestamp di creazione e aggiornamento del record
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;

  public CallDto3()
  {
  }

  public CallDto3(
      Long id,
      String uuid,
      String conversationUuid,
      String direction,
      String status,
      String fromType,
      String fromNumber,
      String toType,
      String toNumber,
      String rate,
      String price,
      Integer duration,
      LocalDateTime startTime,
      LocalDateTime endTime,
      String network,
      String answerUrl,
      String eventUrl,
      String errorTitle,
      String errorDetail,
      Long operatorId,
      Long campagnaId,
      Long contattoId,
      LocalDateTime createdAt,
      LocalDateTime updatedAt)
  {
    this.id = id;
    this.uuid = uuid;
    this.conversationUuid = conversationUuid;
    this.direction = direction;
    this.status = status;
    this.fromType = fromType;
    this.fromNumber = fromNumber;
    this.toType = toType;
    this.toNumber = toNumber;
    this.rate = rate;
    this.price = price;
    this.duration = duration;
    this.startTime = startTime;
    this.endTime = endTime;
    this.network = network;
    this.answerUrl = answerUrl;
    this.eventUrl = eventUrl;
    this.errorTitle = errorTitle;
    this.errorDetail = errorDetail;
    this.operatorId = operatorId;
    this.campagnaId = campagnaId;
    this.contattoId = contattoId;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }
}
