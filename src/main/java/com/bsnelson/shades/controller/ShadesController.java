package com.bsnelson.shades.controller;

import com.bsnelson.shades.models.CloseAllResponse;
import com.bsnelson.shades.models.DevicesResponse;
import com.bsnelson.shades.models.ListDevicesResponse;
import com.bsnelson.shades.service.ShadesService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import reactor.core.publisher.Mono;

import java.util.List;

@AllArgsConstructor
@RestController
@Slf4j
public class ShadesController {
    private ShadesService shadesService;

    @GetMapping(
        value = "/listDevices",
        produces = {MediaType.APPLICATION_JSON_VALUE})
    public Mono<ListDevicesResponse> listDevices() {
        Mono<ListDevicesResponse> resp = shadesService.getList();
        resp.subscribe(
            value -> System.out.println("QUESO listDevices nbr: " + value.getShades().size())
        );
        return resp
            .map(response -> {
                response.getShades().forEach(shade -> shade.setType("newType"));
                return response;
            });
    }

    @GetMapping(
            value = "/getStates",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public Mono<DevicesResponse> getStates() {
        log.debug("Entering getStates service");
        Mono<DevicesResponse> result = shadesService.getStates();
        log.debug("Finished getStates service");
        return result;
    }

    @GetMapping(
            value = "/setPositions/{position}",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public Mono<DevicesResponse> setPositions(
            @PathVariable("position")
            String position) {
        log.debug("Entering setPositions service");
        Mono<DevicesResponse> result = shadesService.setPositions(position);
        log.debug("Finished setPositions service");
        return result;
    }

    @GetMapping(
        value = "/close",
        produces = {MediaType.APPLICATION_JSON_VALUE})
    public Mono<CloseAllResponse> closeAllShades() {
        log.debug("Entering closeAll service");
        Mono<CloseAllResponse> result = shadesService.closeAllShades();
        log.debug("Finished closeAll service");
        return result;
    }

    @GetMapping(
        value = "/closeOld",
        produces = {MediaType.APPLICATION_JSON_VALUE})
    public Mono<DevicesResponse> close() {
        log.debug("Entering close service");
        Mono<DevicesResponse> result = shadesService.setPositions("100");
        log.debug("Finished close service");
        return result;
    }

    @GetMapping(
            value = "/openSeasonal",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public Mono<DevicesResponse> openSeasonal() {
        log.debug("Entering openSeasonal service");
        Mono<DevicesResponse> result = shadesService.openSeasonal();
        log.debug("Finished openSeasonal service");
        return result;
    }

    @GetMapping(
        value = "/reopen",
        produces = {MediaType.APPLICATION_JSON_VALUE})
    public Mono<DevicesResponse> reopen() {
        log.debug("Entering reopen service");
        Mono<DevicesResponse> result = shadesService.reopen();
        log.debug("Finished reopen service");
        return result;
    }
}
