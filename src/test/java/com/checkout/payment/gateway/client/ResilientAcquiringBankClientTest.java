package com.checkout.payment.gateway.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.exception.AcquiringBankException;
import com.checkout.payment.gateway.exception.BankServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ResilientAcquiringBankClientTest {

  private static final BankAuthorizationRequest REQUEST =
      new BankAuthorizationRequest("4111", "12/30", "GBP", 100, "123");

  private AcquiringBankClient delegate;
  private Retry retry;
  private CircuitBreaker circuitBreaker;
  private ResilientAcquiringBankClient client;

  @BeforeEach
  void setUp() {
    delegate = mock(AcquiringBankClient.class);
    retry = Retry.of("test", RetryConfig.custom()
        .maxAttempts(3)
        .intervalFunction(IntervalFunction.ofExponentialBackoff(Duration.ofMillis(1), 2.0))
        .retryExceptions(BankServiceUnavailableException.class)
        .failAfterMaxAttempts(true)
        .build());
    circuitBreaker = CircuitBreaker.of("test", CircuitBreakerConfig.custom()
        .failureRateThreshold(50f)
        .slidingWindowSize(4)
        .minimumNumberOfCalls(2)
        .waitDurationInOpenState(Duration.ofSeconds(60))
        .recordExceptions(BankServiceUnavailableException.class)
        .build());
    client = new ResilientAcquiringBankClient(delegate, retry, circuitBreaker);
  }

  @Test
  void shouldRetryOnBankServiceUnavailableUpToMaxAttempts() {
    when(delegate.authorize(any()))
        .thenThrow(new BankServiceUnavailableException("503"));

    assertThatThrownBy(() -> client.authorize(REQUEST))
        .isInstanceOf(BankServiceUnavailableException.class);

    verify(delegate, times(3)).authorize(any());
  }

  @Test
  void shouldNotRetryOnGenericAcquiringBankException() {
    when(delegate.authorize(any()))
        .thenThrow(new AcquiringBankException("500"));

    assertThatThrownBy(() -> client.authorize(REQUEST))
        .isInstanceOf(AcquiringBankException.class);

    verify(delegate, times(1)).authorize(any());
  }

  @Test
  void shouldReturnSuccessfullyWithoutRetryOnSuccess() {
    when(delegate.authorize(any()))
        .thenReturn(new BankAuthorizationResponse(true, "auth-1"));

    BankAuthorizationResponse response = client.authorize(REQUEST);

    assertThat(response.isAuthorized()).isTrue();
    verify(delegate, times(1)).authorize(any());
  }

  @Test
  void shouldFastFailWithBankUnavailableWhenCircuitBreakerOpens() {
    when(delegate.authorize(any()))
        .thenThrow(new BankServiceUnavailableException("503"));

    for (int i = 0; i < 4; i++) {
      try {
        client.authorize(REQUEST);
      } catch (BankServiceUnavailableException ignored) {
        // expected during breaker tripping
      }
    }

    assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

    long failedCallsBefore = circuitBreaker.getMetrics().getNumberOfFailedCalls();
    assertThatThrownBy(() -> client.authorize(REQUEST))
        .isInstanceOf(BankServiceUnavailableException.class)
        .hasMessageContaining("circuit breaker is open");
    assertThat(circuitBreaker.getMetrics().getNumberOfFailedCalls()).isEqualTo(failedCallsBefore);
  }
}
