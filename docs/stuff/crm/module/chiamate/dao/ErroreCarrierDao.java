package dev.crm.module.chiamate.dao;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.ArrayList;
import java.util.HashMap;
import dev.crm.module.chiamate.dto.EccezioniChiamataDto;
import java.util.ArrayList;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class ErroreCarrierDao
{
  private final List<EccezioniChiamataDto> db = Collections.synchronizedList(new ArrayList<>());
  private final Map<Long, List<EccezioniChiamataDto>> byChiamata = new ConcurrentHashMap<>();
  private final AtomicLong nextId = new AtomicLong(1);

  public EccezioniChiamataDto save(EccezioniChiamataDto dto)
  {
    if (dto.id == null) {
      dto.id = nextId.getAndIncrement();
    }
    if (dto.createdAt == null) {
      dto.createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
    db.add(dto);
    byChiamata
        .computeIfAbsent(dto.chiamataId, k -> Collections.synchronizedList(new ArrayList<>()))
        .add(dto);
    return dto;
  }

  public List<EccezioniChiamataDto> all()
  {
    return new ArrayList<>(db);
  }

  public List<EccezioniChiamataDto> byChiamata(Long chiamataId)
  {
    return new ArrayList<>(byChiamata.getOrDefault(chiamataId, Collections.emptyList()));
  }
}
