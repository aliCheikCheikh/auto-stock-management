package com.aliCheikh.stock.infrastructure.security;

import com.aliCheikh.stock.application.dto.RecoverOwnerAccessCommand;
import com.aliCheikh.stock.application.usecase.RecoverOwnerAccessUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("owner-recovery")
public final class OwnerRecoveryRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OwnerRecoveryRunner.class);

    private final RecoverOwnerAccessUseCase recoverOwnerAccess;
    private final String ownerEmail;
    private final String newPassword;

    public OwnerRecoveryRunner(RecoverOwnerAccessUseCase recoverOwnerAccess,
                               @Value("${OWNER_RECOVERY_EMAIL:}") String ownerEmail,
                               @Value("${OWNER_RECOVERY_PASSWORD:}") String newPassword) {
        this.recoverOwnerAccess = recoverOwnerAccess;
        this.ownerEmail = ownerEmail;
        this.newPassword = newPassword;
    }

    @Override
    public void run(ApplicationArguments arguments) {
        if (ownerEmail.isBlank() || newPassword.isBlank()) {
            throw new IllegalStateException(
                    "OWNER_RECOVERY_EMAIL and OWNER_RECOVERY_PASSWORD are required for recovery.");
        }

        recoverOwnerAccess.execute(new RecoverOwnerAccessCommand(ownerEmail, newPassword));
        log.info("Owner access recovered; all previous sessions have been revoked.");
    }
}
