package dev.jms.app.contatti.dto;

public record ListaDTO(
  Integer id,
  String  nome,
  String  descrizione,
  boolean consenso,
  int     stato,
  String  scadenza,
  String  createdAt,
  String  updatedAt,
  String  deletedAt,
  long    contattiCount
) {}
