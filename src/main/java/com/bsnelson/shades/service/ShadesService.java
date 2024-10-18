package com.bsnelson.shades.service;

import com.bsnelson.shades.client.ShadesClient;
import com.bsnelson.shades.config.DeviceConfiguration;
import com.bsnelson.shades.models.CloseAllResponse;
import com.bsnelson.shades.models.DeviceResponse;
import com.bsnelson.shades.models.DevicesResponse;
import com.bsnelson.shades.models.ListDevicesResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
@Service
@Configuration
@Slf4j
public class ShadesService {
    private final ShadesClient shadesClient;
    private final DeviceConfiguration deviceConfiguration;

    public Mono<ListDevicesResponse> getList() {
        return shadesClient.getDeviceList();
    }

    public Mono<CloseAllResponse> closeAllShades() {
        return shadesClient.closeAllShades();
    }
    public Mono<DevicesResponse> getStates() {
        // Create a list of Monos dynamically from the device list
        List<Mono<DeviceResponse>> shadeMonos = deviceConfiguration.getDevices().stream()
                .map(device -> shadesClient.getShadeState(device.getMac())
                        .subscribeOn(Schedulers.boundedElastic())
                        .onErrorComplete())
                .toList();
        return getMonos(shadeMonos);
    }
    public Mono<DevicesResponse> setPositions(String position) {
        // Create a list of Monos dynamically from the device list
        List<Mono<DeviceResponse>> shadeMonos = deviceConfiguration.getDevices().stream()
                .map(device -> shadesClient.setShadePosition(device.getMac(), position)
                        .subscribeOn(Schedulers.boundedElastic())
                        .onErrorComplete())
                .toList();
        return getMonos(shadeMonos);
    }

    public Mono<DevicesResponse> openSeasonal() {
        // Create a list of Monos dynamically from the device list
        List<Mono<DeviceResponse>> shadeMonos = deviceConfiguration.getDevices().stream()
                .map(device -> shadesClient.setShadePosition(device.getMac(), device.getSeasonalDefault())
                        .subscribeOn(Schedulers.boundedElastic())
                        .onErrorComplete())
                .toList();
        return getMonos(shadeMonos);
    }

    public Mono<DevicesResponse> reopen() {
        // Create a list of Monos dynamically from the device list
        System.out.println("Starting reopen");
        List<Mono<DeviceResponse>> shadeMonos = deviceConfiguration.getDevices().stream()
            .map(device -> shadesClient.getShadeState(device.getMac())
                .subscribeOn(Schedulers.boundedElastic())
                .onErrorComplete())
            .toList();
        Mono<DevicesResponse> response = getMonos(shadeMonos).cache();
        response.subscribe(
            value -> value.getResponses()
                .forEach(i -> System.out.println(i.getMac() + " value is " + i.getPosition()))
        );
        return response;
    }

    private Mono<DevicesResponse> getMonos(List<Mono<DeviceResponse>> shadeMonos) {
        @SuppressWarnings("unchecked")
        Mono<DeviceResponse>[] monoArray = new Mono[shadeMonos.size()];
        monoArray = shadeMonos.toArray(monoArray);
        System.out.println("Hit get with " + shadeMonos.size());
        return Mono.zip(
            responses -> {
                List<DeviceResponse> deviceResponses = Stream.of(responses)
                    .map(response -> (DeviceResponse) response)
                    .collect(Collectors.toList());
                return new DevicesResponse(deviceResponses);
            },
            monoArray
        );
    }}
