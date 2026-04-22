package com.checkout.payment.gateway.client;

import com.checkout.payment.gateway.exception.AcquiringBankException;
import com.checkout.payment.gateway.exception.BankServiceUnavailableException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
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
      int status = ex.getStatusCode().value();
      if (status == HttpStatus.SERVICE_UNAVAILABLE.value()) {
        log.warn("Acquiring bank returned 503 Service Unavailable");
        throw new BankServiceUnavailableException("Acquiring bank returned 503 Service Unavailable",
            ex);
      }
      log.error("Acquiring bank returned error status {}", status);
      throw new AcquiringBankException("Acquiring bank error: " + status, ex);
    } catch (ResourceAccessException ex) {
      log.error("Acquiring bank unreachable", ex);
      throw new BankServiceUnavailableException("Acquiring bank unreachable", ex);
    }
  }
}


