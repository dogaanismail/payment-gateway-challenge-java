package com.checkout.payment.gateway.model;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.time.YearMonth;
import java.util.Set;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PostPaymentRequestValidationTest {

  private static ValidatorFactory factory;
  private static Validator validator;

  @BeforeAll
  static void setUpValidator() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void closeValidator() {
    factory.close();
  }

  @Test
  void shouldHaveNoViolationsForValidRequest() {
    Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(validRequest());

    assertThat(violations).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "1234567890123", "12345678901234567890", "abcdabcdabcdab"})
  void shouldRejectInvalidCardNumbers(String invalid) {
    PostPaymentRequest request = validRequest();
    request.setCardNumber(invalid);

    assertThat(fields(validator.validate(request))).contains("cardNumber");
  }

  @ParameterizedTest
  @ValueSource(ints = {0, 13, -1, 99})
  void shouldRejectInvalidExpiryMonth(int month) {
    PostPaymentRequest request = validRequest();
    request.setExpiryMonth(month);

    assertThat(fields(validator.validate(request))).contains("expiryMonth");
  }

  @Test
  void shouldRejectExpiryInThePast() {
    PostPaymentRequest request = validRequest();
    YearMonth lastMonth = YearMonth.now().minusMonths(1);
    request.setExpiryMonth(lastMonth.getMonthValue());
    request.setExpiryYear(lastMonth.getYear());

    assertThat(fields(validator.validate(request))).contains("expiryInFuture");
  }

  @ParameterizedTest
  @ValueSource(strings = {"JPY", "gbp", "EU", "EUROS", ""})
  void shouldRejectUnsupportedCurrencies(String currency) {
    PostPaymentRequest request = validRequest();
    request.setCurrency(currency);

    assertThat(fields(validator.validate(request))).contains("currency");
  }

  @ParameterizedTest
  @ValueSource(ints = {0, -1, -100})
  void shouldRejectNonPositiveAmount(int amount) {
    PostPaymentRequest request = validRequest();
    request.setAmount(amount);

    assertThat(fields(validator.validate(request))).contains("amount");
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "12", "12345", "abc", "12a"})
  void shouldRejectInvalidCvv(String cvv) {
    PostPaymentRequest request = validRequest();
    request.setCvv(cvv);

    assertThat(fields(validator.validate(request))).contains("cvv");
  }

  @Test
  void shouldNotExposeCardNumberOrCvvInToString() {
    PostPaymentRequest request = validRequest();

    String output = request.toString();

    assertThat(output)
        .doesNotContain(request.getCardNumber())
        .doesNotContain(request.getCvv())
        .contains("currency=GBP")
        .contains("amount=1050");
  }

  private static List<String> fields(Set<ConstraintViolation<PostPaymentRequest>> violations) {
    return violations.stream().map(v -> v.getPropertyPath().toString()).toList();
  }

  private static PostPaymentRequest validRequest() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("2222405343248877");
    request.setExpiryMonth(YearMonth.now().plusYears(2).getMonthValue());
    request.setExpiryYear(YearMonth.now().plusYears(2).getYear());
    request.setCurrency("GBP");
    request.setAmount(1050);
    request.setCvv("123");
    return request;
  }
}

