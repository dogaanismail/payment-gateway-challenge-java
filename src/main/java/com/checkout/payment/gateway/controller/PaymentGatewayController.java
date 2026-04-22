package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import com.checkout.payment.gateway.service.ProcessedPayment;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping(PaymentGatewayController.BASE_PATH)
public class PaymentGatewayController {

  public static final String API_VERSION = "v1";
  public static final String BASE_PATH = "/api/" + API_VERSION + "/payments";
  public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
  public static final String IDEMPOTENT_REPLAYED_HEADER = "Idempotent-Replayed";

  private final PaymentGatewayService paymentGatewayService;

  @GetMapping("/{id}")
  public ResponseEntity<PostPaymentResponse> getPaymentById(@PathVariable UUID id) {
    return ResponseEntity.ok(paymentGatewayService.getPaymentById(id));
  }

  @PostMapping
  public ResponseEntity<PostPaymentResponse> createPayment(
      @Valid @RequestBody PostPaymentRequest request,
      @RequestHeader(value = IDEMPOTENCY_KEY_HEADER, required = false)
      @Size(min = 1, max = 255, message = "Idempotency-Key must be 1-255 characters")
      @Pattern(regexp = "^[A-Za-z0-9_\\-]+$",
          message = "Idempotency-Key must contain only letters, digits, '-' or '_'")
      String idempotencyKey) {
    ProcessedPayment processed = paymentGatewayService.processPayment(request, idempotencyKey);
    URI location = ServletUriComponentsBuilder.fromCurrentRequest()
        .replacePath(BASE_PATH + "/" + processed.response().getId())
        .build()
        .toUri();
    return ResponseEntity.created(location)
        .header(IDEMPOTENT_REPLAYED_HEADER, Boolean.toString(processed.replayed()))
        .body(processed.response());
  }
}
