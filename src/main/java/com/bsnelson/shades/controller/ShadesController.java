package com.bsnelson.shades.controller;

import com.bsnelson.shades.service.ShadesService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@RestController
@Slf4j
public class ShadesController {
    private ShadesService shadesService;

    @GetMapping(
        value = "/listDevices",
        produces = {MediaType.APPLICATION_JSON_VALUE})
    public Mono<String> listDevices() {
        return shadesService.getList();
    }

    @GetMapping(
            value = "/getStates",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public Mono<String> getStates() {
        log.debug("Entering getStates service");
        Mono<String> result = shadesService.getStates();
        log.debug("Finished getStates service");
        return result;
    }

    @GetMapping(
            value = "/setPositions/{position}",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public Mono<String> setPositions(
            @PathVariable("position")
            String position) {
        log.debug("Entering setPositions service");
        Mono<String> result = shadesService.setPositions(position);
        log.debug("Finished setPositions service");
        return result;
    }

    @GetMapping(
            value = "/close",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public Mono<String> close() {
        log.debug("Entering close service");
        Mono<String> result = shadesService.setPositions("100");
        log.debug("Finished close service");
        return result;
    }

    @GetMapping(
            value = "/openSeasonal",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public Mono<String> openSeasonal() {
        log.debug("Entering openSeasonal service");
        Mono<String> result = shadesService.openSeasonal();
        log.debug("Finished openSeasonal service");
        return result;
    }
}
