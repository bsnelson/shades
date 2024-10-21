package com.bsnelson.shades.client;

import com.bsnelson.shades.config.ApiConfiguration;
import com.bsnelson.shades.config.Device;
import com.bsnelson.shades.models.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

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
            .toString();
        return (ListDevicesResponse) callClient(uri, ListDevicesResponse.class);
    }

    public CloseAllResponse closeAllShades() {
        log.debug("In closeAll");
        String uri = UriComponentsBuilder.fromUriString(apiConfiguration.getConnectIpAddress())
            .path(apiConfiguration.getCloseAllShades().getPath())
            .build()
            .toString();
        return (CloseAllResponse) callClient(uri, CloseAllResponse.class);
    }

    public DeviceResponse getShadeState(Device device) {
        log.debug("In getStates");
        String uri = UriComponentsBuilder.fromUriString(apiConfiguration.getConnectIpAddress())
            .path(apiConfiguration.getGetShadeState().getPath())
            .buildAndExpand(device.getMac())
            .toString();
        return (DeviceResponse) callClient(uri, DeviceResponse.class);
    }

    public DeviceResponse setShadePosition(Device device, String position) {
        log.debug("In setPosition");
        String uri = UriComponentsBuilder.fromUriString(apiConfiguration.getConnectIpAddress())
            .path(apiConfiguration.getSetShadePosition().getPath())
            .buildAndExpand(device.getMac(), position)
            .toString();
        return (DeviceResponse) callClient(uri, DeviceResponse.class);
    }

    private <T> IResponse callClient(String url, Class<T> clazz) {
        final RestTemplate restTemplate = new RestTemplate();
        return (IResponse) restTemplate.getForObject(url, clazz);
    }
}
