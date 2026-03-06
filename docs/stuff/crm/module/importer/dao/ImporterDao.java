package dev.crm.module.importer.dao;

import dev.springtools.util.DB;
import dev.springtools.util.excel.RowConsumer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.stereotype.Component;

@Component
public class ImporterDao
{
  private final DB db;
  private final String table = "contatti";

  public ImporterDao(DataSource dataSource)
  {
    this.db = new DB(dataSource);
  }

  public RowConsumer getDbRowConsumer()
  {
    RowConsumer consumer;

    consumer = row -> {
      List<String> columns;
      List<Object> params;
      StringBuilder sql;
      StringBuilder placeholders;
      String sqlQuery;

      try {
        db.acquire();
        columns = new ArrayList<>();
        params = new ArrayList<>();

        // Campi da Excel (già normalizzati)
        if (row.containsKey("nome")) {
          columns.add("nome");
          params.add(row.get("nome"));
        }
        if (row.containsKey("cognome")) {
          columns.add("cognome");
          params.add(row.get("cognome"));
        }
        if (row.containsKey("ragione_sociale")) {
          columns.add("ragione_sociale");
          params.add(row.get("ragione_sociale"));
        }
        if (row.containsKey("telefono")) {
          columns.add("telefono");
          params.add(row.get("telefono"));
        }
        if (row.containsKey("email")) {
          columns.add("email");
          params.add(row.get("email"));
        }
        if (row.containsKey("indirizzo")) {
          columns.add("indirizzo");
          params.add(row.get("indirizzo"));
        }
        if (row.containsKey("citta")) {
          columns.add("citta");
          params.add(row.get("citta"));
        }
        if (row.containsKey("cap")) {
          columns.add("cap");
          params.add(row.get("cap"));
        }
        if (row.containsKey("provincia")) {
          columns.add("provincia");
          params.add(row.get("provincia"));
        }
        if (row.containsKey("note")) {
          columns.add("note");
          params.add(row.get("note"));
        }

        // Campi obbligatori con valori di default
        columns.add("stato");
        params.add(1);

        columns.add("consenso");
        params.add(false);

        columns.add("blacklist");
        params.add(false);

        columns.add("created_at");
        params.add(DB.toSqlTimestamp(LocalDateTime.now()));

        // Costruzione query
        sql = new StringBuilder("INSERT INTO ");
        sql.append(table);
        sql.append(" (");
        placeholders = new StringBuilder();

        for (int i = 0; i < columns.size(); i++) {
          if (i > 0) {
            sql.append(", ");
            placeholders.append(", ");
          }
          sql.append(columns.get(i));
          placeholders.append("?");
        }

        sql.append(") VALUES (");
        sql.append(placeholders);
        sql.append(")");

        sqlQuery = sql.toString();
        db.query(sqlQuery, params.toArray());
      } finally {
        db.release();
      }
    };
    return consumer;
  }
}
