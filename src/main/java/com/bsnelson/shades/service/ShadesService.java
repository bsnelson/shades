package com.bsnelson.shades.service;

import com.bsnelson.shades.client.ShadesClient;
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
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
@Configuration
@Slf4j
public class ShadesService {
    public static final String ERROR = "error";
    private final ShadesClient shadesClient;
    private final DeviceConfiguration deviceConfiguration;
    private final RetryConfiguration retryConfiguration;

    public ListDevicesResponse getList() {
        return shadesClient.getDeviceList();
    }

    public CloseAllResponse closeAllShades() {
        return shadesClient.closeAllShades();
    }

    public DevicesResponse getStates() {
        List<CompletableFuture<DeviceResponse>> futures = deviceConfiguration.getDevices().stream()
                .map(device -> CompletableFuture.supplyAsync(() -> shadesClient.getShadeState(device)))
                .toList();
        DevicesResponse response = new DevicesResponse(futures.stream()
            .map(CompletableFuture::join) // This waits for each future to complete
            .toList());
        log.debug("Response is: " + response);
        return response;
    }

    public DevicesResponse setPositions(String position) {
        List<CompletableFuture<DeviceResponse>> futures = deviceConfiguration.getDevices().stream()
            .map(device -> CompletableFuture.supplyAsync(() -> shadesClient.setShadePosition(device, position)))
            .toList();
        DevicesResponse response = new DevicesResponse(futures.stream()
            .map(CompletableFuture::join) // This waits for each future to complete
            .toList());
        log.debug("Response is: " + response);
        return response;
    }

    public DevicesResponse openSeasonal() {
        List<CompletableFuture<DeviceResponse>> futures = deviceConfiguration.getDevices().stream()
            .map(device -> CompletableFuture.supplyAsync(() -> shadesClient.setShadePosition(device, device.getSeasonalDefault())))
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
        int retryable = retryConfiguration.getRetries();
        DurableOperationResponse durableResponse = new DurableOperationResponse();
        durableResponse.setRetries(0);
        durableResponse.setResult(ERROR);
        durableResponse.setFailedDevices(deviceConfiguration.getDevices().stream().map(Device::getName).collect(Collectors.toList()));
        while(retryable > 0 && Objects.equals(durableResponse.getResult(), ERROR)) {
            List<CompletableFuture<DeviceResponse>> futures = mapNamesToDevices(durableResponse.getFailedDevices()).stream()
                .map(device -> CompletableFuture.supplyAsync(() -> shadesClient.setShadePosition(device, (useSeasonal ? device.getSeasonalDefault() : position))))
                .toList();
            DevicesResponse response = new DevicesResponse(futures.stream()
                .map(CompletableFuture::join) // This waits for each future to complete
                .toList());
            List<String> failedDevices = response.getResponses().stream()
                .filter(deviceResponse -> ERROR.equals(deviceResponse.getResult()))
                .map(DeviceResponse::getMac)
                .collect(Collectors.toList());

            if (failedDevices.isEmpty()) {
                durableResponse.setResult("success");
                durableResponse.setFailedDevices(null);
            } else {
                durableResponse.setResult(ERROR);
                durableResponse.setRetries(durableResponse.getRetries() + 1);
                durableResponse.setFailedDevices(mapMacsToNames(failedDevices));
                try {
                    Thread.sleep(1000L * durableResponse.getRetries());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return durableResponse;
    }

    private List<String> mapMacsToNames(List<String> failedMacs) {
        return failedMacs.stream()
            .map(mac -> {
                Device matchingResponse = deviceConfiguration.getDevices().stream()
                    .filter(response -> response.getMac().equals(mac))
                    .findFirst()
                    .orElse(null);
                return matchingResponse.getName();
            })
            .collect(Collectors.toList());
    }

    private List<Device> mapNamesToDevices(List<String> names) {
        return names.stream()
            .map(name -> deviceConfiguration.getDevices().stream()
                .filter(response -> response.getName().equals(name))
                .findFirst()
                .orElse(null))
            .collect(Collectors.toList());
    }
}
