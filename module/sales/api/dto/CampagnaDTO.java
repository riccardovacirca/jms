package dev.jms.app.sales.dto;

/** DTO immutabile per una campagna. */
public record CampagnaDTO(
  Integer id,
  String  nome,
  String  descrizione,
  int     stato,
  String  createdAt,
  String  updatedAt,
  String  deletedAt,
  long    listeCount
) {}
