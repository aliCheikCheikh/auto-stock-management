package com.aliCheikh.stock.infrastructure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Profiles;

@SpringBootApplication
public class StockApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(StockApplication.class, args);

        // Recovery is a one-shot operation; close the context after the runner finishes.
        if (context.getEnvironment().acceptsProfiles(Profiles.of("owner-recovery"))) {
            context.close();
        }
    }
}
