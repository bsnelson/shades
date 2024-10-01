package com.bsnelson.shades;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ConfigService {
    @Autowired
    private Environment environment;

    public String getConfigValue(String configKey) {
        return environment.getProperty(configKey);
    }
}
