package com.bsnelson.shades.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SomaListResponse implements IResponse{
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
