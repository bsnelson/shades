package com.bsnelson.shades.controller;

import com.bsnelson.shades.service.ShadesService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@RestController
@Slf4j
public class GetStateController {
    private ShadesService shadesService;

    @GetMapping(
        value = "/getStates",
        produces = {MediaType.APPLICATION_JSON_VALUE})
    public Mono<String> getStates() {
        log.debug("Entering getStates service");
        Mono<String> result = shadesService.getStates();
        log.debug("Finished getStates service");
        return result;
    }
}
