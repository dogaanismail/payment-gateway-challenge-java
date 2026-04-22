package com.checkout.payment.gateway.mapper;

import com.checkout.payment.gateway.client.BankAuthorizationRequest;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import java.util.UUID;
import lombok.experimental.UtilityClass;

@UtilityClass
public class PaymentMapper {

  public BankAuthorizationRequest toBankAuthorizationRequest(PostPaymentRequest request) {
    return new BankAuthorizationRequest(
        request.getCardNumber(),
        request.getFormattedExpiryDate(),
        request.getCurrency(),
        request.getAmount(),
        request.getCvv());
  }

  public PostPaymentResponse toPaymentResponse(UUID id, PaymentStatus status,
      PostPaymentRequest request) {
    PostPaymentResponse response = new PostPaymentResponse();
    response.setId(id);
    response.setStatus(status);
    response.setCardNumberLastFour(request.getLastFour());
    response.setExpiryMonth(request.getExpiryMonth());
    response.setExpiryYear(request.getExpiryYear());
    response.setCurrency(request.getCurrency());
    response.setAmount(request.getAmount());

    return response;
  }
}

