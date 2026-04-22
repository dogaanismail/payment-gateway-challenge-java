package com.checkout.payment.gateway.idempotency;

import static org.assertj.core.api.Assertions.assertThat;

import com.checkout.payment.gateway.model.PostPaymentResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class InMemoryIdempotencyStoreConcurrencyTest {

  @Test
  void shouldOnlyInvokeSupplierOnceWhenManyThreadsUseSameKeyConcurrently() throws Exception {
    InMemoryIdempotencyStore store = new InMemoryIdempotencyStore();
    int threads = 16;
    String key = "key-" + UUID.randomUUID();
    AtomicInteger supplierInvocations = new AtomicInteger();
    CountDownLatch start = new CountDownLatch(1);
    ExecutorService pool = Executors.newFixedThreadPool(threads);

    List<Future<IdempotencyResult>> futures = new ArrayList<>(threads);
    for (int i = 0; i < threads; i++) {
      futures.add(pool.submit(() -> {
        start.await();
        return store.computeIfAbsent(key, "fp", () -> {
          supplierInvocations.incrementAndGet();
          try {
            Thread.sleep(20);
          } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
          }
          PostPaymentResponse r = new PostPaymentResponse();
          r.setId(UUID.randomUUID());
          return r;
        });
      }));
    }
    start.countDown();
    pool.shutdown();
    pool.awaitTermination(5, TimeUnit.SECONDS);

    assertThat(supplierInvocations.get()).isEqualTo(1);
    UUID firstId = futures.getFirst().get().response().getId();
    int replayed = 0;
    for (Future<IdempotencyResult> f : futures) {
      IdempotencyResult r = f.get();
      assertThat(r.response().getId()).isEqualTo(firstId);
      if (r.replayed()) {
        replayed++;
      }
    }
    assertThat(replayed).isEqualTo(threads - 1);
  }
}

