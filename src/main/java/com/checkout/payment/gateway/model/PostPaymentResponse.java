package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostPaymentResponse {

  private UUID id;
  private PaymentStatus status;

  @JsonProperty("card_number_last_four")
  private String cardNumberLastFour;

  @JsonProperty("expiry_month")
  private int expiryMonth;

  @JsonProperty("expiry_year")
  private int expiryYear;

  private String currency;
  private int amount;
}
