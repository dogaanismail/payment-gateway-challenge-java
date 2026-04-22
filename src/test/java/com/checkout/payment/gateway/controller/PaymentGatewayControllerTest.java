package com.checkout.payment.gateway.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.client.AcquiringBankClient;
import com.checkout.payment.gateway.client.BankAuthorizationResponse;
import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.AcquiringBankException;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
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
        .andExpect(jsonPath("$.status").value("Rejected"));
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
}
