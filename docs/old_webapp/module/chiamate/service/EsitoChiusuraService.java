package dev.crm.module.chiamate.service;

import dev.crm.module.chiamate.dao.ConfigurazioneEsitiDao;
import dev.crm.module.chiamate.dao.EsitoDao;
import dev.crm.module.chiamate.dto.ConfigurazioneEsitiDto;
import dev.crm.module.chiamate.dto.EsitoDto;
import dev.crm.module.chiamate.dto.StatoContattoDto;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class EsitoChiusuraService
{
  private final EsitoDao dao;
  private final ConfigurazioneEsitiDao configDao;
  private final StatoContattoService statoContattoService;

  public EsitoChiusuraService(
      EsitoDao dao, ConfigurazioneEsitiDao configDao, StatoContattoService statoContattoService)
  {
    this.dao = dao;
    this.configDao = configDao;
    this.statoContattoService = statoContattoService;
  }

  public EsitoDto chiudi(EsitoDto dto)
  {
    // Valida configurazione esito
    Optional<ConfigurazioneEsitiDto> configOpt = configDao.find(dto.codiceEsito);
    if (configOpt.isPresent()) {
      ConfigurazioneEsitiDto config = configOpt.get();

      // Imposta tipo esito dalla configurazione
      if (dto.tipoEsito == null) {
        dto.tipoEsito = config.tipoEsito;
      }

      // Valida durata minima
      if (config.durataMinima != null && config.durataMinima > 0) {
        if (dto.durata == null || dto.durata < config.durataMinima) {
          throw new IllegalArgumentException(
              "Durata chiamata insufficiente. Minimo richiesto: "
                  + config.durataMinima
                  + " secondi");
        }
      }
    }

    // Salva esito
    EsitoDto saved = dao.save(dto);

    // Aggiorna stato contatto
    if (dto.contattoId != null) {
      String nuovoStato = mappaStatoContatto(dto.tipoEsito);
      StatoContattoDto statoDto = new StatoContattoDto();
      statoDto.contattoId = dto.contattoId;
      statoDto.stato = nuovoStato;
      statoDto.ultimoEsito = dto.codiceEsito;
      statoContattoService.aggiorna(statoDto);
    }

    return saved;
  }

  private String mappaStatoContatto(String tipoEsito)
  {
    if (tipoEsito == null)
      return "LAVORATO";
    switch (tipoEsito) {
      case "POSITIVO" :
        return "POSITIVO";
      case "NEGATIVO" :
        return "NEGATIVO";
      case "RICHIAMO" :
        return "RICHIAMO";
      default :
        return "LAVORATO";
    }
  }

  public Optional<EsitoDto> find(Long id)
  {
    return dao.find(id);
  }

  public List<EsitoDto> findAll()
  {
    return dao.findAll();
  }

  public List<EsitoDto> findByChiamata(Long chiamataId)
  {
    return dao.findByChiamata(chiamataId);
  }

  public List<EsitoDto> findByContatto(Long contattoId)
  {
    return dao.findByContatto(contattoId);
  }
}
