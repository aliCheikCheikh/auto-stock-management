package com.aliCheikh.stock.application.dto;

import com.aliCheikh.stock.domain.model.user.UserId;
import java.util.List;

public record ReceiveStockCommand(
        String productReference,       // On cherche par référence !
        ProductInfo newProductInfo,    // Peut être 'null' si le produit existe déjà
        UserId userId,
        List<TargetLocation> distributions // La répartition demandée
) {}