package com.checkout.payment.gateway.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

  public static final String REQUEST_ID_HEADER = "X-Request-Id";
  public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
  public static final String REQUEST_ID_MDC_KEY = "requestId";
  public static final String CORRELATION_ID_MDC_KEY = "correlationId";

  @Override
  protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
      FilterChain chain) throws ServletException, IOException {

    String requestId = headerOrGenerate(request, REQUEST_ID_HEADER);
    String correlationId = headerOrDefault(request, CORRELATION_ID_HEADER, requestId);

    response.setHeader(REQUEST_ID_HEADER, requestId);
    response.setHeader(CORRELATION_ID_HEADER, correlationId);
    MDC.put(REQUEST_ID_MDC_KEY, requestId);
    MDC.put(CORRELATION_ID_MDC_KEY, correlationId);

    try {
      chain.doFilter(request, response);
    } finally {
      MDC.remove(REQUEST_ID_MDC_KEY);
      MDC.remove(CORRELATION_ID_MDC_KEY);
    }
  }

  private static String headerOrGenerate(HttpServletRequest request, String name) {
    String value = request.getHeader(name);
    return (value == null || value.isBlank()) ? UUID.randomUUID().toString() : value;
  }

  private static String headerOrDefault(HttpServletRequest request, String name, String fallback) {
    String value = request.getHeader(name);
    return (value == null || value.isBlank()) ? fallback : value;
  }
}
