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
public class ListDevicesResponse {
    String result;
    String version;
    List<Result> shades;
    @Data
    public static class Result {
        String name;
        String mac;
        String type;
        String gen;
    }
}
