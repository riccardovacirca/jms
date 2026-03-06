package dev.crm.module.chiamate.dao;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.ArrayList;
import java.util.HashMap;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

@Repository
public class LogChiamataDao
{

  public static class LogEntry
  {
    public String timestamp;
    public Long chiamataId;
    public String evento;
    public String dettaglio;
  }

  private final List<LogEntry> log = Collections.synchronizedList(new ArrayList<>());
  private final Map<Long, List<LogEntry>> byChiamata = new ConcurrentHashMap<>();

  public void log(Long chiamataId, String evento, String dettaglio)
  {
    LogEntry entry = new LogEntry();
    entry.timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    entry.chiamataId = chiamataId;
    entry.evento = evento;
    entry.dettaglio = dettaglio;

    log.add(entry);

    byChiamata
        .computeIfAbsent(chiamataId, k -> Collections.synchronizedList(new ArrayList<>()))
        .add(entry);
  }

  public void log(String evento)
  {
    log(null, evento, null);
  }

  public List<LogEntry> all()
  {
    return new ArrayList<>(log);
  }

  public List<LogEntry> byChiamata(Long chiamataId)
  {
    return new ArrayList<>(byChiamata.getOrDefault(chiamataId, Collections.emptyList()));
  }
}
