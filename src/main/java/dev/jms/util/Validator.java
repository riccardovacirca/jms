package dev.jms.util;

import java.util.function.Predicate;
import java.util.function.UnaryOperator;

/**
 * Validatore statico minimale, senza reflection né annotazioni.
 * I metodi lanciano ValidationException (unchecked) al primo vincolo violato.
 * Si compongono in catena: Validator.maxLength(Validator.required(v, "x"), 50, "x")
 */
public final class Validator
{
  private Validator() {}

  // -------------------------
  // STRING
  // -------------------------

  /** Fallisce se il valore è null o blank. */
  public static String required(String value, String name)
  {
    if (value == null || value.isBlank()) {
      throw new ValidationException(name + " is required");
    }
    return value;
  }

  /** Fallisce se il valore è non-null ed è blank (whitespace). Null è accettato. */
  public static String notBlank(String value, String name)
  {
    if (value != null && value.isBlank()) {
      throw new ValidationException(name + " must not be blank");
    }
    return value;
  }

  /** Fallisce se la lunghezza supera max. Null è accettato. */
  public static String maxLength(String value, int max, String name)
  {
    if (value != null && value.length() > max) {
      throw new ValidationException(name + " too long (max " + max + ")");
    }
    return value;
  }

  /** Fallisce se la lunghezza è inferiore a min. Null è accettato. */
  public static String minLength(String value, int min, String name)
  {
    if (value != null && value.length() < min) {
      throw new ValidationException(name + " too short (min " + min + ")");
    }
    return value;
  }

  /** Fallisce se il valore non corrisponde alla regex. Null è accettato. */
  public static String pattern(String value, String regex, String name)
  {
    if (value != null && !value.matches(regex)) {
      throw new ValidationException(name + " invalid format");
    }
    return value;
  }

  /** Fallisce se il valore non è un indirizzo email valido. Null è accettato. */
  public static String email(String value, String name)
  {
    return pattern(value, "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$", name);
  }

  /** Fallisce se il valore non è un UUID nel formato standard. Null è accettato. */
  public static String uuid(String value, String name)
  {
    return pattern(
      value,
      "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
      name
    );
  }

  /** Fallisce se il valore non è uno di quelli consentiti. */
  public static String oneOf(String value, String name, String... allowed)
  {
    for (String a : allowed) {
      if (a.equals(value)) {
        return value;
      }
    }
    throw new ValidationException(name + " invalid value");
  }

  // -------------------------
  // NUMERIC
  // -------------------------

  /** Parsa e fallisce se il valore non è un intero positivo (> 0). */
  public static int positiveInt(String value, String name)
  {
    int v;
    try {
      v = Integer.parseInt(value);
    } catch (NumberFormatException e) {
      throw new ValidationException(name + " must be a positive integer");
    }
    if (v <= 0) {
      throw new ValidationException(name + " must be a positive integer");
    }
    return v;
  }

  /** Fallisce se il valore è fuori dall'intervallo [min, max]. */
  public static int range(int value, int min, int max, String name)
  {
    if (value < min || value > max) {
      throw new ValidationException(name + " out of range (" + min + "-" + max + ")");
    }
    return value;
  }

  // -------------------------
  // BOOLEAN
  // -------------------------

  /** Parsa e fallisce se il valore non è "true" o "false" (case-insensitive). */
  public static boolean bool(String value, String name)
  {
    if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
      throw new ValidationException(name + " must be boolean");
    }
    return Boolean.parseBoolean(value);
  }

  // -------------------------
  // HELPERS
  // -------------------------

  /**
   * Applica il validatore solo se il valore è non-null e non-blank.
   * Utile per campi opzionali che, se forniti, devono rispettare un vincolo.
   * Esempio: Validator.optional(value, v -> Validator.email(v, "email"))
   */
  public static String optional(String value, UnaryOperator<String> validator)
  {
    if (value == null || value.isBlank()) {
      return value;
    }
    return validator.apply(value);
  }

  /**
   * Applica un predicato personalizzato.
   * Esempio: Validator.custom(value, v -> v.startsWith("IT"), "codice")
   */
  public static <T> T custom(T value, Predicate<T> predicate, String name)
  {
    if (!predicate.test(value)) {
      throw new ValidationException(name + " invalid value");
    }
    return value;
  }
}
