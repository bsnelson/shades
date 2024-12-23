package com.bsnelson.shades.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SunsaDevicesResponse implements IResponse{
    List<Result> devices;
    @Data
    public static class Result {
        String name;
        Integer idDevice;
        Integer position;
        String batteryPercentage;
    }
}
