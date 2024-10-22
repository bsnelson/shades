package com.bsnelson.shades.controller;

import com.bsnelson.shades.models.CloseAllResponse;
import com.bsnelson.shades.models.DevicesResponse;
import com.bsnelson.shades.models.DurableOperationResponse;
import com.bsnelson.shades.models.ListDevicesResponse;
import com.bsnelson.shades.service.ShadesService;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;

import java.util.List;

@AllArgsConstructor
@RestController
@Slf4j
public class ShadesController {
    private ShadesService shadesService;

    @GetMapping(
        value = "/listDevices",
        produces = {MediaType.APPLICATION_JSON_VALUE})
    public ListDevicesResponse listDevices() {
        return shadesService.getList();
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
            value = "/setPositions/{position}",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public DevicesResponse setPositions(
            @PathVariable("position")
            String position) {
        log.debug("Entering setPositions service");
        DevicesResponse result = shadesService.setPositions(position);
        log.debug("Finished setPositions service");
        return result;
    }

    @GetMapping(
        value = "/close",
        produces = {MediaType.APPLICATION_JSON_VALUE})
    public CloseAllResponse closeAllShades() {
        log.debug("Entering closeAll service");
        CloseAllResponse result = shadesService.closeAllShades();
        log.debug("Finished closeAll service");
        return result;
    }

    @GetMapping(
        value = "/closeOld",
        produces = {MediaType.APPLICATION_JSON_VALUE})
    public DevicesResponse close() {
        log.debug("Entering close service");
        DevicesResponse result = shadesService.setPositions("100");
        log.debug("Finished close service");
        return result;
    }

    @GetMapping(
            value = "/openSeasonal",
            produces = {MediaType.APPLICATION_JSON_VALUE})
    public DevicesResponse openSeasonal() {
        log.debug("Entering openSeasonal service");
        DevicesResponse result = shadesService.openSeasonal();
        log.debug("Finished openSeasonal service");
        return result;
    }

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
