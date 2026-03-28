/*
 * tools - Java Tools Library
 * Copyright (C) 2018-2025 Riccardo Vacirca
 * Licensed under Exclusive Free Beta License
 * See LICENSE.md for full terms
 */
package dev.jms.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Java database abstraction layer.
 * Thin wrapper over plain JDBC with HikariCP as DataSource provider.
 * No annotations, no AOP, no hidden behavior.
 */
public class DB
{
  private static HikariDataSource sharedDataSource;

  private final DataSource dataSource;

  /** Connection bound to current thread */
  private final ThreadLocal<Connection> connection;

  /** Last generated key bound to current thread */
  private final ThreadLocal<Long> lastGeneratedKey;

  public DB(DataSource dataSource)
  {
    this.dataSource = dataSource;
    this.connection = new ThreadLocal<>();
    this.lastGeneratedKey = ThreadLocal.withInitial(() -> -1L);
  }

  // =========================
  // STATIC INIT
  // =========================

  /**
   * Da chiamare una volta in App.main() prima di avviare il server.
   * Non inizializza se db.host, db.name o db.user sono assenti.
   */
  public static void init(Config config)
  {
    String host;
    String name;
    String user;
    String dbPort;
    String password;
    int poolSize;
    HikariConfig hc;

    host = config.get("db.host", "");
    name = config.get("db.name", "");
    user = config.get("db.user", "");

    if (host.isBlank() || name.isBlank() || user.isBlank()) {
      System.out.println("[info] Database non configurato, pool non inizializzato");
    } else {
      dbPort = config.get("db.port", "5432");
      password = config.get("db.password", "");
      poolSize = config.getInt("db.pool.size", 10);

      try {
        hc = new HikariConfig();
        hc.setJdbcUrl("jdbc:postgresql://" + host + ":" + dbPort + "/" + name);
        hc.setUsername(user);
        hc.setPassword(password);
        hc.setMaximumPoolSize(poolSize);
        hc.setInitializationFailTimeout(-1);
        sharedDataSource = new HikariDataSource(hc);
        System.out.println("[info] Pool database inizializzato (" + host + ":" + dbPort + "/" + name + ")");
      } catch (Exception e) {
        System.err.println("[warn] Inizializzazione pool fallita: " + e.getMessage());
      }
    }
  }

  /**
   * Restituisce {@code true} se il pool HikariCP è stato inizializzato con successo.
   *
   * @return {@code true} se il database è configurato e raggiungibile
   */
  public static boolean isConfigured()
  {
    return sharedDataSource != null;
  }

  /**
   * Restituisce il {@link DataSource} condiviso HikariCP.
   * Restituisce {@code null} se il database non è configurato.
   *
   * @return DataSource condiviso, o {@code null}
   */
  public static DataSource getDataSource()
  {
    return sharedDataSource;
  }

  // =========================
  // CONNECTION LIFECYCLE
  // =========================

  /**
   * Apre una connessione dal pool e la associa al thread corrente.
   * No-op se una connessione è già aperta sul thread.
   *
   * @throws Exception se la connessione non può essere acquisita
   */
  public void open() throws Exception
  {
    Connection c;
    if (connection.get() == null) {
      c = dataSource.getConnection();
      connection.set(c);
    }
  }

  /**
   * Rilascia la connessione associata al thread corrente e la restituisce al pool.
   */
  public void close()
  {
    Connection c;
    c = connection.get();
    if (c != null) {
      try {
        c.close();
      } catch (Exception ignored) {}
      connection.remove();
      lastGeneratedKey.remove();
    }
  }

  /**
   * Verifica se la connessione corrente è aperta e non chiusa.
   *
   * @return {@code true} se la connessione è attiva
   */
  public boolean connected()
  {
    Connection c;
    boolean result;
    result = false;
    try {
      c = connection.get();
      result = c != null && !c.isClosed();
    } catch (SQLException e) {
      result = false;
    }
    return result;
  }

  private Connection requireConnection() throws Exception
  {
    Connection c;
    c = connection.get();
    if (c == null) {
      throw new Exception("Connection not available (call open())");
    }
    return c;
  }

  // =========================
  // TRANSACTIONS (MANUAL)
  // =========================

  /**
   * Avvia una transazione manuale disabilitando l'auto-commit.
   *
   * @throws Exception se la connessione non è disponibile
   */
  public void begin() throws Exception
  {
    Connection c;
    c = requireConnection();
    c.setAutoCommit(false);
  }

  /**
   * Esegue il commit della transazione corrente e riabilita l'auto-commit.
   *
   * @throws Exception se la connessione non è disponibile o il commit fallisce
   */
  public void commit() throws Exception
  {
    Connection c;
    c = requireConnection();
    c.commit();
    c.setAutoCommit(true);
  }

  /**
   * Esegue il rollback della transazione corrente e riabilita l'auto-commit.
   *
   * @throws Exception se la connessione non è disponibile o il rollback fallisce
   */
  public void rollback() throws Exception
  {
    Connection c;
    c = requireConnection();
    c.rollback();
    c.setAutoCommit(true);
  }

  // =========================
  // WRITE QUERIES
  // =========================

  /**
   * Esegue una query di scrittura (INSERT, UPDATE, DELETE) e restituisce il numero di righe modificate.
   * Aggiorna {@link #lastInsertId()} se la query genera una chiave auto-generata.
   *
   * @param sql    istruzione SQL con placeholder {@code ?}
   * @param params parametri da legare ai placeholder
   * @return numero di righe modificate
   * @throws Exception se l'esecuzione fallisce
   */
  public int query(String sql, Object... params) throws Exception
  {
    Connection c;
    int rows;

    c = requireConnection();
    rows = 0;
    try (PreparedStatement stmt = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
      bindParameters(stmt, params);
      rows = stmt.executeUpdate();
      try (ResultSet keys = stmt.getGeneratedKeys()) {
        if (keys.next()) {
          Object key;
          key = keys.getObject(1);
          lastGeneratedKey.set(key instanceof Number ? ((Number) key).longValue() : -1L);
        } else {
          lastGeneratedKey.set(-1L);
        }
      }
    }
    return rows;
  }

  /**
   * Restituisce l'ultima chiave auto-generata dall'operazione {@link #query} precedente.
   *
   * @return chiave auto-generata
   * @throws Exception se nessuna chiave auto-generata è disponibile
   */
  public long lastInsertId() throws Exception
  {
    long id;
    id = lastGeneratedKey.get();
    if (id == -1) {
      throw new Exception("No auto-generated key available");
    }
    return id;
  }

  // =========================
  // READ QUERIES
  // =========================

  /**
   * Esegue una query di lettura e restituisce tutte le righe come lista di mappe.
   * Ogni mappa ha come chiave il nome della colonna e come valore il dato tipizzato JDBC.
   *
   * @param sql    istruzione SQL con placeholder {@code ?}
   * @param params parametri da legare ai placeholder
   * @return lista di righe; vuota se nessuna riga trovata
   * @throws Exception se l'esecuzione fallisce
   */
  public ArrayList<HashMap<String, Object>> select(String sql, Object... params) throws Exception
  {
    Connection c;
    ArrayList<HashMap<String, Object>> rsSet;

    c = requireConnection();
    rsSet = new ArrayList<>();

    try (PreparedStatement stmt = c.prepareStatement(sql)) {
      bindParameters(stmt, params);
      try (ResultSet rs = stmt.executeQuery()) {
        ResultSetMetaData meta;
        int cols;

        meta = rs.getMetaData();
        cols = meta.getColumnCount();

        while (rs.next()) {
          HashMap<String, Object> r;
          r = new HashMap<>();
          for (int i = 1; i <= cols; i++) {
            r.put(meta.getColumnName(i), rs.getObject(i));
          }
          rsSet.add(r);
        }
      }
    }
    return rsSet;
  }

  // =========================
  // CURSOR (STREAMING)
  // =========================

  /**
   * Apre un cursore di streaming per iterare righe senza caricarle tutte in memoria.
   * Chiudere il {@link Cursor} dopo l'uso per liberare le risorse JDBC.
   *
   * @param sql    istruzione SQL con placeholder {@code ?}
   * @param params parametri da legare ai placeholder
   * @return cursore posizionato prima della prima riga
   * @throws Exception se l'esecuzione fallisce
   */
  public Cursor cursor(String sql, Object... params) throws Exception
  {
    Connection c;
    PreparedStatement stmt;
    ResultSet rs;

    c = requireConnection();
    stmt = c.prepareStatement(sql);
    bindParameters(stmt, params);
    rs = stmt.executeQuery();
    return new Cursor(rs, stmt);
  }

  // =========================
  // METADATA
  // =========================

  /**
   * Restituisce l'insieme dei nomi di colonna (in minuscolo) per la tabella indicata.
   *
   * @param tableName nome della tabella da interrogare
   * @return insieme di nomi colonna in minuscolo
   * @throws Exception se la query sui metadati fallisce
   */
  public HashSet<String> getTableColumns(String tableName) throws Exception
  {
    Connection c;
    HashSet<String> columns;
    DatabaseMetaData meta;
    ResultSet rs;

    c = requireConnection();
    columns = new HashSet<>();
    meta = c.getMetaData();
    rs = meta.getColumns(null, null, tableName, null);

    if (!rs.next()) {
      rs.close();
      rs = meta.getColumns(null, null, tableName.toUpperCase(), null);
    } else {
      rs.close();
      rs = meta.getColumns(null, null, tableName, null);
    }

    while (rs.next()) {
      String name;
      name = rs.getString("COLUMN_NAME");
      if (name != null) {
        columns.add(name.toLowerCase());
      }
    }
    rs.close();
    return columns;
  }

  // =========================
  // HELPERS
  // =========================

  private void bindParameters(PreparedStatement stmt, Object... params) throws SQLException
  {
    for (int i = 0; i < params.length; i++) {
      stmt.setObject(i + 1, params[i]);
    }
  }

  // ========================================
  // Type Conversion Helpers (Java 8+ Time API)
  // ========================================

  /**
   * Converte un valore {@link java.sql.Date} JDBC in {@link java.time.LocalDate}.
   *
   * @param sqlDate valore proveniente da un ResultSet
   * @return LocalDate corrispondente, o {@code null} se il valore non è un {@link java.sql.Date}
   */
  public static java.time.LocalDate toLocalDate(Object sqlDate)
  {
    java.time.LocalDate result;
    result = null;
    if (sqlDate instanceof java.sql.Date) {
      result = ((java.sql.Date) sqlDate).toLocalDate();
    }
    return result;
  }

  /**
   * Converte un valore {@link java.sql.Time} JDBC in {@link java.time.LocalTime}.
   *
   * @param sqlTime valore proveniente da un ResultSet
   * @return LocalTime corrispondente, o {@code null} se il valore non è un {@link java.sql.Time}
   */
  public static java.time.LocalTime toLocalTime(Object sqlTime)
  {
    java.time.LocalTime result;
    result = null;
    if (sqlTime instanceof java.sql.Time) {
      result = ((java.sql.Time) sqlTime).toLocalTime();
    }
    return result;
  }

  /**
   * Converte un valore {@link java.sql.Timestamp} JDBC o una stringa ISO in {@link java.time.LocalDateTime}.
   *
   * @param sqlTimestamp valore proveniente da un ResultSet o stringa ISO-8601
   * @return LocalDateTime corrispondente, o {@code null} se la conversione non è possibile
   */
  public static java.time.LocalDateTime toLocalDateTime(Object sqlTimestamp)
  {
    java.time.LocalDateTime result;
    result = null;
    if (sqlTimestamp instanceof java.sql.Timestamp) {
      result = ((java.sql.Timestamp) sqlTimestamp).toLocalDateTime();
    } else if (sqlTimestamp instanceof String) {
      try {
        result = java.time.LocalDateTime.parse((String) sqlTimestamp);
      } catch (Exception e) {
        result = null;
      }
    }
    return result;
  }

  /**
   * Converte un {@link java.time.LocalDate} in {@link java.sql.Date} per i parametri JDBC.
   *
   * @param localDate data da convertire
   * @return Date SQL corrispondente, o {@code null} se {@code localDate} è {@code null}
   */
  public static java.sql.Date toSqlDate(java.time.LocalDate localDate)
  {
    java.sql.Date result;
    result = null;
    if (localDate != null) {
      result = java.sql.Date.valueOf(localDate);
    }
    return result;
  }

  /**
   * Converte un {@link java.time.LocalTime} in {@link java.sql.Time} per i parametri JDBC.
   *
   * @param localTime ora da convertire
   * @return Time SQL corrispondente, o {@code null} se {@code localTime} è {@code null}
   */
  public static java.sql.Time toSqlTime(java.time.LocalTime localTime)
  {
    java.sql.Time result;
    result = null;
    if (localTime != null) {
      result = java.sql.Time.valueOf(localTime);
    }
    return result;
  }

  /**
   * Converte un {@link java.time.LocalDateTime} in {@link java.sql.Timestamp} per i parametri JDBC.
   *
   * @param localDateTime data-ora da convertire
   * @return Timestamp SQL corrispondente, o {@code null} se {@code localDateTime} è {@code null}
   */
  public static java.sql.Timestamp toSqlTimestamp(java.time.LocalDateTime localDateTime)
  {
    java.sql.Timestamp result;
    result = null;
    if (localDateTime != null) {
      result = java.sql.Timestamp.valueOf(localDateTime);
    }
    return result;
  }

  /**
   * Converte un valore numerico proveniente da un ResultSet in {@link Long}.
   *
   * @param value valore da convertire
   * @return Long corrispondente, o {@code null} se il valore non è numerico
   */
  public static Long toLong(Object value)
  {
    Long result;
    result = null;
    if (value instanceof Long) {
      result = (Long) value;
    } else if (value instanceof Number) {
      result = ((Number) value).longValue();
    }
    return result;
  }

  /**
   * Converte un valore numerico proveniente da un ResultSet in {@link Integer}.
   *
   * @param value valore da convertire
   * @return Integer corrispondente, o {@code null} se il valore non è numerico
   */
  public static Integer toInteger(Object value)
  {
    Integer result;
    result = null;
    if (value instanceof Integer) {
      result = (Integer) value;
    } else if (value instanceof Number) {
      result = ((Number) value).intValue();
    }
    return result;
  }

  /**
   * Converte un valore booleano o numerico proveniente da un ResultSet in {@link Boolean}.
   * Un numero != 0 è considerato {@code true}.
   *
   * @param value valore da convertire
   * @return Boolean corrispondente, o {@code null} se il valore non è compatibile
   */
  public static Boolean toBoolean(Object value)
  {
    Boolean result;
    result = null;
    if (value instanceof Boolean) {
      result = (Boolean) value;
    } else if (value instanceof Number) {
      result = ((Number) value).intValue() != 0;
    }
    return result;
  }

  /**
   * Converte qualsiasi valore proveniente da un ResultSet in {@link String} tramite {@code toString()}.
   *
   * @param value valore da convertire
   * @return stringa corrispondente, o {@code null} se {@code value} è {@code null}
   */
  public static String toString(Object value)
  {
    String result;
    result = null;
    if (value != null) {
      result = value.toString();
    }
    return result;
  }

  /**
   * Converte un valore {@link java.math.BigDecimal} proveniente da un ResultSet.
   *
   * @param value valore da convertire
   * @return BigDecimal corrispondente, o {@code null} se il valore non è un BigDecimal
   */
  public static java.math.BigDecimal toBigDecimal(Object value)
  {
    java.math.BigDecimal result;
    result = null;
    if (value instanceof java.math.BigDecimal) {
      result = (java.math.BigDecimal) value;
    }
    return result;
  }

  // =========================
  // CURSOR (streaming)
  // =========================

  public static class Cursor
  {
    private final ResultSet rs;
    private final PreparedStatement stmt;

    Cursor(ResultSet rs, PreparedStatement stmt)
    {
      this.rs = rs;
      this.stmt = stmt;
    }

    public boolean next() throws Exception
    {
      return rs.next();
    }

    public Object get(String column) throws Exception
    {
      return rs.getObject(column);
    }

    public HashMap<String, Object> getRow() throws Exception
    {
      HashMap<String, Object> r;
      ResultSetMetaData meta;
      int cols;

      r = new HashMap<>();
      meta = rs.getMetaData();
      cols = meta.getColumnCount();

      for (int i = 1; i <= cols; i++) {
        r.put(meta.getColumnName(i), rs.getObject(i));
      }
      return r;
    }

    public void close()
    {
      try {
        rs.close();
      } catch (Exception ignored) {}
      try {
        stmt.close();
      } catch (Exception ignored) {}
    }
  }
}
