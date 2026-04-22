package com.checkout.payment.gateway.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.checkout.payment.gateway.client.BankAuthorizationRequest;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PaymentMapperTest {

  @Test
  void shouldCopyAllFieldsAndFormatExpiryWhenMappingToBankAuthorizationRequest() {
    PostPaymentRequest request = sampleRequest();

    BankAuthorizationRequest bankRequest = PaymentMapper.toBankAuthorizationRequest(request);

    assertThat(bankRequest.getCardNumber()).isEqualTo("2222405343248877");
    assertThat(bankRequest.getExpiryDate()).isEqualTo("04/2030");
    assertThat(bankRequest.getCurrency()).isEqualTo("GBP");
    assertThat(bankRequest.getAmount()).isEqualTo(1050);
    assertThat(bankRequest.getCvv()).isEqualTo("123");
  }

  @Test
  void shouldPadSingleDigitMonthWhenFormattingExpiryDate() {
    PostPaymentRequest request = sampleRequest();
    request.setExpiryMonth(3);

    BankAuthorizationRequest bankRequest = PaymentMapper.toBankAuthorizationRequest(request);

    assertThat(bankRequest.getExpiryDate()).isEqualTo("03/2030");
  }

  @Test
  void shouldMapAllFieldsAndDeriveLastFourWhenBuildingPaymentResponse() {
    PostPaymentRequest request = sampleRequest();
    UUID id = UUID.randomUUID();

    PostPaymentResponse response =
        PaymentMapper.toPaymentResponse(id, PaymentStatus.AUTHORIZED, request);

    assertThat(response.getId()).isEqualTo(id);
    assertThat(response.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    assertThat(response.getCardNumberLastFour()).isEqualTo("8877");
    assertThat(response.getExpiryMonth()).isEqualTo(4);
    assertThat(response.getExpiryYear()).isEqualTo(2030);
    assertThat(response.getCurrency()).isEqualTo("GBP");
    assertThat(response.getAmount()).isEqualTo(1050);
  }

  @Test
  void shouldNeverExposeFullCardNumberInPaymentResponse() {
    PostPaymentRequest request = sampleRequest();

    PostPaymentResponse response =
        PaymentMapper.toPaymentResponse(UUID.randomUUID(), PaymentStatus.DECLINED, request);

    assertThat(response.getCardNumberLastFour()).hasSize(4);
    assertThat(response).hasNoNullFieldsOrPropertiesExcept();
  }

  private PostPaymentRequest sampleRequest() {
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

