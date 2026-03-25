package dev.jms.app.module.aes.helper;

import dev.jms.util.Config;
import dev.jms.util.File;
import dev.jms.util.Log;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Job schedulato per la pulizia automatica dei file temporanei del modulo aes.
 * <p>
 * Elimina file più vecchi di {@code aes.temp.retention.days} giorni dalla directory {@code /tmp/aes/}.
 * </p>
 */
public class CleanupJob
{
  private static final Log log = Log.get(CleanupJob.class);
  private static final String BASE_DIR = "/tmp/aes";

  /**
   * Esegue la pulizia dei file temporanei.
   * Metodo statico invocato dallo scheduler.
   */
  public static void run()
  {
    Config config;
    int retentionDays;
    Instant cutoff;
    int deleted;

    try {
      config = new Config();
      retentionDays = config.getInt("aes.temp.retention.days", 7);
      cutoff = Instant.now().minus(retentionDays, ChronoUnit.DAYS);

      log.info("[AES Cleanup] Started - retention: {} days, cutoff: {}", retentionDays, cutoff);

      if (!File.exists(BASE_DIR)) {
        log.info("[AES Cleanup] Directory {} does not exist, skipping", BASE_DIR);
        return;
      }

      deleted = cleanupDirectory(BASE_DIR, cutoff);

      log.info("[AES Cleanup] Completed - deleted {} files", deleted);

    } catch (Exception e) {
      log.error("[AES Cleanup] Failed", e);
    }
  }

  /**
   * Scansiona ricorsivamente una directory e elimina file più vecchi del cutoff.
   *
   * @param dirPath path directory da scansionare
   * @param cutoff  instant cutoff - file più vecchi vengono eliminati
   * @return numero di file eliminati
   */
  private static int cleanupDirectory(String dirPath, Instant cutoff) throws Exception
  {
    Path dir;
    Stream<Path> stream;
    List<Path> oldFiles;
    int deleted;

    dir = Path.of(dirPath);
    stream = Files.walk(dir);
    oldFiles = stream
      .filter(Files::isRegularFile)
      .filter(p -> isOlderThan(p, cutoff))
      .collect(Collectors.toList());
    stream.close();

    deleted = 0;
    for (Path file : oldFiles) {
      try {
        Files.delete(file);
        deleted++;
        log.debug("[AES Cleanup] Deleted: {}", file);
      } catch (Exception e) {
        log.warn("[AES Cleanup] Failed to delete {}: {}", file, e.getMessage());
      }
    }

    return deleted;
  }

  /**
   * Verifica se un file è più vecchio del cutoff specificato.
   *
   * @param file   path del file
   * @param cutoff instant cutoff
   * @return true se il file è più vecchio
   */
  private static boolean isOlderThan(Path file, Instant cutoff)
  {
    BasicFileAttributes attrs;
    Instant lastModified;

    try {
      attrs = Files.readAttributes(file, BasicFileAttributes.class);
      lastModified = attrs.lastModifiedTime().toInstant();
      return lastModified.isBefore(cutoff);
    } catch (Exception e) {
      log.warn("[AES Cleanup] Cannot read attributes for {}: {}", file, e.getMessage());
      return false;
    }
  }
}
