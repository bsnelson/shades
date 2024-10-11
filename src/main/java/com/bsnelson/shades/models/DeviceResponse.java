package com.bsnelson.shades.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeviceResponse {
    String result;
    String version;
    String mac;
    String position;
    Boolean closed_upwards;
}
