package dev.crm.module.chiamate.dao;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.ArrayList;
import java.util.HashMap;
import dev.crm.module.chiamate.dto.MetadatiDto;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class MetadatiDao
{
  private final Map<Long, MetadatiDto> byChiamata = new ConcurrentHashMap<>();

  public MetadatiDto save(MetadatiDto dto)
  {
    if (dto.metadati == null) {
      dto.metadati = new HashMap<>();
    }
    MetadatiDto existing = byChiamata.get(dto.chiamataId);
    if (existing != null) {
      existing.metadati.putAll(dto.metadati);
      return existing;
    }
    byChiamata.put(dto.chiamataId, dto);
    return dto;
  }

  public Optional<MetadatiDto> find(Long chiamataId)
  {
    return Optional.ofNullable(byChiamata.get(chiamataId));
  }
}
