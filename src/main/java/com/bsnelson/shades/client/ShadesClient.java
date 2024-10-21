package com.bsnelson.shades.client;

import com.bsnelson.shades.config.ApiConfiguration;
import com.bsnelson.shades.config.Device;
import com.bsnelson.shades.models.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@AllArgsConstructor
@Component
@Slf4j
public class ShadesClient {
    private WebClient shadesWebClient;
    private ApiConfiguration apiConfiguration;

    public ListDevicesResponse getDeviceList() {
        log.debug("In listDevices");
        String uri = UriComponentsBuilder.fromUriString(apiConfiguration.getConnectIpAddress())
            .path(apiConfiguration.getListDevices().getPath())
            .build()
            //  .buildAndExpand(URLEncoder.encode(device.getMac(), StandardCharsets.UTF_8))
            .toString();
        return (ListDevicesResponse) callClient(uri, ListDevicesResponse.class);
    }
//
//    public Mono<CloseAllResponse> closeAllShades() {
//        return shadesWebClient
//            .get()
//            .uri(
//                uriBuilder ->
//                    uriBuilder
//                        .path(apiConfiguration.getCloseAllShades().getPath())
//                        .build())
//            .retrieve()
//            .bodyToMono(CloseAllResponse.class);
//    }

    public DeviceResponse getShadeState(Device device) {
        log.debug("In getStates");
        String uri = UriComponentsBuilder.fromUriString(apiConfiguration.getConnectIpAddress())
            .path(apiConfiguration.getGetShadeState().getPath())
            .buildAndExpand(device.getMac())
          //  .buildAndExpand(URLEncoder.encode(device.getMac(), StandardCharsets.UTF_8))
            .toString();
        return (DeviceResponse) callClient(uri, DeviceResponse.class);
    }

//    public Mono<DeviceResponse> setShadePosition(String mac, String position) {
//        log.debug("In setState(" + mac + ")");
//        Mono<DeviceResponse> result = shadesWebClient
//                .get()
//                .uri(
//                    uriBuilder ->
//                        uriBuilder
//                            .path(apiConfiguration.getSetShadePosition().getPath())
//                            .queryParam("close_upwards", "1")
//                            .build(mac,position))
//                .retrieve()
//                .bodyToMono(DeviceResponse.class);
//        log.debug("Return setPosition(" + mac + ")");
//        return result;
//    }


    private <T> IResponse callClient(String url, Class<T> clazz) {
        final RestTemplate restTemplate = new RestTemplate();
        //restTemplate.getMessageConverters().addFirst(new StringHttpMessageConverter(StandardCharsets.UTF_8));
        return (IResponse) restTemplate.getForObject(url, clazz);
    }
}
