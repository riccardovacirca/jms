package dev.crm.module.chiamate.service;

import dev.crm.module.chiamate.dao.LogChiamataDao;
import dev.crm.module.chiamate.dto.StatoChiamataDto;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;

@Service
public class StatoChiamataService
{
  private final LogChiamataDao log;
  private final Map<Long, StatoChiamataDto> stati = new ConcurrentHashMap<>();

  public StatoChiamataService(LogChiamataDao log)
  {
    this.log = log;
  }

  public void aggiorna(StatoChiamataDto dto)
  {
    setStato(dto.chiamataId, dto.stato);
  }

  public void setStato(Long chiamataId, String stato)
  {
    StatoChiamataDto dto = new StatoChiamataDto();
    dto.chiamataId = chiamataId;
    dto.stato = stato;
    dto.updatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    stati.put(chiamataId, dto);
    log.log(chiamataId, "STATO", "Stato aggiornato a " + stato);
  }

  public Optional<StatoChiamataDto> getStato(Long chiamataId)
  {
    return Optional.ofNullable(stati.get(chiamataId));
  }

  public String getStatoValue(Long chiamataId)
  {
    StatoChiamataDto dto = stati.get(chiamataId);
    return dto != null ? dto.stato : null;
  }
}
