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
public class SomaDeviceResponse implements IResponse{
    String result;
    String version;
    String mac;
    String name;
    String position;
    Boolean closed_upwards;
    Integer battery_percentage;
}
