package com.bsnelson.shades;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@RestController
@Slf4j
public class StateController {
    private ShadesService shadesService;

    @GetMapping(
        value = "/getStates",
        produces = {MediaType.APPLICATION_JSON_VALUE})
    public Mono<String> getStates() {
        return shadesService.getStates();
    }
}
