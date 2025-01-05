package com.bsnelson.shades.util;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CacheEvictionScheduler {

    // Evicts all entries in the "deviceStates" cache every 10 minutes
    @Scheduled(fixedRate = 60 * 60 * 1000) // 60 minutes in milliseconds
    @CacheEvict(value = "sunsaList", allEntries = true)
    public void clearCache() {
        System.out.println("Cache evicted at: " + System.currentTimeMillis());
    }
}