package dev.crm.module.chiamate.dao;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.ArrayList;
import java.util.HashMap;
import dev.crm.module.chiamate.dto.EsitoDto;
import java.util.ArrayList;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class EsitoDao
{
  private final Map<Long, EsitoDto> db = new ConcurrentHashMap<>();
  private final Map<Long, List<EsitoDto>> byChiamata = new ConcurrentHashMap<>();
  private final Map<Long, List<EsitoDto>> byContatto = new ConcurrentHashMap<>();
  private final AtomicLong nextId = new AtomicLong(1);

  public EsitoDto save(EsitoDto dto)
  {
    if (dto.id == null) {
      dto.id = nextId.getAndIncrement();
    }
    if (dto.createdAt == null) {
      dto.createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
    db.put(dto.id, dto);

    if (dto.chiamataId != null) {
      byChiamata
          .computeIfAbsent(dto.chiamataId, k -> Collections.synchronizedList(new ArrayList<>()))
          .add(dto);
    }
    if (dto.contattoId != null) {
      byContatto
          .computeIfAbsent(dto.contattoId, k -> Collections.synchronizedList(new ArrayList<>()))
          .add(dto);
    }

    return dto;
  }

  public Optional<EsitoDto> find(Long id)
  {
    return Optional.ofNullable(db.get(id));
  }

  public List<EsitoDto> findAll()
  {
    return new ArrayList<>(db.values());
  }

  public List<EsitoDto> findByChiamata(Long chiamataId)
  {
    return new ArrayList<>(byChiamata.getOrDefault(chiamataId, Collections.emptyList()));
  }

  public List<EsitoDto> findByContatto(Long contattoId)
  {
    return new ArrayList<>(byContatto.getOrDefault(contattoId, Collections.emptyList()));
  }
}
