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
public class CloseAllResponse {
    String result;
    String version;
    String msg;
    List<Result> results;
    private class Result {
        String mac;
        String name;
        String result;
        List<String> errors;
    }
}
