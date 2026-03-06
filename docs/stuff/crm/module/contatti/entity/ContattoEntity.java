package dev.crm.module.contatti.entity;

import java.time.LocalDateTime;

public class ContattoEntity
{
  public Long id;
  public String nome;
  public String cognome;
  public String ragioneSociale;
  public String telefono;
  public String email;
  public String indirizzo;
  public String citta;
  public String cap;
  public String provincia;
  public String note;
  public Integer stato;
  public Boolean consenso;
  public Boolean blacklist;
  public LocalDateTime createdAt;
  public LocalDateTime updatedAt;
  public Integer listeCount;

  public ContattoEntity()
  {
  }

  public ContattoEntity(
      Long id,
      String nome,
      String cognome,
      String ragioneSociale,
      String telefono,
      String email,
      String indirizzo,
      String citta,
      String cap,
      String provincia,
      String note,
      Integer stato,
      Boolean consenso,
      Boolean blacklist,
      LocalDateTime createdAt,
      LocalDateTime updatedAt)
  {
    this.id = id;
    this.nome = nome;
    this.cognome = cognome;
    this.ragioneSociale = ragioneSociale;
    this.telefono = telefono;
    this.email = email;
    this.indirizzo = indirizzo;
    this.citta = citta;
    this.cap = cap;
    this.provincia = provincia;
    this.note = note;
    this.stato = stato;
    this.consenso = consenso;
    this.blacklist = blacklist;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
  }

  public String getDisplayName()
  {
    StringBuilder sb;

    if (ragioneSociale != null && !ragioneSociale.isEmpty()) {
      return ragioneSociale;
    }
    sb = new StringBuilder();
    if (cognome != null)
      sb.append(cognome);
    if (nome != null) {
      if (sb.length() > 0)
        sb.append(" ");
      sb.append(nome);
    }
    return sb.length() > 0 ? sb.toString() : "Contatto #" + id;
  }
}
