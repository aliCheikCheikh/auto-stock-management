package com.aliCheikh.stock.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Création ou renommage d'une famille de pièces.
 *
 * <p>La validation ne rejette ici que l'absurde — nom vide ou trop long. L'unicité relève du
 * métier et se vérifie plus bas.</p>
 */
public record CategoryRequest(@NotBlank @Size(max = 255) String name) {
}
