package dev.jms.app.user.helper;

import dev.jms.util.Auth;
import dev.jms.util.Config;
import dev.jms.util.DB;
import dev.jms.util.Log;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * Gestisce la creazione automatica dell'account root configurato in application.properties.
 * <p>
 * Se i parametri root.password e root.email sono configurati,
 * crea un account con username "root" e ruolo admin alla prima esecuzione (solo se non esiste già).
 * </p>
 */
public final class RootAccountSetup
{
  private static final String ROOT_USERNAME = "root";

  private RootAccountSetup()
  {
  }

  /**
   * Crea l'account root se configurato e non già esistente.
   * <p>
   * Controlla i parametri di configurazione root.password e root.email.
   * Se entrambi sono valorizzati, verifica che non esista già un account con username "root".
   * Se non esiste, crea l'account con ruolo 'admin' e must_change_password = false.
   * </p>
   * <p>
   * L'account root ha username fisso "root" e tutti i privilegi
   * (ruolo admin con permessi: can_admin, can_write, can_delete, can_send_mail).
   * </p>
   *
   * @param config configurazione applicazione
   */
  public static void createIfConfigured(Config config)
  {
    String password;
    String email;
    Connection conn;
    PreparedStatement ps;
    ResultSet rs;
    String passwordHash;

    password = config.get("root.password", "");
    email = config.get("root.email", "");

    // Se uno qualsiasi dei parametri è vuoto, salta la creazione
    if (password.isBlank() || email.isBlank()) {
      return;
    }

    conn = null;
    ps = null;
    rs = null;

    try {
      conn = DB.getConnection();

      // Verifica se esiste già l'account root
      ps = conn.prepareStatement("SELECT id FROM accounts WHERE username = ?");
      ps.setString(1, ROOT_USERNAME);
      rs = ps.executeQuery();

      if (rs.next()) {
        Log.info("[user] Account root già esistente");
        return;
      }

      rs.close();
      ps.close();

      // Crea l'account root con tutti i privilegi
      passwordHash = Auth.hash(password);

      ps = conn.prepareStatement(
        "INSERT INTO accounts (username, email, password_hash, ruolo, must_change_password) " +
        "VALUES (?, ?, ?, 'admin', false)"
      );
      ps.setString(1, ROOT_USERNAME);
      ps.setString(2, email);
      ps.setString(3, passwordHash);

      int rows;
      rows = ps.executeUpdate();

      if (rows > 0) {
        Log.info("[user] Account root creato con successo (username: root, ruolo: admin, privilegi: tutti)");
      }
    } catch (Exception e) {
      Log.error("[user] Errore nella creazione account root: " + e.getMessage(), e);
    } finally {
      try {
        if (rs != null) rs.close();
        if (ps != null) ps.close();
        if (conn != null) conn.close();
      } catch (Exception e) {
        // Ignora errori di chiusura
      }
    }
  }
}
