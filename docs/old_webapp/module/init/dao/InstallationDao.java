package dev.crm.module.init.dao;

import dev.springtools.util.DB;
import java.util.ArrayList;
import java.util.HashMap;
import javax.sql.DataSource;

public class InstallationDao
{
  private final DataSource dataSource;

  public InstallationDao(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  public Boolean isWizardCompleted() throws Exception
  {
    DB db;
    ArrayList<HashMap<String, Object>> rs;
    Integer completed;

    db = new DB(dataSource);
    try {
      db.acquire();
      rs = db.select("SELECT wizard_completed FROM installation_metadata LIMIT 1");
      if (rs.size() == 0) {
        return false;
      }
      completed = (Integer) rs.get(0).get("wizard_completed");
      return completed != null && completed == 1;
    } finally {
      db.release();
    }
  }

  public void markWizardCompleted() throws Exception
  {
    DB db;

    db = new DB(dataSource);
    try {
      db.acquire();
      db.begin();

      db.query(
          "UPDATE installation_metadata SET wizard_completed = 1, "
              + "wizard_completed_at = CURRENT_TIMESTAMP WHERE id = 1");

      db.commit();
    } catch (Exception e) {
      db.rollback();
      throw e;
    } finally {
      db.release();
    }
  }
}
