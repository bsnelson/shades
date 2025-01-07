package com.bsnelson.shades.client;

import com.bsnelson.shades.config.Device;
import com.bsnelson.shades.config.SomaConfiguration;
import com.bsnelson.shades.exception.RestTemplateResponseErrorHandler;
import com.bsnelson.shades.models.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

@AllArgsConstructor
@Component
@Slf4j
public class SomaShadesClient {
    private SomaConfiguration somaConfiguration;

    public SomaListResponse getDeviceList() {
        log.debug("In listDevices");
        String uri = UriComponentsBuilder.fromUriString(somaConfiguration.getConnectIpAddress())
            .path(somaConfiguration.getListDevices().getPath())
            .build()
            .toString();
        return (SomaListResponse) callClient(uri, SomaListResponse.class);
    }

    public CloseAllResponse closeAllShades() {
        log.debug("In closeAll");
        String uri = UriComponentsBuilder.fromUriString(somaConfiguration.getConnectIpAddress())
            .path(somaConfiguration.getCloseAllShades().getPath())
            .build()
            .toString();
        return (CloseAllResponse) callClient(uri, CloseAllResponse.class);
    }

    public SomaDeviceResponse getShadeState(Device device) {
        log.debug("In getStates");
        String uri = UriComponentsBuilder.fromUriString(somaConfiguration.getConnectIpAddress())
            .path(somaConfiguration.getGetShadeState().getPath())
            .buildAndExpand(device.getId())
            .toString();
        return (SomaDeviceResponse) callClient(uri, SomaDeviceResponse.class);
    }

    public SomaDeviceResponse setShadePosition(Device device, String position) {
        log.debug("In setPosition");
        String uri = UriComponentsBuilder.fromUriString(somaConfiguration.getConnectIpAddress())
            .path(somaConfiguration.getSetShadePosition().getPath())
            .buildAndExpand(device.getId(), position)
            .toString();
        return (SomaDeviceResponse) callClient(uri, SomaDeviceResponse.class);
    }

    private <T> IResponse callClient(String url, Class<T> clazz) {
        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
        final RestTemplate restTemplate = restTemplateBuilder
                .errorHandler(new RestTemplateResponseErrorHandler())
                .build();
        return (IResponse) restTemplate.getForObject(url, clazz);
    }
}
