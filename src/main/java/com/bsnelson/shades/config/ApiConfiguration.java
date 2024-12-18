package com.bsnelson.shades.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.MultiValueMap;

@Getter
@Configuration
@Data
@NoArgsConstructor
public class ApiConfiguration {
    @Value("${downstream.retries}")
    private String retries;
}
