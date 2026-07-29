package com.aliCheikh.stock.infrastructure;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.Profiles;

@SpringBootApplication
public class StockApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(StockApplication.class, args);

        // La récupération est une opération ponctuelle : une fois le runner terminé,
        // fermer le contexte empêche ce démarrage exceptionnel de devenir un serveur durable.
        if (context.getEnvironment().acceptsProfiles(Profiles.of("owner-recovery"))) {
            context.close();
        }
    }
}
