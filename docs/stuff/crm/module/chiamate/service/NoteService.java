package dev.crm.module.chiamate.service;

import dev.crm.module.chiamate.dao.LogChiamataDao;
import dev.crm.module.chiamate.dao.NoteChiamataDao;
import dev.crm.module.chiamate.dto.NoteChiamataDto;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class NoteService
{
  private final NoteChiamataDao dao;
  private final LogChiamataDao log;

  public NoteService(NoteChiamataDao dao, LogChiamataDao log)
  {
    this.dao = dao;
    this.log = log;
  }

  public NoteChiamataDto salva(NoteChiamataDto dto)
  {
    NoteChiamataDto saved = dao.save(dto);
    log.log(dto.chiamataId, "NOTA", "Nota aggiunta: " + truncate(dto.nota, 50));
    return saved;
  }

  public List<NoteChiamataDto> getNote(Long chiamataId)
  {
    return dao.byChiamata(chiamataId);
  }

  private String truncate(String s, int len)
  {
    if (s == null)
      return "";
    return s.length() > len ? s.substring(0, len) + "..." : s;
  }
}
