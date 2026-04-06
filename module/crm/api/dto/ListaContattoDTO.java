package dev.jms.app.crm.dto;

public record ListaContattoDTO(
  Integer id,
  int     listaId,
  int     contattoId,
  String  createdAt,
  String  nome,
  String  cognome,
  String  telefono
) {}
