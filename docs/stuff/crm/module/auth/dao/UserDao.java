package dev.crm.module.auth.dao;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.ArrayList;
import java.util.HashMap;
import dev.crm.module.auth.dto.UserDto;
import java.util.ArrayList;
import java.util.HashMap;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class UserDao
{
  private final Map<Long, UserDto> db = new ConcurrentHashMap<>();
  private final Map<String, UserDto> byUsername = new ConcurrentHashMap<>();
  private final AtomicLong nextId = new AtomicLong(1);

  @PostConstruct
  public void init()
  {
    // Utenti predefiniti
    creaUtente("admin", "admin", "ADMIN");
    creaUtente("operatore", "operatore", "OPERATORE");
  }

  private void creaUtente(String username, String password, String ruolo)
  {
    UserDto user = new UserDto();
    user.id = nextId.getAndIncrement();
    user.username = username;
    user.password = password;
    user.ruolo = ruolo;
    user.attivo = true;
    db.put(user.id, user);
    byUsername.put(user.username, user);
  }

  public Optional<UserDto> findByUsername(String username)
  {
    return Optional.ofNullable(byUsername.get(username));
  }

  public Optional<UserDto> find(Long id)
  {
    return Optional.ofNullable(db.get(id));
  }

  public boolean verificaCredenziali(String username, String password)
  {
    UserDto user = byUsername.get(username);
    return user != null && user.attivo && user.password.equals(password);
  }

  public List<UserDto> findAll()
  {
    return new ArrayList<>(db.values());
  }
}
