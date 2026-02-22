package {{APP_PACKAGE}};

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Config {

  private final Properties props = new Properties();

  private static final String EXTERNAL_CONFIG = "/app/config/application.properties";

  public Config() {
    File external = new File(EXTERNAL_CONFIG);
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

  public String get(String key, String defaultValue) {
    String envKey = key.toUpperCase().replace('.', '_');
    String envVal = System.getenv(envKey);
    if (envVal != null && !envVal.isBlank()) return envVal;
    return props.getProperty(key, defaultValue);
  }

  public int getInt(String key, int defaultValue) {
    try {
      return Integer.parseInt(get(key, String.valueOf(defaultValue)));
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }
}
