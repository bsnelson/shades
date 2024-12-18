package com.bsnelson.shades.client;

import com.bsnelson.shades.config.Device;
import com.bsnelson.shades.config.SunsaConfiguration;
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
public class SunsaShadesClient {
    private WebClient shadesWebClient;
    private SunsaConfiguration sunsaConfiguration;

    public ListDevicesResponse getDeviceList() {
        log.debug("In listDevices");
        String uri = UriComponentsBuilder.fromUriString(sunsaConfiguration.getSunsaBaseUrl())
                .path(sunsaConfiguration.getListDevices().getPath())
                .build()
                .toString();
        return (ListDevicesResponse) callClient(uri, ListDevicesResponse.class);
    }

    public DeviceResponse setShadePosition(Device device, String position) {
        log.debug("In setPosition");
        String uri = UriComponentsBuilder.fromUriString(sunsaConfiguration.getSunsaBaseUrl())
                .path(sunsaConfiguration.getSetShadePosition().getPath())
                .buildAndExpand(device.getMac(), position)
                .toString();
        return (DeviceResponse) callClient(uri, DeviceResponse.class);
    }

    private <T> IResponse callClient(String url, Class<T> clazz) {
        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
        final RestTemplate restTemplate = restTemplateBuilder
                .errorHandler(new RestTemplateResponseErrorHandler())
                .build();
        return (IResponse) restTemplate.getForObject(url, clazz);
    }
}
