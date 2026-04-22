package com.checkout.payment.gateway.client;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BankAuthorizationRequestTest {

  @Test
  void shouldNotExposeCardNumberOrCvvInToString() {
    BankAuthorizationRequest request =
        new BankAuthorizationRequest("2222405343248877", "04/2030", "GBP", 1050, "123");

    String output = request.toString();

    assertThat(output)
        .doesNotContain("2222405343248877")
        .doesNotContain("123")
        .contains("currency=GBP")
        .contains("expiryDate=04/2030")
        .contains("amount=1050");
  }
}

