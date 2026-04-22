package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.model.ApiErrorResponse;
import com.checkout.payment.gateway.model.ApiErrorResponse.FieldViolation;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@Slf4j
@RestControllerAdvice
public class CommonExceptionHandler {

  @ExceptionHandler(PaymentNotFoundException.class)
  public ResponseEntity<ApiErrorResponse> handleNotFound(PaymentNotFoundException ex,
      HttpServletRequest req) {
    log.debug("Payment not found", ex);
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ApiErrorResponse.of("Error", "PAYMENT_NOT_FOUND", "Payment not found",
            req.getRequestURI()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex,
      HttpServletRequest req) {
    List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
        .map(fe -> new FieldViolation(fe.getField(), fe.getDefaultMessage()))
        .toList();
    log.info("Rejected payment due to validation: {}", violations);
    return ResponseEntity.badRequest().body(ApiErrorResponse.rejected(
        "VALIDATION_FAILED", "Request validation failed", violations, req.getRequestURI()));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<ApiErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
      HttpServletRequest req) {
    Throwable cause = ex.getMostSpecificCause();
    log.info("Rejected payment due to malformed request: {}",
        cause != null ? cause.getMessage() : ex.getMessage());
    return ResponseEntity.badRequest().body(ApiErrorResponse.rejected(
        "MALFORMED_REQUEST", "Malformed request body", null, req.getRequestURI()));
  }

  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
      HttpServletRequest req) {
    log.info("Rejected request due to type mismatch on '{}'", ex.getName());
    String message = "Invalid value for path/query parameter '" + ex.getName() + "'";
    return ResponseEntity.badRequest().body(ApiErrorResponse.rejected(
        "INVALID_PARAMETER", message,
        List.of(new FieldViolation(ex.getName(), message)), req.getRequestURI()));
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ApiErrorResponse> handleUnsupportedMediaType(
      HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
    log.info("Unsupported media type: {}", ex.getContentType());
    return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
        .body(ApiErrorResponse.of("Error", "UNSUPPORTED_MEDIA_TYPE",
            "Content-Type must be application/json", req.getRequestURI()));
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(
      HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
    log.info("Method not allowed: {}", ex.getMethod());
    return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
        .body(ApiErrorResponse.of("Error", "METHOD_NOT_ALLOWED",
            "HTTP method " + ex.getMethod() + " is not supported on this endpoint",
            req.getRequestURI()));
  }

  @ExceptionHandler(AcquiringBankException.class)
  public ResponseEntity<ApiErrorResponse> handleBank(AcquiringBankException ex,
      HttpServletRequest req) {
    log.error("Acquiring bank failure", ex);
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(ApiErrorResponse.of("Error", "ACQUIRING_BANK_UNAVAILABLE",
            "Acquiring bank unavailable", req.getRequestURI()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, HttpServletRequest req) {
    log.error("Unexpected error", ex);
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ApiErrorResponse.of("Error", "INTERNAL_ERROR", "Internal server error",
            req.getRequestURI()));
  }
}
