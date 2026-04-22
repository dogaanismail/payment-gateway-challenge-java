package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.client.AcquiringBankClient;
import com.checkout.payment.gateway.client.BankAuthorizationResponse;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.AcquiringBankException;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.mapper.PaymentMapper;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
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

  public PostPaymentResponse getPaymentById(UUID id) {
    log.debug("Fetching payment with id={}", id);
    return paymentsRepository.get(id).orElseThrow(() -> new PaymentNotFoundException(id));
  }

  public PostPaymentResponse processPayment(PostPaymentRequest request) {
    BankAuthorizationResponse bankResponse;
    try {
      bankResponse = acquiringBankClient.authorize(PaymentMapper.toBankAuthorizationRequest(request));
    } catch (AcquiringBankException ex) {
      log.warn("Acquiring bank call failed for last4={}", request.getLastFour());
      throw ex;
    }

    PaymentStatus status = bankResponse.isAuthorized()
        ? PaymentStatus.AUTHORIZED
        : PaymentStatus.DECLINED;

    PostPaymentResponse response =
        PaymentMapper.toPaymentResponse(UUID.randomUUID(), status, request);
    paymentsRepository.add(response);
    log.info("Processed payment id={} status={} last4={}",
        response.getId(), status, response.getCardNumberLastFour());
    return response;
  }
}
