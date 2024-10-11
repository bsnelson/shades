package com.bsnelson.shades.client;

import com.bsnelson.shades.config.ApiConfiguration;
import com.bsnelson.shades.models.CloseAllResponse;
import com.bsnelson.shades.models.DeviceResponse;
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

    public Mono<CloseAllResponse> closeAllShades() {
        return shadesWebClient
            .get()
            .uri(
                uriBuilder ->
                    uriBuilder
                        .path(apiConfiguration.getCloseAllShades().getPath())
                        .build())
            .retrieve()
            .bodyToMono(CloseAllResponse.class);
    }

    public Mono<DeviceResponse> getShadeState(String mac) {
        log.debug("In getState(" + mac + ")");
        Mono<DeviceResponse> result = shadesWebClient
            .get()
            .uri(
                 uriBuilder ->
                    uriBuilder
                        .path(apiConfiguration.getGetShadeState().getPath())
                        .build(mac))
                .retrieve()
                .bodyToMono(DeviceResponse.class);
        log.debug("Return getState(" + mac + ")");
        return result;
    }

    public Mono<DeviceResponse> setShadePosition(String mac, String position) {
        log.debug("In setState(" + mac + ")");
        Mono<DeviceResponse> result = shadesWebClient
                .get()
                .uri(
                    uriBuilder ->
                        uriBuilder
                            .path(apiConfiguration.getSetShadePosition().getPath())
                            .queryParam("close_upwards", "1")
                            .build(mac,position))
                .retrieve()
                .bodyToMono(DeviceResponse.class);
        log.debug("Return setPosition(" + mac + ")");
        return result;
    }

}
