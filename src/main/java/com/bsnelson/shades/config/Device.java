package com.bsnelson.shades.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import java.util.List;

@Data
@AllArgsConstructor
@Valid
@NoArgsConstructor
@Builder
public class Device {
    @NotBlank
    private String id;
    private String type;
    private String name;
    private String seasonalDefault;
    private List<String> groups;
}
