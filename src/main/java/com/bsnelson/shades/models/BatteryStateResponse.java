package com.bsnelson.shades.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BatteryStateResponse implements IResponse {
    private String name;
    private Integer battery_percentage;
}