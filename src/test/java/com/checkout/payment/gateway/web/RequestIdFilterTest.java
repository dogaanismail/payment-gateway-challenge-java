package com.checkout.payment.gateway.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

  private final RequestIdFilter filter = new RequestIdFilter();

  @Test
  void shouldEchoIncomingRequestIdHeader() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.addHeader(RequestIdFilter.HEADER, "abc-123");
    FilterChain chain = (req, res) ->
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isEqualTo("abc-123");

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader(RequestIdFilter.HEADER)).isEqualTo("abc-123");
    assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNull();
  }

  @Test
  void shouldGenerateRequestIdWhenHeaderIsAbsent() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = (req, res) ->
        assertThat(MDC.get(RequestIdFilter.MDC_KEY)).isNotBlank();

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader(RequestIdFilter.HEADER)).isNotBlank();
  }

  @Test
  void shouldGenerateRequestIdWhenHeaderIsBlank() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.addHeader(RequestIdFilter.HEADER, "   ");

    filter.doFilter(request, response, (req, res) -> { });

    assertThat(response.getHeader(RequestIdFilter.HEADER))
        .isNotBlank()
        .isNotEqualTo("   ");
  }
}

