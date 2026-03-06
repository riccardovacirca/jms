package dev.crm.module.chiamate.dao;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.ArrayList;
import java.util.HashMap;
import dev.crm.module.chiamate.dto.AvvioChiamataDto;
import java.util.ArrayList;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Repository;

@Repository
public class ChiamataDao
{
  private final Map<Long, AvvioChiamataDto> db = new ConcurrentHashMap<>();
  private final AtomicLong nextId = new AtomicLong(1);

  public AvvioChiamataDto save(AvvioChiamataDto dto)
  {
    if (dto.id == null) {
      dto.id = nextId.getAndIncrement();
    }
    if (dto.createdAt == null) {
      dto.createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }
    db.put(dto.id, dto);
    return dto;
  }

  public Optional<AvvioChiamataDto> find(Long id)
  {
    return Optional.ofNullable(db.get(id));
  }

  public List<AvvioChiamataDto> findAll()
  {
    return new ArrayList<>(db.values());
  }

  public List<AvvioChiamataDto> findByOperatore(Long operatoreId)
  {
    List<AvvioChiamataDto> result = new ArrayList<>();
    for (AvvioChiamataDto c : db.values()) {
      if (Objects.equals(c.operatoreId, operatoreId)) {
        result.add(c);
      }
    }
    return result;
  }

  public void delete(Long id)
  {
    db.remove(id);
  }

  public int count()
  {
    return db.size();
  }
}
