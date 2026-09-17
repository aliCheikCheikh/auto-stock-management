package com.aliCheikh.stock.infrastructure.persistence.mapper;

import com.aliCheikh.stock.domain.model.customer.CustomerId;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.sale.Sale;
import com.aliCheikh.stock.domain.model.sale.Payment;
import com.aliCheikh.stock.domain.model.sale.PaymentId;
import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.sale.SaleLineDto;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.infrastructure.persistence.entity.PaymentJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.SaleJpaEntity;
import com.aliCheikh.stock.infrastructure.persistence.entity.SaleLineJpaEntity;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Currency;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Component
public class SaleJpaMapper {

    public Sale toDomain(SaleJpaEntity entity) {
        Objects.requireNonNull(entity, "entity cannot be null");

        return Sale.rehydrate(
                SaleId.of(entity.getId()),
                UserId.of(entity.getSoldBy()),
                entity.getOccurredAt(),
                Money.create(
                        entity.getTotalAmount(),
                        Currency.getInstance(entity.getTotalCurrency())
                ),
                entity.getSaleLines()
                        .stream()
                        .sorted(Comparator.comparingInt(SaleLineJpaEntity::getLineNumber))
                        .map(line -> new SaleLineDto(
                                ProductId.of(line.getProductId()),
                                line.getQuantity(),
                                Money.create(
                                        line.getUnitPriceAmount(),
                                        Currency.getInstance(line.getUnitPriceCurrency())
                                ),
                                Money.create(
                                        line.getLineTotalAmount(),
                                        Currency.getInstance(line.getLineTotalCurrency())
                                )
                        ))
                        .toList(),
                // A cash sale may have a null customer ID.
                entity.getCustomerId() == null ? null : CustomerId.of(entity.getCustomerId()),
                entity.getPayments()
                        .stream()
                        // Order payments from oldest to newest.
                        .sorted(Comparator.comparing(PaymentJpaEntity::getReceivedAt))
                        .map(payment -> Payment.rehydrate(
                                PaymentId.of(payment.getId()),
                                Money.create(payment.getAmount(), Currency.getInstance(payment.getCurrency())),
                                UserId.of(payment.getReceivedBy()),
                                payment.getReceivedAt()
                        ))
                        .toList()
        );
    }

    public SaleJpaEntity toEntity(Sale sale) {
        Objects.requireNonNull(sale, "sale cannot be null");

        SaleJpaEntity entity = SaleJpaEntity.of(
                sale.getSaleId().getValue(),
                sale.getSoldBy().getValue(),
                sale.getOccurredAt(),
                sale.getTotalAmount().getAmount(),
                sale.getTotalAmount().getCurrency().getCurrencyCode(),
                sale.getCustomerId().map(CustomerId::getValue).orElse(null)
        );

        Set<SaleLineJpaEntity> saleLineEntities = new HashSet<>();
        int lineNumber = 1;

        for (SaleLineDto line : sale.getLines()) {
            saleLineEntities.add(SaleLineJpaEntity.of(
                    entity,
                    lineNumber,
                    line.productId().getValue(),
                    line.quantity(),
                    line.unitPrice().getAmount(),
                    line.unitPrice().getCurrency().getCurrencyCode(),
                    line.lineTotal().getAmount(),
                    line.lineTotal().getCurrency().getCurrencyCode()
            ));
            lineNumber++;
        }

        entity.replaceSaleLines(saleLineEntities);

        // Preserve payment identities when saving the sale again.
        entity.replacePayments(sale.getPayments().stream()
                .map(payment -> PaymentJpaEntity.of(
                        payment.getPaymentId().getValue(),
                        entity,
                        payment.getAmount().getAmount(),
                        payment.getAmount().getCurrency().getCurrencyCode(),
                        payment.getReceivedAt(),
                        payment.getReceivedBy().getValue()
                ))
                .toList());

        return entity;
    }
}
