package com.bsnelson.shades;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class WebClientConfig {
    private final WebClient.Builder webClientBuilder;
    private final ApiConfiguration apiConfiguration;

    WebClientConfig(
            WebClient.Builder webClientBuilder,
            ApiConfiguration apiConfiguration
    ) {
        this.webClientBuilder = webClientBuilder;
        this.apiConfiguration = apiConfiguration;
        var provider =
                ConnectionProvider.builder("shadeConnectionClient")
                        .maxConnections(20)
                        .maxIdleTime(Duration.ofSeconds(300))
                        .build();
        HttpClient client = HttpClient.create(provider).compress(false);
        webClientBuilder.clientConnector(new ReactorClientHttpConnector(client));
        webClientBuilder.defaultHeaders(headers -> headers.setContentType(MediaType.APPLICATION_JSON));
    }

    @Bean
    public WebClient shadesWebClient() {
        return webClientBuilder
                .clone()
                .baseUrl(apiConfiguration.getConnectIpAddress())
                .build();
    }
}

