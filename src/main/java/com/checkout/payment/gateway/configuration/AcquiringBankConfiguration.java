package com.checkout.payment.gateway.configuration;

import com.checkout.payment.gateway.client.AcquiringBankClient;
import com.checkout.payment.gateway.client.HttpAcquiringBankClient;
import com.checkout.payment.gateway.client.ResilientAcquiringBankClient;
import com.checkout.payment.gateway.exception.BankServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig.SlidingWindowType;
import io.github.resilience4j.circuitbreaker.event.CircuitBreakerOnStateTransitionEvent;
import io.github.resilience4j.core.IntervalFunction;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.event.RetryOnErrorEvent;
import io.github.resilience4j.retry.event.RetryOnRetryEvent;
import java.net.http.HttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Slf4j
@Configuration
@EnableConfigurationProperties(AcquiringBankProperties.class)
public class AcquiringBankConfiguration {

  private static final String ACQUIRING_BANK = "acquiringBank";

  @Bean
  public RestClient acquiringBankRestClient(AcquiringBankProperties properties) {
    HttpClient httpClient = HttpClient.newBuilder()
        .connectTimeout(properties.getConnectTimeout())
        .build();
    JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
    factory.setReadTimeout(properties.getReadTimeout());
    return RestClient.builder()
        .baseUrl(properties.getUrl())
        .requestFactory(factory)
        .build();
  }

  @Bean
  public Retry acquiringBankRetry(AcquiringBankProperties properties) {
    Retry retry = Retry.of(ACQUIRING_BANK, retryConfig(properties.getResilience().getRetry()));
    retry.getEventPublisher()
        .onRetry(this::logRetry)
        .onError(this::logRetriesExhausted);
    return retry;
  }

  @Bean
  public CircuitBreaker acquiringBankCircuitBreaker(AcquiringBankProperties properties) {
    CircuitBreaker breaker = CircuitBreaker.of(
        ACQUIRING_BANK, circuitBreakerConfig(properties.getResilience().getCircuitBreaker()));
    breaker.getEventPublisher().onStateTransition(this::logStateTransition);
    return breaker;
  }

  @Bean
  public AcquiringBankClient acquiringBankClient(RestClient acquiringBankRestClient,
      Retry acquiringBankRetry, CircuitBreaker acquiringBankCircuitBreaker) {
    HttpAcquiringBankClient httpClient = new HttpAcquiringBankClient(acquiringBankRestClient);
    return new ResilientAcquiringBankClient(
        httpClient, acquiringBankRetry, acquiringBankCircuitBreaker);
  }

  private static RetryConfig retryConfig(AcquiringBankProperties.Retry properties) {
    return RetryConfig.custom()
        .maxAttempts(properties.getMaxAttempts())
        .intervalFunction(IntervalFunction.ofExponentialBackoff(
            properties.getInitialBackoff(), properties.getBackoffMultiplier()))
        .retryExceptions(BankServiceUnavailableException.class)
        .failAfterMaxAttempts(true)
        .build();
  }

  private static CircuitBreakerConfig circuitBreakerConfig(
      AcquiringBankProperties.CircuitBreaker properties) {
    return CircuitBreakerConfig.custom()
        .failureRateThreshold(properties.getFailureRateThreshold())
        .slidingWindowType(SlidingWindowType.COUNT_BASED)
        .slidingWindowSize(properties.getSlidingWindowSize())
        .minimumNumberOfCalls(properties.getMinimumNumberOfCalls())
        .waitDurationInOpenState(properties.getWaitDurationInOpenState())
        .permittedNumberOfCallsInHalfOpenState(properties.getPermittedCallsInHalfOpenState())
        .recordExceptions(BankServiceUnavailableException.class)
        .build();
  }

  private void logRetry(RetryOnRetryEvent event) {
    log.warn("Retrying acquiring bank call attempt={} cause={}",
        event.getNumberOfRetryAttempts() + 1, event.getLastThrowable());
  }

  private void logRetriesExhausted(RetryOnErrorEvent event) {
    log.error("Acquiring bank retries exhausted after {} attempts",
        event.getNumberOfRetryAttempts());
  }

  private void logStateTransition(CircuitBreakerOnStateTransitionEvent event) {
    log.warn("Acquiring bank circuit breaker {} -> {}",
        event.getStateTransition().getFromState(),
        event.getStateTransition().getToState());
  }

}
