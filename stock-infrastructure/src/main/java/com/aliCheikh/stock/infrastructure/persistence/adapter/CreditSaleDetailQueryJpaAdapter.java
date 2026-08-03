package com.aliCheikh.stock.infrastructure.persistence.adapter;

import com.aliCheikh.stock.application.dto.CreditSaleDetailView;
import com.aliCheikh.stock.application.dto.CreditSaleLineView;
import com.aliCheikh.stock.application.dto.CreditSalePaymentView;
import com.aliCheikh.stock.application.port.CreditSaleDetailQueryPort;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.infrastructure.persistence.projection.CreditSaleHeaderProjection;
import com.aliCheikh.stock.infrastructure.persistence.projection.CreditSaleLineProjection;
import com.aliCheikh.stock.infrastructure.persistence.projection.CreditSalePaymentProjection;
import com.aliCheikh.stock.infrastructure.persistence.repository.CreditSaleDetailJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Currency;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Assemble le détail d'une créance à partir de trois projections plates.
 *
 * <p>L'agrégat {@code Sale} n'est pas rechargé : il ne porte que des identifiants de produits et
 * d'utilisateurs, quand cet écran a besoin de leurs noms. Le reconstruire imposerait ensuite une
 * requête par produit — un N+1 pour un résultat moins complet.</p>
 */
@Repository
public class CreditSaleDetailQueryJpaAdapter implements CreditSaleDetailQueryPort {

    private final CreditSaleDetailJpaRepository creditSaleDetailJpaRepository;

    public CreditSaleDetailQueryJpaAdapter(CreditSaleDetailJpaRepository creditSaleDetailJpaRepository) {
        this.creditSaleDetailJpaRepository = Objects.requireNonNull(
                creditSaleDetailJpaRepository, "creditSaleDetailJpaRepository cannot be null");
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CreditSaleDetailView> findCreditSaleDetail(UUID saleId) {
        Objects.requireNonNull(saleId, "saleId cannot be null");

        return creditSaleDetailJpaRepository.findHeader(saleId)
                .map(header -> toView(
                        header,
                        creditSaleDetailJpaRepository.findLines(saleId),
                        creditSaleDetailJpaRepository.findPayments(saleId)));
    }

    private static CreditSaleDetailView toView(CreditSaleHeaderProjection header,
                                               List<CreditSaleLineProjection> lines,
                                               List<CreditSalePaymentProjection> payments) {
        Currency currency = Currency.getInstance(header.getCurrency());
        Money totalAmount = Money.create(header.getTotalAmount(), currency);
        Money amountPaid = Money.create(header.getAmountPaid(), currency);
        // Le solde reste dérivé du total et des encaissements : une seule source de vérité.
        Money amountDue = totalAmount.subtract(amountPaid);

        return new CreditSaleDetailView(
                header.getSaleId(),
                header.getOccurredAt(),
                header.getSellerId(),
                header.getSellerName(),
                header.getCustomerId(),
                header.getCustomerGivenName(),
                header.getCustomerFatherName(),
                header.getCustomerPhoneNumber(),
                lines.stream().map(CreditSaleDetailQueryJpaAdapter::toLineView).toList(),
                totalAmount,
                amountPaid,
                amountDue,
                !amountDue.isPositive(),
                payments.stream().map(CreditSaleDetailQueryJpaAdapter::toPaymentView).toList());
    }

    private static CreditSaleLineView toLineView(CreditSaleLineProjection line) {
        Currency currency = Currency.getInstance(line.getCurrency());

        return new CreditSaleLineView(
                line.getProductId(),
                line.getProductName(),
                line.getProductReference(),
                line.getQuantity(),
                Money.create(line.getUnitPriceAmount(), currency),
                Money.create(line.getLineTotalAmount(), currency));
    }

    private static CreditSalePaymentView toPaymentView(CreditSalePaymentProjection payment) {
        return new CreditSalePaymentView(
                payment.getPaymentId(),
                Money.create(payment.getAmount(), Currency.getInstance(payment.getCurrency())),
                payment.getReceivedAt(),
                payment.getReceivedById(),
                payment.getReceivedByName());
    }
}
