package com.bsnelson.shades.config;

import lombok.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
@Data
@NoArgsConstructor
public class ApiConfiguration {
    @Value("${downstream.retries}")
    private String retries;
}
