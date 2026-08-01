package com.aliCheikh.stock.infrastructure.web.mapper;

import com.aliCheikh.stock.application.dto.ListSalesQuery;
import com.aliCheikh.stock.application.dto.RecordPaymentCommand;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.dto.SaleView;
import com.aliCheikh.stock.application.dto.SellLineCommand;
import com.aliCheikh.stock.application.dto.SellProductCommand;
import com.aliCheikh.stock.application.dto.SellProductResult;
import com.aliCheikh.stock.domain.model.customer.CustomerId;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.sale.Sale;
import com.aliCheikh.stock.domain.model.sale.SaleId;
import com.aliCheikh.stock.domain.model.sale.SaleLineDto;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.infrastructure.web.dto.CreateSaleLine;
import com.aliCheikh.stock.infrastructure.web.dto.CreateSaleRequest;
import com.aliCheikh.stock.infrastructure.web.dto.MoneyResponse;
import com.aliCheikh.stock.infrastructure.web.dto.PageMetaResponse;
import com.aliCheikh.stock.infrastructure.web.dto.PageOfSaleResponse;
import com.aliCheikh.stock.infrastructure.web.dto.RecordPaymentRequest;
import com.aliCheikh.stock.infrastructure.web.dto.SaleLineResponse;
import com.aliCheikh.stock.infrastructure.web.dto.SaleResponse;

import java.time.LocalDateTime;
import java.util.Currency;
import java.util.List;
import java.util.UUID;

public final class SaleWebMapper {
    private static final List<String> DEFAULT_SORT = List.of("createdAt,desc");

    /**
     * Devise du magasin (Franc CFA). L'API reçoit un montant nu ; la devise est celle du catalogue.
     * Le domaine refusera de toute façon un acompte dans une devise différente du total.
     */
    private static final Currency SHOP_CURRENCY = Currency.getInstance("XAF");

    private SaleWebMapper() {
    }

    public static SellProductCommand toCommand(CreateSaleRequest request, UUID sellerId) {
        return new SellProductCommand(
                UserId.of(sellerId),
                ShopId.of(request.shopId()),
                request.lines().stream()
                        .map(SaleWebMapper::toLineCommand)
                        .toList(),
                request.customerId() == null ? null : CustomerId.of(request.customerId()),
                // L'acompte est saisi dans la devise du magasin ; absent, la vente est au comptant.
                request.amountPaid() == null ? null : Money.create(request.amountPaid(), SHOP_CURRENCY)
        );
    }

    /** Encaissement d'un remboursement : le montant est saisi nu, dans la devise du magasin. */
    public static RecordPaymentCommand toCommand(UUID saleId, RecordPaymentRequest request, UUID receivedBy) {
        return new RecordPaymentCommand(
                SaleId.of(saleId),
                Money.create(request.amount(), SHOP_CURRENCY),
                UserId.of(receivedBy));
    }

    public static SaleResponse toResponse(SellProductResult result) {
        return new SaleResponse(
                result.saleId().getValue(),
                result.sellerId().getValue(),
                // La création renvoie l'auteur de l'appel : le front connaît déjà son propre nom.
                null,
                result.lines().stream()
                        .map(SaleWebMapper::toSaleLineResponse)
                        .toList(),
                moneyToResponse(result.totalAmount()),
                result.createdAt(),
                result.customerId().map(CustomerId::getValue).orElse(null),
                moneyToResponse(result.amountPaid()),
                moneyToResponse(result.amountDue())
        );
    }

    public static SaleResponse toResponse(Sale sale) {
        return new SaleResponse(
                sale.getSaleId().getValue(),
                sale.getSoldBy().getValue(),
                null,
                sale.getLines().stream()
                        .map(SaleWebMapper::toSaleLineResponse)
                        .toList(),
                moneyToResponse(sale.getTotalAmount()),
                sale.getOccurredAt(),
                sale.getCustomerId().map(CustomerId::getValue).orElse(null),
                moneyToResponse(sale.getAmountPaid()),
                moneyToResponse(sale.getAmountDue())
        );
    }

    /**
     * L'historique porte le solde restant dû, pour que l'écran des ventes dise sous quelle forme
     * chacune a été encaissée sans interroger un second endpoint. Le client et le montant versé,
     * eux, restent l'affaire de l'écran des créances, qui joint déjà les informations du client.
     */
    private static SaleResponse toResponse(SaleView saleView) {
        return new SaleResponse(
                saleView.saleId().getValue(),
                saleView.sellerId().getValue(),
                saleView.sellerName(),
                saleView.lines().stream()
                        .map(SaleWebMapper::toSaleLineResponse)
                        .toList(),
                moneyToResponse(saleView.totalAmount()),
                saleView.createdAt(),
                null,
                null,
                nullableMoneyToResponse(saleView.amountDue())
        );
    }

    private static SellLineCommand toLineCommand(CreateSaleLine line) {
        return new SellLineCommand(
                ProductId.of(line.productId()),
                line.quantity()
        );
    }

    private static SaleLineResponse toSaleLineResponse(SaleLineDto line) {
        return new SaleLineResponse(
                line.productId().getValue(),
                line.quantity(),
                moneyToResponse(line.unitPrice()),
                moneyToResponse(line.lineTotal())
        );
    }

    public static ListSalesQuery toQuery(
            int page,
            int size,
            List<String> sort,
            UUID sellerId,
            UUID shopId,
            LocalDateTime from,
            LocalDateTime to
    ) {
        return new ListSalesQuery(
                page,
                size,
                normalizeSort(sort),
                sellerId == null ? null : UserId.of(sellerId),
                shopId == null ? null : ShopId.of(shopId),
                from,
                to
        );
    }

    public static PageOfSaleResponse toPageResponse(PageResult<SaleView> page) {
        return new PageOfSaleResponse(
                page.content().stream()
                        .map(SaleWebMapper::toResponse)
                        .toList(),
                new PageMetaResponse(
                        page.page(),
                        page.size(),
                        page.totalElements(),
                        page.totalPages()
                )
        );
    }

    /** Une vente dont le règlement n'a pas pu être résolu n'affiche rien plutôt qu'un faux zéro. */
    private static MoneyResponse nullableMoneyToResponse(Money money) {
        return money == null ? null : moneyToResponse(money);
    }

    private static MoneyResponse moneyToResponse(Money money) {
        return new MoneyResponse(
                money.getAmount().toPlainString(),
                money.getCurrency().getCurrencyCode()
        );
    }

    private static List<String> normalizeSort(List<String> sort) {
        if (sort == null || sort.isEmpty()) {
            return DEFAULT_SORT;
        }

        return sort;
    }
}
