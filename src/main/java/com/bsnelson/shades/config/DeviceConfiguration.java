package com.bsnelson.shades.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Data
@NoArgsConstructor
@ConfigurationProperties("downstream")
@EnableConfigurationProperties
public class DeviceConfiguration {
    private List<Devices> devices;

    @Data
    @AllArgsConstructor
    @Valid
    @NoArgsConstructor
    @Builder
    public static class Devices {
        @NotBlank
        private String mac;
        private String name;
        private String seasonalDefault;
        private List<String> groups;
    }
}
