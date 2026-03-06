package dev.crm.module.chiamate.dao;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.ArrayList;
import java.util.HashMap;
import dev.crm.module.chiamate.dto.StatoContattoDto;
import java.util.ArrayList;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class StatoContattoDao
{
  private final Map<Long, StatoContattoDto> stati = new ConcurrentHashMap<>();

  public StatoContattoDto aggiorna(StatoContattoDto dto)
  {
    dto.updatedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    stati.put(dto.contattoId, dto);
    return dto;
  }

  public Optional<StatoContattoDto> find(Long contattoId)
  {
    return Optional.ofNullable(stati.get(contattoId));
  }

  public List<StatoContattoDto> findAll()
  {
    return new ArrayList<>(stati.values());
  }

  public List<StatoContattoDto> findByStato(String stato)
  {
    List<StatoContattoDto> result = new ArrayList<>();
    for (StatoContattoDto s : stati.values()) {
      if (Objects.equals(s.stato, stato)) {
        result.add(s);
      }
    }
    return result;
  }
}
