package {{APP_PACKAGE}}.contatti.dao;

import {{APP_PACKAGE}}.contatti.dto.ContattoDTO;
import dev.jms.util.DB;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ContattoDAO
{
  private final DB db;

  public ContattoDAO(DB db)
  {
    this.db = db;
  }

  /** Restituisce la lista paginata. Se listaId è specificato, filtra per quella lista. */
  public List<ContattoDTO> findAll(int page, int size, Integer listaId) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    if (listaId != null) {
      sql =
        "SELECT c.*, COUNT(lc.lista_id) AS liste_count " +
        "FROM contatti c " +
        "LEFT JOIN lista_contatti lc ON lc.contatto_id = c.id " +
        "WHERE c.id IN (SELECT contatto_id FROM lista_contatti WHERE lista_id = ?) " +
        "GROUP BY c.id " +
        "ORDER BY c.cognome, c.nome " +
        "LIMIT ? OFFSET ?";
      rows = db.select(sql, listaId, size, (page - 1) * size);
    } else {
      sql =
        "SELECT c.*, COUNT(lc.lista_id) AS liste_count " +
        "FROM contatti c " +
        "LEFT JOIN lista_contatti lc ON lc.contatto_id = c.id " +
        "GROUP BY c.id " +
        "ORDER BY c.cognome, c.nome " +
        "LIMIT ? OFFSET ?";
      rows = db.select(sql, size, (page - 1) * size);
    }
    return rows.stream().map(this::toDTO).toList();
  }

  /** Conta il totale per la paginazione. */
  public int count(Integer listaId) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    if (listaId != null) {
      sql = "SELECT COUNT(*) AS n FROM contatti WHERE id IN (SELECT contatto_id FROM lista_contatti WHERE lista_id = ?)";
      rows = db.select(sql, listaId);
    } else {
      sql = "SELECT COUNT(*) AS n FROM contatti";
      rows = db.select(sql);
    }
    return DB.toInteger(rows.get(0).get("n"));
  }

  /** Cerca per id. Restituisce null se non trovato. */
  public ContattoDTO findById(int id) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql =
      "SELECT c.*, COUNT(lc.lista_id) AS liste_count " +
      "FROM contatti c " +
      "LEFT JOIN lista_contatti lc ON lc.contatto_id = c.id " +
      "WHERE c.id = ? " +
      "GROUP BY c.id";
    rows = db.select(sql, id);
    return rows.isEmpty() ? null : toDTO(rows.get(0));
  }

  /** Ricerca full-text paginata su nome, cognome, ragione sociale, telefono, email. */
  public List<ContattoDTO> search(String query, int page, int size) throws Exception
  {
    String sql;
    String pattern;
    ArrayList<HashMap<String, Object>> rows;

    pattern = "%" + query.toLowerCase() + "%";
    sql =
      "SELECT c.*, COUNT(lc.lista_id) AS liste_count " +
      "FROM contatti c " +
      "LEFT JOIN lista_contatti lc ON lc.contatto_id = c.id " +
      "WHERE LOWER(c.nome) LIKE ? OR LOWER(c.cognome) LIKE ? " +
      "   OR LOWER(c.ragione_sociale) LIKE ? OR LOWER(c.telefono) LIKE ? OR LOWER(c.email) LIKE ? " +
      "GROUP BY c.id " +
      "ORDER BY c.cognome, c.nome " +
      "LIMIT ? OFFSET ?";
    rows = db.select(sql, pattern, pattern, pattern, pattern, pattern, size, (page - 1) * size);
    return rows.stream().map(this::toDTO).toList();
  }

  /** Conta il totale per la paginazione della ricerca. */
  public int countSearch(String query) throws Exception
  {
    String sql;
    String pattern;
    ArrayList<HashMap<String, Object>> rows;

    pattern = "%" + query.toLowerCase() + "%";
    sql =
      "SELECT COUNT(DISTINCT c.id) AS n FROM contatti c " +
      "WHERE LOWER(c.nome) LIKE ? OR LOWER(c.cognome) LIKE ? " +
      "   OR LOWER(c.ragione_sociale) LIKE ? OR LOWER(c.telefono) LIKE ? OR LOWER(c.email) LIKE ?";
    rows = db.select(sql, pattern, pattern, pattern, pattern, pattern);
    return DB.toInteger(rows.get(0).get("n"));
  }

  /** Inserisce un nuovo contatto. Restituisce l'id generato. */
  public int insert(ContattoDTO c) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    sql =
      "INSERT INTO contatti " +
      "(nome, cognome, ragione_sociale, telefono, email, indirizzo, citta, cap, provincia, note, stato, consenso, blacklist) " +
      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
      "RETURNING id";
    rows = db.select(sql,
      c.nome(), c.cognome(), c.ragioneSociale(), c.telefono(), c.email(),
      c.indirizzo(), c.citta(), c.cap(), c.provincia(), c.note(),
      c.stato(), c.consenso(), c.blacklist()
    );
    return DB.toInteger(rows.get(0).get("id"));
  }

  /** Aggiorna tutti i campi del contatto. Il contatto deve avere id valorizzato. */
  public void update(ContattoDTO c) throws Exception
  {
    String sql;

    sql =
      "UPDATE contatti " +
      "SET nome = ?, cognome = ?, ragione_sociale = ?, telefono = ?, email = ?, " +
      "    indirizzo = ?, citta = ?, cap = ?, provincia = ?, note = ?, " +
      "    stato = ?, consenso = ?, blacklist = ?, updated_at = NOW() " +
      "WHERE id = ?";
    db.query(sql,
      c.nome(), c.cognome(), c.ragioneSociale(), c.telefono(), c.email(),
      c.indirizzo(), c.citta(), c.cap(), c.provincia(), c.note(),
      c.stato(), c.consenso(), c.blacklist(), c.id()
    );
  }

  /** Elimina il contatto. Le righe in lista_contatti vengono eliminate in cascade. */
  public void delete(int id) throws Exception
  {
    db.query("DELETE FROM contatti WHERE id = ?", id);
  }

  /** Aggiorna solo il campo stato. */
  public void updateStato(int id, int stato) throws Exception
  {
    db.query("UPDATE contatti SET stato = ?, updated_at = NOW() WHERE id = ?", stato, id);
  }

  /** Aggiorna solo il flag blacklist. */
  public void setBlacklist(int id, boolean blacklist) throws Exception
  {
    db.query("UPDATE contatti SET blacklist = ?, updated_at = NOW() WHERE id = ?", blacklist, id);
  }

  /** Verifica se esiste già un contatto con il numero di telefono, escludendo opzionalmente un id. */
  public boolean existsByTelefono(String telefono, Integer excludeId) throws Exception
  {
    String sql;
    ArrayList<HashMap<String, Object>> rows;

    if (excludeId != null) {
      sql = "SELECT COUNT(*) AS n FROM contatti WHERE telefono = ? AND id != ?";
      rows = db.select(sql, telefono, excludeId);
    } else {
      sql = "SELECT COUNT(*) AS n FROM contatti WHERE telefono = ?";
      rows = db.select(sql, telefono);
    }
    return DB.toInteger(rows.get(0).get("n")) > 0;
  }

  // -------------------------
  // mapping privato
  // -------------------------

  private ContattoDTO toDTO(HashMap<String, Object> row)
  {
    LocalDateTime createdAt;
    LocalDateTime updatedAt;

    createdAt = DB.toLocalDateTime(row.get("created_at"));
    updatedAt = DB.toLocalDateTime(row.get("updated_at"));

    return new ContattoDTO(
      DB.toInteger(row.get("id")),
      DB.toString(row.get("nome")),
      DB.toString(row.get("cognome")),
      DB.toString(row.get("ragione_sociale")),
      DB.toString(row.get("telefono")),
      DB.toString(row.get("email")),
      DB.toString(row.get("indirizzo")),
      DB.toString(row.get("citta")),
      DB.toString(row.get("cap")),
      DB.toString(row.get("provincia")),
      DB.toString(row.get("note")),
      DB.toInteger(row.get("stato")),
      DB.toBoolean(row.get("consenso")),
      DB.toBoolean(row.get("blacklist")),
      createdAt != null ? createdAt.toString() : null,
      updatedAt != null ? updatedAt.toString() : null,
      DB.toLong(row.get("liste_count"))
    );
  }
}
