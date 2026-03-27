package dev.jms.util;

/**
 * Ruoli utente gerarchici.
 * Ogni ruolo include tutti i privilegi dei ruoli di livello inferiore.
 *
 * <ul>
 *   <li>{@code GUEST}  (0) — stato implicito non autenticato; solo lettura su risorse pubbliche.</li>
 *   <li>{@code USER}   (1) — utente autenticato standard.</li>
 *   <li>{@code ADMIN}  (2) — amministratore; può gestire account USER.</li>
 *   <li>{@code ROOT}   (3) — superutente; può gestire account ADMIN; unico per installazione.</li>
 * </ul>
 *
 * <p>GUEST non è un ruolo DB: è lo stato implicito quando manca il JWT.
 * I ruoli nel DB sono: {@code user}, {@code admin}, {@code root}.
 */
public enum Role
{
  GUEST(0),
  USER(1),
  ADMIN(2),
  ROOT(3);

  private final int level;

  Role(int level)
  {
    this.level = level;
  }

  /** Livello numerico del ruolo (cresce con i privilegi). */
  public int level()
  {
    return level;
  }

  /**
   * Restituisce il ruolo corrispondente al livello numerico.
   *
   * @param level livello numerico
   * @return il {@link Role} corrispondente
   * @throws IllegalArgumentException se il livello non corrisponde a nessun ruolo
   */
  public static Role of(int level)
  {
    Role result;

    result = null;
    for (Role r : values()) {
      if (r.level == level) {
        result = r;
        break;
      }
    }
    if (result == null) {
      throw new IllegalArgumentException("Livello ruolo non valido: " + level);
    }
    return result;
  }

  /**
   * Restituisce il ruolo corrispondente al nome stringa (case-insensitive).
   *
   * @param name nome del ruolo (es. "admin")
   * @return il {@link Role} corrispondente
   * @throws IllegalArgumentException se il nome non corrisponde a nessun ruolo
   */
  public static Role of(String name)
  {
    if (name == null) {
      throw new IllegalArgumentException("Nome ruolo null");
    }
    return Role.valueOf(name.toUpperCase());
  }
}
