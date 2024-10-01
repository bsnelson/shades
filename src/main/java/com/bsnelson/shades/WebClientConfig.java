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

    WebClientConfig(
            WebClient.Builder webClientBuilder
    ) {
        this.webClientBuilder = webClientBuilder;
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
                .build();
    }
}

