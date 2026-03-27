package dev.jms.util;

/**
 * Tipi di permesso per il controllo degli accessi.
 *
 * <ul>
 *   <li>{@code READ}  — accesso in lettura; può essere concesso anche a {@link Role#GUEST}.</li>
 *   <li>{@code WRITE} — accesso in scrittura, modifica o cancellazione; richiede almeno {@link Role#USER}.</li>
 * </ul>
 *
 * <p>Usato insieme a {@link Role} in {@link Access#require(dev.jms.util.HttpRequest, Role, Permission)}.
 */
public enum Permission
{
  READ,
  WRITE
}
