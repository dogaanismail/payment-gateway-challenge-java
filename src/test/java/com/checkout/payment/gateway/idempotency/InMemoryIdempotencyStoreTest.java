package com.checkout.payment.gateway.idempotency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.checkout.payment.gateway.exception.IdempotencyKeyReuseException;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class InMemoryIdempotencyStoreTest {

  private final InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();

  @Test
  void shouldExecuteSupplierAndReturnFreshResultOnFirstCall() {
    PostPaymentResponse response = response();

    IdempotencyResult result = store.computeIfAbsent("key-1", "fp", () -> response);

    assertThat(result.replayed()).isFalse();
    assertThat(result.response()).isSameAs(response);
  }

  @Test
  void shouldReplayCachedResponseAndNotInvokeSupplierOnSecondCallWithSameFingerprint() {
    PostPaymentResponse first = response();
    AtomicInteger calls = new AtomicInteger();

    store.computeIfAbsent("key-2", "fp", () -> { calls.incrementAndGet(); return first; });
    IdempotencyResult replay = store.computeIfAbsent("key-2", "fp", () -> {
      calls.incrementAndGet();
      return response();
    });

    assertThat(calls.get()).isEqualTo(1);
    assertThat(replay.replayed()).isTrue();
    assertThat(replay.response()).isSameAs(first);
  }

  @Test
  void shouldThrowWhenSameKeyIsReusedWithDifferentFingerprint() {
    store.computeIfAbsent("key-3", "fp-original", this::response);

    assertThatThrownBy(() ->
        store.computeIfAbsent("key-3", "fp-different", this::response))
        .isInstanceOf(IdempotencyKeyReuseException.class)
        .hasMessageContaining("key-3");
  }

  @Test
  void shouldNotCacheWhenSupplierThrows() {
    assertThatThrownBy(() ->
        store.computeIfAbsent("key-4", "fp", () -> { throw new RuntimeException("boom"); }))
        .isInstanceOf(RuntimeException.class);

    PostPaymentResponse retry = response();
    IdempotencyResult result = store.computeIfAbsent("key-4", "fp", () -> retry);

    assertThat(result.replayed()).isFalse();
    assertThat(result.response()).isSameAs(retry);
  }

  private PostPaymentResponse response() {
    PostPaymentResponse response = new PostPaymentResponse();
    response.setId(UUID.randomUUID());
    return response;
  }
}

