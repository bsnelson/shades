package com.bsnelson.shades.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SomaDeviceResponse implements IResponse{
    String result;
    String version;
    String id;
    String position;
    Boolean closed_upwards;
}
