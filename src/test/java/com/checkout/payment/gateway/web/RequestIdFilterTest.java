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
    request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "abc-123");
    FilterChain chain = (req, res) ->
        assertThat(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY)).isEqualTo("abc-123");

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).isEqualTo("abc-123");
    assertThat(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY)).isNull();
  }

  @Test
  void shouldGenerateRequestIdWhenHeaderIsAbsent() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = (req, res) ->
        assertThat(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY)).isNotBlank();

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).isNotBlank();
  }

  @Test
  void shouldGenerateRequestIdWhenHeaderIsBlank() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "   ");

    filter.doFilter(request, response, (req, res) -> { });

    assertThat(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER))
        .isNotBlank()
        .isNotEqualTo("   ");
  }

  @Test
  void shouldEchoIncomingCorrelationIdHeader() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.addHeader(RequestIdFilter.CORRELATION_ID_HEADER, "journey-42");
    FilterChain chain = (req, res) ->
        assertThat(MDC.get(RequestIdFilter.CORRELATION_ID_MDC_KEY)).isEqualTo("journey-42");

    filter.doFilter(request, response, chain);

    assertThat(response.getHeader(RequestIdFilter.CORRELATION_ID_HEADER)).isEqualTo("journey-42");
    assertThat(MDC.get(RequestIdFilter.CORRELATION_ID_MDC_KEY)).isNull();
  }

  @Test
  void shouldFallBackCorrelationIdToRequestIdWhenAbsent() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();
    request.addHeader(RequestIdFilter.REQUEST_ID_HEADER, "req-7");

    filter.doFilter(request, response, (req, res) -> { });

    assertThat(response.getHeader(RequestIdFilter.REQUEST_ID_HEADER)).isEqualTo("req-7");
    assertThat(response.getHeader(RequestIdFilter.CORRELATION_ID_HEADER)).isEqualTo("req-7");
  }

  @Test
  void shouldGenerateBothIdsWhenNeitherHeaderIsProvided() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilter(request, response, (req, res) -> { });

    String requestId = response.getHeader(RequestIdFilter.REQUEST_ID_HEADER);
    String correlationId = response.getHeader(RequestIdFilter.CORRELATION_ID_HEADER);
    assertThat(requestId).isNotBlank();
    assertThat(correlationId).isNotBlank().isEqualTo(requestId);
  }
}
