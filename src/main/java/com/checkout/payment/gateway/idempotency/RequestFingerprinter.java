package com.checkout.payment.gateway.idempotency;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RequestFingerprinter {

  public String fingerprint(PostPaymentRequest request) {
    String canonical = String.join("|",
        nullSafe(request.getCardNumber()),
        Integer.toString(request.getExpiryMonth()),
        Integer.toString(request.getExpiryYear()),
        nullSafe(request.getCurrency()),
        Integer.toString(request.getAmount()),
        nullSafe(request.getCvv()));
    return sha256(canonical);
  }

  private String sha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("SHA-256 not available", ex);
    }
  }

  private String nullSafe(String value) {
    return value == null ? "" : value;
  }
}

