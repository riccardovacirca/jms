package dev.crm.module.cloud.service;

import dev.crm.module.cloud.dao.InstallationMetadataDao;
import dev.crm.module.cloud.dto.InstallationMetadataDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

public class InstallationService
{
  private static final Logger log = LoggerFactory.getLogger(InstallationService.class);

  private final InstallationMetadataDao dao;
  private final TokenGenerator tokenGenerator;

  private InstallationMetadataDto cachedMetadata;

  public InstallationService(
    InstallationMetadataDao dao,
    TokenGenerator tokenGenerator
  )
  {
    this.dao = dao;
    this.tokenGenerator = tokenGenerator;
  }

  public InstallationMetadataDto getOrCreateInstallation() throws Exception
  {
    Optional<InstallationMetadataDto> existing;
    InstallationMetadataDto metadata;
    String installationId;
    String sharedSecret;

    log.debug("[INSTALLATION] [GET_OR_CREATE] operation=start");

    // Cache per evitare query ripetute
    if (cachedMetadata != null) {
      log.debug("[INSTALLATION] [GET_OR_CREATE] source=cache, installation_id={}", cachedMetadata.installationId);
      return cachedMetadata;
    }

    log.debug("[INSTALLATION] [GET_OR_CREATE] source=database, query=default");

    // Cerca installazione "default"
    existing = dao.findByInstallationId("default");

    if (existing.isPresent()) {
      metadata = existing.get();

      log.info("[INSTALLATION] [GET_OR_CREATE] result=found, installation_id={}", metadata.installationId);

      // Se ha ancora il secret di default, rigeneralo
      if ("changeme_on_first_run".equals(metadata.sharedSecret)) {
        log.warn("[INSTALLATION] [SECRET_ROTATION] status=rotating, installation_id={}, reason=default_secret_detected", metadata.installationId);

        sharedSecret = generateSecureSecret();

        metadata.sharedSecret = sharedSecret;

        dao.update(metadata);

        log.info("[INSTALLATION] [SECRET_ROTATION] status=completed, installation_id={}, secret_length={}", metadata.installationId, sharedSecret.length());
      }

      cachedMetadata = metadata;

      return metadata;
    }

    // Crea nuova installazione
    log.warn("[INSTALLATION] [GET_OR_CREATE] result=not_found, action=creating_new");

    installationId = "inst_" + UUID.randomUUID().toString();
    sharedSecret = generateSecureSecret();

    metadata = new InstallationMetadataDto();
    metadata.installationId = installationId;
    metadata.installationName = "CRM Installation";
    metadata.sharedSecret = sharedSecret;
    metadata.isActive = true;

    metadata = dao.insert(metadata);

    log.info("[INSTALLATION] [GET_OR_CREATE] result=created, installation_id={}, secret_length={}", installationId, sharedSecret.length());

    cachedMetadata = metadata;

    return metadata;
  }

  public String generateEventUrlToken(String conversationUuid) throws Exception
  {
    InstallationMetadataDto metadata;
    String token;

    metadata = getOrCreateInstallation();

    token = tokenGenerator.generateInstallationToken(
      metadata.installationId,
      conversationUuid,
      metadata.sharedSecret
    );

    log.info("[TOKEN_GEN] installation_id={}, conversation_uuid={}", metadata.installationId, conversationUuid);

    return token;
  }

  public boolean validateEventUrlToken(
    String token,
    String installationId
  ) throws Exception
  {
    InstallationMetadataDto metadata;
    boolean isValid;

    metadata = getOrCreateInstallation();

    // Verifica che l'installation_id corrisponda
    if (!metadata.installationId.equals(installationId)) {
      log.warn("[TOKEN_VALIDATE] result=FAIL, reason=installation_mismatch, expected={}, received={}", metadata.installationId, installationId);
      return false;
    }

    isValid = tokenGenerator.validateInstallationToken(
      token,
      installationId,
      metadata.sharedSecret
    );

    log.info("[TOKEN_VALIDATE] result={}, installation_id={}", isValid ? "SUCCESS" : "FAIL", installationId);

    return isValid;
  }

  private String generateSecureSecret()
  {
    SecureRandom random;
    byte[] bytes;
    String secret;

    random = new SecureRandom();
    bytes = new byte[32];

    random.nextBytes(bytes);

    secret = Base64.getEncoder().encodeToString(bytes);

    return secret;
  }

  public void clearCache()
  {
    cachedMetadata = null;
  }
}
