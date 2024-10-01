package com.bsnelson.shades.service;

import com.bsnelson.shades.client.ShadesClient;
import com.bsnelson.shades.config.DeviceConfiguration;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@AllArgsConstructor
@Service
@Configuration
@Slf4j
public class ShadesService {
    private final ShadesClient shadesClient;
    private final DeviceConfiguration deviceConfiguration;

    public Mono<String> getList() {
        return shadesClient.getDeviceList();
    }

    public Mono<String> getStates() {
        return Mono.zip(
                shadesClient
                        .getShadeState(deviceConfiguration.getDevices().get(0).getMac())
                        .subscribeOn(Schedulers.boundedElastic())
                        .onErrorComplete(),
                shadesClient
                        .getShadeState(deviceConfiguration.getDevices().get(1).getMac())
                        .subscribeOn(Schedulers.boundedElastic())
                        .onErrorComplete(),
                shadesClient
                        .getShadeState(deviceConfiguration.getDevices().get(2).getMac())
                        .subscribeOn(Schedulers.boundedElastic())
                        .onErrorComplete())
                .flatMap(
                        tuple -> {
                            String ret1 = tuple.getT1();
                            String ret2 = tuple.getT2();
                            String ret3 = tuple.getT3();
                            return Mono.just("{ \"responses\": [ " + ret1 + "," + ret2 + "," + ret3 + "] }");
                        }
                );
    }
}
