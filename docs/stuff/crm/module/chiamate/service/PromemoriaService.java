package dev.crm.module.chiamate.service;

import dev.crm.module.chiamate.dao.PromemoriaDao;
import dev.crm.module.chiamate.dto.PromemoriaDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PromemoriaService
{

  private final PromemoriaDao dao;

  public PromemoriaService(PromemoriaDao dao)
  {
    this.dao = dao;
  }

  public PromemoriaDto create(PromemoriaDto dto) throws Exception
  {
    long id = dao.insert(dto);
    dto.id = id;
    return dto;
  }

  public void markInviato(Long id) throws Exception
  {
    dao.markInviato(id);
  }

  public List<PromemoriaDto> findByRichiamoId(Long richiamoId) throws Exception
  {
    return dao.findByRichiamoId(richiamoId);
  }

  public List<PromemoriaDto> findDaInviare() throws Exception
  {
    return dao.findDaInviare();
  }

  public void delete(Long id) throws Exception
  {
    dao.delete(id);
  }
}
