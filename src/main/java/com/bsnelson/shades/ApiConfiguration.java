package com.bsnelson.shades;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.MultiValueMap;
import org.springframework.validation.annotation.Validated;

@Configuration
@Data
@NoArgsConstructor
@ConfigurationProperties("downstream.api")
@EnableConfigurationProperties
public class ApiConfiguration {
    private ApiEndpoint shade;

    @Data
    @AllArgsConstructor
    @Valid
    @NoArgsConstructor
    @Builder
    public static class ApiEndpoint {
        @NotBlank private String path;
        private MultiValueMap<String, String> queryParams;
    }
}
