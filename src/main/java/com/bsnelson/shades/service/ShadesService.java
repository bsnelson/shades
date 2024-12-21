package com.bsnelson.shades.service;

import com.bsnelson.shades.client.SomaShadesClient;
import com.bsnelson.shades.client.SunsaShadesClient;
import com.bsnelson.shades.config.Device;
import com.bsnelson.shades.config.DeviceConfiguration;
import com.bsnelson.shades.config.RetryConfiguration;
import com.bsnelson.shades.models.*;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import static java.util.stream.Collectors.toList;

@AllArgsConstructor
@Service
@Configuration
@Slf4j
public class ShadesService {
    public static final String ERROR = "error";
    private final SomaShadesClient somaShadesClient;
    private final SunsaShadesClient sunsaShadesClient;
    private final DeviceConfiguration deviceConfiguration;
    private final RetryConfiguration retryConfiguration;

    public ListDevicesResponse getList() {
        return somaShadesClient.getDeviceList();
    }

    public CloseAllResponse closeAllShades() {
        return somaShadesClient.closeAllShades();
    }

    public DevicesResponse getStates() {
        List<CompletableFuture<SomaDeviceResponse>> futures = deviceConfiguration.getDevices().stream()
                .map(device -> CompletableFuture.supplyAsync(() -> somaShadesClient.getShadeState(device)))
                .toList();
        DevicesResponse response = new DevicesResponse(futures.stream()
            .map(CompletableFuture::join) // This waits for each future to complete
            .toList());
        log.debug("Response is: " + response);
        return response;
    }

    public DevicesResponse setPositions(String position) {
        List<CompletableFuture<SomaDeviceResponse>> futures = deviceConfiguration.getDevices().stream()
            .map(device -> CompletableFuture.supplyAsync(() -> somaShadesClient.setShadePosition(device, position)))
            .toList();
        DevicesResponse response = new DevicesResponse(futures.stream()
            .map(CompletableFuture::join) // This waits for each future to complete
            .toList());
        log.debug("Response is: " + response);
        return response;
    }

    public DevicesResponse openSeasonal() {
        List<CompletableFuture<SomaDeviceResponse>> futures = deviceConfiguration.getDevices().stream()
            .map(device -> CompletableFuture.supplyAsync(() -> somaShadesClient.setShadePosition(device, device.getSeasonalDefault())))
            .toList();
        DevicesResponse response = new DevicesResponse(futures.stream()
            .map(CompletableFuture::join) // This waits for each future to complete
            .toList());
        log.debug("Response is: " + response);
        return response;
    }

    public DurableOperationResponse reopen() {
        return durablePosition(true, "");
    }

    public DurableOperationResponse reclose() {
        return durablePosition(false, "100");
    }
    
    public DurableOperationResponse durablePosition(boolean useSeasonal, String position) {
        int retriable = retryConfiguration.getRetries();
        DurableOperationResponse durableResponse = new DurableOperationResponse();
        durableResponse.setRetries(0);
        durableResponse.setResult(ERROR);
        durableResponse.setFailedDevices(deviceConfiguration.getDevices().stream().map(Device::getName).collect(toList()));
        while(retriable >= 0 && Objects.equals(durableResponse.getResult(), ERROR)) {
            List<CompletableFuture<IResponse>> futures = mapNamesToDevices(durableResponse.getFailedDevices()).stream()
                .map(device -> CompletableFuture.supplyAsync(() -> {
                    if (device.getType().equals("sunsa")) {
                        return sunsaShadesClient.setShadePosition(device, (useSeasonal ? device.getSeasonalDefault() : position);
                    } else if (device.getType().equals("soma")) {
                        return somaShadesClient.setShadePosition(device, (useSeasonal ? device.getSeasonalDefault() : position);
                    }})
                .toList();
            DevicesResponse response = new DevicesResponse(futures.stream()
                .map(CompletableFuture::join) // This waits for each future to complete
                .toList());
            List<String> failedDevices = response.getResponses().stream()
                .filter(somaDeviceResponse -> ERROR.equals(somaDeviceResponse.getResult()))
                .map(SomaDeviceResponse::getId)
                .collect(toList());

            if (failedDevices.isEmpty()) {
                durableResponse.setResult("success");
                durableResponse.setFailedDevices(null);
            } else {
                durableResponse.setResult(ERROR);
                durableResponse.setFailedDevices(mapIdsToNames(failedDevices));
                log.info("Failed, retries {}, retriable {}", retryConfiguration.getRetries(), retriable);
                if (--retriable >=
                        0) {
                    durableResponse.setRetries(durableResponse.getRetries() + 1);
                    try {
                        Thread.sleep(1000L * durableResponse.getRetries());
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return durableResponse;
    }

    private List<String> mapIdsToNames(List<String> failedIds) {
        return failedIds.stream()
            .map(id -> {
                Device matchingResponse = deviceConfiguration.getDevices().stream()
                    .filter(response -> response.getId().equals(id))
                    .findFirst()
                    .orElse(null);
                return matchingResponse.getName();
            })
            .collect(toList());
    }

    private List<Device> mapNamesToDevices(List<String> names) {
        return names.stream()
            .map(name -> deviceConfiguration.getDevices().stream()
                .filter(response -> response.getName().equals(name))
                .findFirst()
                .orElse(null))
            .collect(toList());
    }
}
