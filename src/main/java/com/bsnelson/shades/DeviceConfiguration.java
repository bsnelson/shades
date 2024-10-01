package com.bsnelson.shades;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.MultiValueMap;

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
        private List<String> groups;
    }
}
