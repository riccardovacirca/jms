package dev.crm.module.chiamate.service;

import dev.crm.module.chiamate.dao.LogChiamataDao;
import dev.crm.module.chiamate.dao.MetadatiDao;
import dev.crm.module.chiamate.dto.MetadatiDto;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class MetadatiService
{
  private final MetadatiDao dao;
  private final LogChiamataDao log;

  public MetadatiService(MetadatiDao dao, LogChiamataDao log)
  {
    this.dao = dao;
    this.log = log;
  }

  public MetadatiDto salva(MetadatiDto dto)
  {
    MetadatiDto saved = dao.save(dto);
    log.log(dto.chiamataId, "METADATI", "Metadati aggiornati: " + dto.metadati.keySet());
    return saved;
  }

  public Optional<MetadatiDto> getMetadati(Long chiamataId)
  {
    return dao.find(chiamataId);
  }
}
