package com.aliCheikh.stock.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/** Fournit l'heure métier indépendamment du fuseau du serveur d'hébergement. */
@Configuration
public class BusinessTimeConfiguration {

    @Bean
    public Clock businessClock(
            @Value("${app.business-time-zone:Africa/Ndjamena}") String timeZone) {
        return Clock.system(ZoneId.of(timeZone));
    }
}
