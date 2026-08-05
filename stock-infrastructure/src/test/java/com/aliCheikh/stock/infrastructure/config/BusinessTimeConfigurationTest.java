package com.aliCheikh.stock.infrastructure.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class BusinessTimeConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(BusinessTimeConfiguration.class);

    @Test
    void should_use_ndjamena_as_the_default_business_time_zone() {
        contextRunner.run(context -> assertThat(context.getBean(Clock.class).getZone())
                .isEqualTo(ZoneId.of("Africa/Ndjamena")));
    }

    @Test
    void should_allow_the_business_time_zone_to_be_configured() {
        contextRunner
                .withPropertyValues("app.business-time-zone=Africa/Dakar")
                .run(context -> assertThat(context.getBean(Clock.class).getZone())
                        .isEqualTo(ZoneId.of("Africa/Dakar")));
    }
}
