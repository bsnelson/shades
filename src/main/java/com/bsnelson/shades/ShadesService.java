package com.bsnelson.shades;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@Service
@Slf4j
public class ShadesService {
    private ShadesClient shadesClient;

    public Mono<String> getList() {
        return shadesClient.getDeviceList();
    }
}
