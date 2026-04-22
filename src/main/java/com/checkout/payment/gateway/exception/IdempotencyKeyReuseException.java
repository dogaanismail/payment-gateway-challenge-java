package com.checkout.payment.gateway.exception;

import lombok.Getter;

@Getter
public class IdempotencyKeyReuseException extends RuntimeException {

  private final String key;

  public IdempotencyKeyReuseException(String key) {
    super("Idempotency key reused with a different request body: " + key);
    this.key = key;
  }
}

