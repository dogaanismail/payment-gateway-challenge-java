package com.checkout.payment.gateway.idempotency;

import com.checkout.payment.gateway.exception.IdempotencyKeyReuseException;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

@Component
public class InMemoryIdempotencyStore implements IdempotencyStore {

  private final ConcurrentHashMap<String, IdempotencyRecord> records = new ConcurrentHashMap<>();

  @Override
  public IdempotencyResult computeIfAbsent(String key, String fingerprint,
      Supplier<PostPaymentResponse> supplier) {
    AtomicBoolean computed = new AtomicBoolean(false);

    IdempotencyRecord idempotencyRecord = records.computeIfAbsent(key, k -> {
      PostPaymentResponse response = supplier.get();
      computed.set(true);
      return new IdempotencyRecord(fingerprint, response, Instant.now());
    });

    if (!computed.get() && !idempotencyRecord.fingerprint().equals(fingerprint)) {
      throw new IdempotencyKeyReuseException(key);
    }

    return new IdempotencyResult(idempotencyRecord.response(), !computed.get());
  }
}

