package com.bsnelson.shades.config;

import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.MultiValueMap;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;

@Getter
@Configuration
@Data
@NoArgsConstructor
@ConfigurationProperties("downstream.api.sunsa")
@EnableConfigurationProperties
public class SunsaConfiguration {
    private ApiEndpoint listDevices;
    private ApiEndpoint setShadePosition;

    @Getter
    @Value("${downstream.sunsa.baseUrl}")
    private String sunsaBaseUrl;

    @Getter
    @Value("${downstream.sunsa.apiKey}")
    private String apiKey;

    @Getter
    @Value("${downstream.sunsa.idUser}")
    private String idUser;

    @Data
    @AllArgsConstructor
    @Valid
    @NoArgsConstructor
    @Builder
    public static class ApiEndpoint {
        @NotBlank
        private String path;
        private MultiValueMap<String, String> queryParams;
    }
}
