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
      return;
    }

    dbPort   = config.get("db.port", "5432");
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

  public static boolean isConfigured()
  {
    return sharedDataSource != null;
  }

  public static DataSource getDataSource()
  {
    return sharedDataSource;
  }

  // =========================
  // CONNECTION LIFECYCLE
  // =========================

  public void open() throws Exception
  {
    Connection c;
    if (connection.get() == null) {
      c = dataSource.getConnection();
      connection.set(c);
    }
  }

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

  public void begin() throws Exception
  {
    Connection c;
    c = requireConnection();
    c.setAutoCommit(false);
  }

  public void commit() throws Exception
  {
    Connection c;
    c = requireConnection();
    c.commit();
    c.setAutoCommit(true);
  }

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

  public static java.time.LocalDate toLocalDate(Object sqlDate)
  {
    java.time.LocalDate result;
    result = null;
    if (sqlDate instanceof java.sql.Date) {
      result = ((java.sql.Date) sqlDate).toLocalDate();
    }
    return result;
  }

  public static java.time.LocalTime toLocalTime(Object sqlTime)
  {
    java.time.LocalTime result;
    result = null;
    if (sqlTime instanceof java.sql.Time) {
      result = ((java.sql.Time) sqlTime).toLocalTime();
    }
    return result;
  }

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

  public static java.sql.Date toSqlDate(java.time.LocalDate localDate)
  {
    java.sql.Date result;
    result = null;
    if (localDate != null) {
      result = java.sql.Date.valueOf(localDate);
    }
    return result;
  }

  public static java.sql.Time toSqlTime(java.time.LocalTime localTime)
  {
    java.sql.Time result;
    result = null;
    if (localTime != null) {
      result = java.sql.Time.valueOf(localTime);
    }
    return result;
  }

  public static java.sql.Timestamp toSqlTimestamp(java.time.LocalDateTime localDateTime)
  {
    java.sql.Timestamp result;
    result = null;
    if (localDateTime != null) {
      result = java.sql.Timestamp.valueOf(localDateTime);
    }
    return result;
  }

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

  public static String toString(Object value)
  {
    String result;
    result = null;
    if (value != null) {
      result = value.toString();
    }
    return result;
  }

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
