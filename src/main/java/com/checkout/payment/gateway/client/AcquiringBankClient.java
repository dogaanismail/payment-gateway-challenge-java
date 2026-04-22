package com.checkout.payment.gateway.client;

public interface AcquiringBankClient {

  BankAuthorizationResponse authorize(BankAuthorizationRequest request);
}

