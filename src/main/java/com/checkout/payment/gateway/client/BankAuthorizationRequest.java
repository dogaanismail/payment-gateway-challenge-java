package com.checkout.payment.gateway.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@RequiredArgsConstructor
@ToString(exclude = {"cardNumber", "cvv"})
public class BankAuthorizationRequest {

  @JsonProperty("card_number")
  private final String cardNumber;

  @JsonProperty("expiry_date")
  private final String expiryDate;

  private final String currency;
  private final int amount;
  private final String cvv;
}
