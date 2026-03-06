package dev.crm.module.chiamate.service;

import dev.crm.module.chiamate.dao.ChiamataDao;
import dev.crm.module.chiamate.dao.LogChiamataDao;
import dev.crm.module.chiamate.dto.AvvioChiamataDto;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class AvvioChiamataService
{
  private final ChiamataDao dao;
  private final LogChiamataDao log;
  private final StatoChiamataService statoService;

  public AvvioChiamataService(
      ChiamataDao dao, LogChiamataDao log, StatoChiamataService statoService)
  {
    this.dao = dao;
    this.log = log;
    this.statoService = statoService;
  }

  public AvvioChiamataDto avvia(AvvioChiamataDto dto)
  {
    AvvioChiamataDto c = dao.save(dto);
    log.log(c.id, "AVVIO", "Avvio chiamata verso " + c.numero);
    statoService.setStato(c.id, "AVVIATA");
    return c;
  }

  public Optional<AvvioChiamataDto> find(Long id)
  {
    return dao.find(id);
  }

  public List<AvvioChiamataDto> findAll()
  {
    return dao.findAll();
  }

  public List<AvvioChiamataDto> findByOperatore(Long operatoreId)
  {
    return dao.findByOperatore(operatoreId);
  }

  public void termina(Long id)
  {
    statoService.setStato(id, "TERMINATA");
    log.log(id, "TERMINA", "Chiamata terminata");
  }
}
