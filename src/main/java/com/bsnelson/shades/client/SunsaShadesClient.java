package com.bsnelson.shades.client;

import com.bsnelson.shades.config.Device;
import com.bsnelson.shades.config.SunsaConfiguration;
import com.bsnelson.shades.exception.RestTemplateResponseErrorHandler;
import com.bsnelson.shades.models.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
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

import java.util.Optional;

@AllArgsConstructor
@Component
@Slf4j
public class SunsaShadesClient {
    private WebClient shadesWebClient;
    private SunsaConfiguration sunsaConfiguration;

    public SunsaListResponse getDeviceList() {
        log.debug("In listDevices");
        String uri = UriComponentsBuilder.fromUriString(sunsaConfiguration.getSunsaBaseUrl())
                .path(sunsaConfiguration.getListDevices().getPath())
                .queryParam("publicApiKey", sunsaConfiguration.getApiKey())
                .buildAndExpand(sunsaConfiguration.getIdUser())
                .toString();
        return (SunsaListResponse) clientGet(uri, SunsaListResponse.class);
    }

    public SunsaListDeviceResponse getShadeState(Device device) {
        log.debug("In getShadeState");
        SunsaListResponse devicesResponse = this.getDeviceList();
        Optional<SunsaListDeviceResponse> match = devicesResponse.getDevices().stream()
                .filter(sunsaDeviceResponse -> ((Integer) sunsaDeviceResponse.getIdDevice()).toString().equals(device.getId()))
                .findFirst();
        return match.orElse(null);
    }

    public SunsaPutDeviceResponse setShadePosition(Device device, String position) {
        log.debug("In setPosition");
        String uri = UriComponentsBuilder.fromUriString(sunsaConfiguration.getSunsaBaseUrl())
                .path(sunsaConfiguration.getSetShadePosition().getPath())
                .buildAndExpand(sunsaConfiguration.getIdUser(), device.getId(), sunsaConfiguration.getApiKey())
                .toString();
        String template = "{ \"Position\": %s }";
        String body = String.format(template, position);
        return (SunsaPutDeviceResponse) clientPut(uri, body);
    }

    private <T> IResponse clientGet(String url, Class<T> clazz) {
        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
        final RestTemplate restTemplate = restTemplateBuilder
                .errorHandler(new RestTemplateResponseErrorHandler())
                .build();
        return (IResponse) restTemplate.getForObject(url, clazz);
    }

    @SneakyThrows
    private <T> IResponse clientPut(String url, String body) {
        RestTemplateBuilder restTemplateBuilder = new RestTemplateBuilder();
        RestTemplate restTemplate = restTemplateBuilder
                .errorHandler(new RestTemplateResponseErrorHandler())
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<String> requestEntity = new HttpEntity<>(body, headers);

        log.debug("Sending PUT request to URL: {}", url);
        log.debug("Request body: {}", body);

        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.PUT,
                requestEntity,
                String.class);

        log.debug("Response status code: {}", response.getStatusCode());
        log.debug("Raw response body: {}", response.getBody());

        ObjectMapper objectMapper = new ObjectMapper();
        SunsaPutDeviceResponse deviceResponse = objectMapper.readValue(response.getBody(), SunsaPutDeviceResponse.class);

        return deviceResponse;
    }}
