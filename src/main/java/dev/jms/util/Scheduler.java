package dev.jms.util;

import org.jobrunr.configuration.JobRunr;
import org.jobrunr.configuration.JobRunrConfiguration.JobRunrConfigurationResult;
import org.jobrunr.jobs.lambdas.JobLambda;
import org.jobrunr.scheduling.BackgroundJob;
import org.jobrunr.server.BackgroundJobServer;
import org.jobrunr.server.BackgroundJobServerConfiguration;
import org.jobrunr.storage.sql.postgres.PostgresStorageProvider;
import javax.sql.DataSource;

/**
 * Scheduler per job pianificati in stile cron, basato su JobRunr.
 *
 * <p>Utilizza PostgreSQL come storage persistente: i job sopravvivono ai riavvii
 * e non vengono rieseguiti in caso di restart rapido. Le tabelle {@code jobrunr_*}
 * vengono create automaticamente al primo avvio.</p>
 *
 * <p>Inizializzare una volta in {@code App.main()} dopo {@code DB.init()},
 * poi registrare i job ricorrenti tramite {@link #register(String, String, JobLambda)}.</p>
 *
 * <p>I metodi schedulati devono essere statici e privi di parametri per garantire
 * la corretta serializzazione da parte di JobRunr:</p>
 * <pre>
 *   Scheduler.register("cleanup-nightly", "0 2 * * *", CleanupHandler::nightly);
 * </pre>
 *
 * <p>Proprietà di configurazione:</p>
 * <ul>
 *   <li>{@code scheduler.enabled} — abilita/disabilita lo scheduler (default: {@code true})</li>
 *   <li>{@code scheduler.poll.interval.seconds} — intervallo di polling in secondi (default: {@code 15})</li>
 * </ul>
 */
public class Scheduler
{
  private static JobRunrConfigurationResult jobRunr;

  /**
   * Inizializza lo scheduler con PostgreSQL come storage.
   * Da chiamare in {@code App.main()} dopo {@code DB.init()}.
   * Se {@code scheduler.enabled=false} l'inizializzazione viene saltata
   * e le successive chiamate a {@link #register} sono no-op silenziosi.
   *
   * @param config configurazione applicazione
   * @param ds     DataSource PostgreSQL già inizializzato
   */
  public static void init(Config config, DataSource ds)
  {
    boolean enabled;
    int pollInterval;

    enabled = config.get("scheduler.enabled", "true").equalsIgnoreCase("true");

    if (enabled) {
      pollInterval = config.getInt("scheduler.poll.interval.seconds", 15);

      jobRunr = JobRunr.configure()
        .useStorageProvider(new PostgresStorageProvider(ds))
        .useBackgroundJobServer(
          BackgroundJobServerConfiguration.usingStandardBackgroundJobServerConfiguration()
            .andPollIntervalInSeconds(pollInterval)
        )
        .initialize();

      System.out.println("[info] Scheduler inizializzato (poll interval: " + pollInterval + "s)");
    } else {
      System.out.println("[info] Scheduler disabilitato (scheduler.enabled=false)");
    }
  }

  /**
   * Restituisce {@code true} se lo scheduler è stato inizializzato e abilitato.
   *
   * @return {@code true} se attivo
   */
  public static boolean isInitialized()
  {
    return jobRunr != null;
  }

  /**
   * Registra un job ricorrente identificato da un ID univoco.
   * Se lo scheduler non è inizializzato (disabilitato) la chiamata è un no-op silenzioso.
   * Se un job con lo stesso ID esiste già nel DB, viene aggiornato (idempotente).
   *
   * <p>Il metodo target deve essere statico e senza parametri:</p>
   * <pre>
   *   Scheduler.register("report-daily", "0 8 * * *", ReportHandler::daily);
   * </pre>
   *
   * @param id   identificatore univoco del job (chiave nel DB)
   * @param cron espressione cron standard a 5 campi (minuto ora giorno mese giorno-settimana)
   * @param job  method reference statico senza parametri
   */
  public static void register(String id, String cron, JobLambda job)
  {
    if (jobRunr != null) {
      BackgroundJob.scheduleRecurrently(id, cron, job);
      System.out.println("[info] Scheduler: job '" + id + "' registrato (" + cron + ")");
    }
  }

  /**
   * Termina lo scheduler gracefully.
   * Da aggiungere al shutdown hook in {@code App.main()}.
   */
  public static void shutdown()
  {
    BackgroundJobServer server;

    if (jobRunr != null) {
      server = JobRunr.getBackgroundJobServer();
      if (server != null) {
        server.stop();
      }
      System.out.println("[info] Scheduler terminato");
    }
  }
}
