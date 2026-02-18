/*
 * tools - Java Tools Library
 * Copyright (C) 2018-2025 Riccardo Vacirca
 * Licensed under Exclusive Free Beta License
 * See LICENSE.md for full terms
 */
package dev.jms.util;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Java database abstraction layer.
 * Thin wrapper over plain JDBC with HikariCP as DataSource provider.
 * No annotations, no AOP, no hidden behavior.
 */
public class DB
{

  private final DataSource dataSource;

  /** Connection bound to current thread */
  private final ThreadLocal<Connection> connection = new ThreadLocal<>();

  /** Last generated key bound to current thread */
  private final ThreadLocal<Long> lastGeneratedKey = ThreadLocal.withInitial(() -> -1L);

  public DB(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  // =========================
  // CONNECTION LIFECYCLE
  // =========================

  public void open() throws Exception
  {
    if (connection.get() != null) {
      return;
    }
    Connection c = dataSource.getConnection();
    connection.set(c);
  }

  public void close()
  {
    Connection c = connection.get();
    if (c != null) {
      try { c.close(); } catch (Exception ignored) {}
      connection.remove();
      lastGeneratedKey.remove();
    }
  }

  public boolean connected()
  {
    try {
      Connection c = connection.get();
      return c != null && !c.isClosed();
    } catch (SQLException e) {
      return false;
    }
  }

  private Connection requireConnection() throws Exception
  {
    Connection c = connection.get();
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
    Connection c = requireConnection();
    c.setAutoCommit(false);
  }

  public void commit() throws Exception
  {
    Connection c = requireConnection();
    c.commit();
    c.setAutoCommit(true);
  }

  public void rollback() throws Exception
  {
    Connection c = requireConnection();
    c.rollback();
    c.setAutoCommit(true);
  }

  // =========================
  // WRITE QUERIES
  // =========================

  public int query(String sql, Object... params) throws Exception
  {
    Connection c = requireConnection();

    try (PreparedStatement stmt = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

      bindParameters(stmt, params);
      int rows = stmt.executeUpdate();

      try (ResultSet keys = stmt.getGeneratedKeys()) {
        if (keys.next()) {
          lastGeneratedKey.set(keys.getLong(1));
        } else {
          lastGeneratedKey.set(-1L);
        }
      }

      return rows;
    }
  }

  public long lastInsertId() throws Exception
  {
    long id = lastGeneratedKey.get();
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
    Connection c = requireConnection();
    PreparedStatement stmt = c.prepareStatement(sql);
    bindParameters(stmt, params);
    ResultSet rs = stmt.executeQuery();
    return new Cursor(rs, stmt);
  }

  // =========================
  // METADATA
  // =========================

  public Set<String> getTableColumns(String tableName) throws Exception
  {
    Connection c = requireConnection();
    Set<String> columns = new HashSet<>();

    DatabaseMetaData meta = c.getMetaData();
    ResultSet rs = meta.getColumns(null, null, tableName, null);

    if (!rs.next()) {
      rs.close();
      rs = meta.getColumns(null, null, tableName.toUpperCase(), null);
    } else {
      rs.close();
      rs = meta.getColumns(null, null, tableName, null);
    }

    while (rs.next()) {
      String name = rs.getString("COLUMN_NAME");
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
   * Converts SQL Date to Java LocalDate
   *
   * @param sqlDate
   *          SQL Date object from ResultSet
   * @return LocalDate or null if input is null
   */
  public static java.time.LocalDate toLocalDate(Object sqlDate)
  {
    if (sqlDate == null) {
      return null;
    }
    if (sqlDate instanceof java.sql.Date) {
      return ((java.sql.Date) sqlDate).toLocalDate();
    }
    return null;
  }

  /**
   * Converts SQL Time to Java LocalTime
   *
   * @param sqlTime
   *          SQL Time object from ResultSet
   * @return LocalTime or null if input is null
   */
  public static java.time.LocalTime toLocalTime(Object sqlTime)
  {
    if (sqlTime == null) {
      return null;
    }
    if (sqlTime instanceof java.sql.Time) {
      return ((java.sql.Time) sqlTime).toLocalTime();
    }
    return null;
  }

  /**
   * Converts SQL Timestamp to Java LocalDateTime
   *
   * @param sqlTimestamp
   *          SQL Timestamp object from ResultSet
   * @return LocalDateTime or null if input is null
   */
  public static java.time.LocalDateTime toLocalDateTime(Object sqlTimestamp)
  {
    if (sqlTimestamp == null) {
      return null;
    }
    if (sqlTimestamp instanceof java.sql.Timestamp) {
      return ((java.sql.Timestamp) sqlTimestamp).toLocalDateTime();
    }
    if (sqlTimestamp instanceof String) {
      try {
        return java.time.LocalDateTime.parse((String) sqlTimestamp);
      } catch (Exception e) {
        return null;
      }
    }
    return null;
  }

  /**
   * Converts Java LocalDate to SQL Date
   *
   * @param localDate
   *          Java LocalDate
   * @return SQL Date or null if input is null
   */
  public static java.sql.Date toSqlDate(java.time.LocalDate localDate)
  {
    if (localDate == null) {
      return null;
    }
    return java.sql.Date.valueOf(localDate);
  }

  /**
   * Converts Java LocalTime to SQL Time
   *
   * @param localTime
   *          Java LocalTime
   * @return SQL Time or null if input is null
   */
  public static java.sql.Time toSqlTime(java.time.LocalTime localTime)
  {
    if (localTime == null) {
      return null;
    }
    return java.sql.Time.valueOf(localTime);
  }

  /**
   * Converts Java LocalDateTime to SQL Timestamp
   *
   * @param localDateTime
   *          Java LocalDateTime
   * @return SQL Timestamp or null if input is null
   */
  public static java.sql.Timestamp toSqlTimestamp(java.time.LocalDateTime localDateTime)
  {
    if (localDateTime == null) {
      return null;
    }
    return java.sql.Timestamp.valueOf(localDateTime);
  }

  /**
   * Safely casts Object to Long
   *
   * @param value
   *          Object from ResultSet
   * @return Long or null if input is null or not castable
   */
  public static Long toLong(Object value)
  {
    if (value == null) {
      return null;
    }
    if (value instanceof Long) {
      return (Long) value;
    }
    if (value instanceof Number) {
      return ((Number) value).longValue();
    }
    return null;
  }

  /**
   * Safely casts Object to Integer
   *
   * @param value
   *          Object from ResultSet
   * @return Integer or null if input is null or not castable
   */
  public static Integer toInteger(Object value)
  {
    if (value == null) {
      return null;
    }
    if (value instanceof Integer) {
      return (Integer) value;
    }
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    return null;
  }

  /**
   * Safely casts Object to Boolean
   *
   * @param value
   *          Object from ResultSet
   * @return Boolean or null if input is null or not castable
   */
  public static Boolean toBoolean(Object value)
  {
    if (value == null) {
      return null;
    }
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    if (value instanceof Number) {
      return ((Number) value).intValue() != 0;
    }
    return null;
  }

  /**
   * Safely casts Object to String
   *
   * @param value
   *          Object from ResultSet
   * @return String or null if input is null
   */
  public static String toString(Object value)
  {
    if (value == null) {
      return null;
    }
    return value.toString();
  }

  /**
   * Safely casts Object to BigDecimal
   *
   * @param value
   *          Object from ResultSet
   * @return BigDecimal or null if input is null or not castable
   */
  public static java.math.BigDecimal toBigDecimal(Object value)
  {
    if (value == null) {
      return null;
    }
    if (value instanceof java.math.BigDecimal) {
      return (java.math.BigDecimal) value;
    }
    return null;
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
      } catch (Exception ignored) {
      }
      try {
        stmt.close();
      } catch (Exception ignored) {
      }
    }
  }
}
