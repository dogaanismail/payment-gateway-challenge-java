package com.checkout.payment.gateway.client;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class BankAuthorizationResponse {

  private final boolean authorized;
  private final String authorizationCode;

  @JsonCreator
  public BankAuthorizationResponse(
      @JsonProperty("authorized") boolean authorized,
      @JsonProperty("authorization_code") String authorizationCode) {
    this.authorized = authorized;
    this.authorizationCode = authorizationCode;
  }
}
