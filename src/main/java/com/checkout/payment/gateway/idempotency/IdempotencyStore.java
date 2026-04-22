package com.checkout.payment.gateway.idempotency;

import com.checkout.payment.gateway.model.PostPaymentResponse;
import java.util.function.Supplier;

public interface IdempotencyStore {

  IdempotencyResult computeIfAbsent(
      String key,
      String fingerprint,
      Supplier<PostPaymentResponse> supplier);
}

