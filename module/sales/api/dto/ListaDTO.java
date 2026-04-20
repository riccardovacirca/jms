package dev.jms.app.sales.dto;

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
  boolean isDefault,
  long    contattiCount
) {}
