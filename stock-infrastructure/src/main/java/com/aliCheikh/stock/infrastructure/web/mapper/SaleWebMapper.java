package com.aliCheikh.stock.infrastructure.web.mapper;

import com.aliCheikh.stock.application.dto.ListSalesQuery;
import com.aliCheikh.stock.application.dto.PageResult;
import com.aliCheikh.stock.application.dto.SaleView;
import com.aliCheikh.stock.application.dto.SellLineCommand;
import com.aliCheikh.stock.application.dto.SellProductCommand;
import com.aliCheikh.stock.application.dto.SellProductResult;
import com.aliCheikh.stock.domain.model.product.ProductId;
import com.aliCheikh.stock.domain.model.sale.Sale;
import com.aliCheikh.stock.domain.model.sale.SaleLineDto;
import com.aliCheikh.stock.domain.model.shared.Money;
import com.aliCheikh.stock.domain.model.shop.ShopId;
import com.aliCheikh.stock.domain.model.user.UserId;
import com.aliCheikh.stock.infrastructure.web.dto.CreateSaleLine;
import com.aliCheikh.stock.infrastructure.web.dto.CreateSaleRequest;
import com.aliCheikh.stock.infrastructure.web.dto.MoneyResponse;
import com.aliCheikh.stock.infrastructure.web.dto.PageMetaResponse;
import com.aliCheikh.stock.infrastructure.web.dto.PageOfSaleResponse;
import com.aliCheikh.stock.infrastructure.web.dto.SaleLineResponse;
import com.aliCheikh.stock.infrastructure.web.dto.SaleResponse;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public final class SaleWebMapper {
    private static final List<String> DEFAULT_SORT = List.of("createdAt,desc");

    private SaleWebMapper() {
    }

    public static SellProductCommand toCommand(CreateSaleRequest request) {
        return new SellProductCommand(
                UserId.of(request.sellerId()),
                ShopId.of(request.shopId()),
                request.lines().stream()
                        .map(SaleWebMapper::toLineCommand)
                        .toList()
        );
    }

    public static SaleResponse toResponse(SellProductResult result) {
        return new SaleResponse(
                result.saleId().getValue(),
                result.sellerId().getValue(),
                result.lines().stream()
                        .map(SaleWebMapper::toSaleLineResponse)
                        .toList(),
                moneyToResponse(result.totalAmount()),
                result.createdAt()
        );
    }

    public static SaleResponse toResponse(Sale sale) {
        return new SaleResponse(
                sale.getSaleId().getValue(),
                sale.getSoldBy().getValue(),
                sale.getLines().stream()
                        .map(SaleWebMapper::toSaleLineResponse)
                        .toList(),
                moneyToResponse(sale.getTotalAmount()),
                sale.getOccurredAt()
        );
    }

    private static SaleResponse toResponse(SaleView saleView) {
        return new SaleResponse(
                saleView.saleId().getValue(),
                saleView.sellerId().getValue(),
                saleView.lines().stream()
                        .map(SaleWebMapper::toSaleLineResponse)
                        .toList(),
                moneyToResponse(saleView.totalAmount()),
                saleView.createdAt()
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
