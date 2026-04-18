package dev.jms.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Carica la configurazione applicativa da /app/config/application.properties.
 * Le variabili d'ambiente hanno precedenza sulle chiavi del file (es. DB_HOST sovrascrive db.host).
 */
public class Config
{
  private final Properties props = new Properties();

  private static final String EXTERNAL_CONFIG = "/app/config/application.properties";

  /**
   * Carica la configurazione da {@value #EXTERNAL_CONFIG}.
   * Termina il processo con exit code 1 se il file non esiste o non è leggibile.
   */
  public Config()
  {
    File external;

    external = new File(EXTERNAL_CONFIG);
    if (!external.exists()) {
      System.err.println("[error] File di configurazione non trovato: " + EXTERNAL_CONFIG);
      System.exit(1);
    }
    try (InputStream in = new FileInputStream(external)) {
      props.load(in);
      System.out.println("[info] Configurazione caricata da " + EXTERNAL_CONFIG);
    } catch (IOException e) {
      System.err.println("[error] Errore lettura configurazione: " + e.getMessage());
      System.exit(1);
    }
  }

  /**
   * Restituisce il valore della proprietà, con override da variabile d'ambiente.
   * La chiave viene convertita in uppercase con underscore (es. {@code db.host} → {@code DB_HOST}).
   *
   * @param key          chiave della proprietà
   * @param defaultValue valore di fallback se la chiave non è presente
   * @return valore risolto
   */
  public String get(String key, String defaultValue)
  {
    String envKey;
    String envVal;
    String result;

    envKey = key.toUpperCase().replace('.', '_');
    envVal = System.getenv(envKey);
    if (envVal != null && !envVal.isBlank()) {
      result = envVal;
    } else {
      result = props.getProperty(key, defaultValue);
    }
    return result;
  }

  /**
   * Restituisce il valore della proprietà come intero.
   * Ritorna {@code defaultValue} se la chiave non è presente o non è un intero valido.
   *
   * @param key          chiave della proprietà
   * @param defaultValue valore di fallback
   * @return valore intero risolto
   */
  public int getInt(String key, int defaultValue)
  {
    int result;

    try {
      result = Integer.parseInt(get(key, String.valueOf(defaultValue)));
    } catch (NumberFormatException e) {
      result = defaultValue;
    }
    return result;
  }

  /**
   * Restituisce tutte le proprietà con prefisso {@code public.} come mappa,
   * strippando il prefisso dalle chiavi.
   * Supporta override via variabile d'ambiente (es. {@code PUBLIC_APP_TITLE}
   * sovrascrive {@code public.app_title}).
   *
   * @return mappa delle proprietà pubbliche (chiave senza prefisso → valore)
   */
  public java.util.Map<String, String> getPublic()
  {
    java.util.Map<String, String> result;
    String stripped;
    String value;

    result = new java.util.LinkedHashMap<>();
    for (String key : props.stringPropertyNames()) {
      if (key.startsWith("public.")) {
        stripped = key.substring("public.".length());
        value = get(key, "");
        result.put(stripped, value);
      }
    }
    return result;
  }
}
