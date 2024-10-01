package com.bsnelson.shades;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@Component
@Slf4j
public class ShadesClient {
    private WebClient shadesWebClient;
    private ApiConfiguration apiConfiguration;

    public Mono<String> getShadeResponse(String id, MultiValueMap<String, String> options) {
        return shadesWebClient
            .get()
            .uri(
                 uriBuilder ->
                    uriBuilder
                        .path(apiConfiguration.getShade().getPath())
                        .queryParams(options)
                        .build(id))
                .retrieve()
                .bodyToMono(String.class);
    }
}
