package dev.crm.module.chiamate.service;

import dev.crm.module.chiamate.dao.StoricoRichiamoDao;
import dev.crm.module.chiamate.dto.StoricoRichiamoDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StoricoService
{

  private final StoricoRichiamoDao dao;

  public StoricoService(StoricoRichiamoDao dao)
  {
    this.dao = dao;
  }

  public void log(Long richiamoId, Long operatoreId, String azione, String dettagli)
      throws Exception
  {
    dao.insert(richiamoId, operatoreId, azione, dettagli);
  }

  public List<StoricoRichiamoDto> findByRichiamoId(Long richiamoId) throws Exception
  {
    return dao.findByRichiamoId(richiamoId);
  }

  public List<StoricoRichiamoDto> findByOperatoreId(Long operatoreId, Integer limit)
      throws Exception
  {
    return dao.findByOperatoreId(operatoreId, limit);
  }
}
