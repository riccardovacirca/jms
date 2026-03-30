package dev.jms.app.module.cti.vonage.dto;

/**
 * DTO per la tabella {@code cti_operatori}.
 * Rappresenta un operatore CTI con il suo identificatore Vonage
 * e l'eventuale associazione all'account applicativo.
 */
public class OperatorDTO
{
  private final Long    id;
  private final String  vonageUserId;
  private final Long    accountId;
  private final String  nome;
  private final Boolean attivo;

  /**
   * @param id           chiave primaria
   * @param vonageUserId nome utente nel sistema Vonage (claim {@code sub} del JWT SDK)
   * @param accountId    id account applicativo associato, o {@code null} se non collegato
   * @param nome         nome visualizzato dell'operatore, o {@code null}
   * @param attivo       true se l'operatore è abilitato
   */
  public OperatorDTO(Long id, String vonageUserId, Long accountId,
                     String nome, Boolean attivo)
  {
    this.id           = id;
    this.vonageUserId = vonageUserId;
    this.accountId    = accountId;
    this.nome         = nome;
    this.attivo       = attivo;
  }

  /** @return chiave primaria */
  public Long id() { return id; }

  /** @return identificatore utente nel sistema Vonage */
  public String vonageUserId() { return vonageUserId; }

  /** @return id account applicativo associato, o {@code null} */
  public Long accountId() { return accountId; }

  /** @return nome visualizzato, o {@code null} */
  public String nome() { return nome; }

  /** @return true se l'operatore è abilitato */
  public Boolean attivo() { return attivo; }
}
