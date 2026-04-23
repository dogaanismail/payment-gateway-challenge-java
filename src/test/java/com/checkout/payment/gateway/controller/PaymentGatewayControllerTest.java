package com.checkout.payment.gateway.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.client.AcquiringBankClient;
import com.checkout.payment.gateway.client.BankAuthorizationResponse;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.AcquiringBankException;
import com.checkout.payment.gateway.exception.BankServiceUnavailableException;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  private PaymentsRepository paymentsRepository;

  @MockitoBean
  private AcquiringBankClient acquiringBankClient;

  @Test
  void shouldReturnPaymentWhenIdExists() throws Exception {
    PostPaymentResponse payment = new PostPaymentResponse();
    payment.setId(UUID.randomUUID());
    payment.setAmount(10);
    payment.setCurrency("USD");
    payment.setStatus(PaymentStatus.AUTHORIZED);
    payment.setExpiryMonth(12);
    payment.setExpiryYear(2030);
    payment.setCardNumberLastFour("4321");

    paymentsRepository.add(payment);

    mvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/" + payment.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(payment.getStatus().getName()))
        .andExpect(jsonPath("$.card_number_last_four").value("4321"))
        .andExpect(jsonPath("$.expiry_month").value(12))
        .andExpect(jsonPath("$.expiry_year").value(2030))
        .andExpect(jsonPath("$.currency").value("USD"))
        .andExpect(jsonPath("$.amount").value(10));
  }

  @Test
  void shouldReturnNotFoundWhenPaymentIdDoesNotExist() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message").value("Payment not found"));
  }

  @Test
  void shouldReturnAuthorizedResponseWhenBankAuthorizesPayment() throws Exception {
    when(acquiringBankClient.authorize(any()))
        .thenReturn(new BankAuthorizationResponse(true, "auth-123"));

    mvc.perform(MockMvcRequestBuilders.post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "card_number": "2222405343248877",
                  "expiry_month": 4,
                  "expiry_year": 2030,
                  "currency": "GBP",
                  "amount": 100,
                  "cvv": "123"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"))
        .andExpect(jsonPath("$.status").value("Authorized"))
        .andExpect(jsonPath("$.card_number_last_four").value("8877"))
        .andExpect(jsonPath("$.currency").value("GBP"))
        .andExpect(jsonPath("$.amount").value(100));
  }

  @Test
  void shouldReturnDeclinedResponseWhenBankDeclinesPayment() throws Exception {
    when(acquiringBankClient.authorize(any()))
        .thenReturn(new BankAuthorizationResponse(false, null));

    mvc.perform(MockMvcRequestBuilders.post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "card_number": "2222405343248877",
                  "expiry_month": 4,
                  "expiry_year": 2030,
                  "currency": "USD",
                  "amount": 50,
                  "cvv": "1234"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("Declined"));
  }

  @Test
  void shouldReturnRejectedResponseWhenRequestIsInvalid() throws Exception {
    mvc.perform(MockMvcRequestBuilders.post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "card_number": "12",
                  "expiry_month": 13,
                  "expiry_year": 1999,
                  "currency": "JPY",
                  "amount": 0,
                  "cvv": "12"
                }
                """))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("Rejected"))
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.errors").isArray())
        .andExpect(jsonPath("$.errors[*].field",
            org.hamcrest.Matchers.hasItems("cardNumber", "expiryMonth", "currency", "amount",
                "cvv")));
  }

  @Test
  void shouldReturnBadGatewayWhenAcquiringBankIsUnavailable() throws Exception {
    when(acquiringBankClient.authorize(any()))
        .thenThrow(new AcquiringBankException("down"));

    mvc.perform(MockMvcRequestBuilders.post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "card_number": "2222405343248870",
                  "expiry_month": 4,
                  "expiry_year": 2030,
                  "currency": "EUR",
                  "amount": 200,
                  "cvv": "123"
                }
                """))
        .andExpect(status().isBadGateway());
  }

  @Test
  void shouldReturnBadRequestWhenPaymentIdIsNotAUuid() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/not-a-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("Rejected"))
        .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
        .andExpect(jsonPath("$.errors[0].field").value("id"));
  }

  @Test
  void shouldReturnRejectedWhenJsonBodyIsMalformed() throws Exception {
    mvc.perform(MockMvcRequestBuilders.post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{not valid json"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value("Rejected"))
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
  }

  @Test
  void shouldReturnUnsupportedMediaTypeWhenContentTypeIsNotJson() throws Exception {
    mvc.perform(MockMvcRequestBuilders.post("/api/v1/payments")
            .contentType(MediaType.TEXT_PLAIN)
            .content("hello"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
  }

  @Test
  void shouldReturnMethodNotAllowedWhenHttpMethodIsNotSupported() throws Exception {
    mvc.perform(MockMvcRequestBuilders.delete("/api/v1/payments/" + UUID.randomUUID()))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
  }

  @Test
  void shouldReplayResponseWhenSameIdempotencyKeyIsReused() throws Exception {
    when(acquiringBankClient.authorize(any()))
        .thenReturn(new BankAuthorizationResponse(true, "auth-xyz"));
    String key = "idem-" + UUID.randomUUID();
    String body = "{\"card_number\":\"2222405343248877\",\"expiry_month\":4,\"expiry_year\":2030,\"currency\":\"GBP\",\"amount\":100,\"cvv\":\"123\"}";

    String firstId = mvc.perform(MockMvcRequestBuilders.post("/api/v1/payments")
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated())
        .andExpect(header().string("Idempotent-Replayed", "false"))
        .andReturn().getResponse().getContentAsString();

    mvc.perform(MockMvcRequestBuilders.post("/api/v1/payments")
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(body))
        .andExpect(status().isCreated())
        .andExpect(header().string("Idempotent-Replayed", "true"))
        .andExpect(content().json(firstId));

    verify(acquiringBankClient, times(1)).authorize(any());
  }

  @Test
  void shouldReturnUnprocessableEntityWhenIdempotencyKeyIsReusedWithDifferentBody()
      throws Exception {
    when(acquiringBankClient.authorize(any()))
        .thenReturn(new BankAuthorizationResponse(true, "auth-xyz"));
    String key = "idem-" + UUID.randomUUID();

    mvc.perform(MockMvcRequestBuilders.post("/api/v1/payments")
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"card_number\":\"2222405343248877\",\"expiry_month\":4,\"expiry_year\":2030,\"currency\":\"GBP\",\"amount\":100,\"cvv\":\"123\"}"))
        .andExpect(status().isCreated());

    mvc.perform(MockMvcRequestBuilders.post("/api/v1/payments")
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"card_number\":\"2222405343248877\",\"expiry_month\":4,\"expiry_year\":2030,\"currency\":\"GBP\",\"amount\":999,\"cvv\":\"123\"}"))
        .andExpect(status().isUnprocessableContent())
        .andExpect(jsonPath("$.code").value("IDEMPOTENCY_KEY_REUSED"));
  }

  @Test
  void shouldEchoIncomingRequestIdHeader() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/" + UUID.randomUUID())
            .header("X-Request-Id", "trace-42"))
        .andExpect(header().string("X-Request-Id", "trace-42"));
  }

  @Test
  void shouldGenerateRequestIdWhenHeaderIsAbsent() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/" + UUID.randomUUID()))
        .andExpect(header().exists("X-Request-Id"));
  }

  @Test
  void shouldReturnServiceUnavailableWhenAcquiringBankReturns503() throws Exception {
    when(acquiringBankClient.authorize(any()))
        .thenThrow(new BankServiceUnavailableException("bank 503"));

    mvc.perform(MockMvcRequestBuilders.post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "card_number": "2222405343248870",
                  "expiry_month": 4,
                  "expiry_year": 2030,
                  "currency": "EUR",
                  "amount": 200,
                  "cvv": "123"
                }
                """))
        .andExpect(status().isServiceUnavailable())
        .andExpect(header().string("Retry-After", "30"))
        .andExpect(jsonPath("$.code").value("ACQUIRING_BANK_SERVICE_UNAVAILABLE"));
  }

  @Test
  void shouldNeverReturnFullCardNumberOrCvvAndShouldExposeOnlyLastFour() throws Exception {
    when(acquiringBankClient.authorize(any()))
        .thenReturn(new BankAuthorizationResponse(true, "auth-mask"));

    String body = mvc.perform(MockMvcRequestBuilders.post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "card_number": "4242424242421111",
                  "expiry_month": 4,
                  "expiry_year": 2030,
                  "currency": "GBP",
                  "amount": 100,
                  "cvv": "987"
                }
                """))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.card_number_last_four").value("1111"))
        .andExpect(jsonPath("$.card_number").doesNotExist())
        .andExpect(jsonPath("$.cardNumber").doesNotExist())
        .andExpect(jsonPath("$.cvv").doesNotExist())
        .andReturn().getResponse().getContentAsString();

    Assertions.assertThat(body)
        .doesNotContain("4242424242421111")
        .doesNotContain("987");
  }

  @Test
  void shouldNotPersistOrReturnCvvWhenFetchingPaymentById() throws Exception {
    when(acquiringBankClient.authorize(any()))
        .thenReturn(new BankAuthorizationResponse(true, "auth-store"));

    String createdBody = mvc.perform(MockMvcRequestBuilders.post("/api/v1/payments")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "card_number": "4242424242421111",
                  "expiry_month": 4,
                  "expiry_year": 2030,
                  "currency": "GBP",
                  "amount": 100,
                  "cvv": "987"
                }
                """))
        .andExpect(status().isCreated())
        .andReturn().getResponse().getContentAsString();

    String id = com.jayway.jsonpath.JsonPath.read(createdBody, "$.id");

    String fetched = mvc.perform(MockMvcRequestBuilders.get("/api/v1/payments/" + id))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cvv").doesNotExist())
        .andExpect(jsonPath("$.card_number").doesNotExist())
        .andReturn().getResponse().getContentAsString();

    Assertions.assertThat(fetched)
        .doesNotContain("4242424242421111")
        .doesNotContain("987");
  }
}

