package dev.crm.module.chiamate.service;

import dev.crm.module.chiamate.dao.StatoContattoDao;
import dev.crm.module.chiamate.dto.StatoContattoDto;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class StatoContattoService
{
  private final StatoContattoDao dao;

  public StatoContattoService(StatoContattoDao dao)
  {
    this.dao = dao;
  }

  public StatoContattoDto aggiorna(StatoContattoDto dto)
  {
    return dao.aggiorna(dto);
  }

  public Optional<StatoContattoDto> find(Long contattoId)
  {
    return dao.find(contattoId);
  }

  public List<StatoContattoDto> findAll()
  {
    return dao.findAll();
  }

  public List<StatoContattoDto> findByStato(String stato)
  {
    return dao.findByStato(stato);
  }
}
