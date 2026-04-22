package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.exception.AcquiringBankException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class HttpAcquiringBankClient implements AcquiringBankClient {

  private final RestClient acquiringBankRestClient;

  @Override
  public BankAuthorizationResponse authorize(BankAuthorizationRequest request) {
    try {
      BankAuthorizationResponse response = acquiringBankRestClient.post()
          .contentType(MediaType.APPLICATION_JSON)
          .body(request)
          .retrieve()
          .body(BankAuthorizationResponse.class);
      if (response == null) {
        throw new AcquiringBankException("Empty response from acquiring bank");
      }
      return response;
    } catch (RestClientResponseException ex) {
      log.error("Acquiring bank returned error status {}", ex.getStatusCode().value());
      throw new AcquiringBankException(
          "Acquiring bank error: " + ex.getStatusCode().value(), ex);
    } catch (ResourceAccessException ex) {
      log.error("Acquiring bank unreachable", ex);
      throw new AcquiringBankException("Acquiring bank unreachable", ex);
    }
  }
}
