package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.AcquiringBankClient;
import com.checkout.payment.gateway.client.BankAuthorizationResponse;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.idempotency.IdempotencyResult;
import com.checkout.payment.gateway.idempotency.IdempotencyStore;
import com.checkout.payment.gateway.idempotency.RequestFingerprinter;
import com.checkout.payment.gateway.mapper.PaymentMapper;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentGatewayService {

  private final PaymentsRepository paymentsRepository;
  private final AcquiringBankClient acquiringBankClient;
  private final IdempotencyStore idempotencyStore;

  public PostPaymentResponse getPaymentById(UUID id) {
    log.debug("Fetching payment with id={}", id);
    return paymentsRepository.get(id).orElseThrow(() -> new PaymentNotFoundException(id));
  }

  public ProcessedPayment processPayment(PostPaymentRequest request, String idempotencyKey) {
    log.debug("Processing payment last4={} amount={} currency={} idempotencyKey={}",
        request.getLastFour(), request.getAmount(), request.getCurrency(),
        idempotencyKey == null ? "<none>" : "<present>");

    long startNanos = System.nanoTime();
    try {
      ProcessedPayment processed = (idempotencyKey == null || idempotencyKey.isBlank())
          ? new ProcessedPayment(doProcess(request), false)
          : processIdempotent(request, idempotencyKey);

      log.info("Payment processed id={} status={} replayed={} durationMs={}",
          processed.response().getId(),
          processed.response().getStatus(),
          processed.replayed(),
          elapsedMs(startNanos));
      return processed;
    } catch (RuntimeException ex) {
      log.warn("Payment failed last4={} durationMs={} cause={}",
          request.getLastFour(), elapsedMs(startNanos), ex.toString());
      throw ex;
    }
  }

  private ProcessedPayment processIdempotent(PostPaymentRequest request, String idempotencyKey) {
    String fingerprint = RequestFingerprinter.fingerprint(request);
    IdempotencyResult result = idempotencyStore.computeIfAbsent(
        idempotencyKey, fingerprint, () -> doProcess(request));

    if (result.replayed()) {
      log.info("Replayed idempotent payment id={} key={}",
          result.response().getId(), idempotencyKey);
    }

    return new ProcessedPayment(result.response(), result.replayed());
  }

  private PostPaymentResponse doProcess(PostPaymentRequest request) {
    BankAuthorizationResponse bankResponse = acquiringBankClient.authorize(
        PaymentMapper.toBankAuthorizationRequest(request));

    PaymentStatus status = bankResponse.isAuthorized()
        ? PaymentStatus.AUTHORIZED
        : PaymentStatus.DECLINED;

    PostPaymentResponse response =
        PaymentMapper.toPaymentResponse(UUID.randomUUID(), status, request);
    paymentsRepository.add(response);
    return response;
  }

  private static long elapsedMs(long startNanos) {
    return Duration.ofNanos(System.nanoTime() - startNanos).toMillis();
  }
}
