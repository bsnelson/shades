package com.bsnelson.shades.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SunsaListDeviceResponse implements IResponse{
    private String name;
    private int idDevice;
    private BlindType blindType;
    private String apiUrl;
    private Light light;
    private boolean isConnected;
    private String batteryPercentage;
    private int position;
    private DefaultSmartHomeDirection defaultSmartHomeDirection;
    private Temperature temperature;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BlindType {
        private String text;
        private int value;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Light {
        private String text;
        private Integer value;
        private String unit;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DefaultSmartHomeDirection {
        private String text;
        private int value;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Temperature {
        private String text;
        private int value;
        private String unit;
    }
}