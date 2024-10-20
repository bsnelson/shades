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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@AllArgsConstructor
@Service
@Configuration
@Slf4j
public class ShadesService {
    private final ShadesClient shadesClient;
    private final DeviceConfiguration deviceConfiguration;
    private final RetryConfiguration retryConfiguration;

//    public ListDevicesResponse getList() {
//        return shadesClient.getDeviceList();
//    }

//    public Mono<CloseAllResponse> closeAllShades() {
//        return shadesClient.closeAllShades();
//    }

    public DevicesResponse getStates() {
        List<CompletableFuture<DeviceResponse>> futures = deviceConfiguration.getDevices().stream()
                .map(device -> CompletableFuture.supplyAsync(() -> shadesClient.getShadeState(device)))
                .toList();

        DevicesResponse response = new DevicesResponse(futures.stream()
            .map(CompletableFuture::join) // This waits for each future to complete
            .toList());
        log.info("Response is: " + response);
        return response;
    }

//    private <T> List<IResponse> callClients(List<String> paths, Class<T> clazz) {
//        final ExecutorService executorService = Executors.newFixedThreadPool(paths.size());
//        List<CompletableFuture<IResponse>> futures = paths.stream()
//            .map(url -> CompletableFuture.supplyAsync(() -> callClient(url, clazz), executorService))
//            .toList();
//
//        return futures.stream()
//            .map(CompletableFuture::join) // This waits for each future to complete
//            .toList();
//    }
//
//    public Mono<DevicesResponse> setPositions(String position) {
//        // Create a list of Monos dynamically from the device list
//        List<Mono<DeviceResponse>> shadeMonos = deviceConfiguration.getDevices().stream()
//                .map(device -> shadesClient.setShadePosition(device.getMac(), position)
//                        .subscribeOn(Schedulers.boundedElastic())
//                        .onErrorComplete())
//                .toList();
//        return getMonos(shadeMonos);
//    }
//
//    public Mono<DevicesResponse> openSeasonal() {
//        // Create a list of Monos dynamically from the device list
//        List<Mono<DeviceResponse>> shadeMonos = deviceConfiguration.getDevices().stream()
//                .map(device -> shadesClient.setShadePosition(device.getMac(), device.getSeasonalDefault())
//                        .subscribeOn(Schedulers.boundedElastic())
//                        .onErrorComplete())
//                .toList();
//        return getMonos(shadeMonos);
//    }
//
//    public Mono<Void> reopen() {
//        // Create a list of Monos dynamically from the device list
//        int remainingRetries = 0;
//        log.info("Starting reopen");
//        List<Device> devices = deviceConfiguration.getDevices();
//        return setAndValidateDevices(devices, remainingRetries);
//    }
//    // Reactive method to handle the full set -> get -> validate workflow
//    private Mono<Void> setAndValidateDevices(List<Device> devices, int remainingRetries) {
//        return makeSetCalls(devices) // Perform the `set()` calls in parallel
//            .then(makeGetCalls(devices)) // Once `set()` calls complete, perform the `get()` calls
//            .flatMapMany(responses -> Flux.fromIterable(responses)
//                .filter(response -> !withinSpec("100", response)) // Filter out non-compliant devices
//                .collectList()
//            )
//            .flatMap(nonCompliantDevices -> {
//                if (nonCompliantDevices.isEmpty()) {
//                    // All devices are in compliance
//                    return Mono.empty(); // Signal completion
//                } else if (remainingRetries > 0) {
//                    // Retry with non-compliant devices
//                    System.out.println("Retrying with non-compliant devices: " + nonCompliantDevices);
//                    return setAndValidateDevices(mapResponsesToDevices(nonCompliantDevices), remainingRetries - 1); // Retry
//                } else {
//                    // Exhausted retries
//                    return Mono.error(new RuntimeException("Retries exhausted, devices are not in compliance."));
//                }
//            }).then();
//    }
//
//    public List<Device> mapResponsesToDevices(List<DeviceResponse> deviceResponses) {
//        List<Device> devices = deviceConfiguration.getDevices();
//        return devices.stream()
//            .peek(device -> {
//                // Find a matching DeviceResponse by mac
//                DeviceResponse matchingResponse = deviceResponses.stream()
//                    .filter(response -> response.getMac().equals(device.getMac()))
//                    .findFirst()
//                    .orElse(null);
//            })
//            .collect(Collectors.toList());
//    }
//
//    private Mono<List<DeviceResponse>> makeSetCalls(List<Device> devices) {
//            return Mono.just(devices)
//                .flatMapMany(Flux::fromIterable) // Convert the list into a Flux
//                .flatMap(deviceName -> setDevice(deviceName, deviceName.getSeasonalDefault()).subscribeOn(Schedulers.boundedElastic())) // Process each device
//                .collectList(); // Collect results into a Mono<List<Void>> (or a relevant type)
//    }
//
//    // Simulates parallel `get()` calls to devices
//    private Mono<List<DeviceResponse>> makeGetCalls(List<Device> devices) {
//        return Mono.defer(() -> {
//            // Use Flux internally to process each device, but return the result as a Mono
//            return Mono.just(devices)
//                .flatMapMany(Flux::fromIterable) // Convert the list into a Flux
//                .flatMap(deviceName -> getDeviceState(deviceName).subscribeOn(Schedulers.boundedElastic())) // Process each device
//                .collectList(); // Collect results into a Mono<List<Void>> (or a relevant type)
//        });
//    }
//
//    // Simulates a device `set()` call (HTTP call)
//    private Mono<DeviceResponse> setDevice(Device device, String position) {
//        return shadesClient.setShadePosition(device.getMac(), position);
//    }
//
//    // Simulates a device `get()` call (HTTP call)
//    private Mono<DeviceResponse> getDeviceState(Device device) {
//        return shadesClient.getShadeState(device.getMac());
//    }
//
    // Checks if the device response is in compliance (for demonstration, position < 10 is compliant)
//    private static boolean isInCompliance(DeviceResponse response) {
//        return response.getPosition() < 10;
//    }
//
//    // A simple class to represent the response from a `get()` call
//    static class DeviceResponse {
//        private final String deviceName;
//        private final int position;
//
//        public DeviceResponse(String deviceName, int position) {
//            this.deviceName = deviceName;
//            this.position = position;
//        }
//
//        public String getDeviceName() {
//            return deviceName;
//        }
//
//        public int getPosition() {
//            return position;
//        }
//    }

//    private boolean withinSpec(String stringDesired, DeviceResponse resp) {
//        double desired = 0.0;
//        double actual = 0.0;
//        try {
//            desired = new BigDecimal(stringDesired).doubleValue();
//            actual = new BigDecimal(resp.getPosition()).doubleValue();
//        } catch (Exception ignored) {}
//        log.info("Checking ver " + resp.getVersion() + ", pos " + resp.getPosition());
//        return actual <= desired * 1.1 && actual >= desired * 0.9;
//    }
//
//    private Mono<DevicesResponse> getMonos(List<Mono<DeviceResponse>> shadeMonos) {
//        @SuppressWarnings("unchecked")
//        Mono<DeviceResponse>[] monoArray = new Mono[shadeMonos.size()];
//        monoArray = shadeMonos.toArray(monoArray);
//        System.out.println("Hit get with " + shadeMonos.size());
//        return Mono.zip(
//            responses -> {
//                List<DeviceResponse> deviceResponses = Stream.of(responses)
//                    .map(response -> (DeviceResponse) response)
//                    .collect(Collectors.toList());
//                return new DevicesResponse(deviceResponses);
//            },
//            monoArray
//        ).log();
//    }
}
