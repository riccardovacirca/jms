package dev.jms.app.crm.dto;

/** Rappresenta un contatto con tutti i suoi campi. */
public record ContattoDTO(
  Integer id,
  String  nome,
  String  cognome,
  String  ragioneSociale,
  String  telefono,
  String  email,
  String  indirizzo,
  String  citta,
  String  cap,
  String  provincia,
  String  note,
  int     stato,
  boolean consenso,
  boolean blacklist,
  String  createdAt,
  String  updatedAt,
  long    listeCount
) {}
