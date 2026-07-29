package com.aliCheikh.stock.application.port;

public interface PasswordProtection {

    String protect(String password);

    boolean matches(String password, String protectedPassword);
}
