package dev.crm.module.init.service;

import dev.crm.module.auth.dao.UtenteDao;
import dev.crm.module.auth.dto.UtenteDto;
import dev.crm.module.init.dao.AziendaDao;
import dev.crm.module.init.dao.InstallationDao;
import dev.crm.module.init.dao.SedeDao;
import dev.crm.module.init.dto.*;
import dev.crm.module.logs.dto.LogCreateRequestDto;
import dev.crm.module.logs.service.LogService;
import org.springframework.security.crypto.password.PasswordEncoder;

public class InitService
{
  private static final String MODULE = "init";

  private final AziendaDao aziendaDao;
  private final SedeDao sedeDao;
  private final UtenteDao utenteDao;
  private final InstallationDao installationDao;
  private final PasswordEncoder passwordEncoder;
  private final LogService logService;

  public InitService(
      AziendaDao aziendaDao,
      SedeDao sedeDao,
      UtenteDao utenteDao,
      InstallationDao installationDao,
      PasswordEncoder passwordEncoder,
      LogService logService)
  {
    this.aziendaDao = aziendaDao;
    this.sedeDao = sedeDao;
    this.utenteDao = utenteDao;
    this.installationDao = installationDao;
    this.passwordEncoder = passwordEncoder;
    this.logService = logService;
  }

  private void log(String message) throws Exception
  {
    LogCreateRequestDto logDto;

    logDto = new LogCreateRequestDto();
    logDto.level = "INFO";
    logDto.module = MODULE;
    logDto.message = message;

    logService.create(logDto, null, null, null, null);
  }

  public WizardStatusDto getStatus() throws Exception
  {
    Boolean wizardCompleted;
    AziendaDto azienda;
    Integer totalSedi;

    wizardCompleted = installationDao.isWizardCompleted();
    azienda = aziendaDao.findFirst();
    totalSedi = sedeDao.count();

    WizardStatusDto status;

    status = new WizardStatusDto(wizardCompleted, null, azienda != null, true, totalSedi, 0);

    return status;
  }

  public void completeWizard(WizardCompleteRequestDto request) throws Exception
  {
    Boolean wizardCompleted;
    Long aziendaId;
    Long sedeId;
    int sediCount;
    int adminCount;
    UtenteDto utente;

    log("Wizard completion started");

    // Verifica che wizard non sia già completato
    wizardCompleted = installationDao.isWizardCompleted();
    if (wizardCompleted) {
      log("ERROR: Wizard already completed - aborting");
      throw new IllegalStateException("Wizard already completed");
    }

    log("Wizard status verified - not completed yet");

    // Valida dati essenziali
    if (request.azienda == null) {
      log("ERROR: Missing azienda data");
      throw new IllegalArgumentException("Dati azienda obbligatori");
    }
    if (request.ownerAccount == null) {
      log("ERROR: Missing owner account data");
      throw new IllegalArgumentException("Account owner obbligatorio");
    }
    if (request.ownerAccount.password == null || request.ownerAccount.password.isEmpty()) {
      log("ERROR: Missing owner password");
      throw new IllegalArgumentException("Password owner obbligatoria");
    }

    log("Input validation completed successfully");

    // Inserisci azienda
    log("Inserting azienda: " + request.azienda.ragioneSociale);
    aziendaId = aziendaDao.insert(request.azienda);
    log("Azienda inserted with ID: " + aziendaId);

    // Inserisci sedi operative
    sediCount = 0;
    if (request.sedi != null && !request.sedi.isEmpty()) {
      log("Inserting " + request.sedi.size() + " sedi operative");
      for (SedeDto sede : request.sedi) {
        sedeId = sedeDao.insert(sede);
        sediCount++;
        log("Sede inserted: " + sede.nome + " (ID: " + sedeId + ")");
      }
      log("All sedi inserted successfully - total: " + sediCount);
    } else {
      log("No sedi to insert");
    }

    // Inserisci owner account nella tabella utenti
    log("Inserting owner account: " + request.ownerAccount.username);
    utente = new UtenteDto();
    utente.username = request.ownerAccount.username;
    utente.passwordHash = passwordEncoder.encode(request.ownerAccount.password);
    utente.email = request.ownerAccount.email;
    utente.ruolo = "ADMIN";
    utente.attivo = true;
    utente.nome = request.ownerAccount.nome;
    utente.cognome = request.ownerAccount.cognome;
    utente.telefono = request.ownerAccount.telefono;
    utente = utenteDao.insert(utente);
    log("Owner account inserted with ID: " + utente.id);

    // Inserisci admin accounts
    adminCount = 0;
    if (request.adminAccounts != null && !request.adminAccounts.isEmpty()) {
      log("Inserting " + request.adminAccounts.size() + " admin accounts");
      for (OwnerAccountDto admin : request.adminAccounts) {
        if (admin.password != null && !admin.password.isEmpty()) {
          utente = new UtenteDto();
          utente.username = admin.username;
          utente.passwordHash = passwordEncoder.encode(admin.password);
          utente.email = admin.email;
          utente.ruolo = "ADMIN";
          utente.attivo = true;
          utente.nome = admin.nome;
          utente.cognome = admin.cognome;
          utente.telefono = admin.telefono;
          utenteDao.insert(utente);
          adminCount++;
          log("Admin account inserted: " + admin.username);
        }
      }
      log("All admin accounts inserted successfully - total: " + adminCount);
    } else {
      log("No admin accounts to insert");
    }

    // Marca wizard come completato
    log("Marking wizard as completed");
    installationDao.markWizardCompleted();
    log("Wizard marked as completed successfully");

    log("Wizard completion finished - Summary: azienda=1, sedi=" + sediCount + ", owner=1, admins="
        + adminCount);
  }
}
