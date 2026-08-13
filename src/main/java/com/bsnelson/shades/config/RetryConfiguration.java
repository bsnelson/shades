package com.bsnelson.shades.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@Data
@NoArgsConstructor
@ConfigurationProperties("downstream")
@EnableConfigurationProperties
public class RetryConfiguration {
    private Integer retries;
}
