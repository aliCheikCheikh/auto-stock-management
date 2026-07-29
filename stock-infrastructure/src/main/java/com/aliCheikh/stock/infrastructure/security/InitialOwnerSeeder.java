package com.aliCheikh.stock.infrastructure.security;

import com.aliCheikh.stock.application.dto.ProvisionFirstOwnerCommand;
import com.aliCheikh.stock.application.usecase.ProvisionFirstOwnerUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("dev")
public final class InitialOwnerSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(InitialOwnerSeeder.class);

    private final ProvisionFirstOwnerUseCase provisionFirstOwner;
    private final String ownerDisplayName;
    private final String ownerEmail;
    private final String ownerPassword;

    public InitialOwnerSeeder(ProvisionFirstOwnerUseCase provisionFirstOwner,
                              @Value("${INITIAL_OWNER_DISPLAY_NAME:}") String ownerDisplayName,
                              @Value("${INITIAL_OWNER_EMAIL:}") String ownerEmail,
                              @Value("${INITIAL_OWNER_PASSWORD:}") String ownerPassword) {
        this.provisionFirstOwner = provisionFirstOwner;
        this.ownerDisplayName = ownerDisplayName;
        this.ownerEmail = ownerEmail;
        this.ownerPassword = ownerPassword;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (ownerDisplayName.isBlank() || ownerEmail.isBlank() || ownerPassword.isBlank()) {
            log.info("Amorçage du propriétaire ignoré : configuration de développement incomplète.");
            return;
        }

        provisionFirstOwner.execute(new ProvisionFirstOwnerCommand(
                        ownerDisplayName,
                        ownerEmail,
                        ownerPassword))
                .ifPresentOrElse(
                        owner -> log.info("Premier propriétaire de développement créé."),
                        () -> log.info("Amorçage ignoré : le registre des utilisateurs n'est pas vide."));
    }
}
