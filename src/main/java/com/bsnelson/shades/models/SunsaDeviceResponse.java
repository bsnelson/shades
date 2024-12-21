package com.bsnelson.shades.models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SunsaDeviceResponse implements IResponse{
    String idDevice;
    String name;
    String position;
    String apiUrl;
}
