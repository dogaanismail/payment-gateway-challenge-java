package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.ErrorResponse;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class CommonExceptionHandler {

  @ExceptionHandler(PaymentNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(PaymentNotFoundException ex) {
    log.debug("Payment not found", ex);
    return new ResponseEntity<>(new ErrorResponse("Payment not found"), HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<RejectedResponse> handleValidation(MethodArgumentNotValidException ex) {
    String message = ex.getBindingResult().getFieldErrors().stream()
        .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
        .collect(Collectors.joining("; "));
    log.info("Rejected payment due to validation: {}", message);
    return ResponseEntity.badRequest().body(new RejectedResponse(PaymentStatus.REJECTED, message));
  }

  @ExceptionHandler(HttpMessageNotReadableException.class)
  public ResponseEntity<RejectedResponse> handleUnreadable(HttpMessageNotReadableException ex) {
    Throwable cause = ex.getMostSpecificCause();
    log.info("Rejected payment due to malformed request: {}",
        cause != null ? cause.getMessage() : ex.getMessage());
    return ResponseEntity.badRequest()
        .body(new RejectedResponse(PaymentStatus.REJECTED, "Malformed request body"));
  }

  @ExceptionHandler(AcquiringBankException.class)
  public ResponseEntity<ErrorResponse> handleBank(AcquiringBankException ex) {
    log.error("Acquiring bank failure", ex);
    return new ResponseEntity<>(new ErrorResponse("Acquiring bank unavailable"),
        HttpStatus.BAD_GATEWAY);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
    log.error("Unexpected error", ex);
    return new ResponseEntity<>(new ErrorResponse("Internal server error"),
        HttpStatus.INTERNAL_SERVER_ERROR);
  }

  public record RejectedResponse(PaymentStatus status, String message) {}
}
