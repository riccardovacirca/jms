package dev.crm.module.auth.dao;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.ArrayList;
import java.util.HashMap;
import dev.crm.module.auth.dto.UtenteDto;
import java.util.ArrayList;
import java.util.HashMap;
import dev.springtools.util.DB;


import java.util.Optional;
import javax.sql.DataSource;

public class UtenteDao
{
  private final DataSource dataSource;

  public UtenteDao(DataSource dataSource)
  {
    this.dataSource = dataSource;
  }

  public Optional<UtenteDto> findByUsername(String username) throws Exception
  {
    DB db;
    ArrayList<HashMap<String, Object>> rs;

    db = new DB(dataSource);
    try {
      db.acquire();
      rs = db.select(
        "SELECT * FROM utenti WHERE username = ? AND attivo = 1",
        username
      );
      if (rs.size() == 0) return Optional.empty();
      return Optional.of(mapToDto(rs.get(0)));
    } finally {
      db.release();
    }
  }

  public Optional<UtenteDto> findById(Long id) throws Exception
  {
    DB db;
    ArrayList<HashMap<String, Object>> rs;

    db = new DB(dataSource);
    try {
      db.acquire();
      rs = db.select("SELECT * FROM utenti WHERE id = ?", id);
      if (rs.size() == 0) return Optional.empty();
      return Optional.of(mapToDto(rs.get(0)));
    } finally {
      db.release();
    }
  }

  public UtenteDto insert(UtenteDto dto) throws Exception
  {
    DB db;
    long id;
    ArrayList<HashMap<String, Object>> rs;

    db = new DB(dataSource);
    try {
      db.acquire();
      db.query(
        "INSERT INTO utenti (username, password_hash, email, ruolo, attivo, nome, cognome, telefono) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
        dto.username, dto.passwordHash, dto.email, dto.ruolo,
        dto.attivo, dto.nome, dto.cognome, dto.telefono
      );
      id = db.lastInsertId();

      // Fetch inserted record using same connection
      rs = db.select("SELECT * FROM utenti WHERE id = ?", id);
      if (rs.size() == 0) {
        throw new IllegalStateException("Failed to retrieve inserted utente with id: " + id);
      }
      return mapToDto(rs.get(0));
    } finally {
      db.release();
    }
  }

  private UtenteDto mapToDto(HashMap<String, Object> r)
  {
    UtenteDto dto;

    dto = new UtenteDto();
    dto.id = DB.toLong(r.get("id"));
    dto.username = DB.toString(r.get("username"));
    dto.passwordHash = DB.toString(r.get("password_hash"));
    dto.email = DB.toString(r.get("email"));
    dto.ruolo = DB.toString(r.get("ruolo"));
    dto.attivo = DB.toBoolean(r.get("attivo"));
    dto.nome = DB.toString(r.get("nome"));
    dto.cognome = DB.toString(r.get("cognome"));
    dto.telefono = DB.toString(r.get("telefono"));
    dto.createdAt = DB.toLocalDateTime(r.get("created_at"));
    dto.updatedAt = DB.toLocalDateTime(r.get("updated_at"));
    return dto;
  }
}
