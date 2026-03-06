package dev.crm.module.auth.dao;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.ArrayList;
import java.util.HashMap;
import dev.crm.module.auth.dto.SessionDto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class SessionDao
{
  private final Map<String, SessionDto> sessions = new ConcurrentHashMap<>();
  private final Map<Long, List<SessionDto>> byUser = new ConcurrentHashMap<>();

  public void salva(SessionDto dto)
  {
    sessions.put(dto.token, dto);
    byUser
        .computeIfAbsent(dto.userId, k -> Collections.synchronizedList(new ArrayList<>()))
        .add(dto);
  }

  public Optional<SessionDto> get(String token)
  {
    return Optional.ofNullable(sessions.get(token));
  }

  public void rimuovi(String token)
  {
    SessionDto s = sessions.remove(token);
    if (s != null) {
      List<SessionDto> userSessions = byUser.get(s.userId);
      if (userSessions != null) {
        userSessions.removeIf(sess -> token.equals(sess.token));
      }
    }
  }

  public List<SessionDto> findByUser(Long userId)
  {
    return new ArrayList<>(byUser.getOrDefault(userId, Collections.emptyList()));
  }

  public List<SessionDto> findAllActive()
  {
    List<SessionDto> active = new ArrayList<>();
    for (SessionDto s : sessions.values()) {
      if (s.attiva) {
        active.add(s);
      }
    }
    return active;
  }

  public void invalidaByUser(Long userId)
  {
    List<SessionDto> userSessions = byUser.get(userId);
    if (userSessions != null) {
      for (SessionDto s : userSessions) {
        s.attiva = false;
        sessions.remove(s.token);
      }
      userSessions.clear();
    }
  }
}
