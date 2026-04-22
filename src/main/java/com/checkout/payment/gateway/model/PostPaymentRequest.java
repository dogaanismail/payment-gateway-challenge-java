package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.io.Serializable;
import java.time.YearMonth;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(exclude = {"cardNumber", "cvv"})
public class PostPaymentRequest implements Serializable {

  @JsonProperty("card_number")
  @NotBlank
  @Pattern(regexp = "^[0-9]{14,19}$", message = "card_number must be 14-19 numeric digits")
  private String cardNumber;

  @JsonProperty("expiry_month")
  @Min(value = 1, message = "expiry_month must be between 1 and 12")
  @Max(value = 12, message = "expiry_month must be between 1 and 12")
  private int expiryMonth;

  @JsonProperty("expiry_year")
  @Min(value = 1, message = "expiry_year is required")
  private int expiryYear;

  @NotBlank
  @Pattern(regexp = "^(GBP|USD|EUR)$", message = "currency must be one of GBP, USD, EUR")
  private String currency;

  @Min(value = 1, message = "amount must be a positive integer")
  private int amount;

  @NotBlank
  @Pattern(regexp = "^[0-9]{3,4}$", message = "cvv must be 3-4 numeric digits")
  private String cvv;

  @JsonIgnore
  @AssertTrue(message = "expiry_month + expiry_year must be in the future")
  public boolean isExpiryInFuture() {
    if (expiryMonth < 1 || expiryMonth > 12 || expiryYear < 1) {
      return true;
    }
    return YearMonth.of(expiryYear, expiryMonth).isAfter(YearMonth.now());
  }

  @JsonIgnore
  public String getLastFour() {
    return cardNumber == null ? null : cardNumber.substring(cardNumber.length() - 4);
  }

  @JsonIgnore
  public String getFormattedExpiryDate() {
    return String.format("%02d/%d", expiryMonth, expiryYear);
  }
}
