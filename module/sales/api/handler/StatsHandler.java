package dev.jms.app.sales.handler;

import dev.jms.util.DB;
import dev.jms.util.HttpRequest;
import dev.jms.util.HttpResponse;
import dev.jms.util.Permission;
import dev.jms.util.Role;
import dev.jms.util.Session;

import java.util.HashMap;
import java.util.List;

/** Handler per le statistiche aggregate del modulo sales. */
public class StatsHandler
{
  /**
   * GET /api/sales/stats — conteggi aggregati di contatti, liste e campagne.
   */
  public void stats(HttpRequest req, HttpResponse res, Session session, DB db) throws Exception
  {
    String sql;
    List<HashMap<String, Object>> rows;
    HashMap<String, Object> out;

    session.require(Role.USER, Permission.READ);

    out = new HashMap<>();

    sql = "SELECT COUNT(*) AS n FROM jms_sales_contatti";
    rows = db.select(sql);
    out.put("contatti", DB.toLong(rows.get(0).get("n")));

    sql = "SELECT COUNT(*) AS n FROM jms_sales_liste";
    rows = db.select(sql);
    out.put("liste", DB.toLong(rows.get(0).get("n")));

    sql = "SELECT COUNT(*) AS n FROM jms_sales_campagne";
    rows = db.select(sql);
    out.put("campagne", DB.toLong(rows.get(0).get("n")));

    res.status(200)
       .contentType("application/json")
       .err(false)
       .log(null)
       .out(out)
       .send();
  }
}
