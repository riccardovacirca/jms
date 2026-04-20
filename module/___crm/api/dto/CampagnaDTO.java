package dev.jms.app.crm.dto;

/** DTO immutabile per una campagna CRM. */
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
