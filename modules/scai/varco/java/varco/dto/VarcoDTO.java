package {{APP_PACKAGE}}.varco.dto;

/**
 * Rappresenta un varco di accesso.
 */
public record VarcoDTO(
  Long    id,
  String  codiceVarco,     // max 6
  String  descRidotta,     // max 128
  String  descLunga,       // max 256
  String  codRepertorio,   // max 6 - FK verso scai_repertorio
  String  slugSdc,         // max 64 - FK verso scai_sistemi_campo
  String  createdAt,       // ISO-8601 timestamp
  Long    createdBy,       // User ID
  String  updatedAt,       // ISO-8601 timestamp
  Long    updatedBy        // User ID
) {}
