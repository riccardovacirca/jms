package dev.jms.util;

import io.undertow.server.handlers.CookieImpl;

/**
 * Gestione centralizzata dei cookie HTTP.
 *
 * <p>Responsabilità:
 * <ul>
 *   <li>Configurazione globale dei flag di sicurezza (Secure, SameSite).</li>
 *   <li>Nomi canonici dei cookie di autenticazione.</li>
 *   <li>Costruzione degli oggetti cookie con i flag corretti (usata da {@link HttpResponse}).</li>
 * </ul>
 *
 * <p>La configurazione va inizializzata una volta all'avvio tramite {@link #configure(boolean, String)}.
 * {@link HttpResponse#configureCookies(boolean, String)} delega a questo metodo.
 */
public final class Cookie
{
  /** Nome del cookie che trasporta il JWT di accesso (scadenza breve). */
  public static final String ACCESS_TOKEN  = "access_token";

  /** Nome del cookie che trasporta il token di refresh (scadenza lunga, opaco, persistito su DB). */
  public static final String REFRESH_TOKEN = "refresh_token";

  /** Nome del cookie che identifica la sessione server-side. */
  public static final String SESSION_ID = "session_id";

  private static boolean secure   = false;
  private static String  sameSite = "Lax";

  private Cookie() {}

  /**
   * Configura i flag di sicurezza applicati a tutti i cookie dell'applicazione.
   * Da chiamare una volta in {@code App.main()} (o tramite {@link HttpResponse#configureCookies}).
   *
   * @param secure   se {@code true} aggiunge il flag {@code Secure} (richiede HTTPS)
   * @param sameSite {@code Strict} | {@code Lax} | {@code None} (None richiede Secure=true)
   */
  public static void configure(boolean secure, String sameSite)
  {
    Cookie.secure   = secure;
    Cookie.sameSite = sameSite;
  }

  /**
   * Costruisce un cookie con i flag di sicurezza globali e la durata specificata.
   * Usato da {@link HttpResponse#cookie(String, String, int)}.
   *
   * @param name   nome del cookie
   * @param value  valore del cookie
   * @param maxAge durata in secondi
   * @return istanza {@link io.undertow.server.handlers.Cookie} pronta per essere inviata
   */
  static io.undertow.server.handlers.Cookie build(String name, String value, int maxAge)
  {
    io.undertow.server.handlers.Cookie cookie;

    cookie = new CookieImpl(name, value)
      .setHttpOnly(true)
      .setPath("/")
      .setMaxAge(maxAge)
      .setSameSiteMode(sameSite);

    if (secure) {
      cookie.setSecure(true);
    }
    return cookie;
  }

  /**
   * Costruisce un cookie di cancellazione (maxAge=0, valore vuoto).
   * Usato da {@link HttpResponse#clearCookie(String)}.
   *
   * @param name nome del cookie da cancellare
   * @return istanza {@link io.undertow.server.handlers.Cookie} di cancellazione
   */
  static io.undertow.server.handlers.Cookie buildCleared(String name)
  {
    return build(name, "", 0);
  }
}
