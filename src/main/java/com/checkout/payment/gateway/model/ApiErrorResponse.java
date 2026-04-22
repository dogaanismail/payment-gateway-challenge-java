package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiErrorResponse(
    String status,
    String code,
    String message,
    List<FieldViolation> errors,
    Instant timestamp,
    String path) {

  public static ApiErrorResponse of(String status, String code, String message, String path) {
    return new ApiErrorResponse(status, code, message, null, Instant.now(), path);
  }

  public static ApiErrorResponse rejected(String code, String message,
      List<FieldViolation> errors, String path) {
    return new ApiErrorResponse("Rejected", code, message, errors, Instant.now(), path);
  }

  public record FieldViolation(String field, String message) {}
}

