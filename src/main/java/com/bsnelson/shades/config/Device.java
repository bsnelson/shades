package com.bsnelson.shades.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@Valid
@NoArgsConstructor
@Builder
public class Device {
    @NotBlank
    private String mac;
    private String name;
    private String seasonalDefault;
    private List<String> groups;
}
