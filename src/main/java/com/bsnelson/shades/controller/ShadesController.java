package com.bsnelson.shades.controller;

import com.bsnelson.shades.models.*;
import com.bsnelson.shades.service.ShadesService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;

import static org.springframework.web.bind.annotation.RequestMethod.*;

@AllArgsConstructor
@RestController
@Slf4j
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {GET, POST, PUT, DELETE, OPTIONS})
public class ShadesController {
    private ShadesService shadesService;

    @GetMapping(
        value = "/listDevices",
        produces = {MediaType.APPLICATION_JSON_VALUE})
    public DeviceListResponse listDevices() {
        return shadesService.getList();
    }

    @GetMapping(
            value = "/listNames",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public NamesResponse listNames() {
        return shadesService.listNames();
    }

    @GetMapping(
            value = "/getStates",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public DevicesResponse getStates() {
        log.debug("Entering getStates service");
        DevicesResponse result = shadesService.getStates();
        log.debug("Finished getStates service");
        return result;
    }

    @GetMapping(
            value = "/setPosition/{position}",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public DevicesResponse setPosition(
            @PathVariable("position") String position,
            @RequestParam(value = "group", required = false) String group,
            @RequestParam(value = "name", required = false) String name) {
        log.debug("Entering setPositions service with position: {}, group: {}, name: {}", position, group, name);
        DevicesResponse result = shadesService.setPosition(position, group, name);
        log.debug("Finished setPositions service");
        return result;
    }

//    @GetMapping(
//            value = "/openSeasonal",
//            produces = {MediaType.APPLICATION_JSON_VALUE})
//    public DevicesResponse openSeasonal() {
//        log.debug("Entering openSeasonal service");
//        DevicesResponse result = shadesService.openSeasonal();
//        log.debug("Finished openSeasonal service");
//        return result;
//    }
//
    @GetMapping(
        value = "/reopen",
        produces = {MediaType.APPLICATION_JSON_VALUE})
    public DurableOperationResponse reopen() {
        log.debug("Entering reopen service");
        DurableOperationResponse result = shadesService.reopen();
        log.debug("Finished reopen service");
        return result;
    }
    @GetMapping(
        value = "/reclose",
        produces = {MediaType.APPLICATION_JSON_VALUE})
    public DurableOperationResponse reclose() {
        log.debug("Entering reclose service");
        DurableOperationResponse result = shadesService.reclose();
        log.debug("Finished reclose service");
        return result;
    }
}
