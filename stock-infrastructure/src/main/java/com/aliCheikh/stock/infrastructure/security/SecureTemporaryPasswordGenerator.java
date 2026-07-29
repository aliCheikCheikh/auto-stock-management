package com.aliCheikh.stock.infrastructure.security;

import com.aliCheikh.stock.application.port.TemporaryPasswordGenerator;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public final class SecureTemporaryPasswordGenerator implements TemporaryPasswordGenerator {

    private static final String READABLE_CHARACTERS =
            "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int GROUP_COUNT = 4;
    private static final int CHARACTERS_PER_GROUP = 4;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String generate() {
        StringBuilder password = new StringBuilder();
        for (int group = 0; group < GROUP_COUNT; group++) {
            if (group > 0) {
                password.append('-');
            }
            for (int character = 0; character < CHARACTERS_PER_GROUP; character++) {
                password.append(READABLE_CHARACTERS.charAt(secureRandom.nextInt(READABLE_CHARACTERS.length())));
            }
        }
        return password.toString();
    }
}
