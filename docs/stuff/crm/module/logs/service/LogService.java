package dev.crm.module.logs.service;

import dev.crm.module.logs.dao.LogDao;
import dev.crm.module.logs.dto.LogCreateRequestDto;
import dev.crm.module.logs.dto.LogDto;
import java.util.List;

public class LogService
{
  private final LogDao logDao;

  public LogService(LogDao logDao)
  {
    this.logDao = logDao;
  }

  public List<LogDto> findAll(int limit, int offset) throws Exception
  {
    return logDao.findAll(limit, offset);
  }

  public List<LogDto> findByModule(String module, int limit, int offset) throws Exception
  {
    return logDao.findByModule(module, limit, offset);
  }

  public List<LogDto> findByLevel(String level, int limit, int offset) throws Exception
  {
    return logDao.findByLevel(level, limit, offset);
  }

  public LogDto findById(Long id) throws Exception
  {
    return logDao.findById(id);
  }

  public LogDto create(LogCreateRequestDto request, Long userId, String sessionId, String ipAddress,
      String userAgent) throws Exception
  {
    LogDto dto;
    Long id;

    dto = new LogDto();
    dto.level = request.level.toUpperCase();
    dto.module = request.module;
    dto.message = request.message;
    dto.data = request.data;
    dto.userId = userId;
    dto.sessionId = sessionId;
    dto.ipAddress = ipAddress;
    dto.userAgent = userAgent;

    id = logDao.insert(dto);
    dto.id = id;

    return dto;
  }

  public boolean delete(Long id) throws Exception
  {
    Integer affected;

    affected = logDao.delete(id);
    return affected > 0;
  }

  public int deleteOlderThan(int days) throws Exception
  {
    return logDao.deleteOlderThan(days);
  }
}
