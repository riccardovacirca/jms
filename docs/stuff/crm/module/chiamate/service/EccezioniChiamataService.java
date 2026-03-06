package dev.crm.module.chiamate.service;

import dev.crm.module.chiamate.dao.ErroreCarrierDao;
import dev.crm.module.chiamate.dao.LogChiamataDao;
import dev.crm.module.chiamate.dto.EccezioniChiamataDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class EccezioniChiamataService
{
  private final ErroreCarrierDao dao;
  private final LogChiamataDao log;
  private final StatoChiamataService statoService;

  public EccezioniChiamataService(
      ErroreCarrierDao dao, LogChiamataDao log, StatoChiamataService statoService)
  {
    this.dao = dao;
    this.log = log;
    this.statoService = statoService;
  }

  public EccezioniChiamataDto registra(EccezioniChiamataDto dto)
  {
    EccezioniChiamataDto saved = dao.save(dto);
    log.log(dto.chiamataId, "ERRORE", "Errore [" + dto.codice + "]: " + dto.descrizione);
    statoService.setStato(dto.chiamataId, "ERRORE");
    return saved;
  }

  public List<EccezioniChiamataDto> all()
  {
    return dao.all();
  }

  public List<EccezioniChiamataDto> byChiamata(Long chiamataId)
  {
    return dao.byChiamata(chiamataId);
  }
}
