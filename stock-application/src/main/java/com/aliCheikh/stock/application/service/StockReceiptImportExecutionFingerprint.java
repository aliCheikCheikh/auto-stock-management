package com.aliCheikh.stock.application.service;

import com.aliCheikh.stock.application.dto.StockReceiptImportExecutionCommand;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public final class StockReceiptImportExecutionFingerprint {

    public String calculate(StockReceiptImportExecutionCommand command) {
        MessageDigest digest = sha256();
        update(digest, command.file().content());
        update(digest, command.shopId().toString().getBytes(StandardCharsets.UTF_8));
        update(digest, command.userId().toString().getBytes(StandardCharsets.UTF_8));
        command.selectedLineNumbers().stream().sorted()
                .forEach(lineNumber -> update(digest, ByteBuffer.allocate(Integer.BYTES)
                        .putInt(lineNumber).array()));
        return toHex(digest.digest());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", exception);
        }
    }

    private static void update(MessageDigest digest, byte[] value) {
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(value.length).array());
        digest.update(value);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            hex.append(String.format("%02x", value));
        }
        return hex.toString();
    }
}
