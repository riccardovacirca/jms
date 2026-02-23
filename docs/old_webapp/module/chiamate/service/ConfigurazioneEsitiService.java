package dev.crm.module.chiamate.service;

import dev.crm.module.chiamate.dao.ConfigurazioneEsitiDao;
import dev.crm.module.chiamate.dto.ConfigurazioneEsitiDto;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class ConfigurazioneEsitiService
{
  private final ConfigurazioneEsitiDao dao;

  public ConfigurazioneEsitiService(ConfigurazioneEsitiDao dao)
  {
    this.dao = dao;
  }

  public ConfigurazioneEsitiDto salva(ConfigurazioneEsitiDto dto)
  {
    return dao.salva(dto);
  }

  public Optional<ConfigurazioneEsitiDto> get(String codice)
  {
    return dao.find(codice);
  }

  public List<ConfigurazioneEsitiDto> findAll()
  {
    return dao.findAll();
  }

  public List<ConfigurazioneEsitiDto> findAttivi()
  {
    return dao.findAttivi();
  }

  public void delete(String codice)
  {
    dao.delete(codice);
  }
}
