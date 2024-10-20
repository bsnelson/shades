package com.bsnelson.shades.config;

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
    private List<Device> devices;
}
