package dev.crm.module.auth.service;

import dev.crm.module.auth.dao.SessionDao;
import dev.crm.module.auth.dto.SessionDto;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class SessionService
{
  private final SessionDao dao;

  public SessionService(SessionDao dao)
  {
    this.dao = dao;
  }

  public Optional<SessionDto> getSession(String token)
  {
    return dao.get(token);
  }

  public boolean isValid(String token)
  {
    return dao.get(token).map(s -> s.attiva).orElse(false);
  }

  public List<SessionDto> getActiveSessions()
  {
    return dao.findAllActive();
  }

  public void invalidateUser(Long userId)
  {
    dao.invalidaByUser(userId);
  }
}
