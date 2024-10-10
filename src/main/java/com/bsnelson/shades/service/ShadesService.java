package com.bsnelson.shades.service;

import com.bsnelson.shades.client.ShadesClient;
import com.bsnelson.shades.config.DeviceConfiguration;
import com.bsnelson.shades.models.ListDevicesResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

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

    public Mono<String> getStates() {
        // Create a list of Monos dynamically from the device list
        List<Mono<String>> shadeMonos = deviceConfiguration.getDevices().stream()
                .map(device -> shadesClient.getShadeState(device.getMac())
                        .subscribeOn(Schedulers.boundedElastic())
                        .onErrorComplete())
                .toList();
        return getMonos(shadeMonos);
    }
    public Mono<String> setPositions(String position) {
        // Create a list of Monos dynamically from the device list
        List<Mono<String>> shadeMonos = deviceConfiguration.getDevices().stream()
                .map(device -> shadesClient.setShadePosition(device.getMac(), position)
                        .subscribeOn(Schedulers.boundedElastic())
                        .onErrorComplete())
                .toList();
        return getMonos(shadeMonos);
    }

    public Mono<String> openSeasonal() {
        // Create a list of Monos dynamically from the device list
        List<Mono<String>> shadeMonos = deviceConfiguration.getDevices().stream()
                .map(device -> shadesClient.setShadePosition(device.getMac(), device.getSeasonalDefault())
                        .subscribeOn(Schedulers.boundedElastic())
                        .onErrorComplete())
                .toList();
        return getMonos(shadeMonos);
    }

    private Mono<String> getMonos(List<Mono<String>> shadeMonos) {
        // Use Mono.zip to combine the Monos, converting the list to an array
        return Mono.zip(shadeMonos, responses -> {
            StringBuilder result = new StringBuilder("{ \"responses\": [");
            for (int i = 0; i < responses.length; i++) {
                log.debug("Got a response");
                result.append(responses[i]);
                if (i < responses.length - 1) {
                    result.append(", ");
                }
            }
            result.append("] }");
            return result.toString();
        });
    }
}
