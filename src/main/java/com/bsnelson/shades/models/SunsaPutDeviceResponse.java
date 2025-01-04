package com.bsnelson.shades.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SunsaPutDeviceResponse implements IResponse{
    private Device device;
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Device {
        String idDevice;
        String name;
        String position;
    }
}
