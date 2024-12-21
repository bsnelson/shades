package com.bsnelson.shades.client;

import com.bsnelson.shades.config.Device;
import com.bsnelson.shades.config.SunsaConfiguration;
import com.bsnelson.shades.exception.RestTemplateResponseErrorHandler;
import com.bsnelson.shades.models.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
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
        return (ListDevicesResponse) clientGet(uri, ListDevicesResponse.class);
    }

    public SunsaDeviceResponse setShadePosition(Device device, String position) {
        log.debug("In setPosition");
        String uri = UriComponentsBuilder.fromUriString(sunsaConfiguration.getSunsaBaseUrl())
                .path(sunsaConfiguration.getSetShadePosition().getPath())
                .buildAndExpand(device.getId(), position)
                .toString();
        String template = "{ \"Position\": %s }";
        String body = String.format(template, position);
        return (SunsaDeviceResponse) clientPut(uri, body);
    }

    private <T> IResponse clientGet(String url, Class<T> clazz) {
        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
        final RestTemplate restTemplate = restTemplateBuilder
                .errorHandler(new RestTemplateResponseErrorHandler())
                .build();
        return (IResponse) restTemplate.getForObject(url, clazz);
    }

    private <T> IResponse clientPut(String url, String body) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);
        ResponseEntity<SunsaDeviceResponse> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                requestEntity,
                SunsaDeviceResponse.class);
        return response.getBody();
    }
}
