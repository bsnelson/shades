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

    public DeviceListResponse getList() {
        List<SomaListResponse.Result> somaDevices = somaShadesClient.getDeviceList().getShades();
        List<SunsaListDeviceResponse> sunsaDevices = sunsaShadesClient.getDeviceList().getDevices();

        return DeviceListResponse.builder()
                .somaDevices(somaDevices)
                .sunsaDevices(sunsaDevices)
                .build();
    }

    public DevicesResponse getStates() {
        List<CompletableFuture<IResponse>> futures = deviceConfiguration.getDevices().stream()
                .map(device -> CompletableFuture.supplyAsync(() -> {
                    if (device.getType().equals("sunsa")) {
                        return sunsaShadesClient.getShadeState(device);
                    } else if (device.getType().equals("soma")) {
                        return somaShadesClient.getShadeState(device);
                    }
                    return null;
                }))
                .toList();
        DevicesResponse response = new DevicesResponse(futures.stream()
            .map(CompletableFuture::join) // This waits for each future to complete
            .toList());
        log.debug("Response is: " + response);
        return response;
    }

    public DevicesResponse setPosition(String position, String group, String name) {
        List<Device> filteredDevices = deviceConfiguration.getDevices().stream()
                .filter(device -> (group == null || device.getGroups().contains(group)) &&
                        (name == null || device.getName().equals(name)))
                .toList();

        List<CompletableFuture<IResponse>> futures = filteredDevices.stream()
                .map(device -> CompletableFuture.supplyAsync(() -> {
                    if (device.getType().equals("sunsa")) {
                        return sunsaShadesClient.setShadePosition(device, position);
                    } else if (device.getType().equals("soma")) {
                        return somaShadesClient.setShadePosition(device, position);
                    }
                    return null;
                }))
                .toList();

        DevicesResponse response = new DevicesResponse(futures.stream()
                .map(CompletableFuture::join) // This waits for each future to complete
                .toList());

        log.debug("Response is: " + response);
        return response;
    }

//    public DevicesResponse setPositions(String position) {
//        List<CompletableFuture<SomaDeviceResponse>> futures = deviceConfiguration.getDevices().stream()
//            .map(device -> CompletableFuture.supplyAsync(() -> somaShadesClient.setShadePosition(device, position)))
//            .toList();
//        DevicesResponse response = new DevicesResponse(futures.stream()
//            .map(CompletableFuture::join) // This waits for each future to complete
//            .toList());
//        log.debug("Response is: " + response);
//        return response;
//    }

//    public DevicesResponse openSeasonal() {
//        List<CompletableFuture<SomaDeviceResponse>> futures = deviceConfiguration.getDevices().stream()
//            .map(device -> CompletableFuture.supplyAsync(() -> somaShadesClient.setShadePosition(device, device.getSeasonalDefault())))
//            .toList();
//        DevicesResponse response = new DevicesResponse(futures.stream()
//            .map(CompletableFuture::join) // This waits for each future to complete
//            .toList());
//        log.debug("Response is: " + response);
//        return response;
//    }

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
                        return sunsaShadesClient.setShadePosition(device, (useSeasonal ? device.getSeasonalDefault() : position));
                    } else if (device.getType().equals("soma")) {
                        return somaShadesClient.setShadePosition(device, (useSeasonal ? device.getSeasonalDefault() : position));
                    }
                    return null;
                }))
                .toList();
            DevicesResponse response = new DevicesResponse(futures.stream()
                .map(CompletableFuture::join) // This waits for each future to complete
                .toList());
            List<String> failedDevices = response.getResponses().stream()
                .filter(deviceResponse -> {
                        // Check the type and process accordingly
                        if (deviceResponse instanceof SomaDeviceResponse somaResponse) {
                            return ERROR.equals(somaResponse.getResult());
                        } else if (deviceResponse instanceof SunsaPutDeviceResponse sunsaResponse) {
                            return !isNextHighestMultipleOfTen(useSeasonal ? getSeasonalFromDeviceId(getIdFromIResponse(deviceResponse)) : position, sunsaResponse.getDevice().getPosition());  //.getSeasonalDefault() : position, sunsaResponse.getDevice().getPosition());
                        }
                        return false; // Unknown type, exclude it
                    })
                .map(deviceResponse -> {
                    if (deviceResponse instanceof SomaDeviceResponse somaResponse) {
                        return somaResponse.getMac();
                    } else {
                        SunsaPutDeviceResponse sunsaResponse = (SunsaPutDeviceResponse) deviceResponse;
                        return sunsaResponse.getDevice().getIdDevice();
                    }
                })
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

    private String getIdFromIResponse(IResponse response) {
        if (response instanceof SomaDeviceResponse somaResponse) {
            return somaResponse.getMac();
        } else if (response instanceof SunsaPutDeviceResponse sunsaResponse) {
            return sunsaResponse.getDevice().getIdDevice();
        }
        return null;
    }

    private String getSeasonalFromDeviceId(String id) {
        return deviceConfiguration.getDevices().stream()
            .filter(device -> device.getId().equals(id))
            .findFirst()
            .map(Device::getSeasonalDefault)
            .orElse(null);
    }

    private boolean isNextHighestMultipleOfTen(String position, String getPosition) {
        int pos = Integer.parseInt(position);
        int nextMultipleOfTen = ((pos + 9) / 10) * 10;
        return Integer.parseInt(getPosition) == nextMultipleOfTen ||
                Integer.parseInt(getPosition) == -nextMultipleOfTen;
    }
}
