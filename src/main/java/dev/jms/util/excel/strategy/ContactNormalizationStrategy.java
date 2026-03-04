package dev.jms.util.excel.strategy;

import java.util.HashMap;
import java.util.Map;

/**
 * Strategia di normalizzazione per contatti CRM. Normalizza telefoni, email, nomi, cognomi secondo
 * le regole del dominio.
 */
public class ContactNormalizationStrategy implements NormalizationStrategy
{

  @Override
  public Map<String, Object> normalize(Map<String, Object> row)
  {
    Map<String, Object> normalized = new HashMap<>();

    for (Map.Entry<String, Object> entry : row.entrySet()) {
      String key = entry.getKey();
      Object value = entry.getValue();

      if (value == null) {
        normalized.put(key, null);
        continue;
      }

      // Normalizzazione specifica per campo
      switch (key.toLowerCase()) {
        case "telefono" :
        case "phone" :
        case "tel" :
          normalized.put(key, normalizePhone(value));
          break;

        case "email" :
        case "mail" :
          normalized.put(key, normalizeEmail(value));
          break;

        case "nome" :
        case "name" :
        case "first_name" :
        case "firstname" :
          normalized.put(key, normalizeName(value));
          break;

        case "cognome" :
        case "surname" :
        case "last_name" :
        case "lastname" :
          normalized.put(key, normalizeName(value));
          break;

        default :
          // Per altri campi: trim se stringa
          if (value instanceof String) {
            normalized.put(key, ((String) value).trim());
          } else {
            normalized.put(key, value);
          }
      }
    }

    return normalized;
  }

  /**
   * Normalizza numero di telefono. - Converte numeri (double/float) in stringhe senza decimali -
   * Rimuove spazi e caratteri speciali
   */
  private String normalizePhone(Object value)
  {
    String phone;

    if (value instanceof Number) {
      // Converte numeri in stringhe senza decimali
      long longValue = ((Number) value).longValue();
      phone = String.valueOf(longValue);
    } else {
      phone = value.toString();
    }

    // Rimuove spazi, trattini, parentesi
    phone = phone.replaceAll("[\\s\\-\\(\\)\\.]", "");

    // Rimuove prefisso +39 o 0039 per numeri italiani (opzionale)
    // phone = phone.replaceFirst("^(\\+39|0039)", "");

    return phone.isEmpty() ? null : phone;
  }

  /** Normalizza email: trim e lowercase */
  private String normalizeEmail(Object value)
  {
    String email = value.toString().trim().toLowerCase();
    return email.isEmpty() ? null : email;
  }

  /** Normalizza nome/cognome: trim e capitalize */
  private String normalizeName(Object value)
  {
    String name = value.toString().trim();
    if (name.isEmpty()) {
      return null;
    }

    // Capitalize: prima lettera maiuscola, resto minuscolo
    return name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
  }
}
