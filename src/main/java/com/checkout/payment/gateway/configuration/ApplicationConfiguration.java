package com.checkout.payment.gateway.configuration;

import java.net.http.HttpClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(AcquiringBankProperties.class)
public class ApplicationConfiguration {

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
}


