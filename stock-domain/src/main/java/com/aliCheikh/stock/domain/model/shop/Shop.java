package com.aliCheikh.stock.domain.model.shop;

import com.aliCheikh.stock.domain.exception.shop.InvalidShopAddressException;
import com.aliCheikh.stock.domain.exception.shop.InvalidShopNameException;

import java.util.Objects;

public class Shop {
    private final ShopId shopId;
    private String name;
    private String address;

    public Shop(ShopId shopId, String name, String address) {
        this.shopId = Objects.requireNonNull(shopId, "shopId cannot be null");
        this.name = validateName(name);
        this.address = validateAddress(address);
    }

    public void rename(String newName) {
        this.name = validateName(newName);
    }

    public void updateAddress(String newAddress) {
        this.address = validateAddress(newAddress);
    }

    // --- Gardiens (DRY) ---
    private String validateName(String nameToValidate) {
        if (nameToValidate == null || nameToValidate.isBlank()) {
            throw new InvalidShopNameException(nameToValidate);
        }
        return nameToValidate;
    }

    private String validateAddress(String addressToValidate) {
        if (addressToValidate == null || addressToValidate.isBlank()) {
            throw new InvalidShopAddressException(addressToValidate);
        }
        return addressToValidate;
    }

    // --- Getters ---
    public ShopId getShopId() {
        return shopId;
    }

    public String getName() {
        return name;
    }

    public String getAddress() {
        return address;
    }
}