package dev.jms.app.module.cti.vonage.dto;

/**
 * DTO per la tabella {@code cti_operators}.
 * Rappresenta un operatore CTI con il suo identificatore Vonage
 * e l'eventuale associazione all'account applicativo.
 */
public class OperatorDTO
{
  private final Long    id;
  private final String  vonageUserId;
  private final Long    accountId;
  private final String  displayName;
  private final Boolean active;

  /**
   * @param id           chiave primaria
   * @param vonageUserId nome utente nel sistema Vonage (claim {@code sub} del JWT SDK)
   * @param accountId    id account applicativo associato (Long), o {@code null} se non collegato
   * @param displayName  nome visualizzato dell'operatore, o {@code null}
   * @param active       true se l'operatore è abilitato
   */
  public OperatorDTO(Long id, String vonageUserId, Long accountId,
                     String displayName, Boolean active)
  {
    this.id           = id;
    this.vonageUserId = vonageUserId;
    this.accountId    = accountId;
    this.displayName  = displayName;
    this.active       = active;
  }

  /** @return chiave primaria */
  public Long id() { return id; }

  /** @return identificatore utente nel sistema Vonage */
  public String vonageUserId() { return vonageUserId; }

  /** @return id account applicativo associato, o {@code null} */
  public Long accountId() { return accountId; }

  /** @return nome visualizzato, o {@code null} */
  public String displayName() { return displayName; }

  /** @return true se l'operatore è abilitato */
  public Boolean active() { return active; }
}
