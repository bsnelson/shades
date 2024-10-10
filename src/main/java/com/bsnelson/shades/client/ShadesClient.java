package com.bsnelson.shades.client;

import com.bsnelson.shades.config.ApiConfiguration;
import com.bsnelson.shades.models.ListDevicesResponse;
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

    public Mono<ListDevicesResponse> getDeviceList() {
        return shadesWebClient
                .get()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path(apiConfiguration.getListDevices().getPath())
                                        .build())
                .retrieve()
                .bodyToMono(ListDevicesResponse.class);
    }

    public Mono<String> getShadeState(String mac) {
        log.debug("In getState(" + mac + ")");
        Mono<String> result = shadesWebClient
            .get()
            .uri(
                 uriBuilder ->
                    uriBuilder
                        .path(apiConfiguration.getGetShadeState().getPath())
                        .build(mac))
                .retrieve()
                .bodyToMono(String.class);
        log.debug("Return getState(" + mac + ")");
        return result;
    }

    public Mono<String> setShadePosition(String mac, String position) {
        log.debug("In setState(" + mac + ")");
        Mono<String> result = shadesWebClient
                .get()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path(apiConfiguration.getSetShadePosition().getPath())
                                        .queryParam("close_upwards", "1")
                                        .build(mac,position))
                .retrieve()
                .bodyToMono(String.class);
        log.debug("Return getState(" + mac + ")");
        return result;
    }

}
