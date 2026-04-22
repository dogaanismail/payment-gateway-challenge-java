package com.checkout.payment.gateway.idempotency;

import static org.assertj.core.api.Assertions.assertThat;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import org.junit.jupiter.api.Test;

class RequestFingerprinterTest {

  @Test
  void shouldProduceIdenticalFingerprintForIdenticalRequests() {
    String first = RequestFingerprinter.fingerprint(request());
    String second = RequestFingerprinter.fingerprint(request());

    assertThat(first).isEqualTo(second).hasSize(64);
  }

  @Test
  void shouldProduceDifferentFingerprintWhenAmountChanges() {
    PostPaymentRequest a = request();
    PostPaymentRequest b = request();
    b.setAmount(2000);

    assertThat(RequestFingerprinter.fingerprint(a))
        .isNotEqualTo(RequestFingerprinter.fingerprint(b));
  }

  @Test
  void shouldProduceDifferentFingerprintWhenCardNumberChanges() {
    PostPaymentRequest a = request();
    PostPaymentRequest b = request();
    b.setCardNumber("4111111111111111");

    assertThat(RequestFingerprinter.fingerprint(a))
        .isNotEqualTo(RequestFingerprinter.fingerprint(b));
  }

  private PostPaymentRequest request() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("2222405343248877");
    request.setExpiryMonth(4);
    request.setExpiryYear(2030);
    request.setCurrency("GBP");
    request.setAmount(1050);
    request.setCvv("123");
    return request;
  }
}

