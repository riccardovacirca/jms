package dev.crm.module.cloud.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class TokenGenerator
{
  private static final Logger log = LoggerFactory.getLogger(TokenGenerator.class);
  private static final String HMAC_ALGORITHM = "HmacSHA256";
  private static final long TOKEN_VALIDITY_MS = 3600000; // 1 ora

  public String generateInstallationToken(
    String installationId,
    String conversationUuid,
    String sharedSecret
  ) throws Exception
  {
    String payload;
    String signature;
    String token;
    long timestamp;

    timestamp = System.currentTimeMillis();

    payload = installationId + ":" + conversationUuid + ":" + timestamp;

    signature = generateHmacSignature(payload, sharedSecret);

    token = Base64.getEncoder().encodeToString(
      (payload + ":" + signature).getBytes(StandardCharsets.UTF_8)
    );

    return token;
  }

  public boolean validateInstallationToken(
    String token,
    String expectedInstallationId,
    String sharedSecret
  )
  {
    String decoded;
    String[] parts;
    String installationId;
    String conversationUuid;
    String timestampStr;
    String signature;
    String expectedSignature;
    String payload;
    long timestamp;
    long now;
    long age;

    try {
      decoded = new String(
        Base64.getDecoder().decode(token),
        StandardCharsets.UTF_8
      );

      parts = decoded.split(":");

      if (parts.length != 4) {
        log.warn("[TOKEN_PARSE] result=FAIL, reason=invalid_format, expected_parts=4, actual_parts={}", parts.length);
        return false;
      }

      installationId = parts[0];
      conversationUuid = parts[1];
      timestampStr = parts[2];
      signature = parts[3];

      // Valida installation_id
      if (!installationId.equals(expectedInstallationId)) {
        log.warn("[TOKEN_PARSE] result=FAIL, reason=installation_mismatch");
        return false;
      }

      // Valida timestamp (max 1 ora)
      timestamp = Long.parseLong(timestampStr);
      now = System.currentTimeMillis();
      age = now - timestamp;

      if (age > TOKEN_VALIDITY_MS) {
        log.warn("[TOKEN_PARSE] result=FAIL, reason=expired, age_ms={}, max_ms={}", age, TOKEN_VALIDITY_MS);
        return false;
      }

      // Valida firma HMAC
      payload = installationId + ":" + conversationUuid + ":" + timestampStr;
      expectedSignature = generateHmacSignature(payload, sharedSecret);

      if (!signature.equals(expectedSignature)) {
        log.warn("[TOKEN_PARSE] result=FAIL, reason=signature_mismatch");
        return false;
      }

      return true;
    }
    catch (Exception e) {
      log.error("[TOKEN_PARSE] result=FAIL, reason=exception, error={}", e.getMessage());
      return false;
    }
  }

  private String generateHmacSignature(String data, String secret) throws Exception
  {
    Mac hmac;
    SecretKeySpec secretKey;
    byte[] signatureBytes;

    hmac = Mac.getInstance(HMAC_ALGORITHM);

    secretKey = new SecretKeySpec(
      secret.getBytes(StandardCharsets.UTF_8),
      HMAC_ALGORITHM
    );

    hmac.init(secretKey);

    signatureBytes = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));

    return Base64.getEncoder().encodeToString(signatureBytes);
  }
}
