package com.bsnelson.shades;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@RestController
@Slf4j
public class ListController {
    private ShadesService shadesService;

    @GetMapping(
        value = "/listDevices",
        produces = {MediaType.APPLICATION_JSON_VALUE})
    public Mono<String> listDevices() {
        return shadesService.getList();
    }
}
