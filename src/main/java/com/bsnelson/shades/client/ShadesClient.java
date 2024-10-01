package com.bsnelson.shades.client;

import com.bsnelson.shades.config.ApiConfiguration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@Component
@Slf4j
public class ShadesClient {
    private WebClient shadesWebClient;
    private ApiConfiguration apiConfiguration;

    public Mono<String> getDeviceList() {
        return shadesWebClient
                .get()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path(apiConfiguration.getListDevices().getPath())
                                        .build())
                .retrieve()
                .bodyToMono(String.class);
    }

    public Mono<String> getShadeState(String mac) {
        return shadesWebClient
            .get()
            .uri(
                 uriBuilder ->
                    uriBuilder
                        .path(apiConfiguration.getGetShadeState().getPath())
                        .build(mac))
                .retrieve()
                .bodyToMono(String.class);
    }
}
