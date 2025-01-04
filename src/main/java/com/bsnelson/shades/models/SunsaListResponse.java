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
public class SunsaListResponse implements IResponse {
    private List<SunsaListDeviceResponse> devices;
    private int deviceCount;
}